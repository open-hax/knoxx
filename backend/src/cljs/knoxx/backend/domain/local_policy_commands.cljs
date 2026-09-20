(ns knoxx.backend.domain.local-policy-commands
  "Finite accepted command grammar, replay validation and first-fact idempotence."
  (:require [knoxx.backend.domain.local-policy :as policy]
            [knoxx.backend.domain.local-policy-directory :as directory]
            [knoxx.backend.domain.local-policy-updates :as updates]
            [knoxx.backend.law.local-policy :as law]))

(defn- stable-command
  [command]
  (-> (select-keys command [:id :operation :args])
      (update :args (fn [args] (mapv #(if (map? %) (dissoc % :verified-principal) %) args)))
      (assoc :actor (select-keys (:actor command) [:user-id :membership-id]))))

(defn- retry-authorized!
  [state {:keys [operation actor args]}]
  (let [[a b] args
        member (when (#{:policy/membership-roles :policy/membership-tools} operation) (law/present! state :memberships a))
        role (when (= operation :policy/role-tools) (law/present! state :roles a))
        [org-id permission]
        (case operation
          :policy/create-org [nil "platform.org.create"]
          :policy/create-user [(:org-id a) "org.users.create"]
          :policy/create-role [(:org-id a) "org.roles.create"]
          :policy/create-data-lake [(:org-id a) "org.datalakes.create"]
          :policy/membership-roles [(:org-id member) "org.members.update"]
          :policy/membership-tools [(:org-id member) "org.user_policy.update"]
          :policy/role-tools [(:org-id role) "org.tool_policy.update"]
          :policy/update-user [(:org-id b) "org.members.update"]
          :policy/upsert-credential [(:org-id b) "org.user_policy.update"]
          (law/refuse! 400 "local_policy_unknown_command" "Unknown local policy command"))]
    (policy/authorize! state actor org-id permission)))

(defn- execute
  [state {:keys [operation actor args id at]}]
  (let [[a b] args]
    (case operation
      :policy/create-org (directory/create-org state actor a (str "org_" id) at)
      :policy/create-user (directory/create-user state actor a {:user-id (str "user_" id) :membership-id (str "member_" id)} at)
      :policy/create-role (directory/create-role state actor a (str "role_" id) at)
      :policy/create-data-lake (directory/create-data-lake state actor a (str "lake_" id) at)
      :policy/membership-roles (updates/membership-roles state actor a b at)
      :policy/membership-tools (updates/tool-policies state actor :memberships a b at)
      :policy/role-tools (updates/tool-policies state actor :roles a b at)
      :policy/update-user (updates/update-user state actor a b at)
      :policy/upsert-credential (updates/upsert-credential state actor a b (str "credential_" id) at)
      (law/refuse! 400 "local_policy_unknown_command" "Unknown local policy command"))))

(defn admit
  "Return the first accepted result on exact retry; changed operation IDs always conflict."
  [state command]
  (law/assert-schema! law/Command command)
  (retry-authorized! state command)
  (let [stable (stable-command command)]
    (if-let [existing (get-in state [:commands (:id command)])]
      (if (= stable (:request existing)) [state (:result existing)]
          (law/refuse! 409 "local_policy_operation_conflict" "Operation ID is already bound to a different command"))
      (let [[next result] (execute state command)
            audit {:id (:id command) :at (:at command) :operation (:operation command)
                   :principal-id (get-in command [:actor :principal :principal/id])
                   :user-id (get-in command [:actor :user-id])
                   :membership-id (get-in command [:actor :membership-id])}]
        [(-> next (assoc-in [:commands (:id command)] {:request stable :result result})
             (update :audit (fnil conj []) audit)) result]))))
