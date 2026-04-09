# Translation Review UI

Date: 2026-04-06
Status: child spec
Points: 5
Epic: `knowledge-ops-translation-review-epic.md`
Depends on: `knowledge-ops-translation-routes.md`, `knowledge-ops-translation-export.md`

---

## Purpose

Build translation review UI in Knoxx frontend. Knoxx owns the entire product UI — Shibboleth provides only backend pipeline mechanics.

---

## UI Layout

```
┌─────────────────┬──────────────────────┬──────────────────┐
│ Source Text     │ Translated Text      │ Label Panel      │
│ (original)      │ (MT output)          │                  │
│                 │                      │ adequacy         │
│ [highlighted    │ [edit enabled for    │ fluency          │
│  uncertain      │  corrected_text]     │ terminology      │
│  segments]      │                      │ risk             │
│                 │                      │ overall          │
│                 │                      │ editor_notes     │
├─────────────────┴──────────────────────┴──────────────────┤
│         [Approve]  [Needs Edit]  [Reject]  [Skip]         │
└───────────────────────────────────────────────────────────┘
```

**Three-panel layout:**
- Left: Source text (original English)
- Center: Translated text (editable for corrections)
- Right: Label panel with dropdown selects

---

## Route Location

**Knoxx frontend owns the UI:**
- Route: `/translations` — translation review workbench
- Route: `/translations/segments/:id` — single segment review

No Shibboleth UI involved. Knoxx frontend calls OpenPlanner translation API directly.

---

## Components

### TranslationReviewPage.tsx

Main review page at `/translations`:

```tsx
// orgs/open-hax/knoxx/frontend/src/pages/TranslationPage.tsx

export function TranslationReviewPage() {
  const { user, org } = useAuth();
  const [segments, setSegments] = useState<TranslationSegment[]>([]);
  const [current, setCurrent] = useState<TranslationSegment | null>(null);
  const [labels, setLabels] = useState<LabelForm>(defaultLabels);

  useEffect(() => {
    fetchSegments({ status: "pending", limit: 50 });
  }, []);

  const fetchSegments = async (params) => {
    const res = await fetch(`/api/translations/segments?${new URLSearchParams(params)}`);
    const data = await res.json();
    setSegments(data.segments);
    if (data.segments.length > 0) setCurrent(data.segments[0]);
  };

  const submitLabel = async (overall: "approve" | "needs_edit" | "reject") => {
    if (!current) return;
    await fetch(`/api/translations/segments/${current.id}/labels`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...labels, overall }),
    });
    // Move to next segment
    const nextIdx = segments.findIndex(s => s.id === current.id) + 1;
    if (nextIdx < segments.length) {
      setCurrent(segments[nextIdx]);
    } else {
      fetchSegments({ status: "pending", limit: 50 });
    }
  };

  return (
    <div className="translation-review">
      <TranslationStats />
      <div className="review-panels">
        <SourcePanel text={current?.source_text} />
        <TranslationPanel
          text={current?.translated_text}
          correctedText={labels.corrected_text}
          onChange={(text) => setLabels(l => ({ ...l, corrected_text: text }))}
        />
        <LabelPanel labels={labels} onChange={setLabels} />
      </div>
      <ActionButtons
        onApprove={() => submitLabel("approve")}
        onNeedsEdit={() => submitLabel("needs_edit")}
        onReject={() => submitLabel("reject")}
        onSkip={() => {/* load next */}}
      />
    </div>
  );
}
```

### LabelPanel.tsx

```tsx
// orgs/open-hax/knoxx/frontend/src/components/LabelPanel.tsx

interface LabelPanelProps {
  labels: LabelForm;
  onChange: (labels: LabelForm) => void;
}

export function LabelPanel({ labels, onChange }: LabelPanelProps) {
  return (
    <div className="label-panel">
      <Select
        label="Adequacy"
        value={labels.adequacy}
        options={["excellent", "good", "adequate", "poor", "unusable"]}
        onChange={(v) => onChange({ ...labels, adequacy: v })}
      />
      <Select
        label="Fluency"
        value={labels.fluency}
        options={["excellent", "good", "adequate", "poor", "unusable"]}
        onChange={(v) => onChange({ ...labels, fluency: v })}
      />
      <Select
        label="Terminology"
        value={labels.terminology}
        options={["correct", "minor_errors", "major_errors"]}
        onChange={(v) => onChange({ ...labels, terminology: v })}
      />
      <Select
        label="Risk"
        value={labels.risk}
        options={["safe", "sensitive", "policy_violation"]}
        onChange={(v) => onChange({ ...labels, risk: v })}
      />
      <Textarea
        label="Editor Notes"
        value={labels.editor_notes}
        onChange={(v) => onChange({ ...labels, editor_notes: v })}
      />
    </div>
  );
}
```

---

## API Proxy

Knoxx backend proxies translation requests to OpenPlanner:

```clojure
;; In knoxx-backend core.cljs

(route! app "GET" "/api/translations/segments"
        (fn [request reply]
          (with-request-context! runtime request reply
            (fn [ctx]
              (ensure-permission! ctx "org.translations.read")
              (-> (fetch-json (openplanner-url config "/v1/translations/segments")
                              #js {:headers (openplanner-headers config)
                                   :query (aget request "query")})
                  (.then (fn [res]
                           (json-response! reply 200 (js->clj res :keywordize-keys true))))
                  (.catch (fn [err]
                            (error-response! reply err))))))))

(route! app "POST" "/api/translations/segments/:id/labels"
        (fn [request reply]
          (with-request-context! runtime request reply
            (fn [ctx]
              (ensure-permission! ctx "org.translations.review")
              (let [segment-id (aget request "params" "id")
                    body (aget request "body")]
                (-> (fetch-json (openplanner-url config (str "/v1/translations/segments/" segment-id "/labels"))
                                #js {:method "POST"
                                     :headers (merge (openplanner-headers config)
                                                    {"X-Knoxx-User-Id" (str (ctx-user-id ctx))
                                                     "X-Knoxx-User-Email" (str (ctx-user-email ctx))
                                                     "X-Knoxx-Org-Id" (str (ctx-org-id ctx))})
                                     :body body})
                    (.then (fn [res]
                             (json-response! reply 200 (js->clj res :keywordize-keys true))))
                    (.catch (fn [err]
                              (error-response! reply err)))))))))
```

---

## Files to Create/Modify

| File | Purpose |
|------|---------|
| `orgs/open-hax/knoxx/frontend/src/pages/TranslationPage.tsx` | Main translation review page |
| `orgs/open-hax/knoxx/frontend/src/components/LabelPanel.tsx` | Label selection panel |
| `orgs/open-hax/knoxx/frontend/src/components/TranslationPanel.tsx` | Source/translated text panels |
| `orgs/open-hax/knoxx/backend/src/cljs/knoxx/backend/core.cljs` | API proxy routes |

---

## Exit Criteria

- [ ] `/translations` route loads in Knoxx frontend
- [ ] Page loads translation segments from OpenPlanner via Knoxx backend
- [ ] Submitting labels persists to OpenPlanner
- [ ] Auth context flows through (user, org, permissions)
- [ ] Permission check on `org.translations.review` blocks unauthorized users
- [ ] Three-panel layout renders correctly
- [ ] Approve/Needs Edit/Reject/Skip actions work
