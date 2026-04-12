# Knoxx Workbench UX — Epic and Sub-Spec Breakdown

**Parent spec**: `knowledge-ops-workbench-ux-v1.md`
**Date**: 2026-04-12
**Status**: Phase 1 complete, Phase 2 unblocked
**Total estimated points**: 40 points across 25 sub-specs

---

## Dependency Gates

| Gate | Required by | Status |
|------|-------------|--------|
| P1A tenant enforcement | Status bar (collection context), all views that touch documents/collections | ✅ **CLEARED** — `src/plugins/tenant.ts` implemented |
| P1B graph-memory coherence | Memory Inspector, Agent Workspace | Not ready |
| P3 CMS/review boundary | Content Editor, Review Queue | Partial |

**Recommendation**: Phase 2 is now unblocked. Continue with Context Bar Navigation (0.2) and Status Bar (0.3). Epic 3 (Memory Inspector) and Epic 5 (Agent Workspace) must wait for P1B.

---

## Epic 0: Shell Foundation (8 pts) ✅ COMPLETE

**Purpose**: Three-pane shell, navigation, status bar, keyboard system.

### 0.1 Shell Layout Component (2 pts) ✅ COMPLETE

**Scope**: Three-pane responsive shell with Context Bar, Main Canvas, Inspection Panel.

**Files**:
- `src/shell/Shell.tsx` — root layout component
- `src/shell/Shell.module.css` — responsive grid styles

**Exit criteria**:
- [x] Shell renders at 375px, 768px, 1024px, 1440px
- [x] Left rail collapses to icon strip below 768px
- [x] Right panel collapses to icon strip below 768px
- [x] No horizontal overflow at any breakpoint

**Dependencies**: None (pure layout)

**Commit**: `afc20331` — feat(workbench): add three-pane Shell layout component (Epic 0.1)

---

### 0.2 Context Bar Navigation (2 pts) ✅ COMPLETE

**Scope**: Left rail nav with six named view entries.

**Files**:
- `src/shell/ContextBar.tsx` — nav component (integrated into Shell.tsx)
- `src/shell/nav-items.ts` — nav configuration

**Exit criteria**:
- [x] All six views reachable from Context Bar
- [x] Active view highlighted
- [x] Keyboard navigation (arrow keys) works
- [x] Collapsed mode shows icons only with tooltips

**Dependencies**: 0.1

**Commit**: `277b8eba` — feat(workbench): add Context Bar Navigation with keyboard support (Epic 0.2)

---

### 0.3 Status Bar (2 pts) ✅ COMPLETE

**Scope**: Bottom status bar with collection, model, tokens, agent status, mode.

**Files**:
- `src/shell/StatusBar.tsx` — status bar component
- `src/shell/status-hooks.ts` — status subscription hooks

**Exit criteria**:
- [x] Shows active collection name
- [x] Shows current LLM provider/model
- [x] Shows token budget (if available)
- [x] Shows agent run count (if any active)
- [x] Shows current keyboard mode

**Dependencies**: 0.1, P1A ✅ (for collection context)

**Commit**: `2ca86641` — feat(workbench): add Status Bar with hooks (Epic 0.3)

---

### 0.4 ChordOverlay Integration (2 pts) ✅ COMPLETE

**Scope**: Spacemacs-style keyboard chord discovery.

**Files**:
- `src/shell/ChordProvider.tsx` — chord context provider
- `src/shell/chord-actions.ts` — action registry

**Exit criteria**:
- [x] Pressing SPC reveals ChordOverlay
- [x] All primary actions have chord hints
- [x] ChordOverlay dismisses on Escape or action completion
- [x] Works from any view

**Dependencies**: 0.1

**Commit**: `19f3a8c9` — feat(workbench): add ChordProvider with WhichKeyPopup integration (Epic 0.4)

---

## Epic 1: Dashboard View (5 pts)

**Purpose**: Landing page showing attention items, agent runs, memory activity.

### 1.1 Dashboard Attention Cards (2 pts) ✅ COMPLETE

**Scope**: Review queue count, approval count, policy violation count with CTAs.

**Files**:
- `src/pages/DashboardPage.tsx` — page component
- `src/components/dashboard/AttentionCard.tsx` — reusable card
- `src/components/dashboard/dashboard-types.ts` — types and config

**Exit criteria**:
- [x] Shows count for each queue type
- [x] Each card has primary CTA button
- [x] Empty state shows warm message

**Dependencies**: 0.1, P3 (for review queue data — mocked for now)

**Commit**: `7be69622` — feat(workbench): add Dashboard Attention Cards (Epic 1.1)

---

### 1.2 Dashboard Agent Run Summary (2 pts)

**Scope**: List of recent/active agent runs with status and quick actions.

**Files**:
- `src/components/dashboard/AgentRunSummary.tsx` — run list component

**Exit criteria**:
- [ ] Shows last 5 runs
- [ ] Status icons: running/done/failed/paused
- [ ] Quick-approve button for paused runs
- [ ] Click navigates to Agent Workspace

**Dependencies**: 0.1, P1B (for agent run data)

---

### 1.3 Dashboard Memory Activity Feed (1 pt)

**Scope**: Recent stigmergic signals as plain-language lines.

**Files**:
- `src/components/dashboard/MemoryActivityFeed.tsx` — activity list

**Exit criteria**:
- [ ] Shows last 5 memory signals
- [ ] Uses Memory Signal Vocabulary chips
- [ ] Click navigates to Memory Inspector

**Dependencies**: 0.1, P1B (for graph memory data)

---

## Epic 2: Content Editor View (6 pts)

**Purpose**: Author and publish structured documents with AI assistance.

### 2.1 Content Editor Shell (2 pts)

**Scope**: Document title, body editor, structured fields panel.

**Files**:
- `src/pages/ContentEditorPage.tsx` — page component
- `src/components/editor/DocumentFields.tsx` — structured fields

**Exit criteria**:
- [ ] Document title editable
- [ ] Body editor (markdown or rich text)
- [ ] Collection selector dropdown
- [ ] Visibility selector dropdown
- [ ] Status indicator (draft/review/published)

**Dependencies**: 0.1, P3 (for CMS data model)

---

### 2.2 Content Editor AI Suggestions (2 pts)

**Scope**: Inline AI suggestion chips with accept/revise/dismiss.

**Files**:
- `src/components/editor/AISuggestionChip.tsx` — suggestion component
- `src/components/editor/suggestion-hooks.ts` — suggestion state

**Exit criteria**:
- [ ] Suggestions appear inline in body
- [ ] Diff preview on hover
- [ ] Accept/Revise/Dismiss buttons
- [ ] Keyboard chords: SPC i a (accept), SPC i d (dismiss)

**Dependencies**: 2.1, P1B (for AI suggestions)

---

### 2.3 Content Editor Provenance Panel (1 pt)

**Scope**: Right panel showing sources, agent, corrections that shaped document.

**Files**:
- `src/components/editor/ProvenancePanel.tsx` — provenance display

**Exit criteria**:
- [ ] Lists sources used
- [ ] Shows which agent (if any) contributed
- [ ] Shows correction history

**Dependencies**: 2.1, P1B (for provenance data)

---

### 2.4 Content Editor Staged Publish Flow (1 pt)

**Scope**: Draft → Review → Published workflow with explicit steps.

**Files**:
- `src/components/editor/PublishWorkflow.tsx` — workflow component

**Exit criteria**:
- [ ] Cannot publish directly from draft
- [ ] Must pass through review state
- [ ] Publish confirmation dialog
- [ ] Keyboard chord: SPC p (publish menu)

**Dependencies**: 2.1, P3 (for visibility state machine)

---

## Epic 3: Review Queue View (5 pts)

**Purpose**: Process pending items with correction capture that writes to memory.

### 3.1 Review Queue Shell (2 pts)

**Scope**: Queue list, item detail, label form.

**Files**:
- `src/pages/ReviewQueuePage.tsx` — page component
- `src/components/review/QueueList.tsx` — queue navigation

**Exit criteria**:
- [ ] Shows all pending items
- [ ] Queue ordered by confidence (lowest first)
- [ ] Item type badge (synthesis/MT/ingestion)
- [ ] Batch actions dropdown

**Dependencies**: 0.1, P3 (for review queue)

---

### 3.2 Review Item Detail (2 pts)

**Scope**: Output display, source comparison, label form.

**Files**:
- `src/components/review/ItemDetail.tsx` — detail panel
- `src/components/review/LabelForm.tsx` — dimension labels

**Exit criteria**:
- [ ] Shows full output
- [ ] Shows confidence score
- [ ] Label dimensions vary by item type
- [ ] Approve/Needs Edit/Reject/Skip buttons
- [ ] Keyboard chords: SPC a (approve), SPC r (reject), SPC s (skip)

**Dependencies**: 3.1

---

### 3.3 Correction Write-Back (1 pt)

**Scope**: Corrections propagate to memory graph.

**Files**:
- `src/components/review/correction-hooks.ts` — write-back logic

**Exit criteria**:
- [ ] Approve → reinforcement signal
- [ ] Reject → contradiction signal
- [ ] Edit → correction signal
- [ ] Signal visible in Memory Inspector

**Dependencies**: 3.2, P1B (for graph memory writes)

---

## Epic 4: Memory Inspector View (5 pts)

**Purpose**: Search-first graph exploration with focal expansion.

### 4.1 Memory Search Interface (2 pts)

**Scope**: Search bar, results list, focal node selection.

**Files**:
- `src/pages/MemoryInspectorPage.tsx` — page component
- `src/components/memory/SearchBar.tsx` — search input
- `src/components/memory/SearchResults.tsx` — results list

**Exit criteria**:
- [ ] Search bar focused by default
- [ ] Searches nodes, edges, trails
- [ ] Results show node label + signal chips
- [ ] Click result → focal node view

**Dependencies**: 0.1, P1B (for graph query)

---

### 4.2 Focal Node View (2 pts)

**Scope**: Single node with expandable 2-hop neighborhood.

**Files**:
- `src/components/memory/FocalNode.tsx` — node detail
- `src/components/memory/NeighborhoodGraph.tsx` — mini graph

**Exit criteria**:
- [ ] Shows node label and type
- [ ] Shows strength, trail heat, salience
- [ ] Shows memory signal chips
- [ ] Expandable neighborhood (1-hop, 2-hop)
- [ ] Edge view selector (raw/discovery/structural/evidence)

**Dependencies**: 4.1, P1B (for graph traversal)

---

### 4.3 Memory History Slider (1 pt)

**Scope**: Temporal replay — "why does the system believe this now?"

**Files**:
- `src/components/memory/HistorySlider.tsx` — timeline component

**Exit criteria**:
- [ ] Slider shows node history
- [ ] Scrubbing shows state at that timestamp
- [ ] Play button animates forward

**Dependencies**: 4.2, P1B (for historical graph state)

---

## Epic 5: Agent Workspace View (5 pts)

**Purpose**: Compose tasks, monitor runs, approve outputs, use scratchpad.

### 5.1 Agent Run List (2 pts)

**Scope**: Active runs list with status and quick actions.

**Files**:
- `src/pages/AgentWorkspacePage.tsx` — page component
- `src/components/agents/RunList.tsx` — run list

**Exit criteria**:
- [ ] Shows active and recent runs
- [ ] Status: running/paused/done/failed
- [ ] Click → run detail
- [ ] New Task button

**Dependencies**: 0.1, P1B (for agent runtime)

---

### 5.2 Agent Run Detail (2 pts)

**Scope**: Goal, scope, tools, step progress, confidence, controls.

**Files**:
- `src/components/agents/RunDetail.tsx` — detail panel
- `src/components/agents/RunControls.tsx` — action buttons

**Exit criteria**:
- [ ] Shows goal text
- [ ] Shows scope (collection, filters)
- [ ] Shows tools touched
- [ ] Shows step progress (X/Y)
- [ ] Shows confidence score
- [ ] Approve/Revise Goal/Stop/Retry buttons
- [ ] Keyboard chords: SPC t x (stop)

**Dependencies**: 5.1

---

### 5.3 Scratchpad Surface (1 pt)

**Scope**: Source assembly, prompt, clean output, export.

**Files**:
- `src/components/agents/Scratchpad.tsx` — synthesis surface

**Exit criteria**:
- [ ] Source assembly panel (drag files/answers/items)
- [ ] Prompt input
- [ ] Clean output (no conversational boilerplate)
- [ ] Export: Copy/Markdown/Save to CMS

**Dependencies**: 5.1

---

## Epic 6: Ops Log View (4 pts)

**Purpose**: Inspect ingestion, sync, embeddings, policy violations.

### 6.1 Ops Event Table (2 pts) ✅ COMPLETE

**Scope**: Time-ordered event log with filtering.

**Files**:
- `src/pages/OpsLogPage.tsx` — page component
- `src/components/ops/EventTable.tsx` — table component
- `src/components/ops/ops-types.ts` — event types

**Exit criteria**:
- [x] Shows time, type, status, summary
- [x] Filter by type (ingestion/embedding/sync/policy/MT)
- [x] Filter by date range
- [x] Status icons: done/warn/error

**Dependencies**: 0.1

**Commit**: `3fc2502c` — feat(workbench): add Ops Event Table (Epic 6.1)

---

### 6.2 Ops Event Detail (1 pt) ✅ COMPLETE

**Scope**: Expandable row with full trace.

**Files**:
- `src/components/ops/EventDetail.tsx` — expanded row

**Exit criteria**:
- [x] Shows full inputs
- [x] Shows full outputs
- [x] Shows duration
- [x] Shows error trace if failed
- [ ] Link to related review item (if any)

**Dependencies**: 6.1

**Commit**: `3fc2502c` — feat(workbench): add Ops Event Table (Epic 6.1)

---

### 6.3 Gardens Sub-Tabs (1 pt)

**Scope**: Dependency Garden and Truth Garden as Ops sub-tabs.

**Files**:
- `src/components/ops/GardensTab.tsx` — gardens wrapper

**Exit criteria**:
- [ ] Dependency Garden accessible from Ops
- [ ] Truth Garden accessible from Ops
- [ ] Share status bar and keyboard system

**Dependencies**: 6.1

---

## Shared Components Epic (2 pts) ✅ COMPLETE

**Purpose**: Reusable components used across views.

### 7.1 Memory Signal Chip (1 pt) ✅ COMPLETE

**Scope**: Fixed vocabulary badge component.

**Files**:
- `src/components/MemorySignalChip.tsx` — chip component

**Exit criteria**:
- [x] Implements all 7 vocabulary terms
- [x] Color mapping matches spec
- [x] Used in Dashboard, Memory Inspector, Provenance

**Dependencies**: None

**Commit**: `e8245572` — feat(workbench): add shared components MemorySignalChip and EmptyState (Epic 7)

---

### 7.2 Empty State Component (1 pt) ✅ COMPLETE

**Scope**: Warm message + primary action for empty lists.

**Files**:
- `src/components/EmptyState.tsx` — empty state component

**Exit criteria**:
- [x] Accepts title, message, action label, action handler
- [x] Consistent styling across all uses
- [x] Used in all views that have list/card content

**Dependencies**: None

**Commit**: `e8245572` — feat(workbench): add shared components MemorySignalChip and EmptyState (Epic 7)

---

## Summary Table

| Epic | Sub-specs | Points | Status | Dependencies |
|------|-----------|--------|--------|--------------|
| 0. Shell Foundation | 4 | 8 | ✅ Complete | None (layout), P1A ✅ (status bar) |
| 1. Dashboard | 3 | 5 | 2/5 complete | P3, P1B |
| 2. Content Editor | 4 | 6 | Not started | P3, P1B |
| 3. Review Queue | 3 | 5 | Not started | P3, P1B |
| 4. Memory Inspector | 3 | 5 | Blocked on P1B | P1B |
| 5. Agent Workspace | 3 | 5 | Blocked on P1B | P1B |
| 6. Ops Log | 3 | 4 | 2/3 complete (3 pts done) | None |
| 7. Shared Components | 2 | 2 | ✅ Complete | None |
| **Total** | **25** | **40** | **15 pts done, 2 pts unblocked** | |

**Progress**:
- ✅ Complete: 15 points (Epic 0, Epic 1.1, Epic 6.1+6.2, Epic 7)
- 🔓 Unblocked: 2 points (2.1, 3.1)
- 🔒 Blocked on P3: 6 points (1.2, 1.3, 2.3, 2.4, 3.2)
- 🔒 Blocked on P1B: 14 points (2.2, 3.3, 4.x, 5.x)

---

## Recommended Execution Order

**Phase 1 (COMPLETE — 10 pts)**:
- [x] 7.1 Memory Signal Chip (1 pt) — `e8245572`
- [x] 7.2 Empty State Component (1 pt) — `e8245572`
- [x] 0.1 Shell Layout Component (2 pt) — `afc20331`
- [x] 0.2 Context Bar Navigation (2 pt) — `277b8eba`
- [x] 0.3 Status Bar (2 pt) — `2ca86641`
- [x] 0.4 ChordOverlay Integration (2 pt) — `19f3a8c9`

**Phase 2 (IN PROGRESS — 2 pts)**:
- [x] 1.1 Dashboard Attention Cards (2 pt) ✅
- [ ] 2.1 Content Editor Shell (2 pt)
- [ ] 3.1 Review Queue Shell (2 pt)

**Phase 3 (after P3 clears)**:
- [ ] 1.2 Dashboard Agent Run Summary (2 pt)
- [ ] 2.3 Content Editor Provenance Panel (1 pt)
- [ ] 2.4 Content Editor Staged Publish Flow (1 pt)
- [ ] 3.2 Review Item Detail (2 pt)

**Phase 4 (after P1B clears)**:
- [ ] 1.3 Dashboard Memory Activity Feed (1 pt)
- [ ] 2.2 Content Editor AI Suggestions (2 pt)
- [ ] 3.3 Correction Write-Back (1 pt)
- [ ] 4.1 Memory Search Interface (2 pt)
- [ ] 4.2 Focal Node View (2 pt)
- [ ] 4.3 Memory History Slider (1 pt)
- [ ] 5.1 Agent Run List (2 pt)
- [ ] 5.2 Agent Run Detail (2 pt)
- [ ] 5.3 Scratchpad Surface (1 pt)

**Phase 5 (polish)**:
- [ ] 6.2 Ops Event Detail (1 pt)
- [ ] 6.3 Gardens Sub-Tabs (1 pt)

---

## Exit Criteria Checklist

Each sub-spec should track its exit criteria in its own file:

```
specs/workbench/
├── 0.1-shell-layout.md
├── 0.2-context-bar.md
├── 0.3-status-bar.md
├── 0.4-chord-overlay.md
├── 1.1-dashboard-attention.md
├── 1.2-dashboard-agent-runs.md
├── 1.3-dashboard-memory-activity.md
├── 2.1-content-editor-shell.md
├── 2.2-content-editor-ai-suggestions.md
├── 2.3-content-editor-provenance.md
├── 2.4-content-editor-publish-flow.md
├── 3.1-review-queue-shell.md
├── 3.2-review-item-detail.md
├── 3.3-review-correction-writeback.md
├── 4.1-memory-search.md
├── 4.2-memory-focal-node.md
├── 4.3-memory-history-slider.md
├── 5.1-agent-run-list.md
├── 5.2-agent-run-detail.md
├── 5.3-agent-scratchpad.md
├── 6.1-ops-event-table.md
├── 6.2-ops-event-detail.md
├── 6.3-ops-gardens-tabs.md
├── 7.1-memory-signal-chip.md
└── 7.2-empty-state.md
```
