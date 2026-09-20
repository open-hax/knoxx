(ns knoxx.backend.law.policy-values-test
  (:require #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is]])
            [knoxx.backend.law.policy-values :as values]))

(deftest principal-identifiers-preserve-actor-case-but-canonicalize-email
  (is (= "admin@example.com" (values/normalize-email " Admin@Example.COM ")))
  (is (= "Editor-1" (values/normalize-actor-id " Editor-1 ")))
  (is (nil? (values/normalize-email "  ")))
  (is (nil? (values/normalize-actor-id nil)))
  (is (= "self-admin-at-example-com" (values/self-org-slug " Admin@Example.COM "))))

(deftest role-aliases-and-contract-projection-preserve-finite-identifiers
  (is (= ["system_admin" "system-admin"] (values/role-slug-aliases " system_admin ")))
  (is (= [] (values/role-slug-aliases nil)))
  (is (= ["system-admin" "editor"]
         (values/actor-projection-role-slugs {:actor/roles [:system_admin " editor " :editor 12 nil]})))
  (is (= ["editor" "reviewer"] (values/requested-role-slugs [" editor " nil "reviewer" "editor"])))
  (is (= #{"editor"}
         (values/known-contract-role-slugs [{:contractClass "roles" :id "editor"}
                                            {:contractClass "capabilities" :id "publish"}]))))

(deftest bootstrap-normalization-keeps-the-migration-revocation-set
  (is (= ["system-admin@open-hax.local" "previous@example.com"]
         (values/previous-bootstrap-system-admin-emails
          {:bootstrapSystemAdminPreviousEmails "Previous@example.com, current@example.com Previous@example.com"}
          "CURRENT@example.com")))
  (is (= ["knowledge-worker"] (values/bootstrap-allowlist-role-slugs {})))
  (is (= ["editor" "reviewer"]
         (values/bootstrap-allowlist-role-slugs {:bootstrapAllowlistRoleSlugs "editor, reviewer editor"}))))
