(ns knoxx.backend.law.persistence-instant-test
  "Portable validator cases on the JVM and both persistence contracts in CLJS."
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing]])
            #?(:clj [knoxx.backend.law.persistence-instant :as instant])
            #?(:cljs [knoxx.backend.law.run-store :as run])
            #?(:cljs [knoxx.backend.law.thread-store :as thread])
            [malli.core :as m]))

(def schemas
  "Exercise the portable schema on the JVM and its two consumers in CLJS."
  #?(:clj [instant/Instant] :cljs [run/Instant thread/Instant]))

(deftest canonical-calendar-instants-remain-valid
  (doseq [schema schemas
          value ["0000-02-29T00:00:00.000Z"
                 "0001-01-01T00:00:00.000Z"
                 "1970-01-01T00:00:00.000Z"
                 "1900-02-28T23:59:59.999Z"
                 "2000-02-29T12:34:56.789Z"
                 "2024-02-29T12:34:56.789Z"
                 "2026-04-30T23:59:59.999Z"
                 "2026-12-31T23:59:59.999Z"
                 "2400-02-29T00:00:00.000Z"
                 "9999-12-31T23:59:59.999Z"]]
    (is (m/validate schema value) value)))

(deftest impossible-calendar-and-clock-values-are-refused
  (doseq [schema schemas
          value ["2026-02-30T12:00:00.000Z"
                 "2026-02-29T00:00:00.000Z"
                 "1900-02-29T00:00:00.000Z"
                 "2100-02-29T00:00:00.000Z"
                 "2026-04-31T00:00:00.000Z"
                 "2026-06-31T00:00:00.000Z"
                 "2026-09-31T00:00:00.000Z"
                 "2026-11-31T00:00:00.000Z"
                 "2026-00-01T00:00:00.000Z"
                 "2026-13-01T00:00:00.000Z"
                 "2026-01-00T00:00:00.000Z"
                 "2026-01-32T00:00:00.000Z"
                 "2026-01-01T24:00:00.000Z"
                 "2026-01-01T00:60:00.000Z"
                 "2016-12-31T23:59:60.000Z"
                 "2026-01-01T00:00:99.000Z"]]
    (is (not (m/validate schema value)) value)))

(deftest alternate-wire-shapes-remain-refused
  (testing "persistence clocks use fixed-width UTC milliseconds, not arbitrary ISO dates"
    (doseq [schema schemas
            value [nil 0 {} "" "2026-02-28"
                   "2026-02-28T00:00:00Z"
                   "2026-02-28T00:00:00.00Z"
                   "2026-02-28T00:00:00.0000Z"
                   "2026-02-28T00:00:00.000+00:00"
                   "2026-02-28T00:00:00.000z"
                   "+010000-01-01T00:00:00.000Z"
                   "2026-02-28T00:00:00.000Z\n"]]
      (is (not (m/validate schema value)) (pr-str value)))))
