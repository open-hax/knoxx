(ns knoxx.backend.domain.local-policy-reads
  "Named read operations for the policy directory; no query emulation."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.local-policy :as policy]
            [knoxx.backend.law.local-policy :as law]
            [knoxx.backend.domain.local-policy-projection :as projection]))

(defn active-credential?
  "Credentials are usable only while their exact owner, membership and organization remain active."
  [state row]
  (let [member (policy/membership-for state (:user-id row) (:org-id row))]
    (and member (= (:actor-id row) (:actor-id member))
         (every? #(= "active" (:status %))
                 [row member (get-in state [:users (:user-id row)]) (get-in state [:orgs (:org-id row)])]))))

(defn scoped-credential
  "Require all supplied actor, membership and org coordinates; ambiguous ownership is refused."
  [state actor-id provider {:keys [org-id membership-id]}]
  (let [members (filter #(and (= actor-id (:actor-id %))
                               (or (nil? org-id) (= org-id (:org-id %)))
                               (or (nil? membership-id) (= membership-id (:id %))))
                         (vals (:memberships state)))]
    (when (> (count members) 1)
      (law/refuse! 409 "local_policy_ambiguous_actor" "Actor credential lookup requires an exact membership scope"))
    (when-let [member (first members)]
      (some #(when (and (= [(:user-id member) (:org-id member) provider]
                           [(:user-id %) (:org-id %) (:provider %)])
                        (active-credential? state %)) %)
            (vals (:credentials state))))))

(defn authorize-read!
  "Recheck scoped directory read authority against the exact projection being returned."
  [state actor operation args]
  (let [[a] args
        ctx (policy/actor-context state actor)
        [org-id permissions]
        (case operation
          :policy/orgs [nil ["platform.org.read"]]
          :policy/users [(:org-id a) [(if (:org-id a) "org.users.read" "platform.org.read")]]
          :policy/roles [(:org-id a) [(if (:org-id a) "org.roles.read" "platform.roles.manage")]]
          :policy/memberships [(:org-id a) ["org.members.read"]]
          :policy/data-lakes [(:org-id a) ["org.datalakes.read"]]
          :policy/role [(:org-id (get-in state [:roles a])) ["org.roles.read" "org.tool_policy.update"]]
          :policy/membership [(:org-id (get-in state [:memberships a])) ["org.members.read" "org.members.update" "org.user_policy.update"]]
          :policy/permissions [(get-in ctx [:org :id]) ["platform.roles.manage" "org.roles.read"]]
          :policy/tools [(get-in ctx [:org :id]) ["platform.roles.manage" "org.tool_policy.read" "org.user_policy.read"]]
          (law/refuse! 403 "local_policy_private_read" "This read is restricted to trusted provider composition"))]
    (when-not (or (policy/administrator? ctx)
                   (and (or (nil? org-id) (= org-id (get-in ctx [:org :id])))
                        (some (set (:permissions ctx)) permissions)))
      (law/refuse! 403 "local_policy_permission_denied" "Current policy does not authorize this directory read"))))

(defn read-operation
  "Evaluate one declared read against an immutable directory projection."
  [state operation args]
  (let [[a b c] args
        org-id (:org-id a)]
    (case operation
      :policy/state state
      :policy/context (policy/context state a b)
      :policy/org-by-id (get-in state [:orgs a])
      :policy/org-by-slug (some #(when (= a (:slug %)) %) (vals (:orgs state)))
      :policy/orgs {:orgs (mapv #(projection/org state %) (policy/ordered (vals (:orgs state))))}
      :policy/role {:role (get-in state [:roles a])}
      :policy/roles {:roles (policy/ordered (filter #(or (nil? org-id) (nil? (:org-id %)) (= org-id (:org-id %))) (vals (:roles state))))}
      :policy/role-by-slug (policy/role-by-slug state org-id (:slug a))
      :policy/membership {:membership (projection/membership state (get-in state [:memberships a]))}
      :policy/memberships {:memberships (mapv #(projection/membership state %) (policy/ordered (filter #(= org-id (:org-id %)) (vals (:memberships state)))))}
      :policy/users {:users (mapv #(projection/user state % org-id)
                                  (policy/ordered (filter #(or (nil? org-id) (policy/membership-for state (:id %) org-id)) (vals (:users state)))))}
      :policy/data-lakes {:data-lakes (policy/ordered (filter #(= org-id (:org-id %)) (vals (:data-lakes state))))}
      :policy/credential {:credential (projection/credential (scoped-credential state a b c) true)}
      :policy/credentials {:credentials (mapv #(projection/credential % true) (policy/ordered (filter #(and (= a (:provider %)) (active-credential? state %)) (vals (:credentials state)))))}
      :policy/permissions {:permissions (mapv (fn [code] {:id code :code code :resourceKind (first (str/split code #"\.")) :description ""})
                                             (sort (set (mapcat :permissions (vals (:roles state))))))}
      :policy/tools {:tools (policy/ordered (vals (:tools state)))}
      :policy/audit {:events (vec (:audit state))}
      (law/refuse! 400 "local_policy_unknown_read" "Unknown policy read operation"))))

(def operations
  "The complete finite read grammar available to providers."
  #{:policy/state :policy/context :policy/org-by-id :policy/org-by-slug :policy/orgs
    :policy/role :policy/roles :policy/role-by-slug :policy/membership :policy/memberships
    :policy/users :policy/data-lakes :policy/credential :policy/credentials
    :policy/permissions :policy/tools :policy/audit})
