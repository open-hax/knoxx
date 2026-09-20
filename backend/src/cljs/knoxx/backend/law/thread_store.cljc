(ns knoxx.backend.law.thread-store
  "Portable contracts for durable conversation state and stamped admissions."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def NonBlank [:and :string [:fn #(not (str/blank? %))]])
(def Milliseconds [:and :int [:fn #(<= 0 %)]])
(def Instant [:and NonBlank [:re #"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z$"]])
(def active-statuses #{"running" "queued" "waiting_input"})

(defn edn-value?
  "Accept inspectable portable EDN and refuse opaque runtime handles."
  [value]
  (cond
    (or (nil? value) (string? value) (boolean? value) (keyword? value)) true
    (number? value) (and (= value value) (not (#{##Inf ##-Inf} value)))
    (map? value) (every? #(and (edn-value? (key %)) (edn-value? (val %))) value)
    (or (vector? value) (list? value) (set? value)) (every? edn-value? value)
    :else false))

(def DataMap [:and :map [:fn edn-value?]])
(def Thread
  [:and [:map [:session_id NonBlank]
         [:conversation_id {:optional true} [:maybe NonBlank]]
         [:status {:optional true} :string]]
   [:fn edn-value?]])
(def Stamp
  [:and [:map {:closed true} [:at Instant] [:at-ms Milliseconds]
         [:expires-at Instant] [:expires-ms Milliseconds] [:instance-id NonBlank]]
   [:fn #(< (:at-ms %) (:expires-ms %))]])
(def Operation
  [:multi {:dispatch :kind}
   [:put [:map {:closed true} [:kind [:= :put]] [:thread-id NonBlank] [:thread Thread] [:stamp Stamp]]]
   [:patch [:map {:closed true} [:kind [:= :patch]] [:thread-id NonBlank] [:patch DataMap] [:stamp Stamp]]]
   [:rewind [:map {:closed true} [:kind [:= :rewind]] [:thread-id NonBlank]
             [:turns [:and :int [:fn pos?]]] [:stamp Stamp]]]
   [:delete [:map {:closed true} [:kind [:= :delete]] [:thread-id NonBlank] [:stamp Stamp]]]])

(defn assert-valid!
  "Return boundary data or a classified refusal before state can change."
  [contract schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid thread persistence data"
                    {:status 400 :code "thread_store_invalid" :contract contract})))
  value)

(defn ttl-ms
  "Normal conversations expire after one hour; sticky IDs after 24 hours."
  [thread-id]
  (* 1000 3600 (if (str/includes? (str thread-id) "-sticky") 24 1)))
