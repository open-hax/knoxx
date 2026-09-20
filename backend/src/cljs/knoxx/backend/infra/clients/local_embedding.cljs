(ns knoxx.backend.infra.clients.local-embedding
  "Checked local embedding transport; configuration is required only when invoked."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.law.local-embedding :as law]))

(defn- fail! [status code message]
  (throw (ex-info message {:status status :code code})))

(defn- checked-config! [config]
  (let [base (:embed-provider-base-url config)
        model (:embed-provider-model config)
        dimensions (:embed-provider-dimensions config)]
    (when-not (and (string? base) (not (str/blank? base))
                   (string? model) (not (str/blank? model))
                   (integer? dimensions) (pos? dimensions))
      (fail! 503 "embedding_unconfigured" "Local embedding endpoint, model and dimensions are required"))
    (when-not (re-matches #"https?://(?:127\.0\.0\.1|localhost|\[::1\])(?::[0-9]+)?(?:/v1)?/?" base)
      (fail! 503 "embedding_endpoint_invalid" "Local embeddings require a loopback endpoint"))
    {:base base :model model :dimensions dimensions}))

(defn ^:async embed!
  "Embed a nonempty vector of texts and validate exact model, dimensions and aligned finite vectors."
  [config texts]
  (when-not (and (vector? texts) (seq texts)
                 (every? #(and (string? %) (not (str/blank? %))) texts))
    (fail! 400 "embedding_invalid_input" "Embedding input must be a nonempty vector of nonblank strings"))
  (let [{:keys [base model dimensions]} (checked-config! config)]
    (try
      (law/checked-result!
       model dimensions (count texts)
       (await (xfetch/json!
               xfetch/default-client
               {:url (str (str/replace base #"/+$" "") "/embeddings")
                :timeout-ms 120000
                :opts {:method "POST" :headers {"Content-Type" "application/json"}
                       :json {:model model :input texts :dimensions dimensions :encoding_format "float"}}})))
      (catch :default error
        (if (ex-data error) (throw error)
            (fail! 502 "embedding_transport_failed" "Local embedding transport failed"))))))
