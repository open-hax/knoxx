(ns knoxx.backend.infra.stores.mongo-rate-limits
  "Compatibility facade for selected atomic rate-limit persistence."
  (:require [knoxx.backend.infra.cache-services :as cache]))

(def COLLECTION_NAME "knoxx_rate_limits")
(defonce ^:private increment-fn* (atom nil))
(defn set-increment-fn! "Install an explicit test counter seam." [f] (reset! increment-fn* f))
(defn setup-indexes! "Initialize explicit Mongo indexes." [db] (cache/setup-indexes! db))
(defn increment-rate-limit!
  "Increment a fixed window without extending the initial deadline."
  ([key window-seconds] (increment-rate-limit! nil key window-seconds))
  ([db key window-seconds]
   (if-let [f @increment-fn*] (f key window-seconds)
     (cache/increment! db key (* window-seconds 1000)))))
