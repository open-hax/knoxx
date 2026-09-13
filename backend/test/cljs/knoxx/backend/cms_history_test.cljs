(ns knoxx.backend.cms-history-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.reader :as reader]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.cms-document :as domain]
            [knoxx.backend.domain.node.fs :as node-fs]
            [knoxx.backend.extern.cms-store :as files]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.fastify.cms-documents :as transport]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.cms-store :as store]
            [knoxx.backend.infra.http-server :as http]
            [knoxx.backend.infra.publication-source-revision :as source]
            [knoxx.backend.infra.routes.cms-publication :as publication]))

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
            (is (= {:note "keep" :legacy/visibility "review"} (:metadata saved)))
            (is (= 2 (count (:revisions (store/history! org id)))))
            (is (some #{(:source paths)} (:source_paths (first (store/list! org)))))
            (is (= "system:legacy-cms-import" (:actor (first (:revisions (store/history! org id)))))))))
      (finally (clean! root)))))

(deftest legacy-visibility-does-not-prevent-listing-or-grant-publication
  (let [root (temporary-root)]
    (try
      (with-redefs [files/roots #(roots root) files/configured? (constantly true)]
        (doseq [visibility ["public" "archived"]]
          (let [paths (files/paths "legacy" visibility)]
            (.mkdirSync fs (:root paths) #js {:recursive true})
            (.writeFileSync fs (:metadata paths)
                           (js/JSON.stringify #js {:title visibility :content "retained" :visibility visibility}))))
        (let [documents (store/list! "legacy")]
          (is (= 2 (count documents)))
          (doseq [document documents]
            (is (= "internal" (:visibility document)))
            (is (= (:doc_id document) (get-in document [:metadata :legacy/visibility])))
            (is (= "retained" (:content document)))
            (is (every? #(= :withheld (:publication/state %))
                        (rest (:resources (reader/read-string
                                           (text (:manifest (files/paths "legacy" (:doc_id document))))))))))))
      (finally (clean! root)))))

(defn- resource-document [org value]
  {:document/id (keyword (str "cms." org) (str "doc-" (:doc_id value)))
   :document/org-id org :document/source {:path (:source_path value)}})

(deftest ^:async publication-rejects-an-edit-during-resource-loading
  (let [root (temporary-root)]
    (try
      (with-redefs [files/roots #(roots root) files/configured? (constantly true)]
        (let [org "publish" initial (store/save! org "a" nil (body "Initial" "base" []))
              id (:doc_id initial) document (resource-document org initial)
              target (:manifest (files/paths org id))
              before (text target)
              intent (second (:resources (reader/read-string before)))
              index {:documents {(:document/id document) document} :publications [intent]}
              updated (atom nil)]
          (with-redefs [publication/resource-index! (fn [_ _] index)
                        publication/publication-file-path!
                        (fn [_ _] (reset! updated (store/save! org "b" id (body "Edited" "changed" [(:revision initial)]))) target)]
            (is (= "cms_revision_changed"
                   (try (await (publication/set-publication-state! {} {} (:publication/id intent)
                                                                  {:publication/state :published}))
                        nil (catch :default e (:code (ex-data e))))))
            (is (= before (text target)) "No stale publication intent is written"))
          (let [fresh (resource-document org @updated)]
            (with-redefs [publication/resource-index! (fn [_ _] (assoc index :documents {(:document/id fresh) fresh}))
                          publication/publication-file-path! (fn [_ _] target)]
              (await (publication/set-publication-state! {} {} (:publication/id intent) {:publication/state :published}))
              (is (= :published (:publication/state (second (:resources (reader/read-string (text target)))))))))))
      (finally (clean! root)))))

(deftest ^:async source-digest-rejects-an-edit-during-immutable-source-read
  (let [root (temporary-root)]
    (try
      (with-redefs [files/roots #(roots root) files/configured? (constantly true)]
        (let [org "digest" initial (store/save! org "a" nil (body "Initial" "base" []))
              document (resource-document org initial)]
          (with-redefs [source/source-root (fn [_] (:content (roots root)))
                        node-fs/read-file-or-nil! (fn [p]
                                                  (let [original (text p)]
                                                    (store/save! org "b" (:doc_id initial)
                                                                 (body "Edited" "changed" [(:revision initial)]))
                                                    original))]
            (is (= "cms_revision_changed"
                   (try (await (source/source-revisions! {} [document]))
                        nil (catch :default e (:code (ex-data e))))))
            (is (= "base" (text (:source_path initial))) "The original revision remains immutable"))))
      (finally (clean! root)))))

(deftest logical-source-discovery-retains-one-history-with-concurrent-creation
  (let [root (temporary-root)]
    (try
      (with-redefs [files/roots #(roots root) files/configured? (constantly true)]
        (let [a (store/save! "paths" "a" nil (assoc (body "A" "body A" []) :source_path "docs/foo.md" :metadata {:publication_kind "playlist" :blocks [{:type "track" :path "music/a.ogg"}]}))
              b (store/save! "paths" "b" nil (assoc (body "B" "body B" []) :source_path "docs/foo.md"))
              docs (store/list! "paths")]
          (is (= (:doc_id a) (:doc_id b)))
          (is (= 1 (count docs)))
          (is (= {:publication_kind "playlist" :blocks [{:type "track" :path "music/a.ogg"}]}
                 (:metadata (some (fn [r] (when (= (:revision a) (:revision r)) r))
                                  (:revisions (store/history! "paths" (:doc_id a)))))))
          (is (:conflicted b))
          (is (some #{"docs/foo.md"} (:source_paths (first docs))))
          (is (= #{"body A" "body B"} (set (map :content (:revisions (store/history! "paths" (:doc_id a)))))))
          (let [saved (store/save! "paths" "a" (:doc_id a)
                                   (assoc (body "Resolved" "both" (:revision_heads b)) :source_path (:source_path a)))]
            (is (= "docs/foo.md" (:logical_source_path saved)))
            (is (false? (:conflicted saved)))))
        (doseq [unsafe ["/etc/passwd" "../escape" "docs/../escape" "docs//foo" "C:/escape" "docs\\foo"]]
          (is (thrown? cljs.core/ExceptionInfo
                       (store/save! "paths" "a" nil (assoc (body "Denied" "body" []) :source_path unsafe))))))
      (finally (clean! root)))))
