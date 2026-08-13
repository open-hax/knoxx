(ns knoxx.backend.domain.translation-config-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.domain.translation-config :as config]
            [knoxx.backend.law.translation-config :as law]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def glm-model {:model/id "glm-5" :model/label "GLM 5"})
(def gpt-model {:model/id "gpt-5.5" :model/label "GPT 5.5"})
(def scoped-model {:model/id "xiaomi/mimo-v2-pro" :model/label "MiMo v2 Pro"})
(def colon-model {:model/id "gemma4:31b" :model/label "Gemma 4 31B"})

(def global-config
  {:namespace :knoxx.translation
   :policy/id :pipeline-default
   :translation/model "glm-5"
   :translation/source-locale :en
   :translation/default-review :required})

(def catalog [glm-model gpt-model scoped-model colon-model])

(def base-resources (into [global-config] catalog))

(defn- index-of [resources] (config/index-resources resources))

;; ── 1 global resolution with no legacy backend ────────────────────────────

(deftest global-config-resolves-from-resources
  (let [resolved (config/resolve-config (index-of base-resources) {})]
    (is (= {:translation/model "glm-5"
            :translation/source-locale :en
            :translation/default-review :required}
           resolved))
    (is (true? (m/validate law/TranslationPipelineConfig resolved))))
  (testing "and mentions no legacy translation backend anywhere"
    (let [legacy-marker (str "open" "planner")]
      (is (not (str/includes? (str/lower-case (pr-str (config/resolve-config
                                                       (index-of base-resources) {})))
                              legacy-marker)))))
  (testing "a graph with no global config is an explicit failure, not a default"
    (is (thrown-with-msg? js/Error #"no translation pipeline configuration"
                          (config/resolve-config (index-of catalog) {})))))

;; ── 2 org override merges before validation ───────────────────────────────

(deftest org-override-merges-before-validation
  (let [override {:namespace :orgs.acme
                  :policy/id :translation-pipeline
                  :translation/model "gpt-5.5"}
        index (index-of (conj base-resources override))]
    (testing "the override wins for the keys it sets"
      (is (= "gpt-5.5" (:translation/model (config/resolve-config index {:org-id "acme"})))))
    (testing "and inherits the rest from the global default"
      (let [resolved (config/resolve-config index {:org-id "acme"})]
        (is (= :en (:translation/source-locale resolved)))
        (is (= :required (:translation/default-review resolved)))))
    (testing "another org is unaffected"
      (is (= "glm-5" (:translation/model (config/resolve-config index {:org-id "other"})))))
    (testing "no org still resolves the global default"
      (is (= "glm-5" (:translation/model (config/resolve-config index {})))))))

(deftest org-override-cannot-smuggle-an-invalid-model
  (let [bad-override {:namespace :orgs.acme
                      :policy/id :translation-pipeline
                      :translation/model "not-in-catalog"}
        index (index-of (conj base-resources bad-override))]
    (testing "the global default alone is valid"
      (is (some? (config/resolve-config index {}))))
    (testing "but the override fails catalog validation — merge happens first"
      (is (thrown-with-msg? js/Error #"unknown translation model"
                            (config/resolve-config index {:org-id "acme"}))))))

;; ── 3 unknown model refs ──────────────────────────────────────────────────

(deftest unknown-model-ref-throws
  (let [index (index-of base-resources)
        err (try (config/validate-model-ref! index {:translation/model "ghost"})
                 nil
                 (catch :default e e))]
    (is (some? err))
    (is (= "ghost" (:translation/model (ex-data err))))
    (testing "the error names what the catalog does hold"
      (is (contains? (set (:known-models (ex-data err))) "glm-5")))))

;; ── 4 catalog-shaped model ids survive intact ─────────────────────────────

(deftest catalog-model-ids-are-not-keywordized
  (testing "real catalog ids carry /, : and . — keywordizing would mangle them"
    (doseq [model-id ["xiaomi/mimo-v2-pro" "gemma4:31b" "gpt-5.5"]]
      (testing model-id
        (let [index (index-of base-resources)
              patched (config/apply-patch index {} {:translation/model model-id})]
          (is (= model-id (:translation/model patched)))
          (is (string? (:translation/model patched)))
          (testing "and it round-trips through the wire unchanged"
            (is (= model-id (:model (config/config->wire patched))))
            (is (= model-id (:translation/model
                             (config/wire->config (config/config->wire patched)))))))))))

;; ── 5/7 wire key convention ───────────────────────────────────────────────

(deftest config-patch-decodes-unqualified-wire-key
  (testing "the exact body clj->js produces, round-tripped through the helper's
            own serialization rather than hand-written"
    (let [sent {:model "glm-5"}
          on-the-wire (js->clj (clj->js sent) :keywordize-keys true)]
      (is (= {:model "glm-5"} on-the-wire))
      (is (true? (m/validate law/TranslationConfigPatchJson on-the-wire)))
      (is (= {:translation/model "glm-5"} (config/decode-config-patch on-the-wire))))))

(deftest qualified-wire-key-is-rejected
  (testing "closed maps are what make this fail; an open map would accept the
            qualified key alongside the unqualified one and silently no-op"
    (is (false? (m/validate law/TranslationConfigPatchJson {:translation/model "glm-5"})))
    (is (thrown? js/Error (config/decode-config-patch {:translation/model "glm-5"})))
    (testing "and an extra key is rejected rather than ignored"
      (is (false? (m/validate law/TranslationConfigPatchJson
                              {:model "glm-5" :translation/model "gpt-5.5"})))))
  (testing "the response contract is closed too"
    (is (false? (m/validate law/TranslationConfigWireJson
                            {:model "glm-5" :source-locale "en"
                             :default-review "required" :extra true})))))

;; ── 6 the patch actually changes authority ────────────────────────────────

(deftest config-patch-changes-authoritative-model
  (let [index (index-of base-resources)
        wire {:model "gpt-5.5"}
        patched (config/apply-patch index {} (config/decode-config-patch wire))
        written (config/config-resource index patched)
        reindexed (config/index-resources (into [written] catalog))]
    (testing "the patched config reports the new model"
      (is (= "gpt-5.5" (:translation/model patched))))
    (testing "and so does a fresh resolution over the written resource"
      (is (= "gpt-5.5" (:translation/model (config/resolve-config reindexed {})))))
    (testing "not the previous one"
      (is (not= "glm-5" (:translation/model (config/resolve-config reindexed {})))))
    (testing "the written resource keeps its policy identity"
      (is (= :pipeline-default (:policy/id written))))))

(deftest patch-cannot-move-anything-but-the-model
  (let [index (index-of base-resources)]
    (doseq [[label patch] [["source locale" {:translation/source-locale :fr}]
                           ["review policy" {:translation/default-review :none}]
                           ["both" {:translation/model "glm-5"
                                    :translation/default-review :none}]]]
      (testing label
        (is (thrown? js/Error (config/apply-patch index {} patch)))))))

;; ── 8 response round trip ─────────────────────────────────────────────────

(deftest config-response-round-trips
  (let [resolved (config/resolve-config (index-of base-resources) {})
        wire (config/config->wire resolved)]
    (testing "every value crosses as a JSON scalar"
      (is (= {:model "glm-5" :source-locale "en" :default-review "required"} wire))
      (is (every? string? (vals wire)))
      (is (true? (m/validate law/TranslationConfigWireJson wire))))
    (testing "and decodes back to the same domain values"
      (is (= resolved (config/wire->config wire))))
    (testing "with no EDN colon on any value"
      (is (empty? (filter #(str/starts-with? % ":") (vals wire)))))))

;; ── Single authority ──────────────────────────────────────────────────────

(deftest one-resolution-path-for-every-consumer
  (testing "changing the resource-selected model changes what every consumer sees,
            because they all read this one function"
    (let [switched (assoc global-config :translation/model "gemma4:31b")
          index (config/index-resources (into [switched] catalog))
          resolved (config/resolve-config index {})]
      (is (= "gemma4:31b" (:translation/model resolved)))
      (testing "the wire form the worker consumes reports the same id"
        (is (= "gemma4:31b" (:model (config/config->wire resolved)))))
      (testing "and decoding it yields the same canonical value"
        (is (= (:translation/model resolved)
               (:translation/model (config/wire->config (config/config->wire resolved)))))))))

;; ── Real resource graph ───────────────────────────────────────────────────

(deftest ^:async ships-a-loadable-global-config-resource
  (testing "the authored manifest actually loads and resolves through the real
            loader — a manifest whose ids or shape were wrong would be silently
            dropped and this would find nothing"
    (let [records (await (resources/load-all-resource-records! {}))
          definitions (->> records (filter :ok?) (mapv :resource/definition))
          index (config/index-resources definitions)]
      (is (contains? (:configs index) config/global-config-id)
          "contracts/policies/translation_pipeline.edn must reach the index")
      (testing "and it resolves against the real model catalog"
        (let [resolved (config/resolve-config index {})]
          (is (true? (m/validate law/TranslationPipelineConfig resolved)))
          (is (string? (:translation/model resolved)))
          (testing "with the selected model present in the catalog"
            (is (contains? (:models index) (:translation/model resolved))))))
      (testing "the model catalog itself loaded"
        (is (pos? (count (:models index))))))))
