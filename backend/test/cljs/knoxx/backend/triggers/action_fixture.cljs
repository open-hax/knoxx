(ns knoxx.backend.triggers.action-fixture
  "Scoped action effects for trigger/source tests; real matching and dispatch stay intact."
  (:require [knoxx.backend.domain.action.registry :as action-registry]))

(defn recording-fixture
  "Capture each start-agent invocation and restore the real MultiFn method after every test."
  ([calls]
   (recording-fixture calls (fn [_ctx _action] (js/Promise.resolve {:ok true}))))
  ([calls respond!]
   (let [kind :actions/start-agent-session
         original (atom nil)]
     {:before (fn []
                (reset! calls [])
                (reset! original (get (methods action-registry/run-action!) kind))
                (-add-method action-registry/run-action! kind
                             (fn [ctx action]
                               (swap! calls conj {:ctx ctx :action action})
                               (respond! ctx action))))
      :after (fn []
               (if-let [method @original]
                 (-add-method action-registry/run-action! kind method)
                 (remove-method action-registry/run-action! kind)))})))
