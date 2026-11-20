(ns knoxx.backend.law.execution
  "Boundaries for actor-scoped coding execution, independent of the transport."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def coding-tools
  "SDK coding tools whose execution must leave the privileged backend."
  #{"read" "write" "edit" "bash"})

(def Principal
  "A server-resolved actor and organization; tool parameters cannot choose these."
  [:map {:closed true}
   [:actor-id [:string {:min 1 :max 180}]]
   [:org-slug [:string {:min 1 :max 120}]]])

(def Request
  "Only a tool call crosses this boundary, never Docker options or environment."
  [:map {:closed true}
   [:principal Principal]
   [:protocol {:optional true} [:enum "http"]]
   [:tool [:enum "read" "write" "edit" "bash"]]
   [:parameters :map]])

(def Result
  "An SDK tool result must be structurally valid before entering model context."
  [:map [:content [:vector :map]] [:isError {:optional true} :boolean]])

(def HttpResult
  "The worker preserves the HTTP tool response status and body."
  [:map {:closed true} [:status [:int {:min 200 :max 599}]] [:body :map]])

(defn runner-directory
  "Only the trusted integration directory is mounted as worker code."
  [config]
  (str/replace (:runner-file config) #"/[^/]+$" ""))

(def media-environment-keys
  "Public native-renderer settings; service credentials are deliberately absent."
  [:PATH :LD_LIBRARY_PATH :OPENUTAU_HOME :VOICEBANK_DIR])

(defn media-process-environment
  "Native helpers receive only explicit public settings and a temporary HOME."
  [environment]
  (assoc (select-keys environment media-environment-keys) :HOME "/tmp"))

(def ControllerConfig
  "Operator-owned executor settings, never supplied by an agent request."
  [:map {:closed true}
   [:host :string] [:port [:int {:min 1024 :max 65535}]]
   [:auth-key [:re #"[a-f0-9]{64}"]]
   [:image [:string {:min 1}]] [:root-dir [:string {:min 1}]]
   [:runner-file [:string {:min 1}]]
   [:network [:string {:min 1}]]
   [:workspace-overrides {:optional true}
    [:map-of [:re #"[a-f0-9]{64}"]
     [:map {:closed true} [:host-dir :string] [:cwd :string]]]]])

(defn assert-valid!
  "Reject malformed execution requests before any filesystem/process effect."
  [schema value]
  (when-not (m/validate schema value)
    (throw (ex-info "Invalid isolated execution boundary"
                    {:status 400 :code "execution_invalid"})))
  value)

(defn scope-material
  "Length-delimited exact identities; similarity never joins two workspaces."
  [{:keys [actor-id org-slug] :as principal}]
  (assert-valid! Principal principal)
  (str (count org-slug) ":" org-slug (count actor-id) ":" actor-id))

(defn isolated?
  "An isolated deployment must never silently use local coding tools."
  [config]
  (= "isolated" (:execution-mode config)))

(defn restricted?
  "Restricted hosts never expose SDK coding tools backed by the backend process."
  [config]
  (or (isolated? config) (= "disabled" (:execution-mode config))))

(defn resource-loader-options
  "Actor-writable workspaces cannot supply code or prompts to the trusted SDK host."
  [config]
  (cond-> {:cwd (:workspace-root config) :agentDir (:agent-dir config)}
    (restricted? config)
    (assoc :cwd (:agent-dir config)
           :noExtensions true :noSkills true :noPromptTemplates true
           :noThemes true :noContextFiles true)))

(defn assert-owned-worker!
  "Reuse or replace only a controller-owned container with the exact isolation plan."
  [{:keys [image network] :as config} scope host-dir cwd inspected]
  (let [host (:HostConfig inspected)
        container (:Config inspected)
        mounts (set (map #(select-keys % [:Source :Destination :RW]) (:Mounts inspected)))
        expected-mounts #{{:Source host-dir :Destination cwd :RW true}
                          {:Source (runner-directory config) :Destination "/opt/knoxx" :RW false}}]
    (when-not (and (= "true" (get-in container [:Labels :openhax.knoxx.execution]))
                   (= scope (get-in container [:Labels :openhax.knoxx.execution.scope]))
                   (= image (:Image container))
                   (or (not (str/starts-with? image "sha256:")) (= image (:Image inspected)))
                   (= "1000:1000" (:User container))
                   (true? (:ReadonlyRootfs host)) (false? (:Privileged host))
                   (true? (:Init host))
                   (= #{"ALL"} (set (:CapDrop host)))
                   (some #{"no-new-privileges" "no-new-privileges:true"} (:SecurityOpt host))
                   (= 256 (:PidsLimit host)) (= 2147483648 (:Memory host))
                   (= 2000000000 (:NanoCpus host)) (= network (:NetworkMode host))
                   (str/blank? (:PidMode host))
                   (= expected-mounts mounts))
      (throw (ex-info "Existing execution container does not match the owned isolation plan"
                      {:status 503 :code "execution_worker_mismatch"})))
    inspected))

(defn require-client!
  "Missing isolation configuration is a refusal, not a local execution fallback."
  [config]
  (when (or (str/blank? (:execution-base-url config))
            (not (re-matches #"[a-f0-9]{64}" (or (:execution-auth-key config) ""))))
    (throw (ex-info "Isolated execution is not configured"
                    {:status 503 :code "execution_unavailable"})))
  config)

(defn worker-run-args
  "A worker sees its own workspace, an immutable SDK runner, and no service state."
  [{:keys [image network] :as config} scope host-dir cwd]
  ["run" "-d" "--name" (str "knoxx-execution-" scope)
   "--user" "1000:1000" "--read-only" "--init"
   "--cap-drop" "ALL" "--security-opt" "no-new-privileges"
   "--pids-limit" "256" "--memory" "2g" "--cpus" "2"
   "--network" network "--no-healthcheck"
   "--tmpfs" "/tmp:rw,nosuid,nodev,size=268435456"
   "--env" (str "HOME=" cwd)
   "--workdir" cwd
   "--label" "openhax.knoxx.execution=true"
   "--label" (str "openhax.knoxx.execution.scope=" scope)
   "--mount" (str "type=bind,source=" host-dir ",target=" cwd)
   "--mount" (str "type=bind,source=" (runner-directory config) ",target=/opt/knoxx,readonly")
   "--entrypoint" "node" image "-e" "setInterval(()=>{},3600000)"])
