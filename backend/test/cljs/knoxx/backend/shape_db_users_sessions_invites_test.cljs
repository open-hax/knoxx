(ns knoxx.backend.shape-db-users-sessions-invites-test
  (:require [cljs.test :refer [deftest is testing]]
            [honey.sql :as sql]
            [knoxx.backend.shape.db.invites :as q-invites]
            [knoxx.backend.shape.db.memberships :as q-memberships]
            [knoxx.backend.shape.db.roles :as q-roles]
            [knoxx.backend.shape.db.sessions :as q-sessions]
            [knoxx.backend.shape.db.users :as q-users]))

(defn- formatted
  [query]
  (sql/format query {:numbered true}))

(defn- sql-string
  [query]
  (first (formatted query)))

(defn- sql-params
  [query]
  (vec (rest (formatted query))))

(deftest user-query-builders-preserve-normalization-and-casts
  (testing "email lookup normalizes both sides and limits candidates"
    (let [query (q-users/by-email "Person@Example.COM")
          sql-str (sql-string query)]
      (is (re-find #"(?i)from users" sql-str))
      (is (re-find #"(?i)lower\(email\) = lower\(\$1\)" sql-str))
      (is (re-find #"(?i)limit \$2" sql-str))
      (is (= ["Person@Example.COM" 1] (sql-params query)))))
  (testing "partial user updates only set requested mutable fields"
    (let [query (q-users/update-user "user-1" {:email "new@example.test"
                                                :status "active"})
          set-map (:set query)]
      (is (= {:updated_at [:now]
              :email [:lower "new@example.test"]
              :status "active"}
             set-map))
      (is (= [:= :id [:cast "user-1" :uuid]] (:where query)))))
  (testing "membership and credential fanout queries cast every user id"
    (let [memberships (q-users/memberships-for-users ["u1" "u2"] "org-1")
          credentials (q-users/credentials-for-users ["u1"] "org-1")]
      (is (= [:in :m.user_id [[:cast "u1" :uuid] [:cast "u2" :uuid]]]
             (get-in memberships [:where 1])))
      (is (= [:= :m.org_id [:cast "org-1" :uuid]]
             (get-in memberships [:where 2])))
      (is (= [:in :user_id [[:cast "u1" :uuid]]]
             (get-in credentials [:where 1]))))))

(deftest session-query-builders-preserve-token-and-expiry-contracts
  (testing "session insert casts relational ids and returns inserted row"
    (let [query (q-sessions/insert {:user-id "u1"
                                    :membership-id "m1"
                                    :org-id "o1"
                                    :token-hash "hash"
                                    :token-prefix "pref"
                                    :salt "salt"
                                    :email "u@example.test"
                                    :display-name "User"
                                    :auth-provider "local"
                                    :external-subject "sub"
                                    :ip-address "127.0.0.1"
                                    :user-agent "test"
                                    :expires-at "tomorrow"})
          row (first (:values query))]
      (is (= [:cast "u1" :uuid] (:user_id row)))
      (is (= [:cast "m1" :uuid] (:membership_id row)))
      (is (= [:cast "o1" :uuid] (:org_id row)))
      (is (= :* (first (:returning query))))))
  (testing "prefix lookup and expiry cleanup constrain live sessions"
    (let [by-prefix (q-sessions/by-prefix "abc")
          delete-expired (q-sessions/delete-expired)]
      (is (= [:and [:= :token_prefix "abc"] [:> :expires_at [:now]]]
             (:where by-prefix)))
      (is (= 50 (:limit by-prefix)))
      (is (= [:< :expires_at [:now]] (:where delete-expired))))))

(deftest invite-query-builders-preserve-pending-and-redemption-state
  (testing "invite insert lowercases email and tolerates system-created invites"
    (let [query (q-invites/insert {:org-id "org-1"
                                   :code "ABC"
                                   :email "Person@Example.COM"
                                   :role-slugs-json "[\"member\"]"
                                   :expires-at "later"})
          row (first (:values query))]
      (is (= [:cast "org-1" :uuid] (:org_id row)))
      (is (= [:lower "Person@Example.COM"] (:email row)))
      (is (nil? (:inviter_membership_id row)))
      (is (= [:raw "[\"member\"]"] (:role_slugs row)))
      (is (= "pending" (:status row)))))
  (testing "pending lookup enforces code, status, expiry, and one result"
    (let [query (q-invites/pending-by-code "ABC")]
      (is (= [:and [:= :code "ABC"] [:= :status "pending"] [:> :expires_at [:now]]]
             (:where query)))
      (is (= 1 (:limit query)))))
  (testing "redeem marks status and timestamp before returning row"
    (let [query (q-invites/redeem "invite-1")]
      (is (= {:status "redeemed" :redeemed_at [:now]} (:set query)))
      (is (= [:= :id [:cast "invite-1" :uuid]] (:where query))))))

(deftest user-session-and-invite-query-builders-cover-list-and-maintenance-paths
  (testing "user lookup and list queries keep stable ordering and conflict handling"
    (is (= [:= :id [:cast "user-1" :uuid]]
           (:where (q-users/by-id "user-1"))))
    (is (= [:display_name :email] (:order-by (q-users/list-all))))
    (is (= [:= :m.org_id [:cast "org-1" :uuid]]
           (:where (q-users/list-by-org "org-1"))))
    (is (= [:in :m.user_id [[:cast "u1" :uuid] [:cast "u2" :uuid]]]
           (:where (q-users/all-memberships-for-users ["u1" "u2"]))))
    (let [upsert-sql (sql-string (q-users/upsert {:email "A@Example.test"
                                                  :display-name "A"
                                                  :auth-provider "local"
                                                  :external-subject "sub"
                                                  :status "active"}))]
      (is (re-find #"(?i)on conflict" upsert-sql))
      (is (re-find #"EXCLUDED\.display_name" upsert-sql))))
  (testing "session maintenance queries touch, delete, and scan active sessions"
    (is (= [:> :expires_at [:now]] (:where (q-sessions/all-active))))
    (is (= {:last_seen_at [:now]} (:set (q-sessions/touch "session-1"))))
    (is (= [:= :id [:cast "session-1" :uuid]]
           (:where (q-sessions/delete-by-id "session-1")))))
  (testing "invite list queries scope by org and optional status"
    (is (= [:= :org_id [:cast "org-1" :uuid]]
           (:where (q-invites/list-by-org "org-1"))))
    (is (= [:and [:= :org_id [:cast "org-1" :uuid]] [:= :status "pending"]]
           (:where (q-invites/list-by-org-and-status "org-1" "pending"))))))

(deftest role-and-membership-query-builders-cover-role-permission-and-link-paths
  (testing "role lookup/list/mutation builders cast ids and keep deterministic ordering"
    (is (= [:= :id [:cast "role-1" :uuid]] (:where (q-roles/by-id "role-1"))))
    (is (= [:in :id [[:cast "role-1" :uuid] [:cast "role-2" :uuid]]]
           (:where (q-roles/by-ids ["role-1" "role-2"]))))
    (is (= [:and [:in :slug ["member" "admin"]]
            [:or [:= :org_id nil] [:= :org_id [:cast "org-1" :uuid]]]]
           (:where (q-roles/by-slugs-and-org ["member" "admin"] "org-1"))))
    (is (= [[:built_in :desc] :name] (:order-by (q-roles/list-all))))
    (is (= [:= :org_id [:cast "org-1" :uuid]] (:where (q-roles/list-by-org "org-1"))))
    (is (= [:cast "org-1" :uuid]
           (:org_id (first (:values (q-roles/insert {:org-id "org-1"
                                                     :name "Member"
                                                     :slug "member"
                                                     :scope-kind "org"
                                                     :built-in false
                                                     :system-managed false}))))))
    (is (= {:name "Member"
            :scope_kind "org"
            :built_in false
            :system_managed false
            :updated_at [:now]}
           (:set (q-roles/update-role "role-1" {:name "Member"
                                                 :scope-kind "org"
                                                 :built-in false
                                                 :system-managed false}))))
    (is (= [:in :role_id [[:cast "role-1" :uuid]]]
           (:where (q-roles/permissions-for-roles ["role-1"]))))
    (is (= [:in :rp.role_id [[:cast "role-1" :uuid]]]
           (:where (q-roles/permissions-for-roles-legacy ["role-1"]))))
    (is (= [:= :role_id [:cast "role-1" :uuid]]
           (:where (q-roles/delete-permissions "role-1"))))
    (is (re-find #"EXCLUDED\.effect"
                 (sql-string (q-roles/insert-permission-modern "role-1" "admin.read"))))
    (is (= [:in :rtp.role_id [[:cast "role-1" :uuid]]]
           (:where (q-roles/tool-policies-for-roles ["role-1"]))))
    (is (= [:= :role_id [:cast "role-1" :uuid]]
           (:where (q-roles/delete-tool-policies "role-1"))))
    (is (= {:membership_id [:cast "membership-1" :uuid]
            :role_id [:cast "role-1" :uuid]}
           (first (:values (q-roles/insert-membership-role "membership-1" "role-1"))))))
  (testing "membership lookup/list/mutation builders preserve joins and policy links"
    (is (= [:= :m.id [:cast "membership-1" :uuid]]
           (:where (q-memberships/by-id "membership-1"))))
    (is (= [:and [:= :m.user_id [:cast "user-1" :uuid]]
            [:= :m.org_id [:cast "org-1" :uuid]]]
           (:where (q-memberships/by-user-and-org "user-1" "org-1"))))
    (is (= [:= :id [:cast "membership-1" :uuid]]
           (:where (q-memberships/bare-by-id "membership-1"))))
    (is (= [:= :m.org_id [:cast "org-1" :uuid]]
           (:where (q-memberships/list-by-org "org-1"))))
    (is (= [:= :mr.membership_id [:cast "membership-1" :uuid]]
           (:where (q-memberships/role-slugs "membership-1"))))
    (is (= [:= :m.id [:cast "membership-1" :uuid]]
           (:where (q-memberships/with-user-and-org "membership-1"))))
    (is (= [:cast "user-1" :uuid]
           (:user_id (first (:values (q-memberships/upsert {:user-id "user-1"
                                                            :org-id "org-1"
                                                            :status "active"
                                                            :is-default true}))))))
    (is (= {:actor_id "actor" :updated_at [:now]}
           (:set (q-memberships/set-actor-id "membership-1" "actor"))))
    (is (= [:= :membership_id [:cast "membership-1" :uuid]]
           (:where (q-memberships/delete-roles "membership-1"))))
    (is (re-find #"(?i)on conflict"
                 (sql-string (q-memberships/insert-role "membership-1" "role-1"))))
    (is (= [:in :membership_id [[:cast "membership-1" :uuid]]]
           (:where (q-memberships/tool-policies-for-ids ["membership-1"]))))
    (is (= [:= :membership_id [:cast "membership-1" :uuid]]
           (:where (q-memberships/delete-tool-policies "membership-1"))))
    (is (= :memberships (:update q-memberships/backfill-actor-ids))))
  (testing "platform role lookup uses a null org scope while org lookup casts org id"
    (is (= [:and [:= :slug "admin"] [:= :org_id nil]]
           (:where (q-roles/by-slug {:slug "admin"}))))
    (is (= [:and [:= :slug "member"] [:= :org_id [:cast "org-1" :uuid]]]
           (:where (q-roles/by-slug {:slug "member" :org-id "org-1"})))))
  (testing "role and membership tool policy upserts use EXCLUDED dot references"
    (let [role-sql (sql-string (q-roles/insert-tool-policy "role-1" {:tool-id "read"
                                                                      :effect "allow"
                                                                      :constraints-json "{}"}))
          membership-sql (sql-string (q-memberships/insert-tool-policy "membership-1" {:tool-id "read"
                                                                                        :effect "deny"
                                                                                        :constraints-json "{}"}))]
      (is (re-find #"EXCLUDED\.effect" role-sql))
      (is (re-find #"EXCLUDED\.constraints_json" role-sql))
      (is (re-find #"EXCLUDED\.effect" membership-sql))
      (is (re-find #"EXCLUDED\.constraints_json" membership-sql))))
  (testing "email membership lookup switches between org id, org slug, and default ordering"
    (is (= 1 (:limit (q-memberships/by-email-and-org {:user-email "u@example.test"
                                                      :org-id "org-1"}))))
    (is (re-find #"(?i)lower\(o\.slug\) = lower"
                 (sql-string (q-memberships/by-email-and-org {:user-email "u@example.test"
                                                              :org-slug "open-hax"}))))
    (is (re-find #"(?i)o\.is_primary desc"
                 (sql-string (q-memberships/by-email-and-org {:user-email "u@example.test"}))))))
