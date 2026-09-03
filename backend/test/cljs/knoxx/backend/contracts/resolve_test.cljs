(ns knoxx.backend.contracts.resolve-test
  (:require [clojure.string :as str]
            [cljs.test :as t :refer [deftest is testing]]
            [knoxx.backend.domain.actor.scope]
            [knoxx.backend.domain.contracts.loader :as loader]
            [knoxx.backend.domain.contracts.resolve :as sut]
            [knoxx.backend.domain.contracts.roles :as roles]))

(deftest actor-extras-test
  (let [actor-spec {:id "test-actor"
                   :kind :user
                   :system-prompt "You are a test agent"
                   :role-slugs [:developer]
                   :custom-field "extra data"
                   :another-field 42}]
    (testing "extracts unknown fields as extras"
      (let [extras (sut/actor-extras actor-spec)]
        (is (= {:custom-field "extra data" :another-field 42} extras))))))

(deftest actor-extras-no-extras-test
  (let [known-only {:id "known-actor"
                   :kind :user
                   :system-prompt "known"}
        extras (sut/actor-extras known-only)]
    (is (nil? extras))))

(deftest actor-extras-nil-test
  (is (nil? (sut/actor-extras nil))))

(deftest actor-extras-all-known-test
  (let [all-known {:id "actor"
                  :kind :user
                  :default-agent "agent-1"
                  :role-slugs [:dev]
                  :capability-ids [:read]
                  :system-prompt "prompt"
                  :task-prompt "task"
                  :thinking-level "high"
                  :model "gpt-5"
                  :contract-id "contract-1"
                  :model-profile "fast"
                  :tool-policies []}
        extras (sut/actor-extras all-known)]
    (is (nil? extras))))

(deftest known-actor-keys-include-test
  (testing "known-actor-keys is a set"
    (is (set? sut/known-actor-keys)))
  (testing "contains expected keys"
    (is (contains? sut/known-actor-keys :id)))
  (is (contains? sut/known-actor-keys :kind))
  (is (contains? sut/known-actor-keys :system-prompt)))

(def empty-fixture-config
  {:contracts-dir "test/fixtures/empty-contracts"})

(deftest resolve-agent-contract-composes-role-actor-and-agent-task-prompts
  (let [contract {:contract/id "agent-a"
                  :contract/kind :agent
                  :contract/actors ["actor-a"]
                  :trigger-kind :manual
                  :agent {:roles [:role/source-role]}
                  :prompts {:task "agent task"}}]
    (with-redefs [loader/find-contract-record-sync (fn [_ class id]
                                                     (case class
                                                       "agents" {:id id :contract contract}
                                                       nil))
                  sut/default-actor-id (fn [_] "actor-a")
                  sut/resolve-actor (fn [_ _]
                                      {:id "actor-a"
                                       :role-slugs []
                                       :capability-ids []
                                       :task-prompt "actor task"})
                  roles/role-task-prompt (fn [_ role]
                                           (when (= "source-role" role)
                                             "role task"))
                  roles/role-system-prompt (fn [_ _] nil)
                  roles/role-tool-ids (fn [_ _] [])]
      (let [resolved (sut/resolve-agent-contract empty-fixture-config "agent-a")
            task (:task-prompt resolved)]
        (is (sequential? task))
        (is (= ["role task" "actor task" "agent task"] (nth task 2)))))))

(deftest resolve-agent-contract-composes-runtime-sources
  (let [source-contract {:contract/id "openplanner-memory"
                         :contract/kind :source
                         :source/id :source/openplanner-memory
                         :source/provider :openplanner
                         :source/hydration {:strategy :memory-search
                                            :mode :triggered
                                            :k 6}}
        agent-contract {:contract/id "agent-a"
                        :contract/kind :agent
                        :contract/actors ["actor-a"]
                        :trigger-kind :manual
                        :agent {:roles [:role/source-role]}
                        :sources [{:source/ref :source/openplanner-memory
                                   :hydration {:mode :always}}]}
        role-contract {:role/id :role/source-role
                       :role/sources [{:source/ref :source/openplanner-memory
                                       :hydration {:k 4}}]}]
    (with-redefs [loader/find-contract-record-sync (fn [_ class id]
                                                     (case class
                                                       "agents" {:id id :contract agent-contract}
                                                       "sources" {:id id :contract source-contract}
                                                       nil))
                  sut/default-actor-id (fn [_] "actor-a")
                  sut/resolve-actor (fn [_ _]
                                      {:id "actor-a"
                                       :role-slugs []
                                       :capability-ids []
                                       :actor {:actor/sources [:source/openplanner-memory]}})
                  roles/role-contract (fn [_ role]
                                        (when (= "source-role" role)
                                          role-contract))
                  roles/role-task-prompt (fn [_ _] nil)
                  roles/role-system-prompt (fn [_ _] nil)
                  roles/role-tool-ids (fn [_ _] [])]
      (let [resolved (sut/resolve-agent-contract empty-fixture-config "agent-a")
            source (first (:sources resolved))]
        (is (= 1 (count (:sources resolved))))
        (is (= :source/openplanner-memory (:source/id source)))
        (is (= {:strategy :memory-search
                :mode :always
                :k 4}
               (:source/hydration source)))))))

(deftest resolve-agent-contract-applies-tool-deny
  (testing ":tool-deny subtracts a role/capability-granted tool without forking the role"
    (let [agent-contract {:contract/id "agent-a"
                          :contract/kind :agent
                          :contract/actors ["actor-a"]
                          :trigger-kind :manual
                          :agent {:roles [:role/creative]}
                          :tool-deny [:workspace_media.attach]}]
      (with-redefs [loader/find-contract-record-sync (fn [_ class id]
                                                       (case class
                                                         "agents" {:id id :contract agent-contract}
                                                         nil))
                    sut/default-actor-id (fn [_] "actor-a")
                    sut/resolve-actor (fn [_ _] {:id "actor-a" :role-slugs [] :capability-ids []})
                    roles/role-system-prompt (fn [_ _] nil)
                    roles/role-task-prompt (fn [_ _] nil)
                    roles/role-tool-ids (fn [_ role]
                                          (when (= "creative" role)
                                            ["discord.send" "voice.tts" "workspace_media.attach"]))]
        (let [resolved (sut/resolve-agent-contract empty-fixture-config "agent-a")]
          (is (= ["discord.send" "voice.tts"] (:tool-ids resolved))
              "denied tool is removed from the granted set")
          (is (not-any? #(= "workspace_media.attach" (:toolId %)) (:tool-policies resolved))
              "denied tool produces no allow policy")
          (is (not (contains? (set (keys (:extras resolved))) :tool-deny))
              ":tool-deny is a known key, not leaked into extras"))))))

(defn- resolve-with-tools
  "Resolve one agent contract against a fixed role grant of three tools."
  ([contract]
   (resolve-with-tools contract empty-fixture-config))
  ([contract config]
   (with-redefs [loader/find-contract-record-sync (fn [_ class id]
                                                    (case class
                                                      "agents" {:id id :contract contract}
                                                      nil))
                 sut/default-actor-id (fn [_] "actor-a")
                 sut/resolve-actor (fn [_ _] {:id "actor-a" :role-slugs [] :capability-ids []})
                 roles/role-system-prompt (fn [_ _] nil)
                 roles/role-task-prompt (fn [_ _] nil)
                 roles/role-tool-ids (fn [_ role]
                                       (when (= "creative" role)
                                         ["discord.send" "graph_query" "save_translation"]))]
     (sut/resolve-agent-contract config (:contract/id contract)))))

(defn- agent-with
  [extra]
  (merge {:contract/id "agent-allow"
          :contract/kind :agent
          :contract/actors ["actor-a"]
          :trigger-kind :manual
          :agent {:roles [:role/creative]}}
         extra))

(deftest resolve-agent-contract-applies-deployment-model-and-thinking-overrides
  (let [contract (agent-with {:agent {:roles [:role/creative]
                                     :model "gemma4:31b"
                                     :thinking :medium}})
        resolved (resolve-with-tools
                  contract
                  (assoc empty-fixture-config
                         :contracts-dir "test/fixtures/model-contracts"
                         :ollama-base-url "http://127.0.0.1:11434"
                         :agent-model-overrides {"agent-allow" "gemma4:e2b"}
                         :agent-thinking-overrides {"agent-allow" "off"}))]
    (is (= "gemma4:e2b" (:model resolved)))
    (is (= "off" (:thinking-level resolved)))
    (is (= "gemma4:31b" (get-in (:contract resolved) [:agent :model]))
        "deployment selection does not rewrite authored contract truth"))
  (let [contract (agent-with {:agent {:roles [:role/creative]
                                     :model "gemma4:31b"
                                     :thinking :medium}})
        resolved (resolve-with-tools
                  contract
                  (assoc empty-fixture-config
                         :agent-model-overrides {"another-agent" "gemma4:e2b"}
                         :agent-thinking-overrides {"another-agent" "off"}))]
    (is (= "gemma4:31b" (:model resolved)))
    (is (= "medium" (:thinking-level resolved)))))

(deftest resolve-publication-post-drafter-runtime-contract
  (let [config {:contracts-dir "test/fixtures/publication-post-drafter-contracts"
                :generated-contracts-dir "test/fixtures/model-contracts"
                :ollama-base-url "http://127.0.0.1:11434"
                :agent-model-overrides {"publication_post_drafter" "gemma4:e2b"}
                :agent-thinking-overrides {"publication_post_drafter" "off"}}
        required-tool-call-shape
        "`{\"title\":\"<concise title>\",\"content\":\"# <title>\n\n<complete Markdown post>\"}`"
        _ (loader/invalidate-sync-contract-cache!)
        resolved (sut/resolve-agent-contract config "publication_post_drafter" "pi")
        system-prompt (:system-prompt resolved)]
    (testing "the resolved runtime exposes only the pinned draft-save tool"
      (is (= ["save_publication_draft"] (:tool-ids resolved)))
      (is (= [{:toolId "save_publication_draft" :effect "allow"}]
             (:tool-policies resolved)))
      (is (= :required-first (:tools-choice resolved))))
    (testing "deployment overrides select the non-thinking local model"
      (is (= "gemma4:e2b" (:model resolved)))
      (is (= "off" (:thinking-level resolved))))
    (testing "the runtime prompt requires the exact tool arguments and an accepted result"
      (is (string? system-prompt))
      (is (str/includes? system-prompt required-tool-call-shape))
      (is (str/includes? system-prompt "Call save_publication_draft exactly once"))
      (is (str/includes? system-prompt
                         "Do not print `title:` or `content:` as an ordinary assistant reply"))
      (is (str/includes? system-prompt "finish without an accepted tool result")))))

(deftest resolve-agent-contract-accepts-generic-only-ollama-provider
  (let [contract (agent-with {:agent {:roles [:role/creative]
                                     :model "gemma4:31b"
                                     :thinking :medium}})
        resolved (resolve-with-tools
                  contract
                  (assoc empty-fixture-config
                         :contracts-dir "test/fixtures/model-contracts"
                         :ollama-base-url ""
                         :provider-base-urls {"ollama" "http://127.0.0.1:11434"}
                         :agent-model-overrides {"agent-allow" "gemma4:e2b"}
                         :agent-thinking-overrides {"agent-allow" "off"}))]
    (is (= "gemma4:e2b" (:model resolved)))
    (is (= "off" (:thinking-level resolved)))))

(deftest resolve-agent-contract-refuses-invalid-deployment-overrides
  (let [contract (agent-with {:agent {:roles [:role/creative]
                                     :model "gemma4:31b"
                                     :thinking :medium}})
        model-config (assoc empty-fixture-config
                            :contracts-dir "test/fixtures/model-contracts"
                            :ollama-base-url "http://127.0.0.1:11434")]
    (testing "an unknown model cannot silently fall through to another provider"
      (is (thrown-with-msg?
           js/Error
           #"unknown agent model override"
           (resolve-with-tools
            contract
            (assoc model-config
                   :agent-model-overrides {"agent-allow" "gemma4:not-installed"})))))
    (testing "a model-specific unsupported thinking level fails closed"
      (is (thrown-with-msg?
           js/Error
           #"unsupported agent thinking override"
           (resolve-with-tools
            contract
            (assoc model-config
                   :agent-model-overrides {"agent-allow" "gemma4:e2b"}
                   :agent-thinking-overrides {"agent-allow" "high"})))))
    (testing "an unrecognized thinking name fails closed"
      (is (thrown-with-msg?
           js/Error
           #"unsupported agent thinking override"
           (resolve-with-tools
            contract
            (assoc model-config
                   :agent-model-overrides {"agent-allow" "gemma4:e2b"}
                   :agent-thinking-overrides {"agent-allow" "turbo"})))))
    (testing "an exact model with no configured provider fails closed"
      (is (thrown-with-msg?
           js/Error
           #"provider is not configured"
           (resolve-with-tools
            contract
            (assoc (dissoc model-config :ollama-base-url)
                   :agent-model-overrides {"agent-allow" "gemma4:e2b"}
                   :agent-thinking-overrides {"agent-allow" "off"})))))))

(deftest resolve-agent-contract-applies-tools-allowed
  (testing "an allowlist narrows the granted set to exactly what it names"
    ;; Previously inert: `domain.policy.tools` reads `:tools/allowed` only from
    ;; :policy contracts, so an agent could declare one and still receive its
    ;; whole role surface. That is how a translation session kept reaching
    ;; graph_query — and through it OpenPlanner REST.
    (let [resolved (resolve-with-tools (agent-with {:tools/allowed ["save_translation"]}))]
      (is (= ["save_translation"] (:tool-ids resolved)))
      (is (= [{:toolId "save_translation" :effect "allow"}] (:tool-policies resolved))
          "the narrowed set is what becomes allow policies")))

  (testing "an EMPTY allowlist is passthrough, not a muzzle"
    ;; The semantics `domain.policy.tools/tool-call-contract-denied` already
    ;; documents. `contracts/agents/broadcast_studio_audio_transcriber.edn`
    ;; ships `:tools/allowed []`, so reading empty as deny-all would silently
    ;; strip every tool from an agent that works today.
    (let [resolved (resolve-with-tools (agent-with {:tools/allowed []}))]
      (is (= ["discord.send" "graph_query" "save_translation"] (:tool-ids resolved)))))

  (testing "no allowlist at all is passthrough"
    (let [resolved (resolve-with-tools (agent-with {}))]
      (is (= ["discord.send" "graph_query" "save_translation"] (:tool-ids resolved)))))

  (testing "a name the roles never granted cannot be allowed into existence"
    (let [resolved (resolve-with-tools (agent-with {:tools/allowed ["save_translation" "bash"]}))]
      (is (= ["save_translation"] (:tool-ids resolved))
          "the allowlist narrows a granted set; it is not itself a grant")))

  (testing "deny wins over allow, so a contract cannot allow back what it denied"
    (let [resolved (resolve-with-tools (agent-with {:tools/allowed ["save_translation" "graph_query"]
                                                    :tool-deny [:graph_query]}))]
      (is (= ["save_translation"] (:tool-ids resolved))))))

(deftest resolve-agent-contract-validates-tools-choice
  (testing "required-first is resolved as executable policy when a tool remains"
    (let [resolved (resolve-with-tools
                    (agent-with {:tools/allowed ["save_translation"]
                                 :tools/choice :required-first}))]
      (is (= :required-first (:tools-choice resolved)))
      (is (not (contains? (:extras resolved) :tools/choice)))))

  (testing "required-first fails closed when the resolved tool surface is empty"
    (is (thrown-with-msg?
         js/Error
         #"required-first agent contract exposes no tools"
         (resolve-with-tools
          (agent-with {:tools/allowed ["not-granted"]
                       :tools/choice :required-first})))))

  (testing "unsupported choices are rejected instead of silently ignored"
    (is (thrown-with-msg?
         js/Error
         #"unsupported agent tools choice"
         (resolve-with-tools
          (agent-with {:tools/choice :always}))))))

(deftest agent-catalog-treats-missing-trigger-kind-as-manual
  (let [agent-contract {:contract/id "knoxx_default"
                        :contract/kind :agent
                        :contract/actors ["chat_primary"]
                        :agent {:role :role/knowledge_worker}
                        :prompts {:system "agent prompt"}}]
    (with-redefs [loader/load-all-contracts-sync (fn [_]
                                                   [{:id "knoxx_default"
                                                     :contractClass "agents"
                                                     :contract agent-contract}])
                  loader/find-contract-record-sync (fn [_ class id]
                                                     (case class
                                                       "agents" {:id id :contract agent-contract}
                                                       nil))
                  sut/default-actor-id (fn [_] "chat_primary")
                  sut/resolve-actor (fn [_ _]
                                      {:id "chat_primary"
                                       :default-agent "knoxx_default"
                                       :role-slugs ["knowledge-worker"]
                                       :capability-ids []})
                  roles/role-capability-ids (fn [_ role]
                                              (when (= "knowledge-worker" role)
                                                ["read" "sandbox-container"]))
                  roles/role-system-prompt (fn [_ _] nil)
                  roles/role-task-prompt (fn [_ _] nil)
                  roles/role-tool-ids (fn [_ _] [])
                  roles/capability-tool-ids (fn [_ _] [])]
      (let [catalog (sut/agent-contract-catalog {} "chat_primary")]
        (is (= ["knoxx_default"] (mapv :id catalog)))
        (is (= "knoxx_default" (sut/default-agent-contract-id {} "chat_primary")))
        (is (= ["read" "sandbox-container"] (:capability-ids (first catalog))))))))

; ---------------------------------------------------------------------------
; Multi-role composition (agent-role-claims)
; ---------------------------------------------------------------------------

(deftest agent-role-claims-handles-single-role
  (testing "single :role keyword is returned as a one-element vector"
    (let [contract {:agent {:role :knowledge_worker}}
          claims   (knoxx.backend.domain.actor.scope/agent-role-claims contract)]
      (is (= [:knowledge_worker] claims)))))

(deftest agent-role-claims-handles-roles-vector
  (testing ":agent {:roles [...]} returns all roles"
    (let [contract {:agent {:roles [:creative_catalyst :developer]}}
          claims   (knoxx.backend.domain.actor.scope/agent-role-claims contract)]
      (is (= [:creative_catalyst :developer] claims)))))

(deftest agent-role-claims-merges-role-and-roles
  (testing ":role and :roles are merged and deduped"
    (let [contract {:agent {:role :knowledge_worker
                            :roles [:developer :knowledge_worker]}}
          claims   (knoxx.backend.domain.actor.scope/agent-role-claims contract)]
      (is (= 2 (count claims)))
      (is (= (set claims) #{:knowledge_worker :developer})))))

(deftest agent-role-claims-legacy-actor-roles
  (testing ":actor/roles feeds into role claims alongside :agent {:role}"
    (let [contract {:actor/roles [:basic_user]
                    :agent {:role :knowledge_worker}}
          claims   (knoxx.backend.domain.actor.scope/agent-role-claims contract)]
      (is (contains? (set claims) :basic_user))
      (is (contains? (set claims) :knowledge_worker)))))
