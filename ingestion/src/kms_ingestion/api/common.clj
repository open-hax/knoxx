(ns kms-ingestion.api.common
  "Shared request/response helpers for the ingestion API."
  (:require
   [cheshire.core :as json]
   [clojure.string :as str])
  (:import
   [java.io InputStream Reader]))

(defn json->clj
  "Parse JSON string or PGobject to Clojure data."
  [s]
  (when s
    (cond
      (string? s)
      (json/parse-string s keyword)

      (instance? org.postgresql.util.PGobject s)
      (json/parse-string (.getValue ^org.postgresql.util.PGobject s) keyword)

      :else
      s)))

(defn clj->json
  "Serialize Clojure data to JSON string."
  [x]
  (json/generate-string x))

(defn uuid-str
  "Convert UUID-like values to string."
  [x]
  (str x))

(defn ts-str
  "Convert SQL timestamp to ISO-8601 UTC string."
  [x]
  (when x
    (if (instance? java.sql.Timestamp x)
      (str (.toInstant ^java.sql.Timestamp x))
      (str x))))

(defn get-tenant-id
  "Extract tenant-id from request."
  [request]
  (or (-> request :params :tenant_id)
      (get (:params request) "tenant_id")
      (-> request :query-params :tenant_id)
      (get (:query-params request) "tenant_id")
      "devel"))

(defn timeout-error?
  [value]
  (let [s (some-> value str str/lower-case)]
    (boolean (and s (or (str/includes? s "timed out")
                        (str/includes? s "timeout"))))))

(defn request-body->map
  "Return a request body as a Clojure map regardless of whether muuntaja decoded it.
   Falls back to parsing raw JSON from InputStream/Reader/string bodies."
  [request]
  (let [body (or (:body-params request) (:body request))]
    (cond
      (map? body)
      body

      (string? body)
      (or (json/parse-string body keyword) {})

      (instance? InputStream body)
      (let [s (slurp body)]
        (if (str/blank? s)
          {}
          (json/parse-string s keyword)))

      (instance? Reader body)
      (let [s (slurp body)]
        (if (str/blank? s)
          {}
          (json/parse-string s keyword)))

      :else
      {})))
