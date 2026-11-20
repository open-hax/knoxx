(ns knoxx.backend.extern.platform-action
  "Own opaque tool execution/results and typed exception decoding.")

(defn- data-map
  [value]
  (cond (map? value) value
        (some? value) (js->clj value :keywordize-keys true)
        :else {}))

(defn ^:async execute!
  "Capture the original result or error without inferring status from prose."
  [invoke]
  (try
    (let [value (await (invoke))
          details (if (map? value) (:details value) (some-> value (aget "details")))
          failed? (true? (if (map? value) (:isError value) (some-> value (aget "isError"))))]
      {:value value :failed? failed? :details (data-map details)})
    (catch :default error
      {:error error :failed? true
       :error-data (merge (cond-> {}
                            (integer? (.-status error)) (assoc :http-status (.-status error))
                            (integer? (.-code error)) (assoc :platform-code (.-code error)))
                          (ex-data error))})))

(defn outcome-message
  "Short stage/outcome wording, derived from execution evidence rather than model prose."
  [{:keys [status http-status]}]
  (str "Write outcome: " (name status)
       (when http-status (str " (HTTP " http-status ")")) ". "
       (case status
         :not-attempted "No platform write was attempted; correct the preflight failure."
         :confirmed "A platform confirmation was returned; inspect its actual identifiers."
         :refused (if (contains? #{401 403 429} http-status)
                    "Stop this effect for this audience in this run."
                    "This request was explicitly refused; a changed request still needs normal admission.")
         :partial "Some messages were delivered. Do not retry or switch aliases for this audience."
         :unknown "The outcome is unresolved. Do not retry or switch aliases for this audience.")))

(defn result-with-outcome
  "Preserve the tool's result fields and opaque media; append truthful failure
   wording and structured outcome metadata without mutating the original value."
  [value outcome]
  (let [metadata (clj->js outcome)
        details (if (map? value) (:details value) (some-> value (aget "details")))
        failed? (true? (if (map? value) (:isError value) (some-> value (aget "isError"))))
        content (if (map? value) (:content value) (some-> value (aget "content")))
        content (if (array? content) (vec (array-seq content)) (vec (or content [])))
        content (cond-> content failed? (conj #js {:type "text" :text (outcome-message outcome)}))]
    (if (map? value)
      (assoc value :details (assoc (or details {}) :actionOutcome outcome) :content content)
      (.assign js/Object (js-obj) value
               #js {:details (.assign js/Object (js-obj) details #js {:actionOutcome metadata})
                    :content (into-array content)}))))

(defn failure-with-outcome
  "Keep the original typed cause and message, adding an explicit action outcome."
  [error outcome]
  (ex-info (str (ex-message error) " " (outcome-message outcome))
           (assoc (or (ex-data error) {}) :action-outcome outcome)
           error))
