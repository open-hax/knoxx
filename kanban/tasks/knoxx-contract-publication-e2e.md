---
uuid: "knoxx-contract-publication-e2e"
title: "Prove contract-owned publish -> translate -> review -> materialize with OpenPlanner REST absent"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "cms", "translations", "publication", "e2e", "deploy"]
created_at: "2026-08-12T00:00:00Z"
points: 5
category: tasks
---
# Prove contract-owned publish -> translate -> review -> materialize with OpenPlanner REST absent

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Turn the architectural claim into a deploy gate. The system only counts as decoupled when one complete publication journey succeeds while the OpenPlanner REST service is deliberately unavailable.

## Scenario

Use one disposable document, one garden, one source locale, and one translated target locale requiring review.

The test must prove:

```text
resource document/publication intent
  -> publication projection visible in CMS
  -> translation requested from unmet intent
  -> translated segments persisted through Knoxx translation boundary
  -> reviewer approval recorded
  -> publication gate clears
  -> publication adapter materializes exact requested locale/revision
  -> receipt reports convergence
  -> public/read surface serves the materialized translation
```

OpenPlanner REST must not be started or stubbed. A hidden call to `/api/openplanner/...` should fail the test, not be accidentally satisfied.

## CLJS pseudocode

```clojure
(deftest contract-owned-publication-without-openplanner-rest
  (async done
    (let [fixture (fixtures/publication-system
                   {:openplanner-rest :disabled
                    :adapter :fake-publication-target})]
      (-> (fixtures/write-resources!
           fixture
           [{:document/id :docs/probe
             :document/title "Probe"
             :document/source {:path "fixtures/probe.md"}}
            {:publication/id :docs/probe-es
             :publication/document :docs/probe
             :publication/garden :gardens/probe
             :publication/locale :es
             :publication/revision :source/current
             :publication/state :published
             :translation/review :required}])

          (.then #(cms/load-document! fixture :docs/probe))
          (.then (fn [view]
                   (is (= :published
                          (get-in view [:publications 0 :desired])))))

          (.then #(reconciler/tick! fixture))
          (.then #(is (= [:translation-missing]
                          (publication/current-blockers fixture :docs/probe-es))))

          (.then #(translation/complete-probe! fixture :docs/probe-es))
          (.then #(review/approve-probe! fixture :docs/probe-es))
          (.then #(reconciler/tick! fixture))

          (.then (fn [_]
                   (is (empty?
                        (publication/current-blockers fixture :docs/probe-es)))
                   (is (= "probe-revision"
                          (:materialized/revision
                           (receipts/latest fixture :docs/probe-es))))
                   (is (= 0 (openplanner-rest/call-count fixture)))
                   (done)))))))
```

Deploy-smoke shape:

```clojure
(defn verify-contract-publication! [client]
  (-> (client/get! "/api/publications/health")
      (.then #(assert (= 200 (:status %))))
      (.then #(client/get! "/api/translations/documents?limit=1"))
      (.then #(assert (= 200 (:status %))))))
```

## Failure assertions

The test should fail if:

- CMS falls back to `garden_publications` metadata;
- translation config reaches `/api/openplanner/v1/translations/config`;
- garden discovery reaches `/api/openplanner/v1/gardens`;
- review-required publication materializes before approval;
- adapter materializes the wrong locale or source revision;
- desired state is rewritten to match a failed adapter observation instead of reporting drift.

## Done when

- The E2E passes with OpenPlanner REST absent and fails when any legacy authority path is restored.
- The deploy verification path includes the new publication/CMS surfaces unconditionally.
- One receipt chain is sufficient to explain how a resource intent converged to a public translated artifact.
