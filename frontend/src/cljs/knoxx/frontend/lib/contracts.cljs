(ns knoxx.frontend.lib.contracts
  "Named boundary for the legacy contract transport and its Vite configuration.
   Keep API base, development identity defaults, and error formatting unchanged."
  (:require ["@open-hax/knoxx-frontend-bridge" :as bridge]))

(defn- decode [value]
  (js->clj value :keywordize-keys true))

(defn ^:async list-contracts
  "Decode contract listings from the configured browser transport."
  ([] (decode (await (bridge/listContracts))))
  ([contract-class] (decode (await (bridge/listContracts contract-class)))))

(defn ^:async get-contract
  "Decode contract EDN and validation results from the browser transport."
  [contract-id contract-class]
  (decode (await (bridge/getContract contract-id contract-class))))

(defn ^:async save-contract
  "Save contract EDN through the configured browser transport."
  [contract-id edn-text contract-class]
  (decode (await (bridge/saveContract contract-id edn-text contract-class))))

(defn ^:async validate-contract
  "Validate contract EDN through the configured browser transport."
  [edn-text contract-class]
  (decode (await (bridge/validateContract edn-text contract-class))))

(defn ^:async copy-contract
  "Copy a contract through the configured browser transport."
  [source-id new-id contract-class]
  (decode (await (bridge/copyContract source-id new-id contract-class))))
