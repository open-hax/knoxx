(ns knoxx.backend.infra.stores.mongo-policy-roles-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-policy-roles :as roles]))

;; Mock built on the directory-test matches-query? pattern, extended for the
;; query/write shapes this store uses: $in / $exists / $type / $or operators,
;; insertMany, deleteMany, and the partial-unique createIndex (a no-op here).

(declare matches-query?)

(defn- matches-clause? [actual v]
  (cond
    (and (map? v) (contains? v :$in)) (contains? (set (:$in v)) actual)
    (and (map? v) (contains? v :$exists)) (= (:$exists v) (some? actual))
    (and (map? v) (contains? v :$type)) (and (some? actual) (string? actual))
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
                          on-insert-doc (js->clj (aget update "$setOnInsert") :keywordize-keys true)
                          upsert? (boolean (and opts (aget opts "upsert")))
                          hit? (some #(matches-query? % q) @docs)]
                      (cond
                        hit?
                        (swap! docs (fn [ds] (mapv #(if (matches-query? % q) (merge % set-doc) %) ds)))

                        upsert?
                        (swap! docs conj (merge q on-insert-doc set-doc)))
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

(deftest ^:async role-ensure-idempotency-test
  (testing "ensure-role! is idempotent on (org,slug); id stable, name updates"
    (let [db (mock-db)
          r1 (await (roles/ensure-role! db {:org-id "o1" :name "Dev" :slug "developer"
                                            :scope-kind "org" :built-in false :system-managed false}))
          r2 (await (roles/ensure-role! db {:org-id "o1" :name "Developer" :slug "developer"
                                            :scope-kind "org" :built-in true :system-managed true}))]
      (is (some? (:id r1)) "doc->row presents :id")
      (is (= (:id r1) (:id r2)) "same (org,slug) keeps the same id")
      (is (= "Developer" (:name r2)) "name updated on conflict")
      (is (true? (:built_in r2)) "built_in updated")
      (is (true? (:system_managed r2)) "system_managed updated")
      (is (= "o1" (:org_id r2)) "org_id preserved as PG column")
      (is (nil? (:role_id r2)) "row-shape adapter hides role_id")
      (is (nil? (:_id r2)) "row-shape adapter hides _id")
      (is (= 1 (count (await (roles/list-roles! db {:org-id "o1"})))) "no duplicate row"))))

(deftest ^:async role-slug-canonicalisation-test
  (testing "mixed-case slug writes are findable: slug lower-cased on write AND read"
    (let [db (mock-db)
          r (await (roles/ensure-role! db {:org-id "o1" :name "Admin" :slug "Org-Admin"
                                           :scope-kind "org"}))]
      (is (= "org-admin" (:slug r)) "stored lower-case")
      (is (= (:id r) (:id (await (roles/find-role db {:org-id "o1" :slug "ORG-ADMIN"}))))
          "lower-case lookup finds mixed-case write")
      (let [again (await (roles/ensure-role! db {:org-id "o1" :name "Admin2" :slug "ORG-ADMIN"
                                                 :scope-kind "org"}))]
        (is (= (:id r) (:id again)) "case variants upsert the same role")
        (is (= 1 (count (await (roles/list-roles! db {:org-id "o1"}))))
            "no case duplicates")))))

(deftest ^:async role-platform-vs-org-scope-test
  (testing "find-role distinguishes platform scope (nil org) from org scope"
    (let [db (mock-db)
          plat (await (roles/ensure-role! db {:org-id nil :name "Sysadmin" :slug "system_admin"
                                              :scope-kind "platform" :built-in true}))
          org (await (roles/ensure-role! db {:org-id "o1" :name "Sysadmin" :slug "system_admin"
                                             :scope-kind "org"}))]
      (is (not= (:id plat) (:id org)) "same slug in different scopes are distinct rows")
      (is (nil? (:org_id plat)) "platform role has no org_id")
      (is (= (:id plat) (:id (await (roles/find-role db {:org-id nil :slug "system_admin"})))))
      (is (= (:id org) (:id (await (roles/find-role db {:org-id "o1" :slug "system_admin"}))))))))

(deftest ^:async role-list-ordering-test
  (testing "list-roles! orders built_in desc then name (q-roles/list-all)"
    (let [db (mock-db)]
      (await (roles/ensure-role! db {:org-id "o1" :name "Zeta" :slug "zeta" :built-in false}))
      (await (roles/ensure-role! db {:org-id "o1" :name "Alpha" :slug "alpha" :built-in false}))
      (await (roles/ensure-role! db {:org-id "o1" :name "Builtin" :slug "builtin" :built-in true}))
      (is (= ["Builtin" "Alpha" "Zeta"]
             (mapv :name (await (roles/list-roles! db {:org-id "o1"})))
             ) "built-in first, then alphabetical"))))

(deftest ^:async role-list-org-scope-test
  (testing "list-roles! scopes to org when org-id given, all when not"
    (let [db (mock-db)]
      (await (roles/ensure-role! db {:org-id "o1" :name "A" :slug "a"}))
      (await (roles/ensure-role! db {:org-id "o2" :name "B" :slug "b"}))
      (await (roles/ensure-role! db {:org-id nil :name "P" :slug "p" :scope-kind "platform"}))
      (is (= 1 (count (await (roles/list-roles! db {:org-id "o1"})))) "scoped")
      (is (= 3 (count (await (roles/list-roles! db {})))) "all when no org-id"))))

(deftest ^:async role-by-ids-and-slugs-test
  (testing "list-roles-by-ids! orders by name; list-roles-by-slugs! resolves scope"
    (let [db (mock-db)
          a (await (roles/ensure-role! db {:org-id "o1" :name "Bravo" :slug "bravo"}))
          b (await (roles/ensure-role! db {:org-id "o1" :name "Alpha" :slug "alpha"}))
          p (await (roles/ensure-role! db {:org-id nil :name "Plat" :slug "platrole"
                                           :scope-kind "platform"}))]
      (is (= ["Alpha" "Bravo"]
             (mapv :name (await (roles/list-roles-by-ids! db [(:id a) (:id b)]))))
          "ordered by name")
      (is (= [] (await (roles/list-roles-by-ids! db []))) "empty in, empty out")
      (let [resolved (await (roles/list-roles-by-slugs! db ["ALPHA" "platrole"] "o1"))]
        (is (= #{(:id b) (:id p)} (set (map :id resolved)))
            "case-insensitive; platform + org scope both visible"))
      (is (= [] (await (roles/list-roles-by-slugs! db ["alpha"] "other-org")))
          "org-scoped role invisible to a different org"))))

(deftest ^:async permission-replace-set-test
  (testing "set-role-permissions! replaces (not appends); empty set clears"
    (let [db (mock-db)
          role (await (roles/ensure-role! db {:org-id "o1" :name "R" :slug "r"}))
          rid (:id role)]
      (await (roles/set-role-permissions! db rid ["docs.read" "docs.write" "docs.read"]))
      (is (= ["docs.read" "docs.write"]
             (mapv :code (await (roles/permissions-for-roles! db [rid]))))
          "deduped + ordered by code")
      (await (roles/set-role-permissions! db rid ["files.read"]))
      (is (= ["files.read"]
             (mapv :code (await (roles/permissions-for-roles! db [rid]))))
          "replaced, not appended")
      (is (= rid (:role_id (first (await (roles/permissions-for-roles! db [rid])))))
          "row carries :role_id for the grouping reducer")
      (await (roles/set-role-permissions! db rid []))
      (is (= [] (await (roles/permissions-for-roles! db [rid]))) "empty set clears"))))

(deftest ^:async permissions-for-roles-multi-test
  (testing "permissions-for-roles! returns rows for multiple roles, code-ordered"
    (let [db (mock-db)]
      (await (roles/set-role-permissions! db "ra" ["b.x" "a.y"]))
      (await (roles/set-role-permissions! db "rb" ["c.z"]))
      (let [rows (await (roles/permissions-for-roles! db ["ra" "rb"]))]
        (is (= 3 (count rows)))
        (is (= ["a.y" "b.x" "c.z"] (mapv :code rows)) "global order by code")
        (is (= #{"ra" "rb"} (set (map :role_id rows)))))
      (is (= [] (await (roles/permissions-for-roles! db []))) "empty in, empty out"))))

(deftest ^:async membership-role-replace-set-test
  (testing "set-membership-roles! replace-set; replace? false appends"
    (let [db (mock-db)]
      (await (roles/set-membership-roles! db "m1" true ["r1" "r2" "r1"]))
      (is (= #{"r1" "r2"} (set (await (roles/role-ids-for-membership! db "m1"))))
          "deduped insert")
      (await (roles/set-membership-roles! db "m1" true ["r3"]))
      (is (= #{"r3"} (set (await (roles/role-ids-for-membership! db "m1"))))
          "replace? true clears then inserts")
      (await (roles/set-membership-roles! db "m1" false ["r4"]))
      (is (= #{"r3" "r4"} (set (await (roles/role-ids-for-membership! db "m1"))))
          "replace? false appends without clearing")
      (await (roles/set-membership-roles! db "m1" true []))
      (is (= [] (await (roles/role-ids-for-membership! db "m1"))) "empty replace clears"))))

(deftest ^:async roles-for-memberships-join-test
  (testing "roles-for-memberships! joins link rows to roles, ordered by name"
    (let [db (mock-db)
          ra (await (roles/ensure-role! db {:org-id "o1" :name "Zed" :slug "zed"}))
          rb (await (roles/ensure-role! db {:org-id "o1" :name "Ann" :slug "ann"}))]
      (await (roles/set-membership-roles! db "m1" true [(:id ra) (:id rb)]))
      (await (roles/set-membership-roles! db "m2" true [(:id ra)]))
      (let [rows (await (roles/roles-for-memberships! db ["m1" "m2"]))
            m1-rows (filter #(= "m1" (:membership_id %)) rows)]
        (is (= 3 (count rows)) "one row per (membership, role) link")
        (is (= ["Ann" "Zed"] (mapv :name m1-rows)) "ordered by role name")
        (is (= #{:membership_id :role_id :slug :name :scope_kind :org_id}
               (set (keys (first rows))))
            "join projection matches grouped-membership-roles reducer keys")
        (is (= "o1" (:org_id (first m1-rows))) "carries role org_id"))
      (is (= [] (await (roles/roles-for-memberships! db []))) "empty in, empty out"))))

(deftest ^:async role-doc->row-id-parity-test
  (testing "role-doc->row presents :id, hides role_id + _id; nil-safe"
    (let [r (roles/role-doc->row {:role_id "rid" :_id "x" :slug "s" :name "n" :org_id "o"})]
      (is (= "rid" (:id r)))
      (is (nil? (:role_id r)))
      (is (nil? (:_id r)))
      (is (= "o" (:org_id r)) "non-id columns untouched")
      (is (nil? (roles/role-doc->row nil)) "nil-safe"))))

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
  (testing "index specs avoid partialFilterExpression footguns and carry the uniques"
    (let [calls* (atom [])]
      (await (roles/setup-indexes! (recording-index-db calls*)))
      (let [calls @calls*
            spec-str (pr-str calls)]
        ;; partialFilterExpression rejects {$exists false} (server error 67,
        ;; observed live 2026-06-06) — the store must not use it at all.
        (is (not (re-find #"partialFilterExpression" spec-str))
            "no partial indexes anywhere")
        (is (some #(and (= (:collection %) roles/ROLES_COLLECTION)
                        (= (:keys %) {"org_id" 1 "slug" 1})
                        (true? (get-in % [:opts :unique])))
                  calls)
            "compound unique (org_id, slug) covers platform+org scopes")
        (is (some #(and (= (:collection %) roles/ROLE_PERMISSIONS_COLLECTION)
                        (true? (get-in % [:opts :unique])))
                  calls)
            "role_permissions unique present")
        (is (some #(and (= (:collection %) roles/MEMBERSHIP_ROLES_COLLECTION)
                        (true? (get-in % [:opts :unique])))
                  calls)
            "membership_roles unique present")))))
