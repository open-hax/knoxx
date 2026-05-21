(ns knoxx.backend.domain.action.run-pipeline-test
  (:require [clojure.test :refer [deftest is testing async]]
            [clojure.string :as str]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.action.run-pipeline]))

(deftest run-pipeline-rejects-without-pipeline-id
  (async done
    (let [promise (action-registry/run-action!
                   {:config {} :trigger {}}
                   {:action/kind :actions/run-pipeline
                    :action/with {}})]
      (.catch promise
              (fn [err]
                (is (str/includes? (.-message err) ":pipeline-id"))
                (done))))))

(deftest run-pipeline-rejects-when-pipeline-not-found
  (async done
    (let [promise (action-registry/run-action!
                   {:config {} :trigger {}}
                   {:action/kind :actions/run-pipeline
                    :action/with {:pipeline-id "nonexistent"}})]
      (.catch promise
              (fn [err]
                (is (str/includes? (.-message err) "not found"))
                (done))))))
