# Knoxx Workbench UX — Agentic CMS + Stigmergic Graph Memory
## Spec: `knowledge-ops-workbench-ux-v1.md`

Date: 2026-04-11  
Status: draft / new spec  
Depends on: `knowledge-ops-workbench-ui.md`, `knowledge-ops-full-roadmap.md`, `knowledge-ops-cms-data-model.md`  
Roadmap slot: P5 (workbench/UI/productization)  
Points: TBD (split before execution)

---

## Context

The existing workbench spec (`knowledge-ops-workbench-ui.md`) defines five panels, a left nav,
a scratchpad surface, and a status bar. The `uxx` component library (`@open-hax/uxx`) provides
primitives. The roadmap places full UX productization at P5, after tenant enforcement (P1A),
graph-memory coherence (P1B), and CMS/review boundaries (P3) are real.

This spec adds the missing layer the existing specs do not cover: **the UX model** —
how panels relate, how the agent runtime is exposed, how stigmergic graph memory surfaces
as legible user concepts, and what the complete set of named views looks like.

It was derived from a design conversation focused on the question:
*"What core features would an Agentic AI native CMS with knowledge management and stigmergic
graph memory have UI wise, for a solo builder with no UX team?"*

The guiding sentence for the whole product: **"A publishing workspace with inspectable agents
and explorable memory."**

---

## Design Principles

1. **Content workflow is the product**. The graph, the agents, and the memory are the engine.
   The UI is always a publishing and review workflow on top of them — not a graph browser.
2. **No raw graph on the home screen**. The graph is accessed through search and focal expansion,
   never as a default full-canvas view.
3. **Agents are collaborators, not oracles**. Every agent action shows: goal, scope, tools touched,
   current step, uncertainty/confidence, and explicit approve/revise/stop/retry controls.
4. **Memory is visible but not required to understand**. Stigmergic state is surfaced as
   human-legible badges: `recently reinforced`, `contested`, `decaying`, `high-traffic path`,
   `derived from corrections`. Users do not need to know the backend model to act on these signals.
5. **Correction is the highest-value action**. Every review, label, and edit is an input to the
   memory substrate, not just a content change. The UI should make this feel natural.
6. **One primary action per screen**. Progressive disclosure everywhere.
7. **Equal polish on every state** — skeleton loaders, empty states, error states, run logs.
   There is no "secondary page" that can look unfinished.

---

## Named Views

The workbench has six named views accessible from the left rail (Context Bar):

| View | Route | Primary action | Agent-visible? |
|------|-------|---------------|----------------|
| Dashboard | `/` | Review what changed, what needs attention | Read-only summary |
| Content Editor | `/content/:id` | Author, edit, publish documents | Inline AI suggestions |
| Review Queue | `/review` | Approve, correct, reject agent/pipeline outputs | Yes — write corrections to memory |
| Memory Inspector | `/memory` | Search and explore the knowledge graph | Read-only by default |
| Agent Workspace | `/agents` | Compose tasks, monitor runs, approve results | Full agent control |
| Ops Log | `/ops` | Inspect ingestion, sync state, embeddings, errors | Read-only audit |

These map directly onto the five workbench surfaces from `knowledge-ops-workbench-ui.md` (File Explorer,
Chat, Labels, Synthesize, Gardens), but framed around the user's task rather than around the panel
mechanics.

---

## Shell Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [logo]  Knoxx                             [org ▾] [user] [⚙]   │
├──────────┬───────────────────────────────────────┬───────────────┤
│          │                                       │               │
│ Context  │  Main Canvas                          │  Inspection   │
│ Bar      │  (switches by active view)            │  Panel        │
│          │                                       │               │
│ ─────── │                                       │  Provenance   │
│ Dashboard│                                       │  Memory       │
│ Content  │                                       │  Agent state  │
│ Review   │                                       │  Actions      │
│ Memory   │                                       │               │
│ Agents   │                                       │               │
│ Ops      │                                       │               │
│          │                                       │               │
├──────────┴───────────────────────────────────────┴───────────────┤
│  Status bar: collection • model • tokens • agent status • mode  │
└──────────────────────────────────────────────────────────────────┘
```

**Three-pane rule**: left rail = navigation and saved contexts; center = task canvas;
right panel = inspection on demand (provenance, memory neighborhood, agent reasoning summary, actions).
The right panel collapses to an icon strip on narrow viewports. It is never a required step for the
primary action — it enriches decision-making without blocking it.

The status bar uses `ModeIndicator` from `@open-hax/uxx`. It exposes: active collection, current LLM provider,
token budget, agent run count if any active, and current keyboard mode (for ChordOverlay discoverability).

---

## View 1: Dashboard

**Purpose**: Answer "what changed, what needs my attention, what are agents doing right now" in one screen.

```
┌─────────────────────────────────────────────────────────────────┐
│  Dashboard                                         [2026-04-11] │
├────────────────┬────────────────────────────────────────────────┤
│  Needs review  │  Recent agent runs                             │
│  ─────────── │  ────────────────────                           │
│  4 items       │  ● Ingestion: devel-docs   ✓ 3m ago           │
│  [→ Review]    │  ● Synthesis: Q4 summary   ⏸ awaiting review  │
│                │  ● MT pipeline: batch 7    ✗ failed 12m ago   │
├────────────────┴────────────────────────────────────────────────┤
│  Recent memory activity                                         │
│  ─────────────────────                                         │
│  [node: GraphWeaver] reinforced ×12 today                      │
│  [node: Tenant isolation] contested — 2 conflicting sources    │
│  [node: SSO runbook §3.2] decaying — last accessed 8 days ago  │
└─────────────────────────────────────────────────────────────────┘
```

**Components**:
- Attention card: count + CTA for each queue (review, approval, policy violation)
- Agent run list: icon (running/done/failed/paused), name, elapsed, status chip, quick-approve if paused
- Memory activity feed: recent stigmergic signals as plain-language lines (see Memory Signal Vocabulary below)
- All components use `Card`, `Badge`, `Button` from `@open-hax/uxx`

**Empty states**: If no items need review, show a warm message + context about what the queue means.
Never show a blank card.

---

## View 2: Content Editor

**Purpose**: Author and publish structured documents. The primary CMS authoring surface.

```
┌──────────────────────────────────────────────────────────────────┐
│ ← Back    knowledge-ops-workbench-ux-v1.md           [Publish ▾]│
├──────────────────────────────────────┬───────────────────────────┤
│  Title                               │  PROVENANCE               │
│  ┌──────────────────────────────────┐│                           │
│  │ Knoxx Workbench UX — v1         ││  Sources used:            │
│  └──────────────────────────────────┘│  • knowledge-ops-wb-ui.md │
│                                      │  • chat: "3-pane layout"  │
│  Body                                │  • correction by: riatz   │
│  ┌──────────────────────────────────┐│                           │
│  │ ...content...                   ││  MEMORY                   │
│  │                                  ││                           │
│  │ [AI suggestion: expand §Layout] ││  node: Workbench UX       │
│  │ [Accept] [Revise] [Dismiss]     ││  strength: ████░ medium   │
│  │                                  ││  last reinforced: 2h ago  │
│  └──────────────────────────────────┘│  co-activated with:       │
│                                      │  Agent Runtime, UXX       │
│  Structured fields                   │                           │
│  collection: [devel-docs ▾]          │  [Expand in Memory →]     │
│  visibility: [internal ▾]            │                           │
│  status: [draft]                     │                           │
└──────────────────────────────────────┴───────────────────────────┘
```

**Components**:
- Document title + body editor (rich text or markdown)
- AI suggestion chips inline in the body: a diff preview, accept/revise/dismiss triple
- Structured fields: collection, visibility, status (draft/review/published)
- Right panel: provenance (which sources, which agent, which corrections shaped this), memory node summary,
  link to full graph context
- Publish action: staged — draft → in review → published — never one-click from draft to live
- Keyboard: chord hints for accept/dismiss AI suggestion without mousing

---

## View 3: Review Queue

**Purpose**: Process pending items — agent outputs, MT segments, ingestion results — with correction capture.
Corrections write back to memory, not just to the document.

The three-panel translation review layout from `knowledge-ops-translation-review-ui.md` is the template
for all review modalities, not just translation.

```
┌─────────────────────────────────────────────────────────────────┐
│ Review Queue                            [4 pending] [batch ▾]  │
├────────────────┬───────────────────────────────────────────────┤
│ Queue          │ Item: synthesis-q4-summary.md                 │
│ ────────────  │ Type: agent synthesis    Confidence: 0.71     │
│ ● synthesis ✦  │                                               │
│   q4-summary   │ Output                                        │
│                │ ┌─────────────────────────────────────────────┐│
│ ● MT segment   │ │ # Q4 Summary                               ││
│   batch7 #203  │ │ ...                                        ││
│                │ └─────────────────────────────────────────────┘│
│ ● ingestion    │                                               │
│   devel-docs   │ Labels / Corrections                         │
│   4 flagged    │ correctness:  [good ▾]                       │
│                │ groundedness: [grounded ▾]                   │
│                │ notes:        [__________________]            │
│                │                                               │
│                │ [Approve] [Needs Edit] [Reject] [Skip]       │
└────────────────┴───────────────────────────────────────────────┘
```

**Key behavior**:
- Every approval/correction event is propagated back to the memory graph as a reinforcement or
  contradiction signal — the review queue is a direct feed into stigmergic memory, not just a CRUD form.
- Confidence score from the agent run is visible and drives queue ordering.
- Label form dimensions vary by item type (synthesis: correctness/groundedness/risk;
  MT: adequacy/fluency/terminology/risk; ingestion: relevance/quality/PII-flag).
- "Export to Scratchpad" sends the item to View 5 (Agent Workspace / Synthesize surface) for rework.

---

## View 4: Memory Inspector

**Purpose**: Explore the knowledge graph. Search-first. Never a default full-canvas graph dump.

```
┌─────────────────────────────────────────────────────────────────┐
│ Memory  [🔍 Search nodes, edges, trails...]            [filters]│
├─────────────────────────────────────────────────────────────────┤
│  Focal: GraphWeaver                          [← Back] [Expand +]│
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                                                            │ │
│  │    [OpenPlanner] ──edge: writes── [GraphWeaver]            │ │
│  │         │                              │                   │ │
│  │    edge: populates              edge: syncs               │ │
│  │         │                              │                   │ │
│  │    [KnowledgeLake]              [KnoxxGraph]              │ │
│  │                                                            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Node: GraphWeaver                                               │
│  strength:        ████░ (0.74)                                  │
│  trail heat:      ██░░░ recently cool                           │
│  status:          ⚠ contested — see 2 conflicting nodes         │
│  edge views:      [raw] [discovery] [structural] [evidence]     │
│                                                                  │
│  History  [▶ replay]  ─────2026-04-09──────●──2026-04-11──      │
└─────────────────────────────────────────────────────────────────┘
```

**Components**:
- Search bar always visible and focused by default — the graph opens on a query result or a saved view,
  never a blank canvas
- Focal node panel: single node with expandable neighborhood (2 hops by default, configurable)
- Edge view selector: `raw / discovery / structural / evidence / bridge` — these map directly to
  `openplanner-web-edge-salience-and-backbone-projections.md` edge view contract
- Node detail: strength, trail heat, salience rank, memory signals (see vocabulary below),
  last reinforced timestamp, source provenance list
- History slider: temporal replay mode — "why does the system believe this now?" is answerable
  by sliding the history cursor backwards
- Contested / decaying / bridge-rescued nodes get visual badges, not just tooltip text

**What is NOT in this view**:
- A button to "view all nodes" — this is a search tool, not a force-graph dump
- Any administrative controls — that is Ops (View 6)
- Any authoring controls — that is Content Editor (View 2)

---

## View 5: Agent Workspace

**Purpose**: Compose tasks, monitor live agent runs, approve/reject outputs, access the Scratchpad surface.

```
┌─────────────────────────────────────────────────────────────────┐
│ Agents                                     [+ New Task]        │
├──────────────┬──────────────────────────────────────────────────┤
│ Active runs  │ Run: synthesis-q4-summary                       │
│ ──────────  │ ─────────────────────────────                   │
│ ● synthesis  │ Goal: synthesize Q4 KPIs from devel-docs lake   │
│   q4-summary │ Scope: collection=devel-docs, date≥2025-10-01   │
│   ⏸ paused   │ Tools: query/answer, cms/documents              │
│              │ Status: awaiting human approval                  │
│ ● ingestion  │ Confidence: 0.71                                 │
│   devel-docs │ Step 3/4: output generated                      │
│   ✓ complete │                                                  │
│              │ [Approve]  [Revise Goal]  [Stop]  [View Output] │
│              │                                                  │
│              │ ─────────────────────────────────────────────── │
│              │ Scratchpad                                       │
│              │ Sources: [q4-kpis.md] [ops-report.md] [+ add]  │
│              │ Prompt: [___________________________________]    │
│              │ [Generate]  [Edit Output]  [Save to CMS]        │
└──────────────┴──────────────────────────────────────────────────┘
```

**Agent run components**:
- Run list: name, type, status icon, elapsed time
- Run detail: goal, scope declaration, tools touched, step progress, confidence score
- Controls: Approve / Revise Goal / Stop / Retry — all visible, never hidden in a menu
- "Revise Goal" opens an inline editor over the goal text and re-queues the run

**Scratchpad (Synthesize) surface** — rendered in the lower half of this view:
- Source assembly panel: drag in files, chat answers, labeled items, search results
- Synthesis prompt input
- Clean output panel (no conversational boilerplate)
- Export: copy / markdown / save to CMS as new document

This is the `Panel 4: Synthesize` from `knowledge-ops-workbench-ui.md`, co-located with agent runs
because synthesize is usually the output step of an agent run.

---

## View 6: Ops Log

**Purpose**: Inspect ingestion jobs, sync state, embeddings, policy violations, model evaluations.
Read-only audit surface — no authoring actions.

```
┌─────────────────────────────────────────────────────────────────┐
│ Ops                                  [filter ▾] [date range ▾] │
├─────────────────────────────────────────────────────────────────┤
│ Time         Type         Status   Summary                      │
│ ─────────── ──────────── ──────── ──────────────────────────── │
│ 10:42am     ingestion    ✓ done   devel-docs: 14 files, 2.1MB  │
│ 10:38am     embedding    ✓ done   14 chunks added              │
│ 10:31am     sync         ✓ done   OpenPlanner → GraphWeaver    │
│ 10:12am     policy check ⚠ warn   3 flagged segments (PII)     │
│ 09:55am     MT pipeline  ✗ error  batch7: timeout after 300s   │
└─────────────────────────────────────────────────────────────────┘
                                                [Inspect row →]
```

Each row expands to show: full inputs, outputs, duration, error trace if any, affected entities,
and a link to any related review queue item.

**Gardens are hosted here**: the Dependency Garden and Truth Garden from `knowledge-ops-gardens.md`
are accessible as sub-tabs of Ops, not as top-level nav entries. They are operator surfaces, not
day-to-day user surfaces.

---

## Memory Signal Vocabulary

The UI must never expose raw graph internals as labels. All stigmergic state is translated to
plain-language chips and badges. The vocabulary is fixed — no ad hoc labels.

| Internal state | UI label | Color | Meaning to user |
|----------------|----------|-------|-----------------|
| High co-activation count | `recently reinforced` | green | System has seen this concept come up a lot lately |
| Conflicting evidence nodes | `contested` | amber | Two or more sources say different things about this |
| Low recent access, high decay | `decaying` | gray | This knowledge hasn't been touched in a while — verify before relying on it |
| High salience rank, frequent path | `high-traffic path` | blue | Many queries pass through this node — it is load-bearing |
| Derived from correction events | `shaped by corrections` | teal | Human review has directly updated this node's state |
| Bridge edge rescue | `cross-domain link` | purple | This edge connects otherwise separate topic clusters |
| Projection stale / lagging | `memory out of sync` | red | Graph projection is behind — treat with lower confidence |

These labels appear as small chips on: node cards in the Memory Inspector, provenance panels in the
Content Editor right rail, and the memory activity feed on the Dashboard.

---

## Right Inspection Panel — Content

The inspection panel is context-sensitive. Its content changes based on the active view:

| Active view | Panel shows |
|-------------|-------------|
| Dashboard | Drill-down on selected queue item or memory signal |
| Content Editor | Provenance: sources, agent, corrections; Memory node summary; publish checklist |
| Review Queue | Full diff of agent output vs. source material; confidence breakdown; correction history |
| Memory Inspector | Full node detail: all edges, all signals, source list, temporal history |
| Agent Workspace | Full run trace: every tool call, input/output, cost, confidence per step |
| Ops Log | Full event detail: inputs, outputs, trace, related entities |

The panel collapses on mobile. On tablet (768px), it slides out as a drawer triggered by an inspect button.
On desktop (1024px+), it is always visible as a right rail.

---

## Keyboard System

All primary actions have chord hints via `ChordOverlay` from `@open-hax/uxx`.

| Action | Chord |
|--------|-------|
| Approve review item | `SPC a` |
| Reject review item | `SPC r` |
| Skip review item | `SPC s` |
| Accept AI suggestion | `SPC i a` |
| Dismiss AI suggestion | `SPC i d` |
| Open Memory Inspector for current node | `SPC m` |
| New agent task | `SPC t n` |
| Stop agent run | `SPC t x` |
| Open search | `/` |

Chords follow Spacemacs convention. `SPC` is the leader key. `ChordOverlay` exposes them passively —
pressing `SPC` reveals the overlay without requiring prior knowledge.

---

## Implementation Order (P5 wave)

This spec is slated for P5 in the full roadmap. Work items should be split before execution.

Suggested child spec order:

1. **Shell + nav skeleton** — three-pane layout, left rail, status bar, theme, ChordOverlay integration
2. **Dashboard view** — attention cards, agent run summary, memory activity feed
3. **Review Queue** — review modalities (synthesis, translation, ingestion), correction event write-back
4. **Content Editor** — structured fields, AI suggestion chips, provenance panel, staged publish flow
5. **Memory Inspector** — search-first graph, focal node, edge view selector, history slider
6. **Agent Workspace** — run list, run detail, approve/revise/stop controls, Scratchpad surface
7. **Ops Log** — event table, row expansion, Gardens sub-tabs

Do not start shell work until P1A (tenant enforcement) and P3 (CMS/review boundary) are real, per the
roadmap guidance: "Do not over-polish the UI before the enforcement, retrieval, and CMS boundaries it
is supposed to represent are real."

---

## Files to Create / Modify

| File | Purpose |
|------|---------|
| `orgs/open-hax/knoxx/frontend/src/shell/Shell.tsx` | Three-pane shell, status bar, ChordOverlay mount |
| `orgs/open-hax/knoxx/frontend/src/shell/ContextBar.tsx` | Left rail nav with six named views |
| `orgs/open-hax/knoxx/frontend/src/shell/InspectionPanel.tsx` | Right rail, context-sensitive content |
| `orgs/open-hax/knoxx/frontend/src/pages/DashboardPage.tsx` | View 1 |
| `orgs/open-hax/knoxx/frontend/src/pages/ContentEditorPage.tsx` | View 2 |
| `orgs/open-hax/knoxx/frontend/src/pages/ReviewQueuePage.tsx` | View 3 — extends TranslationReviewPage pattern |
| `orgs/open-hax/knoxx/frontend/src/pages/MemoryInspectorPage.tsx` | View 4 |
| `orgs/open-hax/knoxx/frontend/src/pages/AgentWorkspacePage.tsx` | View 5 + Scratchpad |
| `orgs/open-hax/knoxx/frontend/src/pages/OpsLogPage.tsx` | View 6 + Gardens sub-tabs |
| `orgs/open-hax/knoxx/frontend/src/components/MemorySignalChip.tsx` | Vocabulary chip component |
| `orgs/open-hax/knoxx/frontend/src/components/AgentRunCard.tsx` | Run summary + controls |
| `orgs/open-hax/knoxx/frontend/src/components/ProvenancePanel.tsx` | Shared provenance surface |

All primitives from `@open-hax/uxx`. No page implements its own Button, Card, Input, Modal, or ModeIndicator.

---

## Exit Criteria

- [ ] Shell renders three-pane layout at 375px, 768px, 1024px, 1440px
- [ ] All six named views reachable from Context Bar
- [ ] Status bar shows live collection, model, token, agent, and mode state
- [ ] ChordOverlay accessible from any view via `SPC`
- [ ] Dashboard shows review count, agent run list, and memory activity feed
- [ ] Review Queue processes all three item types (synthesis, translation, ingestion) and writes corrections to memory
- [ ] Content Editor shows AI suggestion chips with accept/revise/dismiss
- [ ] Content Editor publish flow requires review step between draft and published
- [ ] Memory Inspector opens on search result, not a full graph canvas
- [ ] Memory Inspector exposes all five edge view types from OpenPlanner edge view contract
- [ ] Memory Inspector history slider lets user inspect node state at any past timestamp
- [ ] Memory signal vocabulary is fixed — no ad hoc labels in any component
- [ ] Agent Workspace shows full run trace with approve/revise/stop controls
- [ ] Scratchpad can save output directly to CMS
- [ ] Ops Log shows all event types; rows expand to full trace; Gardens accessible as sub-tabs
- [ ] Right panel is collapsible on mobile, drawer on tablet, always-visible on desktop
- [ ] Dark mode works on all views (system preference + manual toggle)
- [ ] All empty states have a warm message and primary action
- [ ] No page below 375px shows overflow or truncation
