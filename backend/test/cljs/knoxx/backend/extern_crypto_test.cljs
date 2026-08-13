(ns knoxx.backend.extern-crypto-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.crypto :as crypto]))

(deftest crypto-boundary-produces-and-compares-hex-data
  (let [salt (crypto/random-hex 16)
        hash (crypto/scrypt-hex "password" salt 64)]
    (testing "native buffers remain behind a scalar CLJS-facing API"
      (is (= 32 (count salt)))
      (is (= 128 (count hash)))
      (is (crypto/secure-hex= hash hash))
      (is (not (crypto/secure-hex= hash (crypto/scrypt-hex "wrong" salt 64)))))))
