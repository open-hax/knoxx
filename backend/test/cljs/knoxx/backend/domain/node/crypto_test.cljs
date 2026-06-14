(ns knoxx.backend.domain.node.crypto-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.node.crypto :as crypto]))

(deftest random-hex-returns-lowercase-hex
  (testing "returns correct length hex string for given byte count"
    (let [hex-8 (crypto/random-hex 8)
          hex-16 (crypto/random-hex 16)
          hex-32 (crypto/random-hex 32)]
      (is (re-matches #"^[0-9a-f]{16}$" hex-8))
      (is (re-matches #"^[0-9a-f]{32}$" hex-16))
      (is (re-matches #"^[0-9a-f]{64}$" hex-32)))))

(deftest random-hex-produces-different-values
  (testing "subsequent calls produce different outputs"
    (is (not= (crypto/random-hex 16) (crypto/random-hex 16)))))

(deftest random-uuid-returns-valid-uuid-v4
  (testing "returns RFC4122 v4 UUID format"
    (let [uuid (crypto/random-uuid)]
      (is (string? uuid))
      (is (re-matches #"^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$" uuid)))))

(deftest random-uuid-produces-different-values
  (testing "subsequent calls produce different UUIDs"
    (is (not= (crypto/random-uuid) (crypto/random-uuid)))))

(deftest sha256-hex-returns-correct-digest
  (testing "SHA-256 of known strings"
    (is (= "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
           (crypto/sha256-hex "")))
    (is (= "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"
           (crypto/sha256-hex "Hello World")))
    (is (= "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"
           (crypto/sha256-hex "Hello World")))))

(deftest sha256-hex-is-consistent
  (testing "same input always produces same output"
    (is (= (crypto/sha256-hex "test") (crypto/sha256-hex "test")))))

(deftest md5-hex-returns-correct-digest
  (testing "MD5 of known strings"
    (is (= "d41d8cd98f00b204e9800998ecf8427e"
           (crypto/md5-hex "")))
    (is (= "b10a8db164e0754105b7a99be72e3fe5"
           (crypto/md5-hex "Hello World")))
    (is (= "b10a8db164e0754105b7a99be72e3fe5"
           (crypto/md5-hex "Hello World")))))

(deftest md5-hex-is-consistent
  (testing "same input always produces same output"
    (is (= (crypto/md5-hex "test") (crypto/md5-hex "test")))))
