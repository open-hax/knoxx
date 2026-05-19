(ns knoxx.backend.authz-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.auth.authz :as authz]))

;; ─── fixtures ────────────────────────────────────────────────────

(def base-ctx
  {:orgId "org-1"
   :userId "user-1"
   :userEmail "alice@example.com"
   :membershipId "mem-1"
   :roleSlugs ["knowledge_worker"]
   :permissions ["docs.read"]
   :toolPolicies [{:toolId "tool-42" :effect "allow"}
                  {:toolId "tool-99" :effect "deny"}]})

;; ─── ctx accessors ───────────────────────────────────────────────

(deftest ctx-accessors-flat-keys
  (testing "flat camelCase keys"
    (is (= "org-1"             (authz/ctx-org-id base-ctx)))
    (is (= "user-1"            (authz/ctx-user-id base-ctx)))
    (is (= "alice@example.com" (authz/ctx-user-email base-ctx)))
    (is (= "mem-1"             (authz/ctx-membership-id base-ctx)))
    (is (nil?                   (authz/ctx-actor-id base-ctx)))))

(deftest ctx-accessors-nested-keys
  (testing "nested org/user/membership shape"
    (let [ctx {:org {:id "org-2" :slug "acme"}
               :user {:id "user-2" :email "bob@example.com"}
               :membership {:id "mem-2"}
               :roles [{:slug "admin"}]}]
      (is (= "org-2"            (authz/ctx-org-id ctx)))
      (is (= "acme"             (authz/ctx-org-slug ctx)))
      (is (= "user-2"           (authz/ctx-user-id ctx)))
      (is (= "bob@example.com" (authz/ctx-user-email ctx)))
      (is (= "mem-2"            (authz/ctx-membership-id ctx)))
      (is (= #{"admin"}         (authz/ctx-role-slugs ctx))))))

;; ─── system-admin? ───────────────────────────────────────────────

(deftest system-admin-flag
  (is (false? (authz/system-admin? base-ctx)))
  (is (true?  (authz/system-admin? (assoc base-ctx :roleSlugs ["system_admin"]))))
  (is (true?  (authz/system-admin? {:roles [{:slug "system_admin"}]})))
  (is (false? (authz/system-admin? {}))))

;; ─── permissions ─────────────────────────────────────────────────

(deftest permission-checks
  (testing "ctx-permitted? exact match"
    (is (true?  (authz/ctx-permitted? base-ctx "docs.read")))
    (is (false? (authz/ctx-permitted? base-ctx "docs.write"))))
  (testing "ctx-any-permission? short-circuits on first hit"
    (is (true?  (authz/ctx-any-permission? base-ctx ["docs.write" "docs.read"])))
    (is (false? (authz/ctx-any-permission? base-ctx ["docs.write" "admin"]))))
  (testing "empty permissions set"
    (is (false? (authz/ctx-permitted? {} "anything")))))

;; ─── tool policies ───────────────────────────────────────────────

(deftest tool-policy-checks
  (testing "allow effect"
    (is (= "allow" (authz/ctx-tool-effect base-ctx "tool-42")))
    (is (true?     (authz/ctx-tool-allowed? base-ctx "tool-42"))))
  (testing "deny effect"
    (is (= "deny"  (authz/ctx-tool-effect base-ctx "tool-99")))
    (is (false?    (authz/ctx-tool-allowed? base-ctx "tool-99"))))
  (testing "unknown tool → nil effect, false allowed"
    (is (nil?   (authz/ctx-tool-effect base-ctx "tool-00")))
    (is (false? (authz/ctx-tool-allowed? base-ctx "tool-00")))))

;; ─── ensure-permission! ──────────────────────────────────────────

(deftest ensure-permission-passes
  (is (= base-ctx (authz/ensure-permission! base-ctx "docs.read"))))

(deftest ensure-permission-throws-when-missing
  (is (thrown? js/Error (authz/ensure-permission! base-ctx "docs.write"))))

(deftest ensure-permission-system-admin-bypass
  (let [admin (assoc base-ctx :roleSlugs ["system_admin"])]
    (is (= admin (authz/ensure-permission! admin "docs.write")))))

;; ─── ensure-org-scope! ───────────────────────────────────────────

(deftest ensure-org-scope-passes
  (is (= base-ctx (authz/ensure-org-scope! base-ctx "org-1" "docs.read"))))

(deftest ensure-org-scope-wrong-org-throws
  (is (thrown? js/Error (authz/ensure-org-scope! base-ctx "org-99" "docs.read"))))

(deftest ensure-org-scope-system-admin-ignores-boundary
  (let [admin (assoc base-ctx :roleSlugs ["system_admin"])]
    (is (= admin (authz/ensure-org-scope! admin "org-99" "docs.read")))))

;; ─── principal-match? ────────────────────────────────────────────

(deftest principal-match-membership-id
  (testing "membership id wins when both sides non-blank"
    (is (true?  (authz/principal-match? base-ctx {:membership_id "mem-1"})))
    (is (false? (authz/principal-match? base-ctx {:membership_id "mem-99"})))))

(deftest principal-match-user-id-fallback
  (let [ctx (dissoc base-ctx :membershipId)]
    (is (true?  (authz/principal-match? ctx {:user_id "user-1"})))
    (is (false? (authz/principal-match? ctx {:user_id "user-99"})))))

(deftest principal-match-email-fallback-case-insensitive
  (let [ctx (-> base-ctx (dissoc :membershipId) (dissoc :userId))]
    (is (true?  (authz/principal-match? ctx {:user_email "ALICE@EXAMPLE.COM"})))
    (is (false? (authz/principal-match? ctx {:user_email "bob@example.com"})))))

(deftest principal-match-system-admin-always-true
  (let [admin (assoc base-ctx :roleSlugs ["system_admin"])]
    (is (true? (authz/principal-match? admin {:membership_id "anything"})))))

(deftest principal-match-enforces-actor-boundary
  (let [ctx (assoc base-ctx :actorId "cms_chat")]
    (is (true? (authz/principal-match? ctx {:membership_id "mem-1" :actor_id "cms_chat"})))
    (is (false? (authz/principal-match? ctx {:membership_id "mem-1" :actor_id "chat_primary"})))
    (is (false? (authz/principal-match? (dissoc ctx :actorId) {:membership_id "mem-1" :actor_id "cms_chat"})))))

(deftest principal-match-allows-actor-only-snapshots
  (is (true? (authz/principal-match? {:actorId "broadcast_studio"} {:actor_id "broadcast_studio"})))
  (is (false? (authz/principal-match? {:actorId "cms_chat"} {:actor_id "broadcast_studio"}))))

;; ─── run-visible? ────────────────────────────────────────────────

(deftest run-visibility-nil-ctx
  (is (true? (authz/run-visible? nil {:org_id "org-1"}))))

(deftest run-visibility-system-admin
  (let [admin (assoc base-ctx :roleSlugs ["system_admin"])]
    (is (true? (authz/run-visible? admin {:org_id "org-99"})))))

(deftest run-visibility-read-all
  (is (true? (authz/run-visible?
               (assoc base-ctx :permissions ["agent.runs.read_all"])
               {:org_id "org-99"}))))

(deftest run-visibility-read-org
  (let [ctx (assoc base-ctx :permissions ["agent.runs.read_org"])]
    (is (true?  (authz/run-visible? ctx {:org_id "org-1"})))
    (is (false? (authz/run-visible? ctx {:org_id "org-99"})))))

(deftest run-visibility-read-own
  (let [ctx (assoc base-ctx :permissions ["agent.runs.read_own"])]
    (is (true?  (authz/run-visible? ctx {:membership_id "mem-1"})))
    (is (false? (authz/run-visible? ctx {:membership_id "mem-99"})))))

(deftest run-visibility-no-permission
  (is (false? (authz/run-visible? (assoc base-ctx :permissions []) {:org_id "org-1"}))))

;; ─── auth-snapshot ───────────────────────────────────────────────

(deftest auth-snapshot-shape
  (let [snap (authz/auth-snapshot base-ctx)]
    (is (= "org-1"             (:org_id snap)))
    (is (= "user-1"            (:user_id snap)))
    (is (= "alice@example.com" (:user_email snap)))
    (is (nil?                  (:actor_id snap)))
    (is (= false               (:is_system_admin snap)))
    (is (vector? (:role_slugs snap)))
    (is (vector? (:permissions snap)))))

(deftest auth-snapshot-has-principal
  (is (true?  (authz/auth-snapshot-has-principal? (authz/auth-snapshot base-ctx))))
  (is (true?  (authz/auth-snapshot-has-principal? {:actor_id "cms_chat"})))
  (is (false? (authz/auth-snapshot-has-principal? {}))))
