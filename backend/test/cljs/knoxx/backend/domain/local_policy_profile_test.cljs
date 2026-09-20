(ns knoxx.backend.domain.local-policy-profile-test
  (:require [cljs.test :as test]
            [knoxx.backend.domain.local-policy-updates :as updates]))

(def ^:private principal
  {:principal/id "operator" :principal/entity-id "operator-entity" :principal/kind :human
   :principal/roles [] :principal/capabilities ["org.members.update"]})

(def ^:private actor
  {:principal principal :user-id "operator-user" :membership-id "operator-member"})

(def ^:private state
  {:orgs {"a" {:id "a" :status "active"} "b" {:id "b" :status "active"}}
   :users {"operator-user" {:id "operator-user" :principal-id "operator" :entity-id "operator-entity"
                            :kind :human :status "active"}
           "shared-user" {:id "shared-user" :display-name "Original name" :status "active"}}
   :memberships {"operator-member" {:id "operator-member" :user-id "operator-user" :org-id "a"
                                    :actor-id "operator" :status "active" :role-ids []}
                 "shared-a" {:id "shared-a" :user-id "shared-user" :org-id "a"
                             :actor-id "shared-actor" :status "active" :role-ids []}
                 "shared-b" {:id "shared-b" :user-id "shared-user" :org-id "b"
                             :actor-id "shared-actor" :status "active" :role-ids []}}
   :roles {"platform-admin" {:id "platform-admin" :slug "system-admin" :permissions []}}})

(test/deftest scoped-administrator-cannot-change-a-shared-global-profile
  (doseq [change [{:display-name "Cross-tenant overwrite"} {:status "disabled"}]]
    (let [failure (try (updates/update-user state actor "shared-user" (assoc change :org-id "a") "later")
                       nil (catch :default error error))]
      (test/is (= 403 (:status (ex-data failure)))))))

(test/deftest scoped-membership-update-preserves-the-other-tenant-and-global-profile
  (let [[updated result] (updates/update-user state actor "shared-user"
                                             {:org-id "a" :membership-status "disabled"} "later")]
    (test/is (:ok result))
    (test/is (= "disabled" (get-in updated [:memberships "shared-a" :status])))
    (test/is (= (get-in state [:memberships "shared-b"]) (get-in updated [:memberships "shared-b"])))
    (test/is (= (get-in state [:users "shared-user"])
                (dissoc (get-in updated [:users "shared-user"]) :updated-at)))))

(test/deftest platform-administrator-can-update-the-global-profile-explicitly
  (let [admin-state (assoc-in state [:memberships "operator-member" :role-ids] ["platform-admin"])
        [updated result] (updates/update-user admin-state actor "shared-user"
                                              {:org-id "a" :display-name "Authorized global name"} "later")]
    (test/is (:ok result))
    (test/is (= "Authorized global name" (get-in updated [:users "shared-user" :display-name])))
    (test/is (= (get-in state [:memberships "shared-b"]) (get-in updated [:memberships "shared-b"])))))
