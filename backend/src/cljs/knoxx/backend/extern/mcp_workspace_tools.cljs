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

(def ^{:doc "MCP workspace read input schema."} read-params
  [:map [:path :string]
   [:offset_bytes {:optional true :description "Zero-based UTF-8 byte offset from a previous next_offset_bytes response."}
    [:int {:min 0 :max 9007199254740991}]]])
(def ^{:doc "MCP workspace write input schema."} write-params [:map [:path :string] [:content :string]
                   [:overwrite {:optional true} :boolean]])
(def ^{:doc "MCP direct Bash input schema."} bash-params [:map [:command :string]
                  [:workdir {:optional true} :string]
                  [:timeout_ms {:optional true} [:int {:min 1000 :max 120000}]]])

(def ^:private ^{:doc "Maximum UTF-8 bytes returned per file-read page."} read-page-bytes 20000)

(defn- utf8-page-end
  "Choose a byte boundary without splitting a UTF-8 continuation sequence."
  [buffer bytes-read]
  (loop [end (min read-page-bytes bytes-read)]
    (if (and (< end bytes-read)
             (pos? end)
             (= 128 (bit-and 192 (aget buffer end))))
      (recur (dec end))
      end)))

(defn- ^:async fill-read-buffer!
  "Read until the bounded buffer is full or the file reaches EOF."
  [handle buffer offset]
  (loop [filled 0]
    (if (= filled (.-length buffer))
      filled
      (let [result (await (.read handle buffer filled (- (.-length buffer) filled) (+ offset filled)))
            n (.-bytesRead result)]
        (if (zero? n) filled (recur (+ filled n)))))))

(defn- ^:async read-file-page!
  "Read a bounded UTF-8 byte page and report the next byte offset."
  [resolved offset]
  (let [handle (await (.open fs resolved "r"))
        buffer (.alloc js/Buffer (+ read-page-bytes 4))]
    (try
      (let [bytes-read (await (fill-read-buffer! handle buffer offset))
            end (utf8-page-end buffer bytes-read)
            content (.toString buffer "utf8" 0 end)
            size (.-size (await (.stat handle)))
            truncated (< (+ offset end) size)]
        {:path resolved :content content :truncated truncated
         :offset_bytes offset :next_offset_bytes (when truncated (+ offset end))})
      (finally (await (.close handle))))))

(defn- ^:async read-execute
  "Read a file page or directory through the MCP tool boundary."
  [runtime config _id params _a _b _c]
  (let [requested (str (or (aget params "path") ""))
        resolved (resolve-workspace-path runtime config requested)
        offset (or (aget params "offset_bytes") 0)
        stat (await (.stat fs resolved))]
    (if (.isDirectory stat)
      (let [text (str/join "\n" (sort (array-seq (await (.readdir fs resolved)))))
            content (subs text 0 (min (count text) read-page-bytes))]
        (tool-text-result content {:path resolved :content content :truncated (< (count content) (count text))}))
      (let [page (await (read-file-page! resolved offset))
            text (cond-> (:content page)
                   (:truncated page) (str "\n\n[Continue with offset_bytes=" (:next_offset_bytes page) "]"))]
        (tool-text-result text page)))))

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
                     (tool "read" "Workspace Read" "Read a file in bounded UTF-8 byte pages or list a directory in configured workspace roots. Use next_offset_bytes to continue. Symlinks are not a containment boundary." read-params read-execute))
                   (when (allowed? "write")
                     (tool "write" "Workspace Write" "Write a file in a configured workspace root; overwrite must be explicit. Symlinks are not a containment boundary." write-params write-execute))
                   (when (allowed? "bash")
                     (tool "bash" "Bash" "Run Bash directly with the Knoxx process permissions and inherited environment. The working directory is not a sandbox. Timeout kills Bash, not necessarily its descendants." bash-params bash-execute))])))))
