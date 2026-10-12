(ns knoxx.backend.extern.bootstrap-readiness-test
  "The actual HTTP lifecycle must await mandatory persistence before opening its listener."
  (:require ["fastify" :default fastify]
            [cljs.test :as test]
            [knoxx.backend.bootstrap :as bootstrap]
            [knoxx.backend.extern.provider-recovery-fixture :as fixture]
            [knoxx.backend.infra.http-server :as http-server]
            [knoxx.backend.infra.lifecycle :as lifecycle]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.clio-run-store :as clio]
            [knoxx.backend.infra.stores.mongo-run-store :as mongo]
            [knoxx.backend.infra.stores.run-provider-startup :as startup]
            [knoxx.backend.infra.stores.session-flush :as session-flush]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.runtime.state :as runtime-state]))

(defn- deferred []
  (let [resolve* (atom nil) reject* (atom nil)]
    {:promise (js/Promise. (fn [resolve reject] (reset! resolve* resolve) (reset! reject* reject)))
     :resolve! (fn [] (@resolve* nil)) :reject! (fn [error] (@reject* error))}))

(defn- http-deps [observed* start-persistence!]
  (let [record! (fn [event] (fn [& _] (swap! observed* conj event)))
        noop (fn [& _])
        app #js {:log #js {:info noop}}]
    {:remember-runtime-context! noop :create-app! (fn [] app)
     :ensure-json-parser! noop :add-debug-hook! noop
     :register-default-plugins! noop :register-ws-routes! noop
     :add-session-hook! noop :register-http-routes! noop
     :start-persistence! start-persistence!
     :listen! (record! :listen) :close! (record! :close)
     :listening {:remember-app! (record! :remember) :install-shutdown! noop
                 :notify-ready! (record! :ready) :start-recovery! (record! :recovery)
                 ;; The original lifecycle invokes this after signaling ready.
                 :start-persistence! start-persistence!}}))

(defn- ^:async startup-outcome [deps]
  (try
    (await (bootstrap/start-http! {} {:host "127.0.0.1" :port 0} {} false deps))
    :started
    (catch :default error error)))

(defn- provider-persistence [log]
  (bootstrap/start-required-persistence!
   log {:connect! (fn [] :owned-db)
        :initialize! (fn [db _log] (startup/install-mongo! db))}))

(test/deftest ^:async delayed-provider-prevents-listening-and-ready
  (let [directory (fixture/temporary-directory) previous @registry/session-store*
        provider (clio/open! {:directory directory}) gate (deferred) observed* (atom [])]
    (try
      (reset! registry/session-store* nil)
      (events/install! nil)
      (with-redefs [mongo/setup-indexes! (fn [_] (:promise gate))
                    mongo/create-mongo-run-store (fn ([_] provider) ([_ _] provider))]
        (let [pending (startup-outcome (http-deps observed* provider-persistence))]
          (await (fixture/drain!))
          (test/is (empty? @observed*) "No listener, readiness, recovery or app publication before provider admission")
          (test/is (nil? @registry/session-store*))
          ((:resolve! gate))
          (test/is (= :started (await pending)))
          (test/is (identical? provider @registry/session-store*))
          (test/is (= [:listen :remember :recovery :ready] @observed*))))
      (finally
        (reset! registry/session-store* previous) (events/install! previous)
        (fixture/remove! directory)))))

(test/deftest ^:async rejected-provider-closes-unpublished-app-and-propagates
  (let [gate (deferred) observed* (atom []) failure (ex-info "run index refused" {})
        previous @registry/session-store* constructed* (atom false)]
    (try
      (reset! registry/session-store* nil)
      (with-redefs [mongo/setup-indexes! (fn [_] (:promise gate))
                    mongo/create-mongo-run-store (fn ([_] (reset! constructed* true)) ([_ _] nil))]
        (let [pending (startup-outcome (http-deps observed* provider-persistence))]
          (await (fixture/drain!))
          ;; Observe the provider rejection even on the old fire-and-forget control.
          (let [rejection (fixture/settled [(:promise gate)])]
            ((:reject! gate) failure)
            (await rejection))
          (test/is (identical? failure (await pending)))
          (test/is (= [:close] @observed*))
          (test/is (nil? @registry/session-store*))
          (test/is (false? @constructed*))))
      (finally (reset! registry/session-store* previous)))))

(test/deftest ^:async missing-or-rejected-connection-prevents-listener
  (doseq [connect! [(fn [] nil) (^:async fn [] (throw (ex-info "connection refused" {})))]]
    (let [observed* (atom []) initialized* (atom false)
          result (await (startup-outcome
                         (http-deps observed*
                                    (fn [log]
                                      (bootstrap/start-required-persistence!
                                       log {:connect! connect!
                                            :initialize! (fn [& _] (reset! initialized* true))})))))]
      (test/is (not= :started result))
      (test/is (= [:close] @observed*))
      (test/is (false? @initialized*)))))

(defn- socket-deps [app observed* gate]
  (assoc (http-deps observed* (fn [_log] (:promise gate)))
         :create-app! (fn [] app)
         :listen! (^:async fn [server host port]
                     (await (.listen server #js {:host host :port port}))
                     (swap! observed* conj :listen))
         :close! (^:async fn [server]
                    (await (.close server))
                    (swap! observed* conj :close))))

(test/deftest ^:async real-fastify-listener-opens-only-after-persistence
  (let [app (fastify #js {:logger false}) observed* (atom []) gate (deferred)]
    (try
      (.get app "/readiness-proof" (fn [_request _reply] "provider ready"))
      (let [pending (startup-outcome (socket-deps app observed* gate))]
        (await (fixture/drain!))
        (test/is (false? (.-listening (.-server app))))
        (test/is (empty? @observed*))
        ((:resolve! gate))
        (test/is (= :started (await pending)))
        (test/is (true? (.-listening (.-server app))))
        (let [port (.-port (.address (.-server app)))
              response (await (js/fetch (str "http://127.0.0.1:" port "/readiness-proof")))]
          (test/is (= 200 (.-status response)))
          (test/is (= "provider ready" (await (.text response))))))
      (finally (await (.close app))))))

(test/deftest ^:async hot-reload-close-log-reports-process-milliseconds
  (let [previous-uptime (.-uptime js/process)
        previous-log (.-log js/console)
        logs* (atom [])
        events* (atom [])]
    (try
      (aset js/process "uptime" (fn [] 1.25))
      (aset js/console "log"
            (fn [message metadata]
              (swap! logs* conj {:message message
                                :pid (.-pid metadata)
                                :uptime-ms (.-uptimeMs metadata)})))
      (with-redefs [session-flush/stop-periodic-flush!
                    (fn [] (swap! events* conj :stop-flush))
                    lifecycle/close-current-http!
                    (fn []
                      (swap! events* conj :close-http)
                      (js/Promise.resolve true))]
        (await (bootstrap/stop-http-before-load!
                (fn [] (swap! events* conj :done)))))
      (aset js/process "uptime" previous-uptime)
      (aset js/console "log" previous-log)
      (test/is (= [:stop-flush :close-http :done] @events*)
               "The real public reload callback completes its close before done")
      (test/is (= ["[knoxx-hot-reload] before-load: closing HTTP server"
                   "[knoxx-hot-reload] before-load: HTTP server closed"]
                  (mapv :message @logs*)))
      (test/is (= [1250 1250] (mapv :uptime-ms @logs*))
               "1.25 seconds of process uptime is reported as 1,250 milliseconds")
      (test/is (= [(.-pid js/process) (.-pid js/process)]
                  (mapv :pid @logs*)))
      (finally
        (aset js/process "uptime" previous-uptime)
        (aset js/console "log" previous-log)))))

(test/deftest ^:async default-bootstrap-request-hook-marks-only-hmr-probe
  (let [app (fastify #js {:logger false})
        boundary-error (ex-info "Stop fixture before plugins, persistence and listening" {})
        closed* (atom [])
        probe-logs* (atom [])
        previous-log (.-log js/console)]
    (try
      (.get app "/api/dev/hmr" (fn [_request _reply] "hmr fixture"))
      (.get app "/health" (fn [_request _reply] "healthy fixture"))
      ;; Retain the real default dependency map, JSON parser and onRequest hook.
      ;; Only an outer plugin boundary is rejected; no DB or listener is opened.
      (with-redefs [runtime-state/remember-context! (fn [& _])
                    http-server/create-app! (fn [] app)
                    http-server/register-default-plugins!
                    (fn [_app] (js/Promise.reject boundary-error))
                    http-server/close!
                    (fn [server]
                      (swap! closed* conj server)
                      ;; Keep this owned in-process app for inject; finally closes it.
                      (js/Promise.resolve true))]
        (try
          (await (bootstrap/start-http!
                  #js {} {:host "127.0.0.1" :port 0} {} false))
          (test/is false "The fixture must stop before later startup effects")
          (catch :default error
            (test/is (identical? boundary-error error)))))
      (test/is (= 1 (count @closed*)))
      (test/is (identical? app (first @closed*)))
      (test/is (false? (.-listening (.-server app))))
      (let [response (try
                       (aset js/console "log"
                             (fn [& args] (swap! probe-logs* conj (vec args))))
                       (await (.inject app #js {:method "GET" :url "/api/dev/hmr"}))
                       (finally (aset js/console "log" previous-log)))]
        (test/is (= 200 (.-statusCode response)))
        (test/is (= "hmr fixture" (.-body response)))
        (test/is (= bootstrap/hmr-probe-token
                    (aget (.-headers response) "x-knoxx-hmr-probe")))
        (test/is (= [["[knoxx-hot-reload-probe]" bootstrap/hmr-probe-token]]
                    (mapv #(vec (take 2 %)) @probe-logs*))
                 "The HMR request emits the process probe marker"))
      (let [response (try
                       (aset js/console "log"
                             (fn [& args] (swap! probe-logs* conj (vec args))))
                       (await (.inject app #js {:method "GET" :url "/health"}))
                       (finally (aset js/console "log" previous-log)))]
        (test/is (= 200 (.-statusCode response)))
        (test/is (= "healthy fixture" (.-body response)))
        (test/is (nil? (aget (.-headers response) "x-knoxx-hmr-probe")))
        (test/is (= 1 (count @probe-logs*))
                 "An ordinary request emits no HMR probe log"))
      (test/is (false? (.-listening (.-server app))))
      (finally
        (aset js/console "log" previous-log)
        (await (.close app))))))
