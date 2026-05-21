(ns knoxx.backend.infra.agent.hydration-sources
  "Hydration source ports for semantic/RAG and conversational memory context."
  (:require [knoxx.backend.infra.agent.hydration :as hydration]))

(defprotocol IHydrationSource
  (hydrate [source hydration-request]))

(defn semantic-hydrate!
  [runtime config {:keys [mode message auth-context]}]
  (hydration/passive-hydration! runtime config mode message auth-context))

(defn memory-hydrate!
  [config {:keys [conversation-id message auth-context agent-spec]}]
  (hydration/passive-memory-hydration! config conversation-id message auth-context agent-spec))

(defrecord SemanticHydrationSource [runtime config]
  IHydrationSource
  (hydrate [_ request]
    (semantic-hydrate! runtime config request)))

(defrecord MemoryHydrationSource [config]
  IHydrationSource
  (hydrate [_ request]
    (memory-hydrate! config request)))

(defrecord CompositeHydrationSource [sources]
  IHydrationSource
  (hydrate [_ request]
    (-> (.all js/Promise (clj->js (mapv #(hydrate % request) sources)))
        (.then (fn [results]
                 (vec (js->clj results :keywordize-keys true)))))))

(defn semantic-source
  [runtime config]
  (->SemanticHydrationSource runtime config))

(defn memory-source
  [config]
  (->MemoryHydrationSource config))

(defn composite-source
  [sources]
  (->CompositeHydrationSource (vec sources)))
