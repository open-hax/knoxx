(ns knoxx.backend.infra.openplanner-fixture
  "Complete finite OpenPlanner protocol fixture; unseeded operations fail explicitly."
  (:require [knoxx.backend.infra.clients.openplanner :as protocol]))

(defn- invoke [handlers operation arguments]
  (if-let [handler (get handlers operation)]
    (apply handler arguments)
    (throw (ex-info "Unseeded OpenPlanner fixture operation" {:operation operation}))))

(defrecord CallbackClient [handlers]
  protocol/IOpenPlannerClient
  (enabled? [client] (invoke handlers :enabled? [client]))
  (health! [client] (invoke handlers :health! [client]))
  (events! [client events] (invoke handlers :events! [client events]))
  (session! [client session-id opts] (invoke handlers :session! [client session-id opts]))
  (sessions! [client opts] (invoke handlers :sessions! [client opts]))
  (vector-search! [client payload] (invoke handlers :vector-search! [client payload]))
  (graph-memory! [client payload] (invoke handlers :graph-memory! [client payload]))
  (graph-export! [client opts] (invoke handlers :graph-export! [client opts]))
  (upsert-document! [client document] (invoke handlers :upsert-document! [client document]))
  (documents-stats! [client] (invoke handlers :documents-stats! [client]))
  (graph-monitoring! [client] (invoke handlers :graph-monitoring! [client]))
  (mongo-collections! [client] (invoke handlers :mongo-collections! [client]))
  (mongo-query! [client payload] (invoke handlers :mongo-query! [client payload]))
  (build-semantic-edges! [client payload] (invoke handlers :build-semantic-edges! [client payload]))
  (record-labels! [client record-ids] (invoke handlers :record-labels! [client record-ids]))
  (record-reaction! [client record-id payload] (invoke handlers :record-reaction! [client record-id payload]))
  (translation-segments! [client opts] (invoke handlers :translation-segments! [client opts]))
  (translation-segment! [client segment-id opts] (invoke handlers :translation-segment! [client segment-id opts]))
  (create-translation-segment! [client segment] (invoke handlers :create-translation-segment! [client segment]))
  (label-translation-segment! [client segment-id payload] (invoke handlers :label-translation-segment! [client segment-id payload]))
  (translation-export-manifest! [client opts] (invoke handlers :translation-export-manifest! [client opts]))
  (translation-export-sft! [client opts] (invoke handlers :translation-export-sft! [client opts]))
  (create-translation-segments-batch! [client payload] (invoke handlers :create-translation-segments-batch! [client payload]))
  (translation-documents! [client opts] (invoke handlers :translation-documents! [client opts]))
  (translation-document! [client document-id target-lang opts] (invoke handlers :translation-document! [client document-id target-lang opts]))
  (review-translation-document! [client document-id target-lang payload] (invoke handlers :review-translation-document! [client document-id target-lang payload]))
  (create-translation-batch! [client payload] (invoke handlers :create-translation-batch! [client payload]))
  (translation-batches! [client opts] (invoke handlers :translation-batches! [client opts]))
  (next-translation-batch! [client opts] (invoke handlers :next-translation-batch! [client opts]))
  (translation-batch! [client batch-id opts] (invoke handlers :translation-batch! [client batch-id opts]))
  (update-translation-batch-status! [client batch-id payload] (invoke handlers :update-translation-batch-status! [client batch-id payload]))
  (v1-json! [client method path body] (invoke handlers :v1-json! [client method path body]))
  (forward-v1! [client request] (invoke handlers :forward-v1! [client request])))

(defn client
  "Create a protocol-complete provider with only explicitly seeded responses."
  [handlers] (->CallbackClient handlers))
