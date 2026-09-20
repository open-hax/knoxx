(ns knoxx.backend.infra.stores.run-provider-startup
  "Publish Mongo run authority only after uniqueness and the durable event writer are ready."
  (:require [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.mongo-run-store :as mongo]
            [knoxx.backend.infra.stores.session-store-registry :as registry]))

(defn ^:async install-mongo!
  "Await Mongo's run identity index, then install events before exposing the provider."
  [db]
  (await (mongo/setup-indexes! db))
  (let [provider (mongo/create-mongo-run-store db)]
    (events/install! provider)
    (reset! registry/session-store* provider)))
