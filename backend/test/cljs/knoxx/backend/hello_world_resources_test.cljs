(ns knoxx.backend.hello-world-resources-test
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.registry :as action-registry]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.schedule.runtime :as schedule-runtime]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

(def resource-config
  {:contracts-dir "test/fixtures/hello-world-contracts"})

(def resource-files
  {:action "actions/hello-world.edn"
   :generator "generators/hello_world_demo.edn"
   :trigger "triggers/hello_world_inbox.edn"
   :schedule "schedules/hello_world_morning_tick.edn"})

(def resource-runtime-capabilities
  ["capabilities/cap_event_runtime.edn"
   "capabilities/cap_schedule_runtime.edn"
   "capabilities/cap_voice_audio_event.edn"])

(def removed-tool-fragments
  [(str "event" "_" "agents")
   (str "schedule" "_" "event" "_" "agent")
   (str "run" "_" "job")
   (str "upsert" "_" "job")
   (str "schedule" "_" "trigger")])

(defn- removed-tool-id?
  [tool-id]
  (let [tool-text (str tool-id)]
    (boolean (some #(str/includes? tool-text %) removed-tool-fragments))))

(def migrated-schedule-trigger-pairs
  [{:trigger-file "triggers/daily_synthesis.edn"
    :schedule-file "schedules/daily_synthesis.edn"
    :event-type :schedule/daily-synthesis}
   {:trigger-file "triggers/deep_synthesis_trigger.edn"
    :schedule-file "schedules/deep_synthesis.edn"
    :event-type :schedule/deep-synthesis}
   {:trigger-file "triggers/fork_tales_creative_director_cron.edn"
    :schedule-file "schedules/fork_tales_creative_director.edn"
    :event-type :schedule/fork-tales-creative-director}
   {:trigger-file "triggers/patrol_trigger.edn"
    :schedule-file "schedules/patrol.edn"
    :event-type :schedule/patrol}
   {:trigger-file "triggers/ussyverse_social_creative_cron.edn"
    :schedule-file "schedules/ussyverse_social_creative.edn"
    :event-type :schedule/ussyverse-social-creative}])

(defn- resource-root
  []
  (first (contract-loader/contract-root-paths resource-config)))

(defn- parsed-record
  [relative-path]
  (let [file-path (.join path (resource-root) relative-path)
        edn-text (.readFileSync node-fs file-path "utf8")]
    (or (contract-loader/parse-contract-file! file-path edn-text)
        (throw (js/Error. (str "hello-world resource failed to parse: " relative-path))))))

(defn- parsed-resource
  [relative-path]
  (:contract (parsed-record relative-path)))

(defn- hello-world-records
  []
  (->> [:action :generator :trigger :schedule]
       (mapv (fn [kind]
               (resources/resource-record (parsed-record (get resource-files kind)))))))

(defn- resource
  [kind id]
  (some (fn [record]
          (when (and (= kind (:resource/kind record))
                     (= id (:resource/id record)))
            (:resource/definition record)))
        (hello-world-records)))

(def hello-event
  {:event/id "evt_hello_world_test"
   :event/type :message/greeting
   :event/actor "system_admin"
   :event/generator {:kind "hello_world_demo"}
   :event/payload {:name "Ada"
                   :time-of-day "morning"
                   :sender "ada"
                   :recipient "system_admin"}})

(deftest hello-world-resource-set-is-separated
  (let [action (resource :action "hello-world")
        generator (resource :generator "hello_world_demo")
        trigger (resource :trigger "hello_world_inbox")
        schedule (resource :schedule "hello_world_morning_tick")]
    (testing "hello-world is represented as separate resource kinds"
      (is (= :action (:contract/kind action)))
      (is (= :generator (:contract/kind generator)))
      (is (= :trigger (:contract/kind trigger)))
      (is (= :schedule (:contract/kind schedule))))
    (testing "action advertises behavior but does not subscribe or schedule"
      (is (= :actions/hello-world (:action/kind action)))
      (is (= [:message/greeting] (:action/responds-to action)))
      (is (nil? (:trigger/events action)))
      (is (nil? (:schedule/rule action))))
    (testing "trigger hears events and requests an action without a schedule"
      (is (= :event (:trigger/kind trigger)))
      (is (= [:message/greeting] (:trigger/events trigger)))
      (is (= :actions/hello-world (:trigger/action trigger)))
      (is (nil? (:trigger/schedule trigger))))
    (testing "schedule emits an event without naming a trigger action"
      (is (= "hello_world_demo" (:schedule/generator schedule)))
      (is (= :message/greeting (get-in schedule [:schedule/event :event/type])))
      (is (nil? (:trigger/action schedule))))
    (testing "generator declares event provenance"
      (is (= :demo (:generator/kind generator)))
      (is (= [:message/greeting] (:generator/emits generator))))))

(deftest schedule-resource-emits-hello-world-event-shape
  (let [event (schedule-runtime/schedule->event (resource :schedule "hello_world_morning_tick"))]
    (is (= :message/greeting (:event/type event)))
    (is (= {:kind "hello_world_demo" :schedule/id "hello_world_morning_tick"}
           (:event/generator event)))
    (is (= "world" (get-in event [:event/payload :name])))))

(deftest migrated-schedule-triggers-use-single-event-path
  (doseq [{:keys [trigger-file schedule-file event-type]} migrated-schedule-trigger-pairs]
    (let [trigger (parsed-resource trigger-file)
          schedule (parsed-resource schedule-file)]
      (testing (str trigger-file " reacts to a schedule event")
        (is (= :event (:trigger/kind trigger)))
        (is (= [event-type] (:trigger/events trigger)))
        (is (nil? (:trigger/schedule trigger))))
      (testing (str schedule-file " emits but does not act")
        (is (= event-type (get-in schedule [:schedule/event :event/type])))
        (is (nil? (:trigger/action schedule)))
        (is (= "knoxx_schedule" (:schedule/generator schedule)))))))

(deftest event-runtime-capabilities-do-not-advertise-removed-tool-ids
  (doseq [capability-file resource-runtime-capabilities]
    (let [capability (parsed-resource capability-file)
          tools (set (:cap/tools capability))]
      (testing capability-file
        (is (empty? (filter removed-tool-id? tools)))
        (when (contains? #{:cap/event-runtime
                           :cap/schedule-runtime}
                         (:cap/id capability))
          (is (contains? tools :events.status))
          (is (contains? tools :events.dispatch)))))))

(deftest ^:async hello-world-action-produces-message-expectation
  (let [result (await (action-registry/run-action!
                      {:actor/id "system_admin"
                       :event hello-event}
                      {:action/id "hello-world"
                       :action/kind :actions/hello-world
                       :action/with {:actor-name "Knoxx"}}))]
    (is (true? (:ok result)))
    (is (= :message/send.expectation (:action/result result)))
    (is (= {:sender "system_admin"
            :recipient "system_admin"
            :text "Hello, Ada! I hope you are having a good morning. My name is Knoxx."}
           (:message/send result)))))

(deftest ^:async hello-world-event-dispatches-through-trigger
  (with-redefs [resources/load-all-resources-sync (fn [_] (hello-world-records))]
    (event-dispatch/reset-dedup!)
    (let [result (await (event-dispatch/dispatch! resource-config hello-event))
          action-result (first (:results result))]
      (is (= ["hello_world_inbox"] (:matchedTriggers result)))
      (is (= :actions/hello-world (:action/kind action-result)))
      (is (= "Hello, Ada! I hope you are having a good morning. My name is Knoxx."
             (get-in action-result [:message/send :text]))))))
