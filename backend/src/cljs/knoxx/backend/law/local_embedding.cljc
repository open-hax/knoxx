(ns knoxx.backend.law.local-embedding
  "Portable contract for local embedding HTTP results.")

(defn- fail! [status code message]
  (throw (ex-info message {:status status :code code})))

(defn- finite-number? [value]
  (and (number? value) (= value value) (not= value ##Inf) (not= value ##-Inf)))

(defn checked-result!
  "Require exact provider identity and one aligned finite vector for every input."
  [model dimensions input-count response]
  (let [body (:body response)
        entries (:data body)]
    (when-not (and (true? (:ok response)) (= 200 (:status response))
                   (map? body) (not (contains? body :error)) (= model (:model body))
                   (vector? entries) (= input-count (count entries))
                   (= (set (range input-count)) (set (map :index entries)))
                   (every? #(and (map? %) (integer? (:index %))
                                  (vector? (:embedding %))
                                  (= dimensions (count (:embedding %)))
                                  (every? finite-number? (:embedding %))) entries))
      (fail! 502 "embedding_invalid_response" "Embedding provider returned inconsistent model, indices or vectors"))
    {:model model :dimensions dimensions :vectors (mapv :embedding (sort-by :index entries))}))
