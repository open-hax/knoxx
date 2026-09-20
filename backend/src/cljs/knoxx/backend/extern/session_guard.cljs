(ns knoxx.backend.extern.session-guard
  "Fastify callback transport for the application's resolved session context."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.law.error-body :as error-body]))

(defn- ^:async outcome!
  [resolve! request]
  (try
    {:ok? true :context (await (resolve! request))}
    (catch :default error
      {:ok? false :error error})))

(defn- transport-error
  [error]
  (let [status (fastify/error-status error)
        code (fastify/error-code error)
        stable-code (if (and (string? code) (not (str/blank? code)))
                      code "session_unavailable")]
    (fastify/http-error status stable-code
                        (:detail (error-body/error-body error status)))))

(defn- ^:async attach!
  [resolve! required? request done]
  (let [{:keys [ok? context error]} (await (outcome! resolve! request))]
    (if (and required? (not ok?))
      (done (transport-error error))
      (do
        (set! (.-ctx ^js request) context)
        (done)))))

(defn pre-handler
  "Adapt session resolution without returning a promise from a callback hook."
  [resolve! required?]
  (fn [request _reply done]
    (attach! resolve! required? request done)
    nil))
