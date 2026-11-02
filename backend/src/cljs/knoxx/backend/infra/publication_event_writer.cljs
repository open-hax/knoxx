(ns knoxx.backend.infra.publication-event-writer
  "Provider-owned immutable publication event admission and projection repair."
  (:require [knoxx.backend.extern.mongo :as extern-mongo]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]))

(defn- ^:async repaired-existing-event!
  [client event-id]
  (openplanner-client/assert-event-projection-repair-supported! client)
  {:ok true
   :count 0
   :ids [event-id]
   :existing true
   :index-result (await (openplanner-client/ensure-event-vectors!
                         client [event-id]))})

(defn- ^:async append-event-with-supported-projections!
  [client event]
  (if (openplanner-client/event-projection-repair-supported? client)
    (let [result (await
                  (openplanner-client/ingest-events-awaiting-projections!
                   client [event]))]
      (assoc result
             :index-result
             (await (openplanner-client/ensure-event-vectors!
                     client [(:id event)]))))
    (await (openplanner-client/events! client [event]))))

(defn ^:async persist-openplanner-event!
  "Append one event through the selected provider and await its projections.

  Look up the durable event by producer identity so a serialized deployment
  replay repairs existing projections without appending another source fact.
  Atomic admission across writers remains the provider's responsibility."
  ([config event]
   (await (persist-openplanner-event!
           config (openplanner-client/client config) event)))
  ([_config client event]
   (let [existing (await (openplanner-client/event-by-id! client (:id event)))]
     (if existing
       (await (repaired-existing-event! client (:id event)))
       (await (append-event-with-supported-projections! client event))))))

(defn ^:async persist-one!
  "Persist a scoped admission fact and report its durable event status."
  [persist-event! document-id phase event]
  (try
    (let [result (await (persist-event! event))]
      {:event/id (:id event)
       :event/status (if (:existing result) :existing :recorded)
       :event/result result})
    (catch :default err
      (if (extern-mongo/duplicate-key-error? err)
        {:event/id (:id event)
         :event/status :existing}
        (throw
         (ex-info "publication document event persistence failed"
                  {:status 502
                   :code "document_admission_event_failed"
                   :document/id document-id
                   :document/admission-phase phase
                   :event/id (:id event)}
                  err))))))
