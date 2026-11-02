(ns knoxx.backend.extern.remote-identity-fixture
  "Owned HTTPS authority and native Fastify requests for identity integration."
  (:require ["node:child_process" :as child]
            ["node:fs" :as fs]
            ["node:https" :as https]
            ["node:path" :as path]
            [axxium.extern.identity-http :as identity-http]
            [knoxx.backend.extern.fetch :as fetch]))

(defn app "Create a real Fastify app without a public listener." [] (identity-http/create-app))
(defn close! "Close a fixture-owned Fastify app." [app] (identity-http/close! app))

(defn ^:async inject!
  "Issue a real Fastify request and decode its status, body and session cookie."
  [app request]
  (let [response (await (.inject app (clj->js request)))
        cookie (some-> response .-headers (aget "set-cookie"))]
    {:status (.-statusCode response) :body (js->clj (.json response) :keywordize-keys true)
     :cookie (when (string? cookie) (first (.split cookie ";")))}))

(defn- ^:async request! [certificate requests {:keys [url method redirect json timeout-ms]}]
  (swap! requests conj {:url url :method method :redirect redirect :timeout-ms timeout-ms})
  (await
   (js/Promise.
    (fn [resolve reject]
      (let [request (.request https url
                             #js {:method method :ca certificate :headers #js {"Content-Type" "application/json"}}
                             (fn [response]
                               (let [chunks (atom [])]
                                 (.on response "data" #(swap! chunks conj %))
                                 (.on response "end"
                                      (fn []
                                        (try
                                          (resolve {:status (.-statusCode response)
                                                    :body (js->clj (js/JSON.parse (.toString (.concat js/Buffer (into-array @chunks))))
                                                                   :keywordize-keys true)})
                                          (catch :default error (reject error))))))))]
        (.on request "error" reject)
        (.setTimeout request timeout-ms #(.destroy request (js/Error. "Fixture request timeout")))
        (.end request (js/JSON.stringify (clj->js json))))))))

(defn- upstream-handler [actor mode upstream-requests]
  (fn [request response]
                                (let [chunks (atom [])]
                                  (.on request "data" #(swap! chunks conj %))
                                  (.on request "end"
                                       (fn []
                                         (swap! upstream-requests conj (.-url request))
                                         (let [body (js->clj (js/JSON.parse (.toString (.concat js/Buffer (into-array @chunks))))
                                                            :keywordize-keys true)
                                               status (cond (= :redirect @mode) 302
                                                            (= :unavailable @mode) 503
                                                            (not= "remote fixture password" (:password body)) 401
                                                            :else 200)]
                                           (.writeHead response status #js {"Content-Type" "application/json"
                                                                          "Location" "/must-not-follow"})
                                           (.end response (js/JSON.stringify
                                                           (clj->js {:actor (if (= :malformed @mode) (assoc actor :status "disabled") actor)})))))))))

(defn ^:async upstream!
  "Start an owned TLS listener with a private test CA; production TLS checks stay enabled."
  [directory actor]
  (let [key-path (path/join directory "authority.key")
        certificate-path (path/join directory "authority.crt")
        _ (child/execFileSync "openssl"
                              #js ["req" "-x509" "-newkey" "rsa:2048" "-nodes" "-days" "1"
                                   "-keyout" key-path "-out" certificate-path "-subj" "/CN=localhost"
                                   "-addext" "subjectAltName=DNS:localhost,IP:127.0.0.1"]
                              #js {:stdio "ignore"})
        certificate (fs/readFileSync certificate-path)
        mode (atom :valid)
        requests (atom [])
        upstream-requests (atom [])
        server (.createServer https #js {:key (fs/readFileSync key-path) :cert certificate}
                              (upstream-handler actor mode upstream-requests))
        _ (await (js/Promise. (fn [resolve reject]
                               (.once server "error" reject)
                               (.listen server 0 "127.0.0.1" resolve))))]
    {:origin (str "https://127.0.0.1:" (.-port (.address server)))
     :mode mode :requests requests :upstream-requests upstream-requests
     :client (reify fetch/IHttpClient
               (json! [_ request] (request! certificate requests request)))
     :close! (fn [] (js/Promise. (fn [resolve reject]
                                  (.close server (fn [error] (if error (reject error) (resolve nil)))))))}))
