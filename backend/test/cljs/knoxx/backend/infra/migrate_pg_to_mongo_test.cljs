(ns knoxx.backend.infra.migrate-pg-to-mongo-test
  "Tests for the PG→ Mongo migration script row adapters.
   Only tests pure data transformation functions (row->doc), not the
   actual migration engine which requires live PG + Mongo connections."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.migrate-pg-to-mongo :as migrate]))

(deftest row->org-doc-test
  (testing "converts PG org row to Mongo document"
    (let [doc (migrate/row->org-doc {:id #uuid "00000000-0000-0000-0000-000000000001"
                                     :slug "acme" :name "Acme" :kind "customer"
                                     :is_primary true :status "active"
                                     :created_at "2025-01-01" :updated_at "2025-01-02"})]
      (is (= "00000000-0000-0000-0000-000000000001" (:org_id doc)))
      (is (= "acme" (:slug doc)))
      (is (= true (:is_primary doc))))))

(deftest row->user-doc-test
  (testing "converts PG user row to Mongo document"
    (let [doc (migrate/row->user-doc {:id #uuid "00000000-0000-0000-0000-000000000002"
                                      :email "a@b.com" :display_name "A"
                                      :auth_provider "github" :external_subject nil
                                      :status "active" :created_at "2025-01-01" :updated_at "2025-01-02"})]
      (is (= "00000000-0000-0000-0000-000000000002" (:user_id doc)))
      (is (= "a@b.com" (:email doc)))
      (is (nil? (:external_subject doc))))))

(deftest row->membership-doc-test
  (testing "converts PG membership row, stringifies FKs"
    (let [doc (migrate/row->membership-doc {:id #uuid "00000000-0000-0000-0000-000000000003"
                                            :user_id #uuid "00000000-0000-0000-0000-000000000002"
                                            :org_id #uuid "00000000-0000-0000-0000-000000000001"
                                            :actor_id "actor-1" :status "active"
                                            :is_default true :created_at "2025-01-01" :updated_at "2025-01-02"})]
      (is (string? (:user_id doc)) "user_id is string")
      (is (string? (:org_id doc)) "org_id is string")
      (is (= "actor-1" (:actor_id doc))))))

(deftest row->role-doc-test
  (testing "converts PG role row, handles nullable org_id"
    (let [doc (migrate/row->role-doc {:id #uuid "00000000-0000-0000-0000-000000000004"
                                      :org_id nil :name "Admin" :slug "admin"
                                      :scope_kind "platform" :built_in true
                                      :system_managed false :created_at "2025-01-01" :updated_at "2025-01-02"})]
      (is (nil? (:org_id doc)) "nullable org_id preserved")
      (is (= "admin" (:slug doc))))))

(deftest row->role-permission-doc-test
  (testing "converts PG role_permission row"
    (let [doc (migrate/row->role-permission-doc {:role_id #uuid "00000000-0000-0000-0000-000000000004"
                                                  :permission_code "agent.chat.use"
                                                  :effect "allow"})]
      (is (string? (:role_id doc)))
      (is (= "agent.chat.use" (:permission_code doc)))
      (is (= "allow" (:effect doc))))))

(deftest row->tool-definition-doc-test
  (testing "converts PG tool_definitions row (TEXT PK)"
    (let [doc (migrate/row->tool-definition-doc {:id "web-search"
                                                  :label "Web Search"
                                                  :description "Search the web"
                                                  :risk_level "medium"})]
      (is (= "web-search" (:tool_id doc)) "TEXT id preserved as-is"))))

(deftest row->actor-credential-doc-test
  (testing "converts PG actor_credentials row, parses secret_json"
    (let [doc (migrate/row->actor-credential-doc
               {:id #uuid "00000000-0000-0000-0000-000000000005"
                :user_id #uuid "00000000-0000-0000-0000-000000000002"
                :org_id #uuid "00000000-0000-0000-0000-000000000001"
                :provider "github" :kind "credential"
                :account_identifier "user@example.com"
                :secret_json "{\"token\":\"abc\"}"
                :status "active" :created_at "2025-01-01" :updated_at "2025-01-02"})]
      (is (= "00000000-0000-0000-0000-000000000005" (:credential_id doc)))
      (is (= {:token "abc"} (:secret_json doc)) "JSONB parsed to map"))))

(deftest row->invite-doc-test
  (testing "converts PG invites row, parses role_slugs"
    (let [doc (migrate/row->invite-doc
               {:id #uuid "00000000-0000-0000-0000-000000000006"
                :org_id #uuid "00000000-0000-0000-0000-000000000001"
                :code "abc123" :email "a@b.com"
                :inviter_membership_id #uuid "00000000-0000-0000-0000-000000000003"
                :role_slugs "[\"basic-user\"]" :status "pending"
                :redeemed_by nil :redeemed_at nil
                :expires_at "2025-01-08" :created_at "2025-01-01"})]
      (is (= "00000000-0000-0000-0000-000000000006" (:invite_id doc)))
      (is (= ["basic-user"] (:role_slugs doc)) "JSONB parsed to vec")
      (is (nil? (:redeemed_by doc)) "nullable FK preserved"))))

(deftest row->config-doc-test
  (testing "converts PG knoxx_config row (TEXT PK)"
    (let [doc (migrate/row->config-doc {:key "session_secret" :value "xyz" :updated_at "2025-01-01"})]
      (is (= "session_secret" (:key doc)))
      (is (= "xyz" (:value doc))))))

(deftest row->studio-audio-asset-doc-test
  (testing "converts PG studio_audio_assets row, preserves binary data"
    (let [buf (js/Buffer.from "fake-image")]
      (let [doc (migrate/row->studio-audio-asset-doc
                 {:id #uuid "00000000-0000-0000-0000-000000000007"
                  :audio_path "/audio/song.mp3" :asset_type "waveform"
                  :image_data buf :mime_type "image/png"
                  :width 800 :height 200 :created_at "2025-01-01"})]
        (is (= buf (:image_data doc)) "binary data preserved as Buffer")
        (is (= 800 (:width doc)))))))

(deftest row->mailbox-entry-doc-test
  (testing "converts PG actor_mailbox_entries row, parses JSONB fields"
    (let [doc (migrate/row->mailbox-entry-doc
               {:id #uuid "00000000-0000-0000-0000-000000000008"
                :kind "actor-message" :status "pending"
                :source_actor_id "src-1" :source_session_id nil
                :source_conversation_id nil :source_run_id nil
                :source_json "{\"event\":\"test\"}" :target_kind "actor"
                :target_actor_id "tgt-1" :target_session_id nil
                :target_conversation_id nil :target_run_id nil
                :target_json "{}" :delivery_mode "follow-up"
                :attempts 0 :next_at nil :expires_at nil
                :delivered_at nil :acknowledged_at nil
                :content_ref_json "{}" :metadata_json "{}"
                :preview "Hello" :last_error nil
                :created_at "2025-01-01" :updated_at "2025-01-01"})]
      (is (= {:event "test"} (:source_json doc)) "source_json parsed")
      (is (= {} (:target_json doc)) "empty JSONB → empty map")
      (is (= "Hello" (:preview doc))))))

(deftest table-specs-count-test
  (testing "table-specs covers all 19 PG tables"
    (is (= 19 (count migrate/table-specs))
        "Expected 19 table migration specs")))

(deftest parse-jsonb-edge-cases-test
  (testing "parse-jsonb handles nil, empty, nested"
    (is (nil? (#'migrate/parse-jsonb nil)))
    (is (= {} (#'migrate/parse-jsonb "{}")))
    (is (= {:a 1} (#'migrate/parse-jsonb "{\"a\":1}")))
    (is (= {:a {:b [1 2]}} (#'migrate/parse-jsonb "{\"a\":{\"b\":[1,2]}}")))))
