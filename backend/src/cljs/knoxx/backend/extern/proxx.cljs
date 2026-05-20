(ns knoxx.backend.extern.proxx
  "JS HTTP/JSON boundary for Proxx-compatible APIs."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.json :as xjson]
            [knoxx.backend.infra.http :as backend-http]))

(defn- trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn- bearer-headers
  [token]
  (let [headers #js {"Content-Type" "application/json"}]
    (when-not (str/blank? token)
      (aset headers "Authorization" (str "Bearer " token)))
    headers))

(defn- chat-completions-url
  [config]
  (str (trim-trailing-slashes (:proxx-base-url config)) "/v1/chat/completions"))

(defn- normalize-chat-completion-response
  [resp]
  (let [ok? (true? (aget resp "ok"))
        status (aget resp "status")
        body (xjson/to-cljs (aget resp "body"))
        first-choice (first (:choices body))
        message (:message first-choice)
        content (or (:content message)
                    (:text first-choice)
                    "")
        reasoning-content (or (:reasoning_content message)
                              (:reasoningContent message)
                              "")]
    (cond-> {:ok? ok?
             :status status}
      (:model body) (assoc :model (:model body))
      (seq (str content)) (assoc :content content)
      (seq (str reasoning-content)) (assoc :reasoning-content reasoning-content)
      (some? body) (assoc :body body))))

(defn chat-completion-with-fetch!
  "POST a CLJS chat completion request map through fetch-json and return a CLJS
   response map. Accepts fetch-json as an argument so tests do not mutate global
   vars while async suites are running."
  [fetch-json config request]
  (-> (fetch-json
       (chat-completions-url config)
       #js {:method "POST"
            :headers (bearer-headers (:proxx-auth-token config))
            :body (xjson/stringify request)})
      (.then normalize-chat-completion-response)))

(defn chat-completion!
  "POST a CLJS chat completion request map to Proxx and return a CLJS response
   map containing :ok?, :status, :model, :content, and :reasoning-content when
   present. Raw JS fetch/JSON shapes are confined here."
  [config request]
  (chat-completion-with-fetch! backend-http/fetch-json config request))
