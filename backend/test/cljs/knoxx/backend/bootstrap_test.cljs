(ns knoxx.backend.bootstrap-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.bootstrap :as bootstrap]))

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
            [:listen "127.0.0.1" 8123]
            :remember-app
            :install-shutdown
            :notify-ready
            :log-listening
            :start-persistence]
           @events*))))

