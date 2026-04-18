(ns kms-ingestion.drivers.registry
  "Driver registry for creating driver instances."
  (:require
   [kms-ingestion.drivers.protocol :as protocol]
   [kms-ingestion.drivers.local :as local]
   [kms-ingestion.drivers.pi-sessions :as pi-sessions]
   [kms-ingestion.drivers.promptdb :as promptdb]))

(def driver-constructors
  {"local"      local/create-driver
   "pi-sessions" pi-sessions/create-driver
   ;; Structured EDN epistemic ingestion — no chunking, schema-validated records
   "promptdb"   promptdb/create-driver
   ;; Future drivers:
   ;; "github"       github/create-driver
   ;; "google_drive" google-drive/create-driver
   })

(defn list-drivers
  "List available driver types."
  []
  (keys driver-constructors))

(defn create-driver
  "Create a driver instance for the given type and config."
  [driver-type config]
  (if-let [constructor (get driver-constructors driver-type)]
    (constructor config)
    (throw (ex-info (str "Unknown driver type: " driver-type)
                    {:driver-type driver-type
                     :available   (list-drivers)}))))
