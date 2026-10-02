(ns knoxx.backend.axxium-identity-test
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.auth.axxium :as identity]
            [knoxx.backend.infra.auth.axxium :as axxium-auth]
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

(deftest mixed-case-authority-email-uses-the-directory-key
  (let [mixed (assoc actor :email "Person@Example.test")
        row (identity/local-user "issuer" mixed "local" "host")]
    (is (= "person@example.test" (:email row)))
    (is (= (:email row) (:email (identity/normalize-actor mixed))))
    (is (= "issuer#actor_123" (:external_subject row)))))

(deftest ^:async password-verification-revokes-the-provider-session
  (let [requests (atom [])
        request! (fn [request]
                   (swap! requests conj request)
                   (js/Promise.resolve
                    (if (str/ends-with? (:url request) "/api/auth/login")
                      {:status 200 :body {:actor actor :token "transient-token"}}
                      {:status 200 :body {:ok true}})))]
    (is (= actor (:actor (await (axxium-auth/authenticate-with!
                            "https://axxium.example.test" request!
                            "person@example.test" "password")))))
    (is (= ["https://axxium.example.test/api/auth/login"
            "https://axxium.example.test/api/auth/logout"]
           (mapv :url @requests)))
    (is (= "Bearer transient-token" (get-in (second @requests) [:headers "Authorization"])))))
