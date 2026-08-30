(ns knoxx.backend.infra.publication-runtime-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-runtime :as runtime]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-content-integrity :as integrity]))

(def ^:private translated "Primer bloque.\n\nSegundo bloque.")

(def ^:private receipt
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden :knoxx.gardens/promethean
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision "sha256-source"
   :translation/revision "candidate-1"
   :translation/content-digest (integrity/content-digest translated)
   :translation/dispatch-key "candidate-1"
   :translation/org-id "org-1"
   :translation/project "review-stage"
   :translation/at "2026-08-30T10:00:00.000Z"})

(def ^:private document
  {:document/id :knoxx.docs/probe
   :document/source-locale :en
   :document/translations {:es {:path "probe.es.md"}}})

(def ^:private intent
  {:publication/document :knoxx.docs/probe
   :publication/garden :knoxx.gardens/promethean
   :publication/locale :es})

(defn- ^:async translated-blocks
  [agent-value authored-value legacy-calls
   & [candidate-receipt legacy-segments]]
  (let [candidate-receipt (or candidate-receipt receipt)
        legacy-segments (or legacy-segments [{:translated_text translated}])]
    (with-redefs [agent-content/content-for-receipt!
                  (fn [_ _] (js/Promise.resolve agent-value))
                  contract-content/localized-content!
                  (fn [_ _ _] (js/Promise.resolve authored-value))
                  openplanner-client/translation-document!
                  (fn [& _]
                    (swap! legacy-calls inc)
                    (js/Promise.resolve {:segments legacy-segments}))]
      (await (#'runtime/translated-blocks!
              ::client
              {:org-id "org-1" :project "review-stage"}
              {:knoxx.docs/probe "/contracts"}
              document intent candidate-receipt "/published")))))

(deftest ^:async publication-renders-only-receipt-bound-target-bytes
  (testing "exact agent bytes are admitted without a weaker fallback"
    (let [legacy-calls (atom 0)]
      (is (= ["Primer bloque." "Segundo bloque."]
             (await (translated-blocks translated "Authored" legacy-calls))))
      (is (zero? @legacy-calls))))

  (testing "tampered agent bytes stop instead of falling through"
    (let [legacy-calls (atom 0)]
      (is (nil? (await (translated-blocks "Changed" translated legacy-calls))))
      (is (zero? @legacy-calls))))

  (testing "an edited authored file cannot reuse its old receipt and approval"
    (let [legacy-calls (atom 0)]
      (is (nil? (await (translated-blocks nil "Changed" legacy-calls))))
      (is (zero? @legacy-calls))))

  (testing "the exact authored bytes remain a lawful fallback"
    (let [legacy-calls (atom 0)]
      (is (= ["Primer bloque." "Segundo bloque."]
             (await (translated-blocks nil translated legacy-calls))))
      (is (zero? @legacy-calls)))))

(deftest ^:async legacy-openplanner-segments-have-authenticated-boundaries
  (let [segments [{:translated_text "Primer segmento."}
                  {:translated_text "Segundo segmento."}]
        joined "Primer segmento.\n\nSegundo segmento."
        joined-receipt (assoc receipt
                              :translation/content-digest
                              (integrity/content-digest joined))
        legacy-calls (atom 0)]
    (is (= ["Primer segmento." "Segundo segmento."]
           (await (translated-blocks nil nil legacy-calls
                                     joined-receipt segments))))
    (is (= 1 @legacy-calls))
    (testing "the former undelimited bytes cannot reuse the joined receipt"
      (let [undelimited-receipt (assoc receipt
                                       :translation/content-digest
                                       (integrity/content-digest
                                        "Primer segmento.Segundo segmento."))]
        (is (nil? (await (translated-blocks nil nil legacy-calls
                                            undelimited-receipt segments))))
        (is (= 2 @legacy-calls))))))
