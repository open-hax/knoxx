(ns knoxx.backend.infra.db.policy.invites
  "Mongo invite ownership around provisioning, which also writes actor contract files.
   Known failures release the claim. Crashes or uncertain completion retain it for
   verified recovery: neither automatic lease stealing nor cross-store rollback is promised."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.mongo-policy-invites :as admission]
            [knoxx.backend.infra.stores.mongo-policy-invites :as mongo-invites]
            ["node:crypto" :as crypto]
            [knoxx.backend.infra.db.policy.connection :as policy-connection]
            [knoxx.backend.infra.db.policy.users :as policy-users]))

(defn- invite-response
  [row code]
  {:invite {:id         (:id row)
            :org-id     (:org_id row)
            :code       code
            :email      (:email row)
            :status     (:status row)
            :expires-at (:expires_at row)
            :created-at (:created_at row)}})

(defn ^:async create-invite!
  [pool uid mid {:keys [org-id email role-slugs inviter-membership-id]}]
  (cond
    (str/blank? org-id) (throw (js/Error. "org-id is required"))
    (str/blank? email)  (throw (js/Error. "email is required"))
    :else
    (when-let [db (await (policy-connection/ensure-mongo-policy-db!))]
      (let [slugs      (or role-slugs ["basic-user"])
            code       (.toString (.randomBytes crypto 8) "hex")
            expires-at (js/Date. (+ (js/Date.now) (* 7 24 3600 1000)))
            row (await (mongo-invites/insert-invite!
                        db {:org-id               org-id
                            :code                 code
                            :email                email
                            :inviter-membership-id (or inviter-membership-id mid)
                            :role-slugs-json      (js/JSON.stringify (clj->js slugs))
                            :expires-at           (.toISOString expires-at)}))]
        (await (policy-connection/append-audit! pool {:actor-user-id uid :actor-membership-id mid
                                   :org-id org-id :action "invite.create"
                                   :resource-kind "invite" :resource-id (:id row)}))
      (invite-response row code)))))

(defn- parse-role-slugs-json
  [value]
  (try
    (let [parsed (cond
                   (nil? value) []
                   (string? value) (js->clj (js/JSON.parse value))
                   :else (js->clj value))]
      (if (sequential? parsed) (vec parsed) []))
    (catch :default _ [])))

(defn- redeemed-invite-response [updated code]
  {:invite {:id          (:id updated)
            :org-id      (:org_id updated)
            :code        code
            :email       (:email updated)
            :status      (:status updated)
            :redeemed-at (:redeemed_at updated)
            :created-at  (:created_at updated)}})

(defn- unconfirmed [invite cause]
  (ex-info "Invite provisioning outcome needs verification before recovery"
           {:status 503 :code "invite_redemption_unconfirmed" :invite-id (:id invite)} cause))

(defn- ^:async release-failed-provisioning! [db invite cause]
  (try
    (when-not (await (admission/release! db (:id invite) (:redemption_claim_id invite)))
      (throw (unconfirmed invite cause)))
    (catch :default recovery-error (throw (unconfirmed invite recovery-error))))
  (throw cause))

(defn- legacy-provision! [pool invite]
  (policy-users/create-user!
   pool nil nil
   {:email (:email invite) :display-name (:email invite) :auth-provider "invite"
    :status "active" :membership-status "active" :org-id (:org_id invite)
    :role-slugs (:role-slugs invite) :is-default true}))

(defn- ^:async provision! [db invite provision-member!]
  (try
    (await (provision-member! (assoc invite :role-slugs
                                     (vec (or (seq (parse-role-slugs-json (:role_slugs invite))) ["basic-user"])))))
    (catch :default cause (await (release-failed-provisioning! db invite cause)))))

(defn ^:async redeem-invite!
  "Reserve, provision and finalize; authenticated callers supply the verified identity email.
   The explicit trusted callback must finish membership/binding admission before finalization."
  ([pool code email]
   (await (redeem-invite! pool code email {:provision! #(legacy-provision! pool %)})))
  ([_pool code email {provision-member! :provision!}]
   (when-not (fn? provision-member!)
     (throw (ex-info "Invite membership provisioner is required" {:status 500 :code "invite_provisioner_missing"})))
   (let [db (await (policy-connection/db!)) invite (await (admission/reserve! db code email))]
     (await (provision! db invite provision-member!))
     (try
       (redeemed-invite-response
        (await (admission/finalize! db (:id invite) (:redemption_claim_id invite))) code)
       ;; Provisioning has completed: releasing now could repeat an uncertain effect.
       (catch :default cause (throw (unconfirmed invite cause)))))))

(defn- invite-row->map
  [{:keys [id org_id code email status role_slugs expires_at redeemed_at created_at]}]
  {:id id
   :org-id org_id
   :code code
   :email email
   :status status
   :role-slugs (parse-role-slugs-json role_slugs)
   :expires-at expires_at
   :redeemed-at redeemed_at
   :created-at created_at})

(defn ^:async list-invites!
  [_pool {:keys [org-id status]}]
  (if (str/blank? org-id)
    (throw (js/Error. "org-id is required"))
    (when-let [db (await (policy-connection/ensure-mongo-policy-db!))]
      {:invites (mapv invite-row->map
                      (await (mongo-invites/list-invites-by-org! db org-id status)))})))
