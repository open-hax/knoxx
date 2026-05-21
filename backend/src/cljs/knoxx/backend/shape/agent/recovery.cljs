(ns knoxx.backend.shape.agent.recovery
  "Schemas for startup and interval recovery of agent sessions/runs."
  (:require [malli.core :as m]
            [malli.error :as me]))

(def RecoveryReason
  [:enum :startup :stale-running :post-drain :manual :unknown])

(def RecoveryStatus
  [:enum :pending :resumed :completed :failed :skipped])

(def RecoveryRequest
  [:map {:closed false}
   [:session-id {:optional true} [:maybe :string]]
   [:conversation-id {:optional true} [:maybe :string]]
   [:run-id {:optional true} [:maybe :string]]
   [:reason {:optional true} RecoveryReason]
   [:session {:optional true} [:map {:closed false}]]
   [:wait-for {:optional true} int?]])

(def RecoveryResult
  [:map {:closed false}
   [:session_id {:optional true} [:maybe :string]]
   [:session-id {:optional true} [:maybe :string]]
   [:conversation_id {:optional true} [:maybe :string]]
   [:conversation-id {:optional true} [:maybe :string]]
   [:resumed :boolean]
   [:reason {:optional true} [:or RecoveryReason :string]]
   [:error {:optional true} [:maybe :string]]
   [:wait_for {:optional true} int?]
   [:wait-for {:optional true} int?]])

(def RecoveryRecord
  [:map {:closed false}
   [:session-id :string]
   [:conversation-id {:optional true} [:maybe :string]]
   [:run-id {:optional true} [:maybe :string]]
   [:status RecoveryStatus]
   [:reason {:optional true} RecoveryReason]
   [:started-at {:optional true} [:or :string int?]]
   [:completed-at {:optional true} [:or :string int?]]
   [:error {:optional true} [:maybe :string]]
   [:session {:optional true} [:map {:closed false}]]])

(defn valid-recovery-record?
  [value]
  (m/validate RecoveryRecord value))

(defn explain-recovery-record
  [value]
  (me/humanize (m/explain RecoveryRecord value)))
