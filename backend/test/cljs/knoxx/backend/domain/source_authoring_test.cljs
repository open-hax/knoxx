(ns knoxx.backend.domain.source-authoring-test
  (:require [cljs.test :as test]
            [knoxx.backend.domain.source-authoring :as authoring]
            [knoxx.backend.infra.publication-source-revision :as revisions]
            [knoxx.backend.infra.source-authoring-store :as store]
            [knoxx.backend.law.source-authoring :as law]
            [knoxx.backend.shape.source-authoring :as shape]
            [malli.core :as m]))

(def ^:private scope {:org-id "org-a" :project "wiki" :document :docs/source})
(def ^:private actor {:id "human-a" :kind :human})
(def ^:private document {:document/id :docs/source :document/title "Source"
                        :document/source-locale :en :document/org-id "org-a"
                        :document/visibility :private :document/source {:path "source.md"}})
(def ^:private publication {:publication/id :docs/source-en :publication/document :docs/source
                           :publication/garden :test/garden :publication/locale :en
                           :publication/revision :source/current :publication/state :draft
                           :publication/path "/en/wiki/source/" :translation/review :none})
(def ^:private manifest {:namespace :docs :resources [document publication]})
(def ^:private wire {:operation_id "create" :title "Source" :content "Original."
                    :source_locale "en" :target_locales ["es"] :garden "test/garden"})
(defn- refusal [operation]
  (try (operation) nil (catch :default error (ex-data error))))

(test/deftest creation-wire-rejects-invalid-locales-before-resource-lookup
  (doseq [invalid ["en/us" "fr/us" "not a locale" " en" "en " "" "e" "en_US"]]
    (doseq [command [(assoc wire :source_locale invalid)
                     (assoc wire :target_locales [invalid])]]
      (let [error (refusal #(shape/decode-create command))]
        (test/is (= 400 (:status error)) (str "Refuse invalid locale " (pr-str invalid)))
        (test/is (= :source-authoring/wire-create (:contract error))))))
  (doseq [valid ["en" "eng" "en-US" "zh-Hant-TW" "not-a-locale"]]
    (let [decoded (shape/decode-create (assoc wire :source_locale valid :target_locales [valid]))]
      (test/is (= (keyword valid) (:source-locale decoded)))
      (test/is (= [(keyword valid)] (:target-locales decoded))))))

(test/deftest creation-facts-require-a-manifest-before-admission
  (let [observed (authoring/source-event scope actor "create" :observe nil document
                                        "Original." (revisions/content-revision "Original.") "now")
        missing (assoc observed :source/action :create)]
    (test/is (m/validate law/Event observed) "Observation facts need no creation manifest")
    (test/is (m/validate law/Event (assoc observed :source/action :save)))
    (test/is (false? (m/validate law/Event missing)))
    (test/is (false? (m/validate law/Event (assoc missing :source/manifest nil))))
    (let [created (authoring/source-event scope actor "create" :create nil document
                                          "Original." (:source/revision observed) "now" manifest)]
      (test/is (m/validate law/Event created))
      (test/is (= manifest (:source/manifest created))))
    (test/is (= 400 (:status (refusal #(authoring/source-event scope actor "create" :create nil document
                                                              "Original." (:source/revision observed) "now")))))
    (let [provider (store/memory-store)]
      (test/is (= 400 (:status (refusal #(store/admit-source! provider scope nil missing)))))
      (test/is (empty? (store/source-events! provider scope))))))

(test/deftest creation-manifest-admission-and-replay-reject-malformed-resource-data
  (let [event (authoring/source-event scope actor "create" :create nil document "Original."
                                      (revisions/content-revision "Original.") "now" manifest)]
    (doseq [invalid [{}
                     (dissoc manifest :namespace)
                     (dissoc manifest :resources)
                     (assoc manifest :resources [])
                     (assoc manifest :resources [document])
                     (assoc-in manifest [:resources 1] (dissoc publication :publication/path))
                     (assoc-in manifest [:resources 0 :document/title] "Different document bytes")
                     (assoc-in manifest [:resources 1 :publication/document] :docs/unrelated)]]
      (let [malformed (assoc event :source/manifest invalid)
            provider (store/memory-store)]
        (test/is (false? (m/validate law/Event malformed)))
        (test/is (= 400 (:status (refusal #(store/admit-source! provider scope nil malformed)))))
        (test/is (empty? (store/source-events! provider scope)))
        (test/is (= 400 (:status (refusal #(store/validated-history scope [malformed])))))))
    (let [provider (store/memory-store)]
      (test/is (= event (:event (store/admit-source! provider scope nil event))))
      (test/is (= [event] (store/validated-history scope [event]))))))
