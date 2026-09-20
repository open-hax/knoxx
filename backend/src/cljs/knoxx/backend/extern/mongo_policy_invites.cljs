(ns knoxx.backend.extern.mongo-policy-invites
  "Atomic invitation ownership on Mongo; a crash never silently replays provisioning."
  (:require [clojure.string :as str]
            [knoxx.backend.law.persistence-instant :as instant]))

(defn- collection [db] (.collection db "knoxx_invites"))

(defn- checked-text! [values]
  (when-not (every? #(and (string? %) (not (str/blank? %))) values)
    (throw (ex-info "Invite coordinates must be nonblank text"
                    {:status 400 :code "invite_coordinates_invalid"}))))

(defn- unconfirmed! []
  (throw (ex-info "Invite provisioning already has an owner; verify recovery before retrying"
                  {:status 409 :code "invite_redemption_unconfirmed"})))

(defn- row-data [raw]
  (when raw
    (let [data (js->clj raw :keywordize-keys true)]
      (-> (into {} (map (fn [[key value]]
                         [key (if (instance? js/Date value) (.toISOString value) value)])) data)
          (assoc :id (:invite_id data))
          (dissoc :invite_id :_id)))))

(defn- checked-expiry! [raw now]
  (let [value (aget raw "expires_at")
        text (if (and (instance? js/Date value) (js/Number.isFinite (.getTime value)))
               (.toISOString value) value)]
    (when-not (instant/instant? text)
      (throw (ex-info "Stored invite expiry is invalid" {:status 503 :code "invite_expiry_invalid"})))
    (when (<= (.getTime (js/Date. text)) (.getTime now))
      (throw (ex-info "Invalid or expired invite code" {:status 400 :code "invite_expired"})))
    value))

(defn- checked-pending! [raw email]
  (when-not raw
    (throw (ex-info "Invalid or expired invite code" {:status 400 :code "invite_not_found"})))
  (when-not (= email (str/lower-case (str (aget raw "email"))))
    (throw (ex-info "Invite email does not match" {:status 403 :code "invite_email_mismatch"})))
  (case (aget raw "status")
    "pending" nil
    "provisioning" (unconfirmed!)
    (throw (ex-info "Invite is no longer pending" {:status 400 :code "invite_not_pending"}))))

(defn ^:async reserve!
  "Claim one matching, unexpired invite. The persisted owner has no automatic lease takeover."
  [db code email]
  (checked-text! [code email])
  (let [coll (collection db) email (str/lower-case (str/trim email))
        raw (await (.findOne coll #js {:code code}))
        _ (checked-pending! raw email)
        now (js/Date.) expiry (checked-expiry! raw now)
        id (aget raw "invite_id") claim-id (str (random-uuid))]
    (checked-text! [id])
    (or (row-data
         (await (.findOneAndUpdate
                 coll
                 #js {:invite_id id :code code :email (aget raw "email") :status "pending"
                      :expires_at #js {"$eq" expiry "$gt" (if (string? expiry) (.toISOString now) now)}}
                 #js {"$set" #js {:status "provisioning" :redemption_claim_id claim-id :provisioning_at now}}
                 #js {:returnDocument "after"})))
        (unconfirmed!))))

(defn ^:async finalize!
  "Only the reserved owner may record completed provisioning; a lost owner refuses."
  [db id claim-id]
  (checked-text! [id claim-id])
  (or (row-data
       (await (.findOneAndUpdate
               (collection db) #js {:invite_id id :status "provisioning" :redemption_claim_id claim-id}
               #js {"$set" #js {:status "redeemed" :redeemed_at (js/Date.)}
                    "$unset" #js {:redemption_claim_id "" :provisioning_at ""}}
               #js {:returnDocument "after"})))
      (unconfirmed!)))

(defn ^:async release!
  "Release only this reservation after a known provisioning failure. Never undo another owner."
  [db id claim-id]
  (checked-text! [id claim-id])
  (let [result (await (.updateOne
                       (collection db) #js {:invite_id id :status "provisioning" :redemption_claim_id claim-id}
                       #js {"$set" #js {:status "pending"}
                            "$unset" #js {:redemption_claim_id "" :provisioning_at ""}}))]
    (= 1 (.-matchedCount result))))
