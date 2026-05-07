(ns knoxx.backend.contract-loader
  "Backwards-compat shim for legacy knoxx.backend.contract-loader requires.

   Contract loading now lives under knoxx.backend.contracts.loader; keep this
   top-level alias so in-progress action work or stale requires do not break the
   shadow build while callers migrate."
  (:require [knoxx.backend.contracts.loader :as impl]))

(def contract-class-order impl/contract-class-order)
(def safe-path-segment! impl/safe-path-segment!)
(def normalize-contract-class impl/normalize-contract-class)
(def contract-root-paths impl/contract-root-paths)
(def contracts-dir-path impl/contracts-dir-path)
(def contract-class-dir-paths impl/contract-class-dir-paths)
(def contract-file-path impl/contract-file-path)
(def role-file-path impl/role-file-path)
(def capability-file-path impl/capability-file-path)
(def actor-file-path impl/actor-file-path)
(def read-edn-file! impl/read-edn-file!)
(def ensure-dir! impl/ensure-dir!)
(def write-edn-file! impl/write-edn-file!)
(def list-contract-ids! impl/list-contract-ids!)
(def list-contract-ids-sync impl/list-contract-ids-sync)
(def list-agent-contract-ids! impl/list-agent-contract-ids!)
(def load-contract! impl/load-contract!)
(def load-all-contracts! impl/load-all-contracts!)
