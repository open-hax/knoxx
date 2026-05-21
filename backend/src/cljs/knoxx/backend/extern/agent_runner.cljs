(ns knoxx.backend.extern.agent-runner
  "JS boundary helpers for agent runner inputs and diagnostics.
   JS->CLJS input normalization, JS error property access, JSON formatting, and
   console error logging live here."
  (:require [clojure.string :as str]))

(defn to-cljs
  [value]
  (cond
    (nil? value) nil
    (map? value) value
    (vector? value) value
    :else (js->clj value :keywordize-keys true)))

(defn to-cljs-map
  [value]
  (let [value (to-cljs value)]
    (if (map? value) value {})))

(defn err-prop
  [err prop]
  (try
    (aget err prop)
    (catch :default _ nil)))

(defn err-message
  [err]
  (or (some-> (err-prop err "message") str str/trim not-empty)
      (some-> err str str/trim not-empty)
      "Unknown error"))

(defn- safe-ex-data
  [err]
  (try
    (ex-data err)
    (catch :default _ nil)))

(defn safe-json
  [value]
  (try
    (.stringify js/JSON (clj->js value) nil 2)
    (catch :default _
      (str value))))

(defn error-diagnostic
  [body err]
  (cond-> {:message (err-message err)
           :runId (:run-id body)
           :conversationId (:conversation-id body)
           :sessionId (:session-id body)
           :model (or (:model body)
                      (get-in body [:agent-spec :model]))
           :contractId (get-in body [:agent-spec :contract-id])
           :actorId (get-in body [:agent-spec :actor-id])}
    (some-> (err-prop err "name") str str/trim not-empty)
    (assoc :name (str (err-prop err "name")))
    (some-> (err-prop err "stack") str str/trim not-empty)
    (assoc :stack (str (err-prop err "stack")))
    (safe-ex-data err)
    (assoc :data (safe-ex-data err))))

(defn log-async-spawn-error!
  [body err]
  (let [diagnostic (error-diagnostic body err)]
    (.error js/console "[agents.runner] async direct spawn failed" (safe-json diagnostic))
    (when-let [stack (:stack diagnostic)]
      (.error js/console "[agents.runner] stack\n" stack))))
