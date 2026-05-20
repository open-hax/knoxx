(defn- proxx-models-url
  [config]
  (let [base (str (or (:proxx-base-url config) ""))]
    (cond
      (str/ends-with? base "/v1") (str base "/models")
      (str/ends-with? base "/v1/") (str base "models")
      (str/ends-with? base "/") (str base "v1/models")
      :else (str base "/v1/models"))))

(defn- fetch-proxx-model-ids!
  "Fetch available model ids from Proxx /v1/models so Knoxx's eta-mu model registry includes
   local Ollama (gemma4, qwen, etc) as well as upstream hosted models.

   Returns a Promise of vector of strings."
  [config]
  (let [token (str (or (:proxx-auth-token config) ""))
        url (proxx-models-url config)]
    (if (str/blank? token)
      (js/Promise.resolve [])
      (-> (js/fetch url #js {:headers #js {"Authorization" (str "Bearer " token)
                                           "Accept" "application/json"}})
          (.then (fn [resp]
                   (if (aget resp "ok")
                     (.json resp)
                     (js/Promise.reject (js/Error. (str "Proxx /v1/models failed with status " (aget resp "status")))))))
          (.then (fn [payload]
                   (let [items (js-array-seq (or (aget payload "data") #js []))
                         ids (->> items
                                  (map (fn [item]
                                         (let [raw (aget item "id")]
                                           (when (and raw (not (str/blank? (str raw))))
                                             (str raw)))))
                                  (remove nil?)
                                  (filter (fn [model-id]
                                            (allowlisted-model-id? config model-id)))
                                  distinct
                                  vec)]
                     ids)))
          (.catch (fn [_err]
                    ;; Keep Knoxx running even if Proxx is offline or auth fails.
                    (js/Promise.resolve [])))))))
