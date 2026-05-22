(ns knoxx.backend.extern.pg
  "Thin extern wrapper around the `pg` npm Pool.

   All query results are returned as promises of keywordized CLJS maps so
   callers never touch JS objects or aget. The pool itself is a JS object
   that stays here at the boundary."
  (:require ["pg" :as pg-lib]))

(defn create-pool!
  [{:keys [connection-string max idle-timeout-ms connect-timeout-ms]}]
  (new (.-Pool pg-lib)
       (clj->js {:connectionString    connection-string
                 :max                 (or max 6)
                 :idleTimeoutMillis   (or idle-timeout-ms 30000)
                 :connectionTimeoutMillis (or connect-timeout-ms 15000)
                 :allowExitOnIdle     true})))

(defn- keywordize-rows
  [result]
  ;; pg returns Array<QueryResult> for multi-statement simple queries (e.g. DDL
  ;; migrations). Take the last element; for single statements result is already
  ;; a QueryResult.
  (let [r (if (.isArray js/Array result)
             (aget result (dec (.-length result)))
             result)]
    {:rows      (mapv #(js->clj % :keywordize-keys true) (array-seq (.-rows r)))
     :row-count (.-rowCount r)}))

(defn query!
  "Execute parameterized SQL against pool-or-client.
   Returns Promise<{:rows [keywordized-CLJS-maps] :row-count N}>."
  [conn sql-str params]
  (let [params-arr (when (seq params) (into-array params))]
    (-> (.query conn sql-str params-arr)
        (.then keywordize-rows))))

(defn query-one!
  "Execute SQL and return Promise<first-row-as-CLJS-map | nil>."
  [conn sql-str params]
  (-> (query! conn sql-str params)
      (.then (fn [{:keys [rows]}] (first rows)))))

(defn with-transaction!
  "Run (f client) inside a PG transaction.
   f receives a pg client; pass it to query!/query-one! in place of the pool."
  [pool f]
  (-> (.connect pool)
      (.then
       (fn [client]
         (-> (.query client "BEGIN")
             (.then (fn [] (f client)))
             (.then (fn [result]
                      (-> (.query client "COMMIT")
                          (.then (fn [] (.release client true)))
                          (.then (fn [] result)))))
             (.catch (fn [err]
                       (-> (.query client "ROLLBACK")
                           (.then (fn [] (.release client false)))
                           (.then (fn [] (throw err)))))))))))

(defn on-pool-error!
  "Attach an error handler to the pool."
  [pool handler]
  (.on pool "error" handler))

(defn on-pool-connect!
  "Attach a connect handler to the pool."
  [pool handler]
  (.on pool "connect" handler))

(defn end-pool!
  "Drain the pool."
  [pool]
  (.end pool))
