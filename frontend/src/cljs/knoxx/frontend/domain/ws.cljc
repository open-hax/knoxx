(ns knoxx.frontend.domain.ws
  "Pure token packet decisions; ordinary JavaScript coercion stays at the edge."
  (:require [knoxx.frontend.law.ws :as law]))

(defn decode-token
  "Decode a delta or explicit replacement; malformed correction data returns nil."
  [payload ordinary-token]
  (when (law/decode-input-valid? {:payload payload :ordinary-token ordinary-token})
    (let [metadata {:run-id (when (string? (:run_id payload)) (:run_id payload))
                    :kind (when (string? (:kind payload)) (:kind payload))}]
      (cond
        (contains? payload :operation)
        (when (law/replacement-valid? payload)
          {:token (:token payload)
           :meta (assoc metadata :operation "replace" :offset (:offset payload))})
        (contains? payload :offset) nil
        :else {:token ordinary-token :meta metadata}))))
