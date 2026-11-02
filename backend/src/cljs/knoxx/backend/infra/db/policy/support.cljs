(ns knoxx.backend.infra.db.policy.support
  "Policy contract-file adapters and shared orchestration helpers."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.actor.scope :as actor-scope]
            [knoxx.backend.infra.db.actors :as policy-actors]
            ["node:path" :as path]
            ["node:fs" :as fs]))

(defn ^:async promise-each
  [items f]
  (doseq [item (or items [])]
    (await (f item)))
  nil)

(defn http-error [status message code]
  (doto (js/Error. message)
    (aset "statusCode" status)
    (aset "code" code)))

(defn- default-contracts-dir []
  (let [configured (some-> (aget js/process.env "CONTRACTS_DIR") str str/trim not-empty)
        cwd (.cwd js/process)]
    (or (some (fn [c] (when (.existsSync fs c) c))
              (map #(.resolve path cwd %)
                   (keep identity [configured "contracts" "../contracts"
                                   "packages/agents/knoxx/contracts"
                                   "orgs/open-hax/openplanner/packages/agents/knoxx/contracts"])))
        (.resolve path cwd (or configured "contracts")))))

(defn- contracts-dir [] (default-contracts-dir))

(defn contracts-config [] {:contracts-dir (default-contracts-dir)})

(defn- upsert-actor-contract! [payload]
  (policy-actors/upsert-actor-contract! (contracts-dir) payload))

(defn- read-only-contract-write-error?
  [err]
  (let [code (some-> (.-code err) str str/trim)
        message (some-> (.-message err) str)]
    (or (#{"EROFS" "EACCES" "EPERM"} code)
        (and message (boolean (re-find #"read-only file system|permission denied|operation not permitted" message))))))

(defn- handle-read-only-actor-contract-error!
  [err]
  (if (read-only-contract-write-error? err)
    (do
      (.warn js/console
             "[knoxx-policy] actor contract write skipped; contracts dir is read-only/unwritable"
             (.-message err))
      nil)
    (throw err)))

(defn ^:async upsert-actor-contract-best-effort!
  [payload]
  (try
    (await (upsert-actor-contract! payload))
    (catch :default err
      (handle-read-only-actor-contract-error! err))))

(defn find-actor-contract-by-id [actor-id]
  (policy-actors/find-actor-contract-by-id (contracts-dir) actor-id))

(defn find-user-actor-contract-by-email [email]
  (policy-actors/find-user-actor-contract-by-email (contracts-dir) email))

(defn list-actor-contracts []
  (policy-actors/list-actor-contracts (contracts-dir)))

(defn user-actor-id-from-email [email]
  (policy-actors/user-actor-id-from-email email))

(defn default-membership-actor-id [role-slugs]
  (actor-scope/default-membership-actor-id role-slugs))
