(ns knoxx.backend.infra.agent.provider
  (:require [clojure.string :as str]
            [knoxx.backend.domain.models :refer [allowlisted-model-id?]]
            [knoxx.backend.infra.clients.proxx :as proxx-client]))

(defn- response-model-items
  [resp]
  (or (get-in resp [:body :data]) []))

(defn- item-model-id
  [item]
  (let [raw (when (map? item) (:id item))]
    (when (and raw (not (str/blank? (str raw))))
      (str raw))))

(defn ^:async fetch-proxx-model-ids!
  "Fetch available model ids from Proxx /v1/models so Knoxx's eta-mu model registry includes
   local Ollama (gemma4, qwen, etc) as well as upstream hosted models.

   Returns a Promise of vector of strings."
  [config]
  (if-not (proxx-client/configured? config)
    []
    (try
      (let [resp (await (proxx-client/models! (proxx-client/client config)))]
        (if (:ok resp)
          (->> (response-model-items resp)
               (map item-model-id)
               (remove nil?)
               (filter (fn [model-id]
                         (allowlisted-model-id? config model-id)))
               distinct
               vec)
          []))
      (catch :default _err
        ;; Keep Knoxx running even if Proxx is offline or auth fails.
        []))))
