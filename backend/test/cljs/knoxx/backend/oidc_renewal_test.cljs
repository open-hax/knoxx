(ns knoxx.backend.oidc-renewal-test
  "Deterministic session races; protocol signature checks are exercised at the RP boundary."
  (:require [cljs.test :refer [async deftest is]]
            [knoxx.backend.infra.auth.oidc :as oidc]
            [knoxx.backend.law.oidc-test :as fixture]))

(defn- ^:async run-test!
  "Bridge cljs.test completion explicitly; every rejected helper fails an assertion."
  [operation done]
  (try (await (operation))
       (catch :default error (is false (str error)))
       (finally (done))))

(defn- setup []
  (let [clock (atom 100000) count-refresh (atom 0) revoked (atom []) counter (atom 0)
        evidence (atom (assoc fixture/evidence :root_session_expires_at 1000))
        consumer (oidc/create-consumer fixture/config
                   {:discover! (fn [_] :rp) :now #(deref clock) :random #(str "opaque-" (swap! counter inc))
                    :introspect! (fn [_ _] @evidence)
                    :refresh! (fn [_ _ grant]
                                (swap! count-refresh inc)
                                (assoc grant :refresh-token "rotated-server-only" :access-token "rotated-access"
                                             :expires-at 190000))
                    :revoke-grant! (fn [_ grant] (swap! revoked conj grant))})
        grant {:subject "subject-1" :nonce "original" :access-token "access" :refresh-token "refresh"
               :expires-at 120000 :session-expires-at 1000000 :link fixture/link}]
    (swap! (:sessions consumer) assoc "handle" grant)
    {:consumer consumer :clock clock :evidence evidence :refreshes count-refresh :revoked revoked
     :policy {:resolve-context! (fn [_] fixture/context)}}))

(defn- ^:async refused [operation]
  (try (await operation) false (catch :default _ true)))

(defn- ^:async renewal* []
  (let [{:keys [consumer policy refreshes evidence]} (setup)
        calls (repeatedly 8 #(oidc/resolve-session! consumer policy "handle"))
        results (await (js/Promise.all (into-array calls)))]
    (is (= 8 (.-length results)))
    (is (= 1 @refreshes))
    (is (= 1000000 (:session-expires-at (get @(:sessions consumer) "handle"))))
    (swap! evidence assoc :active false)
    (is (await (refused (oidc/resolve-session! consumer policy "handle"))))
    (is (empty? @(:sessions consumer)))))

(deftest concurrent-renewal-and-revocation
  (async done (run-test! renewal* done)))

(defn- ^:async logout* []
  (let [{:keys [consumer policy revoked]} (setup)
        release (atom nil) entered (atom nil)
        barrier (js/Promise. (fn [resolve _] (reset! release resolve)))
        started (js/Promise. (fn [resolve _] (reset! entered resolve)))
        blocked (assoc-in consumer [:effects :refresh!]
                  (fn ^:async refresh [_ _ grant]
                    (@entered)
                    (await barrier)
                    (assoc grant :refresh-token "late-refresh" :access-token "late-access" :expires-at 190000)))
        admission (oidc/resolve-session! blocked policy "handle")]
    (await started)
    (await (oidc/logout! consumer "handle"))
    (@release)
    (is (await (refused admission)))
    (is (empty? @(:sessions consumer)))
    (is (= #{"refresh" "late-refresh"} (set (map :refresh-token @revoked))))))

(deftest logout-wins-rotation-and-revokes-both-generations
  (async done (run-test! logout* done)))

(defn- ^:async substitutions* []
  (doseq [change [{:subject "other"} {:nonce "other"} {:refresh-token nil} {:refresh-token "refresh"}]]
    (let [{:keys [consumer policy]} (setup)
          broken (assoc-in consumer [:effects :refresh!]
                   (fn [_ _ grant] (merge grant {:refresh-token "rotated" :expires-at 190000} change)))]
      (is (await (refused (oidc/resolve-session! broken policy "handle"))))
      (is (empty? @(:sessions consumer)))))
  (let [{:keys [consumer policy clock]} (setup)]
    (reset! clock 1000000)
    (is (await (refused (oidc/resolve-session! consumer policy "handle")))))
  (let [{:keys [consumer policy]} (setup)
        allowed (assoc-in consumer [:effects :refresh!]
                  (fn [_ _ grant] (assoc grant :nonce nil :refresh-token "rotated" :expires-at 190000)))]
    (is (:user (await (oidc/resolve-session! allowed policy "handle"))))))

(deftest subject-and-nonce-continuity-and-finite-root-bound
  (async done (run-test! substitutions* done)))

(defn- ^:async straddling-admission* []
  (doseq [old-inactive? [false true]]
    (let [{:keys [consumer policy clock evidence revoked]} (setup)
          release (atom nil) entered (atom nil)
          barrier (js/Promise. (fn [resolve _] (reset! release resolve)))
          started (js/Promise. (fn [resolve _] (reset! entered resolve)))
          blocked (assoc-in consumer [:effects :introspect!]
                    (fn ^:async introspect [_ token]
                      (if (= token "access")
                        (do (@entered) (await barrier) (assoc @evidence :active (not old-inactive?)))
                        @evidence)))]
      (swap! (:sessions consumer) update "handle" assoc :expires-at 131000)
      (let [straddling (oidc/resolve-session! blocked policy "handle")]
        (await started)
        (reset! clock 102000)
        (is (:user (await (oidc/resolve-session! blocked policy "handle"))))
        (@release)
        (if old-inactive?
          (is (await (refused straddling)))
          (is (:user (await straddling))))
        (is (= "rotated-server-only" (:refresh-token (get @(:sessions consumer) "handle"))))
        (is (empty? @revoked) "old evidence must never revoke the successful successor")))))

(deftest admission-straddling-the-renewal-window-preserves-the-successor
  (async done (run-test! straddling-admission* done)))
