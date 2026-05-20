;; Tests for knoxx.backend.domain.node.path and knoxx.backend.domain.node.crypto
;; Pure fns — no async, no fixtures needed.

(ns knoxx.backend.node.path-crypto-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.node.path :as path]
            [knoxx.backend.domain.node.crypto :as crypto]))

;; -- path ------------------------------------------------------------------

(deftest join-combines-parts
  (is (string? (path/join "/a" "b" "c.edn"))))

(deftest join-skips-nil
  (is (= (path/join "/a" "b") (path/join "/a" nil "b"))))

(deftest dirname-returns-parent
  (is (= "/a/b" (path/dirname "/a/b/c.txt"))))

(deftest basename-returns-filename
  (is (= "c.txt" (path/basename "/a/b/c.txt"))))

(deftest basename-strips-ext
  (is (= "c" (path/basename "/a/b/c.txt" ".txt"))))

(deftest extname-returns-dot-ext
  (is (= ".edn" (path/extname "/a/b/c.edn"))))

(deftest extname-empty-for-no-ext
  (is (= "" (path/extname "/a/b/noext"))))

(deftest relative-gives-relative-path
  (is (= "b/c.txt" (path/relative "/a" "/a/b/c.txt"))))

(deftest resolve-returns-absolute
  (is (path/absolute? (path/resolve "/a" "b"))))

(deftest cwd-returns-string
  (is (string? (path/cwd))))

(deftest absolute?-true-for-slash
  (is (true? (path/absolute? "/foo"))))

(deftest absolute?-false-for-relative
  (is (false? (path/absolute? "foo/bar"))))

;; -- crypto ----------------------------------------------------------------

(deftest random-hex-returns-string
  (is (string? (crypto/random-hex 16))))

(deftest random-hex-correct-length
  (is (= 32 (count (crypto/random-hex 16)))))

(deftest random-hex-different-each-time
  (is (not= (crypto/random-hex 16) (crypto/random-hex 16))))

(deftest random-uuid-is-string
  (is (string? (crypto/random-uuid))))

(deftest random-uuid-matches-format
  (is (re-matches #"[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
                  (crypto/random-uuid))))

(deftest sha256-hex-is-64-chars
  (is (= 64 (count (crypto/sha256-hex "hello")))))

(deftest sha256-hex-deterministic
  (is (= (crypto/sha256-hex "hello") (crypto/sha256-hex "hello"))))

(deftest sha256-hex-different-inputs
  (is (not= (crypto/sha256-hex "a") (crypto/sha256-hex "b"))))

(deftest md5-hex-is-32-chars
  (is (= 32 (count (crypto/md5-hex "hello")))))
