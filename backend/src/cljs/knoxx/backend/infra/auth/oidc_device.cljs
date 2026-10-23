(ns knoxx.backend.infra.auth.oidc-device
  "Bounded product device transactions. The protocol worker never grants a product session."
  (:require [knoxx.backend.extern.axxium-oidc :as wire]
            [knoxx.backend.infra.auth.oidc :as oidc]
            [knoxx.backend.law.oidc :as law]))

(defn- require-origin!
  "Device mutations require an enabled client and the exact operator-configured origin."
  [consumer origin]
  (when-not (and (get-in consumer [:config :device-enabled?])
                 (= origin (get-in consumer [:config :public-origin])))
    (throw (ex-info "Device sign-in unavailable" {:status 403}))))

(defn- ^:async revoke!
  "Attempt cleanup of an unused device grant without returning sensitive errors."
  [consumer grant]
  (when grant
    (try (await (wire/revoke-grant! (await (oidc/client! consumer)) grant))
         (catch :default _ nil))))

(defn ^:async cancel!
  "Remove the transaction synchronously; every worker/completion checks this fence."
  [consumer id]
  (let [[before _] (swap-vals! (:devices consumer) dissoc id)]
    (when-let [tx (get before id)]
      (when (:handle tx) (wire/cancel-device! (:handle tx)))
      (await (revoke! consumer (:grant tx))))))

(defn- ^:async expire!
  "Discard device_code at the deadline; retain only a short public expiry tombstone."
  [consumer id]
  (when-let [tx (get @(:devices consumer) id)]
    (swap! (:devices consumer) assoc id
           {:status "expired" :expires-at (+ (wire/now-ms) 60000) :next-poll 0 :public {:interval 5}})
    (await (revoke! consumer (:grant tx)))))

(defn- ^:async work!
  "Retain only a validated library result; cancel/expiry wins every asynchronous result."
  [consumer rp id tx]
  (try
    (let [grant (await (wire/device-grant! rp (:config consumer) (:handle tx)))]
      (if (and (= (:handle tx) (:handle (get @(:devices consumer) id)))
               (= "pending" (:status (get @(:devices consumer) id)))
               (< (wire/now-ms) (:expires-at tx)))
        (swap! (:devices consumer) update id assoc :status "ready" :grant grant)
        (await (revoke! consumer grant))))
    (catch :default error
      (when (and (= (:handle tx) (:handle (get @(:devices consumer) id)))
                 (= "pending" (:status (get @(:devices consumer) id))))
        (swap! (:devices consumer) update id assoc :status (wire/device-failure error))))))

(defn ^:async begin!
  "Initiate against trusted discovery; return only user-facing RFC8628 fields."
  [consumer origin previous]
  (require-origin! consumer origin)
  (await (cancel! consumer previous))
  (doseq [[id tx] @(:devices consumer)]
    (when (<= (:expires-at tx) (wire/now-ms)) (await (cancel! consumer id))))
  (when (>= (count @(:devices consumer)) 1000)
    (throw (ex-info "Device sign-in busy" {:status 503})))
  (let [rp (await (oidc/client! consumer))
        result (await (wire/device-start! rp (:config consumer)))
        id (wire/random-id)
        tx (assoc result :status "pending" :next-poll 0)]
    (when (>= (count @(:devices consumer)) 1000)
      (when (:handle tx) (wire/cancel-device! (:handle tx)))
      (throw (ex-info "Device sign-in busy" {:status 503})))
    (swap! (:devices consumer) assoc id tx)
    (wire/on-device-expiry! (:handle tx) #(expire! consumer id))
    (work! consumer rp id tx)
    {:id id :public (:public tx)}))

(defn ^:async poll!
  "Browser polling consumes ready grants once and enforces its own interval/backoff."
  [consumer policy-context origin id]
  (require-origin! consumer origin)
  (let [tx (get @(:devices consumer) id)
        now (wire/now-ms)
        admission (law/device-poll-admission tx now)]
    (case admission
      :expired (do (await (cancel! consumer id)) {:status "expired"})
      :refuse (throw (ex-info "Device transaction refused" {:status 400}))
      :slow (do (swap! (:devices consumer) update id
                      #(-> % (update-in [:public :interval] + 5)
                           (assoc :next-poll (+ now (* 1000 (+ 5 (get-in tx [:public :interval])))))))
                {:status "pending" :http-status 429 :interval (+ 5 (get-in tx [:public :interval]))})
      :continue
      (let [updated (assoc tx :next-poll (+ now (* 1000 (get-in tx [:public :interval]))))]
        (swap! (:devices consumer) assoc id updated)
        (case (:status tx)
          "ready"
          (let [completing (assoc updated :status "completing")]
            (swap! (:devices consumer) assoc id completing)
            (try
              (let [result (await (oidc/establish! consumer policy-context (:grant tx)
                                   #(= completing (get @(:devices consumer) id))))]
                (assoc result :status "complete"))
              (finally (swap! (:devices consumer) dissoc id))))
          "pending" {:status "pending" :interval (get-in tx [:public :interval])}
          "completing" (throw (ex-info "Device transaction already consumed" {:status 400}))
          (do (await (cancel! consumer id)) {:status (:status tx)}))))))

(defn- guarded
  "Map protocol failures to a fixed public message without sensitive error data."
  [handler]
  (fn ^:async device-handler [request reply]
    (try (await (handler request reply))
         (catch :default error
           (wire/send! reply (or (:status (ex-data error)) 503)
                       {:status "failed" :error "Device sign-in could not be completed. Start again."})))))

(defn- ^:async start-handler!
  "Set only an opaque browser binding and return user-facing protocol fields."
  [consumer request reply]
  (let [{:keys [origin device-id]} (wire/request-data request)
        result (await (begin! consumer origin device-id))]
    (wire/set-cookie! reply wire/device-cookie (:id result) (get-in result [:public :expires_in]))
    (wire/send! reply 200 (:public result))))

(defn- ^:async poll-handler!
  "A completed transaction returns only a status and an opaque session cookie."
  [consumer policy-context request reply]
  (let [{:keys [origin device-id]} (wire/request-data request)
        result (await (poll! consumer policy-context origin device-id))]
    (when (= "complete" (:status result))
      (wire/set-cookie! reply wire/session-cookie (:session-id result) (:ttl result))
      (wire/clear-cookie! reply wire/device-cookie)
      (wire/clear-cookie! reply "knoxx_session"))
    (wire/send! reply (or (:http-status result) 200) (select-keys result [:status :interval]))))

(defn- ^:async cancel-handler!
  "Fence completion before clearing the device transaction cookie."
  [consumer request reply]
  (let [{:keys [origin device-id]} (wire/request-data request)]
    (require-origin! consumer origin)
    (await (cancel! consumer device-id))
    (wire/clear-cookie! reply wire/device-cookie)
    (wire/send! reply 200 {:status "cancelled"})))

(defn register!
  "Mount the fixed device endpoints at the extern boundary."
  [app consumer policy-context]
  (when consumer
    (reset! (:cancel-device consumer) #(cancel! consumer %))
    (wire/register-device-routes! app
      {:start (guarded (partial start-handler! consumer))
       :poll (guarded (partial poll-handler! consumer policy-context))
       :cancel (guarded (partial cancel-handler! consumer))})))
