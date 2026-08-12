---
uuid: "knoxx-contract-publication-e2e"
title: "Prove contract-owned publish -> translate -> review -> materialize with OpenPlanner REST absent"
status: accepted
priority: P2
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

Use one disposable document, one active garden, one source locale, and one translated target locale requiring review.

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

Use Knoxx's native async test shape: `deftest ^:async` plus `await`, not legacy `cljs.test/async` callbacks or Promise chains.

```clojure
(deftest ^:async contract-owned-publication-without-openplanner-rest
  (let [fixture (await (fixtures/publication-system
                        {:openplanner-rest :disabled
                         :adapter :fake-publication-target}))]
    (await
     (fixtures/write-resources!
      fixture
      [{:garden/id :gardens/probe
        :garden/title "Probe Garden"
        :garden/status :active}

       {:document/id :docs/probe
        :document/title "Probe"
        :document/source-locale :en
        :document/source {:path "fixtures/probe.md"}}

       {:publication/id :docs/probe-es
        :publication/document :docs/probe
        :publication/garden :gardens/probe
        :publication/locale :es
        :publication/revision :source/current
        :publication/state :published
        :publication/path "/probe"
        :translation/review :required}]))

    (let [view (await (cms/load-document! fixture :docs/probe))]
      (is (= :published
             (get-in view [:publications 0 :desired]))))

    (await (reconciler/tick! fixture))
    ;; Blockers are independent facts: translation is absent and therefore no
    ;; revision-specific approval exists yet.
    (is (= #{:translation-missing :translation-review-required}
           (set (publication/current-blockers fixture :docs/probe-es))))

    (await (translation/complete-probe! fixture :docs/probe-es))
    (await (review/approve-probe! fixture :docs/probe-es))
    (await (reconciler/tick! fixture))

    (is (empty? (publication/current-blockers fixture :docs/probe-es)))

    (let [receipt (receipts/latest fixture :docs/probe-es)
          public  (await (public/read! fixture
                                       {:garden :gardens/probe
                                        :path "/probe"
                                        :locale :es}))]
      (is (= :docs/probe (:document/id receipt)))
      (is (= :gardens/probe (:target receipt)))
      (is (= :es (:locale receipt)))
      (is (= "probe-revision" (:revision receipt)))
      (is (= "Probe traducido" (:content public)))
      (is (= :es (:locale public)))
      (is (= "probe-revision" (:revision public)))
      (is (= 0 (openplanner-rest/call-count fixture))))))
```

The fixture must establish the concrete current source revision as `"probe-revision"`, so `:source/current` resolves deterministically before materialization.

## Shared deploy-smoke contract

Do not maintain a second partial route list in this card. Import the exact surface contract owned by `knoxx-openplanner-rest-retirement`:

```clojure
(require '[knoxx.backend.law.required-surfaces
           :refer [required-publication-surfaces]])

(defn ^:async verify-contract-publication! [client auth-harness]
  (doseq [{:keys [method path auth]} required-publication-surfaces]
    (let [authorized (await (client/request! method path
                                             (auth-harness/headers-for auth)))]
      (is (= 200 (:status authorized)))
      (when-not (= :public auth)
        (is (contains? #{401 403}
                       (:status (await (client/request! method path {})))))))))
```

The shared list includes all five required surfaces:

```text
GET /api/publications/health
GET /api/publications/documents?limit=1
GET /api/publications/gardens
GET /api/translations/documents?limit=1
GET /api/translations/config
```

## Failure assertions

The test should fail if:

- the garden resource is missing/archived or publication path validation fails;
- CMS falls back to `garden_publications` metadata;
- translation config reaches `/api/openplanner/v1/translations/config`;
- garden discovery reaches `/api/openplanner/v1/gardens`;
- review-required publication materializes before approval;
- adapter materializes the wrong document, target, locale, or concrete source revision;
- the public/read surface does not serve the translated artifact that the receipt says was materialized;
- desired state is rewritten to match a failed adapter observation instead of reporting drift;
- any shared required surface has the wrong method/authorization behavior or becomes optional when OpenPlanner REST is absent.

## Done when

- The E2E passes with OpenPlanner REST absent and fails when any legacy authority path is restored.
- Initial translation/review blockers match the gate's independent blocker semantics.
- The final receipt asserts exact document, target, locale, and concrete revision and the public read returns the translated artifact.
- Deploy verification and E2E use the same complete required-surface contract unconditionally.
- The shared verifier itself uses native `^:async`/`await`; no helper contains `await` inside an ordinary `defn`.
- One receipt chain is sufficient to explain how a resource intent converged to a public translated artifact.
