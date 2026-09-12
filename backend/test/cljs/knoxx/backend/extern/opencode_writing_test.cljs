(ns knoxx.backend.extern.opencode-writing-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.opencode-writing :as sut]))

(defn- fixture! [body]
  (let [root (.mkdtempSync fs (.join path (.tmpdir os) "knoxx-opencode-fixture-"))
        binary (.join path root "client")
        requests (.join path root "requests")]
    (.writeFileSync fs binary (str "#!" (.-execPath js/process) "\n" body) #js {:mode 448})
    {:root root :binary binary :requests requests}))

(defn- config [fixture]
  {:wiki-model-opencode-binary (:binary fixture)
   :wiki-model-opencode-directory (:requests fixture)})

(def ^:private request {:model "opencode/big-pickle" :system-prompt "Only the literal input."
                       :prompt "Hello, world." :timeout-ms 3000})

(def ^:private success-source
  "let input='';process.stdin.setEncoding('utf8');process.stdin.on('data',s=>input+=s);process.stdin.on('end',()=>{const e=process.env;const out={input,cwd:process.cwd(),home:e.HOME,configHome:e.XDG_CONFIG_HOME,secret:e.FORESIGHT_PRIVATE_FIXTURE_SECRET??null,nodeOptions:e.NODE_OPTIONS??null,config:JSON.parse(e.OPENCODE_CONFIG_CONTENT),argv:process.argv.slice(2)};console.log(JSON.stringify({type:'text',sessionID:'fixture',part:{text:JSON.stringify(out)}}));console.log(JSON.stringify({type:'step_finish',sessionID:'fixture',part:{reason:'stop'}}));});")

(deftest ^:async process-isolated-input-environment-and-cleanup
  (let [fixture (fixture! success-source)
        previous (aget (.-env js/process) "FORESIGHT_PRIVATE_FIXTURE_SECRET")]
    (aset (.-env js/process) "FORESIGHT_PRIVATE_FIXTURE_SECRET" "must-not-cross")
    (try
      (let [result (await (sut/complete! (config fixture) request))
            seen (js->clj (js/JSON.parse (:content result)) :keywordize-keys true)]
        (is (= "Hello, world." (:input seen)))
        (is (= (:cwd seen) (:home seen)))
        (is (nil? (:secret seen)))
        (is (nil? (:nodeOptions seen)))
        (is (= "deny" (get-in seen [:config :permission :*])))
        (is (= "Only the literal input." (get-in seen [:config :agent :cms :prompt])))
        (is (= ["run" "--pure" "--model" "opencode/big-pickle" "--agent" "cms" "--format" "json"] (:argv seen)))
        (is (not (.existsSync fs (:cwd seen))))
        (is (empty? (array-seq (.readdirSync fs (:requests fixture))))))
      (finally
        (if previous (aset (.-env js/process) "FORESIGHT_PRIVATE_FIXTURE_SECRET" previous)
            (js-delete (.-env js/process) "FORESIGHT_PRIVATE_FIXTURE_SECRET"))
        (.rmSync fs (:root fixture) #js {:recursive true :force true})))))

(deftest ^:async process-failures-never-return-partial-success
  (doseq [[source timeout expected]
          [["process.stdin.resume();setTimeout(()=>{},10000);" 150 "writing_model_timeout"]
           ["process.stdin.resume();console.log('not json');" 3000 "writing_model_invalid_output"]
           ["process.stdin.resume();process.stdout.write('x'.repeat(3*1024*1024));" 3000 "writing_model_output_limit"]
           ["process.stdin.resume();process.exitCode=2;" 3000 "writing_model_process_failed"]]]
    (let [fixture (fixture! source)]
      (try
        (try (await (sut/complete! (config fixture) (assoc request :timeout-ms timeout)))
             (is false "Failed process returned success")
             (catch :default error (is (= expected (:code (ex-data error))))))
        (is (empty? (array-seq (.readdirSync fs (:requests fixture)))))
        (finally (.rmSync fs (:root fixture) #js {:recursive true :force true}))))))
