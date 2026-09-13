(ns knoxx.backend.axxium-identity-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.auth.axxium :as identity]
            [knoxx.backend.law.axxium-identity :as law]))
(def actor {:id "actor_123" :email "person@example.test" :display_name "Person" :status "active"})
(deftest remote-identity-never-imports-privileges-or-passwords
  (let [row (identity/local-user "https://yoga.axxium.promethean.rest"
               (assoc actor :roles ["admin"] :capabilities ["all"] :password_hash "secret") "user-id" "instance-id")]
    (is (= "https://yoga.axxium.promethean.rest#actor_123" (:external_subject row)))
    (is (= "axxium" (:auth_provider row)))
    (doseq [key [:roles :capabilities :password_hash]] (is (not (contains? row key))))))
(deftest email-collisions-and-inactive-identities-do-not-link
  (is (law/same-binding? {:auth_provider "axxium" :external_subject "issuer#actor" :status "active"} "issuer#actor"))
  (doseq [user [{:auth_provider "local" :external_subject "issuer#actor" :status "active"}
                {:auth_provider "axxium" :external_subject "different#actor" :status "active"}
                {:auth_provider "axxium" :external_subject "issuer#actor" :status "disabled"}]]
    (is (not (law/same-binding? user "issuer#actor")))))
(deftest malformed-and-disabled-authority-responses-fail
  (is (= actor (law/require-actor! actor)))
  (doseq [change [{:id ""} {:id "$operator"} {:email {:$ne nil}} {:status "disabled"} {:display_name ""}]]
    (is (thrown? cljs.core/ExceptionInfo (law/require-actor! (merge actor change))))))
