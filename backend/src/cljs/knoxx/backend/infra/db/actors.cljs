(ns knoxx.backend.infra.db.actors
  "Compatibility wrapper for the policy actor slice.

   New code should prefer knoxx.backend.domain.policy.protocol plus the EDN/SQL
   adapters. This namespace remains because policy_db.cljs is still a legacy
   facade consumed by runtime JS method names."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.policy.edn-adapter :as edn-adapter]
            [knoxx.backend.domain.policy.protocol :as policy]
            [knoxx.backend.domain.policy.sql-adapter :as sql-adapter]))

(defn normalize-actor-id
  [value]
  (some-> value str str/trim not-empty))

(defn normalize-email
  [value]
  (some-> value str str/trim str/lower-case not-empty))

(defn user-actor-id-from-email
  [email]
  (some-> email
          normalize-email
          (str/replace #"[^a-z0-9]+" "_")
          (str/replace #"^_+|_+$" "")
          not-empty))

(defn actor-email-from-id
  [actor-id]
  (let [slug (-> (str actor-id)
                 str/trim
                 str/lower-case
                 (str/replace #"[^a-z0-9._+-]+" "-")
                 (str/replace #"^[-.]+|[-.]+$" ""))]
    (when-not (str/blank? slug)
      (str slug "@actors.local"))))

(defn- edn-store
  [contracts-dir]
  (edn-adapter/create-store contracts-dir))

(defn actor-contract-file-path
  [contracts-dir actor-id]
  (edn-adapter/actor-contract-file-path (edn-store contracts-dir) actor-id))

(defn- actor-summary
  [actor]
  {:id (:actor/id actor)
   :kind (or (:actor/kind actor) :agent)
   :email (normalize-email (or (:actor/email actor)
                               (when (= :user (:actor/kind actor))
                                 (:actor/username actor))))
   :username (some-> (:actor/username actor) str str/trim not-empty)
   :org (some-> (:actor/org actor) str str/trim not-empty)
   :label (some-> (:actor/label actor) str str/trim not-empty)
   :role-slugs (->> (or (:actor/roles actor) [])
                    (map (fn [role]
                           (cond
                             (keyword? role) (-> role name (str/replace #"-" "_"))
                             (string? role) (-> role str/trim (str/replace #"-" "_"))
                             :else nil)))
                    (remove str/blank?)
                    distinct
                    vec)
   :actor actor})

(defn list-actor-contracts
  [contracts-dir]
  (->> (policy/list-actors (edn-store contracts-dir))
       (mapv actor-summary)))

(defn find-actor-contract-by-id
  [contracts-dir actor-id]
  (some-> (policy/get-actor (edn-store contracts-dir) actor-id)
          actor-summary))

(defn find-user-actor-contract-by-email
  [contracts-dir email]
  (when-let [normalized-email (normalize-email email)]
    (->> (list-actor-contracts contracts-dir)
         (filter #(and (= :user (:kind %))
                       (= normalized-email (:email %))))
         first)))

(defn upsert-actor-contract!
  [contracts-dir {:keys [actor-id email display-name org-slug role-slugs kind]}]
  (let [existing (some-> (find-actor-contract-by-id contracts-dir actor-id) :actor)
        normalized-email (normalize-email email)
        actor (cond-> (or existing {})
                true (assoc :actor/id (or (normalize-actor-id actor-id)
                                          (user-actor-id-from-email email)))
                (not (:actor/kind existing)) (assoc :actor/kind (or kind :agent))
                normalized-email (assoc :actor/email normalized-email
                                        :actor/username normalized-email)
                (some-> org-slug str str/trim not-empty) (assoc :actor/org (str/trim (str org-slug)))
                (some-> display-name str str/trim not-empty) (assoc :actor/label (str/trim (str display-name)))
                (seq role-slugs) (assoc :actor/roles (->> role-slugs
                                                          (keep (fn [role]
                                                                  (when-let [slug (some-> role str str/trim not-empty)]
                                                                    (keyword "role" (str/replace slug #"_" "-")))))
                                                          distinct
                                                          vec)))]
    (policy/upsert-actor! (edn-store contracts-dir) actor)))

(defn credential-select-query
  [actor-id provider]
  (sql-adapter/actor-credential-select-query actor-id provider))

(defn user-memberships-query
  [user-ids org-id]
  (sql-adapter/user-memberships-query user-ids org-id))

(defn format-sql
  [query-map]
  (sql-adapter/format-sql query-map))

(defn contract-tool-ids
  [contracts-dir]
  (edn-adapter/contract-tool-ids (edn-store contracts-dir)))
