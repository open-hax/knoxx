(ns knoxx.backend.law.mongo
  "Malli contracts for scalars decoded at the Mongo boundary.

   Contract policy only — no I/O. extern.mongo calls these on the way out, so
   the admissible shape of a decoded value is stated in one place rather than
   re-derived by each caller that consumes it."
  (:require [clojure.string :as str]
            [malli.core :as m]))

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

(def FieldEqualityQuery
  "A field-equality query handed to the Mongo adapter.

   Non-empty is the load-bearing part. An empty query matches every document
   in the collection, so on a delete path it is the difference between
   removing one authorization code and removing all of them. Values are
   scalars because this shape is equality matching only — an operator map is
   a different contract and does not belong on these call sites."
  [:and
   [:map-of keyword? [:or string? number? boolean?]]
   [:fn {:error/message "must name at least one field"} seq]])

(def DecodedDocument
  "A document decoded out of the driver: CLJS data with keyword keys, or
   nothing at all when the query matched none."
  [:maybe [:map-of keyword? any?]])

(def NonBlankIdentifier
  "A canonical identifier safe to use as the exclusion anchor of a bulk
   Mongo mutation. Empty identifiers are forbidden because `$ne \"\"` would
   select every normally persisted document."
  [:and string? [:fn {:error/message "must be nonblank"} (complement str/blank?)]])

(def TransactionTopology
  "A Mongo deployment topology that supports multi-document transactions.

   Standalone servers are intentionally absent. Bootstrap credential rotation
   deactivates an old credential and installs its replacement as one fact; a
   topology that cannot commit both writes atomically cannot satisfy that
   contract."
  [:map
   [:kind [:enum :replica-set :sharded-cluster]]
   [:set-name {:optional true} NonBlankIdentifier]])

(defn- field-name? [field]
  (and (keyword? field)
       (not (str/starts-with? (name field) "$"))))

(defn- mutation-field-name [field]
  (cond
    (keyword? field) (name field)
    (string? field) field
    :else nil))

(def MutationDateFields
  "The nonempty set of mutation fields whose epoch-millisecond values the
   Mongo extern must encode as BSON Dates.

   Field names remain CLJS keywords or strings upstream. Dollar-prefixed names
   are operators, not document fields, and are refused here."
  [:and
   [:set [:or keyword? string?]]
   [:fn {:error/message "must name at least one ordinary mutation field"}
    (fn [fields]
      (and (seq fields)
           (every? (fn [field]
                     (let [field-name (mutation-field-name field)]
                       (and (not (str/blank? field-name))
                            (not (str/starts-with? field-name "$")))))
                   fields)))]])

(declare mutation-query-shape?)

(defn- field-condition? [condition]
  (or (string? condition)
      (number? condition)
      (boolean? condition)
      (nil? condition)
      (and (map? condition)
           (= 1 (count condition))
           (let [[operator operand] (first condition)]
             (case operator
               :$in (and (vector? operand)
                         (every? #(or (string? %)
                                     (number? %)
                                     (boolean? %)
                                     (nil? %))
                                 operand))
               :$exists (boolean? operand)
               :$ne (or (string? operand)
                        (number? operand)
                        (boolean? operand)
                        (nil? operand))
               false)))))

(defn- mutation-query-clause? [[field condition]]
  (if (= :$or field)
    (and (vector? condition)
         (seq condition)
         (every? mutation-query-shape? condition))
    (and (field-name? field)
         (field-condition? condition))))

(defn- mutation-query-shape? [query]
  (and (map? query)
       (seq query)
       (every? mutation-query-clause? query)))

(def MutationQuery
  "A nonempty update query composed only from field equality, `$in`, `$ne`,
   `$exists`, and nonempty `$or` clauses.

   Generic mutation adapters must reject `{}` because Mongo interprets it as
   every document. Keeping the operator allow-list here also prevents a caller
   from smuggling executable or otherwise unreviewed query operators through a
   supposedly CLJS-first boundary."
  [:fn {:error/message "must be a nonempty admissible mutation query"}
   mutation-query-shape?])

(def MutationUpdate
  "A nonempty Mongo update document containing nonempty `$set`, `$unset`, or
   `$setOnInsert` field maps.

   Replacement documents and arbitrary update operators are deliberately not
   part of this adapter's contract."
  [:and
   [:map-of
    [:enum :$set :$unset :$setOnInsert "$set" "$unset" "$setOnInsert"]
    [:and
     [:map-of [:or keyword? string?] any?]
     [:fn {:error/message "mutation operator must set at least one field"} seq]]]
   [:fn {:error/message "must contain at least one mutation operator"} seq]])

(defn valid-query?
  [query]
  (m/validate FieldEqualityQuery query))

(defn valid-document?
  [doc]
  (m/validate DecodedDocument doc))

(defn valid-identifier?
  "Whether value is a nonblank canonical identifier."
  [value]
  (m/validate NonBlankIdentifier value))

(defn transaction-capable-topology?
  "Whether a decoded Mongo hello result names a transaction-capable topology."
  [topology]
  (m/validate TransactionTopology topology))

(defn valid-mutation-query?
  "Whether query is safe for the generic update boundary."
  [query]
  (m/validate MutationQuery query))

(defn valid-mutation-update?
  "Whether update is an admissible nonempty mutation document."
  [update]
  (m/validate MutationUpdate update))

(defn valid-mutation-date-encoding?
  "Whether every declared BSON Date field exists in update and contains a
   valid epoch-millisecond scalar.

   Nil means no Date encoding was requested. Requiring every declared field to
   exist makes a misspelled field fail closed instead of silently persisting a
   number where a BSON Date was intended."
  [update fields]
  (or
   (nil? fields)
   (and
    (m/validate MutationDateFields fields)
    (every?
     (fn [field]
       (let [target (mutation-field-name field)
             values (for [[_ assignments] update
                          [candidate value] assignments
                          :when (= target (mutation-field-name candidate))]
                      value)]
         (and (seq values)
              (every? valid-epoch-ms? values))))
     fields))))
