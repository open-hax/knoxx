(ns knoxx.frontend.pages.gardens.logic-test
  (:require [cljs.test :as t]
            [knoxx.frontend.pages.gardens.logic :as logic]))

(t/deftest public-site-links-preserve-contract-paths
  (t/is (= "https://open-hax.promethean.rest/es/"
         (logic/public-url "https://open-hax.promethean.rest/" "/es/")))
  (t/is (= "http://localhost:4173/docs/probe"
         (logic/public-url "http://localhost:4173" "docs/probe"))))

(t/deftest deployment-wire-normalizes-without-content-or-style
  (let [view (logic/normalize-deployment
              {:site-url "https://open-hax.promethean.rest"
               :gardens
               [{:garden {:id "open-hax/garden" :title "Open Hax"
                           :status "active" :locales ["en" "es"]}
                 :publications
                 [{:id "open-hax/home-es" :locale "es" :path "/es/"
                   :state "published"}
                  {:id "open-hax/home-en" :locale "en" :path "/"
                   :state "published"}]}]})
        garden (first (:gardens view))]
    (t/is (= "open-hax/garden" (:id garden)))
    (t/is (= ["en" "es"] (:locales garden)))
    (t/is (= ["en" "es"] (mapv :locale (:placements garden)))
        "placements are stable by locale then path")
    (t/is (= ["https://open-hax.promethean.rest/"
            "https://open-hax.promethean.rest/es/"]
           (mapv :url (:placements garden))))
    (t/testing "Garden review does not regrow OpenPlanner-era content/style fields"
      (t/is (not-any? #(contains? garden %)
                    [:description :theme :auto-translate :content :layout])))))

(t/deftest language-labels-are-human-readable
  (t/is (= "Español" (logic/language-name "es")))
  (t/is (= "xx" (logic/language-name "xx"))))

;; ── reconciliation receipts ────────────────────────────────────────────────

(t/deftest receipt-summary-matches-the-namespaced-wire-value
  (t/testing "the reconcile route encodes keyword VALUES as namespace/name
            (shape.resource-identity/encode-wire-values) while clj->js strips
            the namespace from map KEYS — so the key is :type and the value
            carries its namespace"
    (t/is (= "Published." (logic/receipt-summary {:type "publication/materialized"})))
    (t/is (= "Already published at this revision; nothing changed."
           (logic/receipt-summary {:type "publication/noop"})))
    (t/is (= "Withdrawn from publication."
           (logic/receipt-summary {:type "publication/removed"}))))

  (t/testing "a bare name does not match — that collapse is the thing the wire
            encoding exists to prevent, so it must report as unrecognized
            rather than being silently accepted"
    (t/is (= "Reconciliation recorded: materialized."
           (logic/receipt-summary {:type "materialized"}))))

  (t/testing "a blocked plan names its blockers rather than claiming success"
    (t/is (= "Blocked: translation-missing, translation-review-required"
           (logic/receipt-summary
            {:type "publication/blocked"
             :blockers ["translation-missing" "translation-review-required"]})))
    (t/is (= "Blocked: the plan is not admissible"
           (logic/receipt-summary {:type "publication/blocked" :blockers []}))))

  (t/testing "an unrecognized or absent type reports as recorded, never as
            success: the reconciler emits a receipt for failure too"
    (t/is (= "Reconciliation failed; see the receipt journal."
           (logic/receipt-summary {:type "publication/failed"})))
    (t/is (= "Reconciliation recorded." (logic/receipt-summary {})))))

(t/deftest placement-published-reads-desired-state
  (t/testing "`:state` is the contract's DESIRED state, not evidence that bytes
            exist — which is why the publish action is offered for a placement
            already marked published"
    (t/is (true? (logic/placement-published? {:state "published"})))
    (t/is (false? (logic/placement-published? {:state "withheld"})))
    (t/is (false? (logic/placement-published? {:state "archived"})))
    (t/is (false? (logic/placement-published? {})))))

(t/deftest receipt-tone-never-paints-a-non-success-as-success
  (t/testing "the words and the colour have to agree; a summary written so a
            blocked plan does not read as published is undone by a banner that
            paints every outcome emerald"
    (t/is (= :success (logic/receipt-tone {:type "publication/materialized"})))
    (t/is (= :success (logic/receipt-tone {:type "publication/noop"})))
    (t/is (= :success (logic/receipt-tone {:type "publication/removed"})))
    (t/is (= :warning (logic/receipt-tone {:type "publication/blocked"})))
    (t/is (= :error (logic/receipt-tone {:type "publication/failed"}))))

  (t/testing "an unrecognized or absent type is a warning, not a success: the
            reconciler emits receipts for outcomes that are not wins, and the
            unknown case is likelier to be one of those"
    (t/is (= :warning (logic/receipt-tone {:type "publication/something-new"})))
    (t/is (= :warning (logic/receipt-tone {})))
    (t/is (= :warning (logic/receipt-tone {:type "materialized"})))))

(t/deftest a-noop-with-a-reason-is-a-refusal-not-a-success
  (t/testing "a noop has three causes and the receipt says which in :reason.
            `converge` emits its noop with NO reason, so absence is the only
            case that means 'already published' — the others are the planner
            declining to publish at all, and reporting them as success told a
            reviewer their content was live when it was not"
    (t/is (= "Already published at this revision; nothing changed."
           (logic/receipt-summary {:type "publication/noop"})))
    (t/is (= :success (logic/receipt-tone {:type "publication/noop"})))

    (t/is (= "Not published: the contract does not ask for this to be public."
           (logic/receipt-summary {:type "publication/noop"
                                   :reason "publication-not-public"})))
    (t/is (= :warning (logic/receipt-tone {:type "publication/noop"
                                         :reason "publication-not-public"})))

    (t/is (= "Not published: the garden is not active."
           (logic/receipt-summary {:type "publication/noop"
                                   :reason "garden-not-active"})))
    (t/is (= :warning (logic/receipt-tone {:type "publication/noop"
                                         :reason "garden-not-active"})))

    (t/testing "an unrecognized reason is surfaced verbatim rather than
              flattened into the converged message"
      (t/is (= "Nothing was done: some-new-reason."
             (logic/receipt-summary {:type "publication/noop"
                                     :reason "some-new-reason"})))
      (t/is (= :warning (logic/receipt-tone {:type "publication/noop"
                                           :reason "some-new-reason"}))))))

;; ── publish-all runs ───────────────────────────────────────────────────────

(t/deftest run-summary-counts-every-outcome-not-only-successes
  (t/testing "most of a garden is usually not publishable yet — a locale awaiting
            translation or approval answers blocked — so a summary naming only
            what published would read as though the rest had quietly worked"
    (t/is (= "2 published, 1 already current, 3 blocked, (6 attempted)"
           (logic/run-summary
            [{:type "publication/materialized"}
             {:type "publication/materialized"}
             {:type "publication/noop"}
             {:type "publication/blocked" :blockers ["translation-missing"]}
             {:type "publication/blocked" :blockers ["translation-missing"]}
             {:type "publication/blocked" :blockers ["translation-review-required"]}])))

    (t/testing "a reasoned noop is not counted as already-current: the planner
              declined to publish it"
      (t/is (= "1 not published, (1 attempted)"
             (logic/run-summary [{:type "publication/noop"
                                  :reason "garden-not-active"}]))))

    (t/is (= "1 failed, (1 attempted)"
           (logic/run-summary [{:type "publication/failed"}])))
    (t/is (= "nothing to publish, (0 attempted)" (logic/run-summary [])))))

(t/deftest run-tone-reports-the-worst-outcome
  (t/testing "a single failure must not be hidden behind a majority of successes"
    (t/is (= :success (logic/run-tone [{:type "publication/materialized"}
                                     {:type "publication/noop"}])))
    (t/is (= :warning (logic/run-tone [{:type "publication/materialized"}
                                     {:type "publication/blocked"}])))
    (t/is (= :error (logic/run-tone [{:type "publication/materialized"}
                                   {:type "publication/blocked"}
                                   {:type "publication/failed"}])))
    (t/is (= :success (logic/run-tone [])))))

(t/deftest publishable-placements-are-the-ones-the-contract-asks-to-publish
  (t/is (= [{:id "a" :state "published"}]
         (logic/publishable-placements
          {:placements [{:id "a" :state "published"}
                        {:id "b" :state "withheld"}
                        {:id "c" :state "archived"}]}))))
