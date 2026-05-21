(ns knoxx.backend.domain.agent.text-delta
  "Pure text-delta helpers for provider streams."
  (:require [clojure.string :as str]))

(defn- duplicate-normalized-text
  [s]
  (str/lower-case (str/replace (str s) #"[\s\W_]+" "")))

(defn- boundary-ended?
  [s]
  (boolean (re-find #"[\s\W_]$" s)))

(defn- duplicated-prefix?
  [previous appended]
  (or (and (str/starts-with? appended previous)
           (let [remaining (.slice appended (count previous))]
             (and (pos? (count remaining))
                  (boolean (re-find #"^[\s\W_]" remaining)))))
      (and (boundary-ended? previous)
           (seq (duplicate-normalized-text previous))
           (= (duplicate-normalized-text appended)
              (duplicate-normalized-text previous)))))

(defn- max-overlap
  [left right]
  (loop [n (min (count left) (count right))]
    (cond
      (zero? n) 0
      (str/ends-with? left (.slice right 0 n)) n
      :else (recur (dec n)))))

(defn diff-appended-text
  "Return only the suffix in current that has not already appeared at the end of
   previous. Handles provider cumulative chunks and duplicated-prefix glitches."
  [previous current]
  (let [previous (str (or previous ""))
        current (str (or current ""))]
    (cond
      (str/blank? current) ""
      (str/blank? previous) current
      (= current previous) ""
      (str/starts-with? current previous) (let [appended (.slice current (count previous))]
                                            (if (duplicated-prefix? previous appended)
                                              (.slice appended (count previous))
                                              appended))
      :else (.slice current (max-overlap previous current)))))

(defn suppress-replayed-prefix-delta
  "Pure version of stream replay-prefix suppression.

   Inputs:
   - previous: cumulative text already emitted
   - replay-offset: offset into previous when an in-progress replay is being
     suppressed, or nil/0 when no replay is active
   - delta: next provider delta

   Returns {:delta <safe delta> :replay-offset <next offset or nil>}."
  [previous replay-offset delta]
  (let [previous (str (or previous ""))
        delta (str (or delta ""))
        offset (long (or replay-offset 0))
        delta-len (count delta)
        previous-len (count previous)
        prior-has-boundary? (boolean (re-find #"[\s\W_]" previous))
        next-offset (fn [value]
                      (when (< value previous-len)
                        value))]
    (cond
      (str/blank? delta)
      {:delta "" :replay-offset (when (pos? offset) offset)}

      (pos? offset)
      (let [expected (.slice previous offset (min previous-len (+ offset delta-len)))]
        (cond
          (= delta expected)
          {:delta "" :replay-offset (next-offset (+ offset delta-len))}

          (str/starts-with? delta expected)
          {:delta (.slice delta (count expected))
           :replay-offset nil}

          :else
          {:delta delta :replay-offset nil}))

      (and prior-has-boundary?
           (not (boolean (re-find #"\s$" previous)))
           (str/starts-with? previous delta)
           (< delta-len previous-len))
      {:delta "" :replay-offset (next-offset delta-len)}

      :else
      {:delta delta :replay-offset nil})))
