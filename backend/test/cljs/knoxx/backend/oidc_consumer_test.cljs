(ns knoxx.backend.oidc-consumer-test
  "Consumer transaction, policy and revocation proofs with deterministic authority effects."
  (:require ["@fastify/cookie" :default cookie-plugin]
            ["fastify" :default fastify]
            [cljs.test :as test]
            [knoxx.backend.extern.axxium-oidc :as wire]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.auth.oidc :as oidc]
            [knoxx.backend.infra.auth.session :as session]
            [knoxx.backend.law.oidc-test :as fixture]
            [knoxx.backend.infra.routes.auth :as auth-routes]))

(defn- fixture
  "Create mutable evidence, never a fabricated human or database session."
  []
  (let [clock (atom 100000) evidence (atom fixture/evidence) calls (atom []) counter (atom 0)
        policy-context {:resolve-context! (fn [headers]
                                            (swap! calls conj [:context headers])
                                            fixture/context)}
        effects {:now #(deref clock) :random #(str "opaque-" (swap! counter inc))
                 :discover! (fn [_] :test-rp)
                 :authorization! (fn [_ _] {:state "state-1" :nonce "nonce-1" :verifier "verifier-1"
                                            :url "https://authority.example/authorize"})
                 :exchange! (fn [_ _ _ transaction]
                              (swap! calls conj [:exchange transaction])
                              (assoc fixture/grant :access-token "server-only-fixture-token"))
                 :introspect! (fn [_ _]
                                (swap! calls conj [:introspect])
                                (if (= :unavailable @evidence)
                                  (throw (js/Error. "sensitive provider error")) @evidence))
                 :revoke! (fn [_ _] (swap! calls conj [:revoke]))}]
    {:consumer (oidc/create-consumer fixture/config effects) :clock clock :evidence evidence
     :calls calls :policy policy-context}))

(defn- ^:async sign-in!
  "Finish an isolated consumer transaction through its public API."
  [{:keys [consumer policy]}]
  (let [start (await (oidc/begin! consumer))]
    (await (oidc/complete! consumer policy {"state" "state-1" "code" "code-1"} (:browser-binding start)))))

(defn- ^:async rejection-code
  "Observe a rejected async operation without swallowing a successful admission."
  [f]
  (try (await (f)) :unexpected-success
       (catch :default error (:code (ex-data error)))))

(test/deftest ^:async state-is-browser-bound-expiring-and-one-use
  (doseq [mode [:browser-binding :expiry :missing-code :duplicate-value :valid]]
    (let [{:keys [consumer policy clock] :as f} (fixture)
          start (await (oidc/begin! consumer))
          query (case mode :missing-code {"state" "state-1"}
                           :duplicate-value {"state" ["state-1" "state-1"] "code" "code-1"}
                           {"state" "state-1" "code" "code-1"})
          browser-binding (if (= mode :browser-binding) "other-browser" (:browser-binding start))]
      (when (= mode :expiry) (reset! clock 400000))
      (if (= mode :valid)
        (test/is (:session-id (await (oidc/complete! consumer policy query browser-binding))))
        (test/is (= :oidc-state-invalid (await (rejection-code #(oidc/complete! consumer policy query browser-binding))))))
      (test/is (= :oidc-state-invalid
             (await (rejection-code #(oidc/complete! consumer (:policy f)
                                                      {"state" "state-1" "code" "code-1"} (:browser-binding start)))))))))

(test/deftest ^:async no-email-linking-or-identity-creation
  (let [{:keys [consumer policy calls]} (fixture)
        unlinked (assoc-in consumer [:config :links] [])
        start (await (oidc/begin! unlinked))]
    (test/is (= :oidc-link-required
           (await (rejection-code #(oidc/complete! unlinked policy {"state" "state-1" "code" "x"} (:browser-binding start))))))
    (test/is (empty? @(:sessions consumer)))
    (test/is (not-any? #(= :context (first %)) @calls) "Unlinked identity never reaches local account lookup")
    (test/is (= [:revoke] (last @calls)) "Failed linkage revokes the newly exchanged grant")))

(test/deftest ^:async local-tenant-collision-refuses-session
  (let [{:keys [consumer]} (fixture)
        start (await (oidc/begin! consumer))
        collision {:resolve-context! (fn [_] (assoc-in fixture/context [:org :id] "other-tenant"))}]
    (test/is (= :oidc-link-conflict
           (await (rejection-code #(oidc/complete! consumer collision {"state" "state-1" "code" "x"} (:browser-binding start))))))
    (test/is (empty? @(:sessions consumer)))))

(test/deftest ^:async every-request-consumes-grant-evidence
  (doseq [failure [:revoked :unavailable :expired :wrong-client]]
    (let [{:keys [consumer policy evidence clock calls] :as f} (fixture)
          {:keys [session-id]} (await (sign-in! f))]
      (test/is (= "axxium-oidc" (:auth-provider (await (oidc/resolve-session! consumer policy session-id)))))
      (test/is (= 2 (count (filter #(= [:introspect] %) @calls))) "No positive evidence cache")
      (case failure
        :revoked (swap! evidence assoc :active false)
        :unavailable (reset! evidence :unavailable)
        :expired (reset! clock 200000)
        :wrong-client (swap! evidence assoc :client_id "other-product"))
      (test/is (contains? #{:oidc-grant-invalid :oidc-unavailable :oidc-session-expired}
                     (await (rejection-code #(oidc/resolve-session! consumer policy session-id)))))
      (test/is (empty? @(:sessions consumer)) "Failed evidence destroys local admission")
      (test/is (= :oidc-session-expired
             (await (rejection-code #(oidc/resolve-session! consumer policy session-id))))))))

(test/deftest ^:async logout-wins-inflight-admission-and-revocation-failure
  (let [{:keys [consumer policy] :as f} (fixture)
        {:keys [session-id]} (await (sign-in! f))
        release (atom nil)
        pending (js/Promise. (fn [resolve-promise _reject] (reset! release resolve-promise)))
        blocked (assoc-in consumer [:effects :introspect!] (fn [_ _] pending))
        admission (oidc/resolve-session! blocked policy session-id)]
    (await (oidc/logout! consumer session-id))
    (@release fixture/evidence)
    (test/is (= :oidc-session-expired (await (rejection-code #(identity admission))))))
  (let [{:keys [consumer policy] :as f} (fixture)
        {:keys [session-id]} (await (sign-in! f))
        broken (assoc-in consumer [:effects :revoke!] (fn [_ _] (throw (js/Error. "offline"))))]
    (try (await (oidc/logout! broken session-id)) (catch :default _ nil))
    (test/is (= :oidc-session-expired
           (await (rejection-code #(oidc/resolve-session! consumer policy session-id)))))))

(defn- ^:async route-fixture!
  "Mount real Fastify routes/hook without the optional native session hook."
  [{:keys [consumer policy]}]
  (let [app (fastify)]
    (.register app cookie-plugin)
    (oidc/register-consumer! app policy consumer)
    (.get app "/ws/stream" (fn [_request reply] (wire/send! reply 200 {:fixture true})))
    (.get app "/api/auth/context"
          (^:async fn [request reply]
            (wire/send! reply 200 (await (session/resolve-auth-context request policy)))))
    app))

(test/deftest ^:async protected-context-does-not-fall-back-to-spoofed-headers
  (let [{:keys [evidence] :as f} (fixture)
        {:keys [session-id]} (await (sign-in! f))
        app (await (route-fixture! f))
        request #js {:method "GET" :url "/api/auth/context"
                     :headers #js {:cookie (str wire/session-cookie "=" session-id)
                                   "x-knoxx-membership-id" "admin-membership"
                                   "x-knoxx-org-id" "other-tenant"}}]
    (try
      (let [ok (await (.inject app request))]
        (test/is (= 200 (.-statusCode ok)))
        (test/is (= "membership-1" (.. (.json ok) -membership -id))))
      (reset! evidence :unavailable)
      (let [denied (await (.inject app request))]
        (test/is (= 503 (.-statusCode denied)))
        (test/is (not (re-find #"sensitive provider" (.-body denied)))))
      (finally (await (.close app))))))

(test/deftest ^:async cleared-oidc-cookie-does-not-open-an-anonymous-websocket
  (let [f (fixture) app (await (route-fixture! f))]
    (try
      (test/is (= 401 (.-statusCode (await (.inject app #js {:method "GET" :url "/ws/stream"
                                                            :headers #js {:upgrade "websocket"}})))))
      (finally (await (.close app))))))

(test/deftest ^:async logout-requires-exact-origin-and-has-no-redirect-input
  (let [f (fixture) {:keys [session-id]} (await (sign-in! f)) app (await (route-fixture! f))]
    (try
      (doseq [origin ["https://attacker.example" "null" ""]]
        (test/is (= 403 (.-statusCode (await (.inject app #js {:method "POST" :url "/api/auth/axxium/logout"
                                                         :headers #js {:origin origin
                                                                       :cookie (str wire/session-cookie "=" session-id)}}))))))
      (test/is (seq @(:sessions (:consumer f))))
      (test/is (= 200 (.-statusCode (await (.inject app #js {:method "POST" :url "/api/auth/axxium/logout?redirect=https://attacker.example"
                                                       :headers #js {:origin "https://knoxx.example"
                                                                     :cookie (str wire/session-cookie "=" session-id)}})))))
      (test/is (empty? @(:sessions (:consumer f))))
      (finally (await (.close app))))))

(test/deftest ^:async cached-request-context-cannot-bypass-revocation
  (let [{:keys [consumer evidence policy] :as f} (fixture)
        {:keys [session-id]} (await (sign-in! f)) app (await (route-fixture! f))
        original-policy-db authz/policy-db
        request #js {:headers #js {"x-knoxx-membership-id" "admin-membership"}
                     :cookies (clj->js {wire/session-cookie session-id})
                     "__knoxxRequestContext" #js {:user #js {:id "cached-admin"}}}]
    (set! authz/policy-db (fn [_] policy))
    (try
      (test/is (= fixture/context
                  (dissoc (await (authz/resolve-request-context! nil request)) :auth-provider)))
      (swap! evidence assoc :active false)
      (test/is (= :oidc-grant-invalid
                  (await (rejection-code #(authz/resolve-request-context! nil request)))))
      (test/is (empty? @(:sessions consumer)))
      (finally
        (set! authz/policy-db original-policy-db)
        (await (.close app))))))

(test/deftest ^:async slow-local-policy-does-not-extend-evidence-lifetime
  (let [{:keys [consumer clock] :as f} (fixture)
        {:keys [session-id]} (await (sign-in! f))
        slow-policy {:resolve-context! (fn [_] (swap! clock + 5000) fixture/context)}]
    (test/is (= :oidc-evidence-stale
                (await (rejection-code #(oidc/resolve-session! consumer slow-policy session-id)))))
    (test/is (empty? @(:sessions consumer)))))

(defn- response-cookies
  "Decode light-my-request's null-prototype cookie records at the fixture edge."
  [response]
  (mapv #(js->clj (js/Object.assign #js {} %) :keywordize-keys true)
        (array-seq (.-cookies response))))

(test/deftest ^:async browser-routes-set-only-opaque-cookies-and-fixed-redirect
  (let [f (fixture) app (await (route-fixture! f))]
    (try
      (let [start (await (.inject app #js {:method "GET" :url "/api/auth/axxium/start?redirect=https://attacker.example"}))
            binding-cookie (first (response-cookies start))
            callback-url "/api/auth/axxium/callback?code=fixture-code&state=state-1"
            callback-request #js {:method "GET" :url callback-url
                                   :headers #js {:cookie (str (:name binding-cookie) "=" (:value binding-cookie))}}
            callback (await (.inject app callback-request))
            cookies (response-cookies callback)
            admission-cookie (some #(when (= wire/session-cookie (:name %)) %) cookies)]
        (test/is (= 302 (.-statusCode start)))
        (test/is (= 302 (.-statusCode callback)))
        (test/is (= "https://knoxx.example/" (aget (.-headers callback) "location")))
        (test/is (seq (:value admission-cookie)))
        (test/is (true? (:httpOnly admission-cookie)))
        (test/is (true? (:secure admission-cookie)))
        (test/is (not (re-find #"server-only-fixture-token|fixture-only|verifier-1"
                               (js/JSON.stringify (.-headers callback)))))
        (test/is (= 400 (.-statusCode (await (.inject app callback-request))))))
      (finally (await (.close app))))))

(defn- ^:async verify-cookie-write-origin!
  "Exercise the real admission hook and prove refusal precedes route effects."
  [done]
  (let [f (fixture) app (await (route-fixture! f)) effects (atom 0)]
    (try
      (.post app "/api/fixture/write"
             (fn [_request reply]
               (swap! effects inc)
               (wire/send! reply 200 {:written true})))
      (doseq [origin [nil "https://sibling.example" "https://knoxx.example.attacker.test" "https://knoxx.example"]]
        (let [{:keys [session-id]} (await (sign-in! f))
              headers (cond-> {:cookie (str wire/session-cookie "=" session-id)}
                        origin (assoc :origin origin))
              response (await (.inject app (clj->js {:method "POST" :url "/api/fixture/write"
                                                       :headers headers})))]
          (test/is (= (if (= origin "https://knoxx.example") 200 403) (.-statusCode response)))
          (when-not (= origin "https://knoxx.example")
            (test/is (nil? (aget (.-headers response) "set-cookie")))
            (test/is (contains? @(:sessions (:consumer f)) session-id)))))
      (test/is (= 1 @effects))
      (catch :default error (test/is false (str error)))
      (finally (await (.close app)) (done)))))

(test/deftest cookie-write-origin-is-checked-before-route-effects
  (test/async done (verify-cookie-write-origin! done)))

(defn- ^:async bounded-start* [done]
  (try
    (let [{:keys [consumer policy clock]} (fixture)
          bounded (assoc-in consumer [:effects :authorization!]
                    (fn [_ _] {:state (str "bounded-" @clock) :nonce "nonce" :verifier "verifier"
                               :url "https://authority.example/authorize"}))
          first-binding (await (oidc/begin! bounded))]
      (dotimes [_ 1000] (swap! clock inc) (await (oidc/begin! bounded)))
      (test/is (= 1000 (count @(:pending consumer))))
      (test/is (not (contains? @(:pending consumer) "bounded-100000")))
      (test/is (= :oidc-state-invalid
                 (await (rejection-code #(oidc/complete! bounded policy
                   {"state" "bounded-100000" "code" "code"} (:browser-binding first-binding))))))
      (let [latest (await (oidc/begin! bounded))]
        (test/is (:session-id (await (oidc/complete! bounded policy
                            {"state" (str "bounded-" @clock) "code" "code"}
                            (:browser-binding latest)))))))
    (catch :default error (test/is false (str error)))
    (finally (done))))

(test/deftest pending-capacity-evicts-oldest-without-globally-refusing-login
  (test/async done (bounded-start* done)))

(defn- ^:async disabled-consumer-logout* [done]
  (let [app (fastify)]
    (try
      (.register app cookie-plugin)
      (with-redefs [wire/configured (constantly nil)]
        (auth-routes/register-auth-routes app {}))
      (let [response (await (.inject app #js {:method "POST" :url "/api/auth/logout"
                       :headers #js {:cookie "__Host-knoxx_axxium_session=stale; knoxx_session=invalid"}}))
            cookies (js/JSON.stringify (aget (.-headers response) "set-cookie"))]
        (test/is (= 200 (.-statusCode response)))
        (test/is (true? (aget (.json response) "ok")))
        (test/is (re-find #"__Host-knoxx_axxium_session=" cookies))
        (test/is (re-find #"knoxx_session=" cookies)))
      (catch :default error (test/is false (str error)))
      (finally (await (.close app)) (done)))))

(test/deftest stale-oidc-cookie-cannot-block-native-logout-after-rollback
  (test/async done (disabled-consumer-logout* done)))
