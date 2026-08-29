(ns knoxx.backend.infra.auth.password-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.auth.password :as password]))

(deftest password-secret-round-trip-test
  (let [secret (password/hash-password "correct horse battery staple")]
    (testing "the encoded credential names its algorithm and verifies only the source password"
      (is (= "scrypt" (:algorithm secret)))
      (is (password/valid-password? "correct horse battery staple" secret))
      (is (false? (password/valid-password? "wrong password" secret))))
    (testing "each encoding receives a fresh salt"
      (is (not= (:salt secret)
                (:salt (password/hash-password "correct horse battery staple")))))))

(deftest malformed-password-secret-fails-closed-test
  (is (false? (password/valid-password? "password" nil)))
  (is (false? (password/valid-password? "password" {:algorithm "unknown"}))))
