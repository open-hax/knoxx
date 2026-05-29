(ns knoxx.backend.domain.action.run-pipeline-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.action.run-pipeline]))

(deftest ^:async run-pipeline-rejects-without-pipeline-id
  (try
    (await (action-registry/run-action!
            {:config {} :trigger {}}
            {:action/kind :actions/run-pipeline
             :action/with {}}))
    (is false "should have rejected")
    (catch :default err
      (is (str/includes? (.-message err) ":pipeline-id")))))

(deftest ^:async run-pipeline-rejects-when-pipeline-not-found
  (try
    (await (action-registry/run-action!
            {:config {} :trigger {}}
            {:action/kind :actions/run-pipeline
             :action/with {:pipeline-id "nonexistent"}}))
    (is false "should have rejected")
    (catch :default err
      (is (str/includes? (.-message err) "not found")))))
