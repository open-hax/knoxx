(ns knoxx.frontend.pages.agents.commands-test
  "Admission and stale-response behavior of the extracted workbench commands."
  (:require [cljs.test :as t]
            [knoxx.frontend.pages.agents.commands :as commands]
            [knoxx.frontend.pages.agents.model :as model]
            [knoxx.frontend.pages.agents.runtime-model :as runtime]))

(defn- fixture [state]
  (let [current (atom state)]
    {:current current :state state :set-state #(swap! current %)}))
(defn- deferred []
  (let [complete! (atom nil) promise (js/Promise. (fn [complete _] (reset! complete! complete)))]
    {:promise promise :resolve #(@complete! %)}))

(t/deftest ^:async obsolete-contract-response-cannot-replace-a-new-selection
  (let [first-response (deferred) second-response (deferred)
        context (assoc (fixture {:selected-id "first"}) :contracts {:get (fn [id _] (get {"first" (:promise first-response) "second" (:promise second-response)} id))})
        first-load (commands/load-contract! context "first")]
    (swap! (:current context) assoc :selected-id "second")
    (let [second-load (commands/load-contract! context "second")]
      ((:resolve first-response) {:contract {:contract/id "first"} :ednText "old"})
      (await first-load)
      (t/is (nil? (:draft @(:current context))))
      (t/is (true? (:loading-contract @(:current context))))
      ((:resolve second-response) {:contract {:contract/id "second"} :ednText "new"})
      (await second-load)
      (t/is (= "new" (:edn-text @(:current context))))
      (t/is (false? (:loading-contract @(:current context)))))))

(t/deftest ^:async refused-save-preserves-the-edited-contract-and-releases-busy-state
  (let [draft {:contract/id "research" :prompts {:task "Keep this edit"}}
        calls (atom []) context (assoc (fixture {:draft draft :edn-text (pr-str draft)}) :can-save-contracts? true
                                     :contracts {:save (fn [& args] (swap! calls conj args) (js/Promise.reject (js/Error. "Store unavailable")))})]
    (await (commands/save! context))
    (t/is (= [["research" (pr-str draft) "agents"]] @calls))
    (t/is (= draft (:draft @(:current context))))
    (t/is (false? (:saving @(:current context))))
    (t/is (= {:tone :error :text "Store unavailable"} (:notice @(:current context))))))

(t/deftest ^:async denied-capabilities-do-not-call-service-clients
  (let [calls (atom []) deny #(swap! calls conj %)
        context (assoc (fixture {:draft {:contract/id "research"}})
                       :can-save-contracts? false :can-control-runtime? false
                       :contracts {:save deny} :runtime {:get deny :start deny :stop deny :reset deny :fire deny})]
    (await (commands/save! context)) (await (commands/load-runtime! context))
    (doseq [command [:start :stop :reset]] (await (commands/runtime-command! context command)))
    (await (commands/run-trigger! context "trigger-1"))
    (t/is (empty? @calls))))

(t/deftest malformed-raw-edn-preserves-the-last-structured-contract
  (let [context (fixture {:draft {:contract/id "research"}})]
    (commands/raw-change! context "{:contract/id")
    (t/is (= "{:contract/id" (:edn-text @(:current context))))
    (t/is (= {:contract/id "research"} (:draft @(:current context))))
    (t/is (string? (:parse-error @(:current context))))))

(t/deftest ^:async failed-runtime-command-releases-only-its-busy-indicator
  (let [context (assoc (fixture {:runtime-status {:runtime {:running true}}}) :can-control-runtime? true
                       :runtime {:reset #(js/Promise.reject (js/Error. "Reset refused"))})]
    (await (commands/runtime-command! context :reset))
    (t/is (false? (:resetting-runtime @(:current context))))
    (t/is (= {:runtime {:running true}} (:runtime-status @(:current context))))
    (t/is (= "Reset refused" (get-in @(:current context) [:runtime-notice :text])))))

(t/deftest trigger-target-projections-retain-agent-and-pipeline-identities
  (let [jobs (runtime/trigger-schedule-jobs [{:id "research"} {:id "review"}]
               [{:id "daily" :trigger {:kind :cron :schedule "*/15 * * * *" :target "publishing"}}]
               [{:id "publishing" :pipeline {:steps [{:contract "research"} {:contract "review"} {:contract "unknown"}]}}])]
    (t/is (= ["research" "review"] (mapv :contractSourceId jobs)))
    (t/is (= [15 15] (mapv #(get-in % [:trigger :cadenceMinutes]) jobs)))
    (t/is (every? #(= "daily" (:id %)) jobs))
    (t/is (= [:role/writer :role/reviewer]
             (get-in (model/with-selected-role-ids {:agent {:role :role/legacy}} ["writer" "reviewer" "writer"])
                     [:agent :roles])))))
