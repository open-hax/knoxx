(ns knoxx.backend.infra.routes.document-admission-mongo-test
  (:require ["@open-hax/openplanner-sdk" :as sdk-mod]
            [cljs.test :as test]
            [knoxx.backend.extern.openplanner-sdk :as xsdk]
            [knoxx.backend.infra.clients.openplanner-mongo :as openplanner-mongo]
            [knoxx.backend.infra.routes.document-admission :as admission]))

(test/deftest ^:async mongo-openplanner-persistence-detects-a-replay
  (let [client (openplanner-mongo/client {} nil)
        event {:schema "openplanner.event.v1"
               :id "knoxx-document-admission-real-adapter-replay"
               :ts "2026-09-02T12:00:00.000Z"
               :source "knoxx-publication"
               :kind "docs"
               :source_ref {:project "knoxx-local"
                            :message "knoxx.docs/replay"}
               :text "# Replay"}
        first-result (await (admission/persist-openplanner-event! {} client event))
        retry-result (await (admission/persist-openplanner-event! {} client event))]
    (test/is (true? (:ok first-result)))
    (test/is (not (:existing first-result)))
    (test/is (true? (:existing retry-result)))
    (test/is (= [(:id event)] (:ids retry-result)))))

(test/deftest ^:async mongo-openplanner-replay-repairs-a-missing-vector
  (sdk-mod/__setEventVectorMode "missing")
  (try
    (let [event {:schema "openplanner.event.v1"
                 :id "knoxx-document-admission-real-adapter-vector-repair"
                 :ts "2026-09-02T12:00:00.000Z"
                 :source "knoxx-publication"
                 :kind "docs"
                 :source_ref {:project "knoxx-local"
                              :message "knoxx.docs/vector-repair"}
                 :text "# Repair this durable document event"}
          _ (await (xsdk/events! [event]))
          result (await (admission/persist-openplanner-event!
                         {} (openplanner-mongo/client {} nil) event))
          stored (await (xsdk/mongo-query
                         {:collection "events"
                          :filter {:id (:id event)}}))]
      (test/is (true? (:existing result)))
      (test/is (= [(:id event)]
             (get-in result [:index-result :repaired-event-ids])))
      (test/is (= 1 (get-in result [:index-result :vector-count])))
      (test/is (= 1 (:total stored))
          "repair must not append a second immutable base event"))
    (finally
      (sdk-mod/__setEventVectorMode "valid"))))
