(ns knoxx.backend.extern.opencode-writing
  "Owned process, configuration and NDJSON boundary for tool-free OpenCode writing."
  (:require ["node:child_process" :as child-process]
            ["node:fs" :as fs]
            ["node:path" :as path]
            [clojure.string :as str]
            [knoxx.backend.law.writing-model :as law]))

(def ^:private allowed-models #{"opencode/big-pickle" "opencode/mimo-v2.5-free"})
(def ^:private output-limit (* 2 1024 1024))

(defn- require-options! [config model prompt system-prompt]
  (when-not (and (contains? allowed-models model)
                (string? prompt) (<= 1 (count prompt) 300000)
                (string? system-prompt) (<= (count system-prompt) 200000)
                (every? #(and (string? %) (not (str/blank? %)) (.isAbsolute path %))
                        [(:wiki-model-opencode-binary config)
                         (:wiki-model-opencode-directory config)]))
    (throw (law/failure "writing_model_configuration_invalid"
                        "OpenCode writing requires an allowed model and absolute owned paths")))
  (when (.existsSync fs "/etc/opencode")
    (throw (law/failure "writing_model_managed_config"
                        "Managed OpenCode configuration prevents isolated writing"))))

(defn- inherited-environment []
  (into {} (keep (fn [entry]
                  (let [k (aget entry 0)]
                    (when (re-matches #"PATH|TMPDIR|LANG|LC_ALL|HTTPS?_PROXY|https?_proxy|ALL_PROXY|all_proxy|NO_PROXY|no_proxy|SSL_CERT_FILE|SSL_CERT_DIR|NODE_EXTRA_CA_CERTS|CURL_CA_BUNDLE" k)
                      [k (aget entry 1)]))))
        (array-seq (.entries js/Object (.-env js/process)))))

(defn- isolated-config [model system-prompt]
  {:model model :small_model model :enabled_providers ["opencode"]
   :share "disabled" :permission {"*" "deny"} :default_agent "cms"
   :agent {:cms {:mode "primary" :steps 2 :permission {"*" "deny"}
                 :prompt system-prompt :options {:maxTokens 2048}}}})

(defn- environment [config directory model system-prompt]
  (clj->js
   (cond-> (merge (inherited-environment)
                  {"HOME" directory
                   "XDG_CONFIG_HOME" (.join path directory "config")
                   "XDG_DATA_HOME" (.join path directory "data")
                   "XDG_STATE_HOME" (.join path directory "state")
                   "XDG_CACHE_HOME" (.join path directory "cache")
                   "OPENCODE_CONFIG_CONTENT" (js/JSON.stringify (clj->js (isolated-config model system-prompt)))
                   "OPENCODE_PERMISSION" "{\"*\":\"deny\"}"
                   "OPENCODE_DISABLE_PROJECT_CONFIG" "1"
                   "OPENCODE_DISABLE_EXTERNAL_SKILLS" "1"
                   "OPENCODE_DISABLE_CLAUDE_CODE" "1"
                   "OPENCODE_DISABLE_DEFAULT_PLUGINS" "1"
                   "OPENCODE_DISABLE_MODELS_FETCH" "1"
                   "OPENCODE_DISABLE_AUTOUPDATE" "1"})
     (:wiki-model-opencode-models-path config)
     (assoc "OPENCODE_MODELS_PATH" (:wiki-model-opencode-models-path config)))))

(defn- stop! [child]
  (try (if (= "linux" (.-platform js/process))
         (.kill js/process (- (.-pid child)) "SIGKILL")
         (.kill child "SIGKILL"))
       (catch :default _ nil)))

(defn- fail-process! [child state code message]
  (swap! state #(if (:error %) % (assoc % :error (law/failure code message))))
  (stop! child))

(defn- consume! [child state channel chunk]
  (let [size (.-length (.from js/Buffer chunk "utf8"))]
    (if (> (+ (:bytes @state) size) output-limit)
      (fail-process! child state "writing_model_output_limit" "OpenCode output exceeded its bound")
      (swap! state #(-> % (update :bytes + size) (update channel str chunk))))))

(defn- decode-events [stdout]
  (try
    (mapv #(js->clj (.parse js/JSON %) :keywordize-keys true)
          (remove str/blank? (str/split-lines stdout)))
    (catch :default _
      (throw (law/failure "writing_model_invalid_output" "OpenCode returned malformed NDJSON")))))

(defn- settle! [state model code signal resolve reject]
  (try
    (cond
      (:error @state) (reject (:error @state))
      (or (not= code 0) signal)
      (reject (law/failure "writing_model_process_failed" "OpenCode did not exit successfully"))
      :else (resolve (law/transcript-result (decode-events (:stdout @state)) model)))
    (catch :default error (reject error))))

(defn- run-process! [config directory model system-prompt prompt timeout-ms]
  (js/Promise.
   (fn [resolve reject]
     (let [child (.spawn child-process (:wiki-model-opencode-binary config)
                         #js ["run" "--pure" "--model" model "--agent" "cms" "--format" "json"]
                         #js {:cwd directory :env (environment config directory model system-prompt)
                              :detached (= "linux" (.-platform js/process))
                              :stdio #js ["pipe" "pipe" "pipe"]})
           state (atom {:bytes 0 :stdout "" :stderr ""})
           timer (js/setTimeout #(fail-process! child state "writing_model_timeout"
                                                "OpenCode writing timed out") timeout-ms)]
       (doseq [[stream channel] [[(.-stdout child) :stdout] [(.-stderr child) :stderr]]]
         (.setEncoding stream "utf8")
         (.on stream "data" #(consume! child state channel %)))
       (.on child "error" #(swap! state assoc :error
                                  (law/failure "writing_model_process_failed" "OpenCode could not start")))
       (.on (.-stdin child) "error" #(fail-process! child state "writing_model_process_failed"
                                                     "OpenCode could not receive its input"))
       (.on child "close" (fn [code signal]
                            (js/clearTimeout timer)
                            (settle! state model code signal resolve reject)))
       (.end (.-stdin child) prompt)))))

(defn ^:async complete!
  "Execute one isolated, bounded, tool-free turn and remove its private state."
  [config {:keys [model system-prompt prompt timeout-ms]}]
  (require-options! config model prompt system-prompt)
  (let [timeout (or timeout-ms (:wiki-model-timeout-ms config) 180000)
        parent (:wiki-model-opencode-directory config)]
    (when-not (and (number? timeout) (js/Number.isFinite timeout) (pos? timeout))
      (throw (law/failure "writing_model_configuration_invalid" "OpenCode timeout must be positive")))
    (.mkdirSync fs parent #js {:recursive true :mode 448})
    (let [directory (.mkdtempSync fs (.join path parent "writing-"))]
      (try
        (await (run-process! config directory model system-prompt prompt (min timeout 180000)))
        (finally (.rmSync fs directory #js {:recursive true :force true}))))))
