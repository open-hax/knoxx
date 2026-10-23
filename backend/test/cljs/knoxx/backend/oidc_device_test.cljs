(ns knoxx.backend.oidc-device-test
  "Product transaction fences; synthetic protocol grants never create a root owner."
  (:require [cljs.test :refer [async deftest is]]
            [knoxx.backend.extern.axxium-oidc :as wire]
            [knoxx.backend.infra.auth.oidc :as oidc]
            [knoxx.backend.infra.auth.oidc-device :as device]
            [knoxx.backend.law.oidc-test :as fixture]))

(defn- ^:async run-test!
  "Bridge cljs.test completion explicitly; every rejected helper fails an assertion."
  [operation done]
  (try (await (operation))
       (catch :default error (is false (str error)))
       (finally (done))))

(defn- setup
  "Prepare only a synthetic local context and an opaque transaction."
  []
  (let [consumer (oidc/create-consumer (assoc fixture/config :device-enabled? true)
                   {:discover! (fn [_] :rp) :now (constantly 100000) :random (constantly "session")
                    :introspect! (fn [_ _] fixture/evidence) :revoke-grant! (fn [_ _] nil)})
        grant (assoc fixture/grant :access-token "server-only")
        handle #js {:controller (js/AbortController.)}
        tx {:handle handle :status "ready" :next-poll 0 :expires-at (+ (wire/now-ms) 600000)
            :grant grant :public {:interval 5}}]
    (swap! (:devices consumer) assoc "browser" tx)
    {:consumer consumer :policy {:resolve-context! (fn [_] fixture/context)}}))

(defn- ^:async refused?
  "An unexpected success must fail the caller's assertion."
  [operation]
  (try (await operation) false (catch :default _ true)))

(defn- ^:async browser-bound* []
  (let [{:keys [consumer policy]} (setup)]
    (is (await (refused? (device/poll! consumer policy "https://evil.invalid" "browser"))))
    (is (await (refused? (device/poll! consumer policy "https://knoxx.example" "wrong"))))
    (is (empty? @(:sessions consumer)))
    (is (= "complete" (:status (await (device/poll! consumer policy "https://knoxx.example" "browser")))))
    (is (= "subject-1" (:subject (get @(:sessions consumer) "session"))))
    (is (await (refused? (device/poll! consumer policy "https://knoxx.example" "browser"))))))

(deftest origin-cookie-and-one-use-product-admission
  (async done (run-test! browser-bound* done)))

(defn- ^:async cancelled* []
  (let [{:keys [consumer]} (setup) entered (atom nil) release (atom nil)
        began (js/Promise. (fn [resolve _] (reset! entered resolve)))
        barrier (js/Promise. (fn [resolve _] (reset! release resolve)))
        policy {:resolve-context! (fn ^:async context [_] (@entered) (await barrier) fixture/context)}
        completing (device/poll! consumer policy "https://knoxx.example" "browser")]
    (await began)
    (await (device/cancel! consumer "browser"))
    (@release)
    (is (await (refused? completing)))
    (is (empty? @(:sessions consumer)))
    (is (empty? @(:devices consumer))))
  (let [{:keys [consumer policy]} (setup)]
    (swap! (:devices consumer) assoc-in ["browser" :expires-at] 0)
    (is (= "expired" (:status (await (device/poll! consumer policy "https://knoxx.example" "browser")))))
    (is (empty? @(:sessions consumer)))))

(deftest cancellation-and-expiry-win-inflight-local-admission
  (async done (run-test! cancelled* done)))
