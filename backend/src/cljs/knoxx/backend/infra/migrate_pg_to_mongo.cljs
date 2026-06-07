(ns knoxx.backend.infra.migrate-pg-to-mongo
  "One-time PG → Mongo migration script for all 19 policy tables.

   Usage:
     shadow-cljs compile migrate
     node dist/migrate.js

   Or via npm:
     npm run migrate

   Environment variables:
     KNOXX_POLICY_DATABASE_URL (or DATABASE_URL) — PG connection string
     MONGODB_URI (or OPENPLANNER_MONGODB_URI) — Mongo connection string
     MONGODB_DB (or OPENPLANNER_MONGODB_DB) — Mongo database name

   Idempotent: can re-run safely. Uses upsert (updateOne with upsert: true)
   on every document. Validates row counts after each table migration."
  (:require
    [clojure.string :as str]
    [knoxx.backend.extern.pg :as pg]
    [knoxx.backend.infra.mongo-client :as mongo]))

;; ---------------------------------------------------------------------------
;; Table definitions: PG table → Mongo collection + document adapter
;; ---------------------------------------------------------------------------

(defn- parse-jsonb
  "Parse a JSONB value from PG into a CLJS map."
  [v]
  (when v
    (if (string? v)
      (try (js->clj (js/JSON.parse v) :keywordize-keys true) (catch :default _ {}))
      (js->clj v :keywordize-keys true))))

(defn row->org-doc
  [{:keys [id slug name kind is_primary status created_at updated_at]}]
  {:org_id (str id) :slug slug :name name :kind kind
   :is_primary is_primary :status status
   :created_at created_at :updated_at updated_at})

(defn row->user-doc
  [{:keys [id email display_name auth_provider external_subject status created_at updated_at]}]
  {:user_id (str id) :email email :display_name display_name
   :auth_provider auth_provider :external_subject external_subject
   :status status :created_at created_at :updated_at updated_at})

(defn row->membership-doc
  [{:keys [id user_id org_id actor_id status is_default created_at updated_at]}]
  {:membership_id (str id) :user_id (str user_id) :org_id (str org_id)
   :actor_id actor_id :status status :is_default is_default
   :created_at created_at :updated_at updated_at})

(defn row->role-doc
  [{:keys [id org_id name slug scope_kind built_in system_managed created_at updated_at]}]
  {:role_id (str id) :org_id (when org_id (str org_id)) :name name :slug slug
   :scope_kind scope_kind :built_in built_in :system_managed system_managed
   :created_at created_at :updated_at updated_at})

(defn row->role-permission-doc
  [{:keys [role_id permission_code effect]}]
  {:role_id (str role_id) :permission_code permission_code :effect effect})

(defn row->membership-role-doc
  [{:keys [membership_id role_id]}]
  {:membership_id (str membership_id) :role_id (str role_id)})

(defn row->tool-definition-doc
  [{:keys [id label description risk_level]}]
  {:tool_id id :label label :description description :risk_level risk_level})

(defn row->role-tool-policy-doc
  [{:keys [role_id tool_id effect constraints_json]}]
  {:role_id (str role_id) :tool_id tool_id :effect effect
   :constraints_json (parse-jsonb constraints_json)})

(defn row->user-tool-policy-doc
  [{:keys [membership_id tool_id effect constraints_json]}]
  {:membership_id (str membership_id) :tool_id tool_id :effect effect
   :constraints_json (parse-jsonb constraints_json)})

(defn row->actor-credential-doc
  [{:keys [id user_id org_id provider kind account_identifier secret_json status created_at updated_at]}]
  {:credential_id (str id) :user_id (str user_id) :org_id (str org_id)
   :provider provider :kind kind :account_identifier account_identifier
   :secret_json (parse-jsonb secret_json) :status status
   :created_at created_at :updated_at updated_at})

(defn row->data-lake-doc
  [{:keys [id org_id name slug kind config_json status created_at updated_at]}]
  {:lake_id (str id) :org_id (str org_id) :name name :slug slug :kind kind
   :config_json (parse-jsonb config_json) :status status
   :created_at created_at :updated_at updated_at})

(defn row->audit-event-doc
  [{:keys [id actor_user_id actor_membership_id org_id action resource_kind resource_id before_json after_json created_at]}]
  {:audit_id (str id) :actor_user_id (when actor_user_id (str actor_user_id))
   :actor_membership_id (when actor_membership_id (str actor_membership_id))
   :org_id (when org_id (str org_id))
   :action action :resource_kind resource_kind :resource_id resource_id
   :before_json (parse-jsonb before_json) :after_json (parse-jsonb after_json)
   :created_at created_at})

(defn row->session-doc
  [{:keys [id user_id membership_id org_id token_hash token_prefix salt email display_name
           auth_provider external_subject ip_address user_agent expires_at last_seen_at created_at]}]
  {:session_id (str id) :user_id (str user_id) :membership_id (str membership_id)
   :org_id (str org_id) :token_hash token_hash :token_prefix token_prefix :salt salt
   :email email :display_name display_name :auth_provider auth_provider
   :external_subject external_subject :ip_address ip_address :user_agent user_agent
   :expires_at expires_at :last_seen_at last_seen_at :created_at created_at})

(defn row->invite-doc
  [{:keys [id org_id code email inviter_membership_id role_slugs status redeemed_by redeemed_at expires_at created_at]}]
  {:invite_id (str id) :org_id (str org_id) :code code :email email
   :inviter_membership_id (when inviter_membership_id (str inviter_membership_id))
   :role_slugs (parse-jsonb role_slugs) :status status
   :redeemed_by (when redeemed_by (str redeemed_by)) :redeemed_at redeemed_at
   :expires_at expires_at :created_at created_at})

(defn row->config-doc
  [{:keys [key value updated_at]}]
  {:key key :value value :updated_at updated_at})

(defn row->mailbox-entry-doc
  [{:keys [id kind status source_actor_id source_session_id source_conversation_id source_run_id
           source_json target_kind target_actor_id target_session_id target_conversation_id target_run_id
           target_json delivery_mode attempts next_at expires_at delivered_at acknowledged_at
           content_ref_json metadata_json preview last_error created_at updated_at]}]
  {:entry_id (str id) :kind kind :status status
   :source_actor_id source_actor_id :source_session_id source_session_id
   :source_conversation_id source_conversation_id :source_run_id source_run_id
   :source_json (parse-jsonb source_json) :target_kind target_kind
   :target_actor_id target_actor_id :target_session_id target_session_id
   :target_conversation_id target_conversation_id :target_run_id target_run_id
   :target_json (parse-jsonb target_json) :delivery_mode delivery_mode
   :attempts attempts :next_at next_at :expires_at expires_at
   :delivered_at delivered_at :acknowledged_at acknowledged_at
   :content_ref_json (parse-jsonb content_ref_json)
   :metadata_json (parse-jsonb metadata_json)
   :preview preview :last_error last_error
   :created_at created_at :updated_at updated_at})

(defn row->mailbox-route-doc
  [{:keys [actor_id conversation_id session_id run_id contract_id status source_json expires_at last_seen_at created_at]}]
  {:actor_id actor_id :conversation_id conversation_id :session_id session_id
   :run_id run_id :contract_id contract_id :status status
   :source_json (parse-jsonb source_json) :expires_at expires_at
   :last_seen_at last_seen_at :created_at created_at})

(defn row->studio-state-doc
  [{:keys [id user_id org_id kind state_json created_at updated_at]}]
  {:state_id (str id) :user_id (str user_id) :org_id (str org_id) :kind kind
   :state_json (parse-jsonb state_json) :created_at created_at :updated_at updated_at})

(defn row->studio-audio-asset-doc
  [{:keys [id audio_path asset_type image_data mime_type width height created_at]}]
  {:asset_id (str id) :audio_path audio_path :asset_type asset_type
   :image_data image_data :mime_type mime_type :width width :height height
   :created_at created_at})

;; ---------------------------------------------------------------------------
;; Table migration specs
;; ---------------------------------------------------------------------------

(def table-specs
  "Ordered list of [pg-table mongo-collection row->doc-fn unique-keys].
   Order respects FK dependencies: referenced tables first."
  [[:orgs                  "knoxx_orgs"                  row->org-doc                  [:org_id]]
   [:users                 "knoxx_users"                 row->user-doc                 [:user_id]]
   [:memberships           "knoxx_memberships"           row->membership-doc           [:membership_id]]
   [:roles                 "knoxx_roles"                 row->role-doc                 [:role_id]]
   [:role_permissions      "knoxx_role_permissions"      row->role-permission-doc      [:role_id :permission_code]]
   [:membership_roles      "knoxx_membership_roles"      row->membership-role-doc      [:membership_id :role_id]]
   [:tool_definitions      "knoxx_tool_definitions"      row->tool-definition-doc      [:tool_id]]
   [:role_tool_policies    "knoxx_role_tool_policies"    row->role-tool-policy-doc     [:role_id :tool_id]]
   [:user_tool_policies    "knoxx_user_tool_policies"    row->user-tool-policy-doc     [:membership_id :tool_id]]
   [:actor_credentials     "knoxx_actor_credentials"     row->actor-credential-doc     [:credential_id]]
   [:data_lakes            "knoxx_data_lakes"            row->data-lake-doc            [:lake_id]]
   [:audit_events          "knoxx_audit_events"          row->audit-event-doc          [:audit_id]]
   [:sessions              "knoxx_sessions"              row->session-doc              [:session_id]]
   [:invites               "knoxx_invites"               row->invite-doc               [:invite_id]]
   [:knoxx_config          "knoxx_config"                row->config-doc               [:key]]
   [:actor_mailbox_entries "knoxx_actor_mailbox_entries" row->mailbox-entry-doc        [:entry_id]]
   [:actor_mailbox_routes  "knoxx_actor_mailbox_routes"  row->mailbox-route-doc        [:actor_id]]
   [:studio_state          "knoxx_studio_state"          row->studio-state-doc         [:state_id]]
   [:studio_audio_assets   "knoxx_studio_audio_assets"   row->studio-audio-asset-doc   [:asset_id]]])

;; ---------------------------------------------------------------------------
;; Migration engine
;; ---------------------------------------------------------------------------

(defn- ^:async pg-count!
  "Return row count for a PG table."
  [pool table-name]
  (let [{:keys [rows]} (await (pg/query! pool (str "SELECT COUNT(*) AS cnt FROM " (name table-name)) []))]
    (:cnt (first rows))))

(defn- ^:async mongo-count!
  "Return document count for a Mongo collection."
  [db coll-name]
  (await (.countDocuments (.collection db coll-name))))

(defn- ^:async migrate-table!
  "Migrate one table: read all PG rows, upsert into Mongo, return stats."
  [pool db [pg-table mongo-coll row->doc unique-keys]]
  (let [start-ms (js/Date.now)]
    (js/console.log (str "  [" pg-table "] reading from PG..."))
    (await
     (-> (pg/query! pool (str "SELECT * FROM " (name pg-table)) [])
         (.then
          (fn [{:keys [rows]}]
            (let [pg-count (count rows)
                  docs (mapv row->doc rows)]
              (js/console.log (str "  [" pg-table "] " pg-count " rows → " mongo-coll))
              (-> (.bulkWrite (.collection db mongo-coll)
                              (clj->js
                               (mapv (fn [doc]
                                       (let [filter (select-keys doc unique-keys)]
                                         {:updateOne
                                          {:filter filter
                                           :update {"$set" (dissoc doc :_id)}
                                           :upsert true}}))
                                     docs))
                              #js {"ordered" false})
                  (.then (fn [result]
                           (let [elapsed-ms (- (js/Date.now) start-ms)]
                             (js/console.log (str "  [" pg-table "] done in " elapsed-ms "ms — "
                                                  "upserted:" (.-upsertedCount result)
                                                  " modified:" (.-modifiedCount result)))
                             {:table pg-table
                              :collection mongo-coll
                              :pg-count pg-count
                              :mongo-count nil  ; validated separately
                              :elapsed-ms elapsed-ms
                              :upserted (.-upsertedCount result)
                              :modified (.-modifiedCount result)})))))))))))

(defn- ^:async validate-counts!
  "Validate PG row count matches Mongo doc count for each table."
  [pool db specs]
  (js/console.log "\n=== Count Validation ===")
  (await
   (-> (js/Promise.all
        (into-array
         (mapv (fn [[pg-table mongo-coll _row->doc _unique-keys]]
                 (-> (js/Promise.all #js [(pg-count! pool pg-table)
                                          (mongo-count! db mongo-coll)])
                     (.then (fn [counts]
                              (let [pg-cnt (aget counts 0)
                                    mongo-cnt (aget counts 1)
                                    match? (= pg-cnt mongo-cnt)]
                                (js/console.log (str "  " pg-table " → " mongo-coll
                                                     ": PG=" pg-cnt " Mongo=" mongo-cnt
                                                     (if match? " ✓" " ✗ MISMATCH")))
                                {:table pg-table :collection mongo-coll
                                 :pg-count pg-cnt :mongo-count mongo-cnt :match match?})))))
               specs)))
       (.then (fn [results]
                (let [all-match? (every? :match (array-seq results))
                      mismatches (remove :match (array-seq results))]
                  (when (seq mismatches)
                    (js/console.error (str "\n✗ " (count mismatches) " table(s) have count mismatches!"))
                    (doseq [m mismatches]
                      (js/console.error (str "  " (:table m) ": PG=" (:pg-count m)
                                             " Mongo=" (:mongo-count m)))))
                  (if all-match?
                    (js/console.log "\n✓ All table counts match!")
                    (js/console.error "\n✗ Count validation failed!"))
                  {:all-match all-match?
                   :mismatches (vec mismatches)
                   :results (vec (array-seq results))}))))))

;; ---------------------------------------------------------------------------
;; Entry point
;; ---------------------------------------------------------------------------

(defn- resolve-pg-url []
  (or (aget js/process.env "KNOXX_POLICY_DATABASE_URL")
      (aget js/process.env "DATABASE_URL")
      (do (js/console.error "Migration requires KNOXX_POLICY_DATABASE_URL or DATABASE_URL")
          (js/process.exit 1))))

(defn ^:async run-migration!
  "Run the full PG→Mongo migration. Returns a result map."
  []
  (js/console.log "=== PG → Mongo Migration ===")
  (js/console.log (str "Time: " (js/Date.)))
  (let [pg-url (resolve-pg-url)
        pool (pg/create-pool! {:connection-string pg-url :max 2})
        _ (js/console.log (str "PG: " (str/replace pg-url #"://[^@]+@" "://***@")))
        db (await (mongo/init-mongo!))]
    (if-not db
      (do (js/console.error "Failed to connect to MongoDB")
          (js/process.exit 1))
      (do
        (js/console.log (str "Mongo: " (or (aget js/process.env "MONGODB_URI")
                                           (aget js/process.env "OPENPLANNER_MONGODB_URI")
                                           "mongodb://localhost:27017")
                              "/" (or (aget js/process.env "MONGODB_DB")
                                      (aget js/process.env "OPENPLANNER_MONGODB_DB")
                                      "openplanner")))
        (js/console.log (str "\nMigrating " (count table-specs) " tables...\n"))
        (let [results (atom [])]
          (doseq [spec table-specs]
            (let [result (await (migrate-table! pool db spec))]
              (swap! results conj result)))
          (let [validation (await (validate-counts! pool db table-specs))
                summary {:tables (count table-specs)
                         :total-pg-rows (reduce + 0 (map :pg-count (:results validation)))
                         :total-mongo-docs (reduce + 0 (map :mongo-count (:results validation)))
                         :all-match (:all-match validation)
                         :mismatches (:mismatches validation)
                         :elapsed-ms (reduce + 0 (map :elapsed-ms @results))}]
             (js/console.log "\n=== Summary ===")
            (js/console.log (str "Tables migrated: " (:tables summary)))
            (js/console.log (str "Total PG rows: " (:total-pg-rows summary)))
            (js/console.log (str "Total Mongo docs: " (:total-mongo-docs summary)))
            (js/console.log (str "Total time: " (:elapsed-ms summary) "ms"))
            (js/console.log (str "All counts match: " (:all-match summary)))
            (pg/end-pool! pool)
            (await (mongo/close-mongo!))
            summary))))))

(defn ^:async -main
  "Entry point for `node dist/migrate.js`."
  []
  (try
    (let [result (await (run-migration!))]
      (if (:all-match result)
        (do (js/console.log "\n✓ Migration complete!")
            (js/process.exit 0))
        (do (js/console.error "\n✗ Migration completed with count mismatches!")
            (js/process.exit 1))))
    (catch :default err
      (js/console.error "\n✗ Migration failed!" err)
      (js/process.exit 1))))
