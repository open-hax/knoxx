(ns knoxx.backend.extern.native-music
  "Boundary for the existing native Node music engine and its execFile result.
   Files, process options and JS decoding stay here; callers exchange CLJS data."
  (:require [knoxx.backend.extern.json :as json]
            [malli.core :as m]
            ["node:child_process" :refer [execFile]]
            ["node:crypto" :refer [randomUUID]]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            ["node:util" :refer [promisify]]))

(def ^:private exec-file-async (promisify execFile))

(def request-contract
  [:map [:spec-json [:string {:min 1}]] [:output-path [:string {:min 1}]]])

(def result-contract
  [:map
   [:ok [:= true]]
   [:outputPath [:string {:min 1}]]
   [:durationSec [:and number? [:> 0]]]
   [:sampleRate [:int {:min 1}]]
   [:channels [:int {:min 1}]]
   [:samples [:int {:min 1}]]])

(defn default-output-path
  "Return the existing workspace-relative default filename."
  []
  (str "Music/generated/" (randomUUID) ".wav"))

(defn decode-result!
  "Decode only stdout from Node's opaque {stdout, stderr} process result.
   Return validated, keywordized engine metadata or reject malformed output."
  [process-result]
  (let [result (json/parse-object (.-stdout process-result))]
    (when-not (m/validate result-contract result)
      (throw (ex-info "Native music engine stdout violates its result contract"
                      {:type :native-music/invalid-result})))
    result))

(defn ^:async generate!
  "Render through scripts/synthesize-music.mjs relative to the backend cwd.
   The request/result contracts describe CLJS data; native failures propagate."
  [{:keys [spec-json output-path] :as request}]
  (when-not (m/validate request-contract request)
    (throw (ex-info "Invalid native music generation request"
                    {:type :native-music/invalid-request})))
  (let [directory (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-music-spec-")))
        spec-path (.join path directory "spec.json")
        script-path (.resolve path (.cwd js/process) "scripts" "synthesize-music.mjs")]
    (try
      (await (.writeFile fs spec-path spec-json "utf8"))
      (let [process-result (await (exec-file-async (.-execPath js/process)
                                                 #js [script-path spec-path output-path]
                                                 #js {:timeout 120000 :maxBuffer 1048576}))]
        (decode-result! process-result))
      (finally
        (await (.rm fs directory #js {:recursive true :force true}))))))
