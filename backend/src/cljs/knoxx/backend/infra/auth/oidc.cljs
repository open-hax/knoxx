(ns knoxx.backend.infra.auth.oidc
  "Server-side OIDC transactions and sessions; no remote identity becomes local policy."
  (:require [knoxx.backend.extern.axxium-oidc :as wire]
            [knoxx.backend.infra.db.policy :as policy-db]
            [knoxx.backend.law.oidc :as law]))

(defonce ^:private installed (atom nil))

(defn create-consumer
  "Construct isolated state with explicit protocol effects, also used by adversarial tests."
  ([config]
   (create-consumer config {:discover! wire/discover! :authorization! wire/authorization!
                            :exchange! wire/exchange! :refresh! wire/refresh! :introspect! wire/introspect!
                            :revoke-grant! wire/revoke-grant!
                            :revoke! wire/revoke! :now wire/now-ms :random wire/random-id}))
  ([config effects]
   (wire/require-config! config)
   {:config config :effects effects :client (atom nil) :pending (atom {}) :sessions (atom {}) :renewing (atom {}) :devices (atom {}) :cancel-device (atom nil)}))

(defn enabled? "Report whether operator-configured redirect SSO is installed." []
  (some? @installed))

(defn present?
  "An OIDC cookie selects OIDC admission even when the consumer is unavailable."
  [request]
  (some? (:session-id (wire/request-data request))))

(defn- unavailable!
  "Raise a generic safe error; protocol exception details may contain sensitive evidence."
  []
  (throw (ex-info "Axxium login is unavailable. Try signing in again."
                  {:status 503 :code :oidc-unavailable})))

(defn- require-consumer! "Fail closed for a cookie whose consumer is disabled." []
  (or @installed (unavailable!)))

(defn ^:async client!
  "Retain only successful discovery; failure is never cached as admission."
  [{:keys [config effects client]}]
  (or @client
      (let [rp (await ((:discover! effects) config))]
        (reset! client rp)
        rp)))

(defn- prune!
  "Bound the lifetime and count of sensitive heap records."
  [entries now limit]
  (swap! entries #(into {} (filter (fn [[_ value]] (< now (or (:session-expires-at value) (:expires-at value))))) %))
  (when (>= (count @entries) limit) (unavailable!)))

(defn ^:async begin!
  "Create a five-minute one-use transaction bound to a separate browser cookie."
  [{:keys [config effects pending] :as consumer}]
  (let [now ((:now effects))]
    (prune! pending now 1000)
    (let [rp (await (client! consumer))
          transaction (await ((:authorization! effects) rp config))
          browser-binding ((:random effects))]
      ;; Recheck after awaits so concurrent begin requests cannot overrun the cap.
      (prune! pending ((:now effects)) 1000)
      (swap! pending assoc (:state transaction)
             (assoc transaction :browser-binding browser-binding :expires-at (+ now 300000)))
      {:url (:url transaction) :browser-binding browser-binding})))

(defn consume-transaction!
  "Remove state before any await, including failed attempts and browser-binding mismatches."
  [{:keys [effects pending]} query browser-binding]
  (let [state (get query "state")
        candidate (if (string? state) state
                      (some (fn [[transaction-state tx]] (when (= browser-binding (:browser-binding tx)) transaction-state)) @pending))
        [before _after] (swap-vals! pending dissoc candidate)
        transaction (get before candidate)]
    (when-not (and (law/nonblank? state) (law/nonblank? (get query "code"))
                   (= state (:state transaction))
                   (every? string? (vals query))
                   transaction (law/nonblank? browser-binding) (= browser-binding (:browser-binding transaction))
                   (< ((:now effects)) (:expires-at transaction)))
      (throw (ex-info "Shared login response expired or did not match this browser. Start again."
                      {:status 400 :code :oidc-state-invalid})))
    transaction))

(defn- ^:async local-context!
  "Resolve only the approved membership and recheck all three local identity/tenant IDs."
  [policy-context link]
  (law/require-context!
   link (await (policy-db/resolve-context! policy-context {"x-knoxx-membership-id" (:membership-id link)}))))

(defn- ^:async grant-evidence!
  "Every admission re-introspects; positive evidence is cached for zero seconds."
  [{:keys [config effects] :as consumer} grant]
  (let [rp (await (client! consumer))
        evidence (await ((:introspect! effects) rp (:access-token grant)))]
    (law/require-grant! config grant evidence ((:now effects)))))

(declare revoke-grant!)

(defn ^:async establish!
  "Common code/device completion: linkage, local policy and live parent evidence."
  [{:keys [config effects sessions] :as consumer} policy-context grant current?]
  (let [rp (await (client! consumer))]
    (try
      (let [link (law/approved-link! (:links config) (:issuer config) (:subject grant))
            started ((:now effects))
            ctx (await (local-context! policy-context link))
            evidence (await (grant-evidence! consumer grant))
            bound (law/session-bound grant evidence started)
            _ (law/require-fresh-evidence! started ((:now effects)))
            session-id ((:random effects))]
        (prune! sessions ((:now effects)) 10000)
        (when-not (current?)
          (throw (ex-info "Axxium session was signed out" {:status 401 :code :oidc-session-expired})))
        (swap! sessions assoc session-id (assoc grant :link link :session-expires-at bound))
        {:session-id session-id :context ctx
         :ttl (max 1 (quot (- bound ((:now effects))) 1000))})
      (catch :default error
        (try (await (revoke-grant! consumer rp grant))
             (catch :default _ nil))
        (throw error)))))

(defn ^:async complete!
  "Consume a browser-bound authorization code before using common admission."
  [{:keys [config effects] :as consumer} policy-context query browser-binding]
  (let [transaction (consume-transaction! consumer query browser-binding)
        rp (await (client! consumer))
        grant (await ((:exchange! effects) rp config query transaction))]
    (await (establish! consumer policy-context grant (constantly true)))))

(defn- ^:async revoke-grant!
  "Revoke every server-held token; injected older fixtures may supply access-only effects."
  [consumer rp grant]
  (if-let [revoke (:revoke-grant! (:effects consumer))]
    (await (revoke rp grant))
    (await ((:revoke! (:effects consumer)) rp (:access-token grant)))))

(defn- ^:async renew-session!
  "A single rotation wins only while the exact prior session still exists."
  [{:keys [config sessions effects] :as consumer} session-id previous]
  (let [rp (await (client! consumer))
        renewed (await ((:refresh! effects) rp config previous))]
    (try
      (law/require-renewal! previous renewed)
      (let [evidence (await (grant-evidence! consumer renewed))
            bound (min (:session-expires-at previous)
                       (law/session-bound renewed evidence ((:now effects))))
            current (merge previous renewed {:nonce (:nonce previous) :session-expires-at bound})]
        (when-not (= previous (get @sessions session-id))
          (throw (ex-info "Axxium session was signed out" {:status 401 :code :oidc-session-expired})))
        (swap! sessions assoc session-id current)
        current)
      (catch :default error
        (try (await (revoke-grant! consumer rp renewed)) (catch :default _ nil))
        (throw error)))))

(defn- ^:async current-grant!
  "Singleflight is per opaque session, never per actor or browser account."
  [{:keys [effects renewing] :as consumer} session-id grant]
  (if (and (:refresh-token grant)
           (<= (- (:expires-at grant) ((:now effects))) law/refresh-window-ms))
    (if-let [pending (get @renewing session-id)]
      (await pending)
      (let [pending (renew-session! consumer session-id grant)]
        (swap! renewing assoc session-id pending)
        (try (await pending)
             (finally (swap! renewing dissoc session-id)))))
    grant))

(defn ^:async resolve-session!
  "Authorize an opaque OIDC session with live authority evidence and fresh local policy."
  [{:keys [sessions effects] :as consumer} policy-context session-id]
  (let [grant (get @sessions session-id)]
    (when-not (and grant (< ((:now effects)) (or (:session-expires-at grant) (:expires-at grant))))
      (swap! sessions dissoc session-id)
      (throw (ex-info "Axxium session expired. Sign in again." {:status 401 :code :oidc-session-expired})))
    (try
      (let [grant (await (current-grant! consumer session-id grant))
            started ((:now effects))
            evidence (await (grant-evidence! consumer grant))
            ctx (await (local-context! policy-context (:link grant)))]
        (law/require-fresh-evidence! started ((:now effects)))
        (law/require-grant! (:config consumer) grant evidence ((:now effects)))
        (when (:refresh-token grant)
          (law/session-bound grant evidence ((:now effects))))
        ;; Logout must win an in-flight introspection or policy lookup.
        (when-not (= grant (get @sessions session-id))
          (throw (ex-info "Axxium session was signed out" {:status 401 :code :oidc-session-expired})))
        (with-meta (assoc ctx :auth-provider "axxium-oidc")
          {:oidc/expires-at (min (:expires-at grant)
                                 (or (:session-expires-at grant) (:expires-at grant))
                                 (law/session-bound grant evidence ((:now effects))))}))
      (catch :default error
        (let [[before _] (swap-vals! sessions dissoc session-id)]
          (when-let [current (get before session-id)]
            (try (await (revoke-grant! consumer (await (client! consumer)) current))
                 (catch :default _ nil))))
        (if (:code (ex-data error)) (throw error) (unavailable!))))))

(defn resolve-request!
  "Core auth entry: OIDC admission never falls through to native or supplied headers."
  [request policy-context]
  (resolve-session! (require-consumer!) policy-context (:session-id (wire/request-data request))))

(defn ^:async logout!
  "Delete local admission before revocation; failed authority calls cannot restore it."
  ([consumer session-id] (await (logout! consumer session-id nil)))
  ([{:keys [sessions cancel-device] :as consumer} session-id device-id]
   (let [[before _after] (swap-vals! sessions dissoc session-id)]
     (when-let [cancel @cancel-device] (await (cancel device-id)))
     (when-let [grant (get before session-id)]
       (await (revoke-grant! consumer (await (client! consumer)) grant))))))

(defn ^:async logout-request!
  "Require exact trusted Origin for browser logout; report remote revocation failure safely."
  [request reply]
  (let [{:keys [session-id origin browser-binding device-id]} (wire/request-data request)
        consumer (require-consumer!)]
    (if-not (= (get-in consumer [:config :public-origin]) origin)
      {:status 403 :body {:error "Logout requires the configured Knoxx origin"}}
      (do
        (wire/clear-cookie! reply wire/session-cookie)
        (wire/clear-cookie! reply wire/transaction-cookie)
        (wire/clear-cookie! reply wire/device-cookie)
        (when browser-binding
          (swap! (:pending consumer)
                 #(into {} (remove (fn [[_ tx]] (= browser-binding (:browser-binding tx)))) %)))
        (try
          (await (logout! consumer session-id device-id))
          {:status 200 :body {:ok true}}
          (catch :default _
            {:status 503 :body {:error "Signed out of Knoxx; authority revocation could not be confirmed"}}))))))

(defn- safe-error!
  "Expose only our approved messages, never provider exception details."
  [reply error]
  (let [{:keys [code status]} (ex-data error)
        safe? (contains? #{:oidc-link-required :oidc-link-conflict :oidc-state-invalid
                            :oidc-grant-invalid :oidc-session-expired} code)]
    (wire/send! reply (if safe? status 503)
                {:error (if safe? (ex-message error) "Axxium login could not be completed. Start again.")})))

(defn- ^:async admit-request!
  "Reject revoked OIDC admission before any route can consume identity headers."
  [policy-context native-auth request reply]
  (when (and (wire/protected-request? request)
             (or (present? request) (and (enabled?) (wire/upgraded-request? request))))
    (try
      (if (present? request)
        (let [consumer (require-consumer!)
                _ (law/require-browser-origin! (:config consumer) (wire/request-data request))
                ctx (await (resolve-request! request policy-context))
                id (:session-id (wire/request-data request))
                grant (get @(:sessions consumer) id)
                bound (or (:session-expires-at grant) (:expires-at grant))]
            (wire/replace-identity-headers! request ctx)
            (wire/set-cookie! reply wire/session-cookie id
              (max 0 (quot (- bound (wire/now-ms)) 1000)))
            (wire/limit-upgrade! request (:oidc/expires-at (meta ctx))))
        ;; A reconnect after a refused/cleared OIDC cookie must not become an
        ;; anonymous realtime subscription. Existing native identities still work.
        (if native-auth
          (wire/replace-identity-headers! request (await (native-auth request policy-context)))
          (throw (ex-info "A signed-in session is required" {:status 401}))))
      (catch :default error
        (wire/clear-cookie! reply wire/session-cookie)
        (wire/send! reply (or (:status (ex-data error)) 401)
                    {:error "Axxium session is no longer admissible. Sign in again."})))))

(defn- ^:async start-handler!
  "Start redirect SSO without a caller-controlled destination."
  [_request reply]
  (try
    (let [{:keys [url browser-binding]} (await (begin! (require-consumer!)))]
      (wire/set-cookie! reply wire/transaction-cookie browser-binding 300)
      (wire/redirect! reply url))
    (catch :default error (safe-error! reply error))))

(defn- ^:async callback-handler!
  "Issue an opaque browser session only after all evidence and linkage pass."
  [policy-context request reply]
  (wire/clear-cookie! reply wire/transaction-cookie)
  (try
    (let [consumer (require-consumer!)
          {:keys [query browser-binding]} (wire/request-data request)
          {:keys [session-id ttl]} (await (complete! consumer policy-context query browser-binding))]
      (wire/clear-cookie! reply "knoxx_session")
      (wire/set-cookie! reply wire/session-cookie session-id ttl)
      (wire/redirect! reply (str (get-in consumer [:config :public-origin]) "/")))
    (catch :default error (safe-error! reply error))))

(defn- ^:async logout-handler!
  "Render only safe logout results."
  [request reply]
  (try
    (let [{:keys [status body]} (await (logout-request! request reply))]
      (wire/send! reply status body))
    (catch :default error (safe-error! reply error))))

(defn register-consumer!
  "Mount an operator-owned consumer; isolated tests may supply a bounded protocol fixture."
  ([app policy-context consumer] (register-consumer! app policy-context consumer nil))
  ([app policy-context consumer native-auth]
   (reset! installed consumer)
   (wire/register-routes!
    app {:admit (partial admit-request! policy-context native-auth)
         :start start-handler! :callback (partial callback-handler! policy-context)
         :logout logout-handler!})))

(defn register!
  "Install optional trusted config; discovery remains lazy and failure never grants admission."
  [app policy-context config native-auth]
  (register-consumer! app policy-context (when config (create-consumer config)) native-auth))
