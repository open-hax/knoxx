(ns knoxx.backend.contracts.loader-test
  (:require [clojure.string :as str]
            [cljs.test :refer [deftest is testing async]]
            [knoxx.backend.domain.contracts.loader :as sut]))

;; ---------------------------------------------------------------------------
;; normalize-contract-class
;; ---------------------------------------------------------------------------

(deftest normalize-contract-class-canonical
  (is (= "agents"       (sut/normalize-contract-class "agents")))
  (is (= "actors"       (sut/normalize-contract-class "actors")))
  (is (= "roles"        (sut/normalize-contract-class "roles")))
  (is (= "capabilities" (sut/normalize-contract-class "capabilities")))
  (is (= "policies"     (sut/normalize-contract-class "policies")))
  (is (= "source_modes" (sut/normalize-contract-class "source_modes")))
  (is (= "sources"      (sut/normalize-contract-class "sources")))
  (is (= "model_families" (sut/normalize-contract-class "model_families")))
  (is (= "models"       (sut/normalize-contract-class "models")))
  (is (= "runtime_features" (sut/normalize-contract-class "runtime_features")))
  (is (= "actions"      (sut/normalize-contract-class "actions")))
  (is (= "pipelines"    (sut/normalize-contract-class "pipelines")))
  (is (= "triggers"     (sut/normalize-contract-class "triggers"))))

(deftest normalize-contract-class-aliases
  (testing "agent singular -> agents"
    (is (= "agents" (sut/normalize-contract-class "agent"))))
  (testing "contract -> agents (legacy)"
    (is (= "agents" (sut/normalize-contract-class "contract"))))
  (testing "nil -> agents (default)"
    (is (= "agents" (sut/normalize-contract-class nil))))
  (testing "user -> actors"
    (is (= "actors" (sut/normalize-contract-class "user"))))
  (testing "model-family -> model_families"
    (is (= "model_families" (sut/normalize-contract-class "model-family"))))
  (testing "cap -> capabilities"
    (is (= "capabilities" (sut/normalize-contract-class "cap"))))
  (testing "runtime-feature -> runtime_features"
    (is (= "runtime_features" (sut/normalize-contract-class "runtime-feature"))))
  (testing "source-mode -> source_modes"
    (is (= "source_modes" (sut/normalize-contract-class "source-mode"))))
  (testing "runtime-source -> sources"
    (is (= "sources" (sut/normalize-contract-class "runtime-source")))))

(deftest normalize-contract-class-rejects-unknown
  (is (thrown? js/Error (sut/normalize-contract-class "weasel"))))

(deftest normalize-contract-class-case-insensitive
  (is (= "agents" (sut/normalize-contract-class "AGENTS")))
  (is (= "roles"  (sut/normalize-contract-class "ROLES"))))

;; ---------------------------------------------------------------------------
;; parse-contract-file! (tested via exposed helper)
;; ---------------------------------------------------------------------------

(deftest parse-contract-file-valid-agent
  (let [edn-text "{:contract/id \"test-agent\" :contract/kind :agent :trigger-kind :manual :agent {:role :knowledge_worker} :contract/actors #{\"chat_primary\"}}"
        result   (#'sut/parse-contract-file! "/fake/path.edn" edn-text)]
    (is (some? result))
    (is (:ok? result))
    (is (= "test-agent" (:id result)))
    (is (= "agents" (:contractClass result)))))

(deftest parse-contract-file-runtime-feature
  (let [edn-text "{:contract/kind :runtime-feature :contract/id \"eta-mu.opmf-contract-gate\" :runtime/feature :opmf-contract-gate :enabled false}"
        result   (#'sut/parse-contract-file! "/fake/runtime_features/opmf_contract_gate.edn" edn-text)]
    (is (some? result))
    (is (:ok? result))
    (is (= "eta-mu.opmf-contract-gate" (:id result)))
    (is (= "runtime_features" (:contractClass result)))))

(deftest parse-contract-file-source-mode
  (testing ":source-mode kind is canonicalized before raw :contract/kind reaches class normalization"
    (let [edn-text "{:contract/kind :source-mode :contract/id \"discord-synthesis\" :source-mode/id :source-mode/discord-synthesis :source/kind :discord :source/mode :template-synthesize}"
          result   (#'sut/parse-contract-file! "/fake/source_modes/discord_synthesis.edn" edn-text)]
      (is (some? result))
      (is (:ok? result))
      (is (= "discord-synthesis" (:id result)))
      (is (= "source_modes" (:contractClass result))))))

(deftest parse-contract-file-runtime-source
  (let [edn-text "{:contract/kind :source :contract/id \"openplanner-memory\" :source/id :source/openplanner-memory :source/provider :openplanner :source/hydration {:mode :triggered :k 6}}"
        result   (#'sut/parse-contract-file! "/fake/sources/openplanner_memory.edn" edn-text)]
    (is (some? result))
    (is (:ok? result))
    (is (= "openplanner-memory" (:id result)))
    (is (= "sources" (:contractClass result)))))

(deftest parse-contract-file-missing-id-returns-nil
  (let [edn-text "{:contract/kind :agent :trigger-kind :manual :agent {:role :knowledge_worker}}"
        result   (#'sut/parse-contract-file! "/fake/path.edn" edn-text)]
    (is (nil? result))))

(deftest parse-contract-file-missing-kind-returns-nil
  (let [edn-text "{:contract/id \"orphan\"}"
        result   (#'sut/parse-contract-file! "/fake/path.edn" edn-text)]
    (is (nil? result))))

(deftest parse-contract-file-invalid-edn-returns-nil
  (let [result (#'sut/parse-contract-file! "/bad.edn" "this is not edn {{{")]
    (is (nil? result))))

;; ---------------------------------------------------------------------------
;; dedup-contracts
;; ---------------------------------------------------------------------------

(deftest dedup-contracts-unique-pass-through
  (let [a {:contractClass "agents" :id "a" :file-path "/a.edn"}
        b {:contractClass "agents" :id "b" :file-path "/b.edn"}
        c {:contractClass "roles"  :id "a" :file-path "/r.edn"}]
    (is (= 3 (count (#'sut/dedup-contracts [a b c]))))))

(deftest dedup-contracts-collision-keeps-first
  (let [first-one {:contractClass "agents" :id "dupe" :file-path "/first.edn"}
        second    {:contractClass "agents" :id "dupe" :file-path "/second.edn"}
        result    (#'sut/dedup-contracts [first-one second])]
    (is (= 1 (count result)))
    (is (= "/first.edn" (:file-path (first result))))))

(deftest dedup-contracts-nils-stripped
  (let [good {:contractClass "agents" :id "x" :file-path "/x.edn"}
        result (#'sut/dedup-contracts [nil good nil])]
    (is (= 1 (count result)))
    (is (= "x" (:id (first result))))))

(deftest dedup-contracts-same-id-different-class-both-kept
  (let [agent {:contractClass "agents" :id "shared-id" :file-path "/a.edn"}
        role  {:contractClass "roles"  :id "shared-id" :file-path "/r.edn"}]
    (is (= 2 (count (#'sut/dedup-contracts [agent role]))))))

;; ---------------------------------------------------------------------------
;; entry->file-path
;; ---------------------------------------------------------------------------

(deftest entry->file-path-returns-nil-for-directory
  (let [fake-dir #js {:isFile (fn [] false) :name "subdir" :parentPath "/contracts"}]
    (is (nil? (#'sut/entry->file-path fake-dir)))))

(deftest entry->file-path-returns-nil-for-hidden-file
  (let [fake #js {:isFile (fn [] true) :name ".gitkeep" :parentPath "/contracts"}]
    (is (nil? (#'sut/entry->file-path fake)))))

(deftest entry->file-path-returns-nil-for-non-edn
  (let [fake #js {:isFile (fn [] true) :name "readme.md" :parentPath "/contracts"}]
    (is (nil? (#'sut/entry->file-path fake)))))

(deftest entry->file-path-returns-path-for-edn
  (let [fake #js {:isFile (fn [] true) :name "my_agent.edn" :parentPath "/contracts/agents"}]
    (is (string? (#'sut/entry->file-path fake)))
    (is (str/includes? (#'sut/entry->file-path fake) "my_agent.edn"))))

;; ---------------------------------------------------------------------------
;; load-all-contracts! — integration against real contracts dir
;; ---------------------------------------------------------------------------

(deftest load-all-contracts-returns-promise
  (let [config {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}
        p      (sut/load-all-contracts! config)]
    (is (instance? js/Promise p))))

(deftest load-all-contracts-resolves-non-empty
  (async done
    ;; fixtures: 2 valid agents + 1 role + 1 capability + 1 actor + 1 sub-agent = 6 valid records
    ;; broken.edn and no_identity.edn are silently dropped
    (let [config {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}]
      (-> (sut/load-all-contracts! config)
          (.then (fn [contracts]
                   (is (= 6 (count contracts))
                       (str "expected 6 valid contracts, got " (count contracts) ": " (pr-str (mapv :id contracts))))
                   (done)))
          (.catch (fn [err]
                    (is false (str "rejected: " (.-message err)))
                    (done)))))))

(deftest load-all-contracts-records-have-required-keys
  (async done
    (let [config {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}]
      (-> (sut/load-all-contracts! config)
          (.then (fn [contracts]
                   (doseq [c contracts]
                     (is (string? (:id c))           (str "missing :id in "          (pr-str c)))
                     (is (string? (:contractClass c)) (str "missing :contractClass in " (pr-str c)))
                     (is (true?   (:ok? c))           (str "invalid slipped through: " (pr-str c))))
                   (done)))
          (.catch (fn [err]
                    (is false (str "rejected: " (.-message err)))
                    (done)))))))

(deftest load-all-contracts-agents-only-filter
  (async done
    (let [config {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}]
      (-> (sut/load-all-contracts! config)
          (.then (fn [all]
                   (let [agents (filter #(= "agents" (:contractClass %)) all)]
                     (is (seq agents) "expected at least one agent contract")
                     (is (every? #(= "agents" (:contractClass %)) agents)))
                   (done)))
          (.catch (fn [err]
                    (is false (str "rejected: " (.-message err)))
                    (done)))))))

(deftest load-all-contracts-bad-root-resolves-empty
  (async done
    (-> (sut/load-all-contracts! {:contracts-dir "/nonexistent/path/does/not/exist"})
        (.then (fn [contracts]
                 (is (empty? contracts))
                 (done)))
        (.catch (fn [err]
                  (is false (str "must not reject on bad root: " (.-message err)))
                  (done))))))

(deftest contract-file-path-prefers-body-identity-record
  (testing "capability ids come from :cap/id, while legacy filenames may still carry cap_ prefixes"
    (let [config {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}
          file-path (sut/contract-file-path config "capabilities" "test-cap")]
      (is (str/includes? file-path "capabilities/test_cap.edn")))))

(deftest dedup-contracts-preserves-all-classes
  (async done
    (let [config {:contracts-dir "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend/test/fixtures/contracts"}]
      (-> (sut/load-all-contracts! config)
          (.then (fn [all]
                   (let [classes (set (map :contractClass all))]
                     (is (contains? classes "agents"))
                     (is (contains? classes "roles"))
                     (is (contains? classes "capabilities")))
                   (done)))
          (.catch (fn [err]
                    (is false (str "rejected: " (.-message err)))
                    (done)))))))
