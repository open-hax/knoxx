(ns knoxx.backend.infra.clients.openplanner-mongo
  "Direct-mongo OpenPlanner client.

   Data-plane operations (events, sessions, vector search, mongo browse,
   health) run in-process through @open-hax/openplanner-sdk — direct MongoDB
   with self-sourced embeddings, no REST hop. Translation is also a direct
   Mongo projection; only unrelated legacy operations delegate to REST.

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
    (xsdk/translation-segments! opts))
  (translation-segment! [_ segment-id opts]
    (xsdk/translation-segment! segment-id opts))
  (create-translation-segment! [_ segment]
    (xsdk/create-translation-segment! segment))
  (label-translation-segment! [_ segment-id payload]
    (xsdk/label-translation-segment! segment-id payload))
  (translation-export-manifest! [_ opts]
    (xsdk/translation-export-manifest! opts))
  (translation-export-sft! [_ opts]
    (xsdk/translation-export-sft! opts))
  (create-translation-segments-batch! [_ payload]
    (xsdk/create-translation-segments-batch! payload))
  (translation-documents! [_ opts]
    (xsdk/translation-documents! opts))
  (translation-document! [_ document-id target-lang opts]
    (xsdk/translation-document! document-id target-lang opts))
  (review-translation-document! [_ document-id target-lang payload]
    (xsdk/review-translation-document! document-id target-lang payload))
  (create-translation-batch! [_ payload]
    (xsdk/create-translation-batch! payload))
  (translation-batches! [_ opts]
    (xsdk/translation-batches! opts))
  (next-translation-batch! [_ opts]
    (xsdk/next-translation-batch! opts))
  (translation-batch! [_ batch-id opts]
    (xsdk/translation-batch! batch-id opts))
  (update-translation-batch-status! [_ batch-id payload]
    (xsdk/update-translation-batch-status! batch-id payload))
  (v1-json! [_ method path body]
    (openplanner-client/v1-json! rest-client method path body))
  (forward-v1! [_ request]
    (openplanner-client/forward-v1! rest-client request)))

(defn client
  "Build a direct-mongo OpenPlanner client. REST remains only for legacy
   non-translation operations that have not been ported into the SDK."
  [config rest-client]
  (->MongoOpenPlannerClient config rest-client))

;; Register with the factory so (openplanner-client/client config) returns the
;; direct-mongo record whenever mongo mode is selected.
(openplanner-client/register-direct-client-factory! client)
