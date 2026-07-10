(ns knoxx.backend.infra.clients.openplanner-mongo
  "Direct-mongo OpenPlanner client.

   Data-plane operations (events, sessions, vector search, mongo browse,
   health) run in-process through @open-hax/openplanner-sdk — direct MongoDB
   with self-sourced embeddings, no REST hop. Operations not yet ported
   delegate to the wrapped REST client, which also remains the transport for
   external consumers of the OpenPlanner API.

   Loading this namespace registers the record constructor with the client
   factory in knoxx.backend.infra.clients.openplanner (which cannot require
   this namespace without creating a cycle)."
  (:require [knoxx.backend.extern.openplanner-sdk :as xsdk]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]))

(defrecord MongoOpenPlannerClient [config rest-client]
  openplanner-client/IOpenPlannerClient
  (enabled? [_]
    ;; The mongo data plane configures itself from env with working defaults;
    ;; REST-delegated operations still check the wrapped client themselves.
    true)
  (health! [_]
    (xsdk/health))
  (events! [_ events]
    (xsdk/events! events))
  (session! [_ session-id opts]
    (xsdk/session session-id opts))
  (sessions! [_ opts]
    (xsdk/sessions opts))
  (vector-search! [_ payload]
    (xsdk/vector-search! payload))
  (mongo-collections! [_]
    (xsdk/mongo-collections))
  (mongo-query! [_ payload]
    (xsdk/mongo-query payload))
  (graph-memory! [_ payload]
    (openplanner-client/graph-memory! rest-client payload))
  (graph-export! [_ opts]
    (openplanner-client/graph-export! rest-client opts))
  (upsert-document! [_ document]
    (openplanner-client/upsert-document! rest-client document))
  (documents-stats! [_]
    (openplanner-client/documents-stats! rest-client))
  (graph-monitoring! [_]
    (openplanner-client/graph-monitoring! rest-client))
  (build-semantic-edges! [_ payload]
    (openplanner-client/build-semantic-edges! rest-client payload))
  (record-labels! [_ record-ids]
    (openplanner-client/record-labels! rest-client record-ids))
  (record-reaction! [_ record-id payload]
    (openplanner-client/record-reaction! rest-client record-id payload))
  (translation-segments! [_ opts]
    (openplanner-client/translation-segments! rest-client opts))
  (translation-segment! [_ segment-id]
    (openplanner-client/translation-segment! rest-client segment-id))
  (create-translation-segment! [_ segment]
    (openplanner-client/create-translation-segment! rest-client segment))
  (label-translation-segment! [_ segment-id payload]
    (openplanner-client/label-translation-segment! rest-client segment-id payload))
  (translation-export-manifest! [_ project]
    (openplanner-client/translation-export-manifest! rest-client project))
  (translation-export-sft! [_ opts]
    (openplanner-client/translation-export-sft! rest-client opts))
  (create-translation-segments-batch! [_ payload]
    (openplanner-client/create-translation-segments-batch! rest-client payload))
  (translation-documents! [_ opts]
    (openplanner-client/translation-documents! rest-client opts))
  (translation-document! [_ document-id target-lang]
    (openplanner-client/translation-document! rest-client document-id target-lang))
  (review-translation-document! [_ document-id target-lang payload]
    (openplanner-client/review-translation-document! rest-client document-id target-lang payload))
  (create-translation-batch! [_ payload]
    (openplanner-client/create-translation-batch! rest-client payload))
  (translation-batches! [_ opts]
    (openplanner-client/translation-batches! rest-client opts))
  (next-translation-batch! [_]
    (openplanner-client/next-translation-batch! rest-client))
  (translation-batch! [_ batch-id]
    (openplanner-client/translation-batch! rest-client batch-id))
  (update-translation-batch-status! [_ batch-id payload]
    (openplanner-client/update-translation-batch-status! rest-client batch-id payload))
  (v1-json! [_ method path body]
    (openplanner-client/v1-json! rest-client method path body))
  (forward-v1! [_ request]
    (openplanner-client/forward-v1! rest-client request)))

(defn client
  "Build a direct-mongo OpenPlanner client wrapping a REST client for the
   operations that have not been ported off the API yet."
  [config rest-client]
  (->MongoOpenPlannerClient config rest-client))

;; Register with the factory so (openplanner-client/client config) returns the
;; direct-mongo record whenever mongo mode is selected.
(openplanner-client/register-direct-client-factory! client)
