(ns knoxx.backend.law.mongo
  "Malli contracts for scalars decoded at the Mongo boundary.

   Contract policy only — no I/O. extern.mongo calls these on the way out, so
   the admissible shape of a decoded value is stated in one place rather than
   re-derived by each caller that consumes it."
  (:require [malli.core :as m]))

(def max-time-value
  "ECMAScript's maximum time value: ±100,000,000 days from the epoch.
   A number outside this cannot name an instant, and js/Date rejects it too."
  8.64e15)

(def EpochMillis
  "A decoded instant, in milliseconds since the epoch.

   The bounds do the work that a bare number? cannot. Infinity is a number and
   compares greater than every clock reading, so an expiry carrying it would
   leave a credential live for good; NaN is a number whose comparisons are all
   false, which fails safe here but only by accident. Both are outside the
   range and both are rejected by it."
  [:and number? [:>= (- max-time-value)] [:<= max-time-value]])

(defn valid-epoch-ms?
  [ms]
  (m/validate EpochMillis ms))
