(ns knoxx.frontend.pages.events.context-test
  "Native Events context rendering and the chat pin contract."
  (:require ["react-dom/server" :as rds]
            [cljs.test :as t]
            [clojure.string :as str]
            [helix.core :as hx]
            [knoxx.frontend.pages.events.context :as context]))

(t/deftest empty-selection-keeps-capability-state-visible
  (let [markup (rds/renderToStaticMarkup
                (hx/$ context/events-context-panel
                      {:can-control? false :can-read-tools? false :tools #js []}))]
    (t/is (str/includes? markup "locked"))
    (t/is (str/includes? markup "hidden"))
    (t/is (str/includes? markup "Select a runtime job in the center panel."))))

(t/deftest selected-job-retains-source-trigger-and-catalog-feedback
  (let [markup (rds/renderToStaticMarkup
                (hx/$ context/events-context-panel
                      {:can-control? true :can-read-tools? true :tools #js [#js {} #js {}]
                       :tools-error "catalog read failed"
                       :selected-job {:id "job/one" :name "Research <agent>" :description "Selected context"
                                      :source {:kind "github"} :trigger {:kind "cron"}
                                      :contractSourceId "research-contract"}}))]
    (doseq [text ["allowed" "2 tools" "job/one" "Research &lt;agent&gt;" "github" "cron"
                 "research-contract" "Selected context" "catalog read failed"]]
      (t/is (str/includes? markup text) text))))

(t/deftest chat-pin-contract-preserves-runtime-fallback-and-bounded-job-context
  (let [fallback (context/pinned-context nil)
        selected (context/pinned-context {:id "job-1" :name "Research" :description (apply str (repeat 300 "x"))})]
    (t/is (= {:id "events:runtime" :title "Events runtime and audit logs" :path "/events" :kind "file"}
             (dissoc fallback :snippet)))
    (t/is (= {:id "event-job:job-1" :title "Event job: Research" :path "/events/jobs/job-1" :kind "file"}
             (dissoc selected :snippet)))
    (t/is (= 243 (count (:snippet selected))))
    (t/is (str/ends-with? (:snippet selected) "..."))
    (t/is (= "job-2" (:snippet (context/pinned-context {:id "job-2" :name "Fallback"}))))))
