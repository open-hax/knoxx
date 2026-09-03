(ns knoxx.backend.config-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.config :as config]))

(def ^:private missing-env-value (js-obj))

(defn- with-env!
  [bindings f]
  (let [env (.-env js/process)
        previous (into {}
                       (map (fn [[k _]]
                              [k (let [value (aget env k)]
                                   (if (some? value) value missing-env-value))]))
                       bindings)]
    (doseq [[k v] bindings]
      (if (nil? v)
        (js-delete env k)
        (aset env k v)))
    (try
      (f)
      (finally
        (doseq [[k old-value] previous]
          (if (identical? old-value missing-env-value)
            (js-delete env k)
            (aset env k old-value)))))))

(deftest cfg-defaults-use-workspace-neutral-names
  (with-env! {"WORKSPACE_ROOT" nil
              "WORKSPACE_PATH" nil
              "KNOXX_WORKSPACE_ROOT" nil
              "WORKSPACE_PROJECT_NAME" nil
              "KNOXX_WORKSPACE_PROJECT" nil
              "KNOXX_COLLECTION_NAME" nil
              "KNOXX_OPENPLANNER_PROJECT" nil
              "KNOXX_AGENT_SYSTEM_PROMPT" nil}
    (fn []
      (let [cfg (config/cfg)]
        (testing "filesystem and collection defaults do not depend on a developer checkout name"
          (is (= "/app/workspace" (:workspace-root cfg)))
          (is (= "workspace" (:project-name cfg)))
          (is (= "workspace_docs" (:collection-name cfg)))
          (is (= "workspace" (:openplanner-mcp-project cfg))))
        (testing "the default agent prompt describes the active workspace instead of a local corpus"
          (is (str/includes? (:agent-system-prompt cfg) "active workspace corpus"))
          (is (not (str/includes? (:agent-system-prompt cfg) (str "devel" " corpus")))))))))

(deftest cfg-prefers-first-nonblank-workspace-env-alias
  (with-env! {"WORKSPACE_ROOT" ""
              "WORKSPACE_PATH" "/workspace/from-workspace-path"
              "KNOXX_WORKSPACE_ROOT" "/workspace/from-knoxx-root"
              "WORKSPACE_PROJECT_NAME" ""
              "KNOXX_WORKSPACE_PROJECT" "portable-project"
              "KNOXX_OPENPLANNER_PROJECT" ""}
    (fn []
      (let [cfg (config/cfg)]
        (is (= "/workspace/from-workspace-path" (:workspace-root cfg)))
        (is (= "portable-project" (:project-name cfg)))
        (is (= "portable-project" (:openplanner-mcp-project cfg)))))))

(deftest cfg-openplanner-project-override-wins-over-workspace-project
  (with-env! {"WORKSPACE_PROJECT_NAME" "workspace-project"
              "KNOXX_WORKSPACE_PROJECT" "fallback-project"
              "KNOXX_OPENPLANNER_PROJECT" "openplanner-project"}
    (fn []
      (is (= "openplanner-project" (:openplanner-mcp-project (config/cfg)))))))

(deftest cfg-keeps-ollama-optional-and-accepts-local-provider-settings
  (with-env! {"OLLAMA_BASE_URL" nil
              "OLLAMA_DEFAULT_MODEL" nil}
    (fn []
      (is (= "" (:ollama-base-url (config/cfg))))
      (is (nil? (:ollama-default-model (config/cfg))))))
  (with-env! {"OLLAMA_BASE_URL" "http://127.0.0.1:11434"
              "OLLAMA_DEFAULT_MODEL" "gemma4:e4b"}
    (fn []
      (let [cfg (config/cfg)]
        (is (= "http://127.0.0.1:11434" (:ollama-base-url cfg)))
        (is (= "gemma4:e4b" (:ollama-default-model cfg)))))))

(deftest cfg-parses-agent-deployment-overrides
  (with-env! {"KNOXX_AGENT_MODEL_OVERRIDES" nil
              "KNOXX_AGENT_THINKING_OVERRIDES" nil}
    (fn []
      (is (= {} (:agent-model-overrides (config/cfg))))
      (is (= {} (:agent-thinking-overrides (config/cfg))))))
  (with-env! {"KNOXX_AGENT_MODEL_OVERRIDES" " publication_translator = gemma4:e2b , other = gpt-5.5 "
              "KNOXX_AGENT_THINKING_OVERRIDES" "publication_translator=off"}
    (fn []
      (let [cfg (config/cfg)]
        (is (= {"publication_translator" "gemma4:e2b"
                "other" "gpt-5.5"}
               (:agent-model-overrides cfg)))
        (is (= {"publication_translator" "off"}
               (:agent-thinking-overrides cfg)))))))

(deftest cfg-bounds-event-triggered-agent-concurrency
  (with-env! {"KNOXX_EVENT_AGENT_CONCURRENCY" nil
              "KNOXX_EVENT_AGENT_QUEUE_LIMIT" nil}
    (fn []
      (let [cfg (config/cfg)]
        (is (= 1 (:event-agent-concurrency cfg)))
        (is (= 256 (:event-agent-queue-limit cfg))))))
  (with-env! {"KNOXX_EVENT_AGENT_CONCURRENCY" "2"
              "KNOXX_EVENT_AGENT_QUEUE_LIMIT" "95"}
    (fn []
      (let [cfg (config/cfg)]
        (is (= 2 (:event-agent-concurrency cfg)))
        (is (= 95 (:event-agent-queue-limit cfg))))))
  (with-env! {"KNOXX_EVENT_AGENT_CONCURRENCY" "0"
              "KNOXX_EVENT_AGENT_QUEUE_LIMIT" "-1"}
    (fn []
      (let [cfg (config/cfg)]
        (is (= 1 (:event-agent-concurrency cfg)))
        (is (= 1 (:event-agent-queue-limit cfg)))))))

(deftest cfg-keeps-event-turn-timeout-separate-from-interactive-timeout
  (with-env! {"KNOXX_AGENT_TURN_TIMEOUT_MS" "1200"
              "KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS" nil}
    (fn []
      (let [cfg (config/cfg)]
        (is (= 1200 (:agent-turn-timeout-ms cfg)))
        (is (= 0 (:event-agent-turn-timeout-ms cfg))))))
  (with-env! {"KNOXX_AGENT_TURN_TIMEOUT_MS" "1200"
              "KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS" "300000"}
    (fn []
      (let [cfg (config/cfg)]
        (is (= 1200 (:agent-turn-timeout-ms cfg)))
        (is (= 300000 (:event-agent-turn-timeout-ms cfg)))))))

(deftest cfg-rejects-event-timeouts-that-node-would-clamp-or-misparse
  (doseq [invalid ["" "   " "0" "-1" "1.5" "3000000000" "not-a-number"
                   (apply str (repeat 400 "9"))]]
    (with-env! {"KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS" invalid}
      (fn []
        (try
          (config/cfg)
          (is false (str "accepted invalid event timeout " invalid))
          (catch :default err
            (is (= :config/invalid-node-timeout
                   (:error/kind (ex-data err)))))))))
  (with-env! {"KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS" "2147483647"}
    (fn []
      (is (= 2147483647
             (:event-agent-turn-timeout-ms (config/cfg)))))))

(deftest cfg-keeps-generated-contracts-separate-from-authored-contracts
  (with-env! {"CONTRACTS_DIR" "/app/contracts"
              "KNOXX_GENERATED_CONTRACTS_DIR" nil}
    (fn []
      (let [cfg (config/cfg)]
        (is (= "/app/contracts" (:contracts-dir cfg)))
        (is (nil? (:generated-contracts-dir cfg))))))
  (with-env! {"CONTRACTS_DIR" "/app/contracts"
              "KNOXX_GENERATED_CONTRACTS_DIR" "/app/workspace/.knoxx/contracts"}
    (fn []
      (let [cfg (config/cfg)]
        (is (= "/app/contracts" (:contracts-dir cfg)))
        (is (= "/app/workspace/.knoxx/contracts"
               (:generated-contracts-dir cfg)))))))

(deftest cfg-session-project-name-uses-a-nonblank-override
  (testing "unset and blank values retain the safe session-project default"
    (doseq [value [nil "" " "]]
      (with-env! {"KNOXX_SESSION_PROJECT_NAME" value}
        (fn []
          (is (= "knoxx-session" (:session-project-name (config/cfg))))))))
  (testing "an explicit nonblank deployment value wins"
    (with-env! {"KNOXX_SESSION_PROJECT_NAME" "review-stage"}
      (fn []
        (is (= "review-stage" (:session-project-name (config/cfg))))))))

(deftest publication-site-url-is-deployment-configuration
  (with-env! {"KNOXX_PUBLICATION_SITE_URL" nil
              "KNOXX_PUBLICATION_CONTENT_ROOT" nil}
    (fn []
      (is (= "http://localhost:4173" (:publication-site-url (config/cfg))))
      (is (nil? (:publication-content-root (config/cfg))))))
  (with-env! {"KNOXX_PUBLICATION_SITE_URL" "https://open-hax.promethean.rest"
              "KNOXX_PUBLICATION_CONTENT_ROOT" "/srv/website-content"}
    (fn []
      (is (= "https://open-hax.promethean.rest"
             (:publication-site-url (config/cfg))))
      (is (= "/srv/website-content" (:publication-content-root (config/cfg)))))))

(deftest sandbox-user-is-an-optional-host-identity-assertion
  (testing "the runtime derives the effective uid:gid when no assertion exists"
    (with-env! {"DOCKER_USER" nil}
      (fn [] (is (nil? (:sandbox-user (config/cfg)))))))
  (testing "an explicit identity remains available for fail-closed comparison"
    (with-env! {"DOCKER_USER" "1000:1000"}
      (fn [] (is (= "1000:1000" (:sandbox-user (config/cfg))))))))

;; ── Event-runtime kill switch ──────────────────────────────────────────────

(deftest event-runtimes-disabled-defaults-to-off
  (testing "unset means event runtimes run, which is the production behavior"
    (with-env! {"KNOXX_DISABLE_EVENT_RUNTIMES" nil}
      (fn [] (is (false? (:event-runtimes-disabled? (config/cfg))))))))

(deftest event-runtimes-disabled-needs-an-explicit-affirmative
  (testing "only an explicit affirmative disables them"
    (doseq [value ["1" "true" "TRUE" "yes" "on"]]
      (with-env! {"KNOXX_DISABLE_EVENT_RUNTIMES" value}
        (fn [] (is (true? (:event-runtimes-disabled? (config/cfg)))
                   (str value " should disable"))))))
  (testing "and anything else leaves them running"
    ;; A flag that silences schedules, triggers and Discord must never be
    ;; switchable by accident: a typo, a blank, or the string "false" all have
    ;; to fail safe toward running.
    (doseq [value ["" " " "false" "0" "no" "off" "disabled" "ture"]]
      (with-env! {"KNOXX_DISABLE_EVENT_RUNTIMES" value}
        (fn [] (is (false? (:event-runtimes-disabled? (config/cfg)))
                   (str (pr-str value) " must NOT disable")))))))
