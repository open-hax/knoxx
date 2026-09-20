(ns knoxx.backend.mongo-policy-mutations-test
  "Exercise Mongo orchestration ports together with actual actor contract persistence."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.infra.db.actors :as actors]
            [knoxx.backend.infra.db.policy.connection :as connection]
            [knoxx.backend.infra.db.policy.projection :as projection]
            [knoxx.backend.infra.db.policy.roles :as roles]
            [knoxx.backend.infra.db.policy.support :as support]
            [knoxx.backend.infra.db.policy.users :as users]
            [knoxx.backend.infra.stores.mongo-policy-directory :as directory]
            [knoxx.backend.infra.stores.mongo-policy-roles :as mongo-roles]))

(def ^:private initial-state
  {:user {:id "user-a" :email "alice@example.test" :display_name "Alice"}
   :membership {:id "member-a" :user_id "user-a" :org_id "org-a"
                :actor_id "actor-a" :status "active" :is_default true}
   :role-ids ["role-basic"]
   :role-catalog {"role-basic" "basic-user" "role-admin" "system-admin"}
   :tool-policies [{:tool-id "wiki.read" :effect "allow"}]
   :effects []})

(defn- joined-member [state]
  (merge (:membership state) (select-keys (:user state) [:email :display_name])
         {:org_slug "tenant-a"}))

(defn- effective-roles [state]
  (mapv (fn [id] {:membership_id "member-a" :role_id id
                 :slug (get (:role-catalog state) id)}) (:role-ids state)))

(defn- apply-roles! [state {:keys [role-ids role-slugs replace]}]
  (let [by-slug (into {} (map (fn [[id slug]] [slug id])) (:role-catalog @state))
        selected (concat role-ids (map by-slug role-slugs))]
    (swap! state update :effects conj :roles)
    (swap! state update :role-ids #(vec (distinct (concat (when-not replace %) selected))))))

(defn- with-default-db-arity [f]
  (fn ([arg] (f nil arg)) ([db arg] (f db arg))))

(defn- with-default-db-two-args [f]
  (fn ([a b] (f nil a b)) ([db a b] (f db a b))))

(defn- ^:async with-read-ports! [state contracts run!]
  (with-redefs [connection/db! (fn [] :fixture-db)
                directory/find-user-by-email! (with-default-db-arity (fn [_ email] (when (= "alice@example.test" email) (:user @state))))
                directory/get-membership! (with-default-db-arity (fn [_ id] (when (= "member-a" id) (:membership @state))))
                directory/find-membership-by-user-and-org!
                (with-default-db-two-args (fn [_ user org] (when (= ["user-a" "org-a"] [user org]) (:membership @state))))
                directory/find-membership-row-with-user-org! (with-default-db-arity (fn [_ _] (joined-member @state)))
                directory/find-org-by-slug (with-default-db-arity (fn [_ slug] (when (= "tenant-a" slug) {:id "org-a" :slug slug})))
                mongo-roles/roles-for-memberships! (with-default-db-arity (fn [_ _] (effective-roles @state)))
                roles/find-org-by-id (fn [_ _] {:id "org-a" :slug "tenant-a"})
                support/find-user-actor-contract-by-email (fn [email] (actors/find-user-actor-contract-by-email contracts email))]
    (await (run!))))

(defn- ^:async with-directory-writes! [state run!]
  (with-redefs [connection/append-audit! (fn [_ _] (swap! state update :effects conj :audit))
                directory/create-user! (with-default-db-arity (fn [_ payload]
                                         (swap! state update :effects conj :create-user)
                                         (swap! state update :user merge
                                                {:email (:email payload) :display_name (:display-name payload)})
                                         (:user @state)))
                directory/upsert-membership! (with-default-db-arity (fn [_ payload]
                                               (swap! state update :effects conj :upsert-membership)
                                               (swap! state update :membership merge {:org_id (:org-id payload)})
                                               (:membership @state)))
                directory/set-membership-actor-id! (with-default-db-two-args (fn [_ _ actor]
                                                     (swap! state update :effects conj :actor)
                                                     (swap! state assoc-in [:membership :actor_id] actor)))]
    (await (run!))))

(defn- ^:async with-authority-writes! [state contracts run!]
  (with-redefs [roles/set-membership-roles! (fn [_ _ opts] (apply-roles! state opts))
                roles/set-membership-actor-id! (fn [_ _ actor]
                                                 (swap! state update :effects conj :actor)
                                                 (swap! state assoc-in [:membership :actor_id] actor))
                roles/set-membership-tool-policies! (fn [_ _ policies]
                                                      (swap! state update :effects conj :tools)
                                                      (swap! state assoc :tool-policies policies))
                support/upsert-actor-contract-best-effort! (fn [payload] (actors/upsert-actor-contract! contracts payload))]
    (await (run!))))

(defn- ^:async fixture! [run!]
  (let [state (atom initial-state) contracts (disk/temp-directory!)]
    (try
      (actors/upsert-actor-contract! contracts
                                    {:actor-id "actor-a" :email "alice@example.test" :org-slug "tenant-a"
                                     :display-name "Alice" :role-slugs ["basic-user"]})
      (await (with-read-ports! state contracts
               #(with-directory-writes! state
                  (fn [] (with-authority-writes! state contracts
                           (fn [] (run! {:state state :contracts contracts})))))))
      (catch :default error (is false (str "Unexpected policy mutation fixture failure: " error)))
      (finally (fs/remove-tree! contracts)))))

(defn- ^:async refusal [operation]
  (try (await (operation)) nil (catch :default error (ex-data error))))

(deftest ^:async explicit-empty-authority-is-cleared-and-stays-cleared-after-contract-reprojection
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (is (= {:ok true} (await (users/update-user-actor! nil nil nil "user-a"
                                                             {:org-id "org-a" :role-slugs [] :tool-policies []}))))
            (is (= [] (:role-ids @state)))
            (is (= [] (:tool-policies @state)))
            (let [contract (:actor (actors/find-actor-contract-by-id contracts "actor-a"))]
              (is (= [] (:actor/roles contract)))
              (await (projection/project-actor-via-store! nil {:id "org-a"} contract))
              (is (= [] (:role-ids @state))))))))

(deftest ^:async omitted-authority-fields-preserve-existing-membership
  (await (fixture!
          (^:async fn [{:keys [state]}]
            (await (users/update-user-actor! nil nil nil "user-a" {:org-id "org-a"}))
            (is (= ["role-basic"] (:role-ids @state)))
            (is (= (:tool-policies initial-state) (:tool-policies @state)))
            (is (= "actor-a" (get-in @state [:membership :actor_id])))
            (is (not-any? #{:roles :tools :actor} (:effects @state)))))))

(deftest ^:async mongo-unsupported-profile-fields-refuse-before-any-authority-write
  (await (fixture!
          (^:async fn [{:keys [state]}]
            (doseq [field [:status :membership-status :display-name]]
              (let [result (await (refusal #(users/update-user-actor! nil nil nil "user-a"
                                                                     {:org-id "org-a" field "disabled" :role-slugs []})))]
                (is (= 400 (:status result)))
                (is (= "mongo_policy_profile_update_unsupported" (:code result)))))
            (is (= [] (:effects @state)))
            (is (= ["role-basic"] (:role-ids @state)))))))

(deftest ^:async unresolved-explicit-actor-organization-creates-no-directory-state
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (let [contract (assoc (:actor (actors/find-actor-contract-by-id contracts "actor-a")) :actor/org "missing")
                  result (await (refusal #(projection/project-actor-via-store! nil {:id "org-a"} contract)))]
              (is (= 404 (:status result)))
              (is (= "actor_projection_org_not_found" (:code result)))
              (is (= [] (:effects @state))))))))

(deftest ^:async omitted-actor-organization-can-use-the-explicit-primary
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (let [contract (dissoc (:actor (actors/find-actor-contract-by-id contracts "actor-a")) :actor/org)]
              (await (projection/project-actor-via-store! nil {:id "org-a"} contract))
              (is (= "org-a" (get-in @state [:membership :org_id])))
              (is (= "actor-a" (get-in @state [:membership :actor_id]))))))))

(deftest ^:async role-id-creation-persists-effective-contract-roles
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (await (users/create-user! nil nil nil {:email "alice@example.test" :org-id "org-a"
                                                   :actor-id "actor-a" :role-ids ["role-admin"]}))
            (is (= ["role-admin"] (:role-ids @state)))
            (is (= [:role/system-admin] (get-in (actors/find-actor-contract-by-id contracts "actor-a") [:actor :actor/roles])))))))

(deftest ^:async additive-role-update-persists-the-complete-effective-role-set
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (await (users/set-membership-roles-public! nil nil nil "member-a" {:role-ids ["role-admin"] :replace false}))
            (is (= #{"role-basic" "role-admin"} (set (:role-ids @state))))
            (is (= #{:role/basic-user :role/system-admin}
                   (set (get-in (actors/find-actor-contract-by-id contracts "actor-a") [:actor :actor/roles]))))))))

(deftest ^:async conflicting-actor-updates-refuse-before-any-authority-or-contract-write
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (let [before (actors/find-actor-contract-by-id contracts "actor-a")]
              (doseq [operation [#(users/update-user-actor! nil nil nil "user-a"
                                    {:org-id "org-a" :actor-id "actor-b" :role-slugs ["system-admin"]
                                     :tool-policies []})
                                 #(users/set-membership-roles-public! nil nil nil "member-a"
                                    {:actor-id "actor-b" :role-slugs ["system-admin"]})
                                 #(users/create-user! nil nil nil
                                    {:email "alice@example.test" :org-id "org-a" :actor-id "actor-b"
                                     :display-name "Changed" :status "disabled" :role-slugs ["system-admin"]})]]
                (is (= 409 (:status (await (refusal operation)))))
                (is (= [] (:effects @state)))
                (is (= before (actors/find-actor-contract-by-id contracts "actor-a")))
                (is (nil? (actors/find-actor-contract-by-id contracts "actor-b"))))
              (is (= "actor-a" (get-in @state [:membership :actor_id])))
              (is (= ["role-basic"] (:role-ids @state))))))))

(deftest ^:async same-normalized-actor-and-initial-assignment-remain-supported
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (await (users/update-user-actor! nil nil nil "user-a"
                                            {:org-id "org-a" :actor-id " actor-a " :role-slugs []}))
            (is (= "actor-a" (get-in @state [:membership :actor_id])))
            (is (= [] (:role-ids @state)))
            (swap! state assoc-in [:membership :actor_id] nil)
            (await (users/update-user-actor! nil nil nil "user-a"
                                            {:org-id "org-a" :actor-id "actor-b" :role-slugs ["system-admin"]}))
            (is (= "actor-b" (get-in @state [:membership :actor_id])))
            (is (= [:role/system-admin] (get-in (actors/find-actor-contract-by-id contracts "actor-b") [:actor :actor/roles])))))))

(deftest ^:async initial-assignment-race-refuses-before-dependent-writes
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (swap! state assoc-in [:membership :actor_id] nil)
            (with-redefs [roles/set-membership-actor-id! (fn [_ _ _] (throw (ex-info "Other claimant won" {:status 409})))]
              (doseq [operation [#(users/update-user-actor! nil nil nil "user-a"
                                    {:org-id "org-a" :actor-id "actor-b" :role-slugs [] :tool-policies []})
                                 #(users/set-membership-roles-public! nil nil nil "member-a"
                                    {:actor-id "actor-b" :role-slugs []})
                                 #(users/create-user! nil nil nil
                                    {:email "alice@example.test" :org-id "org-a" :actor-id "actor-b" :display-name "Changed"})]]
                (is (= 409 (:status (await (refusal operation)))))
                (is (= [] (:effects @state)))
                (is (nil? (actors/find-actor-contract-by-id contracts "actor-b"))))
              (is (= ["role-basic"] (:role-ids @state))))))))

(deftest ^:async create-existing-member-without-actor-retains-the-assigned-coordinate
  (await (fixture!
          (^:async fn [{:keys [state]}]
            (await (users/create-user! nil nil nil {:email "alice@example.test" :org-id "org-a"
                                                   :role-ids ["role-basic"]}))
            (is (= "actor-a" (get-in @state [:membership :actor_id])))))))

(deftest ^:async full-projection-preflights-all-contracts-before-user-or-role-updates
  (await (fixture!
          (^:async fn [{:keys [state contracts]}]
            (let [original (:actor (actors/find-actor-contract-by-id contracts "actor-a"))
                  changed (assoc original :actor/roles [:role/system-admin])
                  historical (assoc changed :actor/id "actor-old")]
              (is (= 409 (:status (await (refusal #(projection/sync-actor-projections! nil {:id "org-a"}
                                                                                     [changed historical]))))))
              (is (= [] (:effects @state)))
              (is (= ["role-basic"] (:role-ids @state)))
              (is (= "actor-a" (get-in @state [:membership :actor_id]))))))))

(deftest ^:async omitted-contract-roles-preserve-the-existing-declaration
  (await (fixture!
          (^:async fn [{:keys [contracts]}]
            (actors/upsert-actor-contract! contracts {:actor-id "actor-a" :display-name "Renamed"})
            (is (= [:role/basic-user] (get-in (actors/find-actor-contract-by-id contracts "actor-a") [:actor :actor/roles])))
            (actors/upsert-actor-contract! contracts {:actor-id "actor-a" :role-slugs nil})
            (is (= [:role/basic-user] (get-in (actors/find-actor-contract-by-id contracts "actor-a") [:actor :actor/roles])))))))
