(ns knoxx.frontend.api.contracts
  "CLJS API for contract administration with a named browser transport boundary."
  (:require [knoxx.frontend.lib.contracts :as transport]))

(defn list-contracts
  "List contracts, optionally filtering by their contract class."
  ([] (transport/list-contracts))
  ([contract-class] (transport/list-contracts contract-class)))

(defn get-contract
  "Fetch contract EDN and validation results for the selected class."
  [contract-id contract-class]
  (transport/get-contract contract-id contract-class))

(defn save-contract
  "Save raw contract EDN in the selected class."
  [contract-id edn-text contract-class]
  (transport/save-contract contract-id edn-text contract-class))

(defn validate-contract
  "Validate contract EDN without saving a contract."
  [edn-text contract-class]
  (transport/validate-contract edn-text contract-class))

(defn copy-contract
  "Copy a contract to a new identity in the selected class."
  [source-id new-id contract-class]
  (transport/copy-contract source-id new-id contract-class))
