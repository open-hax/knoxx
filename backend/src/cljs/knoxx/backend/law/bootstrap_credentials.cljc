(ns knoxx.backend.law.bootstrap-credentials
  "Pure identity and query laws for the environment-owned bootstrap password.

   Local-password login chooses a default active membership without requiring
   an organization, so the credential retirement law is intentionally global.
   Mongo execution and transaction handling remain outside this namespace."
  (:require [clojure.string :as str]))

(def global-reconciliation-lock-id
  "One serialization lane for every primary organization."
  "bootstrap-system-admin-local-password")

(defn managed-account-identifiers
  "Canonical current and explicitly prior bootstrap account identifiers."
  [current-account previous-accounts]
  (->> (conj (vec previous-accounts) current-account)
       (map #(some-> % str str/trim str/lower-case))
       (remove str/blank?)
       distinct
       vec))

(defn managed-active-password-query
  "Global active-password query for durable markers and migration identities.

   No organization key belongs in this query: a former primary organization
   remains a valid org-scoped credential location while login is unscoped."
  [account-identifiers]
  (cond-> {:provider "local"
           :kind "password"
           :status "active"
           :$or [{:secret_json.bootstrap-system-admin true}]}
    (seq account-identifiers)
    (update :$or conj {:account_identifier {:$in (vec account-identifiers)}})))
