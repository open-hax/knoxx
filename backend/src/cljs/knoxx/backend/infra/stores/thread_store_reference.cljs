(ns knoxx.backend.infra.stores.thread-store-reference
  "Disposable atom projection of pure, timestamped thread operations."
  (:require [knoxx.backend.domain.thread-store :as domain]
            [knoxx.backend.shape.thread-recovery :as recovery]))

(defn projection "Create empty reference state for canonical replay." []
  (let [state (atom domain/empty-state)] {:store state :snapshot #(deref state)}))

(defn admit!
  "Apply a previously stamped transition; no host clock is read during replay."
  [state operation]
  (let [{next-state :state result :result} (domain/transition @state operation)]
    (reset! state next-state) result))

(defn- observed [state value]
  (when value
    (with-meta value {recovery/view-key (domain/startup-view state (:session_id value))})))

(def reads
  "All finite read operations evaluate one explicitly supplied view clock."
  {:thread/startup-view (fn [state id] (domain/startup-view @state id))
   :thread/read (fn [state thread-id at] (observed @state (domain/visible-thread @state thread-id at)))
   :thread/conversation (fn [state conversation-id at] (observed @state (domain/conversation-thread @state conversation-id at)))
   :thread/active (fn [state at] (mapv #(observed @state %) (domain/active-threads @state at)))})
