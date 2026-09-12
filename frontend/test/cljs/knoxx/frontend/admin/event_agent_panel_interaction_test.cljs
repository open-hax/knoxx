(ns knoxx.frontend.admin.event-agent-panel-interaction-test
  "Exercise the extracted runtime editor through its real native HTTP boundary."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.admin.event-agents-panel :as panel]
            [knoxx.frontend.api.event-agents :as api]))

(def ^:private job {:id "job-1" :name "Research agent" :description "" :enabled true
                   :trigger {:kind "cron" :cadenceMinutes 10 :eventKinds []}
                   :source {:kind "github" :mode "poll" :config {}}
                   :filters {} :agentSpec {:role "agent" :model "local-model" :thinkingLevel "off" :systemPrompt "Research" :toolPolicies []}})
(def ^:private snapshot {:configured true :tokenPreview "redacted" :control {:jobs [job]}
                        :runtime {:running false :jobs [] :sources {}} :availableSourceKinds ["github"]})
(def ^:private original-fetch js/fetch)
(def ^:private requests (atom []))
(def ^:private refuse? (atom false))
(defn- response [status body]
  #js {:ok (< status 400) :status status :json #(js/Promise.resolve (clj->js body))
       :text #(js/Promise.resolve (if (string? body) body (js/JSON.stringify (clj->js body))))})
(defn- fetch! [url ^js options]
  (let [body (when (.-body options) (js->clj (js/JSON.parse (.-body options)) :keywordize-keys true))]
    (swap! requests conj {:url url :method (or (.-method options) "GET") :body body})
    (js/Promise.resolve (if (and @refuse? (.-method options)) (response 503 "Runtime unavailable")
                         (response 200 (if body (assoc snapshot :control body) snapshot))))))
(t/use-fixtures :each
  {:before #(do (reset! requests []) (reset! refuse? false) (set! (.-fetch js/globalThis) fetch!))
   :after #(do (rtl/cleanup) (set! (.-fetch js/globalThis) original-fetch))})
(defn- render! [can-manage] (rtl/render (hx/$ panel/event-agents-panel {:can-manage can-manage})))

(t/deftest ^:async edit-save-and-failure-preserve-control-and-release-busy-state
  (let [^js rendered (render! true)]
    (await (.findByLabelText rendered "Model"))
    (.change rtl/fireEvent (.getByLabelText rendered "Model") #js {:target #js {:value "revised-model"}})
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Save runtime"}))
    (await (.findByText rendered "Event-agent control plane updated and runtime reloaded."))
    (t/is (= "revised-model" (get-in (last @requests) [:body :jobs 0 :agentSpec :model])))
    (t/is (= 10 (get-in (last @requests) [:body :jobs 0 :trigger :cadenceMinutes])))
    (reset! refuse? true)
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Save runtime"}))
    (await (.findByText rendered "Runtime unavailable"))
    (t/is (not (.-disabled (.getByRole rendered "button" #js {:name "Save runtime"}))))
    (t/is (= "revised-model" (.-value (.getByLabelText rendered "Model"))))))

(t/deftest ^:async read-only-runtime-controls-preserve-their-permission-gates
  (let [^js rendered (render! false)]
    (await (.findByLabelText rendered "Trigger kind"))
    (doseq [label ["Model" "Trigger kind" "Enabled"]] (t/is (.-disabled (.getByLabelText rendered label))))
    (doseq [label ["Save runtime" "Start runtime" "Full reset" "Run now"]]
      (t/is (.-disabled (.getByRole rendered "button" #js {:name label}))))
    (t/is (= ["GET" "GET"] (mapv :method @requests)))))

(t/deftest ^:async native-api-preserves-encoded-paths-and-unwrapped-wire-bodies
  (await (api/run-event-agent-job "job/with space")) (await (api/fire-trigger "trigger/one"))
  (await (api/update-discord-config "disposable-token"))
  (await (api/dispatch-event-agent-event {:sourceKind "github" :eventKind "issues.opened" :payload {:title "Research"}}))
  (t/is (= ["/api/admin/config/events/jobs/job%2Fwith%20space/run" "/api/admin/triggers/trigger%2Fone/fire"
            "/api/admin/config/discord" "/api/admin/config/events/dispatch"] (mapv :url @requests)))
  (t/is (= {:discordBotToken "disposable-token"} (:body (nth @requests 2))))
  (t/is (= {:sourceKind "github" :eventKind "issues.opened" :payload {:title "Research"}} (:body (last @requests)))))
