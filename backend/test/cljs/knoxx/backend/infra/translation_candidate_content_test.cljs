(ns knoxx.backend.infra.translation-candidate-content-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-candidate-content :as candidate]
            [knoxx.backend.infra.translation-content-integrity :as integrity]))

(def ^:private target "Traducción actual")

(defn- receipt
  [revision at]
  {:receipt/type :translation/completed
   :translation/document :knoxx.docs/probe
   :translation/garden :knoxx.gardens/promethean
   :translation/source-locale :en
   :translation/locale :es
   :translation/source-revision "sha256-source"
   :translation/revision revision
   :translation/content-digest (integrity/content-digest target)
   :translation/dispatch-key revision
   :translation/org-id "org-1"
   :translation/project "review-stage"
   :translation/at at})

(def ^:private documents
  {:knoxx.docs/probe
   {:document/id :knoxx.docs/probe
    :document/source-locale :en
    :document/translations {:es {:path "probe.es.md"}}}})

(deftest ^:async only-the-current-candidates-exact-bytes-are-admitted
  (let [older (receipt "candidate-old" "2026-08-30T10:00:00.000Z")
        current (receipt "candidate-current" "2026-08-30T11:00:00.000Z")
        reads (atom [])]
    (with-redefs [agent-content/content-for-receipt!
                  (fn [_ candidate]
                    (swap! reads conj (:translation/revision candidate))
                    (js/Promise.resolve
                     (when (= "candidate-old" (:translation/revision candidate))
                       target)))]
      (is (empty? (await (candidate/authenticated-receipts!
                          "/published" {} documents [] [older current])))
          "a missing newest artifact triggers recovery, never rollback")
      (is (= ["candidate-current"] @reads)
          "content admission runs only after deterministic supersession"))))

(deftest ^:async exact-agent-and-authored-targets-share-one-admission-law
  (let [agent-receipt (receipt "candidate-agent" "2026-08-30T11:00:00.000Z")
        authored-receipt (assoc (receipt "candidate-authored"
                                         "1970-01-01T00:00:00.000Z")
                                :translation/dispatch-key "authored-content:current")
        agent-reads (atom 0)
        authored-reads (atom 0)]
    (with-redefs [agent-content/content-for-receipt!
                  (fn [_ _]
                    (swap! agent-reads inc)
                    (js/Promise.resolve target))
                  contract-content/localized-content!
                  (fn [& _]
                    (swap! authored-reads inc)
                    (js/Promise.resolve target))]
      (testing "an exact agent entry is admitted"
        (is (= [agent-receipt]
               (await (candidate/authenticated-receipts!
                       "/published" {} documents [] [agent-receipt])))))

      (testing "a current authored identity reads only its declared file"
        (is (= [authored-receipt]
               (await (candidate/authenticated-receipts!
                       "/published" {:knoxx.docs/probe "/contracts"}
                       documents [authored-receipt] [authored-receipt])))))
      (is (= 1 @agent-reads))
      (is (= 1 @authored-reads)))))

(deftest ^:async unbound-history-never-reaches-a-content-provider
  (let [reads (atom 0)
        historical (dissoc (receipt "candidate-old"
                                    "2026-08-30T10:00:00.000Z")
                           :translation/content-digest)]
    (with-redefs [agent-content/content-for-receipt!
                  (fn [& _]
                    (swap! reads inc)
                    (js/Promise.resolve target))]
      (is (empty? (await (candidate/authenticated-receipts!
                          "/published" {} documents [] [historical]))))
      (is (zero? @reads)))))
