(ns knoxx.backend.infra.db.policy.projection
  "Project actor contracts into Mongo directory membership and roles."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.stores.mongo-policy-directory :as mongo-directory]
            [knoxx.backend.infra.db.actors :as policy-actors]
            [knoxx.backend.domain.policy.protocol :as policy]
            [knoxx.backend.infra.db.policy.support :as policy-support]
            [knoxx.backend.law.membership-actor :as actor-law]
            [knoxx.backend.law.policy-values :as values]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]
            [knoxx.backend.infra.db.policy.roles :as policy-roles]))

(defn- ^:async project-actor-role-slugs!
  "Best-effort set the projected membership's roles, keeping the membership
   when role projection fails (mirrors sql-adapter's .catch behavior)."
  [membership actor-id role-slugs]
  (try
    (await (policy-roles/set-membership-roles! nil (:id membership)
                                  {:org-id (:org_id membership)
                                   :role-slugs role-slugs :role-ids []
                                   :replace true :contract-projection true}))
    (catch :default err
      (.warn js/console
             "[policy-mongo] actor role projection failed; keeping actor membership"
             actor-id (.-message err)))))

(defn- ^:async projection-plan!
  "Validate assigned coordinates before admitting any directory projection write."
  [primary-org actor]
  (let [validated (policy/validate-actor! actor)
        actor-id (:actor/id validated)
        db (await (policy-connection/db!))
        email (or (values/normalize-email (:actor/email validated))
                  (policy-actors/actor-email-from-id actor-id))
        display-name (or (some-> (:actor/label validated) str str/trim not-empty)
                         actor-id email)
        role-slugs (values/actor-projection-role-slugs validated)
        org-slug (some-> (:actor/org validated) str str/trim not-empty)
        org (if org-slug
              (or (await (mongo-directory/find-org-by-slug db org-slug))
                  (throw (ex-info "Actor organization was not found"
                                  {:status 404 :code "actor_projection_org_not_found"})))
              primary-org)]
    (when-not org
      (throw (js/Error. "primary org is required for actor projection sync")))
    (let [existing-user (await (mongo-directory/find-user-by-email! db email))
          existing (when existing-user
                     (await (mongo-directory/find-membership-by-user-and-org! db (:id existing-user) (:id org))))]
      (actor-law/assert-assignment! (:actor_id existing) actor-id)
      {:db db :actor-id actor-id :email email :display-name display-name
       :role-slugs role-slugs :org org :existing existing})))

(defn ^:async project-actor-via-store!
  "Project one actor after checking its immutable membership coordinate."
  [_pool primary-org actor]
  (let [{:keys [db actor-id email display-name role-slugs org existing]}
        (await (projection-plan! primary-org actor))]
    (when existing (await (mongo-directory/set-membership-actor-id! db (:id existing) actor-id)))
    (let [user (await (mongo-directory/create-user!
                       db {:email email :display-name display-name
                           :auth-provider "actor-contract" :external-subject nil
                           :status "active"}))
          membership (await (mongo-directory/upsert-membership!
                             db {:user-id (:id user) :org-id (:id org)
                                 :status "active" :is-default true}))]
      (when-not existing
        (await (mongo-directory/set-membership-actor-id! db (:id membership) actor-id)))
      (await (project-actor-role-slugs! membership actor-id role-slugs))
      membership)))

(defn ^:async sync-actor-projections!
  [pool primary-org actors]
  ;; Refuse a conflicting later historical declaration before an earlier one can
  ;; overwrite profile or role state. Each actual projection rechecks for races.
  (doseq [actor actors] (await (projection-plan! primary-org actor)))
  (await (policy-support/promise-each actors #(project-actor-via-store! pool primary-org %)))
  nil)

(defn- sync-user-from-actor-contract!* [pool primary-org payload]
  (let [actor-id (values/normalize-actor-id (or (:actor-id payload) (:actor_id payload)))
        email    (values/normalize-email (:email payload))]
    (if-not (or email actor-id)
      (js/Promise.resolve nil)
      (if-let [contract (or (policy-support/find-actor-contract-by-id actor-id)
                            (policy-support/find-user-actor-contract-by-email email))]
        (sync-actor-projections! pool primary-org [(:actor contract)])
        (js/Promise.resolve nil)))))

(defn ^:async sync-actor-contracts!
  [pool primary-org]
  (await (sync-actor-projections! pool primary-org (mapv :actor (policy-support/list-actor-contracts)))))

(defn sync-user-from-actor-contract!
  [pool primary-org opts]
  (sync-user-from-actor-contract!* pool primary-org opts))
