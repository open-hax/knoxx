(ns knoxx.backend.extern.event-turn-admission
  "Native promise ownership for pending durable FIFO admissions.")

(defn gate
  "Return a waitable admission result and its one-shot completion callback.
  Failures resolve as data so a removed pending entry cannot leak a rejected promise."
  []
  (let [complete* (atom nil)
        promise (js/Promise. (fn [complete _reject] (reset! complete* complete)))]
    {:promise promise :complete! (fn [error] (@complete* error))}))
