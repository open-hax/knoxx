(ns knoxx.backend.law.mailbox-store
  "Finite contracts for tenant-owned messages, delivery leases and acknowledgements."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(def NonBlank [:and string? [:fn #(not (str/blank? %))]])
(def Instant [:and string? [:re #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$"]])
(def Scope [:map {:closed true} [:org-id NonBlank] [:actor-id [:maybe NonBlank]] [:admin? boolean?]])
(def Address
  [:map {:closed true} [:kind {:optional true} NonBlank] [:address {:optional true} NonBlank]
   [:target {:optional true} NonBlank] [:target-type {:optional true} NonBlank]
   [:actor-id {:optional true} NonBlank] [:session-id {:optional true} NonBlank]
   [:conversation-id {:optional true} NonBlank] [:run-id {:optional true} NonBlank]
   [:contract-id {:optional true} NonBlank]])
(def MessageIntent
  [:map {:closed true} [:source Address] [:content NonBlank] [:metadata map?]
   [:mode [:enum "follow-up" "steer" "event" "inbox-only"]]
   [:target [:map {:closed true} [:target NonBlank]
             [:target-type {:optional true} NonBlank]
             [:conversation-id {:optional true} NonBlank] [:session-id {:optional true} NonBlank]
             [:run-id {:optional true} NonBlank]]]])
(def EntryInput
  [:map {:closed true} [:mailbox/id NonBlank] [:mailbox/kind NonBlank]
   [:mailbox/source Address] [:mailbox/target Address]
   [:mailbox/delivery [:map {:closed true} [:mode [:enum "follow-up" "steer" "event" "inbox-only" "direct-run"]]]]
   [:mailbox/content-ref map?] [:mailbox/metadata map?]
   [:mailbox/content {:optional true} string?]
   [:mailbox/intent {:optional true} MessageIntent]
   [:mailbox/preview {:optional true} [:string {:max 241}]]
   [:mailbox/expires-at {:optional true} Instant]])
(def Route
  [:map {:closed true} [:actor-id NonBlank] [:conversation-id NonBlank]
   [:session-id {:optional true} [:maybe NonBlank]] [:run-id {:optional true} [:maybe NonBlank]]
   [:contract-id {:optional true} [:maybe NonBlank]] [:source {:optional true} map?]
   [:ttl-seconds {:optional true} [:int {:min 1 :max 86400}]]])
(def Filters
  [:map {:closed true} [:status {:optional true} NonBlank]
   [:limit {:optional true} [:int {:min 1 :max 200}]]
   [:source-actor-id {:optional true} NonBlank] [:target-actor-id {:optional true} NonBlank]
   [:target-session-id {:optional true} NonBlank] [:source-run-id {:optional true} NonBlank]])
(def ClaimOptions
  [:map {:closed true} [:mailbox-id {:optional true} NonBlank] [:operation-id {:optional true} NonBlank]
   [:statuses {:optional true} [:vector [:enum "pending" "failed"]]]
   [:max-attempts {:optional true} [:int {:min 1 :max 100}]]
   [:limit {:optional true} [:int {:min 1 :max 200}]]
   [:delay-seconds {:optional true} [:int {:min 0 :max 86400}]]
   [:lease-ms {:optional true} [:int {:min 1000 :max 3600000}]]])
(def MarkOptions
  [:map {:closed true} [:claim-id NonBlank] [:operation-id {:optional true} NonBlank]
   [:content-ref {:optional true} map?] [:error {:optional true} string?]])
(def SendRequest
  [:map {:closed true} [:operation-id NonBlank] [:target NonBlank] [:content NonBlank]
   [:mode {:optional true} [:enum "message" "follow-up" "steer" "event" "inbox-only"]]
   [:target-type {:optional true} NonBlank] [:conversation-id {:optional true} NonBlank]
   [:session-id {:optional true} NonBlank] [:run-id {:optional true} NonBlank]
   [:metadata {:optional true} map?] [:lineage {:optional true} map?]])
(def DeliveryRequest
  [:map {:closed true} [:mode [:enum "follow-up" "steer" "event" "inbox-only"]]
   [:target Address] [:content NonBlank] [:metadata map?] [:mailbox-id NonBlank]])
(def Operation
  [:map {:closed true} [:kind [:enum :create :register :unregister :claim :mark :ack]]
   [:scope Scope] [:id NonBlank] [:at Instant] [:payload map?]
   [:lease-until {:optional true} Instant] [:next-at {:optional true} Instant]
   [:route-expires-at {:optional true} Instant]])

(defn refuse!
  "Raise a classified domain or admission failure without side effects."
  [status code message]
  (throw (ex-info message {:status status :code code})))

(defn checked!
  "Validate one mailbox boundary value before admitting effects."
  [contract schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid mailbox data"
                    {:status 400 :code "mailbox_contract_invalid" :contract contract
                     :errors (me/humanize (m/explain schema value))})))
  value)
