;; Tests for knoxx.backend.node.fs
;; Real Node fs, no mocks. Fixtures in /tmp/knoxx-node-fs-test.

(ns knoxx.backend.node.fs-test
  (:require [cljs.test :refer [deftest is testing async]]
            [knoxx.backend.node.fs :as sut]
            [knoxx.backend.node.path :as p]))

(def ^:private root "/tmp/knoxx-node-fs-test")

;; -- sync ------------------------------------------------------------------

(deftest exists?-false-for-missing
  (is (false? (sut/exists? "/no/such/path"))))

(deftest read-file-sync-nil-on-missing
  (is (nil? (sut/read-file-sync "/no/such/file.txt"))))

(deftest readdir-sync-empty-on-missing
  (is (= [] (sut/readdir-sync "/no/such/dir"))))

;; -- async round-trip -------------------------------------------------------

(deftest write-then-read
  (async done
    (-> (sut/mkdir! root)
        (.then #(sut/write-file! (p/join root "hello.txt") "world"))
        (.then #(sut/read-file! (p/join root "hello.txt")))
        (.then (fn [t] (is (= "world" t)) (done)))
        (.catch (fn [e] (is false (str e)) (done))))))

(deftest write-file-ensure-dir-creates-parents
  (async done
    (let [f (p/join root "a" "b" "nested.txt")]
      (-> (sut/write-file-ensure-dir! f "nested")
          (.then #(sut/read-file! f))
          (.then (fn [t] (is (= "nested" t)) (done)))
          (.catch (fn [e] (is false (str e)) (done)))))))

(deftest read-file-rejects-on-missing
  (async done
    (-> (sut/read-file! "/no/such/file.txt")
        (.then (fn [_] (is false "should reject") (done)))
        (.catch (fn [e] (is (= "ENOENT" (.-code e))) (done))))))

(deftest mkdir-is-idempotent
  (async done
    (-> (sut/mkdir! root)
        (.then #(sut/mkdir! root))
        (.then (fn [_] (is true) (done)))
        (.catch (fn [e] (is false (str e)) (done))))))

(deftest stat-returns-cljs-map
  (async done
    (let [f (p/join root "stat.txt")]
      (-> (sut/mkdir! root)
          (.then #(sut/write-file! f "stat me"))
          (.then #(sut/stat! f))
          (.then (fn [s]
                   (is (map? s))
                   (is (true?  (:is-file? s)))
                   (is (false? (:is-dir?  s)))
                   (is (number? (:size s)))
                   (is (string? (:mtime s)))
                   (done)))
          (.catch (fn [e] (is false (str e)) (done)))))))

(deftest stat-or-nil-on-missing
  (async done
    (-> (sut/stat-or-nil! "/no/such/file.txt")
        (.then (fn [v] (is (nil? v)) (done)))
        (.catch (fn [e] (is false (str e)) (done))))))

(deftest readdir-returns-cljs-vec
  (async done
    (-> (sut/mkdir! root)
        (.then #(sut/write-file! (p/join root "r.edn") "{}"))
        (.then #(sut/readdir! root))
        (.then (fn [ns]
                 (is (vector? ns))
                 (is (every? string? ns))
                 (done)))
        (.catch (fn [e] (is false (str e)) (done))))))

(deftest readdir-returns-empty-on-missing
  (async done
    (-> (sut/readdir! "/no/such/dir")
        (.then (fn [v] (is (= [] v)) (done)))
        (.catch (fn [e] (is false (str e)) (done))))))

(deftest readdir-deep-finds-files-recursively
  (async done
    (let [d (p/join root "deep")]
      (-> (sut/write-file-ensure-dir! (p/join d "one.edn") "{}")
          (.then #(sut/write-file-ensure-dir! (p/join d "sub" "two.edn") "{}"))
          (.then #(sut/readdir-deep! d))
          (.then (fn [ps]
                   (is (vector? ps))
                   (is (= 2 (count ps)))
                   (is (every? string? ps))
                   (done)))
          (.catch (fn [e] (is false (str e)) (done)))))))

(deftest readdir-deep-with-pred
  (async done
    (let [d (p/join root "filtered")]
      (-> (sut/write-file-ensure-dir! (p/join d "keep.edn") "{}")
          (.then #(sut/write-file-ensure-dir! (p/join d "drop.txt") "hi"))
          (.then (fn [_] (sut/readdir-deep! d (fn [n] (clojure.string/ends-with? n ".edn")))))
          (.then (fn [ps]
                   (is (= 1 (count ps)))
                   (is (clojure.string/ends-with? (first ps) "keep.edn"))
                   (done)))
          (.catch (fn [e] (is false (str e)) (done)))))))

(deftest readdir-deep-empty-on-missing
  (async done
    (-> (sut/readdir-deep! "/no/such/dir")
        (.then (fn [v] (is (= [] v)) (done)))
        (.catch (fn [e] (is false (str e)) (done))))))

(deftest unlink-removes-file
  (async done
    (let [f (p/join root "del.txt")]
      (-> (sut/mkdir! root)
          (.then #(sut/write-file! f "bye"))
          (.then #(sut/unlink! f))
          (.then (fn [_] (is (false? (sut/exists? f))) (done)))
          (.catch (fn [e] (is false (str e)) (done)))))))

(deftest unlink-tolerates-missing
  (async done
    (-> (sut/unlink! "/no/such/file.txt")
        (.then (fn [_] (is true) (done)))
        (.catch (fn [e] (is false (str e)) (done))))))