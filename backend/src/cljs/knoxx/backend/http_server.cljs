(ns knoxx.backend.http-server
  "Fastify HTTP server construction and lifecycle helpers."
  (:require ["fastify" :default Fastify]
            ["@fastify/cors" :default fastifyCors]
            ["@fastify/websocket" :default fastifyWebsocket]
            ["@fastify/multipart" :default fastifyMultipart]
            ["@fastify/cookie" :default fastifyCookie]
            ["@fastify/formbody" :default fastifyFormbody]))

(defn create-app!
  []
  (Fastify #js {:logger true
                :bodyLimit (* 50 1024 1024)
                :requestTimeout 600000
                :connectionTimeout 600000
                ;; Dev hot reload must not hang forever on keep-alive/websocket
                ;; clients when Fastify is closed from a shadow-cljs hook.
                :forceCloseConnections true}))

(defn ensure-json-empty-body-parser!
  "Allow Content-Type: application/json with empty bodies.

   Fastify's default parser throws FST_ERR_CTP_EMPTY_JSON_BODY, but some
   endpoints are intentionally POST-without-body."
  [^js app]
  (.addContentTypeParser app
                         "application/json"
                         #js {:parseAs "string"}
                         (fn [_req body done]
                           (try
                             (done nil (if (= body "") #js {} (js/JSON.parse body)))
                             (catch :default err
                               (done err))))))

(defn add-hook!
  [^js app hook-name handler]
  (.addHook app hook-name handler))

(defn register-default-plugins!
  [^js app]
  (-> (.register app fastifyCors #js {:origin true})
      (.then (fn [] (.register app fastifyCookie)))
      (.then (fn [] (.register app fastifyFormbody)))
      (.then (fn [] (.register app fastifyMultipart
                                #js {:limits #js {:fileSize (* 50 1024 1024)
                                                  :fieldSize (* 1 1024 1024)
                                                  :files 10}})))
      (.then (fn [] (.register app fastifyWebsocket)))))

(defn listen!
  [^js app host port]
  (.listen app #js {:host host :port port}))

(defn close!
  [^js app]
  (try
    (let [result (.close app)]
      (if (some? result)
        result
        (js/Promise.resolve true)))
    (catch :default err
      (js/Promise.reject err))))
