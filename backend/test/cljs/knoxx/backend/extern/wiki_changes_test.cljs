(ns knoxx.backend.extern.wiki-changes-test
  "Exercise the registered SSE handler with real durable providers and scoped authority."
  (:require ["fastify" :default Fastify]
            ["node:events" :as events]
            [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.source-review :as review-domain]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.extern.wiki-changes :as stream]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.clio-source-authoring-store :as sources]
            [knoxx.backend.infra.clio-source-review-store :as reviews]
            [knoxx.backend.infra.identity :as identity]
            [knoxx.backend.infra.source-review-store :as review-store]
            [knoxx.backend.infra.wiki-commands :as commands]
            [knoxx.backend.infra.wiki-runtime :as runtime]))

(def ^:private scope {:org-id "org-a" :project "wiki"})
(def ^:private context {:org-id "org-a" :user-id "principal-a" :membership-id "member-a"
                       :permissions ["org.publications.read" "org.publications.manage"]})
(def ^:private initial-frame "event: publication-changed\ndata: {\"document\":null}\n\n")

(defn- reply-fixture []
  (let [raw (events/EventEmitter.) frames (atom []) ended (atom 0)]
    (aset raw "writeHead" (fn [& _] nil))
    (aset raw "flushHeaders" (fn [] nil))
    (aset raw "write" (fn [frame] (swap! frames conj frame) true))
    (aset raw "end" (fn [] (swap! ended inc) (.emit raw "close")))
    {:reply #js {:raw raw :hijack (fn [] nil)} :raw raw :frames frames :ended ended}))

(defn- tracked-subscribe [subscribe active]
  (let [track (fn [args]
                (let [unsubscribe (apply subscribe args) released? (atom false)]
                  (swap! active inc)
                  (fn []
                    (when (compare-and-set! released? false true)
                      (swap! active dec)
                      (unsubscribe)))))]
    (fn ([listener] (track [listener]))
        ([selection listener] (track [selection listener])))))

(defn- tracked-clio-subscribe [active]
  (let [subscribe (tracked-subscribe clio/subscribe! active)]
    (fn ([listener] (subscribe {} (fn [_stream] (listener))))
        ([selection listener] (subscribe selection listener)))))

(defn- ^:async fixture! [body]
  (let [directory (disk/temp-directory!) current (atom context) refreshes (atom 0)
        active (atom 0) handler (atom nil) reply (reply-fixture)
        config {:contracts-dir (str directory "/contracts") :session-project-name "wiki"}
        dependencies {:source-provider (sources/open! {:directory (str directory "/source")})
                      :provider (reviews/open! {:directory (str directory "/review")})
                      :now! (constantly "2026-09-20T00:00:00Z")}]
    (try
      (with-redefs [authz/resolve-request-context! (fn [_ _] context)
                    identity/current-context! (fn [_ _] (swap! refreshes inc)
                                                (or @current (throw (ex-info "Revoked" {:status 401}))))
                    fastify/route! (fn [_ route] (reset! handler (:handler route)))
                    clio/subscribe! (tracked-clio-subscribe active)
                    commands/subscribe! (tracked-subscribe commands/subscribe! active)
                    runtime/source-dependencies (fn ([] dependencies) ([_] dependencies))]
        (stream/register! nil {} config)
        (await (@handler #js {} (:reply reply)))
        (await (disk/drain!))
        (await (body (merge reply {:directory directory :config config :dependencies dependencies
                                  :current current :refreshes refreshes :active active}))))
      (finally (.emit (:raw reply) "close") (fs/remove-tree! directory)))))

(defn- review! [provider selected document]
  (let [document-scope (assoc selected :document document)]
    (review-store/admit-source-review!
     provider document-scope nil
     (review-domain/event-for-command
      document-scope {:id "principal" :kind :human}
      {:operation-id "submit" :revision "private-revision" :source-locale :en
       :expected-head nil :action :submit} "2026-09-20T00:00:00Z"))))

(defn- unrelated-store! [directory]
  (clio/open! {:directory (str directory "/oauth") :stream "knoxx/oauth"
               :projection #(let [state (atom nil)] {:store state :snapshot (fn [] @state)})
               :reads {} :writes {:oauth/token (fn [state token] (reset! state token))}}))

(deftest ^:async unrelated-streams-and-tenants-do-not-refresh-authority-or-write-frames
  (await (fixture!
          (^:async fn [{:keys [directory dependencies frames refreshes active raw ended]}]
            (let [provider (:provider dependencies) calls @refreshes]
              (is (= [initial-frame] @frames))
              (is (= 2 @active))
              (await (clio/write! (unrelated-store! directory) :oauth/token ["private-token"]))
              (await (review! provider (assoc scope :org-id "foreign") :docs/one))
              (await (review! provider (assoc scope :project "other") :docs/two))
              (commands/changed! (assoc scope :org-id "foreign" :document :docs/three))
              (await (disk/drain!))
              (is (= calls @refreshes) "Irrelevant appends do no per-client authorization work")
              (is (= [initial-frame] @frames))
              (await (review! provider scope :docs/visible))
              (await (disk/drain!))
              (is (= (inc calls) @refreshes))
              (is (= [initial-frame initial-frame] @frames) "Hints contain no private source or review facts")
              (await (review! provider scope :docs/visible))
              (await (disk/drain!))
              (is (= 2 (count @frames)) "A provider retry does not notify")
              (.emit raw "close")
              (.emit raw "error" (js/Error. "Already closed"))
              (is (= 1 @ended))
              (is (zero? @active) "Both real bus subscriptions are released once")
              (await (review! provider scope :docs/after-close))
              (commands/changed! (assoc scope :document :docs/after-close))
              (await (disk/drain!))
              (is (= (inc calls) @refreshes))
              (is (= 2 (count @frames))))))))

(deftest ^:async revoked-permissions-and-changed-tenant-close-before-writing
  (doseq [replacement [nil (assoc context :permissions []) (assoc context :org-id "foreign")]]
    (await (fixture!
            (^:async fn [{:keys [current dependencies frames ended active]}]
              (reset! current replacement)
              (await (review! (:provider dependencies) scope :docs/trigger))
              (await (disk/drain!))
              (is (= [initial-frame] @frames))
              (is (= 1 @ended))
              (is (zero? @active)))))))

(defn- ^:async create-command! [config]
  (await (commands/create!
          config context {:operation_id "create-one" :title "Private page" :content "Private source."
                          :source_locale "en" :garden "test/garden" :target_locales ["es"]})))

(defn- ^:async seed-garden! [config]
  (fs/ensure-dir! (:contracts-dir config))
  (await (files/write-text!
          (:contracts-dir config) "namespaces/garden.edn"
          "{:namespace :test :resources [{:garden/id :garden :garden/title \"Garden\" :garden/status :active :garden/locales [:en :es]}]}")))

(deftest ^:async durable-source-and-projection-completion-remain-distinct-invalidations
  (await (fixture!
          (^:async fn [{:keys [config frames]}]
            (await (seed-garden! config))
            (let [entered (disk/deferred) release (disk/deferred) write! files/write-text!]
              (with-redefs [files/write-text! (^:async fn [root relative text]
                                               ((:resolve! entered) nil)
                                               (await (:promise release))
                                               (await (write! root relative text)))]
                (let [pending (create-command! config)]
                  (try
                    (await (js/Promise.race #js [(:promise entered) pending]))
                    (await (disk/drain!))
                    (is (= [initial-frame initial-frame] @frames)
                        "Durable source append is observable while projection is paused")
                    ((:resolve! release) nil)
                    (let [created (await pending)]
                      (await (disk/drain!))
                      (is (= 3 (count @frames)))
                      (is (= (str "event: publication-changed\ndata: "
                                  (js/JSON.stringify #js {:document (get-in created [:review :document])}) "\n\n")
                             (last @frames))
                          "The completion hint follows usable content and resource projection"))
                    (finally ((:resolve! release) nil) (await pending))))))))))

(deftest ^:async real-fastify-handler-denies-unauthenticated-and-unauthorized-streams
  (let [app (Fastify #js {:logger false}) current (atom nil)]
    (try
      (with-redefs [authz/resolve-request-context! (fn [_ _]
                                                   (or @current (throw (ex-info "No identity" {:status 401}))))]
        (stream/register! app {} {:session-project-name "wiki"})
        (doseq [[ctx status] [[nil 401] [(assoc context :permissions []) 403]]]
          (reset! current ctx)
          (let [response (await (.inject app #js {:method "GET" :url "/api/publications/changes"}))]
            (is (= status (.-statusCode response)))
            (is (not= "text/event-stream" (aget (.-headers response) "content-type"))))))
      (finally (await (.close app))))))

(deftest ^:async real-fastify-stream-writes-an-initial-frame-and-unsubscribes-on-close
  (let [app (Fastify #js {:logger false}) active (atom 0)]
    (try
      (with-redefs [authz/resolve-request-context! (fn [_ _] context)
                    identity/current-context! (fn [_ _] context)
                    clio/subscribe! (tracked-clio-subscribe active)
                    commands/subscribe! (tracked-subscribe commands/subscribe! active)]
        (.addHook app "onRequest"
                  (fn [_request reply done]
                    (let [raw (.-raw reply) write! (.-write raw)]
                      (set! (.-write raw)
                            (fn [& args]
                              (let [result (.apply write! raw (to-array args))]
                                (js/setImmediate #(.end raw)) result))))
                    (done)))
        (stream/register! app {} {:session-project-name "wiki"})
        (let [response (await (.inject app #js {:method "GET" :url "/api/publications/changes"}))]
          (await (disk/drain!))
          (is (= 200 (.-statusCode response)))
          (is (= initial-frame (.-body response)))
          (is (zero? @active))))
      (finally (await (.close app))))))
