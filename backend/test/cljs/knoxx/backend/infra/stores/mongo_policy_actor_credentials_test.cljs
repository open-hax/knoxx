(ns knoxx.backend.infra.stores.mongo-policy-actor-credentials-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as creds]))

;; Mock built on the same pattern as mongo-policy-tools-test.

(declare matches-query?)

(defn- matches-clause? [actual v]
  (cond
    (and (map? v) (contains? v :$in)) (contains? (set (:$in v)) actual)
    (and (map? v) (contains? v :$exists)) (= (:$exists v) (some? actual))
    (and (map? v) (contains? v :$ne)) (not= actual (:$ne v))
    (map? v) (= actual v)
    :else (= actual v)))

(defn- matches-query? [doc query]
  (cond
    (contains? query :$or) (some #(matches-query? doc %) (:$or query))
    :else (every? (fn [[k v]] (matches-clause? (get doc k) v)) query)))

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
                          upsert? (and opts (.-upsert opts))]
                      (if (some #(matches-query? % q) @docs)
                        (swap! docs (fn [ds] (mapv #(if (matches-query? % q) (merge % set-doc) %) ds)))
                        (when upsert?
                          (swap! docs conj (merge q set-doc set-on-insert))))
                      (js/Promise.resolve #js {})))
       :deleteMany (fn [query]
                     (let [q (js->clj query :keywordize-keys true)]
                       (swap! docs (fn [ds] (vec (remove #(matches-query? % q) ds))))
                       (js/Promise.resolve #js {})))
       :createIndex (fn [& _] (js/Promise.resolve "ok"))})

(defn- mock-db []
  (let [collections (atom {})]
    #js {:collection (fn [name]
                       (let [docs (or (get @collections name)
                                      (let [d (atom [])]
                                        (swap! collections assoc name d)
                                        d))]
                         (mock-collection docs)))}))

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

(deftest ^:async deactivate-other-bootstrap-local-passwords-test
  (let [captured* (atom nil)
        db #js {:collection
                (fn [name]
                  (case name
                    "knoxx_actor_credentials"
                    #js {:updateMany
                         (fn [query update]
                           (reset! captured*
                                   {:query (js->clj query :keywordize-keys true)
                                    :update (js->clj update :keywordize-keys true)})
                           (js/Promise.resolve #js {}))}))}]
    (await (creds/deactivate-other-bootstrap-local-passwords!
            db "current-user" ["PI@OPEN-HAX.LOCAL" "  "]))
    (is (= {:provider "local"
            :kind "password"
            :status "active"
            :user_id {:$ne "current-user"}
            :$or [{:secret_json.bootstrap-system-admin true}
                  {:account_identifier {:$in ["pi@open-hax.local"]}}]}
           (:query @captured*)))
    (is (= "inactive" (get-in @captured* [:update :$set :status])))))

(deftest ^:async deactivate-other-bootstrap-local-passwords-rejects-blank-id-test
  (let [collection-called?* (atom false)
        db #js {:collection
                (fn [_]
                  (reset! collection-called?* true)
                  #js {})}]
    (try
      (await (creds/deactivate-other-bootstrap-local-passwords! db "  "))
      (is false "blank current user id must be rejected")
      (catch js/Error err
        (is (re-find #"current-user-id is required" (.-message err)))))
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
