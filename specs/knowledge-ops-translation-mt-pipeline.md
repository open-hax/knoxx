# Translation MT Pipeline

Date: 2026-04-06
Status: child spec
Points: 3
Epic: `knowledge-ops-translation-review-epic.md`
Depends on: `knowledge-ops-translation-routes.md`

---

## Purpose

Implement MT pipeline that translates devel docs corpus using GLM-5 and writes segments to OpenPlanner.

This slice can be deferred — manually seeded segments are sufficient for demo.

---

## Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  OpenPlanner    │────▶│   MT Pipeline   │────▶│  OpenPlanner    │
│  (source docs)  │     │   (GLM-5)       │     │  (segments)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

1. Query OpenPlanner for documents needing translation
2. Chunk documents into segments (~500 tokens)
3. Call GLM-5 for each segment + target language
4. Write translated segments back to OpenPlanner

---

## Source Document Query

```
GET /v1/documents?project=devel-docs&visibility=public&needs_translation=true&target_langs=es,de
```

Documents are flagged for translation via metadata:

```json
{
  "metadata": {
    "needs_translation": true,
    "target_languages": ["es", "de"]
  }
}
```

---

## Chunking Strategy

- **Chunk size**: ~500 tokens per segment
- **Chunk boundary**: Prefer paragraph breaks, then sentence breaks
- **Preserve structure**: Code blocks, tables, and lists stay intact
- **Metadata**: Track source document ID, chunk index, and token range

```clojure
(defn chunk-document
  [doc]
  (let [text (:content doc)
        paragraphs (str/split text #"\n\n")
        chunks (reduce (fn [acc para]
                        (if (< (count (first acc)) 500)
                          (concat (butlast acc) [(str (last acc) "\n\n" para)])
                          (concat acc [para])))
                      [(first paragraphs)]
                      (rest paragraphs))]
    (map-indexed (fn [idx chunk]
                   {:document_id (:id doc)
                    :segment_index idx
                    :source_text chunk
                    :token_count (count chunk)})
                 chunks)))
```

---

## GLM-5 Translation Call

**Prompt template:**
```
Translate the following text from English to {target_lang}. 
Preserve formatting, technical terms, and code examples.

Text:
{source_text}
```

**API call:**
```clojure
(defn translate-segment
  [source-text target-lang]
  (let [prompt (str "Translate the following text from English to " target-lang ". "
                    "Preserve formatting, technical terms, and code examples.\n\n"
                    "Text:\n" source-text)
        response @(http/post (str proxx-base-url "/v1/chat/completions")
                            {:headers {"Authorization" (str "Bearer " proxx-auth-token)
                                       "Content-Type" "application/json"}
                             :json-params {:model "glm-5"
                                          :messages [{:role "user" :content prompt}]
                                          :temperature 0.3}})]
    {:translated-text (get-in response [:body :choices 0 :message :content])
     :model "glm-5"
     :confidence (extract-confidence response)}))
```

**Confidence extraction:**
GLM-5 doesn't natively provide confidence scores. Estimate via:
- Response length consistency
- Presence of hedging language ("possibly", "might")
- Fallback: default to 0.8 for all translations (reviewers will validate)

---

## Batch Write to OpenPlanner

```clojure
(defn write-translated-segments
  [segments document-id target-lang]
  (let [payload {:segments (map (fn [seg]
                                  {:source_text (:source_text seg)
                                   :translated_text (:translated-text seg)
                                   :source_lang "en"
                                   :target_lang target-lang
                                   :document_id document-id
                                   :segment_index (:segment_index seg)
                                   :mt_model "glm-5"
                                   :confidence (:confidence seg)})
                                segments)}]
    @(http/post (str openplanner-url "/v1/translations/segments/batch")
                {:headers {"Content-Type" "application/json"}
                 :json-params payload})))
```

---

## Pipeline Orchestration

```clojure
(ns knoxx.ingestion.translation
  (:require [org.httpkit.client :as http]
            [clojure.string :as str]))

(defn run-translation-pipeline
  [config]
  (let [docs (fetch-documents-needing-translation config)
        target-langs ["es" "de"]]
    (doseq [doc docs
            target-lang target-langs]
      (let [chunks (chunk-document doc)
            translated (map #(translate-segment (:source_text %) target-lang) chunks)]
        (write-translated-segments translated (:id doc) target-lang)
        (println "Translated" (count chunks) "segments for" (:id doc) "->" target-lang)))))
```

---

## Files to Create/Modify

| File | Purpose |
|------|---------|
| `orgs/open-hax/knoxx/ingestion/src/knoxx/ingestion/translation.clj` | MT orchestration namespace |
| `orgs/open-hax/openplanner/src/routes/v1/documents.ts` | Add `needs_translation` query filter |

---

## Exit Criteria

- [ ] Documents flagged with `needs_translation: true` are picked up
- [ ] Documents chunked into ~500 token segments
- [ ] GLM-5 called for each segment + target language
- [ ] Translated segments written to OpenPlanner via batch endpoint
- [ ] Confidence scores captured (even if estimated)
- [ ] Both target languages (es, de) work
