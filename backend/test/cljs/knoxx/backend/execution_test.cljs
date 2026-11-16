(ns knoxx.backend.execution-test
  (:require [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            [knoxx.backend.extern.crypto :as crypto]
            [knoxx.backend.extern.eta-mu :as sdk]
            [knoxx.backend.extern.fetch :as fetch]
            [knoxx.backend.infra.execution.client :as client]
            [knoxx.backend.infra.execution.tools :as tools]
            [knoxx.backend.law.execution :as law]))

(def config {:execution-mode "isolated" :execution-base-url "http://executor.test"
             :execution-auth-key (apply str (repeat 64 "a"))})
(def principal {:actor-id "open_hax" :org-slug "open-hax"})
(def context {:actor-binding "open_hax" :orgSlug "open-hax"
              :toolPolicies [{:toolId "bash" :effect "allow"}
                             {:toolId "read" :effect "allow"}
                             {:toolId "write" :effect "deny"}]})

(deftest request-boundary-rejects-environment-docker-and-identity-injection
  (let [request {:principal principal :tool "bash" :parameters {:command "id"}}]
    (is (= request (law/assert-valid! law/Request request)))
    (doseq [bad [(assoc request :environment {"KNOXX_API_KEY" "x"})
                 (assoc request :image "privileged")
                 (assoc request :mount "/:/host")
                 (assoc request :principal {:actor-id "open_hax"})
                 (assoc request :tool "docker")]]
      (is (thrown? js/Error (law/assert-valid! law/Request bad)))))
  (is (thrown? js/Error (law/require-client! (dissoc config :execution-auth-key)))))

(deftest exact-identities-never-share-workspace-by-similarity-or-delimiter-ambiguity
  (is (not= (client/workspace config principal)
            (client/workspace config (assoc principal :actor-id "codex"))))
  (is (not= (client/workspace config principal)
            (client/workspace config (assoc principal :org-slug "other-org"))))
  (is (not= (law/scope-material {:org-slug "a:b" :actor-id "c"})
            (law/scope-material {:org-slug "a" :actor-id "b:c"})))
  (is (= 64 (count (crypto/sha256-hex (law/scope-material principal))))))

(deftest isolated-sdk-loader-does-not-discover-executable-workspace-resources
  (let [config (assoc config :workspace-root "/untrusted/workspace" :agent-dir "/trusted/sdk")
        isolated (law/resource-loader-options config)]
    (is (= "/trusted/sdk" (:cwd isolated)))
    (doseq [disabled [:noExtensions :noSkills :noPromptTemplates :noThemes :noContextFiles]]
      (is (true? (get isolated disabled))))
    (is (= {:cwd "/untrusted/workspace" :agentDir "/trusted/sdk"}
           (law/resource-loader-options (assoc config :execution-mode "local"))))))

(deftest worker-name-collisions-and-changed-mounts-refuse-reuse-or-deletion
  (let [config {:image "sha256:trusted" :network "execution-egress" :runner-file "/trusted/runner.mjs"}
        inspected {:Image "sha256:trusted"
                   :Config {:Image "sha256:trusted" :User "1000:1000"
                            :Labels {:openhax.knoxx.execution "true" :openhax.knoxx.execution.scope "digest"}}
                   :HostConfig {:ReadonlyRootfs true :Privileged false :Init true :CapDrop ["ALL"]
                                :SecurityOpt ["no-new-privileges"] :PidsLimit 256
                                :Memory 2147483648 :NanoCpus 2000000000 :NetworkMode "execution-egress" :PidMode ""}
                   :Mounts [{:Source "/data/actor" :Destination "/workspace" :RW true}
                            {:Source "/trusted" :Destination "/opt/knoxx" :RW false}]}
        validate #(law/assert-owned-worker! config "digest" "/data/actor" "/workspace" %)]
    (is (= inspected (validate inspected)))
    (doseq [unsafe [(assoc-in inspected [:Config :Labels :openhax.knoxx.execution.scope] "another-actor")
                    (assoc-in inspected [:HostConfig :Privileged] true)
                    (assoc-in inspected [:HostConfig :ReadonlyRootfs] false)
                    (assoc-in inspected [:HostConfig :NetworkMode] "host")
                    (assoc-in inspected [:Mounts 0 :Source] "/state")
                    (update inspected :Mounts conj {:Source "/var/run/docker.sock" :Destination "/var/run/docker.sock" :RW true})]]
      (is (thrown? js/Error (validate unsafe))))))

(deftest worker-plan-never-mounts-the-backend-or-its-environment
  (let [args (law/worker-run-args {:image "trusted-image" :network "execution-egress"
                                  :runner-file "/trusted/runner.mjs"}
                                 "digest" "/data/actors/digest" "/state/execution-workspaces/digest")
        rendered (str/join " " args)]
    (doseq [required ["1000:1000" "--read-only" "--cap-drop ALL" "no-new-privileges"
                      "--pids-limit" "--memory" "--cpus" "--network execution-egress"]]
      (is (str/includes? rendered required)))
    (doseq [forbidden ["--privileged" "/var/run/docker.sock" "/state/mcp" "/proc:" "KNOXX_API_KEY"
                       "PROXX_AUTH_TOKEN" "--env-file" "--network host"]]
      (is (not (str/includes? rendered forbidden))))
    (is (= 2 (count (filter #{"--mount"} args))))))

(deftest ^:async sdk-coding-names-are-overridden-and-never-fall-back-to-local-execution
  (let [calls (atom [])]
    (with-redefs [client/execute! (fn [_ _ tool parameters]
                                  (swap! calls conj [tool parameters])
                                  (js/Promise.resolve {:content [{:type "text" :text "isolated result"}]}))]
      (let [definitions (tools/create-tools nil config context)
            bash (first (filter #(= "bash" (sdk/tool-runtime-name %)) definitions))
            result (js->clj (await ((sdk/tool-execute bash) "id" #js {:command "id"} nil nil nil))
                            :keywordize-keys true)]
        (is (= #{"read" "bash"} (set (map sdk/tool-runtime-name definitions))))
        (is (= [["bash" {:command "id"}]] @calls))
        (is (= "isolated result" (get-in result [:content 0 :text]))))))
  (is (thrown? js/Error (tools/create-tools nil (dissoc config :execution-auth-key) context))))

(deftest ^:async transport-sends-only-the-validated-principal-and-tool-input
  (let [calls (atom [])
        http-client (fetch/native-client
                     {:fetch-fn (fn [url init]
                                  (swap! calls conj {:url url :body (js->clj (js/JSON.parse (aget init "body")) :keywordize-keys true)})
                                  (js/Promise.resolve
                                   (js/Response. "{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}"
                                                 #js {:status 200})))})]
    (with-redefs [client/principal (fn [_ _] principal)
                  fetch/default-client http-client]
      (is (= {:content [{:type "text" :text "ok"}]}
             (await (client/execute! config context "bash" {:command "id"}))))
      (is (= {:principal principal :tool "bash" :parameters {:command "id"}}
             (:body (first @calls))))
      (is (= #{:principal :tool :parameters} (set (keys (:body (first @calls)))))))))

(deftest ^:async controller-errors-do-not-trigger-a-local-fallback
  (let [http-client (fetch/native-client {:fetch-fn (fn [_ _] (js/Promise.resolve (js/Response. "{}" #js {:status 401})))})]
    (with-redefs [client/principal (fn [_ _] principal)
                  fetch/default-client http-client]
      (try
        (await (client/execute! config context "bash" {:command "id"}))
        (is false "Unavailable controller must refuse execution")
        (catch :default error
          (is (= "execution_refused" (:code (ex-data error)))))))))
