(ns knoxx.backend.extern.mcp-workspace-tools
  "Direct MCP workspace tools; authorization remains bound to the MCP caller."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.text :refer [sanitize-svg-content tool-text-result]]
            [knoxx.backend.domain.tools :refer [create-tool-obj]]
            [knoxx.backend.infra.agent.runtime :refer [resolve-workspace-path]]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            ["node:child_process" :refer [execFile]]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]
            ["node:util" :refer [promisify]]))

(def ^:private ^{:doc "Promise-based child process boundary."} exec-file-async (promisify execFile))

(def ^{:doc "MCP workspace read input schema."} read-params [:map [:path :string]])
(def ^{:doc "MCP workspace write input schema."} write-params [:map [:path :string] [:content :string]
                   [:overwrite {:optional true} :boolean]])
(def ^{:doc "MCP direct Bash input schema."} bash-params [:map [:command :string]
                  [:workdir {:optional true} :string]
                  [:timeout_ms {:optional true} [:int {:min 1000 :max 120000}]]])

(defn- ^:async read-execute
  "Read a file or directory through the MCP tool boundary."
  [runtime config _id params _a _b _c]
  (let [requested (str (or (aget params "path") ""))
        resolved (resolve-workspace-path runtime config requested)
        stat (await (.stat fs resolved))
        text (if (.isDirectory stat)
               (str/join "\n" (sort (array-seq (await (.readdir fs resolved)))))
               (await (.readFile fs resolved "utf8")))
        content (subs text 0 (min (count text) 20000))]
    (tool-text-result content {:path resolved :content content :truncated (< (count content) (count text))})))

(defn- ^:async write-execute
  "Write UTF-8 content, refusing replacement unless explicitly requested."
  [runtime config _id params _a _b _c]
  (let [requested (str (or (aget params "path") ""))
        _ (when (str/blank? requested) (throw (js/Error. "path is required")))
        resolved (resolve-workspace-path runtime config requested)
        raw-content (str (or (aget params "content") ""))
        content (if (re-find #"(?i)\.svg$" resolved)
                  (sanitize-svg-content raw-content)
                  raw-content)
        overwrite (true? (aget params "overwrite"))]
    (await (.mkdir fs (.dirname path resolved) #js {:recursive true}))
    (await (.writeFile fs resolved content #js {:encoding "utf8" :flag (if overwrite "w" "wx")}))
    (tool-text-result (str "Wrote " resolved) {:path resolved :overwrite overwrite})))

(defn- ^:async bash-execute
  "Apply a timeout to the direct Bash process and report failures as MCP errors."
  [runtime config _id params _a _b _c]
  (let [command (str (or (aget params "command") ""))
        _ (when (str/blank? command) (throw (js/Error. "command is required")))
        workdir (resolve-workspace-path runtime config (or (aget params "workdir") ""))
        timeout (min 120000 (max 1000 (or (aget params "timeout_ms") 60000)))]
    (try
      (let [result (await (exec-file-async "/bin/bash" #js ["-lc" command]
                                           #js {:cwd workdir :timeout timeout :killSignal "SIGKILL" :maxBuffer 1048576}))
            stdout (str (or (aget result "stdout") ""))
            stderr (str (or (aget result "stderr") ""))]
        (tool-text-result stdout {:exitCode 0 :stdout stdout :stderr stderr :workdir workdir}))
      (catch :default err
        (let [result (tool-text-result (str "Command failed: " (.-message err))
                                       {:exitCode (or (aget err "code") 1) :stdout (or (aget err "stdout") "")
                                        :stderr (or (aget err "stderr") "") :workdir workdir :failed true})]
          (aset result "isError" true)
          result)))))

(defn create-workspace-tools
  "Construct only tools allowed by the caller's policy; MCP token grants are intersected later."
  [runtime config auth-context]
  (let [allowed? (fn [id] (or (nil? auth-context) (ctx-tool-allowed? auth-context id)))
        tool (fn [id label description params execute]
               (create-tool-obj id label description description [] params execute runtime config))]
    (clj->js
     (vec (remove nil?
                  [(when (allowed? "read")
                     (tool "read" "Workspace Read" "Read a file or list a directory in configured workspace roots. Symlinks are not a containment boundary." read-params read-execute))
                   (when (allowed? "write")
                     (tool "write" "Workspace Write" "Write a file in a configured workspace root; overwrite must be explicit. Symlinks are not a containment boundary." write-params write-execute))
                   (when (allowed? "bash")
                     (tool "bash" "Bash" "Run Bash directly with the Knoxx process permissions and inherited environment. The working directory is not a sandbox. Timeout kills Bash, not necessarily its descendants." bash-params bash-execute))])))))
