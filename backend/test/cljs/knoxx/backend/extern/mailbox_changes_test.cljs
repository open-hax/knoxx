(ns knoxx.backend.extern.mailbox-changes-test
  (:require ["node:events" :refer [EventEmitter]]
            [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.http-server :as http-server]
            [knoxx.backend.extern.mailbox-changes :as stream]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clio-application-store :as ledger]
            [knoxx.backend.infra.stores.clio-mailbox-store :as clio]
            [knoxx.backend.infra.stores.mailbox-store :as registry]
            [knoxx.backend.shape.mailbox-store :as mailbox]))

(def context {:org-id "org-a" :user-id "user-a" :membership-id "member-a" :actor-binding "reader"
              :permissions ["agent.chat.use"]})
(def scope {:org-id "org-a" :actor-id "sender" :admin? false})
(def message {:mailbox/id "visible" :mailbox/kind "actor-message"
              :mailbox/source {:actor-id "sender"} :mailbox/target {:actor-id "reader"}
              :mailbox/content "private body" :mailbox/metadata {} :mailbox/content-ref {}
              :mailbox/delivery {:mode "inbox-only"}})

(defn- tick! [] (js/Promise. (fn [resolve _reject] (js/setImmediate resolve))))
(defn- reply-fixture []
  (let [raw (EventEmitter.) frames (atom []) ended (atom false)]
    (aset raw "writeHead" (fn [& _args] nil))
    (aset raw "flushHeaders" (fn [] nil))
    (aset raw "write" (fn [frame] (swap! frames conj frame) true))
    (aset raw "end" (fn [] (reset! ended true)))
    {:reply #js {:raw raw :hijack (fn [] nil)} :raw raw :frames frames :ended ended}))

(defn- ^:async attempt! [operation]
  (try (await (operation)) nil (catch :default error (ex-data error))))

(deftest ^:async stream-is-scoped-and-only-successful-state-changes-invalidate
  (let [directory (disk/temp-directory!) previous @registry/provider* current (atom context)
        {:keys [reply raw frames ended]} (reply-fixture)]
    (try
      (let [provider (clio/open! {:directory directory})]
        (registry/install! provider)
        (with-redefs [authz/current-context! (fn [_ _]
                                              (or @current (throw (ex-info "revoked" {:status 401}))))]
          (stream/open-stream! {} context reply)
          (await (tick!))
          (is (= ["event: mailbox-changed\ndata: {}\n\n"] @frames))
          (await (mailbox/create-entry! provider (assoc scope :org-id "foreign") message))
          (await (mailbox/create-entry! provider scope
                                       (-> message (assoc :mailbox/id "unrelated")
                                           (assoc-in [:mailbox/target :actor-id] "other"))))
          (await (tick!))
          (is (= 1 (count @frames)))
          (await (mailbox/create-entry! provider scope message))
          (await (tick!))
          (is (= 2 (count @frames)))
          (await (mailbox/create-entry! provider scope message))
          (is (= 409 (:status (await (attempt! #(mailbox/create-entry! provider scope
                                                                                      (assoc message :mailbox/content "changed")))))))
          (is (= "append_refused"
                 (:code (await (attempt!
                                #(mailbox/create-entry!
                                  (assoc-in provider [:ledger :before-append]
                                            (fn [_] (throw (ex-info "disk refused" {:code "append_refused"}))))
                                  scope (assoc message :mailbox/id "refused")))))))
          (await (tick!))
          (is (= 2 (count @frames)))
          (reset! current nil)
          (await (mailbox/create-entry! provider scope (assoc message :mailbox/id "after-revocation")))
          (await (tick!))
          (is (true? @ended))
          (is (= 2 (count @frames)))
          (is (every? #(= "event: mailbox-changed\ndata: {}\n\n" %) @frames))))
      (catch :default error (is false (str "Unexpected mailbox SSE failure: " error)))
      (finally (.emit raw "close") (registry/install! previous) (fs/remove-tree! directory)))))

(deftest invalid-post-append-hook-is-rejected-before-creating-files
  (let [directory (disk/temp-directory!) child (str directory "/invalid")]
    (try
      (is (thrown? cljs.core.ExceptionInfo
                   (ledger/open! {:directory child :stream "fixture" :reads {} :writes {}
                                  :projection (fn [] {}) :after-append "invalid"})))
      (is (false? (fs/exists? child)))
      (finally (fs/remove-tree! directory)))))

(deftest ^:async hijacked-mailbox-stream-does-not-trigger-a-second-fastify-send
  (let [directory (disk/temp-directory!) previous @registry/provider*
        app (http-server/create-app! {:request-logging? false})
        implicit-sends (atom 0)]
    (try
      (registry/install! (clio/open! {:directory directory}))
      (with-redefs [authz/current-context! (fn [_ _] context)]
        (.get app "/stream"
              (fn [_request reply]
                (let [native-send (.-send ^js reply)]
                  (set! (.-send ^js reply)
                        (fn [& args]
                          (swap! implicit-sends inc)
                          (.apply native-send reply (to-array args)))))
                (js/setTimeout #(.end (.-raw ^js reply)) 20)
                (stream/open-stream! {} context reply)))
        (let [response (await (.inject app #js {:method "GET" :url "/stream"}))]
          (is (= 200 (.-statusCode ^js response)))
          (is (= "event: mailbox-changed\ndata: {}\n\n" (.-body ^js response)))
          (is (zero? @implicit-sends))))
      (finally
        (await (http-server/close! app))
        (registry/install! previous)
        (fs/remove-tree! directory)))))
