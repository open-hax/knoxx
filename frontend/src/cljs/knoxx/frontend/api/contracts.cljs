(ns knoxx.frontend.api.contracts
  "CLJS-native API client for contract administration endpoints."
  (:require ["@open-hax/knoxx-frontend-bridge" :as bridge]))

(defn- kw [js-obj]
  (js->clj js-obj :keywordize-keys true))

(defn list-contracts
  ([]
   (-> (bridge/listContracts)
       (.then kw)))
  ([contract-class]
   (-> (bridge/listContracts contract-class)
       (.then kw))))

(defn get-contract [contract-id contract-class]
  (-> (bridge/getContract contract-id contract-class)
      (.then kw)))

(defn save-contract [contract-id edn-text contract-class]
  (-> (bridge/saveContract contract-id edn-text contract-class)
      (.then kw)))

(defn validate-contract [edn-text contract-class]
  (-> (bridge/validateContract edn-text contract-class)
      (.then kw)))

(defn copy-contract [source-id new-id contract-class]
  (-> (bridge/copyContract source-id new-id contract-class)
      (.then kw)))
