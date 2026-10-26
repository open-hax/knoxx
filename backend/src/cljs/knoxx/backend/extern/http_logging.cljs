(ns knoxx.backend.extern.http-logging
  "Own native Fastify/Pino logger configuration without exposing request credentials.")

(defn options
  "Return native logger options; an optional writable is an opaque trusted sink."
  ([] (options nil))
  ([output]
   (let [settings #js {:redact #js ["req.url" "req.headers.authorization" "req.headers.cookie"]}]
     (when output (aset settings "stream" output))
     settings)))
