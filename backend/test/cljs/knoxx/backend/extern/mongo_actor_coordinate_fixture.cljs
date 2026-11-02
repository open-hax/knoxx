(ns knoxx.backend.extern.mongo-actor-coordinate-fixture
  "Own only native actor-coordinate regression seeds and private contract files."
  (:require ["node:fs" :as fs]
            ["node:path" :as path]))

(defn ^:async initial-actor!
  "Seed the fixture membership's initial actor before exercising the guarded production port."
  [db actor]
  (await (.updateOne (.collection db "knoxx_memberships") #js {:membership_id "existing-member"}
                    (clj->js {:$set {:actor_id actor}}))))

(defn ^:async seed-projection-role!
  "Make the historical contract's alternate role real, so a bad projection can overwrite roles."
  [db]
  (await (.insertOne (.collection db "knoxx_roles")
                    #js {:role_id "historical-role" :slug "system-admin" :name "Administrator"
                         :scope_kind "platform"})))

(defn ^:async with-conflicting-contracts!
  "Write two historical declarations for one real membership, restoring environment on exit."
  [directory run!]
  (let [contracts (path/join directory "contracts")
        previous (aget js/process.env "CONTRACTS_DIR")]
    (doseq [folder ["actors" "roles"]] (fs/mkdirSync (path/join contracts folder) #js {:recursive true}))
    (fs/writeFileSync (path/join contracts "roles" "system_admin.edn")
                      (pr-str {:role/id :role/system-admin :role/capabilities [] :role/permissions []}))
    (doseq [actor-id ["existing-local-actor" "stale-actor"]]
      (fs/writeFileSync (path/join contracts "actors" (str actor-id ".edn"))
                        (pr-str {:actor/id actor-id :actor/kind :agent :actor/email "remote@example.test"
                                 :actor/org "existing-org" :actor/roles [:role/system-admin]})))
    (aset js/process.env "CONTRACTS_DIR" contracts)
    (try (await (run!))
         (finally (if (some? previous) (aset js/process.env "CONTRACTS_DIR" previous)
                      (js-delete js/process.env "CONTRACTS_DIR"))))))
