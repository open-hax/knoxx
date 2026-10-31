(ns knoxx.backend.law.oidc-test
  "Portable admission proofs; the same laws run on JVM and in the backend CLJS suite."
  (:require #?(:clj [clojure.test :as test]
               :cljs [cljs.test :as test])
            [knoxx.backend.law.oidc :as law]))

(def link "An explicitly approved fixture link." {:issuer "https://authority.example/oidc" :subject "subject-1"
           :user-id "user-1" :membership-id "membership-1" :org-id "org-1"})
(def config "Operator-owned fixture configuration." {:issuer (:issuer link) :public-origin "https://knoxx.example"
             :client-id "knoxx-fixture" :client-secret "fixture-only" :links [link]})
(def context "An existing Knoxx fixture membership and actor." {:user {:id "user-1" :email "local@example.test"}
              :membership {:id "membership-1" :actor-id "actor-1"} :org {:id "org-1"}
              :actor {:id "actor-1" :binding "actor-1"}})
(def grant "Validated fixture identity lifetime." {:subject "subject-1" :expires-at 200000})
(def evidence "Current client-scoped fixture introspection." {:active true :scope "openid profile" :client_id "knoxx-fixture" :sub "subject-1" :exp 200})

(defn rejected?
  "Extract only the error code from a rejected admission."
  [f]
  (try (f) nil (catch #?(:clj Exception :cljs :default) error (:code (ex-data error)))))

(test/deftest qualified-link-and-collision-refusal
  (test/is (= link (law/approved-link! [link] (:issuer link) "subject-1")))
  (doseq [[issuer subject] [["https://other.example/oidc" "subject-1"]
                            [(:issuer link) "unlinked-subject"]
                            [(:issuer link) "local@example.test"]]]
    (test/is (= :oidc-link-required (rejected? #(law/approved-link! [link] issuer subject)))))
  (test/is (= config (law/require-config! config)))
  (test/is (= :oidc-config (rejected? #(law/require-config! (assoc config :links [link link])))))
  (test/is (= :oidc-config (rejected? #(law/require-config! (assoc config :client-secret "")))))
  (test/is (= :oidc-config (rejected? #(law/require-config! (assoc config :links [(assoc link :issuer "https://other.example")]))))))

(test/deftest local-identity-and-tenant-are-not-imported
  (test/is (= context (law/require-context! link context)))
  (doseq [changed [(assoc-in context [:user :id] "other-user")
                   (assoc-in context [:membership :id] "other-membership")
                   (assoc-in context [:org :id] "other-org")
                   (dissoc context :actor)
                   (update context :actor dissoc :binding)
                   (assoc-in context [:actor :id] "role-derived-default")]]
    (test/is (= :oidc-link-conflict (rejected? #(law/require-context! link changed))))))

(test/deftest grant-must-be-current-and-client-scoped
  (test/is (= evidence (law/require-grant! config grant evidence 100000)))
  (test/is (= (assoc evidence :aud ["knoxx-fixture"] :iss (:issuer config))
         (law/require-grant! config grant (assoc evidence :aud ["knoxx-fixture"] :iss (:issuer config)) 100000)))
  (test/testing "Inactive, missing, cross-subject/client/issuer/audience and expired evidence all refuse"
    (doseq [invalid [(assoc evidence :active false) (dissoc evidence :active)
                     (assoc evidence :client_id "other-client") (dissoc evidence :client_id)
                     (assoc evidence :sub "other-subject") (assoc evidence :iss "https://evil.example")
                     (assoc evidence :aud "other-client") (assoc evidence :aud ["other-client"])
                     (assoc evidence :exp 100) (dissoc evidence :exp)]]
      (test/is (= :oidc-grant-invalid (rejected? #(law/require-grant! config grant invalid 100000))))))
  (test/is (= :oidc-grant-invalid (rejected? #(law/require-grant! config grant evidence 200000))))
  (test/is (= :oidc-grant-invalid (rejected? #(law/require-grant! config (assoc grant :expires-at 100000) evidence 100000)))))

(test/deftest evidence-age-bounds-upgraded-transport-admission
  (test/is (nil? (law/require-fresh-evidence! 100000 104999)))
  (test/is (= :oidc-evidence-stale (rejected? #(law/require-fresh-evidence! 100000 105000))))
  (test/is (= :oidc-evidence-stale (rejected? #(law/require-fresh-evidence! 100000 99999)))))

(test/deftest cookie-writes-require-exact-product-origin
  (doseq [method ["GET" "HEAD" "OPTIONS"]]
    (test/is (nil? (law/require-browser-origin! config {:method method}))))
  (doseq [method ["POST" "PUT" "PATCH" "DELETE"]]
    (test/is (nil? (law/require-browser-origin! config
                    {:method method :origin (:public-origin config)})))
    (doseq [origin [nil "null" "https://sibling.example" "https://knoxx.example.attacker.test"]]
      (test/is (= :oidc-origin-invalid
                  (rejected? #(law/require-browser-origin! config {:method method :origin origin})))))))

(test/deftest browser-session-bound-is-finite-with-or-without-renewal
  (let [now 100000
        deadline (+ now law/session-limit-ms)]
    (doseq [expiry [(dec deadline) deadline (inc deadline) (+ deadline 86400000)]]
      (test/is (= (min deadline expiry)
                  (law/session-bound (assoc grant :expires-at expiry) evidence now))))
    (test/is (= deadline
                (law/session-bound (assoc grant :refresh-token "fixture-refresh")
                                   (assoc evidence :root_session_expires_at (/ (+ deadline 100000) 1000)) now)))
    (test/is (= 200000
                (law/session-bound (assoc grant :refresh-token "fixture-refresh")
                                   (assoc evidence :root_session_expires_at 200) now)))
    (test/is (= :oidc-grant-invalid
                (rejected? #(law/session-bound (assoc grant :refresh-token "fixture-refresh") evidence now))))))
