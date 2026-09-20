(ns knoxx.backend.law.persistence-instant
  "Portable calendar validation for the UTC millisecond format of persistence clocks.")

(def ^:private utc-millis-pattern
  #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z")

(defn- leap-year?
  "Use the proleptic Gregorian rule, including century exceptions."
  [year]
  (and (zero? (mod year 4))
       (or (not (zero? (mod year 100)))
           (zero? (mod year 400)))))

(defn- days-in-month
  "Return the maximum day after the caller has validated the month."
  [year month]
  (case month
    2 (if (leap-year? year) 29 28)
    (4 6 9 11) 30
    31))

(defn instant?
  "Accept calendar-valid fixed-width UTC milliseconds without host date normalization.

   Persistence clocks use seconds 00–59, matching their ISO-encoding adapters;
   leap-second text and alternate offsets or precision are not that wire format."
  [value]
  (boolean
   (when (and (string? value) (re-matches utc-millis-pattern value))
     (let [year (parse-long (subs value 0 4))
           month (parse-long (subs value 5 7))
           day (parse-long (subs value 8 10))
           hour (parse-long (subs value 11 13))
           minute (parse-long (subs value 14 16))
           second (parse-long (subs value 17 19))]
       (and (<= 1 month 12)
            (<= 1 day (days-in-month year month))
            (<= 0 hour 23)
            (<= 0 minute 59)
            (<= 0 second 59))))))

(def Instant
  "A calendar-valid UTC timestamp in the existing persistence wire format."
  [:and :string [:fn instant?]])
