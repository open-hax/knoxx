(ns knoxx.backend.law.thread-store
  "Portable contracts for durable conversation state and stamped admissions."
  (:require [clojure.string :as str]
            [knoxx.backend.law.persistence-instant :as instant]
            [malli.core :as m]))

(def NonBlank [:and :string [:fn #(not (str/blank? %))]])
(def Milliseconds [:and :int [:fn #(<= 0 %)]])
(def Instant instant/Instant)
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

(def identity-fields
  "Canonical slots whose non-null values cannot change within a live thread."
  [:conversation_id :org_id :user_id])

(defn canonical-identity-keys?
  "Refuse aliases that encode to a canonical identity field at a native boundary."
  [value]
  (every? (fn [key]
            (or (not (or (keyword? key) (string? key) (symbol? key)))
                (not (contains? #{"session_id" "conversation_id" "org_id" "user_id"}
                                (first (str/split (name key) #"\."))))
                (contains? #{:session_id :conversation_id :org_id :user_id} key)))
          (keys value)))

(def DataMap [:and :map [:fn edn-value?]])
(def Thread
  [:and [:map [:session_id NonBlank]
         [:conversation_id {:optional true} [:maybe NonBlank]]
         [:org_id {:optional true} [:maybe NonBlank]]
         [:user_id {:optional true} [:maybe NonBlank]]
         [:status {:optional true} :string]]
   [:fn edn-value?] [:fn canonical-identity-keys?]])
(def Stamp
  [:and [:map {:closed true} [:at Instant] [:at-ms Milliseconds]
         [:expires-at Instant] [:expires-ms Milliseconds] [:instance-id NonBlank]]
   [:fn #(< (:at-ms %) (:expires-ms %))]])
(def Operation
  [:multi {:dispatch :kind}
   [:startup [:map {:closed true} [:kind [:= :startup]] [:thread-id NonBlank]
              [:thread Thread] [:expected :map] [:phase [:enum :claim :settle]] [:stamp Stamp]]]
   [:recovery [:map {:closed true} [:kind [:= :recovery]] [:thread-id NonBlank]
               [:observed Thread] [:expected :map]
               [:stamp Stamp]]]
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
