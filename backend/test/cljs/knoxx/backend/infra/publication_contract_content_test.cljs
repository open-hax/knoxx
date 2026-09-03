(ns knoxx.backend.infra.publication-contract-content-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.domain.translation-evidence :as evidence]
            [knoxx.backend.infra.publication-contract-content :as content]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.translation-evidence-store :as store]))

(def ^:private temp-root "/tmp/knoxx-publication-contract-content-test")

(def document
  {:document/id :open-hax.documents/promethean
   :document/title "Promethean"
   :document/source-locale :en
   :document/source {:path "/tmp/source.md"}
   :document/translations {:es {:path "promethean.es.md"}}})

(def intent
  {:publication/id :open-hax.publications/promethean-es
   :publication/document :open-hax.documents/promethean
   :publication/garden :open-hax.gardens/promethean
   :publication/locale :es})

(deftest localized-content-remains-separate-from-placement
  (is (= {:path "promethean.es.md"}
         (content/localized-source document :es)))
  (is (= (str temp-root "/promethean.es.md")
         (content/content-path temp-root (content/localized-source document :es))))
  (testing "a locale with no authored source is absent, not guessed"
    (is (nil? (content/localized-source document :fr)))))

(deftest ^:async authored-content-becomes-idempotent-review-evidence
  (let [translated "Hola, jardín."
        _ (await (fs/write-file-ensure-dir!
                  (str temp-root "/promethean.es.md") translated))
        evidence-store (store/memory-store)
        index {:documents {(:document/id document) document}
               :gardens {}
               :publications [intent]}
        scope {:org-id "open-hax" :project "knoxx-session"}
        source-revisions {(:document/id document)
                          (source-revision/content-revision "Hello, garden.")}
        first-pass (await (content/ensure-receipts!
                           evidence-store index
                           {(:document/id document) temp-root}
                           scope source-revisions))
        _ (await (content/ensure-receipts!
                  evidence-store index
                  {(:document/id document) temp-root}
                  scope source-revisions))
        stored (await (store/completed-translations! evidence-store scope))]
    (is (= 1 (count first-pass)))
    (is (= 1 (count stored)) "rediscovery must not append duplicate evidence")
    (is (= (source-revision/content-revision translated)
           (:translation/revision (first stored))))
    (is (= (source-revision/content-revision translated)
           (:translation/content-digest (first stored))))
    (is (= :open-hax.gardens/promethean
           (:translation/garden (first stored))))))

(deftest current-authored-snapshot-replaces-lexically-newer-stale-history
  (let [base {:receipt/type :translation/completed
              :translation/document :open-hax.documents/promethean
              :translation/garden :open-hax.gardens/promethean
              :translation/source-locale :en
              :translation/locale :es
              :translation/source-revision "sha256-source"
              :translation/dispatch-key "authored-content:open-hax:current"
              :translation/org-id "open-hax"
              :translation/project "knoxx-session"
              :translation/at content/authored-at}
        stale (assoc base
                     :translation/revision "zzzz-stale-digest"
                     :translation/content-digest "zzzz-stale-digest"
                     :translation/dispatch-key "authored-content:open-hax:stale")
        current (assoc base
                       :translation/revision "aaaa-current-digest"
                       :translation/content-digest "aaaa-current-digest")
        normalized (content/current-authored-receipts [stale current] [current])
        indexed (evidence/evidence {:receipts normalized})]
    (is (= [current] normalized))
    (is (= current
           (evidence/receipt-for indexed
                                 :open-hax.documents/promethean
                                 :open-hax.gardens/promethean
                                 :es
                                 "sha256-source")))))

(deftest desired-work-retires-an-authored-receipt-when-the-file-disappears
  (let [authored {:receipt/type :translation/completed
                  :translation/document :open-hax.documents/promethean
                  :translation/garden :open-hax.gardens/promethean
                  :translation/source-locale :en
                  :translation/locale :es
                  :translation/source-revision "sha256-source"
                  :translation/revision "sha256-target"
                  :translation/content-digest "sha256-target"
                  :translation/dispatch-key "authored-content:open-hax:deleted"
                  :translation/org-id "open-hax"
                  :translation/project "knoxx-session"
                  :translation/at content/authored-at}
        agent (assoc authored
                     :translation/revision "sha256-agent"
                     :translation/content-digest "sha256-agent"
                     :translation/dispatch-key "agent-dispatch"
                     :translation/at "2026-08-30T11:00:00.000Z")
        desired (dissoc authored :receipt/type :translation/revision
                        :translation/content-digest :translation/dispatch-key
                        :translation/at)]
    (is (= [agent]
           (content/current-authored-receipts
            [authored agent] [] [desired]))
        "absence retires only authored evidence; other producer history stays")))
