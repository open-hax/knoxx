(ns knoxx.backend.extern.fetch
  "Native fetch boundary.

   Non-extern namespaces should depend on the IHttpClient protocol and pass
   CLJS request maps. This namespace owns AbortController, js/fetch,
   RequestInit construction, Response parsing, and JSON conversion."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.json :as xjson]
            [promesa.core :as p]))

(defprotocol IHttpClient
  (response! [client request]
    "Execute request and resolve with the native Response object.
     Compatibility escape hatch; prefer json!, text!, or array-buffer!.")
  (json! [client request]
    "Execute request and resolve with {:ok :status :body :headers}.
     :body and :headers are CLJS data.")
  (text! [client request]
    "Execute request and resolve with {:ok :status :body :headers} where :body is text.")
  (array-buffer! [client request]
    "Execute request and resolve with {:ok :status :body :headers} where :body is an ArrayBuffer."))

(defn- option-key
  [k]
  (if (keyword? k) (name k) (str k)))

(defn- js-option-value
  [k value]
  (cond
    (= k "headers") (if (map? value) (clj->js value) value)
    (or (map? value) (vector? value) (set? value)) (clj->js value)
    :else value))

(defn- opts->js
  [opts signal]
  (let [out (js/Object.)]
    (when opts
      (if (map? opts)
        (doseq [[k value] opts]
          (when (some? value)
            (let [jk (option-key k)]
              (aset out jk (js-option-value jk value)))))
        (js/Object.assign out opts)))
    (when signal
      (aset out "signal" signal))
    out))

(defn- response->base-map
  [resp]
  {:ok (.-ok resp)
   :status (.-status resp)
   :headers (let [headers (.-headers resp)
                  acc (atom {})]
              (when headers
                (.forEach headers
                          (fn [value key]
                            (swap! acc assoc (str/lower-case key) value))))
              @acc)})

(defn- parse-json-text
  [text]
  (if (str/blank? text)
    {}
    (try
      (xjson/to-cljs (.parse js/JSON text))
      (catch :default _
        {:raw text}))))

(defrecord NativeFetchClient [default-timeout-ms fetch-fn]
  IHttpClient
  (response! [_ {:keys [url opts timeout-ms]}]
    (let [controller (js/AbortController.)
          effective-timeout-ms (or timeout-ms default-timeout-ms 30000)
          timeout-id (js/setTimeout #(.abort controller) effective-timeout-ms)
          request-init (opts->js opts (.-signal controller))
          do-fetch (or fetch-fn js/fetch)]
      (-> (do-fetch url request-init)
          (.finally (fn [] (js/clearTimeout timeout-id))))))
  (json! [client request]
    (p/let [resp (response! client request)
            body (.text resp)]
      (assoc (response->base-map resp)
             :body (parse-json-text body))))
  (text! [client request]
    (p/let [resp (response! client request)
            body (.text resp)]
      (assoc (response->base-map resp) :body body)))
  (array-buffer! [client request]
    (p/let [resp (response! client request)
            body (.arrayBuffer resp)]
      (assoc (response->base-map resp) :body body))))

(def default-client
  (->NativeFetchClient 30000 nil))

(defn native-client
  ([] (native-client {}))
  ([{:keys [default-timeout-ms fetch-fn]}]
   (->NativeFetchClient (or default-timeout-ms 30000) fetch-fn)))
