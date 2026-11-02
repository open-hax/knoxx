(ns knoxx.frontend.api.event-agent-bridge-test
  (:require [cljs.test :as t]
            [knoxx.frontend.api.event-agents :as api]))

(defn- ^:async bridge-transport* []
  (let [requests (atom []) old-fetch js/fetch old-window js/window old-storage js/localStorage
        storage (js/Map.)]
    (try
      (set! (.-window js/globalThis) #js {})
      (set! (.-localStorage js/globalThis) #js {:getItem #(.get storage %)})
      (set! (.-fetch js/globalThis)
            (fn [url opts]
              (swap! requests conj [url opts])
              (js/Promise.resolve #js {:ok true :status 200 :json #(js/Promise.resolve #js {:ok true})})))
      (t/is (= {:ok true} (await (api/run-event-agent-job "job/with space"))))
      (let [[url opts] (last @requests) headers (.-headers opts)]
        (t/is (= "https://fixture.invalid/api/admin/config/events/jobs/job%2Fwith%20space/run" url))
        (t/is (= "include" (.-credentials opts)))
        (t/is (= "fixture@example.invalid" (aget headers "x-knoxx-user-email")))
        (t/is (= "fixture-org" (aget headers "x-knoxx-org-slug"))))
      (.set storage "knoxx_user_email" "stored@example.invalid")
      (await (api/dispatch-event-agent-event {:sourceKind "github" :payload {:title "hello"}}))
      (let [[_ opts] (last @requests)]
        (t/is (= "stored@example.invalid" (aget (.-headers opts) "x-knoxx-user-email")))
        (t/is (= {:sourceKind "github" :payload {:title "hello"}}
                 (js->clj (js/JSON.parse (.-body opts)) :keywordize-keys true))))
      (set! (.-fetch js/globalThis)
            (fn [_ _] (js/Promise.resolve #js {:ok false :status 503 :statusText "Unavailable"
                                              :text #(js/Promise.resolve "Runtime unavailable")})))
      (let [error (try (await (api/start-event-agent-runtime)) nil (catch js/Error e (.-message e)))]
        (t/is (= "503 Unavailable - Runtime unavailable" error)))
      (finally (set! (.-fetch js/globalThis) old-fetch)
               (set! (.-window js/globalThis) old-window)
               (set! (.-localStorage js/globalThis) old-storage)))))

(t/deftest configured-bridge-transport
  (t/async done
    ((^:async fn [] (try (await (bridge-transport*))
                         (catch js/Error e (t/is false (str "bridge test rejected: " e)))
                         (finally (done)))))))
