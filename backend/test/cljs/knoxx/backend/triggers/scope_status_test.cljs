(ns knoxx.backend.triggers.scope-status-test
  "The loaded runtime catalog exposes the same scope opt-in as the start action."
  (:require [cljs.test :as t]
            [knoxx.backend.domain.event.dispatch :as dispatch]
            [knoxx.backend.domain.resources.loader :as resources]))

(defn- entry [id arguments]
  {:resource/kind :trigger :resource/id id
   :resource/definition {:trigger/id id :trigger/kind :event
                         :trigger/listener "pi" :trigger/emitter "knoxx-publication"
                         :trigger/events [:publication/translation-needed]
                         :trigger/action :actions/start-agent-session
                         :trigger/with (merge {:agent-id "publication_translator"}
                                             arguments)}})

(t/deftest resolved-catalog-reports-real-scope-opt-in-values
  (let [fixtures [(entry "scoped" {:scope-from-event true})
                  (entry "disabled" {:scope-from-event false})
                  (entry "absent" {})
                  (entry "alias" {:scopeFromEvent true})]
        observed (atom nil)]
    (with-redefs [resources/load-all-resources-sync
                  (fn [config] (reset! observed config) fixtures)]
      (let [snapshot (dispatch/status-snapshot {:contracts-dir "explicit-fixture-root"})
            triggers (:triggers snapshot)]
        (t/is (= {:contracts-dir "explicit-fixture-root"} @observed))
        (t/is (= [["scoped" true] ["disabled" false] ["absent" false] ["alias" true]]
                 (mapv (juxt :id :scopeFromEvent) triggers)))
        (t/is (every? #(= "knoxx-publication" (:emitter %)) triggers))
        (t/is (every? #(= "publication_translator" (:agent %)) triggers))
        (t/is (every? #(false? (:resourcePoliciesFromEvent %)) triggers))
        (t/is (every? #(false? (:executionSnapshotFromEvent %)) triggers))))))
