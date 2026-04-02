(ns kms-ingestion.server
  "Main entry point for the KMS Ingestion service."
  (:require
   [ring.adapter.jetty :as jetty]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.params :refer [wrap-params]]
   [reitit.ring :as ring]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [muuntaja.core :as m]
   [kms-ingestion.api.routes :as routes]
   [kms-ingestion.db :as db]
   [kms-ingestion.config :as config])
  (:gen-class))

(def app
  (ring/ring-handler
   (ring/router
    routes/routes
    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware]}})
   (ring/create-default-handler)))

(defn wrap-logging
  [handler]
  (fn [request]
    (let [start (System/nanoTime)
          response (handler request)
          elapsed (/ (- (System/nanoTime) start) 1e6)]
      (printf "[%s] %s %s -> %d (%.1fms)%n"
              (java.time.LocalDateTime/now)
              (:request-method request)
              (:uri request)
              (:status response)
              elapsed)
      (flush)
      response)))

(def wrapped-app
  (-> app
      wrap-logging
      wrap-params
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete :patch :options]
                 :access-control-allow-headers ["Content-Type" "Authorization"])))

(defn -main
  [& args]
  (println "Starting KMS Ingestion service...")
  (println (str "Config: " (config/config)))
  
  ;; Initialize database
  (db/init!)
  
  ;; Start server
  (let [port (config/port)]
    (println (str "Server running on http://0.0.0.0:" port))
    (jetty/run-jetty #'wrapped-app {:port port :join? true})))
