#!/usr/bin/env nbb
(ns knoxx.scripts.start-server-dev
  (:require
   [clojure.string :as str]
   [nbb.core :as nbb]
   [promesa.core :as p]
   ["node:fs/promises" :as fs]
   ["node:path" :as path]
   ["node:url" :as url]))

(def backend-dir
  (path/resolve (path/dirname nbb/*file*) ".."))

(def server-entry
  (path/join backend-dir "dist-dev" "server.js"))

(def cljs-env-path
  (path/join backend-dir "dist-dev" "cljs-runtime" "cljs_env.js"))

(def shadow-token-path
  (path/join backend-dir ".shadow-cljs" "server.token"))

(def shadow-url
  (or (aget js/process.env "KNOXX_SHADOW_URL") "http://127.0.0.1:9630"))

(def build-wait-ms
  (js/Number (or (aget js/process.env "KNOXX_SHADOW_BUILD_WAIT_MS") 90000)))

(def poll-ms
  (js/Number (or (aget js/process.env "KNOXX_SHADOW_POLL_MS") 500)))

(def started-at-ms
  (.now js/Date))

(defn log!
  [message]
  (js/console.log (str "[knoxx-backend-dev] " message)))

(defn warn!
  [message]
  (js/console.warn (str "[knoxx-backend-dev] " message)))

(defn sleep
  [ms]
  (js/Promise. (fn [resolve _reject] (js/setTimeout resolve ms))))

(defn shadow-server-ready?
  []
  (-> (p/let [response (js/fetch shadow-url #js {:signal (.timeout js/AbortSignal 1000)})]
        (< (.-status response) 500))
      (.catch (fn [_err] false))))

(defn read-build-artifact
  []
  (-> (p/let [stat (fs/stat server-entry)
              source (fs/readFile server-entry "utf8")]
        #js {:stat stat :source source})
      (.catch (fn [_err] nil))))

(defn artifact-runnable?
  [artifact]
  (boolean
   (and artifact
        (.isFile (.-stat artifact))
        (str/includes? (.-source artifact) "knoxx.backend.entrypoint.init")
        (str/includes? (.-source artifact) "shadow.cljs.devtools.client.node_esm"))))

(defn read-shadow-token
  []
  (p/let [token (fs/readFile shadow-token-path "utf8")]
    (str/trim token)))

(def closure-defines-pattern
  #"globalThis\.CLOSURE_DEFINES\s*=\s*([\s\S]*?)\n;\n")

(defn parse-closure-defines
  [source]
  (when-let [match (.match source closure-defines-pattern)]
    (try
      (js/JSON.parse (aget match 1))
      (catch :default _err
        nil))))

(defn define-value
  [defines k]
  (when defines
    (aget defines k)))

(defn live-watch-defines?
  [defines]
  (boolean
   (and defines
        (true? (define-value defines "shadow.cljs.devtools.client.env.enabled"))
        (true? (define-value defines "shadow.cljs.devtools.client.env.autoload"))
        (= "server-dev" (define-value defines "shadow.cljs.devtools.client.env.build_id"))
        (pos? (js/Number (define-value defines "shadow.cljs.devtools.client.env.worker_client_id")))
        (seq (str (define-value defines "shadow.cljs.devtools.client.env.server_token")))
        (not= "missing" (define-value defines "shadow.cljs.devtools.client.env.server_token")))))

(defn assoc-define!
  [defines k v]
  (aset defines k v)
  defines)

(defn fallback-defines
  [current-defines token]
  (let [defines (js/Object.assign #js {} (or current-defines #js {}))]
    (doto defines
      (assoc-define! "goog.DEBUG" true)
      (assoc-define! "goog.LOCALE" "en")
      (assoc-define! "goog.TRANSPILE" "never")
      (assoc-define! "goog.ENABLE_DEBUG_LOADER" false)
      (assoc-define! "shadow.cljs.devtools.client.env.enabled" true)
      (assoc-define! "shadow.cljs.devtools.client.env.autoload" true)
      (assoc-define! "shadow.cljs.devtools.client.env.build_id" "server-dev")
      (assoc-define! "shadow.cljs.devtools.client.env.worker_client_id"
                     (if (pos? (js/Number (define-value current-defines "shadow.cljs.devtools.client.env.worker_client_id")))
                       (define-value current-defines "shadow.cljs.devtools.client.env.worker_client_id")
                       1))
      (assoc-define! "shadow.cljs.devtools.client.env.server_host"
                     (or (define-value current-defines "shadow.cljs.devtools.client.env.server_host") "localhost"))
      (assoc-define! "shadow.cljs.devtools.client.env.server_port"
                     (or (define-value current-defines "shadow.cljs.devtools.client.env.server_port") 9630))
      (assoc-define! "shadow.cljs.devtools.client.env.server_token"
                     (or (define-value current-defines "shadow.cljs.devtools.client.env.server_token") token))
      (assoc-define! "shadow.cljs.devtools.client.env.ignore_warnings" true)
      (assoc-define! "shadow.cljs.devtools.client.env.module_format"
                     (or (define-value current-defines "shadow.cljs.devtools.client.env.module_format") "goog")))))

(defn patch-shadow-devtools-defines!
  []
  (p/let [source (fs/readFile cljs-env-path "utf8")]
    (let [current-defines (parse-closure-defines source)]
      (if (live-watch-defines? current-defines)
        (log! "using shadow-cljs watch-provided devtools defines for server-dev runtime")
        (p/let [token (read-shadow-token)]
          (let [defines (fallback-defines current-defines token)
                replacement (str "globalThis.CLOSURE_DEFINES = \n" (js/JSON.stringify defines) "\n;\n")
                patched (.replace source closure-defines-pattern replacement)]
            (when (= patched source)
              (throw (js/Error. (str "Unable to patch shadow-cljs devtools defines in " cljs-env-path))))
            (p/let [_ (fs/writeFile cljs-env-path patched)]
              (warn! "patched fallback shadow-cljs devtools defines; prefer restarting knoxx-shadow if hot reload does not attach"))))))))

(defn wait-for-shadow-and-build!
  []
  (let [deadline (+ started-at-ms build-wait-ms)
        state #js {:sawShadow false :sawRunnableArtifact false}]
    (log! (str "waiting for shadow-cljs dev server at " shadow-url))
    (letfn [(step! []
              (if (>= (.now js/Date) deadline)
                (cond
                  (not (.-sawShadow state))
                  (js/Promise.reject (js/Error. (str "Timed out waiting for shadow-cljs dev server at " shadow-url)))

                  (not (.-sawRunnableArtifact state))
                  (js/Promise.reject (js/Error. (str "Timed out waiting for runnable " server-entry)))

                  :else
                  (do
                    (warn! "timed out waiting for a fresh shadow artifact; continuing with existing runnable dist-dev/server.js")
                    nil))
                (p/let [result (js/Promise.all #js [(shadow-server-ready?) (read-build-artifact)])]
                  (let [shadow-ready (aget result 0)
                        artifact (aget result 1)
                        runnable (artifact-runnable? artifact)]
                    (when shadow-ready
                      (aset state "sawShadow" true))
                    (when runnable
                      (aset state "sawRunnableArtifact" true))
                    (if (and shadow-ready runnable)
                      (let [fresh-enough (>= (.-mtimeMs (.-stat artifact)) (- started-at-ms 2000))]
                        (cond
                          fresh-enough
                          (log! "shadow-cljs is ready and dist-dev/server.js was freshly produced")

                          (> (- (.now js/Date) started-at-ms) 10000)
                          (warn! "using existing dist-dev/server.js; shadow-cljs is already ready but did not rewrite the artifact")

                          :else
                          (-> (sleep poll-ms) (.then step!))))
                      (-> (sleep poll-ms) (.then step!)))))))]
      (step!))))

(defn dry-run?
  []
  (= "1" (aget js/process.env "KNOXX_BACKEND_DEV_DRY_RUN")))

(defn main!
  []
  (p/let [_ (wait-for-shadow-and-build!)
          _ (patch-shadow-devtools-defines!)]
    (if (dry-run?)
      (log! "dry run complete; not importing dist-dev/server.js")
      (let [server-url (.-href (url/pathToFileURL server-entry))]
        (log! (str "importing " server-entry))
        (js/import server-url)))))

(-> (main!)
    (.catch (fn [err]
              (js/console.error "[knoxx-backend-dev] unhandled rejection" err)
              (set! (.-exitCode js/process) 1))))
