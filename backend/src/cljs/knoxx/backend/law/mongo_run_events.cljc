(ns knoxx.backend.law.mongo-run-events
  "Bounded immutable event references and the versioned single-run commit head."
  (:require [knoxx.backend.law.run-event :as event]
            [knoxx.backend.law.run-store :as run]
            [malli.core :as m]))

(def fragment-bytes 65536)
(def fragment-record-bytes (* 96 1024))
(def header-record-bytes 4096)
(def run-record-bytes (- (* 16 1024 1024) 1024))
(def Digest [:re #"^[a-f0-9]{64}$"])
(def Uuid [:re #"^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"])
(def Chain [:map {:closed true} [:tail [:maybe Digest]]
            [:last-sequence [:int {:min 0 :max event/max-sequence}]]])
(def empty-chain {:tail nil :last-sequence 0})
(def Envelope [:map {:closed true} [:format [:= :knoxx.run/v2]]
               [:runs :map] [:bindings :map] [:event-chain Chain]])
(def Header [:map {:closed true} [:nonce Uuid] [:run-key Digest] [:event-key Digest]
             [:sequence event/Sequence] [:previous [:maybe Digest]]
             [:first-fragment Uuid] [:fragment-count [:int {:min 1 :max event/max-sequence}]]
             [:encoded-bytes [:int {:min 2 :max event/max-sequence}]] [:payload-digest Digest]])
(def Fragment [:map {:closed true} [:_id Uuid] [:event-nonce Uuid]
               [:ordinal [:int {:min 0 :max event/max-sequence}]] [:next [:maybe Uuid]]
               [:data [:string {:min 4 :max 87384}]]])

(defn corrupt!
  "Refuse incomplete or inconsistent durable event authority."
  []
  (throw (ex-info "Invalid durable Mongo event chain"
                  {:status 503 :code "run_store_corrupt"})))

(defn require!
  "Validate stored metadata as unavailable authority, not as invalid caller input."
  [schema value]
  (when-not (m/validate schema value) (corrupt!))
  value)

(defn chain!
  "A zero sequence has no tail; a nonempty history always names a bounded digest."
  [chain]
  (require! Chain chain)
  (when-not (= (zero? (:last-sequence chain)) (nil? (:tail chain))) (corrupt!))
  chain)

(defn header!
  "Require exact owner, predecessor sequence and complete fixed-size fragment geometry."
  [header run-key sequence]
  (require! Header header)
  (when-not (and (= run-key (:run-key header)) (= sequence (:sequence header))
                 (= (= 1 sequence) (nil? (:previous header)))
                 (even? (:encoded-bytes header))
                 (= (:fragment-count header) (quot (+ (:encoded-bytes header) (dec fragment-bytes)) fragment-bytes)))
    (corrupt!))
  header)

(defn stored-event!
  "Bind a reconstructed immutable event to the run and its committed sequence."
  [binding sequence value]
  (require! run/Event value)
  (require! run/NonBlank (:event_id value))
  (when-not (and (= sequence (:sequence value))
                 (= (select-keys binding [:run_id :session_id :conversation_id])
                    (select-keys value [:run_id :session_id :conversation_id])))
    (corrupt!))
  value)

(defn envelope
  "Exclude materialized event vectors from every new-format atomic run head."
  [state chain]
  (chain! chain)
  {:format :knoxx.run/v2 :runs (:runs state) :bindings (:bindings state) :event-chain chain})

(defn unpack
  "Recover common run state and its immutable history reference without loading history."
  [encoded]
  (require! Envelope encoded)
  (chain! (:event-chain encoded))
  {:state {:runs (:runs encoded) :bindings (:bindings encoded) :events {}}
   :event-chain (:event-chain encoded)})
