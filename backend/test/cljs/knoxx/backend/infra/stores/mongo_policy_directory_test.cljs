(ns knoxx.backend.infra.stores.mongo-policy-directory-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-policy-directory :as dir]))

;; Mock built on the slice-1 matches-query? pattern but extended so
;; findOneAndUpdate applies $set + $setOnInsert (returnDocument "after") and
;; updateOne/updateMany mutate matched docs — the behaviours the directory
;; upserts depend on.

(defn- matches-query? [doc query]
  (every? (fn [[k v]]
            (let [actual (get doc k)]
              (cond
                (and (map? v) (contains? v :$ne)) (not= actual (:$ne v))
                (map? v) (= actual v)
                :else (= actual v))))
          query))

(defn- apply-set! [docs q set-doc]
  (swap! docs (fn [ds] (mapv #(if (matches-query? % q) (merge % set-doc) %) ds)))
  (js/Promise.resolve #js {}))

(defn- mock-pipeline-update-many!
  "Support the one pipeline shape the store uses:
   [{$set {field {$eq [\"$src\" value]}}}] — field := (= doc.src value)."
  [docs update]
  (let [stage (js->clj (aget update 0) :keywordize-keys true)
        set-spec (get stage (keyword "$set"))]
    (doseq [[field expr] set-spec]
      (let [[lhs rhs] (get expr (keyword "$eq"))
            src (keyword (subs (str lhs) 1))] ; "$slug" -> :slug
        (swap! docs (fn [ds]
                      (mapv #(assoc % field (= (get % src) rhs)) ds)))))
    (js/Promise.resolve #js {})))

(defn- mock-find-one-and-update [docs]
  (fn [query update _opts]
    (let [q (js->clj query :keywordize-keys true)
          set-doc (js->clj (.-$set update) :keywordize-keys true)
          soi (js->clj (.-$setOnInsert update) :keywordize-keys true)
          existing (first (filter #(matches-query? % q) @docs))
          merged (merge (or existing q) soi set-doc)]
      (if existing
        (swap! docs (fn [ds] (mapv #(if (matches-query? % q) (merge % set-doc) %) ds)))
        (swap! docs conj merged))
      (js/Promise.resolve (clj->js (if existing (merge existing set-doc) merged))))))

(defn- mock-collection [docs]
  #js {:insertOne (fn [doc]
                    (swap! docs conj (js->clj doc :keywordize-keys true))
                    (js/Promise.resolve #js {}))
       :findOne (fn [query]
                  (let [q (js->clj query :keywordize-keys true)]
                    (js/Promise.resolve
                     (clj->js (first (filter #(matches-query? % q) @docs))))))
       :find (fn [query]
               (let [q (js->clj query :keywordize-keys true)
                     hits (filter #(matches-query? % q) @docs)]
                 #js {:toArray (fn [] (js/Promise.resolve (clj->js hits)))}))
       :findOneAndUpdate (mock-find-one-and-update docs)
       :updateOne (fn [query update _opts]
                    (apply-set! docs (js->clj query :keywordize-keys true)
                                (js->clj (.-$set update) :keywordize-keys true)))
       :updateMany (fn [query update]
                     (if (array? update)
                       (mock-pipeline-update-many! docs update)
                       (apply-set! docs (js->clj query :keywordize-keys true)
                                   (js->clj (.-$set update) :keywordize-keys true))))
       :createIndex (fn [_keys _opts] (js/Promise.resolve "ok"))})

(defn- mock-db []
  (let [collections (atom {})]
    #js {:collection (fn [name]
                       (let [docs (or (get @collections name)
                                      (let [d (atom [])]
                                        (swap! collections assoc name d)
                                        d))]
                         (mock-collection docs)))}))

(deftest ^:async org-upsert-by-slug-idempotency-test
  (testing "ensure-primary-org! is idempotent on slug; id stable, name updates"
    (let [db (mock-db)
          first-org (await (dir/ensure-primary-org! db {:slug "open-hax" :name "Open Hax" :kind "platform_owner"}))
          again (await (dir/ensure-primary-org! db {:slug "open-hax" :name "Open Hax 2" :kind "platform_owner"}))]
      (is (some? (:id first-org)) "doc->row presents :id")
      (is (= (:id first-org) (:id again)) "same slug keeps the same id")
      (is (= "Open Hax 2" (:name again)) "name updated on conflict")
      (is (true? (:is_primary again)) "is_primary forced true")
      (is (= 1 (count (await (dir/list-orgs! db)))) "no duplicate row")
      (is (nil? (:org_id again)) "row-shape adapter hides org_id")
      (is (nil? (:_id again)) "row-shape adapter hides _id"))))

(deftest ^:async org-single-primary-test
  (testing "ensure-primary-org! clears is_primary on other orgs"
    (let [db (mock-db)]
      (await (dir/ensure-primary-org! db {:slug "a" :name "A" :kind "platform_owner"}))
      (await (dir/ensure-primary-org! db {:slug "b" :name "B" :kind "platform_owner"}))
      (let [orgs (await (dir/list-orgs! db))
            primaries (filter :is_primary orgs)]
        (is (= 1 (count primaries)) "exactly one primary")
        (is (= "b" (:slug (first primaries))) "latest wins")))))

(deftest ^:async create-org-and-find-by-slug-test
  (testing "create-org! inserts non-primary; find-org-by-slug is case-insensitive"
    (let [db (mock-db)
          org (await (dir/create-org! db {:slug "acme" :name "Acme"}))]
      (is (false? (:is_primary org)))
      (is (= "customer" (:kind org)) "kind default")
      (is (= "active" (:status org)) "status default")
      (is (= (:id org) (:id (await (dir/find-org-by-slug db "ACME")))) "case-insensitive lookup")
      (is (nil? (await (dir/find-org-by-slug db "missing")))))))

(deftest ^:async slug-canonicalisation-test
  (testing "mixed-case writes are findable: slug lower-cased on write AND read"
    (let [db (mock-db)
          org (await (dir/create-org! db {:slug "ACME" :name "Acme"}))]
      (is (= "acme" (:slug org)) "stored lower-case")
      (is (= (:id org) (:id (await (dir/find-org-by-slug db "acme"))))
          "lower-case lookup finds mixed-case write"))
    (let [db (mock-db)
          a (await (dir/ensure-primary-org! db {:slug "Open-Hax" :name "OH" :kind "platform_owner"}))
          b (await (dir/ensure-primary-org! db {:slug "open-hax" :name "OH" :kind "platform_owner"}))]
      (is (= (:id a) (:id b)) "case variants upsert the same org")
      (is (= 1 (count (await (dir/list-orgs! db)))) "no case duplicates"))))

(deftest ^:async membership-ordering-test
  (testing "list-memberships! orders by created_at ascending (numeric Date sort)"
    (let [db (mock-db)
          coll (.collection db dir/MEMBERSHIPS_COLLECTION)]
      (await (.insertOne coll (clj->js {:membership_id "m2" :org_id "o1" :user_id "u2"
                                        :created_at (js/Date. 2000)})))
      (await (.insertOne coll (clj->js {:membership_id "m1" :org_id "o1" :user_id "u1"
                                        :created_at (js/Date. 1000)})))
      (is (= ["m1" "m2"] (mapv :id (await (dir/list-memberships! db {:org-id "o1"}))))))))

(deftest ^:async user-upsert-by-email-update-test
  (testing "create-user! upserts by lower-cased email, updating mutable fields"
    (let [db (mock-db)
          u1 (await (dir/create-user! db {:email "A@B.C" :display-name "A" :auth-provider "local"}))
          u2 (await (dir/create-user! db {:email "a@b.c" :display-name "A2" :auth-provider "github" :status "suspended"}))]
      (is (= "a@b.c" (:email u1)) "email lower-cased")
      (is (some? (:id u1)) "doc->row presents :id")
      (is (= (:id u1) (:id u2)) "same email keeps same id")
      (is (= "A2" (:display_name u2)) "display_name updated")
      (is (= "github" (:auth_provider u2)) "auth_provider updated")
      (is (= "suspended" (:status u2)) "status updated")
      (is (= 1 (count (await (dir/list-users! db {})))) "no duplicate user")
      (is (nil? (:user_id u2)) "row-shape adapter hides user_id"))))

(deftest ^:async list-users-org-filter-test
  (testing "list-users! scopes to members of an org and orders by display_name"
    (let [db (mock-db)
          ua (await (dir/create-user! db {:email "z@x.io" :display-name "Zed" :auth-provider "local"}))
          ub (await (dir/create-user! db {:email "y@x.io" :display-name "Ann" :auth-provider "local"}))
          org (await (dir/create-org! db {:slug "o" :name "O"}))]
      (await (dir/upsert-membership! db {:user-id (:id ua) :org-id (:id org)}))
      (let [all (await (dir/list-users! db {}))
            scoped (await (dir/list-users! db {:org-id (:id org)}))]
        (is (= 2 (count all)))
        (is (= ["Ann" "Zed"] (mapv :display_name all)) "ordered by display_name")
        (is (= [(:id ua)] (mapv :id scoped)) "only org members")
        (is (not (some #{(:id ub)} (map :id scoped))) "non-member excluded")))))

(deftest ^:async membership-compound-upsert-test
  (testing "upsert-membership! is idempotent on (user_id, org_id)"
    (let [db (mock-db)
          m1 (await (dir/upsert-membership! db {:user-id "u1" :org-id "o1" :status "active" :is-default true}))
          m2 (await (dir/upsert-membership! db {:user-id "u1" :org-id "o1" :status "suspended" :is-default false}))]
      (is (some? (:id m1)) "doc->row presents :id")
      (is (= (:id m1) (:id m2)) "compound key keeps same id")
      (is (= "suspended" (:status m2)) "status updated")
      (is (= false (:is_default m2)) "is_default updated")
      (is (= "u1" (:user_id m2)) "user_id preserved as PG column")
      (is (nil? (:actor_id m1)) "actor_id null on insert, filled later")
      (is (= 1 (count (await (dir/list-memberships! db {:org-id "o1"})))) "no duplicate"))))

(deftest ^:async get-membership-and-list-test
  (testing "get-membership! by id and list-memberships! by org"
    (let [db (mock-db)
          m (await (dir/upsert-membership! db {:user-id "u1" :org-id "o1"}))]
      (await (dir/upsert-membership! db {:user-id "u2" :org-id "o1"}))
      (await (dir/upsert-membership! db {:user-id "u3" :org-id "o2"}))
      (is (= (:id m) (:id (await (dir/get-membership! db (:id m))))) ":id round-trips")
      (is (nil? (await (dir/get-membership! db "nope"))))
      (is (= 2 (count (await (dir/list-memberships! db {:org-id "o1"})))) "scoped by org")
      (is (thrown? js/Error (await (dir/list-memberships! db {:org-id ""})))))))

(deftest ^:async set-membership-actor-id-test
  (testing "set-membership-actor-id! sets actor_id, defaulting blanks"
    (let [db (mock-db)
          m (await (dir/upsert-membership! db {:user-id "u1" :org-id "o1"}))]
      (is (= "system_admin" (await (dir/set-membership-actor-id! db (:id m) "system_admin"))))
      (is (= "system_admin" (:actor_id (await (dir/get-membership! db (:id m))))))
      (is (= "workspace_user" (await (dir/set-membership-actor-id! db (:id m) "  "))) "blank defaults")
      (is (= "workspace_user" (:actor_id (await (dir/get-membership! db (:id m)))))))))

(deftest ^:async doc->row-id-parity-test
  (testing "every adapter presents :id and hides the stored {table}_id + _id"
    (let [org (dir/org-doc->row {:org_id "oid" :_id "x" :slug "s" :name "n"})
          user (dir/user-doc->row {:user_id "uid" :_id "x" :email "e"})
          ms (dir/membership-doc->row {:membership_id "mid" :_id "x" :user_id "u"})]
      (is (= "oid" (:id org))) (is (nil? (:org_id org))) (is (nil? (:_id org)))
      (is (= "uid" (:id user))) (is (nil? (:user_id user)))
      (is (= "mid" (:id ms))) (is (nil? (:membership_id ms)))
      (is (= "u" (:user_id ms)) "non-id columns are untouched")
      (is (nil? (dir/org-doc->row nil)) "nil-safe"))))
