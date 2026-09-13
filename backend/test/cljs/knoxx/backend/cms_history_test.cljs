(ns knoxx.backend.cms-history-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.reader :as reader]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.cms-document :as domain]
            [knoxx.backend.extern.cms-store :as files]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.fastify.cms-documents :as transport]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.cms-store :as store]
            [knoxx.backend.infra.http-server :as http]))

(defn- temporary-root [] (.mkdtempSync fs (.join path (or (aget js/process.env "KNOXX_CMS_VERIFY_ROOT") (.tmpdir os)) "knoxx-cms-history-")))
(defn- roots [root] {:content (.join path root "content") :resources (.join path root "resources")})
(defn- clean! [root] (.rmSync fs root #js {:recursive true :force true}))
(defn- body [title content parents] {:title title :content content :visibility "review" :parents parents})
(defn- text [p] (.readFileSync fs p "utf8"))

(defn- ^:async request! [app method url principal payload]
  (let [port (.-port (.address (.-server app)))
        response (await (js/fetch (str "http://127.0.0.1:" port url)
                                  (clj->js (cond-> {:method method
                                                   :headers {"x-test-principal" principal "content-type" "application/json"}}
                                             payload (assoc :body (js/JSON.stringify (clj->js payload)))))))]
    {:status (.-status response) :body (js->clj (await (.json response)) :keywordize-keys true)}))

(deftest ^:async authenticated-http-history-preserves-stale-saves
  (let [root (temporary-root)
        app (http/create-app!)
        principals {"writer-a" {:org-id "org-one" :actor-id "actor-a" :permissions ["org.publications.read" "org.publications.manage"]}
                    "writer-b" {:org-id "org-one" :actor-id "actor-b" :permissions ["org.publications.read" "org.publications.manage"]}
                    "reader" {:org-id "org-one" :actor-id "reader" :permissions ["org.publications.read"]}
                    "other-org" {:org-id "org-two" :actor-id "actor-c" :permissions ["org.publications.read" "org.publications.manage"]}}]
    (try
      (transport/register! app nil
                           {:route! (fn [app method url handler] (fastify/route! app {:method method :url url :handler handler}))
                            ;; Resolve a known principal at the real auth-context seam;
                            ;; route permission enforcement is the production function.
                            :with-request-context! (fn [_ req _ operation]
                                                     (operation (get principals (aget (.-headers req) "x-test-principal"))))
                            :ensure-permission! authz/ensure-permission!})
      (await (.listen app #js {:host "127.0.0.1" :port 0}))
      (with-redefs [files/roots #(roots root) files/configured? (constantly true)]
        (is (= 403 (:status (await (request! app "GET" "/api/cms/documents" "anonymous" nil)))))
        (is (= 403 (:status (await (request! app "POST" "/api/cms/documents" "reader" (body "Denied" "x" []))))))
        (let [created (await (request! app "POST" "/api/cms/documents" "writer-a"
                                      (assoc (body "First" "base" []) :actor "spoofed" :org_id "org-two")))
              initial (:body created)
              id (:doc_id initial)
              url (str "/api/cms/documents/" id)
              revision (:revision initial)]
          (is (= 200 (:status created)))
          (is (= "base" (text (:source_path initial))))
          (doseq [[method url payload] [["GET" url nil] ["GET" (str url "/history") nil]
                                        ["PATCH" url (body "Forbidden" "x" [revision])]]]
            (is (= 403 (:status (await (request! app method url "anonymous" payload)))))
            (is (= 404 (:status (await (request! app method url "other-org" payload))))))
          (is (= 400 (:status (await (request! app "PATCH" url "writer-a" (body "Invalid" "x" ["unknown-parent"]))))))
          (is (= 428 (:status (await (request! app "PATCH" url "writer-a" {:title "No base" :content "x"})))))
          (let [a (:body (await (request! app "PATCH" url "writer-a" (body "A title" "branch A" [revision]))))
                b (:body (await (request! app "PATCH" url "writer-b" (body "B title" "branch B" [revision]))))
                audit (:body (await (request! app "GET" (str url "/history") "reader" nil)))
                rows (:revisions audit)]
            (is (= "branch B" (:content b)))
            (is (= "branch B" (text (:source_path b))))
            (is (:conflicted b))
            (is (thrown? cljs.core/ExceptionInfo
                         (store/require-resolved! {:document/id (keyword "cms.org-one" (str "doc-" id))
                                                   :document/org-id "org-one"})))
            (is (= #{(:revision a) (:revision b)} (set (:revision_heads b))))
            (is (= #{"base" "branch A" "branch B"} (set (map :content rows))))
            (is (= #{"actor-a" "actor-b"} (set (map :actor rows))))
            (is (every? :at rows))
            (is (= [revision] (:parents (some #(when (= (:revision b) (:revision %)) %) rows))))
            (let [resolved (:body (await (request! app "PATCH" url "writer-a"
                                                (body "Merged title" "branch A and branch B" (:revision_heads b)))))]
              (is (false? (:conflicted resolved)))
              (is (= [(:revision resolved)] (:revision_heads resolved)))
              (is (= 4 (count (:revisions (:body (await (request! app "GET" (str url "/history") "reader" nil)))))))
              (let [history-root (:history (files/paths "org-one" id))]
                (.rmSync fs (.join path history-root "snapshots") #js {:recursive true :force true})
                (is (= resolved (:body (await (request! app "GET" url "reader" nil)))))
                (is (= "branch A and branch B" (text (:source_path resolved))))
                (is (= "Merged title" (:title (reader/read-string (text (.join path (.dirname path (:source_path resolved)) "metadata.edn"))))))
                (is (false? (.existsSync fs (:metadata (files/paths "org-one" id))))))))))
      (finally (await (.close app)) (clean! root)))))

(deftest legacy-import-retains-originals-and-publication-intent
  (let [root (temporary-root)]
    (try
      (with-redefs [files/roots #(roots root) files/configured? (constantly true)]
        (let [org "legacy-org" id "legacy-doc" paths (files/paths org id)
              original {:doc_id id :title "Original" :content "original body" :visibility "review" :metadata {:note "keep"}}
              manifest (update (domain/manifest org id (:source paths) "Original") :resources
                               #(mapv (fn [r] (if (:publication/id r) (assoc r :publication/state :published) r)) %))]
          (.mkdirSync fs (:root paths) #js {:recursive true})
          (.writeFileSync fs (:metadata paths) (js/JSON.stringify (clj->js original)))
          (.writeFileSync fs (:source paths) "original body")
          (files/create-resource! (:manifest paths) manifest)
          (let [before (text (:metadata paths))
                initial (store/read! org id)
                second (store/read! org id)
                saved (store/save! org "editor" id (body "Renamed" "new body" [(:revision initial)]))
                resource {:ok? true :resource/kind :document :resource/file-path (:manifest paths)
                          :resource/definition (first (:resources manifest))}
                projected (:resource/definition (store/project-resource resource))]
            (is (= initial second))
            (is (= before (text (:metadata paths))))
            (is (= "original body" (text (:source paths))))
            (is (= manifest (reader/read-string (text (:manifest paths)))))
            (is (= "Renamed" (:document/title projected)))
            (is (= (:source_path saved) (get-in projected [:document/source :path])))
            (is (= {:note "keep"} (:metadata saved)))
            (is (= 2 (count (:revisions (store/history! org id)))))
            (is (some #{(:source paths)} (:source_paths (first (store/list! org)))))
            (is (= "system:legacy-cms-import" (:actor (first (:revisions (store/history! org id)))))))))
      (finally (clean! root)))))
