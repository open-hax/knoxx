(ns knoxx.backend.infra.clients.openplanner-mongo
  "Direct-mongo OpenPlanner client.

   Data-plane operations (events, sessions, vector search, mongo browse,
   health) run in-process through @open-hax/openplanner-sdk. Translation runs
   through Knoxx's ClojureScript Mongo boundary and OpenPlanner's existing
   ClojureScript translation domain rules. Only unrelated legacy operations
   delegate to REST.

   Loading this namespace registers the record constructor with the client
   factory in knoxx.backend.infra.clients.openplanner (which cannot require
   this namespace without creating a cycle)."
  (:require [knoxx.backend.extern.openplanner-sdk :as xsdk]
            [knoxx.backend.extern.openplanner-translation-mongo :as translations]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]))

(defrecord MongoOpenPlannerClient [config rest-client]
  openplanner-client/IOpenPlannerClient
  (enabled? [_]
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
    (translations/list-segments! opts))
  (translation-segment! [_ segment-id opts]
    (translations/segment! segment-id opts))
  (create-translation-segment! [_ segment]
    (translations/create-segment! segment))
  (label-translation-segment! [_ segment-id payload]
    (translations/label-segment! segment-id payload))
  (translation-export-manifest! [_ opts]
    (translations/manifest! opts))
  (translation-export-sft! [_ opts]
    (translations/export-sft! opts))
  (create-translation-segments-batch! [_ payload]
    (translations/create-segments-batch! payload))
  (translation-documents! [_ opts]
    (translations/documents! opts))
  (translation-document! [_ document-id target-lang opts]
    (translations/document! document-id target-lang opts))
  (review-translation-document! [_ document-id target-lang payload]
    (translations/review-document! document-id target-lang payload))
  (create-translation-batch! [_ payload]
    (translations/create-batch! payload))
  (translation-batches! [_ opts]
    (translations/list-batches! opts))
  (next-translation-batch! [_ opts]
    (translations/next-batch! opts))
  (translation-batch! [_ batch-id opts]
    (translations/batch! batch-id opts))
  (update-translation-batch-status! [_ batch-id payload]
    (translations/update-batch! batch-id payload))
  (v1-json! [_ method path body]
    (openplanner-client/v1-json! rest-client method path body))
  (forward-v1! [_ request]
    (openplanner-client/forward-v1! rest-client request)))

(defn client
  "Build a direct-mongo OpenPlanner client. Translation stays in Knoxx CLJS;
   REST remains only for unrelated legacy operations."
  [config rest-client]
  (->MongoOpenPlannerClient config rest-client))

(openplanner-client/register-direct-client-factory! client)
