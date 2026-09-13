(ns knoxx.backend.provider-recovery-test
  (:require [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            [knoxx.backend.extern.clio-store :as clio-host]
            [knoxx.backend.extern.mcp-oauth-store :as oauth-host]
            [knoxx.backend.extern.provider-recovery-fixture :as fixture]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.clio-policy-store :as policy]
            [knoxx.backend.infra.clio-translation-evidence-store :as evidence]
            [knoxx.backend.infra.clio-translation-split-store :as splits]
            [knoxx.backend.infra.db.policy :as policy-db]
            [knoxx.backend.infra.identity-bindings :as bindings]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.infra.stores.cache-registry :as cache-registry]
            [knoxx.backend.infra.stores.clio-cache-store :as cache]
            [knoxx.backend.infra.stores.clio-mcp-oauth :as oauth]
            [knoxx.backend.infra.stores.clio-run-store :as runs]
            [knoxx.backend.infra.stores.clio-thread-store :as threads]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.stores.mongo-memory-sessions :as memory-sessions]
            [knoxx.backend.infra.stores.mongo-rate-limits :as rate-limits]
            [knoxx.backend.infra.stores.mongo-session-store :as session-facade]
            [knoxx.backend.infra.stores.mongo-session-titles :as titles]
            [knoxx.backend.infra.stores.mongo-temp-memory :as temp-memory]
            [knoxx.backend.infra.translation-evidence-store :as evidence-protocol]
            [knoxx.backend.infra.translation-split-store :as split-protocol]
            [knoxx.backend.law.translation-dispatch :as dispatch]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-fixture :as split-fixture]
            [knoxx.backend.shape.cache-store :as cache-protocol]
            [knoxx.backend.shape.mcp-oauth-store :as oauth-protocol]
            [knoxx.backend.shape.session-persistence :as run-protocol]
            [knoxx.backend.shape.thread-store :as thread-protocol]))

(defn- turn []
  (let [manifest (split-fixture/manifest)
        claim (split-fixture/claim manifest)]
    (split/translation-turn-admission
     split-fixture/digest
     {:dispatch-key "recovery-dispatch" :run-id "recovery-run" :admitted-at split-fixture/recorded-at
      :manifest manifest :candidate-claim claim
      :execution (split/execution-snapshot split-fixture/digest
                   {:agent-id "publication_translator" :model "fixture" :thinking :medium
                    :system-prompt "Translate the admitted text." :tool-ids ["save_translation"]})
      :memory (split/memory-snapshot {:status :empty :examples []})})))

(deftest ^:async translation-split-replay-preserves-qualified-coordinates
  (let [directory (fixture/temporary-directory)]
    (try
      (let [store (splits/open! {:directory directory :digest-hex split-fixture/digest})
            value (turn)]
        (is (= value (await (split-protocol/admit-turn! store value))))
        (is (= value (await (split-protocol/admit-turn! store value))))
        (is (= 1 (count (clio/history (:engine store)))))
        (let [reopened (splits/open! {:directory directory :digest-hex split-fixture/digest})]
          (is (= value (await (split-protocol/turn-for-run! reopened "recovery-run"))))
          (is (= :open-hax.documents/start-here
                 (get-in (await (split-protocol/turn-for-run! reopened "recovery-run"))
                         [:translation-turn/manifest :split-manifest/document]))))
        (fixture/corrupt! directory)
        (try (await (split-protocol/turn-for-run! store "recovery-run")) (is false "Corruption must refuse replay")
             (catch :default error (is (some? error)))))
      (finally (fixture/remove! directory)))))

(deftest ^:async evidence-retry-answer-does-not-create-a-state-transition
  (let [directory (fixture/temporary-directory)]
    (try
      (let [store (evidence/open! {:directory directory})
            record (dispatch/dispatch-record
                     {:document :knoxx.docs/probe :locale :es :revision "source-v1" :replace-stale? false}
                     {:dispatch/garden "knoxx.docs/garden" :dispatch/document-wire-id "knoxx.docs/probe"
                      :dispatch/source-locale :en :dispatch/org-id "org-1" :dispatch/membership-id "member-1"}
                     :dispatch/accepted "2026-08-30T12:00:00.000Z" :attempt-id "attempt-1")]
        (is (= :reserved (:reservation/status (await (evidence-protocol/reserve-dispatch! store record)))))
        (is (= :in-flight (:reservation/status (await (evidence-protocol/reserve-dispatch! store record)))))
        (is (= 1 (count (clio/history (:engine store)))) "Transient :answer cannot become an accepted state change")
        (let [reopened (evidence/open! {:directory directory})]
          (is (= record (await (evidence-protocol/dispatch-for-key! reopened (:dispatch/key record))))))
        (let [engine (:engine store)]
          (await (clio/write! engine "stable-op" :translation-evidence-store/bind-dispatch-batch [record "batch-1"]))
          (try
            (await (clio/write! engine "stable-op" :translation-evidence-store/bind-dispatch-batch [record "other"]))
            (is false "Reusing an operation id with different arguments must conflict")
            (catch :default error (is (= 409 (:status (ex-data error))))))))
      (finally (fixture/remove! directory)))))

(def client
  {:client_id "client-1" :redirect_uris ["http://localhost/callback"]
   :token_endpoint_auth_method "none" :grant_types ["authorization_code"] :response_types ["code"]})

(defn- grant [now]
  {:clientId "client-1" :membershipId "member-1" :axxiumPrincipalId "actor-1" :axxiumEntityId "entity-1"
   :orgSlug "org-1" :tools ["wiki_get"] :createdAt (fixture/instant now)})

(deftest ^:async oauth-exchange-is-single-use-and-never-persists-raw-credentials
  (let [directory (fixture/temporary-directory)]
    (try
      (let [store (oauth/open! {:directory directory})
            peer (oauth/open! {:directory directory})
            now (fixture/now-ms)
            code (assoc (grant now) :redirectUri "http://localhost/callback" :codeChallenge "challenge" :codeChallengeMethod "S256")
            code-id (oauth-host/digest "private-code-value")
            token (assoc (grant now) :expiresAt (fixture/instant (+ now 60000)))
            exchange (fn [value] {:code-id code-id :expected code
                                  :token {:id (oauth-host/digest value) :payload token :ttl 60 :now now}})]
        (await (oauth-protocol/oauth-admit! store :oauth/register {:id "client-1" :payload client}))
        (await (oauth-protocol/oauth-admit! store :oauth/issue-code {:id code-id :payload code :ttl 60 :now now}))
        (let [results (await (fixture/settled
                              [(oauth-protocol/oauth-admit! store :oauth/exchange (exchange "private-token-a"))
                               (oauth-protocol/oauth-admit! peer :oauth/exchange (exchange "private-token-b"))]))]
          (is (= 1 (count (filter #(true? (get-in % [:value :accepted?])) results))))
          (is (every? #(or (= :fulfilled (:status %)) (= 409 (get-in % [:error :status]))) results)))
        (is (nil? (await (oauth-protocol/oauth-read! peer :oauth/code {:id code-id :now now}))))
        (let [items (await (oauth-protocol/oauth-read! peer :oauth/tokens {:membership-id "member-1" :now now}))
              token-id (:tokenId (first items))]
          (is (= 1 (count items)))
          (is (false? (await (oauth-protocol/oauth-admit! peer :oauth/revoke-token {:id token-id :membership-id "foreign"}))))
          (is (true? (await (oauth-protocol/oauth-admit! peer :oauth/revoke-token {:id token-id :membership-id "member-1"}))))
          (is (empty? (await (oauth-protocol/oauth-read! (oauth/open! {:directory directory}) :oauth/tokens {:membership-id "member-1" :now now})))))
        (is (not (str/includes? (fixture/ledger-text directory) "private-code-value")))
        (is (not (str/includes? (fixture/ledger-text directory) "private-token-"))))
      (finally (fixture/remove! directory)))))

(deftest ^:async thread-restart-concurrency-identity-and-expiry
  (let [directory (fixture/temporary-directory) clock (atom 1000)]
    (try
      (let [store (threads/open! {:directory directory :instance-id "instance-1" :now-ms #(deref clock)})
            peer (threads/open! {:directory directory :instance-id "instance-2" :now-ms #(deref clock)})
            initial {:session_id "thread-1" :conversation_id "conversation-1" :org_id "org-1" :status "running"}]
        (await (thread-protocol/put-thread! store initial))
        (is (= "instance-1" (:system_instance_id (await (thread-protocol/read-thread peer "thread-1")))))
        (await (fixture/settled [(thread-protocol/patch-thread! store "thread-1" {:answer "A"})
                                (thread-protocol/patch-thread! peer "thread-1" {:error "B"})]))
        (let [current (await (thread-protocol/read-thread peer "thread-1"))]
          (is (= "A" (:answer current))) (is (= "B" (:error current))))
        (try (await (thread-protocol/patch-thread! store "thread-1" {:org_id "foreign"}))
             (is false "Admitted tenant identity must be immutable")
             (catch :default error (is (= 409 (:status (ex-data error))))))
        (let [before (count (clio/history (:engine store)))]
          (await (thread-protocol/rewind-thread! store "thread-1" 1))
          (is (= before (count (clio/history (:engine store)))) "Rewind without a transcript is a no-op"))
        (reset! clock (+ 1000 3600000))
        (is (nil? (await (thread-protocol/read-thread store "thread-1")))))
      (finally (fixture/remove! directory)))))

(deftest ^:async cache-counter-admissions-preserve-window-and-restart-state
  (let [directory (fixture/temporary-directory) clock (atom 1000)]
    (try
      (let [store (cache/open! {:directory directory :now-ms #(deref clock)})
            peer (cache/open! {:directory directory :now-ms #(deref clock)})]
        (await (cache-protocol/write-value! store :titles "session-1" "Title" 100))
        (is (= "Title" (await (cache-protocol/read-value! peer :titles "session-1"))))
        (let [results (await (fixture/settled [(cache-protocol/increment! store "request" 100)
                                             (cache-protocol/increment! peer "request" 100)]))]
          (is (= #{1 2} (set (map :value results)))))
        (reset! clock 1099)
        (is (= 3 (await (cache-protocol/increment! store "request" 100))))
        (reset! clock 1100)
        (is (nil? (await (cache-protocol/read-value! peer :titles "session-1"))))
        (is (= 1 (await (cache-protocol/increment! peer "request" 100)))))
      (finally (fixture/remove! directory)))))

(deftest ^:async local-policy-removal-of-current-role-revokes-next-command
  (let [directory (fixture/temporary-directory)]
    (try
      (let [store (policy/open! {:directory directory})
            principal {:principal/id "actor-1" :principal/entity-id "entity-1" :principal/kind :human
                       :principal/username "operator" :principal/display-name "Operator" :principal/status :active
                       :principal/roles ["system-admin"] :principal/capabilities []}
            binding {:principal-id "actor-1" :entity-id "entity-1" :kind :human :user-id "user-1"
                     :membership-id "member-1" :org-id "org-1" :actor-id "actor-1"}
            actor {:principal principal :user-id "user-1" :membership-id "member-1"}
            command (fn [id operation args] {:id id :at "2026-09-12T00:00:00.000Z" :actor actor :operation operation :args args})]
        (await (policy/initialize! store {:org {:id "org-1" :slug "org-1" :name "Organization" :status "active"}
                                         :roles {"system-admin" {:permissions ["platform.org.create" "org.members.update"]}} :tools []}))
        (await (policy/observe! store binding principal))
        (is (= ["system-admin"] (:role-slugs (await (policy/read! store :policy/context [binding principal])))))
        (await (policy/command! store (command "revoke-self" :policy/membership-roles ["member-1" {:org-id "org-1" :role-ids [] :replace true}])))
        (is (empty? (:permissions (await (policy/read! (policy/open! {:directory directory}) :policy/context [binding principal])))))
        (try (await (policy/command! store (command "new-org" :policy/create-org [{:name "Refused"}])))
             (is false "A stale authenticated principal must not retain a revoked local role")
             (catch :default error (is (= 403 (:status (ex-data error)))))))
      (finally (fixture/remove! directory)))))

(deftest ^:async change-observers-only-see-accepted-appends-and-cannot-break-durability
  (let [directory (fixture/temporary-directory)
        changes (atom []) failures (atom [])
        unsubscribe (clio/subscribe! (fn [& args] (swap! changes conj args)))
        unsubscribe-failing (clio/subscribe! (^:async fn [] (throw (ex-info "observer-refused" {}))))]
    (try
      (with-redefs [clio-host/report-subscriber-failure! #(swap! failures conj (ex-message %))]
        (let [store (oauth/open! {:directory directory})
              engine (:engine store)
              args [{:id "client-1" :payload client}]]
          (is (true? (await (clio/write! engine "observer-op" :oauth/register args))))
          (await (fixture/drain!))
          (is (= [[]] (mapv vec @changes)) "Notifications carry no event data")
          (is (= ["observer-refused"] @failures))
          (is (true? (await (clio/write! engine "observer-op" :oauth/register args))))
          (try (await (clio/write! engine "observer-op" :oauth/register [{:id "other" :payload client}]))
               (is false "Operation collision must fail without notifying")
               (catch :default error (is (= 409 (:status (ex-data error))))))
          (is (= 1 (count @changes)))
          (is (= client (await (oauth-protocol/oauth-read! (oauth/open! {:directory directory}) :oauth/client {:id "client-1"}))))
          (unsubscribe)
          (unsubscribe-failing)
          (await (oauth-protocol/oauth-admit! store :oauth/register {:id "client-2" :payload (assoc client :client_id "client-2")}))
          (is (= 1 (count @changes)))) )
      (finally (unsubscribe) (unsubscribe-failing) (fixture/remove! directory)))))

(deftest ^:async run-events-restart-order-conflict-and-expiry
  (let [directory (fixture/temporary-directory)
        at (atom "2026-09-12T12:00:00.000Z")
        options {:directory directory :clock! #(deref at) :instance-id "first"}
        run {:run_id "run-1" :session_id "session-1" :conversation_id "conversation-1"
             :org_id "org-1" :user_id "user-1" :status "running"
             :created_at @at :updated_at @at}
        event {:event_id "event-1" :run_id "run-1" :session_id "session-1"
               :conversation_id "conversation-1" :at @at :type "run_started"}]
    (try
      (let [store (runs/open! options)
            peer (runs/open! (assoc options :instance-id "second"))]
        (await (run-protocol/put-run! store run))
        (is (= "first" (:system_instance_id (await (run-protocol/get-run peer "run-1")))))
        (let [result (await (run-protocol/append-event! store event))]
          (is (= 1 (:sequence result)))
          (is (= result (await (run-protocol/append-event! peer event)))))
        (is (= 2 (count (clio/history (:engine store)))) "Exact event retry adds no ledger fact")
        (try (await (run-protocol/append-event! peer (assoc event :type "changed")))
             (is false "Reusing an event identity must refuse changed facts")
             (catch :default error (is (= 409 (:status (ex-data error))))))
        (let [results (await (fixture/settled
                             [(run-protocol/append-event! store (assoc event :event_id "event-2" :type "progress"))
                              (run-protocol/append-event! peer (assoc event :event_id "event-3" :type "progress"))]))]
          (is (every? #(= :fulfilled (:status %)) results)))
        (is (= [2 3] (mapv :sequence (await (run-protocol/events-since peer "run-1" 1)))))
        (is (= [] (await (run-protocol/events-since peer "run-1" @at))) "Legacy timestamps are strict")
        (try (await (run-protocol/patch-run! store "run-1" {:org_id "foreign"}))
             (is false "Owner scope cannot move")
             (catch :default error (is (= 409 (:status (ex-data error))))))
        (try (await (run-protocol/patch-run! store "run-1" {:run_events []}))
             (is false "Run patches cannot replace accepted event history")
             (catch :default error (is (= 409 (:status (ex-data error))))))
        (reset! at "2026-09-12T14:00:00.000Z")
        (is (nil? (await (run-protocol/get-run peer "run-1"))))
        (is (= [] (await (run-protocol/events-since store "run-1" nil))))
        (is (= 4 (count (clio/history (:engine store)))) "Expiry is a view, not a history rewrite"))
      (finally (fixture/remove! directory)))))

(deftest ^:async mongo-identity-rechecks-policy-after-role-hydration
  (let [binding {:principal-id "actor" :entity-id "entity" :kind :human
                 :user-id "user" :org-id "org" :membership-id "member" :actor-id "actor"}
        row {:id "member" :user_id "user" :org_id "org" :actor_id "actor"
             :status "active" :user_status "active" :org_status "active"}
        current (atom row)
        hydrations (atom 0)]
    (with-redefs [bindings/read! (fn [_ _] binding)
                  policy-db/db! (fn [] nil)
                  mongo-directory/find-membership-row-with-user-org! (fn ([_] @current) ([_ _] @current))
                  policy-db/build-request-context
                  (fn [_ _] (swap! hydrations inc) {:permissions ["read"]})]
      (is (= {:permissions ["read"]} (await (policy-db/resolve-bound-context! {} binding))))
      (is (= 2 @hydrations)))
    (doseq [revoked [(assoc row :status "inactive") (assoc row :id "different")
                     (assoc row :actor_id "other") (assoc row :org_status "suspended")]]
      (reset! current row)
      (with-redefs [bindings/read! (fn [_ _] binding)
                    policy-db/db! (fn [] nil)
                    mongo-directory/find-membership-row-with-user-org! (fn ([_] @current) ([_ _] @current))
                    policy-db/build-request-context (fn [_ _] (reset! current revoked) {:permissions ["read"]})]
        (try (await (policy-db/resolve-bound-context! {} binding))
             (is false "Policy revocation during awaited hydration must refuse")
             (catch :default error (is (= 403 (:status (ex-data error))))))))))

(deftest ^:async thread-facade-explicit-mongo-cannot-contaminate-local-cache
  (let [directory (fixture/temporary-directory)
        previous @session-facade/provider*]
    (try
      (let [store (threads/open! {:directory directory})
            db (fixture/mock-thread-db)
            local {:session_id "shared" :conversation_id "local-conversation" :status "running"}
            foreign {:session_id "shared" :conversation_id "mongo-conversation" :status "completed"}]
        (session-facade/install! store)
        (await (session-facade/put-session! local))
        (await (session-facade/put-session! db foreign))
        (is (= "local-conversation" (:conversation_id (session-facade/get-session-sync "shared"))))
        (is (= "mongo-conversation" (:conversation_id (await (session-facade/get-session db "shared")))))
        (await (session-facade/remove-session! db "shared" nil))
        (is (= "local-conversation" (:conversation_id (session-facade/get-session-sync "shared"))))
        (is (= "local-conversation" (:conversation_id (await (session-facade/get-session "shared")))))
        (fixture/corrupt! directory)
        (try (await (session-facade/update-session! "shared" {:answer "uncommitted"}))
             (is false "Failed durable admission must refuse instead of caching the mutation")
             (catch :default error (is (some? error))))
        (is (nil? (:answer (session-facade/get-session-sync "shared")))))
      (finally (session-facade/install! previous) (fixture/remove! directory)))))

(deftest ^:async legacy-cache-facades-use-installed-provider-without-mongo
  (let [directory (fixture/temporary-directory)
        previous (cache-registry/current)]
    (try
      (cache-registry/install! (cache/open! {:directory directory}))
      (with-redefs [mongo-client/get-db (fn [] (throw (ex-info "Mongo must not be consulted" {})))]
        (is (= {:title "Local" :session "session"}
               (await (titles/upsert-title! "session" {:title "Local"}))))
        (is (= {:title "Local"} (await (titles/get-title! "session"))))
        (is (= {:key "memory" :written true}
               (await (temp-memory/set-memory! "memory" {:note :namespaced/value} 60))))
        (is (= {:note :namespaced/value} (await (temp-memory/get-memory! "memory"))))
        (is (= {:value ["session"]}
               (await (memory-sessions/set-cache-entry! "list" {:value ["session"]}))))
        (is (= {:value ["session"]} (await (memory-sessions/get-cache-entry! "list"))))
        (is (= 1 (await (rate-limits/increment-rate-limit! "limit" 60))))
        (is (= 2 (await (rate-limits/increment-rate-limit! "limit" 60))))
        (await (titles/delete-title! "session"))
        (is (nil? (await (titles/get-title! "session"))))
        (await (temp-memory/delete-memory! "memory"))
        (is (nil? (await (temp-memory/get-memory! "memory")))))
      (finally
        (cache-registry/install! previous)
        (fixture/remove! directory)))))
