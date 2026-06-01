;; Tests for knoxx.backend.domain.node.fs
;; Real Node fs, no mocks. Fixtures in /tmp/knoxx-node-fs-test.

(ns knoxx.backend.node.fs-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.node.fs :as sut]
            [knoxx.backend.domain.node.path :as p]))

(def ^:private root "/tmp/knoxx-node-fs-test")

;; -- sync ------------------------------------------------------------------

(deftest exists?-false-for-missing
  (is (false? (sut/exists? "/no/such/path"))))

(deftest read-file-sync-nil-on-missing
  (is (nil? (sut/read-file-sync "/no/such/file.txt"))))

(deftest readdir-sync-empty-on-missing
  (is (= [] (sut/readdir-sync "/no/such/dir"))))

;; -- async round-trip -------------------------------------------------------

(deftest ^:async write-then-read
  (await (sut/mkdir! root))
  (await (sut/write-file! (p/join root "hello.txt") "world"))
  (let [t (await (sut/read-file! (p/join root "hello.txt")))]
    (is (= "world" t))))

(deftest ^:async write-file-ensure-dir-creates-parents
  (let [f (p/join root "a" "b" "nested.txt")]
    (await (sut/write-file-ensure-dir! f "nested"))
    (let [t (await (sut/read-file! f))]
      (is (= "nested" t)))))

(deftest ^:async read-file-rejects-on-missing
  (try
    (await (sut/read-file! "/no/such/file.txt"))
    (is false "should reject")
    (catch :default e
      (is (= "ENOENT" (.-code e))))))

(deftest ^:async mkdir-is-idempotent
  (await (sut/mkdir! root))
  (await (sut/mkdir! root))
  (is true))

(deftest ^:async stat-returns-cljs-map
  (let [f (p/join root "stat.txt")]
    (await (sut/mkdir! root))
    (await (sut/write-file! f "stat me"))
    (let [s (await (sut/stat! f))]
      (is (map? s))
      (is (true?  (:is-file? s)))
      (is (false? (:is-dir?  s)))
      (is (number? (:size s)))
      (is (string? (:mtime s))))))

(deftest ^:async stat-or-nil-on-missing
  (let [v (await (sut/stat-or-nil! "/no/such/file.txt"))]
    (is (nil? v))))

(deftest ^:async readdir-returns-cljs-vec
  (await (sut/mkdir! root))
  (await (sut/write-file! (p/join root "r.edn") "{}"))
  (let [ns (await (sut/readdir! root))]
    (is (vector? ns))
    (is (every? string? ns))))

(deftest ^:async readdir-returns-empty-on-missing
  (let [v (await (sut/readdir! "/no/such/dir"))]
    (is (= [] v))))

(deftest ^:async readdir-deep-finds-files-recursively
  (let [d (p/join root "deep")]
    (await (sut/write-file-ensure-dir! (p/join d "one.edn") "{}"))
    (await (sut/write-file-ensure-dir! (p/join d "sub" "two.edn") "{}"))
    (let [ps (await (sut/readdir-deep! d))]
      (is (vector? ps))
      (is (= 2 (count ps)))
      (is (every? string? ps)))))

(deftest ^:async readdir-deep-with-pred
  (let [d (p/join root "filtered")]
    (await (sut/write-file-ensure-dir! (p/join d "keep.edn") "{}"))
    (await (sut/write-file-ensure-dir! (p/join d "drop.txt") "hi"))
    (let [ps (await (sut/readdir-deep! d (fn [n] (str/ends-with? n ".edn"))))]
      (is (= 1 (count ps)))
      (is (str/ends-with? (first ps) "keep.edn")))))

(deftest ^:async readdir-deep-empty-on-missing
  (let [v (await (sut/readdir-deep! "/no/such/dir"))]
    (is (= [] v))))

(deftest ^:async unlink-removes-file
  (let [f (p/join root "del.txt")]
    (await (sut/mkdir! root))
    (await (sut/write-file! f "bye"))
    (await (sut/unlink! f))
    (is (false? (sut/exists? f)))))

(deftest ^:async unlink-tolerates-missing
  (await (sut/unlink! "/no/such/file.txt"))
  (is true))
