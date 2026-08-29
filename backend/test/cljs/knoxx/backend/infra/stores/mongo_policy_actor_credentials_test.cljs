(ns knoxx.backend.infra.stores.mongo-policy-actor-credentials-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.extern.mongo :as extern-mongo]
            [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as creds]))

;; Mock built on the same pattern as mongo-policy-tools-test.

(declare matches-query?)

(defn- equal-value? [actual expected case-insensitive?]
  (if (and case-insensitive? (string? actual) (string? expected))
    (= (str/lower-case actual) (str/lower-case expected))
    (= actual expected)))

(defn- matches-clause? [actual v case-insensitive?]
  (cond
    (and (map? v) (contains? v :$in))
    (some #(equal-value? actual % case-insensitive?) (:$in v))
    (and (map? v) (contains? v :$exists)) (= (:$exists v) (some? actual))
    (and (map? v) (contains? v :$ne))
    (not (equal-value? actual (:$ne v) case-insensitive?))
    (map? v) (= actual v)
    :else (equal-value? actual v case-insensitive?)))

(defn- document-value [doc field]
  (reduce (fn [value part]
            (get value (keyword part)))
          doc
          (str/split (name field) #"\.")))

(defn- matches-query?
  ([doc query]
   (matches-query? doc query false))
  ([doc query case-insensitive?]
   (let [clauses (:$or query)
         fields (dissoc query :$or)]
     (and (every? (fn [[k v]]
                    (matches-clause?
                     (document-value doc k) v case-insensitive?))
                  fields)
          (or (nil? clauses)
              (some #(matches-query? doc % case-insensitive?) clauses))))))

(defn- mock-collection [docs]
  #js {:insertOne (fn [doc]
                    (swap! docs conj (js->clj doc :keywordize-keys true))
                    (js/Promise.resolve #js {}))
       :insertMany (fn [arr]
                     (swap! docs into (js->clj arr :keywordize-keys true))
                     (js/Promise.resolve #js {}))
       :findOne (fn [query]
                  (let [q (js->clj query :keywordize-keys true)]
                    (js/Promise.resolve
                     (clj->js (first (filter #(matches-query? % q) @docs))))))
       :find (fn [query]
               (let [q (js->clj query :keywordize-keys true)
                     hits (filter #(matches-query? % q) @docs)]
                 #js {:toArray (fn [] (js/Promise.resolve (clj->js hits)))}))
       :updateOne (fn [query update opts]
                    (let [q (js->clj query :keywordize-keys true)
                          set-doc (js->clj (.-$set update) :keywordize-keys true)
                          set-on-insert (js->clj (.-$setOnInsert update) :keywordize-keys true)
                          upsert? (and opts (.-upsert opts))
                          matched? (boolean (some #(matches-query? % q) @docs))]
                      (if matched?
                        (swap! docs (fn [ds] (mapv #(if (matches-query? % q) (merge % set-doc) %) ds)))
                        (when upsert?
                          (swap! docs conj (merge q set-doc set-on-insert))))
                      (js/Promise.resolve
                       #js {:matchedCount (if matched? 1 0)})))
       :updateMany (fn [query update opts]
                     (let [q (js->clj query :keywordize-keys true)
                           set-doc (js->clj (.-$set update) :keywordize-keys true)
                           case-insensitive? (boolean (and opts (.-collation opts)))]
                       (swap! docs
                              (fn [ds]
                                (mapv #(if (matches-query? % q case-insensitive?)
                                         (merge % set-doc)
                                         %)
                                      ds)))
                       (js/Promise.resolve #js {})))
       :deleteMany (fn [query]
                     (let [q (js->clj query :keywordize-keys true)]
                       (swap! docs (fn [ds] (vec (remove #(matches-query? % q) ds))))
                       (js/Promise.resolve #js {})))
       :createIndex (fn [& _] (js/Promise.resolve "ok"))})

(defn- mock-lock-collection [docs]
  #js {:updateOne
       (fn [query update opts]
         ;; Use quoted property access in the native-driver double. These keys
         ;; are encoded by the extern adapter and must not depend on Closure's
         ;; treatment of property-access symbols in the test build.
         (let [id (aget query "_id")
               set-doc (js->clj (aget update "$set") :keywordize-keys true)
               set-on-insert (js->clj (aget update "$setOnInsert") :keywordize-keys true)
               matched? (boolean (some #(= id (:_id %)) @docs))]
           (if matched?
             (swap! docs
                    (fn [values]
                      (mapv #(if (= id (:_id %)) (merge % set-doc) %) values)))
             (when (and opts (aget opts "upsert"))
               (swap! docs conj (merge {:_id id} set-on-insert))))
           (js/Promise.resolve #js {:matchedCount (if matched? 1 0)})))
       :find
       (fn [_query]
         #js {:toArray (fn [] (js/Promise.resolve (clj->js @docs)))})})

(defn- mock-db []
  (let [collections (atom {})]
    #js {:collection (fn [name]
                       (let [docs (or (get @collections name)
                                      (let [d (atom [])]
                                        (swap! collections assoc name d)
                                        d))]
                         (if (= name "knoxx_policy_reconciliation_locks")
                           (mock-lock-collection docs)
                           (mock-collection docs))))}))

(defn- immediate-transaction! [f]
  (let [session #js {:withTransaction (fn [callback _options] (callback))
                     :endSession (fn [] (js/Promise.resolve nil))}
        client #js {:startSession (fn [] session)}]
    (extern-mongo/with-transaction! client f)))

(deftest ^:async upsert-actor-credential-basic-test
  (testing "upsert creates a new credential and returns PG-shaped row"
    (let [db (mock-db)
          row (await (creds/upsert-actor-credential!
                      db "u1" "o1" "github"
                      {:kind "credential"
                       :account-identifier "user@example.com"
                       :secret-json {:token "abc123"}
                       :status "active"}))]
        (is (= "u1" (:user_id row)) "user_id present")
        (is (= "o1" (:org_id row)) "org_id present")
        (is (= "github" (:provider row)) "provider present")
        (is (= "credential" (:kind row)) "kind defaults to credential")
        (is (= "active" (:status row)) "status present")
        (is (= {:token "abc123"} (:secret_json row)) "secret_json stored"))))

(deftest ^:async upsert-actor-credential-merge-secret-test
  (testing "upsert merges secret_json on conflict (PG jsonb || behavior)"
    (let [db (mock-db)]
      (await (creds/upsert-actor-credential!
              db "u1" "o1" "github"
              {:kind "credential"
               :account-identifier "user@example.com"
               :secret-json {:token "old" :refresh "r1"}
               :status "active"}))
      (let [row (await (creds/upsert-actor-credential!
                        db "u1" "o1" "github"
                        {:kind "credential"
                         :account-identifier "user@example.com"
                         :secret-json {:token "new"}
                         :status "active"}))]
        (is (= "new" (get (:secret_json row) :token)) "new key overwrites")
        (is (= "r1" (get (:secret_json row) :refresh)) "old key preserved")))))

(deftest ^:async upsert-actor-credential-idempotent-test
  (testing "repeated upsert with same data is idempotent"
    (let [db (mock-db)]
      (await (creds/upsert-actor-credential!
              db "u1" "o1" "github"
              {:kind "credential" :secret-json {} :status "active"}))
      (await (creds/upsert-actor-credential!
              db "u1" "o1" "github"
              {:kind "credential" :secret-json {} :status "active"}))
      ;; Should still only have one document
      (let [coll (.collection db "knoxx_actor_credentials")
            cursor (.find coll #js {"user_id" "u1" "org_id" "o1" "provider" "github"})
            docs (js->clj (await (.toArray cursor)) :keywordize-keys true)]
        (is (= 1 (count docs)) "only one credential doc")))))

(deftest ^:async deactivate-actor-credential-test
  (testing "deactivation makes a previously active credential unavailable"
    (let [db (mock-db)]
      (await (creds/upsert-actor-credential!
              db "u1" "o1" "local"
              {:kind "password" :secret-json {:hash "secret"} :status "active"}))
      (await (creds/deactivate-actor-credential! db "u1" "o1" "local" "password"))
      (is (nil? (await (creds/get-credential-by-user-org-provider-kind!
                        db "u1" "o1" "local" "password")))))))

(deftest ^:async find-active-bootstrap-local-password-selects-marker-test
  (let [db (mock-db)]
    (await (creds/upsert-actor-credential!
            db "u1" "ordinary-org" "local"
            {:kind "password"
             :account-identifier "admin@example.com"
             :secret-json {:hash "ordinary"}
             :status "active"}))
    (await (creds/upsert-actor-credential!
            db "u1" "bootstrap-org" "local"
            {:kind "password"
             :account-identifier "admin@example.com"
             :secret-json {:hash "bootstrap" :bootstrap-system-admin true}
             :status "active"}))
    (is (= "bootstrap-org"
           (:org_id
            (await (creds/find-active-bootstrap-local-password!
                    db "u1" "ADMIN@EXAMPLE.COM")))))))

(deftest ^:async find-active-bootstrap-local-password-rejects-duplicates-test
  (let [db (mock-db)]
    (doseq [org-id ["org-a" "org-b"]]
      (await (creds/upsert-actor-credential!
              db "u1" org-id "local"
              {:kind "password"
               :account-identifier "admin@example.com"
               :secret-json {:hash org-id :bootstrap-system-admin true}
               :status "active"})))
    (try
      (await (creds/find-active-bootstrap-local-password!
              db "u1" "admin@example.com"))
      (is false "duplicate active bootstrap passwords must fail closed")
      (catch js/Error err
        (is (re-find #"multiple active bootstrap" (.-message err)))))))

(deftest ^:async reconcile-bootstrap-local-password-serializes-and-replaces-test
  (let [db (mock-db)
        transaction-count* (atom 0)
        transaction-tail* (atom (js/Promise.resolve nil))
        with-transaction! (^:async fn [f]
                            (swap! transaction-count* inc)
                            (let [previous @transaction-tail*
                                  release* (atom nil)
                                  next-turn (js/Promise.
                                             (fn [resolve _]
                                               (reset! release* resolve)))]
                              (reset! transaction-tail* next-turn)
                              (await previous)
                              (try
                                (await (immediate-transaction! f))
                                (finally
                                  (@release* nil)))))]
    (await (creds/upsert-actor-credential!
            db "old-user" "former-org" "local"
            {:kind "password"
             :account-identifier "old@example.com"
             :secret-json {:hash "old" :bootstrap-system-admin true}
             :status "active"}))
    (await (creds/upsert-actor-credential!
            db "legacy-user" "former-org" "local"
            {:kind "password"
             :account-identifier "PI@OPEN-HAX.LOCAL"
             :secret-json {:hash "legacy"}
             :status "active"}))
    ;; Simultaneous processes with different configured identities enter the
    ;; one global transaction lane and converge to one active credential even
    ;; while the configured primary organization changes.
    (await
     (js/Promise.all
      #js [(creds/reconcile-bootstrap-local-password!
            db {:user-id "current-user"
                :org-id "current-org"
                :account-identifier "Current@Example.com"
                :previous-account-identifiers ["PI@OPEN-HAX.LOCAL"]
                :secret-json {:hash "current" :bootstrap-system-admin true}}
            with-transaction!)
           (creds/reconcile-bootstrap-local-password!
            db {:user-id "next-user"
                :org-id "current-org"
                :account-identifier "next@example.com"
                :previous-account-identifiers ["current@example.com"]
                :secret-json {:hash "next" :bootstrap-system-admin true}}
            with-transaction!)]))
    (let [credentials (js->clj
                       (await (.toArray
                               (.find (.collection db "knoxx_actor_credentials")
                                      #js {})))
                       :keywordize-keys true)
          active (filterv #(= "active" (:status %)) credentials)
          locks (js->clj
                 (await (.toArray
                         (.find (.collection db "knoxx_policy_reconciliation_locks")
                                #js {})))
                 :keywordize-keys true)]
      (is (= 2 @transaction-count*))
      (is (= ["next-user"] (mapv :user_id active)))
      (is (= {:hash "next" :bootstrap-system-admin true}
             (:secret_json (first active))))
      (is (every? #(= "inactive" (:status %))
                  (filterv #(= "former-org" (:org_id %)) credentials))
          "managed credentials in the former primary org are retired")
      (is (= 1 (count locks)) "all organizations share one intrinsic-unique lock")
      (is (= "bootstrap-system-admin-local-password" (:_id (first locks)))))))

(deftest ^:async reconcile-bootstrap-local-password-aborts-without-lock-test
  (let [credentials-mutated?* (atom false)
        lock-writes* (atom 0)
        db #js {:collection
                (fn [name]
                  (if (= name "knoxx_policy_reconciliation_locks")
                    #js {:updateOne
                         (fn [& _]
                           (swap! lock-writes* inc)
                           ;; Ensure succeeds; the transactional write then
                           ;; proves no lock was matched.
                           (js/Promise.resolve #js {:matchedCount 0}))}
                    #js {:updateMany
                         (fn [& _]
                           (reset! credentials-mutated?* true)
                           (js/Promise.resolve #js {}))}))}
        with-transaction! immediate-transaction!]
    (try
      (await (creds/reconcile-bootstrap-local-password!
              db {:user-id "current-user"
                  :org-id "org"
                  :account-identifier "current@example.com"
                  :previous-account-identifiers []
                  :secret-json nil}
              with-transaction!))
      (is false "a missing transaction lock must abort reconciliation")
      (catch js/Error err
        (is (re-find #"lock disappeared" (.-message err)))))
    (is (= 2 @lock-writes*))
    (is (false? @credentials-mutated?*)
        "credential mutations never run without a matched lock")))

(deftest ^:async reconcile-bootstrap-local-password-rolls-back-before-lockout-test
  (let [credentials* (atom [{:user_id "old-user"
                             :org_id "former-org"
                             :provider "local"
                             :kind "password"
                             :account_identifier "old@example.com"
                             :secret_json {:hash "old" :bootstrap-system-admin true}
                             :status "active"}])
        locks* (atom [])
        credentials (mock-collection credentials*)
        original-update-one (.-updateOne credentials)
        _ (set! (.-updateOne credentials)
                (fn [query update opts]
                  (if (= "current-user" (aget query "user_id"))
                    (js/Promise.reject (js/Error. "simulated upsert failure"))
                    (original-update-one query update opts))))
        db #js {:collection (fn [name]
                              (if (= name "knoxx_actor_credentials")
                                credentials
                                (mock-lock-collection locks*)))}
        with-transaction! (^:async fn [f]
                            (let [before @credentials*]
                              (try
                                (await (immediate-transaction! f))
                                (catch :default err
                                  (reset! credentials* before)
                                  (throw err)))))]
    (try
      (await (creds/reconcile-bootstrap-local-password!
              db {:user-id "current-user"
                  :org-id "current-org"
                  :account-identifier "current@example.com"
                  :previous-account-identifiers ["old@example.com"]
                  :secret-json {:hash "current" :bootstrap-system-admin true}}
              with-transaction!))
      (is false "replacement failure must propagate")
      (catch js/Error err
        (is (= "Mongo updateOne failed" (.-message err)))))
    (is (= ["old-user"]
           (mapv :user_id (filter #(= "active" (:status %)) @credentials*)))
        "transaction rollback keeps the prior administrator active")))

(deftest ^:async reconcile-bootstrap-local-password-revokes-in-one-transaction-test
  (let [db (mock-db)
        transaction-count* (atom 0)
        with-transaction! (fn [f]
                            (swap! transaction-count* inc)
                            (immediate-transaction! f))]
    (await (creds/upsert-actor-credential!
            db "current-user" "org" "local"
            {:kind "password"
             :account-identifier "current@example.com"
             :secret-json {:hash "old" :bootstrap-system-admin true}
             :status "active"}))
    (await (creds/reconcile-bootstrap-local-password!
            db {:user-id "current-user"
                :org-id "org"
                :account-identifier "current@example.com"
                :previous-account-identifiers []
                :secret-json nil}
            with-transaction!))
    (is (= 1 @transaction-count*))
    (is (nil? (await (creds/get-credential-by-user-org-provider-kind!
                      db "current-user" "org" "local" "password"))))))

(deftest ^:async reconcile-bootstrap-local-password-rejects-blank-id-test
  (let [collection-called?* (atom false)
        db #js {:collection
                (fn [_]
                  (reset! collection-called?* true)
                  #js {})}]
    (try
      (await (creds/reconcile-bootstrap-local-password!
              db {:user-id "  "
                  :org-id "org"
                  :account-identifier "admin@example.com"
                  :secret-json {:hash "current"}}
              (fn [_]
                (is false "transaction must not start for invalid input")
                (js/Promise.resolve nil))))
      (is false "blank current user id must be rejected")
      (catch js/Error err
        (is (re-find #"user-id is required" (.-message err)))))
    (is (false? @collection-called?*) "validation happens before database access")))

(deftest ^:async credential-doc->row-test
  (testing "credential-doc->row renames credential_id to :id and drops Mongo fields"
    (let [r (creds/credential-doc->row {:credential_id "c1" :_id "x" :user_id "u1"
                                        :provider "github" :kind "credential"})]
      (is (= "c1" (:id r)))
      (is (nil? (:credential_id r)))
      (is (nil? (:_id r)))
      (is (= "u1" (:user_id r)))
      (is (nil? (creds/credential-doc->row nil)) "nil-safe"))))

(deftest ^:async upsert-generates-credential-id-test
  (testing "upsert generates a credential_id on first insert"
    (let [db (mock-db)
          row (await (creds/upsert-actor-credential!
                      db "u1" "o1" "github"
                      {:kind "credential" :secret-json {} :status "active"}))]
      (is (some? (:id row)) "id is populated")
      (is (string? (:id row)) "id is a string UUID"))))

(deftest ^:async upsert-coalesces-account-identifier-test
  (testing "upsert preserves existing account_identifier when incoming is nil"
    (let [db (mock-db)]
      (await (creds/upsert-actor-credential!
              db "u1" "o1" "github"
              {:kind "credential"
               :account-identifier "original@example.com"
               :secret-json {} :status "active"}))
      (let [row (await (creds/upsert-actor-credential!
                        db "u1" "o1" "github"
                        {:kind "credential"
                         :account-identifier nil
                         :secret-json {:token "new"} :status "active"}))]
        (is (= "original@example.com" (:account_identifier row))
            "existing account_identifier preserved when incoming is nil")))))

(deftest ^:async credential-row->response-test
  (testing "credential-row->response produces camelCase shape matching PG adapter"
    (let [r (creds/credential-row->response {:id "c1" :actor_id "a1" :user_id "u1"
                                             :org_id "o1" :org_slug "acme"
                                             :provider "github" :kind "credential"
                                             :account_identifier "user@example.com"
                                             :status "active"
                                             :secret_json {:token "abc"}
                                             :created_at "2025-01-01" :updated_at "2025-01-02"})]
      (is (= "c1" (:id r)))
      (is (= "a1" (:actorId r)))
      (is (= "u1" (:userId r)))
      (is (= "o1" (:orgId r)))
      (is (= "acme" (:orgSlug r)))
      (is (= "github" (:provider r)))
      (is (= "credential" (:kind r)))
      (is (= "user@example.com" (:accountIdentifier r)))
      (is (= "active" (:status r)))
      (is (= {:token "abc"} (:secretJson r)))
      (is (= "2025-01-01" (:createdAt r)))
      (is (= "2025-01-02" (:updatedAt r)))
      (is (nil? (creds/credential-row->response nil)) "nil-safe"))))

(defn- recording-index-db
  "Mock db whose collections record every createIndex (keys, opts) call."
  [calls*]
  #js {:collection
       (fn [coll-name]
         #js {:createIndex
              (fn [keys opts]
                (swap! calls* conj {:collection coll-name
                                    :keys (js->clj keys)
                                    :opts (js->clj opts :keywordize-keys true)})
                (js/Promise.resolve "ok"))})})

(deftest ^:async setup-indexes-spec-test
  (testing "index specs carry the uniques and no partialFilterExpression"
    (let [calls* (atom [])]
      (await (creds/setup-indexes! (recording-index-db calls*)))
      (let [calls @calls*
            spec-str (pr-str calls)]
        (is (not (re-find #"partialFilterExpression" spec-str))
            "no partial indexes anywhere")
        (is (some #(and (= (:collection %) creds/ACTOR_CREDENTIALS_COLLECTION)
                        (= (:keys %) {"user_id" 1 "org_id" 1 "provider" 1 "kind" 1})
                        (true? (get-in % [:opts :unique])))
                  calls)
            "compound unique on (user_id, org_id, provider, kind)")))))
