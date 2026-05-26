(ns knoxx.backend.infra.db.policy
  "Policy DB public API.

   All queries are HoneySQL maps built in shape.db.*; this namespace
   executes them via extern.pg and exposes a CLJS-typed public surface.

   create-policy-db returns a Promise<CLJS policy context | nil>. The context
   is plain CLJS data whose :pool value is an opaque pg Pool handle owned by
   extern.pg. Public functions take that pool/context and return CLJS maps."
  (:require [clojure.string :as str]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]
            [knoxx.backend.extern.pg :as pg]
            [knoxx.backend.shape.db.audit :as q-audit]
            [knoxx.backend.shape.db.invites :as q-invites]
            [knoxx.backend.shape.db.memberships :as q-memberships]
            [knoxx.backend.shape.db.orgs :as q-orgs]
            [knoxx.backend.shape.db.roles :as q-roles]
            [knoxx.backend.shape.db.sessions :as q-sessions]
            [knoxx.backend.shape.db.users :as q-users]
            [knoxx.backend.infra.db.policy.schema :as db-schema]
            [knoxx.backend.domain.contracts.loader :as contracts-loader]
            [knoxx.backend.domain.contracts.roles :as contracts-roles]
            [knoxx.backend.infra.db.actors :as policy-actors]
            [knoxx.backend.domain.policy.protocol :as policy]
            [knoxx.backend.domain.policy.sql-adapter :as sql-policy]
            [knoxx.backend.infra.registry.tools :as tool-registry]
            ["node:path" :as path]
            ["node:fs" :as fs]
            ["node:crypto" :as crypto]))

(declare set-membership-roles! sync-contract-role-projections! find-org-by-slug)

;; ---------------------------------------------------------------------------
;; SQL execution helpers
;; ---------------------------------------------------------------------------

(defn- honey->sql [honey-map]
  (sql/format honey-map {:numbered true}))

(defn- honey-query! [conn honey-map]
  (let [[sql-str & params] (honey->sql honey-map)]
    (pg/query! conn sql-str params)))

(defn- honey-query-one! [conn honey-map]
  (let [[sql-str & params] (honey->sql honey-map)]
    (pg/query-one! conn sql-str params)))

(defn- promise-each [items f]
  (reduce (fn [p item] (.then p (fn [_] (f item))))
          (js/Promise.resolve nil)
          (or items [])))

;; ---------------------------------------------------------------------------
;; Utility helpers
;; ---------------------------------------------------------------------------

(defn- slugify [value fallback]
  (let [s (-> (str value "")
              str/trim str/lower-case
              (str/replace #"[^a-z0-9]+" "-")
              (str/replace #"^[-]+|[-]+$" ""))]
    (if (str/blank? s) fallback s)))

(defn- normalize-email [v]
  (some-> v str str/trim str/lower-case not-empty))

(defn- normalize-actor-id [v]
  (some-> v str str/trim not-empty))

(defn- unique [vs]
  (vec (distinct (filter some? vs))))

(defn- http-error [status message code]
  (doto (js/Error. message)
    (aset "statusCode" status)
    (aset "code" code)))

(defn- env-positive-int [key default]
  (let [raw (aget js/process.env key)
        n (js/Number raw)]
    (if (and raw (not (js/Number.isNaN n)) (pos? n)) (js/Math.floor n) default)))

(defn- env-truthy? [key]
  (contains? #{"1" "true" "yes" "on" "y"}
             (-> (or (aget js/process.env key) "") str str/trim str/lower-case)))

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
(defn- contracts-config [] {:contracts-dir (default-contracts-dir)})

;; ---------------------------------------------------------------------------
;; Actor contract helpers (delegate to policy-actors)
;; ---------------------------------------------------------------------------

(defn- upsert-actor-contract! [payload]
  (policy-actors/upsert-actor-contract! (contracts-dir) payload))

(defn- find-actor-contract-by-id [actor-id]
  (policy-actors/find-actor-contract-by-id (contracts-dir) actor-id))

(defn- find-user-actor-contract-by-email [email]
  (policy-actors/find-user-actor-contract-by-email (contracts-dir) email))

(defn- list-actor-contracts []
  (policy-actors/list-actor-contracts (contracts-dir)))

(defn- contract-tool-ids []
  (policy-actors/contract-tool-ids (contracts-dir)))

(defn- user-actor-id-from-email [email]
  (policy-actors/user-actor-id-from-email email))

;; ---------------------------------------------------------------------------
;; Basic lookups
;; ---------------------------------------------------------------------------

(defn- find-org-by-id [pool org-id]
  (when-not (str/blank? (str org-id))
    (honey-query-one! pool (q-orgs/by-id org-id))))

(defn- find-org-by-slug [pool slug]
  (if (str/blank? (str slug))
    (js/Promise.resolve nil)
    (honey-query-one! pool (q-orgs/by-slug slug))))

(defn- find-role [pool {:keys [org-id slug]}]
  (honey-query-one! pool (q-roles/by-slug {:slug slug :org-id org-id})))

;; ---------------------------------------------------------------------------
;; Role management
;; ---------------------------------------------------------------------------

(defn- ensure-role! [pool {:keys [org-id name slug scope-kind built-in system-managed]}]
  (-> (find-role pool {:org-id org-id :slug slug})
      (.then
       (fn [existing]
         (if existing
           (honey-query-one! pool (q-roles/update-role (:id existing)
                                                       {:name name :scope-kind scope-kind
                                                        :built-in built-in :system-managed system-managed}))
           (honey-query-one! pool (q-roles/insert {:org-id org-id :name name :slug slug
                                                    :scope-kind scope-kind :built-in built-in
                                                    :system-managed system-managed})))))))

(defn- role-permissions-uses-legacy-ids? [pool]
  (-> (pg/query! pool
        "SELECT column_name FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = 'role_permissions'"
        nil)
      (.then (fn [{:keys [rows]}]
               (contains? (set (map :column_name rows)) "permission_id")))))

(defn- set-role-permissions! [pool role-id permission-codes]
  (pg/with-transaction!
   pool
   (fn [client]
     (let [codes (unique permission-codes)]
       (-> (honey-query! client (q-roles/delete-permissions role-id))
           (.then (fn [_] (role-permissions-uses-legacy-ids? client)))
           (.then
            (fn [legacy?]
              (if (empty? codes)
                nil
                (promise-each
                 codes
                 (fn [code]
                   (if legacy?
                     (pg/query! client
                       "INSERT INTO role_permissions (role_id, permission_id, effect)
                        SELECT $1, id, 'allow' FROM permissions WHERE code = $2
                        ON CONFLICT (role_id, permission_id) DO UPDATE SET effect = EXCLUDED.effect"
                       [role-id code])
                     (honey-query! client (q-roles/insert-permission-modern role-id code)))))))))))))

(defn- normalize-tool-policy [p]
  (if (string? p)
    {:tool-id p :effect "allow" :constraints {}}
    (let [tool-id (or (:tool-id p) (:tool_id p) (:id p))]
      (when-not tool-id (throw (js/Error. "toolId is required for tool policy")))
      {:tool-id (tool-registry/normalize-tool-id tool-id)
       :effect  (if (= (:effect p) "deny") "deny" "allow")
       :constraints (or (:constraints p) {})})))

(defn- ensure-tool-definitions! [conn tool-ids]
  (let [ids (->> tool-ids (keep tool-registry/normalize-tool-id) distinct vec)]
    (if (empty? ids)
      (js/Promise.resolve nil)
      (promise-each
       ids
       (fn [tid]
         (let [{:keys [label description risk-level]} (tool-registry/get-tool tid)]
           (pg/query! conn
             "INSERT INTO tool_definitions (id, label, description, risk_level)
              VALUES ($1, $2, $3, $4)
              ON CONFLICT (id) DO UPDATE
              SET label = EXCLUDED.label, description = EXCLUDED.description,
                  risk_level = EXCLUDED.risk_level"
             [tid (or label tid) (or description "") (or risk-level "low")])))))))

(defn set-role-tool-policies! [pool role-id tool-policies]
  (pg/with-transaction!
   pool
   (fn [client]
     (let [normalized (mapv normalize-tool-policy tool-policies)
           tool-ids   (mapv :tool-id normalized)]
       (-> (honey-query! client (q-roles/delete-tool-policies role-id))
           (.then (fn [_] (ensure-tool-definitions! client tool-ids)))
           (.then
            (fn [_]
              (promise-each
               normalized
               (fn [p]
                 (honey-query! client
                   (q-roles/insert-tool-policy
                    role-id
                    (assoc p :constraints-json
                           (js/JSON.stringify (clj->js (:constraints p))))))))))
           (.then (fn [_] nil)))))))

;; ---------------------------------------------------------------------------
;; Membership roles
;; ---------------------------------------------------------------------------

(defn- resolve-role-ids [pool {:keys [org-id role-ids role-slugs]}]
  (let [base-ids (set (map str (or role-ids [])))]
    (if (empty? role-slugs)
      (js/Promise.resolve (vec base-ids))
      (-> (honey-query! pool (q-roles/by-slugs-and-org (vec role-slugs) org-id))
          (.then
           (fn [{:keys [rows]}]
             (let [found   (into {} (map (fn [r] [(:slug r) (str (:id r))]) rows))
                   missing (filter #(not (contains? found (str/trim (str %)))) role-slugs)]
               (when (seq missing)
                 (throw (js/Error. (str "Role not found for slug(s): " (str/join ", " missing)))))
               (into (vec base-ids) (vals found)))))))))

(defn- canonicalize-contract-role-slugs! [role-slugs]
  (-> (contracts-loader/load-all-contracts! (contracts-config))
      (.then
       (fn [records]
         (let [known (->> records
                          (filter #(= "roles" (:contractClass %)))
                          (keep #(some-> (or (:id %) (get-in % [:contract :role/id]))
                                         str str/trim not-empty))
                          set)]
           (->> (or role-slugs [])
                (keep (fn [raw]
                        (let [s (some-> raw str str/trim not-empty)]
                          (cond
                            (and s (contains? known s)) s
                            (and s (contains? known (slugify s s))) (slugify s s)
                            s (do (.warn js/console "[policy-db] unknown role slug, skipping:" s) nil)
                            :else nil))))
                distinct vec))))))

(defn set-membership-roles!
  [pool membership-id {:keys [org-id role-ids role-slugs replace contract-projection]}]
  (let [slugs-promise (if contract-projection
                        (canonicalize-contract-role-slugs! role-slugs)
                        (js/Promise.resolve (or role-slugs [])))]
    (-> slugs-promise
        (.then (fn [resolved-slugs]
                 (resolve-role-ids pool {:org-id org-id
                                         :role-ids (or role-ids [])
                                         :role-slugs resolved-slugs})))
        (.then
         (fn [resolved-ids]
           (pg/with-transaction!
            pool
            (fn [client]
              (-> (if replace
                    (honey-query! client (q-memberships/delete-roles membership-id))
                    (js/Promise.resolve nil))
                  (.then (fn [_]
                           (promise-each
                            resolved-ids
                            (fn [role-id]
                              (honey-query! client
                                (q-memberships/insert-role membership-id role-id))))))
                  (.then (fn [_] resolved-ids))))))))))

(defn set-membership-tool-policies! [pool membership-id tool-policies]
  (pg/with-transaction!
   pool
   (fn [client]
     (let [normalized (mapv normalize-tool-policy tool-policies)
           tool-ids   (mapv :tool-id normalized)]
       (-> (honey-query! client (q-memberships/delete-tool-policies membership-id))
           (.then (fn [_] (ensure-tool-definitions! client tool-ids)))
           (.then
            (fn [_]
              (promise-each
               normalized
               (fn [p]
                 (honey-query! client
                   (q-memberships/insert-tool-policy
                    membership-id
                    (assoc p :constraints-json
                           (js/JSON.stringify (clj->js (:constraints p))))))))))
           (.then (fn [_] nil)))))))

(defn- set-membership-actor-id! [pool membership-id actor-id]
  (let [resolved (or (normalize-actor-id actor-id) "workspace_user")]
    (-> (honey-query! pool (q-memberships/set-actor-id membership-id resolved))
        (.then (fn [_] resolved)))))

;; ---------------------------------------------------------------------------
;; Hydration
;; ---------------------------------------------------------------------------

(defn- default-membership-actor-id [role-slugs]
  (let [normalized-slugs (set (map #(str/replace (str %) #"-" "_") role-slugs))]
    (if (contains? normalized-slugs "system_admin")
      "system_admin"
      "workspace_user")))

(defn hydrate-role-maps [pool roles]
  (if (empty? roles)
    (js/Promise.resolve [])
    (let [role-ids (mapv :id roles)]
      (-> (js/Promise.all
           [(-> (role-permissions-uses-legacy-ids? pool)
                (.then (fn [legacy?]
                         (if legacy?
                           (honey-query! pool (q-roles/permissions-for-roles-legacy role-ids))
                           (honey-query! pool (q-roles/permissions-for-roles role-ids))))))
            (honey-query! pool (q-roles/tool-policies-for-roles role-ids))])
          (.then
           (fn [[perm-result tool-result]]
             (let [perm-rows  (:rows perm-result)
                   tool-rows  (:rows tool-result)
                   perm-map   (atom {})
                   tool-map   (atom {})]
               (doseq [{:keys [role_id code]} perm-rows]
                 (swap! perm-map update role_id (fnil conj []) code))
               (doseq [{:keys [role_id tool_id effect constraints_json]} tool-rows]
                 (swap! tool-map update role_id (fnil conj [])
                        {:tool-id     tool_id
                         :effect      effect
                         :constraints (js->clj (or constraints_json (js-obj)) :keywordize-keys true)}))
               (vec
                (for [{:keys [id org_id name slug scope_kind built_in system_managed
                               created_at updated_at]} roles]
                  {:id             id
                   :org-id         org_id
                   :name           name
                   :slug           slug
                   :scope-kind     scope_kind
                   :built-in       built_in
                   :system-managed system_managed
                   :created-at     created_at
                   :updated-at     updated_at
                   :permissions    (or (get @perm-map id) [])
                   :tool-policies  (or (get @tool-map id) [])})))))))))

(defn hydrate-memberships [pool memberships]
  (if (empty? memberships)
    (js/Promise.resolve [])
    (let [membership-ids (mapv :id memberships)]
      (-> (js/Promise.all
           [(honey-query! pool (q-roles/roles-for-memberships membership-ids))
            (honey-query! pool (q-memberships/tool-policies-for-ids membership-ids))])
          (.then
           (fn [[role-result tool-result]]
             (let [role-rows  (:rows role-result)
                   tool-rows  (:rows tool-result)
                   roles-by-m (atom {})
                   tools-by-m (atom {})]
               (doseq [{:keys [membership_id role_id slug name scope_kind org_id]} role-rows]
                 (swap! roles-by-m update membership_id (fnil conj [])
                        {:id role_id :slug slug :name name
                         :scope-kind scope_kind :org-id org_id}))
               (doseq [{:keys [membership_id tool_id effect constraints_json]} tool-rows]
                 (swap! tools-by-m update membership_id (fnil conj [])
                        {:tool-id     tool_id
                         :effect      effect
                         :constraints (js->clj (or constraints_json (js-obj)) :keywordize-keys true)}))
               (vec
                (for [{:keys [id user_id org_id actor_id org_name org_slug
                               status is_default created_at updated_at]} memberships]
                  {:id           id
                   :user-id      user_id
                   :org-id       org_id
                   :actor-id     (or (normalize-actor-id actor_id)
                                     (default-membership-actor-id
                                      (map :slug (or (get @roles-by-m id) []))))
                   :org-name     org_name
                   :org-slug     org_slug
                   :status       status
                   :is-default   is_default
                   :created-at   created_at
                   :updated-at   updated_at
                   :roles        (or (get @roles-by-m id) [])
                   :tool-policies (or (get @tools-by-m id) [])})))))))))

;; ---------------------------------------------------------------------------
;; Request context
;; ---------------------------------------------------------------------------

(defn- header-value [headers-like name]
  (when headers-like
    (if (fn? (aget headers-like "get"))
      (str/trim (or (.get headers-like name)
                    (.get headers-like (str/lower-case name)) ""))
      (str/trim (str (or (aget headers-like name)
                         (aget headers-like (str/lower-case name)) ""))))))

(defn- find-request-membership-row [pool headers-like]
  (let [membership-id (header-value headers-like "x-knoxx-membership-id")
        user-email    (some-> (header-value headers-like "x-knoxx-user-email") str/lower-case)
        org-id        (header-value headers-like "x-knoxx-org-id")
        org-slug      (some-> (header-value headers-like "x-knoxx-org-slug") str/lower-case)]
    (cond
      (and (str/blank? membership-id) (str/blank? user-email))
      (js/Promise.reject
       (http-error 401 "Missing x-knoxx-user-email or x-knoxx-membership-id"
                   "request_context_missing"))

      (not (str/blank? membership-id))
      (honey-query-one! pool (q-memberships/by-id membership-id))

      :else
      (honey-query-one! pool (q-memberships/by-email-and-org
                               {:user-email user-email
                                :org-id     org-id
                                :org-slug   org-slug})))))

(defn- rolePriority [slug]
  (case slug "system_admin" 100 "system-admin" 100 "org_admin" 90 "org-admin" 90
    "developer" 80 "data_analyst" 70 "data-analyst" 70 "knowledge_worker" 60 "knowledge-worker" 60 0))

(defn- merge-tool-policies [role-policies membership-policies]
  (let [merged (atom {})]
    (doseq [p role-policies]
      (let [n (normalize-tool-policy p) tid (:tool-id n)]
        (when (or (nil? (get @merged tid))
                  (= (:effect n) "deny")
                  (not= (:effect (get @merged tid)) "deny"))
          (swap! merged assoc tid n))))
    (doseq [p membership-policies]
      (let [n (normalize-tool-policy p)]
        (swap! merged assoc (:tool-id n) n)))
    (->> (vals @merged) (sort-by :tool-id) vec)))

(defn- build-request-context [pool membership-row]
  (cond
    (not membership-row)
    (js/Promise.reject
     (http-error 401 "Request context did not resolve to a membership"
                 "request_context_unresolved"))

    (not= (:user_status membership-row) "active")
    (js/Promise.reject (http-error 403 "User is not active" "user_inactive"))

    (not= (:status membership-row) "active")
    (js/Promise.reject (http-error 403 "Membership is not active" "membership_inactive"))

    (not= (:org_status membership-row) "active")
    (js/Promise.reject (http-error 403 "Org is not active" "org_inactive"))

    :else
    (-> (hydrate-memberships pool [membership-row])
        (.then
         (fn [memberships]
           (let [membership (first memberships)
                 role-ids   (mapv :id (:roles membership))]
             (-> (if (empty? role-ids)
                   (js/Promise.resolve [])
                   (-> (honey-query! pool (q-roles/by-ids role-ids))
                       (.then (fn [{:keys [rows]}] (hydrate-role-maps pool rows)))))
                 (.then
                  (fn [detailed-roles]
                    (let [permissions     (sort (unique (mapcat :permissions detailed-roles)))
                          tool-policies   (merge-tool-policies
                                           (mapcat :tool-policies detailed-roles)
                                           (:tool-policies membership))
                          role-slugs      (sort-by #(- (rolePriority %))
                                                   (map :slug detailed-roles))
                          actor-id        (or (normalize-actor-id (:actor_id membership-row))
                                              (default-membership-actor-id role-slugs))]
                      {:user          {:id           (:user_id membership-row)
                                       :email        (:email membership-row)
                                       :username     (:email membership-row)
                                       :display-name (:display_name membership-row)
                                       :status       (:user_status membership-row)}
                       :org           {:id         (:org_id membership-row)
                                       :slug       (:org_slug membership-row)
                                       :name       (:org_name membership-row)
                                       :status     (:org_status membership-row)
                                       :is-primary (:is_primary membership-row)
                                       :kind       (:org_kind membership-row)}
                       :membership    {:id         (:id membership)
                                       :actor-id   actor-id
                                       :status     (:status membership)
                                       :is-default (:is-default membership)
                                       :created-at (:created-at membership)
                                       :updated-at (:updated-at membership)}
                       :actor         {:id actor-id}
                       :roles         detailed-roles
                       :role-slugs    role-slugs
                       :permissions   permissions
                       :tool-policies tool-policies
                       :membership-tool-policies (:tool-policies membership)
                       :is-system-admin (boolean (some #{"system_admin" "system-admin"} role-slugs))}))))))))))

;; ---------------------------------------------------------------------------
;; Bootstrap & contract sync
;; ---------------------------------------------------------------------------

(defn- sql-policy-store [pool primary-org]
  (sql-policy/create-store
   {:query-one!          (fn [s p] (pg/query-one! pool s p))
    :query!              (fn [s p] (pg/query! pool s p))
    :find-org-by-slug!   (fn [slug] (find-org-by-slug pool slug))
    :set-membership-roles! (fn [mid opts] (set-membership-roles! pool mid opts))
    :primary-org         primary-org}))

(defn- sync-user-from-actor-contract!* [pool primary-org payload]
  (let [actor-id (normalize-actor-id (or (:actor-id payload) (:actor_id payload)))
        email    (normalize-email (:email payload))]
    (if-not (or email actor-id)
      (js/Promise.resolve nil)
      (if-let [contract (or (find-actor-contract-by-id actor-id)
                            (find-user-actor-contract-by-email email))]
        (policy/sync-actor-projections!
         (sql-policy-store pool primary-org)
         [(:actor contract)])
        (js/Promise.resolve nil)))))

(defn sync-contract-role-projections! [pool]
  (-> (contracts-loader/load-all-contracts! (contracts-config))
      (.then
       (fn [records]
         (let [caps-by-id  (->> records
                                (filter #(= "capabilities" (:contractClass %)))
                                (map (fn [r] [(:id r) (:contract r)]))
                                (into {}))
               role-records (->> records
                                 (filter #(= "roles" (:contractClass %)))
                                 vec)]
           (promise-each
            role-records
            (fn [rec]
              (let [slug     (some-> (or (:id rec)
                                         (get-in rec [:contract :role/id]))
                                     str str/trim not-empty)
                    contract (:contract rec)]
                (when slug
                  (let [name  (or (some-> (:role/label contract) str str/trim not-empty)
                                  (some-> (:role/name contract) str str/trim not-empty)
                                  (->> (str/split slug #"[-_]+")
                                       (remove str/blank?)
                                       (map str/capitalize)
                                       (str/join " ")))
                        perms (->> (or (:role/permissions contract) [])
                                   (map str) distinct sort vec)
                        tool-policies
                        (->> (or (:role/capabilities contract) [])
                             (keep #(some-> % str str/trim not-empty))
                             (keep caps-by-id)
                             (mapcat #(or (:cap/tools %) []))
                             (keep tool-registry/normalize-tool-id)
                             distinct sort
                             (mapv (fn [tid] {:tool-id tid :effect "allow"})))]
                    (-> (ensure-role! pool {:org-id nil :name name :slug slug
                                            :scope-kind "platform" :built-in false
                                            :system-managed true})
                        (.then (fn [role]
                                 (-> (set-role-permissions! pool (:id role) perms)
                                     (.then (fn [_]
                                              (set-role-tool-policies! pool (:id role)
                                                                       tool-policies)))))))))))))))
       (.then (fn [_] nil))))

(defn- ensure-primary-org! [pool opts]
  (let [primary-org-slug (or (:primaryOrgSlug opts) (:primary-org-slug opts) "open-hax")
        primary-org-name (or (:primaryOrgName opts) (:primary-org-name opts) "Open Hax")
        primary-org-kind (or (:primaryOrgKind opts) (:primary-org-kind opts) "platform_owner")]
  (let [slug (slugify primary-org-slug "open-hax")
        name (str primary-org-name)
        kind (str primary-org-kind)]
    (-> (honey-query-one! pool (q-orgs/upsert-primary {:slug slug :name name :kind kind}))
        (.then (fn [org]
                 (.then (honey-query! pool (q-orgs/clear-primary-except slug))
                        (fn [_] org))))))))

(defn- ensure-bootstrap-user! [pool primary-org opts]
  (let [email (str/lower-case
               (str (or (:bootstrapSystemAdminEmail opts)
                        (:bootstrap-system-admin-email opts)
                        "system-admin@open-hax.local")))
        dn    (str (or (:bootstrapSystemAdminName opts)
                       (:bootstrap-system-admin-name opts)
                       "Knoxx System Admin"))]
    (-> (honey-query-one! pool (q-users/upsert {:email email :display-name dn
                                                 :auth-provider "bootstrap"
                                                 :external-subject nil :status "active"}))
        (.then
         (fn [user]
           (-> (honey-query-one! pool (q-memberships/upsert
                                       {:user-id (:id user)
                                        :org-id  (:id primary-org)
                                        :status  "active" :is-default true}))
               (.then
                (fn [membership]
                  (-> (find-role pool {:slug "system_admin" :org-id nil})
                      (.then (fn [role]
                               (if role
                                 (honey-query! pool
                                   (q-roles/insert-membership-role
                                    (:id membership) (:id role)))
                                 (js/Promise.resolve nil))))
                      (.then (fn [_]
                               (set-membership-actor-id! pool (:id membership) "system_admin")))
                      (.then (fn [_]
                               {:user user :membership membership})))))))))))

;; ---------------------------------------------------------------------------
;; Audit
;; ---------------------------------------------------------------------------

(defn- append-audit! [pool {:keys [before after] :as opts}]
  (honey-query! pool
    (q-audit/insert-event
     (assoc opts
            :before-json (when before (js/JSON.stringify (clj->js before)))
            :after-json  (when after  (js/JSON.stringify (clj->js after)))))))

;; ---------------------------------------------------------------------------
;; Session persistence
;; ---------------------------------------------------------------------------

(defn- hash-token [token salt]
  (let [h (.createHash crypto "sha256")]
    (.update h (str salt ":" token) "utf8")
    (.digest h "hex")))

(defn- token-prefix [token]
  (let [h (.createHash crypto "sha256")]
    (.update h (str token) "utf8")
    (subs (.digest h "hex") 0 12)))

(defn- generate-salt []
  (.toString (.randomBytes crypto 16) "hex"))

(defn- find-session-in-rows [pool token rows]
  (loop [[row & rest] rows]
    (when row
      (if (= (:token_hash row) (hash-token token (:salt row)))
        (do
          (.catch (honey-query! pool (q-sessions/touch (:id row))) (fn [_] nil))
          {:session {:id            (:id row)
                     :user-id       (:user_id row)
                     :membership-id (:membership_id row)
                     :org-id        (:org_id row)
                     :email         (:email row)
                     :display-name  (:display_name row)
                     :auth-provider (:auth_provider row)
                     :expires-at    (:expires_at row)
                     :created-at    (:created_at row)}})
        (recur rest)))))

;; ---------------------------------------------------------------------------
;; Public API
;; ---------------------------------------------------------------------------

(defn resolve-request-context!
  "Resolve a Knoxx auth context from headers-like (Fastify headers or CLJS map).
   Returns Promise<CLJS ctx map>."
  [pool headers-like]
  (-> (find-request-membership-row pool headers-like)
      (.then (fn [row] (build-request-context pool row)))))

(defn evaluate-tool-access!
  [pool headers-like tool-id]
  (-> (resolve-request-context! pool headers-like)
      (.then (fn [ctx]
               (let [policies (:tool-policies ctx)
                     match    (some #(when (= (:tool-id %) tool-id) %) policies)]
                 {:context ctx
                  :tool-id tool-id
                  :allowed (boolean (and match (= (:effect match) "allow")))})))))

(defn list-actor-credentials!
  "Return active actor credential rows for provider as {:credentials [...]}."
  [pool provider]
  (-> (policy/list-actor-credentials (sql-policy-store pool nil) provider)
      (.then (fn [creds] {:credentials creds}))))

(defn list-permissions!
  [_pool]
  (let [codes (->> (contracts-roles/list-role-slugs (contracts-config))
                   (mapcat #(contracts-roles/role-permissions (contracts-config) %))
                   distinct sort vec)]
    (js/Promise.resolve
     {:permissions (mapv (fn [c]
                           {:id           c
                            :code         c
                            :resourceKind (first (str/split c #"\."))
                            :description  ""})
                         codes)})))

(defn list-tools!
  [pool]
  (-> (pg/query! pool
        "SELECT id, label, description, risk_level FROM tool_definitions ORDER BY id ASC"
        nil)
      (.then (fn [{:keys [rows]}]
               {:tools (mapv (fn [{:keys [id label description risk_level]}]
                               {:id id :label label :description description
                                :risk-level risk_level})
                             rows)}))))

(defn get-bootstrap-context!
  [pool primary-org bootstrap]
  (js/Promise.resolve
   {"primaryOrg"    {"id"        (:id primary-org)
                     "slug"      (:slug primary-org)
                     "name"      (:name primary-org)
                     "kind"      (:kind primary-org)
                     "isPrimary" (:is_primary primary-org)
                     "status"    (:status primary-org)}
    "bootstrapUser" {"id"           (get-in bootstrap [:user :id])
                     "email"        (get-in bootstrap [:user :email])
                     "displayName"  (get-in bootstrap [:user :display_name])
                     "membershipId" (get-in bootstrap [:membership :id])}}))

(defn list-orgs!
  [pool]
  (-> (honey-query! pool (q-orgs/list-with-counts))
      (.then (fn [{:keys [rows]}]
               {:orgs (mapv (fn [{:keys [id slug name kind is_primary status
                                          member_count role_count data_lake_count
                                          created_at updated_at]}]
                              {:id              id :slug slug :name name :kind kind
                               :is-primary      is_primary :status status
                               :member-count    (js/Number (or member_count 0))
                               :role-count      (js/Number (or role_count 0))
                               :data-lake-count (js/Number (or data_lake_count 0))
                               :created-at      created_at :updated-at updated_at})
                            rows)}))))

(defn create-org!
  [pool uid mid {:keys [name slug kind status]
                  :or {kind "customer" status "active"}}]
  (if (str/blank? name)
    (js/Promise.reject (js/Error. "name is required"))
    (let [s (slugify (or slug name) "org")]
      (-> (honey-query-one! pool (q-orgs/insert {:slug s :name name :kind kind :status status}))
          (.then (fn [org]
                   (-> (sync-contract-role-projections! pool)
                       (.then (fn [_]
                                (append-audit! pool {:actor-user-id uid
                                                     :actor-membership-id mid
                                                     :org-id (:id org)
                                                     :action "org.create"
                                                     :resource-kind "org"
                                                     :resource-id (:id org)})))
                       (.then (fn [_]
                                {:org {:id (:id org) :slug (:slug org) :name (:name org)
                                       :kind (:kind org) :is-primary (:is_primary org)
                                       :status (:status org)}})))))))))

(defn list-roles!
  [pool {:keys [org-id]}]
  (-> (if org-id
        (honey-query! pool (q-roles/list-by-org org-id))
        (honey-query! pool (q-roles/list-all)))
      (.then (fn [{:keys [rows]}] (hydrate-role-maps pool rows)))
      (.then (fn [roles] {:roles roles}))))

(defn get-role!
  [pool role-id]
  (-> (honey-query-one! pool (q-roles/by-id role-id))
      (.then (fn [row]
               (if row
                 (-> (hydrate-role-maps pool [row])
                     (.then (fn [h] {:role (first h)})))
                 {:role nil})))))

(defn create-role!
  [pool uid mid {:keys [org-id name slug permission-codes tool-policies]}]
  (cond
    (str/blank? org-id) (js/Promise.reject (js/Error. "org-id is required"))
    (str/blank? name)   (js/Promise.reject (js/Error. "name is required"))
    :else
    (let [s (slugify (or slug name) "role")]
      (-> (ensure-role! pool {:org-id org-id :name name :slug s
                               :scope-kind "org" :built-in false :system-managed false})
          (.then
           (fn [role]
             (-> (set-role-permissions! pool (:id role) (or permission-codes []))
                 (.then (fn [_]
                          (set-role-tool-policies! pool (:id role) (or tool-policies []))))
                 (.then (fn [_]
                          (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                               :org-id org-id :action "role.create"
                                               :resource-kind "role" :resource-id (:id role)})))
                 (.then (fn [_] (hydrate-role-maps pool [role])))
                 (.then (fn [h] {:role (first h)})))))))))

(defn list-users!
  [pool {:keys [org-id]}]
  (-> (if org-id
        (honey-query! pool (q-users/list-by-org org-id))
        (honey-query! pool (q-users/list-all)))
      (.then
       (fn [{:keys [rows]}]
         (let [users    rows
               user-ids (mapv :id users)]
           (-> (if org-id
                 (honey-query! pool (q-users/memberships-for-users user-ids org-id))
                 (honey-query! pool (q-users/all-memberships-for-users user-ids)))
               (.then (fn [{mem-rows :rows}]
                        (hydrate-memberships pool mem-rows)))
               (.then
                (fn [memberships]
                  (let [by-user (atom {})]
                    (doseq [m memberships]
                      (swap! by-user update (:user-id m) (fnil conj []) m))
                    {:users (mapv (fn [{:keys [id email display_name auth_provider
                                               external_subject status created_at updated_at]}]
                                    {:id               id
                                     :email            email
                                     :display-name     display_name
                                     :auth-provider    auth_provider
                                     :external-subject external_subject
                                     :status           status
                                     :created-at       created_at
                                     :updated-at       updated_at
                                     :memberships      (or (get @by-user id) [])})
                                  users)})))))))))

(defn create-user!
  [pool uid mid {:keys [email display-name auth-provider external-subject status
                         membership-status org-id role-slugs role-ids is-default actor-id]
                  :or {auth-provider "local" status "active"
                       membership-status "active" is-default true}}]
  (cond
    (str/blank? email)  (js/Promise.reject (js/Error. "email is required"))
    (str/blank? org-id) (js/Promise.reject (js/Error. "org-id is required"))
    :else
    (let [dn             (or display-name email)
          resolved-slugs (or role-slugs ["knowledge_worker"])
          actor-contract (find-user-actor-contract-by-email email)
          resolved-actor (or (normalize-actor-id actor-id)
                             (:id actor-contract)
                             (user-actor-id-from-email email)
                             (default-membership-actor-id resolved-slugs))]
      (-> (honey-query-one! pool
            (q-users/upsert {:email email :display-name dn
                              :auth-provider auth-provider
                              :external-subject external-subject
                              :status status}))
          (.then
           (fn [user]
             (-> (honey-query-one! pool
                   (q-memberships/upsert {:user-id  (:id user) :org-id org-id
                                          :status   membership-status :is-default is-default}))
                 (.then (fn [ms]
                          (set-membership-roles! pool (:id ms)
                                                 {:org-id    org-id
                                                  :role-ids  (or role-ids [])
                                                  :role-slugs resolved-slugs
                                                  :replace    true})))
                 (.then (fn [_]
                          (pg/query-one! pool
                            "SELECT id FROM memberships WHERE user_id = $1::uuid AND org_id = $2::uuid"
                            [(:id user) org-id])))
                 (.then (fn [ms-row]
                          (-> (set-membership-actor-id! pool (:id ms-row) resolved-actor)
                              (.then (fn [_] (find-org-by-id pool org-id)))
                              (.then (fn [org-row]
                                       (upsert-actor-contract!
                                        {:actor-id    resolved-actor
                                         :email        email
                                         :display-name dn
                                         :org-slug     (:slug org-row)
                                         :role-slugs   resolved-slugs
                                         :kind         :agent}))))))
                 (.then (fn [_]
                          (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                               :org-id org-id :action "user.create_or_update"
                                               :resource-kind "user" :resource-id (:id user)})))
                 (.then (fn [_] {:user nil})))))))))

(defn list-memberships!
  [pool {:keys [org-id]}]
  (if (str/blank? org-id)
    (js/Promise.reject (js/Error. "org-id is required"))
    (-> (honey-query! pool (q-memberships/list-by-org org-id))
        (.then (fn [{:keys [rows]}] (hydrate-memberships pool rows)))
        (.then (fn [ms] {:memberships ms})))))

(defn get-membership!
  [pool membership-id]
  (-> (honey-query-one! pool (q-memberships/by-id membership-id))
      (.then (fn [row]
               (if row
                 (-> (hydrate-memberships pool [row])
                     (.then (fn [h] {:membership (first h)})))
                 {:membership nil})))))

(defn set-membership-roles-public!
  [pool uid mid membership-id {:keys [org-id role-ids role-slugs actor-id replace]
                                 :or {replace true}}]
  (-> (honey-query-one! pool (q-memberships/bare-by-id membership-id))
      (.then
       (fn [ms]
         (if-not ms
           (js/Promise.reject (js/Error. "membership not found"))
           (let [resolved-actor (or (normalize-actor-id actor-id)
                                    (normalize-actor-id (:actor_id ms))
                                    (default-membership-actor-id (or role-slugs [])))]
             (-> (set-membership-roles! pool membership-id
                                        {:org-id    (or org-id (:org_id ms))
                                         :role-ids  (or role-ids [])
                                         :role-slugs (or role-slugs [])
                                         :replace    replace})
                 (.then (fn [_] (set-membership-actor-id! pool membership-id resolved-actor)))
                 (.then (fn [_] (honey-query-one! pool (q-memberships/with-user-and-org membership-id))))
                 (.then (fn [row]
                          (upsert-actor-contract!
                           {:actor-id    resolved-actor
                            :email        (:email row)
                            :display-name (:display_name row)
                            :org-slug     (:org_slug row)
                            :role-slugs   (or role-slugs [])})))
                 (.then (fn [_]
                          (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                               :org-id (:org_id ms) :action "membership.roles.update"
                                               :resource-kind "membership"
                                               :resource-id membership-id})))
                 (.then (fn [_] {:membership nil})))))))))

(defn list-data-lakes!
  [pool {:keys [org-id]}]
  (if (str/blank? org-id)
    (js/Promise.reject (js/Error. "org-id is required"))
    (-> (honey-query! pool (q-orgs/data-lake-by-org org-id))
        (.then (fn [{:keys [rows]}]
                 {:data-lakes (mapv (fn [{:keys [id org_id name slug kind
                                                  config_json status created_at updated_at]}]
                                      {:id         id :org-id org_id :name name :slug slug
                                       :kind       kind
                                       :config     (js->clj (or config_json (js-obj)) :keywordize-keys true)
                                       :status     status :created-at created_at :updated-at updated_at})
                                    rows)})))))

(defn create-data-lake!
  [pool uid mid {:keys [org-id name slug kind config status]
                  :or {kind "workspace_docs" status "active"}}]
  (cond
    (str/blank? org-id) (js/Promise.reject (js/Error. "org-id is required"))
    (str/blank? name)   (js/Promise.reject (js/Error. "name is required"))
    :else
    (let [s          (slugify (or slug name) "lake")
          config-json (js/JSON.stringify (clj->js (or config {})))]
      (-> (honey-query-one! pool
            (q-orgs/insert-data-lake {:org-id org-id :name name :slug s
                                       :kind kind :config-json config-json :status status}))
          (.then (fn [lake]
                   (-> (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                             :org-id org-id :action "data_lake.create"
                                             :resource-kind "data_lake" :resource-id (:id lake)})
                       (.then (fn [_] {:data-lake
                                       {:id (:id lake) :org-id (:org_id lake) :name (:name lake)
                                        :slug (:slug lake) :kind (:kind lake)
                                        :status (:status lake)}})))))))))

;; ---------------------------------------------------------------------------
;; Sessions
;; ---------------------------------------------------------------------------

(defn create-session!
  [pool {:keys [token user-id membership-id org-id email display-name
                 auth-provider external-subject ip-address user-agent]}]
  (if (str/blank? token)
    (js/Promise.reject (js/Error. "token is required"))
    (let [ttl        (js/parseInt (or (aget js/process.env "KNOXX_SESSION_TTL_SECONDS") "86400") 10)
          salt       (generate-salt)
          token-hash (hash-token token salt)
          prefix     (token-prefix token)
          expires-at (js/Date. (+ (js/Date.now) (* ttl 1000)))]
      (-> (honey-query-one! pool
            (q-sessions/insert {:user-id       user-id
                                  :membership-id membership-id
                                  :org-id        org-id
                                  :token-hash    token-hash
                                  :token-prefix  prefix
                                  :salt          salt
                                  :email         email
                                  :display-name  display-name
                                  :auth-provider (or auth-provider "github")
                                  :external-subject external-subject
                                  :ip-address    ip-address
                                  :user-agent    user-agent
                                  :expires-at    (.toISOString expires-at)}))
          (.then
           (fn [row]
             {:session {:id            (:id row)
                         :user-id       (:user_id row)
                         :membership-id (:membership_id row)
                         :org-id        (:org_id row)
                         :email         (:email row)
                         :display-name  (:display_name row)
                         :auth-provider (:auth_provider row)
                         :expires-at    (:expires_at row)
                         :created-at    (:created_at row)}}))))))

(defn get-session-by-token!
  [pool token]
  (if (str/blank? token)
    (js/Promise.resolve nil)
    (let [prefix (token-prefix token)]
      (-> (honey-query! pool (q-sessions/by-prefix prefix))
          (.then
           (fn [{:keys [rows]}]
             (or (find-session-in-rows pool token rows)
                 (-> (honey-query! pool (q-sessions/all-active))
                     (.then (fn [{:keys [rows]}]
                              (find-session-in-rows pool token rows)))))))
          (.catch (fn [_] nil))))))

(defn delete-session-by-token!
  [pool token]
  (-> (get-session-by-token! pool token)
      (.then (fn [result]
               (when-let [sid (get-in result [:session :id])]
                 (.catch (honey-query! pool (q-sessions/delete-by-id sid)) (fn [_] nil)))
               result))))

(defn cleanup-expired-sessions!
  [pool]
  (-> (honey-query! pool (q-sessions/delete-expired))
      (.then (fn [{:keys [row-count]}]
               (when (> (or row-count 0) 0)
                 (.log js/console "[policy-db] Cleaned up" row-count "expired sessions"))
               (or row-count 0)))
      (.catch (fn [_] 0))))

;; ---------------------------------------------------------------------------
;; Invites
;; ---------------------------------------------------------------------------

(defn create-invite!
  [pool uid mid {:keys [org-id email role-slugs inviter-membership-id]}]
  (cond
    (str/blank? org-id) (js/Promise.reject (js/Error. "org-id is required"))
    (str/blank? email)  (js/Promise.reject (js/Error. "email is required"))
    :else
    (let [slugs      (or role-slugs ["knowledge_worker"])
          code       (.toString (.randomBytes crypto 8) "hex")
          expires-at (js/Date. (+ (js/Date.now) (* 7 24 3600 1000)))]
      (-> (honey-query-one! pool
            (q-invites/insert {:org-id               org-id
                                :code                 code
                                :email                email
                                :inviter-membership-id (or inviter-membership-id mid)
                                :role-slugs-json      (js/JSON.stringify (clj->js slugs))
                                :expires-at           (.toISOString expires-at)}))
          (.then (fn [row]
                   (-> (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                             :org-id org-id :action "invite.create"
                                             :resource-kind "invite" :resource-id (:id row)})
                       (.then (fn [_]
                                {:invite {:id         (:id row)
                                          :org-id     (:org_id row)
                                          :code       code
                                          :email      email
                                          :status     (:status row)
                                          :expires-at (:expires_at row)
                                          :created-at (:created_at row)}})))))))))

(defn redeem-invite!
  [pool code email]
  (if (or (str/blank? code) (str/blank? email))
    (js/Promise.reject (js/Error. "code and email are required"))
    (-> (honey-query-one! pool (q-invites/pending-by-code code))
        (.then
         (fn [invite]
           (if-not invite
             (js/Promise.reject (doto (js/Error. "Invalid or expired invite code")
                                  (aset "status" 400)))
             (let [invite-email (str/lower-case (str (:email invite)))
                   req-email    (str/lower-case (str email))]
               (when-not (= invite-email req-email)
                 (throw (doto (js/Error. "Invite email does not match")
                          (aset "status" 403))))
               (-> (honey-query-one! pool (q-invites/redeem (:id invite)))
                   (.then (fn [updated]
                            {:invite {:id          (:id updated)
                                      :org-id      (:org_id updated)
                                      :code        code
                                      :email       (:email updated)
                                      :status      (:status updated)
                                      :redeemed-at (:redeemed_at updated)
                                      :created-at  (:created_at updated)}
                             :user nil}))))))))))

(defn list-invites!
  [pool {:keys [org-id status]}]
  (if (str/blank? org-id)
    (js/Promise.reject (js/Error. "org-id is required"))
    (-> (honey-query! pool (if status
                              (q-invites/list-by-org-and-status org-id status)
                              (q-invites/list-by-org org-id)))
        (.then (fn [{:keys [rows]}]
                 {:invites (mapv (fn [{:keys [id org_id code email status role_slugs
                                               expires_at redeemed_at created_at]}]
                                   {:id          id :org-id org_id :code code :email email
                                    :status      status
                                    :role-slugs  (try
                                                   (cond
                                                     (nil? role_slugs)   []
                                                     (string? role_slugs) (js->clj (js/JSON.parse role_slugs))
                                                     :else                (js->clj role_slugs))
                                                   (catch :default _ []))
                                    :expires-at  expires_at :redeemed-at redeemed_at
                                    :created-at  created_at})
                                 rows)})))))

(defn sync-actor-contracts!
  [pool primary-org]
  (policy/sync-actor-projections!
   (sql-policy-store pool primary-org)
   (mapv :actor (list-actor-contracts))))

(defn sync-user-from-actor-contract!
  [pool primary-org opts]
  (sync-user-from-actor-contract!* pool primary-org opts))

(defn recover-session-secret!
  "Load the session secret from knoxx_config, generating and persisting one if absent.
   Returns Promise<string>."
  [pool]
  (-> (pg/query! pool "SELECT value FROM knoxx_config WHERE key = 'session_secret'" nil)
      (.then (fn [{:keys [rows]}]
               (if-let [stored (:value (first rows))]
                 (do (.log js/console "[knoxx-session] Recovered session secret from database")
                     stored)
                 (let [new-secret (.toString (.randomBytes crypto 32) "hex")]
                   (-> (pg/query! pool
                         "INSERT INTO knoxx_config (key, value) VALUES ('session_secret', $1)
                          ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value"
                         [new-secret])
                       (.then (fn [_]
                                (.log js/console "[knoxx-session] Generated and persisted session secret")
                                new-secret)))))))))

(defn update-user-actor!
  "Update a membership's actor-id and optionally its roles."
  [pool uid mid user-id {:keys [org-id actor-id role-slugs]}]
  (-> (honey-query-one! pool (q-memberships/by-user-and-org user-id org-id))
      (.then (fn [ms]
               (if-not ms
                 (js/Promise.reject (js/Error. "membership not found"))
                 (let [membership-id (:id ms)
                       resolved-actor (or (normalize-actor-id actor-id)
                                          (normalize-actor-id (:actor_id ms))
                                          (default-membership-actor-id (or role-slugs [])))]
                   (-> (if (seq role-slugs)
                         (set-membership-roles! pool membership-id
                                                {:org-id org-id :role-slugs role-slugs :replace true})
                         (js/Promise.resolve nil))
                       (.then (fn [_] (set-membership-actor-id! pool membership-id resolved-actor)))
                       (.then (fn [_]
                                (append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                                     :org-id org-id :action "user.update_actor"
                                                     :resource-kind "user" :resource-id user-id})))
                       (.then (fn [_] {:ok true})))))))))

(defn upsert-actor-credential!
  "Upsert an actor credential by user-id + org-id + provider."
  [pool _uid _mid user-id {:keys [org-id provider kind account-identifier secret-json status]}]
  (-> (honey-query-one! pool (q-memberships/by-user-and-org user-id org-id))
      (.then (fn [ms]
               (if-not ms
                 (js/Promise.reject (js/Error. "actor membership not found"))
                 (pg/query-one! pool
                   "INSERT INTO actor_credentials
                      (user_id, org_id, provider, kind, account_identifier, secret_json, status)
                    VALUES ($1::uuid, $2::uuid, $3, $4, $5, $6::jsonb, $7)
                    ON CONFLICT (user_id, org_id, provider, kind) DO UPDATE SET
                      account_identifier = COALESCE(EXCLUDED.account_identifier,
                                                    actor_credentials.account_identifier),
                      secret_json        = actor_credentials.secret_json || EXCLUDED.secret_json,
                      status             = EXCLUDED.status,
                      updated_at         = NOW()
                    RETURNING *"
                   [user-id org-id provider (or kind "credential") account-identifier
                    (js/JSON.stringify (clj->js (or secret-json {})))
                    (or status "active")]))))
      (.then (fn [row]
               {:credential (when row
                               {:id                 (:id row)
                                :user-id            (:user_id row)
                                :org-id             (:org_id row)
                                :provider           (:provider row)
                                :kind               (:kind row)
                                :account-identifier (:account_identifier row)
                                :status             (:status row)})}))))

;; ---------------------------------------------------------------------------
;; Policy context helpers
;; ---------------------------------------------------------------------------

(defn context-pool [policy-context] (:pool policy-context))
(defn configured? [policy-context] (boolean (or (:pool policy-context) (:query! policy-context))))
(defn context-primary-org [policy-context] (:primary-org policy-context))
(defn context-bootstrap [policy-context] (:bootstrap policy-context))
(defn context-actor-user-id [policy-context] (:bootstrap-user-id policy-context))
(defn context-actor-membership-id [policy-context] (:bootstrap-membership-id policy-context))

(defn close!
  [policy-context]
  (when-let [pool (context-pool policy-context)]
    (pg/end-pool! pool)))

(defn query!
  "Transitional raw-query entrypoint for legacy routes. Prefer named policy DB
   functions backed by shape.db query builders."
  [policy-context sql-str params]
  (cond
    (:query! policy-context)
    ((:query! policy-context) sql-str params)

    (context-pool policy-context)
    (pg/query! (context-pool policy-context) sql-str params)

    :else
    (js/Promise.resolve nil)))

(defn bootstrap-context!
  [policy-context]
  (if-let [f (:bootstrap-context! policy-context)]
    (f)
    (get-bootstrap-context! (context-pool policy-context)
                            (context-primary-org policy-context)
                            (context-bootstrap policy-context))))

(defn resolve-context!
  [policy-context headers-like]
  (if-let [f (:resolve-context! policy-context)]
    (f headers-like)
    (resolve-request-context! (context-pool policy-context) headers-like)))

(defn sync-actor-contracts-for-context!
  [policy-context]
  (if-let [f (:sync-actor-contracts! policy-context)]
    (f)
    (sync-actor-contracts! (context-pool policy-context)
                           (context-primary-org policy-context))))

(defn sync-user-from-actor-contract-for-context!
  [policy-context opts]
  (if-let [f (:sync-user-from-actor-contract! policy-context)]
    (f opts)
    (sync-user-from-actor-contract! (context-pool policy-context)
                                    (context-primary-org policy-context)
                                    opts)))

(defn create-user-for-context!
  [policy-context payload]
  (create-user! (context-pool policy-context)
                (context-actor-user-id policy-context)
                (context-actor-membership-id policy-context)
                payload))

(defn create-invite-for-context!
  [policy-context payload]
  (create-invite! (context-pool policy-context)
                  (context-actor-user-id policy-context)
                  (context-actor-membership-id policy-context)
                  payload))

(defn create-org-for-context!
  [policy-context payload]
  (create-org! (context-pool policy-context)
               (context-actor-user-id policy-context)
               (context-actor-membership-id policy-context)
               payload))

(defn create-role-for-context!
  [policy-context payload]
  (create-role! (context-pool policy-context)
                (context-actor-user-id policy-context)
                (context-actor-membership-id policy-context)
                payload))

(defn create-data-lake-for-context!
  [policy-context payload]
  (create-data-lake! (context-pool policy-context)
                     (context-actor-user-id policy-context)
                     (context-actor-membership-id policy-context)
                     payload))

(defn set-membership-roles-for-context!
  [policy-context membership-id payload]
  (set-membership-roles-public! (context-pool policy-context)
                                (context-actor-user-id policy-context)
                                (context-actor-membership-id policy-context)
                                membership-id
                                payload))

(defn update-user-actor-for-context!
  [policy-context user-id payload]
  (update-user-actor! (context-pool policy-context)
                      (context-actor-user-id policy-context)
                      (context-actor-membership-id policy-context)
                      user-id
                      payload))

(defn upsert-actor-credential-for-context!
  [policy-context user-id payload]
  (upsert-actor-credential! (context-pool policy-context)
                            (context-actor-user-id policy-context)
                            (context-actor-membership-id policy-context)
                            user-id
                            payload))

(defn get-actor-credential!
  [policy-context actor-id provider]
  (-> (policy/get-actor-credential (sql-policy-store (context-pool policy-context)
                                                     (context-primary-org policy-context))
                                   actor-id
                                   provider)
      (.then (fn [credential] {:credential credential}))))

;; ---------------------------------------------------------------------------
;; Initialisation
;; ---------------------------------------------------------------------------

(declare ensure-bootstrap-allowlist-users!)

(defn create-policy-db
  "Initialise the policy DB. Returns Promise<CLJS policy context | nil>."
  [options]
  (let [opts       (if (map? options)
                     options
                     (js->clj options :keywordize-keys true))
        conn-str   (or (:connection-string opts) (:connectionString opts) "")]
    (if (str/blank? conn-str)
      (js/Promise.resolve nil)
      (js/Promise.
       (fn [resolve reject]
         (let [pool (pg/create-pool! {:connection-string      conn-str
                                      :max                    (env-positive-int "KNOXX_POLICY_DB_POOL_MAX" 6)
                                      :idle-timeout-ms        (env-positive-int "KNOXX_POLICY_DB_IDLE_TIMEOUT_MS" 30000)
                                      :connect-timeout-ms     (env-positive-int "KNOXX_POLICY_DB_CONNECT_TIMEOUT_MS" 15000)})]
           (pg/on-pool-error! pool
             (fn [err _] (.error js/console "[policy-db] PG pool error:" (.-message err))))
           (pg/on-pool-connect! pool
             (fn [_] (when (env-truthy? "KNOXX_POLICY_DB_LOG_CONNECTS")
                       (.log js/console "[policy-db] New PG client connected"))))
           (-> (db-schema/ensure-schema! pool)
               (.then (fn [_] (db-schema/insert-permission-seeds! pool)))
               (.then (fn [_] (db-schema/insert-tool-seeds! pool)))
               (.then (fn [_] (ensure-primary-org! pool opts)))
               (.then
                (fn [primary-org]
                  (-> (sync-contract-role-projections! pool)
                      (.then (fn [_] (ensure-bootstrap-user! pool primary-org opts)))
                      (.then (fn [bootstrap]
                               (-> (if (seq (or (:bootstrapAllowlistEmails opts) (:bootstrap-allowlist-emails opts)))
                                     (ensure-bootstrap-allowlist-users! pool primary-org opts)
                                     (js/Promise.resolve nil))
                                   (.catch (fn [err]
                                             (.warn js/console "[policy-db] allowlist failed:"
                                                    (.-message err))
                                             nil))
                                   (.then (fn [_] bootstrap)))))
                      (.then (fn [bootstrap]
                               (-> (sync-actor-contracts! pool primary-org)
                                   (.catch (fn [err]
                                             (.warn js/console "[policy-db] actor sync failed:"
                                                    (.-message err))
                                             nil))
                                   (.then (fn [_] bootstrap)))))
                      (.then (fn [bootstrap]
                               (-> (honey-query! pool q-memberships/backfill-actor-ids)
                                   (.then (fn [_] bootstrap)))))
                      (.then (fn [bootstrap]
                               (.catch (cleanup-expired-sessions! pool) (fn [_] nil))
                               (resolve {:pool pool
                                         :primary-org primary-org
                                         :bootstrap bootstrap
                                         :bootstrap-user-id (get-in bootstrap [:user :id])
                                         :bootstrap-membership-id (get-in bootstrap [:membership :id])})
                               bootstrap)))))
               (.catch reject))))))))

(defn- ensure-bootstrap-allowlist-users! [pool primary-org opts]
  (let [raw-emails (or (:bootstrapAllowlistEmails opts) (:bootstrap-allowlist-emails opts) "")
        emails     (->> (str/split (str raw-emails) #"[\s,]+")
                        (map str/trim) (remove str/blank?) (map str/lower-case) distinct vec)
        raw-roles  (or (:bootstrapAllowlistRoleSlugs opts) (:bootstrap-allowlist-role-slugs opts) "")
        role-slugs (->> (str/split (str raw-roles) #"[\s,]+")
                        (map str/trim) (remove str/blank?) distinct vec)
        role-slugs (if (seq role-slugs) role-slugs ["knowledge_worker"])
        org-id     (:id primary-org)]
    (if (empty? emails)
      (js/Promise.resolve nil)
      (-> (js/Promise.all
           (into-array
            (mapv (fn [email]
                    (-> (honey-query-one! pool
                          (q-users/upsert {:email email :display-name email
                                           :auth-provider "bootstrap"
                                           :external-subject nil :status "active"}))
                        (.then (fn [user]
                                 (honey-query-one! pool
                                   (q-memberships/upsert {:user-id (:id user) :org-id org-id
                                                           :status "active" :is-default false}))))
                        (.then (fn [ms]
                                 (-> (js/Promise.all
                                      (into-array
                                       (mapv (fn [slug]
                                               (-> (find-role pool {:slug slug :org-id org-id})
                                                   (.then (fn [r] (or r (find-role pool {:slug slug :org-id nil}))))
                                                   (.then (fn [r]
                                                            (when r
                                                              (honey-query! pool
                                                                (q-roles/insert-membership-role
                                                                 (:id ms) (:id r))))))))
                                             role-slugs)))
                                     (.then (fn [_]
                                              (set-membership-actor-id!
                                               pool (:id ms) (default-membership-actor-id role-slugs)))))))))
                  emails)))
          (.then (fn [_] nil))))))
