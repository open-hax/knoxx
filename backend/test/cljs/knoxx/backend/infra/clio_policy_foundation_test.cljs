(ns knoxx.backend.infra.clio-policy-foundation-test
  "Direct real-ledger policy admission and replay, without production identity composition."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as fixture]
            [knoxx.backend.infra.clio-application-store :as application]
            [knoxx.backend.infra.clio-policy-store :as policy]))

(def ^:private principal
  {:principal/id "operator" :principal/entity-id "operator-entity" :principal/kind :human
   :principal/roles ["system-admin"] :principal/capabilities []})

(def ^:private operator-binding
  {:principal-id "operator" :entity-id "operator-entity" :kind :human
   :user-id "operator-user" :membership-id "operator-member" :org-id "home" :actor-id "operator"})

(def ^:private actor
  {:principal principal :user-id "operator-user" :membership-id "operator-member"})

(def ^:private catalog
  {:org {:id "home" :slug "home" :name "Home" :status "active"}
   :roles {"system-admin" {:name "Administrator" :permissions [] :tool-policies []}}
   :tools [{:id "example_read"}]})

(defn- command [id operation args]
  {:id id :at "2026-09-20T12:00:00.000Z" :actor actor :operation operation :args args})

(defn- ^:async failure [run!]
  (try (await (run!)) nil (catch :default cause (ex-data cause))))

(defn- ^:async with-policy! [run!]
  (let [directory (fixture/temp-directory!)
        store (policy/open! {:directory directory})]
    (try
      (await (policy/initialize! store catalog))
      (await (policy/observe! store operator-binding principal))
      (await (run! store directory))
      (finally (fs/remove-tree! directory)))))

(deftest ^:async admitted-command-retry-and-reopen-preserve-first-result
  (await (with-policy!
          (^:async fn [store directory]
            (let [request (command "create-editorial" :policy/create-org [{:name "Editorial"}])
                  accepted (await (policy/command! store request))
                  before (application/history (:engine store))]
              (is (= "org_create-editorial" (get-in accepted [:org :id])))
              (is (= ["home" "org_create-editorial"]
                     (mapv :id (:orgs (await (policy/authorized-read! store actor :policy/orgs []))))))
              (is (= accepted (await (policy/command! store (assoc request :at "2026-09-21T12:00:00.000Z")))))
              (is (= before (application/history (:engine store))))
              (is (= 409 (:status (await (failure #(policy/command! store (assoc request :args [{:name "Changed"}])))))))
              (is (= before (application/history (:engine store))))
              (let [reopened (policy/open! {:directory directory})]
                (is (= (await (policy/read! store :policy/state []))
                       (await (policy/read! reopened :policy/state []))))
                (is (= accepted (await (policy/command! reopened request))))
                (is (= before (application/history (:engine reopened))))))))))

(deftest ^:async initialization-and-observation-cannot-restore-revoked-authority
  (await (with-policy!
          (^:async fn [store directory]
            (let [earlier (command "accepted-first" :policy/create-org [{:name "Earlier"}])]
              (await (policy/command! store earlier))
              (await (policy/command! store (command "revoke" :policy/membership-roles
                                                     ["operator-member" {:org-id "home" :role-ids [] :replace true}])))
              (let [before (application/history (:engine store))]
                (is (= 403 (:status (await (failure #(policy/command! store earlier))))))
                (is (= 403 (:status (await (failure #(policy/authorized-read! store actor :policy/orgs []))))))
                (is (= before (application/history (:engine store)))))
              (await (policy/initialize! store catalog))
              (await (policy/observe! store operator-binding principal))
              (let [reopened (policy/open! {:directory directory})
                    state (await (policy/read! reopened :policy/state []))]
                (is (= [] (get-in state [:memberships "operator-member" :role-ids])))
                (is (= 403 (:status (await (failure #(policy/authorized-read! reopened actor :policy/orgs []))))))))))))

(deftest ^:async invalid-identity-and-unauthorized-calls-append-no-facts
  (await (with-policy!
          (^:async fn [store _directory]
            (let [before (application/history (:engine store))]
              (is (= 403 (:status (await (failure #(policy/observe! store (assoc operator-binding :entity-id "foreign") principal))))))
              (is (= 400 (:status (await (failure #(policy/command! store (dissoc (command "bad" :policy/create-org [{:name "Bad"}]) :id)))))))
              (is (= 400 (:status (await (failure #(policy/authorized-read! store nil :policy/orgs []))))))
              (is (= 403 (:status (await (failure #(policy/authorized-read! store actor :policy/state []))))))
              (is (= before (application/history (:engine store)))))))))
