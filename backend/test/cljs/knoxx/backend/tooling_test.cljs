(ns knoxx.backend.tooling-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.runtime.models :as models]
            [knoxx.backend.runtime.roles :as roles]
            [knoxx.backend.tooling :as tooling]))

(def test-config
  (models/enrich-config {:contracts-dir "contracts"
                         :workspace-root "/tmp/knoxx-test-workspace"
                         :proxx-base-url "http://127.0.0.1:8787"
                         :proxx-default-model "glm-5"}))

(deftest allowed-tool-id-set-follows-agent-contract-instead-of-full-runtime
  (testing "contract librarian is limited to its contract tools and loses general write/bash access"
    (let [tool-ids (tooling/allowed-tool-id-set test-config "contract_librarian" nil "contract_librarian" "contract_librarian")]
      (is (contains? tool-ids "read"))
      (is (contains? tool-ids "contract.write"))
      (is (contains? tool-ids "memory_search"))
      (is (not (contains? tool-ids "write")))
      (is (not (contains? tool-ids "edit")))
      (is (not (contains? tool-ids "bash")))
      (is (not (contains? tool-ids "discord.publish"))))))

(deftest create-runtime-tools-only-installs-builtins-allowed-by-contract
  (testing "manual chat agents no longer receive unrestricted write/edit/bash builtins by default"
    (let [runtime #js {}
          tool-names (set (tooling/create-runtime-tools runtime test-config nil "contract_librarian" "contract_librarian" "contract_librarian"))]
      (is (= #{"read"} tool-names)))))

(deftest allowed-tool-id-set-prefers-selected-agent-contract-tools
  (testing "selected manual agent contracts expose their declared tool set even when the caller's base tool policy is narrower"
    (with-redefs [tooling/effective-agent-contract (fn
                                                     ([_ _]
                                                      {:role "creative_catalyst"
                                                       :tool-ids ["mcp.shoedelussy.write_pattern"
                                                                  "audio.spectrogram"
                                                                  "music.identify_file"]})
                                                     ([_ _ _]
                                                      {:role "creative_catalyst"
                                                       :tool-ids ["mcp.shoedelussy.write_pattern"
                                                                  "audio.spectrogram"
                                                                  "music.identify_file"]}))
                  roles/role-tool-ids (fn [_ _] ["read"])]
      (is (= #{"mcp.shoedelussy.write_pattern" "audio.spectrogram" "music.identify_file"}
             (set (tooling/allowed-tool-id-set
                   {}
                   "knowledge_worker"
                   {:toolPolicies [{:toolId "read" :effect "allow"}]}
                   "creative_music_studio"
                   "chat_primary")))))))

(deftest allowed-tool-id-set-without-contract-still-uses-auth-policy
  (testing "plain workspace roles still fall back to request-scoped auth tool policies when no agent contract is selected"
    (with-redefs [tooling/effective-agent-contract (fn
                                                     ([_ _] nil)
                                                     ([_ _ _] nil))
                  roles/role-tool-ids (fn [_ _] ["read" "write"])]
      (is (= #{"read"}
             (set (tooling/allowed-tool-id-set
                   {}
                   "knowledge_worker"
                   {:toolPolicies [{:toolId "read" :effect "allow"}]}
                   nil
                   "chat_primary")))))))

(deftest creative-music-studio-overlay-adds-library-and-editing-tools
  (testing "the canonical root creative_music_studio contract augments the legacy role surface with library browsing and DAW-style editing tools"
    (let [tool-ids (set (tooling/allowed-tool-id-set
                         test-config
                         "knowledge_worker"
                         nil
                         "creative_music_studio"
                         "chat_primary"))]
      (is (contains? tool-ids "read"))
      (is (contains? tool-ids "write"))
      (is (contains? tool-ids "edit"))
      (is (contains? tool-ids "bash"))
      (is (contains? tool-ids "audio.spectrogram"))
      (is (contains? tool-ids "audio.waveform"))
      (is (contains? tool-ids "workspace_media.attach"))
      (is (contains? tool-ids "music.identify_file")))))
