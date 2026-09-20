(ns knoxx.frontend.lib.storage-test
  "Written FIRST (TDD) — defines the contract for the CLJS port of
  src/lib/storage.ts: safe localStorage helpers with quota-exceeded
  eviction of old knoxx_ keys. Uses an injected mock localStorage on
  js/globalThis so the suite runs under node."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.storage :as storage]))

;; ── mock localStorage ────────────────────────────────────────────────────────
;; Backed by an atom of [key value] pairs (insertion-ordered, like real
;; localStorage in practice). `fail-sets` makes setItem throw N times.

(def ^:private store (atom []))
(def ^:private fail-sets (atom 0))

(defn- idx-of [k]
  (some (fn [[i [storage-key _]]] (when (= storage-key k) i))
        (map-indexed vector @store)))

(def ^:private mock-local-storage
  (let [obj #js {}]
    (set! (.-getItem obj)
          (fn [k] (or (some (fn [[storage-key v]] (when (= storage-key k) v)) @store) nil)))
    (set! (.-setItem obj)
          (fn [k v]
            (if (pos? @fail-sets)
              (do (swap! fail-sets dec)
                  (throw (js/Error. "QuotaExceededError")))
              (if-let [i (idx-of k)]
                (swap! store assoc i [k v])
                (swap! store conj [k v])))))
    (set! (.-removeItem obj)
          (fn [k]
            (when-let [i (idx-of k)]
              (swap! store (fn [s] (vec (concat (subvec s 0 i) (subvec s (inc i)))))))))
    (set! (.-key obj)
          (fn [i] (if (< i (count @store)) (first (nth @store i)) nil)))
    (js/Object.defineProperty obj "length"
                              #js {:get (fn [] (count @store))})
    obj))

(def ^:private previous-storage (atom nil))

(t/use-fixtures :each
  {:before (fn []
             (reset! previous-storage (.-localStorage js/globalThis))
             (reset! store [])
             (reset! fail-sets 0)
             (set! (.-localStorage js/globalThis) mock-local-storage))
   :after (fn [] (set! (.-localStorage js/globalThis) @previous-storage))})

(defn- stored-keys []
  (mapv first @store))

;; ── tests ────────────────────────────────────────────────────────────────────

(t/deftest get-set-roundtrip
  (t/is (true? (storage/safe-set-item "knoxx_a" "1")))
  (t/is (= "1" (storage/safe-get-item "knoxx_a")))
  (t/is (nil? (storage/safe-get-item "missing"))))

(t/deftest set-overwrites-existing
  (storage/safe-set-item "knoxx_a" "1")
  (storage/safe-set-item "knoxx_a" "2")
  (t/is (= "2" (storage/safe-get-item "knoxx_a")))
  (t/is (= ["knoxx_a"] (stored-keys))))

(t/deftest quota-exceeded-evicts-oldest-half-of-knoxx-keys-and-retries
  (storage/safe-set-item "knoxx_old1" "x")
  (storage/safe-set-item "knoxx_old2" "x")
  (storage/safe-set-item "knoxx_old3" "x")
  (storage/safe-set-item "knoxx_old4" "x")
  (storage/safe-set-item "other_key" "keep")
  (reset! fail-sets 1)
  (t/is (true? (storage/safe-set-item "knoxx_new" "v")) "retry after eviction succeeds")
  (t/is (= "v" (storage/safe-get-item "knoxx_new")))
  (t/testing "oldest half (2 of 4) of knoxx_ keys evicted; non-knoxx untouched"
    (t/is (nil? (storage/safe-get-item "knoxx_old1")))
    (t/is (nil? (storage/safe-get-item "knoxx_old2")))
    (t/is (= "x" (storage/safe-get-item "knoxx_old3")))
    (t/is (= "x" (storage/safe-get-item "knoxx_old4")))
    (t/is (= "keep" (storage/safe-get-item "other_key")))))

(t/deftest quota-exceeded-never-evicts-the-key-being-set
  (storage/safe-set-item "knoxx_target" "old")
  (storage/safe-set-item "knoxx_other1" "x")
  (storage/safe-set-item "knoxx_other2" "x")
  (reset! fail-sets 1)
  (t/is (true? (storage/safe-set-item "knoxx_target" "new")))
  (t/is (= "new" (storage/safe-get-item "knoxx_target"))))

(t/deftest set-returns-false-when-retry-also-fails
  (reset! fail-sets 2)
  (t/is (false? (storage/safe-set-item "knoxx_a" "v")))
  (t/is (nil? (storage/safe-get-item "knoxx_a"))))

(t/deftest remove-item-is-safe
  (storage/safe-set-item "knoxx_a" "1")
  (storage/safe-remove-item "knoxx_a")
  (t/is (nil? (storage/safe-get-item "knoxx_a")))
  ;; removing a missing key must not throw
  (storage/safe-remove-item "never_there"))

(t/deftest clear-knoxx-storage-removes-only-knoxx-keys
  (storage/safe-set-item "knoxx_a" "1")
  (storage/safe-set-item "knoxx_b" "2")
  (storage/safe-set-item "theme" "dark")
  (storage/clear-knoxx-storage)
  (t/is (= ["theme"] (stored-keys))))
