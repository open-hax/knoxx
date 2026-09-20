(ns knoxx.backend.infra.stores.clio-cache-store
  "Canonical expiring values and atomic fixed-window rate counters."
  (:require [knoxx.backend.domain.cache-store :as domain]
            [knoxx.backend.extern.local-policy :as host]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.law.cache-store :as law]
            [knoxx.backend.shape.cache-store :as protocol]))

(defn- projection []
  (let [state (atom {})] {:store state :snapshot #(deref state)}))

(defn- admit! [state operation]
  (let [{next-state :state result :result} (domain/transition @state operation)]
    (reset! state next-state) result))

(defn- ^:async mutate! [{:keys [engine directory now-ms]} operation]
  (await (host/with-lock! (str directory "/cache-store.lock")
          (fn [] (clio/write! engine :cache/admit [(assoc operation :at (now-ms))])))))

(defrecord ClioCacheStore [engine directory now-ms]
  protocol/ICacheStore
  (read-value! [_ bucket cache-key]
    (law/assert-value-key! bucket cache-key)
    (clio/read! engine :cache/read [bucket cache-key (now-ms)]))
  (write-value! [store bucket cache-key value ttl-ms]
    (law/assert-value-key! bucket cache-key)
    (mutate! store {:kind :put :bucket bucket :key cache-key :value value :ttl-ms ttl-ms}))
  (delete-value! [store bucket cache-key]
    (law/assert-value-key! bucket cache-key)
    (mutate! store {:kind :delete :bucket bucket :key cache-key}))
  protocol/IRateLimitStore
  (increment! [store cache-key window-ms]
    (mutate! store {:kind :increment :bucket :rate-limits :key cache-key :ttl-ms window-ms})))

(defn open!
  "Open explicit cache history. Expiry hides views; replay retains accepted facts."
  [{:keys [directory now-ms] :or {now-ms host/now-ms} :as options}]
  (law/require! (and (map? options) (fn? now-ms)) "Invalid cache provider options")
  (law/assert-clock! (now-ms))
  (let [engine (clio/open! {:directory directory :stream "knoxx/cache" :projection projection
                           :reads {:cache/read (fn [state bucket cache-key at]
                                                 (:value (domain/visible-entry @state bucket cache-key at)))}
                           :writes {:cache/admit admit!}})]
    (->ClioCacheStore engine (:directory engine) now-ms)))
