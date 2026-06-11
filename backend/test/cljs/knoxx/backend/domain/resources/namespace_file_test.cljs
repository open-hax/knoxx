(ns knoxx.backend.domain.resources.namespace-file-test
  "Tests for namespace resource files and composite resource expansion."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.resources.namespace-file :as ns-file]))

(def composite-entry
  {:trigger/id :discord.mentions
   :trigger/actor "discord_automation"
   :trigger/events [:discord.message]
   :trigger/with {:agent-id "ussyverse_social_replies"
                  :task "A Discord event fired."}
   :action/scope {:actions [:actions/noop]
                  :stores [:ussyverse/observed-messages]}
   :action/fn '(fn [ctx action] {:ok true})
   :store/id :observed-messages
   :store/schema [:map [:message-id :int]]})

(def namespace-file
  {:namespace :ussyverse
   :resources [composite-entry]})

;; ── namespace-file? ──────────────────────────────────────────────────

(deftest namespace-file-detection
  (testing "namespace files are detected by :namespace + :resources"
    (is (ns-file/namespace-file? namespace-file))
    (is (not (ns-file/namespace-file? {:contract/id "x" :contract/kind :trigger})))
    (is (not (ns-file/namespace-file? {:namespace :x})))
    (is (not (ns-file/namespace-file? nil)))))

;; ── qualified ids ────────────────────────────────────────────────────

(deftest qualified-id-resolution
  (testing ":namespace + local id -> qualified keyword"
    (is (= :ussyverse/discord.mentions
           (ns-file/qualified-id :ussyverse :discord.mentions)))
    (is (= "ussyverse/discord.mentions"
           (ns-file/qualified-id-str :ussyverse :discord.mentions))))
  (testing "string namespace and id are tolerated"
    (is (= :ussyverse/discord.mentions
           (ns-file/qualified-id "ussyverse" "discord.mentions")))))

;; ── composite expansion ──────────────────────────────────────────────

(deftest entry-kinds-reads-interpreter-id-keys
  (testing "a composite entry declares every kind whose id key is present"
    (is (= #{:trigger :action :store}
           (set (ns-file/entry-kinds (assoc composite-entry :action/id :reply))))))
  (testing "an anonymous action (:action/fn without :action/id) is not a standalone kind"
    (is (= #{:store :trigger} (set (ns-file/entry-kinds composite-entry))))))

(deftest composite-entry-expands-per-interpreter-kind
  (let [defs (ns-file/namespace-file-definitions namespace-file)
        by-kind (group-by :resource/kind defs)]
    (testing "trigger and store definitions are produced; anonymous action is not"
      (is (= #{:trigger :store} (set (keys by-kind)))))
    (testing "trigger definition keeps composite keys and gains qualified identity"
      (let [trigger (:resource/definition (first (:trigger by-kind)))]
        (is (= "ussyverse/discord.mentions" (:contract/id trigger)))
        (is (= :ussyverse/discord.mentions (:resource/qualified-id trigger)))
        (is (= :trigger (:contract/kind trigger)))
        (is (= :event (:trigger/kind trigger)) "trigger kind defaults to :event")
        (is (some? (:action/fn trigger)) "composite keys stay on the definition")
        (is (= [:discord.message] (:trigger/events trigger)))))
    (testing "store definition is keyed by :store/id"
      (let [store (:resource/definition (first (:store by-kind)))]
        (is (= "ussyverse/observed-messages" (:contract/id store)))
        (is (= :store (:contract/kind store)))
        (is (= [:map [:message-id :int]] (:store/schema store)))))))

(deftest explicit-trigger-kind-is-preserved
  (let [defs (ns-file/namespace-file-definitions
              {:namespace :n
               :resources [{:trigger/id :t :trigger/kind :event :trigger/events [:e]}]})]
    (is (= :event (get-in (first defs) [:resource/definition :trigger/kind])))))

;; ── loader integration ───────────────────────────────────────────────

(deftest parse-contract-file-records-expands-namespace-file
  (let [edn-text (pr-str namespace-file)
        records (contract-loader/parse-contract-file-records! "/fake/ns/ussyverse.edn" edn-text)
        by-class (group-by :contractClass records)]
    (testing "one record per interpreter kind, validated"
      (is (= 2 (count records)))
      (is (= #{"triggers" "stores"} (set (keys by-class))))
      (is (every? :ok? records)))
    (testing "records are indexed by qualified id"
      (is (= "ussyverse/discord.mentions" (:id (first (get by-class "triggers")))))
      (is (= "ussyverse/observed-messages" (:id (first (get by-class "stores"))))))))

(deftest parse-contract-file-records-single-contract-compat
  (testing "plain :contract/id files still load as a single record"
    (let [edn-text "{:contract/kind :trigger :contract/id \"solo\" :trigger/kind :event :trigger/events [:x]}"
          records (contract-loader/parse-contract-file-records! "/fake/triggers/solo.edn" edn-text)]
      (is (= 1 (count records)))
      (is (= "solo" (:id (first records))))
      (is (= "triggers" (:contractClass (first records)))))))

(deftest parse-contract-file-records-invalid-edn-is-empty
  (is (= [] (contract-loader/parse-contract-file-records! "/bad.edn" "not edn {{{"))))

(deftest stores-class-is-normalized
  (is (= "stores" (contract-loader/normalize-contract-class "store")))
  (is (= "stores" (contract-loader/normalize-contract-class :store))))

;; ── generalized grammar: every kind registers via :K/id ─────────────

(def multi-kind-file
  {:namespace :deploy
   :resources
   [{:agent/id "greeter"
     :agent {:role :role/knowledge-worker :model "glm-5"}
     :prompts {:system "You greet."}}
    {:role/id :greeter-role
     :role/capabilities [:cap/read]}
    {:cap/id :greet
     :cap/tools [:hello.world]}
    {:generator/id :clock
     :generator/kind :demo
     :generator/emits [:tick]}
    {:schedule/id :morning
     :schedule/rule "*/30 * * * *"
     :schedule/event {:event/type :tick}}
    {:policy/id "no-evil"
     :policy/invariants []}]})

(deftest every-kind-registers-via-its-id-key
  (let [records (contract-loader/parse-contract-file-records!
                 "/fake/ns/deploy.edn" (pr-str multi-kind-file))
        by-class (group-by :contractClass records)]
    (is (= 6 (count records)) (pr-str (mapv :id records)))
    (is (= #{"agents" "roles" "capabilities" "generators" "schedules" "policies"}
           (set (keys by-class))))
    (is (= "deploy/greeter" (:id (first (get by-class "agents")))))
    (is (= "deploy/morning" (:id (first (get by-class "schedules")))))))

;; ── anonymous facets: :K/* without :K/id is owned, not registered ───

(deftest anonymous-facets-are-owned-not-registered
  (testing "an agent facet on a trigger stays anonymous"
    (let [entry {:trigger/id :t
                 :trigger/events [:x]
                 :agent/model "glm-5"
                 :agent/prompts {:system "inline"}}]
      (is (= [:trigger] (ns-file/entry-kinds entry)))
      (is (= #{:trigger :agent} (set (ns-file/facet-kinds entry))))
      (is (= [:agent] (ns-file/anonymous-facets entry)))))
  (testing "registered kinds are not anonymous"
    (is (= [] (ns-file/anonymous-facets {:trigger/id :t :trigger/events [:x]})))))

(deftest anonymous-facets-recorded-on-definitions
  (let [defs (ns-file/namespace-file-definitions
              {:namespace :n
               :resources [{:trigger/id :t
                            :trigger/events [:x]
                            :action/fn '(fn [ctx action] {:ok true})}]})
        definition (:resource/definition (first defs))]
    (is (= [:action] (:resource/anonymous-facets definition)))))

;; ── references use the owning kind's namespace ───────────────────────

(deftest references-do-not-register
  (testing ":model/family is a reference, not a model-family registration"
    (let [entry {:model/id "glm-5" :model/family "glm"}]
      (is (= [:model] (ns-file/entry-kinds entry)))
      (is (= [:model] (ns-file/facet-kinds entry))))))
