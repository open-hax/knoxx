(ns knoxx.frontend.pages.gardens.logic-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.pages.gardens.logic :as logic]))

(deftest public-site-links-preserve-contract-paths
  (is (= "https://open-hax.promethean.rest/es/"
         (logic/public-url "https://open-hax.promethean.rest/" "/es/")))
  (is (= "http://localhost:4173/docs/probe"
         (logic/public-url "http://localhost:4173" "docs/probe"))))

(deftest deployment-wire-normalizes-without-content-or-style
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
    (is (= "open-hax/garden" (:id garden)))
    (is (= ["en" "es"] (:locales garden)))
    (is (= ["en" "es"] (mapv :locale (:placements garden)))
        "placements are stable by locale then path")
    (is (= ["https://open-hax.promethean.rest/"
            "https://open-hax.promethean.rest/es/"]
           (mapv :url (:placements garden))))
    (testing "Garden review does not regrow OpenPlanner-era content/style fields"
      (is (not-any? #(contains? garden %)
                    [:description :theme :auto-translate :content :layout])))))

(deftest language-labels-are-human-readable
  (is (= "Español" (logic/language-name "es")))
  (is (= "xx" (logic/language-name "xx"))))

;; ── reconciliation receipts ────────────────────────────────────────────────

(deftest receipt-summary-matches-the-namespaced-wire-value
  (testing "the reconcile route encodes keyword VALUES as namespace/name
            (shape.resource-identity/encode-wire-values) while clj->js strips
            the namespace from map KEYS — so the key is :type and the value
            carries its namespace"
    (is (= "Published." (logic/receipt-summary {:type "publication/materialized"})))
    (is (= "Already published at this revision; nothing changed."
           (logic/receipt-summary {:type "publication/noop"})))
    (is (= "Withdrawn from publication."
           (logic/receipt-summary {:type "publication/removed"}))))

  (testing "a bare name does not match — that collapse is the thing the wire
            encoding exists to prevent, so it must report as unrecognized
            rather than being silently accepted"
    (is (= "Reconciliation recorded: materialized."
           (logic/receipt-summary {:type "materialized"}))))

  (testing "a blocked plan names its blockers rather than claiming success"
    (is (= "Blocked: translation-missing, translation-review-required"
           (logic/receipt-summary
            {:type "publication/blocked"
             :blockers ["translation-missing" "translation-review-required"]})))
    (is (= "Blocked: the plan is not admissible"
           (logic/receipt-summary {:type "publication/blocked" :blockers []}))))

  (testing "an unrecognized or absent type reports as recorded, never as
            success: the reconciler emits a receipt for failure too"
    (is (= "Reconciliation failed; see the receipt journal."
           (logic/receipt-summary {:type "publication/failed"})))
    (is (= "Reconciliation recorded." (logic/receipt-summary {})))))

(deftest placement-published-reads-desired-state
  (testing "`:state` is the contract's DESIRED state, not evidence that bytes
            exist — which is why the publish action is offered for a placement
            already marked published"
    (is (true? (logic/placement-published? {:state "published"})))
    (is (false? (logic/placement-published? {:state "withheld"})))
    (is (false? (logic/placement-published? {:state "archived"})))
    (is (false? (logic/placement-published? {})))))

(deftest receipt-tone-never-paints-a-non-success-as-success
  (testing "the words and the colour have to agree; a summary written so a
            blocked plan does not read as published is undone by a banner that
            paints every outcome emerald"
    (is (= :success (logic/receipt-tone {:type "publication/materialized"})))
    (is (= :success (logic/receipt-tone {:type "publication/noop"})))
    (is (= :success (logic/receipt-tone {:type "publication/removed"})))
    (is (= :warning (logic/receipt-tone {:type "publication/blocked"})))
    (is (= :error (logic/receipt-tone {:type "publication/failed"}))))

  (testing "an unrecognized or absent type is a warning, not a success: the
            reconciler emits receipts for outcomes that are not wins, and the
            unknown case is likelier to be one of those"
    (is (= :warning (logic/receipt-tone {:type "publication/something-new"})))
    (is (= :warning (logic/receipt-tone {})))
    (is (= :warning (logic/receipt-tone {:type "materialized"})))))
