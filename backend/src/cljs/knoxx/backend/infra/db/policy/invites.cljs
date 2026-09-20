(ns knoxx.backend.infra.db.policy.invites
  "Legacy Mongo invite creation, redemption and listing."
  (:require [clojure.string :as str]
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

(defn- invite-error [message status]
  (doto (js/Error. message)
    (aset "status" status)))

(defn- redeemed-invite-response [updated code]
  {:invite {:id          (:id updated)
            :org-id      (:org_id updated)
            :code        code
            :email       (:email updated)
            :status      (:status updated)
            :redeemed-at (:redeemed_at updated)
            :created-at  (:created_at updated)}})

(defn ^:async redeem-invite!
  [pool code email]
  (if (or (str/blank? code) (str/blank? email))
    (throw (js/Error. "code and email are required"))
    (let [db (await (policy-connection/db!))
          invite (await (mongo-invites/pending-by-code! db code))]
      (if-not invite
        (throw (invite-error "Invalid or expired invite code" 400))
        (let [invite-email (str/lower-case (str (:email invite)))
              req-email    (str/lower-case (str email))
              role-slugs   (or (seq (parse-role-slugs-json (:role_slugs invite)))
                               ["basic-user"])]
          (when-not (= invite-email req-email)
            (throw (invite-error "Invite email does not match" 403)))
          (let [updated (await (mongo-invites/redeem-invite! db (:id invite)))]
            (await (policy-users/create-user! pool nil nil
                                 {:email (:email updated)
                                  :display-name (:email updated)
                                  :auth-provider "invite"
                                  :status "active"
                                  :membership-status "active"
                                  :org-id (:org_id updated)
                                  :role-slugs (vec role-slugs)
                                  :is-default true}))
            (redeemed-invite-response updated code)))))))

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
