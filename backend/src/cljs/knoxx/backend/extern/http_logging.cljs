(ns knoxx.backend.extern.http-logging
  "Own native Fastify/Pino logger configuration without exposing request credentials."
  (:require [clojure.string :as str]))

(defn- request-record
  "Serialize only request metadata; never copy query parameters or headers."
  [request]
  #js {:method (.-method request)
       :path (first (str/split (or (.-url request) "") #"\?" 2))
       :host (or (.-host request) (some-> request .-headers (aget "host")))
       :remoteAddress (.-ip request)})

(defn options
  "Return native logger options; an optional writable is an opaque trusted sink."
  ([] (options nil))
  ([output]
   (let [settings #js {:redact #js ["req.url" "req.headers.authorization" "req.headers.cookie"]
                       :serializers #js {:req request-record}}]
     (when output (aset settings "stream" output))
     settings)))
