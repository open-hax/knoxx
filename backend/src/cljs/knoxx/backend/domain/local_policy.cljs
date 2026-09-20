(ns knoxx.backend.domain.local-policy
  "Pure policy projections and grant checks over an accepted directory history."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [knoxx.backend.law.identity-binding :as binding-law]
            [knoxx.backend.law.local-policy :as law]))

(defn ordered
  "Stable response order independent of EDN map iteration."
  [items] (vec (sort-by :id items)))

(defn membership-for
  "Resolve the unique user/organization membership."
  [state user-id org-id]
  (some #(when (= [user-id org-id] [(:user-id %) (:org-id %)]) %)
        (vals (:memberships state))))

(defn role-by-slug
  "Prefer the organization's exact role, then an explicitly installed platform role."
  [state org-id slug]
  (let [normalized (str/replace slug "_" "-")
        candidates (filter #(= normalized (:slug %)) (vals (:roles state)))]
    (or (some #(when (= org-id (:org-id %)) %) candidates)
        (some #(when (nil? (:org-id %)) %) candidates))))

(defn resolve-roles
  "Resolve explicit role references and refuse missing or foreign-tenant grants."
  [state org-id ids slugs]
  (let [roles (concat (map #(law/present! state :roles %) ids)
                      (map #(or (role-by-slug state org-id %)
                                (law/refuse! 400 "local_policy_unknown_role" "Unknown role slug")) slugs))]
    (doseq [role roles]
      (when-not (or (nil? (:org-id role)) (= org-id (:org-id role)))
        (law/refuse! 403 "local_policy_foreign_role" "Role belongs to another organization")))
    (ordered (vals (into {} (map (juxt :id identity)) roles)))))

(defn membership-roles
  "Use durable overrides, otherwise current Axxium roles from the explicit catalog."
  [state membership principal]
  (if (contains? membership :role-ids)
    (resolve-roles state (:org-id membership) (:role-ids membership) [])
    (->> (:principal/roles principal)
         (keep (fn [slug]
                 (some #(when (and (nil? (:org-id %)) (= slug (:slug %))) %)
                       (vals (:roles state))))) ordered)))

(defn merge-policies
  "Explicit deny wins across roles and membership policies."
  [policies]
  (->> policies
       (reduce (fn [result item]
                 (if (= "deny" (:effect (get result (:tool-id item))))
                   result (assoc result (:tool-id item) item))) {})
       vals (sort-by :tool-id) vec))

(defn context
  "Resolve an exact active binding using the current directory and current principal."
  [state binding principal]
  (binding-law/assert-principal-binding! principal binding)
  (let [user (law/present! state :users (:user-id binding))
        org (law/present! state :orgs (:org-id binding))
        member (law/present! state :memberships (:membership-id binding))
        roles (membership-roles state member principal)]
    (when-not (and (= (:principal/id principal) (:principal-id user))
                   (= (:user-id binding) (:user-id member)) (= (:org-id binding) (:org-id member))
                   (= (:actor-id binding) (:actor-id member))
                   (every? #(= "active" (:status %)) [user org member]))
      (law/refuse! 403 "local_policy_inactive_binding" "The policy membership is inactive or no longer matches its identity"))
    {:user (select-keys user [:id :email :username :display-name :status])
     :org org :membership (assoc (select-keys member [:id :status :is-default]) :actor-id (:actor-id member))
     :actor {:id (:actor-id member) :binding (:actor-id member)} :actor-binding (:actor-id member)
     :roles roles :role-slugs (vec (sort (map :slug roles)))
     :permissions (vec (sort (set (concat (:principal/capabilities principal) (mapcat :permissions roles)))))
     :tool-policies (merge-policies (concat (mapcat :tool-policies roles) (:tool-policies member)))
     :membership-tool-policies (or (:tool-policies member) [])}))

(defn actor-context
  "Recheck the command actor against the same projection the command will mutate."
  [state actor]
  (law/assert-schema! law/Actor actor)
  (let [member (law/present! state :memberships (:membership-id actor))
        user (law/present! state :users (:user-id actor))
        principal (:principal actor)]
    (context state {:principal-id (:principal-id user) :entity-id (:entity-id user)
                    :kind (:kind user) :user-id (:id user) :membership-id (:id member)
                    :org-id (:org-id member) :actor-id (:actor-id member)} principal)))

(defn administrator?
  "Only the installed platform administrator role grants cross-organization authority."
  [ctx]
  (boolean (some #(and (nil? (:org-id %)) (= "system-admin" (:slug %))) (:roles ctx))))

(defn authorize!
  "Require current durable permission and tenant membership at command admission."
  [state actor org-id permission]
  (let [ctx (actor-context state actor)]
    (when-not (or (administrator? ctx)
                   (and (= org-id (get-in ctx [:org :id]))
                        (contains? (set (:permissions ctx)) permission)))
      (law/refuse! 403 "local_policy_permission_denied" "Current policy does not authorize this command"))
    ctx))

(defn grantable!
  "Organization administrators cannot create or assign authority they do not hold."
  [ctx roles]
  (when-not (administrator? ctx)
    (let [permissions (set (:permissions ctx))
          own-policies (into {} (map (juxt :tool-id identity)) (:tool-policies ctx))
          resulting (into {} (map (juxt :tool-id identity)) (merge-policies (mapcat :tool-policies roles)))]
      (doseq [[id item] own-policies]
        (when (and (= "deny" (:effect item)) (not= item (get resulting id)))
          (law/refuse! 403 "local_policy_deny_removal" "Cannot remove an effective denial from authority you administer")))
      (doseq [role roles]
        (when (or (#{"system-admin" "system_admin"} (:slug role))
                   (not (set/subset? (set (:permissions role)) permissions)))
          (law/refuse! 403 "local_policy_grant_escalation" "Cannot grant permissions outside current authority"))
        (doseq [policy (:tool-policies role)]
          (when (and (= "allow" (:effect policy))
                     (not= policy (get own-policies (:tool-id policy))))
            (law/refuse! 403 "local_policy_grant_escalation" "Cannot grant a tool policy outside current authority"))))))
  roles)
