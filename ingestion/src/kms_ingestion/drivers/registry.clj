(ns kms-ingestion.drivers.registry
  "Driver registry for creating driver instances."
  (:require
   [kms-ingestion.drivers.local :as local]
   [kms-ingestion.drivers.eta-mu-sessions :as eta-mu-sessions]
   [kms-ingestion.drivers.promptdb :as promptdb]
   [kms-ingestion.drivers.audio :as audio]
   [kms-ingestion.drivers.image :as image]
   [kms-ingestion.drivers.scraper :as scraper]))

(def driver-constructors
  {"local"      local/create-driver
   "eta-mu-sessions" eta-mu-sessions/create-driver
   ;; Structured EDN epistemic ingestion — no chunking, schema-validated records
   "promptdb"   promptdb/create-driver
   ;; Audio file ingestion with AI descriptions
   "audio"      audio/create-audio-driver
   ;; Image file ingestion with AI labels/descriptions
   "image"      image/create-image-driver
   ;; Audio scraper — crawls URLs, downloads audio files to local folder
   "scraper"    scraper/create-scraper-driver
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
