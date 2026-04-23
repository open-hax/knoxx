(ns knoxx.backend.tools.sandbox-container
  "TTL-bound docker-exec sandbox tools for isolated development work."
  (:require [clojure.string :as str]
            [knoxx.backend.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.text :refer [clip-text tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! type-optional]]))

(def ^:private sandbox-label-prefix "openhax.knoxx.sandbox")
(def ^:private sandbox-max-buffer-bytes (* 2 1024 1024))

(defn- shell-single-quote
  [value]
  (str "'" (str/replace (str (or value "")) #"'" "'\"'\"'") "'"))

(defn- base64-utf8
  [text]
  (.toString (.from js/Buffer (str (or text "")) "utf8") "base64"))

(defn- path-resolve
  [^js node-path & parts]
  (.apply (aget node-path "resolve") node-path (into-array parts)))

(defn- fs-mkdir!
  [^js node-fs path opts]
  (.mkdir node-fs path opts))

(defn- fs-read-file!
  ([^js node-fs path]
   (.readFile node-fs path))
  ([^js node-fs path encoding]
   (.readFile node-fs path encoding)))

(defn- fs-write-file!
  ([^js node-fs path content]
   (.writeFile node-fs path content))
  ([^js node-fs path content encoding]
   (.writeFile node-fs path content encoding)))

(defn- docker-bin
  [config]
  (or (:sandbox-docker-bin config) "docker"))

(defn- sandbox-container-name
  [sandbox-id]
  (str "knoxx-sandbox-" sandbox-id))

(declare sandbox-host-dir)

(defn- sandbox-metadata-path
  [runtime config sandbox-id]
  (let [node-path (aget runtime "path")]
    (path-resolve node-path (sandbox-host-dir runtime config sandbox-id) ".knoxx-sandbox.json")))

(defn- sandbox-build-context
  [runtime config]
  (let [node-path (aget runtime "path")]
    (path-resolve node-path (.cwd js/process) (or (:sandbox-build-context config) "."))))

(defn- sandbox-dockerfile
  [runtime config]
  (let [node-path (aget runtime "path")]
    (path-resolve node-path (.cwd js/process) (or (:sandbox-dockerfile config) "docker/sandbox/Dockerfile"))))

(defn- sandbox-host-dir
  [runtime config sandbox-id]
  (let [node-path (aget runtime "path")]
    (path-resolve node-path (:sandbox-root-dir config) sandbox-id)))

(defn- sandbox-workdir
  [config]
  (or (:sandbox-workdir config) "/workspace"))

(defn- sandbox-user
  [config]
  (or (:sandbox-user config) "1000:1000"))

(defn- clamp-ttl-seconds
  [config ttl-seconds]
  (let [default-ttl (max 60 (or (:sandbox-default-ttl-seconds config) 1800))
        max-ttl (max default-ttl (or (:sandbox-max-ttl-seconds config) 86400))
        requested (if (number? ttl-seconds) ttl-seconds default-ttl)]
    (-> requested int (max 60) (min max-ttl))))

(defn- normalize-sandbox-path
  [raw-path]
  (let [path (some-> raw-path str str/trim not-empty)]
    (when-not path
      (throw (js/Error. "path is required")))
    (when (or (str/starts-with? path "/")
              (str/includes? path "../")
              (= path "..")
              (str/starts-with? path "../"))
      (throw (js/Error. "sandbox paths must stay relative to the sandbox workdir")))
    path))

(defn- exec-file-result!
  [exec-file-async bin args opts]
  (-> (exec-file-async bin (clj->js args) (clj->js (merge {:maxBuffer sandbox-max-buffer-bytes} opts)))
      (.then (fn [result]
               {:ok true
                :exitCode 0
                :stdout (str (or (aget result "stdout") ""))
                :stderr (str (or (aget result "stderr") ""))}))
      (.catch (fn [err]
                {:ok false
                 :exitCode (or (aget err "code") 1)
                 :stdout (str (or (aget err "stdout") ""))
                 :stderr (str (or (aget err "stderr") ""))
                 :error (or (.-message err) (str err))}))))

(defn- docker-command!
  [runtime config args opts]
  (let [exec-file-async (aget runtime "execFileAsync")]
    (when-not exec-file-async
      (throw (js/Error. "execFileAsync runtime dependency is not available")))
    (exec-file-result! exec-file-async (docker-bin config) args opts)))

(defn- sandbox-metadata!
  [runtime config sandbox-id]
  (let [node-fs (aget runtime "fs")
        metadata-path (sandbox-metadata-path runtime config sandbox-id)]
    (-> (fs-read-file! node-fs metadata-path "utf8")
        (.then (fn [text]
                 (js->clj (.parse js/JSON (str text)) :keywordize-keys true)))
        (.catch (fn [_] nil)))))

(defn- write-sandbox-metadata!
  [runtime config sandbox-id metadata]
  (let [node-fs (aget runtime "fs")
        metadata-path (sandbox-metadata-path runtime config sandbox-id)]
    (-> (fs-write-file! node-fs metadata-path (.stringify js/JSON (clj->js metadata) nil 2) "utf8")
        (.then (fn [] metadata)))))

(defn- refresh-sandbox-ttl!
  [runtime config sandbox-id]
  (-> (sandbox-metadata! runtime config sandbox-id)
      (.then (fn [metadata]
               (let [created-at-ms (or (:createdAtMs metadata)
                                       (when-let [created-at (:createdAt metadata)]
                                         (.parse js/Date (str created-at)))
                                       (.now js/Date))
                     max-ttl-seconds (clamp-ttl-seconds config (:sandbox-max-ttl-seconds config))
                     max-expires-at (or (:maxExpiresAt metadata)
                                        (+ created-at-ms (* 1000 max-ttl-seconds)))
                     ttl-seconds (clamp-ttl-seconds config (:ttlSeconds metadata))
                     requested-expires-at (+ (.now js/Date) (* 1000 ttl-seconds))
                     expires-at (min requested-expires-at max-expires-at)]
                 (write-sandbox-metadata! runtime config sandbox-id (merge metadata {:createdAtMs created-at-ms
                                                                                      :maxExpiresAt max-expires-at
                                                                                      :ttlSeconds ttl-seconds
                                                                                      :expiresAt expires-at})))))))

(defn- ensure-sandbox-image!
  [runtime config image]
  (-> (docker-command! runtime config ["image" "inspect" image] {:timeout 30000})
      (.then (fn [{:keys [ok stderr error]}]
               (if ok
                 true
                 (let [dockerfile (sandbox-dockerfile runtime config)
                       build-context (sandbox-build-context runtime config)]
                   (if (or (str/includes? stderr "No such image")
                           (str/includes? stderr "No such object")
                           (str/includes? (str error) "No such image"))
                     (if (= (str image) (str (:sandbox-image config)))
                       (-> (docker-command! runtime config ["build" "-t" image "-f" dockerfile build-context] {:timeout 1800000})
                           (.then (fn [{build-ok :ok build-stderr :stderr build-error :error}]
                                    (when-not build-ok
                                      (throw (js/Error. (str "docker build failed: " (or build-error build-stderr)))))
                                    true)))
                       ;; Non-default images should be pulled by `docker run`.
                       true)
                     (throw (js/Error. (str "docker image inspect failed: " (or error stderr)))))))))))

(defn- sandbox-inspect!
  [runtime config sandbox-id]
  (let [container-name (sandbox-container-name sandbox-id)]
    (-> (docker-command! runtime config ["inspect" container-name] {:timeout 30000})
        (.then (fn [{:keys [ok stdout stderr error]}]
                 (if-not ok
                   (if (or (str/includes? stderr "No such object")
                           (str/includes? stderr "No such container"))
                     nil
                     (throw (js/Error. (str "docker inspect failed: " (or error stderr)))))
                   (let [items (.parse js/JSON stdout)
                         item (aget items 0)
                         labels (js->clj (or (aget item "Config" "Labels") #js {}) :keywordize-keys false)
                         state (js->clj (or (aget item "State") #js {}) :keywordize-keys true)
                         mounts (js->clj (or (aget item "Mounts") #js []) :keywordize-keys true)
                         label-expires-at (js/parseInt (str (or (get labels (str sandbox-label-prefix ".expiresAt")) "0")) 10)]
                     (-> (sandbox-metadata! runtime config sandbox-id)
                         (.then (fn [metadata]
                                  (let [expires-at (or (:expiresAt metadata) label-expires-at 0)]
                                    {:sandboxId sandbox-id
                                     :containerName container-name
                                     :image (or (aget item "Config" "Image") "")
                                     :hostDir (or (:Source (first mounts)) "")
                                     :workdir (or (aget item "Config" "WorkingDir") (sandbox-workdir config))
                                     :createdAt (or (:createdAt metadata) (aget item "Created") "")
                                     :running (boolean (:Running state))
                                     :status (or (:Status state) "unknown")
                                     :exitCode (or (:ExitCode state) 0)
                                     :expiresAt expires-at
                                     :ttlSeconds (if (pos? expires-at)
                                                   (max 0 (int (/ (- expires-at (.now js/Date)) 1000)))
                                                   0)})))))))))))


(defn- sandbox-destroy!
  [runtime config sandbox-id]
  (let [node-fs (aget runtime "fs")
        host-dir (sandbox-host-dir runtime config sandbox-id)
        container-name (sandbox-container-name sandbox-id)]
    (-> (docker-command! runtime config ["rm" "-f" container-name] {:timeout 30000})
        (.then (fn [_]
                 (.rm node-fs host-dir #js {:recursive true :force true})))
        (.catch (fn [_]
                  (.rm node-fs host-dir #js {:recursive true :force true})))
        (.then (fn [_]
                 {:sandboxId sandbox-id
                  :containerName container-name
                  :destroyed true
                  :hostDir host-dir})))))

(defn- ensure-live-sandbox!
  [runtime config sandbox-id]
  (-> (sandbox-inspect! runtime config sandbox-id)
      (.then (fn [info]
               (when-not info
                 (throw (js/Error. (str "Sandbox not found: " sandbox-id))))
               (if (and (pos? (:expiresAt info)) (> (.now js/Date) (:expiresAt info)))
                 (-> (sandbox-destroy! runtime config sandbox-id)
                     (.then (fn [_]
                              (throw (js/Error. (str "Sandbox expired and was destroyed: " sandbox-id))))))
                 (-> (refresh-sandbox-ttl! runtime config sandbox-id)
                     (.then (fn [metadata]
                              (assoc info
                                     :expiresAt (:expiresAt metadata)
                                     :ttlSeconds (max 0 (int (/ (- (:expiresAt metadata) (.now js/Date)) 1000))))))))))))

(defn sandbox-create!
  [runtime config {:keys [ttl-seconds image]}]
  (let [node-fs (aget runtime "fs")
        node-crypto (aget runtime "crypto")
        sandbox-id (.randomUUID node-crypto)
        ttl (clamp-ttl-seconds config ttl-seconds)
        created-at-ms (.now js/Date)
        max-ttl-seconds (clamp-ttl-seconds config (:sandbox-max-ttl-seconds config))
        max-expires-at (+ created-at-ms (* 1000 max-ttl-seconds))
        expires-at (min (+ created-at-ms (* ttl 1000)) max-expires-at)
        host-dir (sandbox-host-dir runtime config sandbox-id)
        container-name (sandbox-container-name sandbox-id)
        workdir (sandbox-workdir config)
        image (or (some-> image str str/trim not-empty)
                  (:sandbox-image config))
        keepalive-cmd "trap 'exit 0' TERM INT; while true; do sleep 3600; done"]
    (-> (ensure-sandbox-image! runtime config image)
        (.then (fn [_]
                 (fs-mkdir! node-fs host-dir #js {:recursive true})))
        (.then (fn []
                 (docker-command!
                  runtime
                  config
                  ["run" "-d" "--rm"
                   "--name" container-name
                   "--user" (sandbox-user config)
                   "--workdir" workdir
                   "-e" (str "HOME=" workdir)
                   "--label" (str sandbox-label-prefix "=true")
                   "--label" (str sandbox-label-prefix ".id=" sandbox-id)
                   "--label" (str sandbox-label-prefix ".expiresAt=" expires-at)
                   "-v" (str host-dir ":" workdir)
                   image
                   "sh" "-lc" keepalive-cmd]
                  {:timeout 180000})))
        (.then (fn [{:keys [ok stderr error]}]
                 (when-not ok
                   (throw (js/Error. (str "docker run failed: " (or error stderr)))))
                  (-> (write-sandbox-metadata! runtime config sandbox-id {:sandboxId sandbox-id
                                                                          :image image
                                                                          :createdAtMs created-at-ms
                                                                          :maxExpiresAt max-expires-at
                                                                          :ttlSeconds ttl
                                                                          :createdAt (.toISOString (js/Date.))
                                                                          :expiresAt expires-at})
                      (.then (fn [_]
                               (ensure-live-sandbox! runtime config sandbox-id)))))))))

(defn- sandbox-exec!
  [runtime config sandbox-id command timeout-ms]
  (-> (ensure-live-sandbox! runtime config sandbox-id)
      (.then (fn [info]
               (-> (docker-command! runtime config
                                   ["exec" "-w" (:workdir info) (:containerName info) "sh" "-lc" command]
                                   {:timeout (or timeout-ms 120000)})
                   (.then (fn [result]
                            (assoc result
                                   :sandboxId sandbox-id
                                   :containerName (:containerName info)
                                   :workdir (:workdir info)
                                   :ttlSeconds (:ttlSeconds info)))))))))

(defn create-sandbox-custom-tools
  ([runtime config] (create-sandbox-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [Type (aget runtime "Type")
         allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))
         create-params (.Object Type #js {:ttl_seconds (type-optional Type (.Number Type #js {:description "Requested sandbox lifetime in seconds." :minimum 60 :maximum 86400}))
                                          :image (type-optional Type (.String Type #js {:description "Optional container image override."}))})
         id-params (.Object Type #js {:sandbox_id (.String Type #js {:description "Sandbox id returned by sandbox_container.create."})})
         exec-params (.Object Type #js {:sandbox_id (.String Type #js {:description "Sandbox id returned by sandbox_container.create."})
                                        :command (.String Type #js {:description "Shell command to execute inside the sandbox workdir."})
                                        :timeout_ms (type-optional Type (.Number Type #js {:description "Command timeout in milliseconds." :minimum 1000 :maximum 300000}))})
         read-params (.Object Type #js {:sandbox_id (.String Type #js {:description "Sandbox id returned by sandbox_container.create."})
                                        :path (.String Type #js {:description "Relative path inside the sandbox workdir."})
                                        :max_chars (type-optional Type (.Number Type #js {:description "Maximum characters to return." :minimum 200 :maximum 20000}))})
         write-params (.Object Type #js {:sandbox_id (.String Type #js {:description "Sandbox id returned by sandbox_container.create."})
                                         :path (.String Type #js {:description "Relative path inside the sandbox workdir."})
                                         :content (.String Type #js {:description "UTF-8 text content to write."})})
         commit-params (.Object Type #js {:sandbox_id (.String Type #js {:description "Sandbox id returned by sandbox_container.create."})
                                          :message (.String Type #js {:description "Git commit message."})})]
     (letfn [(create-execute [_tool-call-id params a b c]
               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))]
                 (maybe-tool-update! on-update "Creating sandbox container…")
                 (-> (sandbox-create! runtime config {:ttl-seconds (aget params "ttl_seconds")
                                                      :image (aget params "image")})
                     (.then (fn [result]
                              (tool-text-result
                               (str "Created sandbox " (:sandboxId result)
                                    " using " (:image result)
                                    " with ~" (:ttlSeconds result) "s remaining.")
                               result))))))

             (status-execute [_tool-call-id params a b c]
               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                     sandbox-id (or (aget params "sandbox_id") (aget params "sandboxId") "")]
                 (maybe-tool-update! on-update (str "Inspecting sandbox " sandbox-id "…"))
                 (-> (ensure-live-sandbox! runtime config sandbox-id)
                     (.then (fn [result]
                              (if result
                                (tool-text-result
                                 (str "Sandbox " sandbox-id " status=" (:status result)
                                      ", ttl=" (:ttlSeconds result) "s")
                                 result)
                                (tool-text-result
                                 (str "Sandbox " sandbox-id " not found.")
                                 {:sandboxId sandbox-id :exists false})))))))

             (exec-execute [_tool-call-id params a b c]
               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                     sandbox-id (or (aget params "sandbox_id") (aget params "sandboxId") "")
                     command (or (aget params "command") "")
                     timeout-ms (aget params "timeout_ms")]
                 (when (str/blank? sandbox-id)
                   (throw (js/Error. "sandbox_id is required")))
                 (when (str/blank? (str/trim command))
                   (throw (js/Error. "command is required")))
                 (maybe-tool-update! on-update (str "Executing in sandbox " sandbox-id "…"))
                 (-> (sandbox-exec! runtime config sandbox-id command timeout-ms)
                     (.then (fn [result]
                              (tool-text-result
                               (str "Sandbox exec exit=" (:exitCode result)
                                    (when-not (str/blank? (:stdout result))
                                      (str "

stdout:
" (first (clip-text (:stdout result) 8000))))
                                    (when-not (str/blank? (:stderr result))
                                      (str "

stderr:
" (first (clip-text (:stderr result) 8000)))))
                               result))))))

             (read-execute [_tool-call-id params a b c]
               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                     sandbox-id (or (aget params "sandbox_id") (aget params "sandboxId") "")
                     path (normalize-sandbox-path (aget params "path"))
                     max-chars (max 200 (min 20000 (or (aget params "max_chars") 6000)))
                     command (str "cat " (shell-single-quote path))]
                 (maybe-tool-update! on-update (str "Reading sandbox file " path "…"))
                 (-> (sandbox-exec! runtime config sandbox-id command 60000)
                     (.then (fn [result]
                              (let [[content _] (clip-text (:stdout result) max-chars)]
                                (tool-text-result
                                 (str "Sandbox file " path "

" content)
                                 (assoc result :path path :content content))))))))

             (write-execute [_tool-call-id params a b c]
               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                     sandbox-id (or (aget params "sandbox_id") (aget params "sandboxId") "")
                     path (normalize-sandbox-path (aget params "path"))
                     content (or (aget params "content") "")
                     encoded (base64-utf8 content)
                     command (str "mkdir -p $(dirname -- " (shell-single-quote path) ") && printf %s "
                                  (shell-single-quote encoded)
                                  " | base64 -d > " (shell-single-quote path))]
                 (maybe-tool-update! on-update (str "Writing sandbox file " path "…"))
                 (-> (sandbox-exec! runtime config sandbox-id command 60000)
                     (.then (fn [result]
                              (tool-text-result
                               (str "Wrote sandbox file " path)
                               (assoc result :path path :bytes (count content))))))))

             (commit-execute [_tool-call-id params a b c]
               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                     sandbox-id (or (aget params "sandbox_id") (aget params "sandboxId") "")
                     message (or (aget params "message") "sandbox update")
                     command (str "if [ ! -d .git ]; then git init -q .; fi && "
                                  "git config user.email sandbox@knoxx.local && "
                                  "git config user.name 'Knoxx Sandbox' && "
                                  "git add -A && "
                                  "if git diff --cached --quiet; then echo 'No staged changes to commit'; "
                                  "else git commit -m " (shell-single-quote message) "; fi && "
                                  "git status --short --branch")]
                 (maybe-tool-update! on-update (str "Committing sandbox workspace for " sandbox-id "…"))
                 (-> (sandbox-exec! runtime config sandbox-id command 120000)
                     (.then (fn [result]
                              (tool-text-result
                               (str "Sandbox commit exit=" (:exitCode result)
                                    (when-not (str/blank? (:stdout result))
                                      (str "

" (first (clip-text (:stdout result) 8000)))))
                               result))))))

             (destroy-execute [_tool-call-id params a b c]
               (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
                     sandbox-id (or (aget params "sandbox_id") (aget params "sandboxId") "")]
                 (maybe-tool-update! on-update (str "Destroying sandbox " sandbox-id "…"))
                 (-> (sandbox-destroy! runtime config sandbox-id)
                     (.then (fn [result]
                              (tool-text-result (str "Destroyed sandbox " sandbox-id)
                                                result))))))]
       (clj->js
        (vec
         (remove nil?
                 [(when (allowed? "sandbox_container.create")
                    (doto (js-obj)
                      (aset "name" "sandbox_container.create")
                      (aset "label" "Sandbox Create")
                      (aset "description" "Create a TTL-bound sandbox container for isolated development work.")
                      (aset "promptSnippet" "Create a temporary docker-backed sandbox when you need coding or shell access without touching the live Knoxx source tree.")
                      (aset "parameters" create-params)
                      (aset "execute" create-execute)))
                  (when (allowed? "sandbox_container.status")
                    (doto (js-obj)
                      (aset "name" "sandbox_container.status")
                      (aset "label" "Sandbox Status")
                      (aset "description" "Inspect sandbox container runtime status and remaining TTL.")
                      (aset "parameters" id-params)
                      (aset "execute" status-execute)))
                  (when (allowed? "sandbox_container.exec")
                    (doto (js-obj)
                      (aset "name" "sandbox_container.exec")
                      (aset "label" "Sandbox Exec")
                      (aset "description" "Execute a shell command inside the sandbox workdir using docker exec.")
                      (aset "parameters" exec-params)
                      (aset "execute" exec-execute)))
                  (when (allowed? "sandbox_container.read")
                    (doto (js-obj)
                      (aset "name" "sandbox_container.read")
                      (aset "label" "Sandbox Read")
                      (aset "description" "Read a UTF-8 text file from the sandbox workdir.")
                      (aset "parameters" read-params)
                      (aset "execute" read-execute)))
                  (when (allowed? "sandbox_container.write")
                    (doto (js-obj)
                      (aset "name" "sandbox_container.write")
                      (aset "label" "Sandbox Write")
                      (aset "description" "Write a UTF-8 text file into the sandbox workdir.")
                      (aset "parameters" write-params)
                      (aset "execute" write-execute)))
                  (when (allowed? "sandbox_container.commit")
                    (doto (js-obj)
                      (aset "name" "sandbox_container.commit")
                      (aset "label" "Sandbox Commit")
                      (aset "description" "Create a git commit inside the sandbox workdir.")
                      (aset "parameters" commit-params)
                      (aset "execute" commit-execute)))
                  (when (allowed? "sandbox_container.destroy")
                    (doto (js-obj)
                      (aset "name" "sandbox_container.destroy")
                      (aset "label" "Sandbox Destroy")
                      (aset "description" "Destroy a sandbox container and remove its temporary workspace.")
                      (aset "parameters" id-params)
                      (aset "execute" destroy-execute)))])))))))
