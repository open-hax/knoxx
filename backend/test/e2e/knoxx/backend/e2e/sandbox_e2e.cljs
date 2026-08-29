(ns knoxx.backend.e2e.sandbox-e2e
  "The sandbox tools, against a real container.

   These are the tools least amenable to a fixture sweep: every one after
   create needs a sandbox_id the previous call returned, so calling them
   individually only ever proves they reject a missing id. The value is in the
   chain, and the chain has to run against a real docker daemon — a stubbed
   `docker` binary would assert the argv we already wrote and never prove a
   container starts.

   Skipped with a loud log when no daemon is reachable, and hard-failed when
   KNOXX_E2E_REQUIRE_DOCKER=true, which the deploy host sets. A silently
   skipped sandbox test must never read as a passing one."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.e2e.mcp-client :as mcp]))

(def ^:private probe-path "e2e-probe.txt")
(def ^:private probe-content "sandbox-e2e-content")
(def ^:private missing-probe-path "missing-e2e-probe.txt")

(def ^:private uuid-pattern
  "Sandbox ids are crypto.randomUUID values.

   Anchored on the full hyphenated shape on purpose: a looser hex pattern
   matched only the first group and produced a container name that had never
   existed, which then read as a create that silently did nothing."
  #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

(defn- sandbox-id-from
  "The sandbox id a create result reported.

   Preferring structured content when the transport carries it, and otherwise
   reading the id out of the sentence create returns."
  [result]
  (or (some-> result :structuredContent :sandboxId)
      (some-> result :details :sandboxId)
      (re-find uuid-pattern (str (mcp/tool-text result)))))

(defn- ^:async call!
  [client tool-name args]
  (let [rpc (await (mcp/call-tool! client tool-name args))]
    (assoc (mcp/call-outcome rpc) :result (:result rpc))))

(defn- ^:async destroy-quietly!
  [client sandbox-id]
  (when sandbox-id
    (try
      (await (mcp/call-tool! client "sandbox_container_destroy" {:sandbox_id sandbox-id}))
      (catch :default err
        (harness/warn!
         (str "[sandbox-e2e] destroy failed; a container may be left behind: "
              sandbox-id " " err))))))

(defn- ^:async create-sandbox!
  [client id*]
  (testing "create returns a usable sandbox id"
    (let [created (await (call! client "sandbox_container_create" {:ttl_seconds 300}))]
      (is (= :ok (:status created))
          (str "sandbox_container_create failed: " (:detail created)))
      (reset! id* (sandbox-id-from (:result created)))
      (is (some? @id*)
          (str "create reported no sandbox id: " (:detail created)))
      @id*)))

(defn- ^:async assert-live!
  [client sandbox-id]
  (testing "status reports the sandbox as live"
    (let [status (await (call! client "sandbox_container_status"
                               {:sandbox_id sandbox-id}))]
      (is (= :ok (:status status))
          (str "sandbox_container_status failed: " (:detail status))))))

(defn- ^:async assert-metadata-private!
  [client sandbox-id]
  (testing "Knoxx metadata is outside the container-visible bind mount"
    (let [ran (await (call! client "sandbox_container_exec"
                            {:sandbox_id sandbox-id
                             :command (str "if [ -e .knoxx-sandbox.json ]; "
                                           "then echo exposed; exit 19; "
                                           "else echo private; fi")
                             :timeout_ms 30000}))]
      (is (= :ok (:status ran))
          (str "sandbox_container_exec failed: " (:detail ran)))
      (is (str/includes? (str (:detail ran)) "Sandbox exec exit=0")
          (str "sandbox metadata was visible in the workdir: " (:detail ran)))
      (is (str/includes? (str (:detail ran)) "private")
          (str "sandbox metadata privacy probe omitted its marker: " (:detail ran))))))

(defn- ^:async assert-internal-command-errors!
  [client sandbox-id]
  (testing "a failed Knoxx-owned read is a tool error"
    (let [read-back (await (call! client "sandbox_container_read"
                                  {:sandbox_id sandbox-id :path missing-probe-path}))]
      (is (= :tool-error (:status read-back))
          (str "missing read must be a tool error: " read-back))
      (is (str/includes? (str (:detail read-back)) "sandbox read failed (exit 1)")
          (str "missing read omitted bounded exit evidence: " (:detail read-back)))))

  (testing "a failed Knoxx-owned write is a tool error"
    (await (call! client "sandbox_container_exec"
                  {:sandbox_id sandbox-id
                   :command "mkdir -p readonly && chmod 500 readonly"}))
    (let [wrote (await (call! client "sandbox_container_write"
                              {:sandbox_id sandbox-id
                               :path "readonly/probe.txt"
                               :content probe-content}))]
      (is (= :tool-error (:status wrote))
          (str "unwritable path must be a tool error: " wrote))
      (is (str/includes? (str (:detail wrote)) "sandbox write failed (exit ")
          (str "failed write omitted bounded exit evidence: " (:detail wrote))))
    (await (call! client "sandbox_container_exec"
                  {:sandbox_id sandbox-id
                   :command "chmod 700 readonly && rm -rf readonly"})))

  (testing "a failed Knoxx-owned commit is a tool error"
    (await (call! client "sandbox_container_exec"
                  {:sandbox_id sandbox-id
                   :command "mkdir -p .git && chmod 000 .git"}))
    (let [committed (await (call! client "sandbox_container_commit"
                                  {:sandbox_id sandbox-id :message "must fail"}))]
      (is (= :tool-error (:status committed))
          (str "unusable git metadata must be a tool error: " committed))
      (is (str/includes? (str (:detail committed)) "sandbox commit failed (exit ")
          (str "failed commit omitted bounded exit evidence: " (:detail committed))))
    (await (call! client "sandbox_container_exec"
                  {:sandbox_id sandbox-id
                   :command "chmod 700 .git && rm -rf .git"}))))

(defn- ^:async assert-file-roundtrip!
  [client sandbox-id]
  (testing "a written file reads back byte for byte"
    (let [wrote (await (call! client "sandbox_container_write"
                              {:sandbox_id sandbox-id
                               :path probe-path
                               :content probe-content}))]
      (is (= :ok (:status wrote))
          (str "sandbox_container_write failed: " (:detail wrote))))
    (let [read-back (await (call! client "sandbox_container_read"
                                  {:sandbox_id sandbox-id :path probe-path}))]
      (is (= :ok (:status read-back))
          (str "sandbox_container_read failed: " (:detail read-back)))
      (is (str/includes? (str (:detail read-back)) probe-content)
          (str "the file did not read back: " (:detail read-back))))))

(defn- ^:async assert-exec!
  [client sandbox-id]
  (testing "exec sees the shared file from inside the container"
    (let [ran (await (call! client "sandbox_container_exec"
                            {:sandbox_id sandbox-id
                             :command (str "cat " probe-path)
                             :timeout_ms 30000}))]
      (is (= :ok (:status ran))
          (str "sandbox_container_exec failed: " (:detail ran)))
      (is (str/includes? (str (:detail ran)) probe-content)
          (str "exec did not observe the written file: " (:detail ran)))))
  (testing "a user-requested nonzero exit remains observable result data"
    (let [ran (await (call! client "sandbox_container_exec"
                            {:sandbox_id sandbox-id
                             :command "exit 17"
                             :timeout_ms 30000}))]
      (is (= :ok (:status ran))
          (str "user exec nonzero must remain result data: " ran))
      (is (str/includes? (str (:detail ran)) "Sandbox exec exit=17")
          (str "user exec omitted its exit code: " (:detail ran)))))
  (testing "exec is genuinely inside a container, not on this host"
    (let [ran (await (call! client "sandbox_container_exec"
                            {:sandbox_id sandbox-id
                             :command "cat /proc/self/cgroup; hostname"
                             :timeout_ms 30000}))]
      (is (= :ok (:status ran))
          (str "sandbox_container_exec failed: " (:detail ran)))
      (is (not (str/includes? (str (:detail ran)) (harness/host-name)))
          (str "the command reported this host's hostname: " (:detail ran))))))

(defn- ^:async assert-commit!
  [client sandbox-id]
  (testing "commit records the working tree"
    (let [committed (await (call! client "sandbox_container_commit"
                                  {:sandbox_id sandbox-id :message "e2e probe"}))]
      (is (= :ok (:status committed))
          (str "sandbox_container_commit failed: " (:detail committed))))))

(defn- ^:async assert-destroyed!
  [client sandbox-id id*]
  (testing "destroy removes the sandbox"
    (let [destroyed (await (call! client "sandbox_container_destroy"
                                  {:sandbox_id sandbox-id}))]
      (is (= :ok (:status destroyed))
          (str "sandbox_container_destroy failed: " (:detail destroyed)))
      (when (= :ok (:status destroyed))
        (reset! id* nil))))
  (testing "a later status call reports the expected missing-sandbox error"
    (let [after (await (call! client "sandbox_container_status"
                              {:sandbox_id sandbox-id}))]
      (is (= :tool-error (:status after))
          (str "destroyed sandbox must produce a tool-level absence: " after))
      (is (str/includes? (str (:detail after)) (str "Sandbox not found: " sandbox-id))
          (str "destroyed sandbox reported the wrong failure: " (:detail after))))))

(defn- ^:async run-lifecycle!
  [client id*]
  (await (mcp/initialize! client))
  (when-let [sandbox-id (await (create-sandbox! client id*))]
    (await (assert-live! client sandbox-id))
    (await (assert-metadata-private! client sandbox-id))
    (await (assert-internal-command-errors! client sandbox-id))
    (await (assert-file-roundtrip! client sandbox-id))
    (await (assert-exec! client sandbox-id))
    (await (assert-commit! client sandbox-id))
    (await (assert-destroyed! client sandbox-id id*))))

(deftest ^:async sandbox-lifecycle-test
  (let [docker? (await (harness/docker-available?))]
    (cond
      (and (not docker?) (harness/require-docker?))
      (is false
          (str "KNOXX_E2E_REQUIRE_DOCKER=true but no docker daemon answered. "
               "The sandbox tools cannot run here."))

      (not docker?)
      (harness/report!
       (str "\n[sandbox-e2e] SKIPPED — no docker daemon. "
            "Set KNOXX_E2E_REQUIRE_DOCKER=true to make this a failure."))

      :else
      (let [started (await (harness/start!))
            client  (harness/client started)
            id*     (atom nil)]
        (try
          (await (run-lifecycle! client id*))
          (finally
            ;; Runs even when an assertion above threw, so a failing test never
            ;; leaves a TTL-bound container behind on a developer's machine or
            ;; the deploy host.
            (await (destroy-quietly! client @id*))
            (await (harness/stop! started))))))))
