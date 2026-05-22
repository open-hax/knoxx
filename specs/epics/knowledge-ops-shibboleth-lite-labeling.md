# Knowledge Ops — Shibboleth-Lite Labeling Workflow Spec

> *Same DSL mechanics as adversarial safety labeling, repurposed for corporate knowledge QA and translation review.*

---

## Purpose

Define the labeling schema, task definitions, and review workflow for the "Shibboleth-lite" subsystem — the human-in-the-loop expert review layer that produces training-grade labeled data for the multi-tenant knowledge platform.

---

## Context

Shibboleth (https://github.com/octave-commons/shibboleth) is a Clojure-based generative DSL + pipeline for building multilingual adversarial prompt evaluation datasets. It defines attack taxonomies, harm categories, transforms, and pipelines as first-class macros.

For the knowledge ops platform, we reuse the **mechanics** (DSL, MT, clustering, provenance, labeling UI) but change **semantics** from adversarial safety to corporate QA and translation correctness.

---

## Repo Boundary

| Component | Contents | Visibility |
| :-- | :-- | :-- |
| `shibboleth-core` | Pipeline engine, MT orchestration, clustering, DSL primitives, provenance | Internal, safe to reuse |
| `shibboleth-adversarial` | Attack families, harm categories, hostile transforms, obfuscated content | Lab only, never in client builds |
| `futuresight-km-labeler` | Corporate schemas, safe transforms, labeling API + UI | Client-facing, depends only on `shibboleth-core` |

---

## Corporate QA Label Schema

### Example Record

```edn
{:example-id    "uuid"
 :tenant-id     "acme"
 :domain-id     "support"
 :question-original "How do I reset my SSO token?"
 :question-lang "en"
 :context-chunks [{:id "doc-123#chunk-4"
                   :source-title "SSO Runbook"
                   :source-url "https://confluence/..."
                   :text "..."}]
 :answer-original   "..."
 :answer-lang       "en"
 :answer-translated nil          ; optional
 :answer-target-lang nil}        ; optional
```

### Label Dimensions

| Field | Type | Values | Required |
| :-- | :-- | :-- | :-- |
| `correctness` | enum | `:correct`, `:partially-correct`, `:incorrect` | Yes |
| `groundedness` | enum | `:fully-grounded`, `:partially-grounded`, `:hallucinated` | Yes |
| `completeness` | enum | `:sufficient`, `:missing-key-details`, `:overly-verbose` | Yes |
| `tone` | enum | `:on-brand`, `:neutral`, `:off-brand` | No |
| `risk` | enum | `:safe`, `:sensitive-but-ok`, `:policy-violation` | Yes |
| `pii-leakage` | enum | `:none`, `:suspected`, `:confirmed` | Yes |
| `translation-quality` | enum | `:good`, `:minor-errors`, `:major-errors`, `:unusable` | When translated |
| `overall-label` | enum | `:approve`, `:needs-edit`, `:reject` | Yes |
| `editor-notes` | string | Free text | No |
| `gold-answer` | string | Expert-edited answer | When needs-edit or reject |

---

## Translation Segment Schema

For website/page translation review (Phase 2):

```edn
{:page-id       "uuid"
 :segment-id    "uuid"
 :source-text   "..."
 :source-lang   "en"
 :mt-text       "..."          ; machine translation output
 :mt-model      "qwen-14b"
 :final-text    nil             ; filled by reviewer
 :target-lang   "es"
 :labels        {:adequacy     :enum
                 :fluency      :enum
                 :terminology  :enum
                 :risk         :enum}}
```

---

## DSL Definitions (Pseudocode)

### Label Schema

```clj
(dsl/def-label-schema! :corporate-qna/v1
  {:fields
   [{:name :correctness     :type :enum :values [:correct :partially-correct :incorrect] :required? true}
    {:name :groundedness    :type :enum :values [:fully-grounded :partially-grounded :hallucinated] :required? true}
    {:name :completeness    :type :enum :values [:sufficient :missing-key-details :overly-verbose] :required? true}
    {:name :tone            :type :enum :values [:on-brand :neutral :off-brand] :required? false}
    {:name :risk            :type :enum :values [:safe :sensitive-but-ok :policy-violation] :required? true}
    {:name :pii-leakage     :type :enum :values [:none :suspected :confirmed] :required? true}
    {:name :translation-quality :type :enum :values [:good :minor-errors :major-errors :unusable]
     :required? (dsl/when-present? :answer-translated)}
    {:name :overall-label   :type :enum :values [:approve :needs-edit :reject] :required? true}
    {:name :editor-notes    :type :string :required? false}
    {:name :gold-answer     :type :string
     :required? (dsl/when-value? :overall-label #{:needs-edit :reject})}]})
```

### Label Task

```clj
(dsl/def-label-task! :tenant-qna-review/v1
  {:label-schema :corporate-qna/v1
   :presentation {:question-key :question-original
                  :answer-key   :answer-original
                  :context-keys [:context-chunks]
                  :language-selector {:source :question-lang :target :ui-lang}
                  :show-translation? true}
   :storage      {:bucket "futuresight-km-labels"
                  :prefix "tenant/${tenant-id}/qna/labels"}})
```

### Pipeline

```clj
(dsl/def-pipeline :tenant-qna-labeling/v1
  {:source   (dsl/ref :km-events/answered-queries)
   :filters  [(dsl/tenant-filter) (dsl/language-filter)]
   :transforms [(dsl/attach-context {:max-chunks 5})
                (dsl/normalize-fields)
                (dsl/project-keys [:example-id :tenant-id :domain-id
                                   :question-original :question-lang
                                   :context-chunks
                                   :answer-original :answer-lang
                                   :answer-translated :answer-target-lang])]
   :sink     (dsl/ref :label-queue/tenant-qna-review)})
```

---

## Review UI Layout

```
┌─────────────────┬──────────────────────┬──────────────────┐
│ Question +      │ Model Answer         │ Label Panel      │
│ Context Excerpts│                      │                  │
│                 │ [edit gold answer]   │ correctness      │
│ Doc titles      │                      │ groundedness     │
│ Source links    │                      │ completeness     │
│                 │                      │ tone             │
│                 │                      │ risk             │
│                 │                      │ pii-leakage      │
│                 │                      │ translation-qual │
│                 │                      │ overall-label    │
│                 │                      │ editor-notes     │
├─────────────────┴──────────────────────┴──────────────────┤
│              [Save & Next]  [Skip]                         │
└───────────────────────────────────────────────────────────┘
```

---

## How Labels Feed Downstream

### Evaluation Metrics

- Filter to `overall-label != :reject`
- Compute accuracy/groundedness per tenant/domain/model version
- Track translation-quality breakdown when `answer-translated` is present

### SFT Datasets

- For rows with `overall-label = :approve` and optional `gold-answer`
- Format: `{prompt = question + context, target = gold-answer-or-answer}`
- Datasheet and manifests for reproducibility

### RLHF (Later)

- Derive rewards: `reward = correctness_score - risk_penalty - hallucination_penalty`
- Preference pairs from approve vs reject pairs

---

## API Contract

### Ragussy → Shibboleth (label request)

```json
{
  "example_id": "uuid",
  "tenant_id": "acme",
  "domain_id": "support",
  "question": "How do I reset my SSO token?",
  "question_lang": "en",
  "answer": "...",
  "answer_lang": "en",
  "context": [
    {
      "id": "doc-123#chunk-4",
      "source_title": "SSO Runbook",
      "source_url": "https://confluence/...",
      "text": "..."
    }
  ],
  "model": "qwen-14b",
  "metadata": {
    "ragussy_run_id": "...",
    "timestamp": "..."
  }
}
```

### Shibboleth → Storage (label response)

```json
{
  "example_id": "uuid",
  "tenant_id": "acme",
  "labels": {
    "correctness": "partially-correct",
    "groundedness": "partially-grounded",
    "risk": "safe",
    "translation_quality": "good",
    "overall": "needs-edit"
  },
  "gold_answer": "...",
  "notes": "...",
  "labeler_id": "user-123",
  "labeled_at": "..."
}
```

---

## Existing Code References

### Label Schema — implemented in TypeScript and Python

| File | What it implements | Coverage |
|------|-------------------|----------|
| `packages/futuresight-kms/src/schema/index.ts` | `CorrectnessEnum`, `GroundednessEnum`, `CompletenessEnum`, `ToneEnum`, `RiskEnum`, `PiiLeakageEnum`, `TranslationQualityEnum`, `OverallLabelEnum` — all 8 enums match this spec exactly | Full |
| `packages/futuresight-kms/src/schema/index.ts` | `LabelDimensionsSchema` — correctness, groundedness, completeness?, tone?, risk, pii_leakage, translation_quality?, overall | Full |
| `packages/futuresight-kms/src/schema/index.ts` | `KmLabelSchema` — example_id, tenant_id, domain_id, question, context, labels, gold_answer, editor_notes, model, labeler_id, timestamps | Full |
| `packages/futuresight-kms/python/km_labels/models.py` | `LabelDimensions` (Pydantic) — mirrors Zod schema | Full |
| `packages/futuresight-kms/python/km_labels/models.py` | `KmLabel`, `CreateKmLabelPayload`, `UpdateKmLabelPayload` | Full |
| `packages/futuresight-kms/python/km_labels/database.py` | `km_labels` table — example_id UUID PK, labels JSONB, gold_answer TEXT, context JSONB | Full |

### Review UI — partially implemented

| File | What it implements | Gap |
|------|-------------------|-----|
| `packages/futuresight-kms/frontend/components/KnowledgeLabeler.tsx` | 3-panel layout (question+context, answer, label panel) with all 8 dropdown selects, gold answer textarea, editor notes, save/skip buttons | Uses **hardcoded placeholder data** (SSO token reset example). No real data fetching. No "load next" queue |
| `packages/futuresight-kms/frontend/components/styles.css` | 2-column grid, styled selects, dark mode, responsive breakpoint at 1024px | No 3-column layout as described in this spec |
| `orgs/octave-commons/shibboleth/ui/src/ChatLab.tsx` | Manual chat labeling: session creation, transcript, harm_category/response_class labeling, training export preview | Different schema (adversarial). Pattern is reusable but labels are harm-specific, not corporate QA |
| `orgs/octave-commons/shibboleth/ui/src/api.ts` | `fetchChatSessions`, `labelChatItem`, `writeChatExportSnapshot` | Shibboleth API, not futuresight-kms API |

### DSL Definitions — NOT IMPLEMENTED for corporate QA

| File | What exists | Gap |
|------|-------------|-----|
| `orgs/octave-commons/shibboleth/src/promptbench/taxonomy/families.clj` | `def-attack-family` macro | No `def-label-schema!` macro exists — the pseudocode in this spec is aspirational |
| `orgs/octave-commons/shibboleth/src/promptbench/transform/core.clj` | `def-transform`, `def-transform-chain` | Reusable for corporate transforms (chunking, normalization) |
| `orgs/octave-commons/shibboleth/src/promptbench/pipeline/core.clj` | `def-pipeline` macro | Reusable for corporate labeling pipelines |
| `orgs/octave-commons/shibboleth/src/promptbench/pipeline/sources.clj` | `def-source` macro | Could register corporate data sources |

### API Contract — partially implemented

| File | What implements | Gap |
|------|----------------|-----|
| `packages/futuresight-kms/python/km_labels/routers/labels.py` | `POST /api/km-labels` (create), `PATCH /{tenant_id}/{example_id}` (update), `GET /{tenant_id}` (list with filters) | Matches the label response contract in this spec |
| `packages/futuresight-kms/src/bridge/index.ts` | `KmLabelsClient.createLabel()` — sends `CreateKmLabelPayload` matching the label request contract | Client-side only — no server-side integration with Ragussy |
| `orgs/mojomast/ragussy/backend/app/api/shibboleth.py` | `POST /api/shibboleth/handoff` — exports conversation with `SessionHandoff` schema | Targets Shibboleth's adversarial schema, not futuresight-kms corporate schema. No reverse path |

### Export Pipeline — implemented

| File | What implements |
|------|----------------|
| `packages/futuresight-kms/python/km_labels/routers/export.py` | `GET /{tenant_id}/sft` — builds `{prompt, target}` from question+context+gold_answer, streams JSONL with SHA-256 checksum |
| `packages/futuresight-kms/python/km_labels/routers/export.py` | `GET /{tenant_id}/rlhf` — separates approved vs rejected, optional preference pairing |
| `packages/futuresight-kms/python/km_labels/routers/export.py` | `GET /{tenant_id}/manifest` — counts by label category, Shibboleth-style datasheet |
| `packages/futuresight-kms/src/schema/index.ts` | `ExportManifestSchema` — export_id, tenant_id, format, filters, checksums, datasheet |

---

## Sources

- Shibboleth codebase: https://github.com/octave-commons/shibboleth
- HITL annotation tooling patterns (Label Studio, LangSmith annotation queues)
- Enterprise RAG evaluation workflows (RAGAS, LLM judges)
- RLHF + HITL at enterprise scale

---

## Status

Draft — derived from inbox conversation on 2026-04-01.
