(ns knoxx.backend.extern.app-health
  "Dependency health probes and native HTTP health responses."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.promise :as promise]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.clients.proxx :as proxx-client]
            [knoxx.backend.infra.http :as infra-http]))

(defn- dependency-status
  [configured reachable result]
  {:configured configured
   :reachable (boolean reachable)
   :status_code (:status result)
   :detail (:body result)})

(defn health-deps-ok
  "Execute health deps ok through its owned application boundary." [reply proxx-configured openplanner-configured ollama-configured
                       [proxx-res openplanner-res ollama-res]]
  (let [proxx-ok       (and proxx-configured (:ok proxx-res))
        openplanner-ok (and openplanner-configured (:ok openplanner-res))
        ollama-ok      (and ollama-configured (:ok ollama-res))
        healthy        (and proxx-ok
                            openplanner-ok
                            (or (not ollama-configured) ollama-ok))]
    (infra-http/json-response!
     reply
     (if healthy 200 503)
     {:status (if healthy "ok" "unhealthy")
      :service "knoxx-backend-cljs"
      :dependencies {:proxx (dependency-status proxx-configured proxx-ok proxx-res)
                     :openplanner (dependency-status openplanner-configured openplanner-ok openplanner-res)
                     :ollama (dependency-status ollama-configured ollama-ok ollama-res)}})))

(defn health-deps-err
  "Execute health deps err through its owned application boundary." [reply err]
  (infra-http/json-response! reply 503 {:status "unhealthy"
                             :service "knoxx-backend-cljs"
                             :error (str err)}))

(defn- knoxx-health-ok [reply config proxx-configured openplanner-configured ollama-configured
                        [proxx-res openplanner-res ollama-res]]
  (let [proxx-ok       (and proxx-configured (:ok proxx-res))
        openplanner-ok (and openplanner-configured (:ok openplanner-res))
        ollama-ok      (and ollama-configured (:ok ollama-res))
        ;; Healthy only when every configured dependency is reachable.
        healthy        (and (or (not proxx-configured) proxx-ok)
                            (or (not openplanner-configured) openplanner-ok)
                            (or (not ollama-configured) ollama-ok)
                            (or proxx-configured openplanner-configured ollama-configured))]
    (infra-http/json-response!
     reply
     (if healthy 200 503)
     {:reachable healthy
      :configured (boolean (or proxx-configured openplanner-configured ollama-configured))
      :base_url (:knoxx-base-url config)
      :status_code (if healthy 200 503)
      :status (if healthy "ok" "unhealthy")
      :details {:mode "shadow-cljs-eta-mu-sdk"
                :status (if healthy "ok" "unhealthy")
                :project (:project-name config)
                :collection {:name (:collection-name config)
                             :pointsCount nil}
                :dependencies
                {:proxx (dependency-status proxx-configured proxx-ok proxx-res)
                 :openplanner (dependency-status openplanner-configured openplanner-ok openplanner-res)
                 :ollama (dependency-status ollama-configured ollama-ok ollama-res)}}})))

(defn- knoxx-health-err [reply config err]
  (infra-http/json-response! reply 503 {:reachable false
                             :configured true
                             :base_url (:knoxx-base-url config)
                             :status_code 503
                             :status "unhealthy"
                             :error (str err)}))

(defn- data-health-ok [reply results]
  (infra-http/json-response! reply 200
                  {:ok true
                   :services {:openplanner (nth results 0)
                              :proxx (nth results 1)
                              :ingestion (nth results 2)
                              :graph-weaver (nth results 3)
                              :shuvcrawl (nth results 4)
                              :vexx (nth results 5)
                              :eros-eris-field-app (nth results 6)
                              :myrmex (nth results 7)
                              :ollama (nth results 8)}}))

(defn- data-health-err [reply err]
  (infra-http/json-response! reply 500 {:error (.-message err)}))

(defn- health-result
  [url resp]
  {:ok (:ok resp) :status (:status resp) :url url :detail (:body resp)})

(defn- health-error-result
  [url err]
  {:ok false :error (.-message err) :url url})

(defn ^:async service-health-check!
  "Execute service health check through its owned application boundary."
  [url headers]
  (try
    (health-result url (await (infra-http/fetch-json url {:headers (or headers {}) :method "GET"})))
    (catch :default err
      (health-error-result url err))))

(defn ^:async openplanner-health-check!
  "Execute openplanner health check through its owned application boundary."
  [config]
  (try
    (health-result "openplanner:/v1/health"
                   (await (openplanner-client/health! (openplanner-client/client config))))
    (catch :default err
      (health-error-result "openplanner:/v1/health" err))))

(defn ^:async proxx-health-check!
  "Execute proxx health check through its owned application boundary."
  [config]
  (let [url (str (:proxx-base-url config) "/health")]
    (try
      (health-result url (await (proxx-client/health! (proxx-client/client config))))
      (catch :default err
        (health-error-result url err)))))

(defn ^:async ollama-health-check!
  "Execute ollama health check through its owned application boundary."
  [config]
  (if (str/blank? (:ollama-base-url config))
    {:ok false :configured false :url nil :detail {:status "not configured"}}
    (service-health-check! (str (str/replace (:ollama-base-url config) #"/+$" "")
                                "/api/version")
                           nil)))

(defn- unavailable-health
  [service-name]
  (js/Promise.resolve {:ok false
                       :status 503
                       :body {:detail (str service-name " is not configured")}}))

(defn dependency-probes
  "Execute dependency probes through its owned application boundary."
  [config]
  (let [proxx-configured (and (not (str/blank? (:proxx-base-url config)))
                              (not (str/blank? (:proxx-auth-token config))))
        ollama-configured (not (str/blank? (:ollama-base-url config)))
        openplanner (openplanner-client/client config)
        openplanner-configured (openplanner-client/enabled? openplanner)]
    {:proxx-configured proxx-configured
     :openplanner-configured openplanner-configured
     :ollama-configured ollama-configured
     :promises [(if proxx-configured
                  (proxx-client/health! (proxx-client/client config))
                  (unavailable-health "Proxx"))
                (if openplanner-configured
                  (openplanner-client/health! openplanner)
                  (unavailable-health "OpenPlanner"))
                (if ollama-configured
                  (ollama-health-check! config)
                  (unavailable-health "Ollama"))]}))

(defn ^:async send-data-health!
  "Execute send data health through its owned application boundary."
  [config reply]
  (let [ingestion-base (:ingestion-base-url config)]
    (try
      (data-health-ok reply
                      (await (promise/all-vec
                              [(openplanner-health-check! config)
                               (proxx-health-check! config)
                               (service-health-check! (str ingestion-base "/health") nil)
                               (service-health-check! "http://127.0.0.1:8796/api/status" nil)
                               (service-health-check! "http://127.0.0.1:3777/health" nil)
                               (service-health-check! "http://127.0.0.1:8787/v1/health" nil)
                               (service-health-check! "http://127.0.0.1:8786/health" nil)
                               (service-health-check! "http://127.0.0.1:8801/health" nil)
                               (ollama-health-check! config)])))
      (catch :default err
        (data-health-err reply err)))))

(defn ^:async send-knoxx-health!
  "Execute send knoxx health through its owned application boundary."
  [config reply]
  (let [{:keys [proxx-configured openplanner-configured ollama-configured promises]}
        (dependency-probes config)]
    (try
      (knoxx-health-ok reply
                       config
                       proxx-configured
                       openplanner-configured
                       ollama-configured
                       (await (promise/all-vec promises)))
      (catch :default err
        (knoxx-health-err reply config err)))))
