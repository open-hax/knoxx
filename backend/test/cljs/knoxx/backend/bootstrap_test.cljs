(ns knoxx.backend.bootstrap-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.bootstrap :as bootstrap]
            [knoxx.backend.infra.agent.resume :as resume]
            [knoxx.backend.infra.persistence-bootstrap :as persistence]
            [knoxx.backend.infra.stores.session-flush :as flush]))

(defn- policy-bootstrap-deps
  [events* create-policy-context!]
  {:create-policy-context! create-policy-context!
   :remember-lifecycle-context!
   (fn [_runtime _cfg _policy-context _cookie-hook?]
     (swap! events* conj :remember-context))
   :start-http!
   (fn [_runtime _cfg _policy-context _cookie-hook?]
     (swap! events* conj :start-http)
     (js/Promise.resolve :started))
   :runtime-factory
   (fn []
     (swap! events* conj :create-runtime)
     #js {})})

(deftest ^:async rejected-policy-bootstrap-never-composes-http-test
  (let [events* (atom [])
        failure (js/Error. "policy rejected")]
    (try
      (await
       (bootstrap/start-policy-http!
        {:host "127.0.0.1" :port 8000} false {}
        (policy-bootstrap-deps
         events*
         (fn [_]
           (swap! events* conj :create-policy)
           (js/Promise.reject failure)))))
      (is false "a rejected policy initialization must propagate")
      (catch :default err
        (is (identical? failure err))))
    (is (= [:create-policy] @events*)
        "rejection occurs before runtime creation, routes, or listener setup")))

(deftest ^:async nil-policy-bootstrap-never-composes-http-test
  (let [events* (atom [])]
    (try
      (await
       (bootstrap/start-policy-http!
        {:host "127.0.0.1" :port 8000} false {}
        (policy-bootstrap-deps
         events*
         (fn [_]
           (swap! events* conj :create-policy)
           (js/Promise.resolve nil)))))
      (is false "a nil policy initialization must fail closed")
      (catch js/Error err
        (is (= "Knoxx policy DB returned no usable policy context"
               (.-message err)))))
    (is (= [:create-policy] @events*)
        "nil occurs before runtime creation, routes, or listener setup")))

(deftest ^:async valid-policy-bootstrap-starts-http-test
  (let [events* (atom [])
        policy-context {:primary-org {:id "org"}}
        result
        (await
         (bootstrap/start-policy-http!
          {:host "127.0.0.1" :port 8000} true {:option true}
          (policy-bootstrap-deps
           events*
           (fn [options]
             (swap! events* conj [:create-policy options])
             (js/Promise.resolve policy-context)))))]
    (is (= :started result))
    (is (= [[:create-policy {:option true}]
            :create-runtime
            :remember-context
            :start-http]
           @events*))))

(deftest ^:async valid-policy-http-composes-routes-listens-and-signals-test
  (let [events* (atom [])
        app #js {:log #js {:info (fn [_message]
                                  (swap! events* conj :log-listening))}}
        record! (fn [event]
                  (fn [& _]
                    (swap! events* conj event)))
        async-record! (fn [event]
                        (fn [& _]
                          (swap! events* conj event)
                          (js/Promise.resolve nil)))
        deps
        {:remember-runtime-context! (record! :remember-runtime-context)
         :create-app! (fn []
                        (swap! events* conj :create-app)
                        app)
         :ensure-json-parser! (record! :ensure-json-parser)
         :add-debug-hook! (record! :add-debug-hook)
         :register-default-plugins! (async-record! :register-default-plugins)
         :register-ws-routes! (async-record! :register-ws-routes)
         :add-session-hook! (async-record! :add-session-hook)
         :register-http-routes! (async-record! :register-http-routes)
         :listen! (fn [_app host port]
                    (swap! events* conj [:listen host port])
                    (js/Promise.resolve nil))
         :listening
         {:remember-app! (record! :remember-app)
          :install-shutdown! (record! :install-shutdown)
          :notify-ready! (record! :notify-ready)
          :start-recovery! (record! :start-recovery)
          :start-persistence! (record! :start-persistence)}}
        policy-context {:primary-org {:id "org"}}
        result (await (bootstrap/start-http!
                       #js {} {:host "127.0.0.1" :port 8123}
                       policy-context true deps))]
    (is (= app result))
    (is (= [:remember-runtime-context
            :create-app
            :ensure-json-parser
            :add-debug-hook
            :register-default-plugins
            :register-ws-routes
            :add-session-hook
            :register-http-routes
            :start-persistence
            [:listen "127.0.0.1" 8123]
            :remember-app
            :install-shutdown
            :notify-ready
            :log-listening
            :start-recovery]
           @events*))))


(deftest ^:async persistence-rejection-prevents-listen-and-readiness-test
  (let [events* (atom [])
        failure (js/Error. "persistence unavailable")
        noop (fn [& _] nil)
        deps {:remember-runtime-context! noop
              :create-app! (fn [] #js {:log #js {}})
              :ensure-json-parser! noop :add-debug-hook! noop
              :register-default-plugins! noop :register-ws-routes! noop
              :add-session-hook! noop :register-http-routes! noop
              :listen! (fn [& _] (swap! events* conj :listen))
              :close-app! (fn [_] (swap! events* conj :close))
              :listening {:start-persistence! (fn [& _] (js/Promise.reject failure))
                          :notify-ready! (fn [] (swap! events* conj :ready))}}]
    (try
      (await (bootstrap/start-http! #js {} {} {} false deps))
      (is false "persistence failure must propagate")
      (catch :default err
        (is (identical? failure err))))
    (is (= [:close] @events*) "failure closes the app before listener and readiness")))

(deftest ^:async bind-failure-must-not-start-recovery-test
  (let [events* (atom [])
        noop (fn [& _] nil)
        deps {:remember-runtime-context! noop
              :create-app! (fn [] #js {:log #js {}})
              :ensure-json-parser! noop :add-debug-hook! noop
              :register-default-plugins! noop :register-ws-routes! noop
              :add-session-hook! noop :register-http-routes! noop
              :listen! (fn [& _] (swap! events* conj :listen) (js/Promise.reject (js/Error. "bind failed")))
              :close-app! (fn [_] (swap! events* conj :close))
              :listening {:start-persistence! bootstrap/start-session-persistence!}}]
    (with-redefs [persistence/mongo-required? (constantly false)
                  resume/resume-on-process-startup! (fn [_ _ _] (swap! events* conj :scan) (js/Promise.resolve nil))
                  resume/start-periodic-recovery! (fn [_ _ _] (swap! events* conj :recovery-timer))
                  flush/start-periodic-flush! (fn ([] (swap! events* conj :flush-timer))
                                                ([_] (swap! events* conj :flush-timer)))]
      (try (await (bootstrap/start-http! #js {} {} {} false deps))
           (is false "failed bind must propagate")
           (catch :default error (is (= "bind failed" (.-message error)))))
      (is (= [:listen :close] @events*) "failed bind closes app without starting recovery"))))

(defn- pending-start [events*]
  (let [resolve* (atom nil)
        pending (js/Promise. (fn [resolve _] (reset! resolve* resolve)))
        noop (fn [& _] nil)
        app #js {:log #js {:info noop}}
        deps {:remember-runtime-context! noop :create-app! (fn [] app)
              :ensure-json-parser! noop :add-debug-hook! noop
              :register-default-plugins! noop :register-ws-routes! noop
              :add-session-hook! noop :register-http-routes! noop
              :listen! (fn [& _] (swap! events* conj :listen))
              :close-app! (fn [_] (swap! events* conj :close))
              :listening {:start-persistence! (fn [& _] (swap! events* conj :install) pending)
                          :remember-app! noop :install-shutdown! noop
                          :notify-ready! (fn [] (swap! events* conj :ready))
                          :start-recovery! (fn [& _] (swap! events* conj :recovery))}}]
    {:result (bootstrap/start-http! #js {} {} {} false deps)
     :resolve (fn [] (@resolve* nil))}))

(deftest ^:async pending-persistence-delays-listen-and-readiness-test
  (let [events* (atom [])
        {:keys [result resolve]} (pending-start events*)]
    (await (js/Promise. (fn [resolve _] (js/setTimeout resolve 0))))
    (is (= [:install] @events*) "pending persistence must not expose a listener")
    (resolve)
    (await result)
    (is (= [:install :listen :ready :recovery] @events*))))

(deftest ^:async characterize-readiness-while-recovery-pending-test
  (let [events* (atom [])
        completed* (atom false)
        pending (js/Promise. (fn [_ _] nil))
        noop (fn [& _] nil)
        app #js {:log #js {:info noop}}
        deps {:remember-runtime-context! noop :create-app! (fn [] app)
              :ensure-json-parser! noop :add-debug-hook! noop
              :register-default-plugins! noop :register-ws-routes! noop
              :add-session-hook! noop :register-http-routes! noop
              :listen! (fn [& _] (swap! events* conj :listener-bound))
              :close-app! noop
              :listening {:start-persistence! noop :remember-app! noop :install-shutdown! noop
                          :notify-ready! (fn [] (swap! events* conj :ready))
                          :start-recovery! (fn [& _] (swap! events* conj :recovery-started) pending)}}]
    (await (bootstrap/start-http! #js {} {} {} false deps))
    (is (= [:listener-bound :ready :recovery-started] @events*))
    (is (false? @completed*) "startup returned while the injected recovery promise remained pending")))
