(ns knoxx.backend.shape.db.sessions
  "Pure HoneySQL query builders for session persistence. No execution."
  (:require [honey.sql.helpers :as h]))

(defn insert
  [{:keys [user-id membership-id org-id token-hash token-prefix salt
           email display-name auth-provider external-subject
           ip-address user-agent expires-at]}]
  (-> (h/insert-into :sessions)
      (h/values [{:user_id          [:cast user-id :uuid]
                  :membership_id    [:cast membership-id :uuid]
                  :org_id           [:cast org-id :uuid]
                  :token_hash       token-hash
                  :token_prefix     token-prefix
                  :salt             salt
                  :email            email
                  :display_name     display-name
                  :auth_provider    auth-provider
                  :external_subject external-subject
                  :ip_address       ip-address
                  :user_agent       user-agent
                  :expires_at       expires-at}])
      (h/returning :*)))

(defn by-prefix
  "Candidate sessions for constant-time token verification."
  [prefix]
  (-> (h/select :*)
      (h/from :sessions)
      (h/where [:and
                [:= :token_prefix prefix]
                [:> :expires_at [:now]]])
      (h/order-by [:created_at :desc])
      (h/limit 50)))

(defn all-active
  "Fallback for sessions created before token_prefix was added."
  []
  (-> (h/select :*)
      (h/from :sessions)
      (h/where [:> :expires_at [:now]])
      (h/order-by [:created_at :desc])
      (h/limit 200)))

(defn touch
  "Update last_seen_at for an active session."
  [session-id]
  (-> (h/update :sessions)
      (h/set {:last_seen_at [:now]})
      (h/where [:= :id [:cast session-id :uuid]])))

(defn delete-by-id
  [session-id]
  (-> (h/delete-from :sessions)
      (h/where [:= :id [:cast session-id :uuid]])))

(defn delete-expired
  []
  (-> (h/delete-from :sessions)
      (h/where [:< :expires_at [:now]])))
