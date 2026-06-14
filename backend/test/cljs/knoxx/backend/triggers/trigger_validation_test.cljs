(ns knoxx.backend.triggers.trigger-validation-test
  "Verify the fixture trigger/source contracts pass parsing and Malli validation.
   Uses test/fixtures snapshots — the live contracts/ folder is operator-owned
   and must not be validated by the test suite."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.law.contracts :as law-contracts]))

(def fixture-config
  {:contracts-dir "test/fixtures/trigger-contracts"})

(deftest ussyverse-trigger-parses-and-validates
  (testing "the ussyverse trigger file parses and validates successfully"
    (let [file-path (.join (js/require "node:path")
                           (first (contract-loader/contract-root-paths fixture-config))
                           "triggers"
                           "ussyverse_social_replies_event.edn")
          edn-text (.readFileSync (js/require "node:fs") file-path "utf8")
          record (first (contract-loader/parse-contract-file-records! file-path edn-text))]
      (is (some? record)
          (str "Trigger should parse and validate. File: " file-path))
      (when record
        (is (= "ussyverse_social_replies_event" (:id record)))
        (is (= "triggers" (:contractClass record)))
        (let [validation (law-contracts/validate "triggers" (:contract record))]
          (is (true? (:ok validation))
              (str "Trigger should pass Malli validation. Errors: " (pr-str (:errors validation)))))))))

(deftest discord-source-parses-and-validates
  (testing "the discord gateway source file parses and validates successfully"
    (let [file-path (.join (js/require "node:path")
                           (first (contract-loader/contract-root-paths fixture-config))
                           "sources"
                           "discord_gateway.edn")
          edn-text (.readFileSync (js/require "node:fs") file-path "utf8")
          record (first (contract-loader/parse-contract-file-records! file-path edn-text))]
      (is (some? record)
          (str "Source should parse and validate. File: " file-path))
      (when record
        (is (= "discord_gateway" (:id record)))
        (is (= "sources" (:contractClass record)))
        (let [validation (law-contracts/validate "sources" (:contract record))]
          (is (true? (:ok validation))
              (str "Source should pass Malli validation. Errors: " (pr-str (:errors validation)))))))))
