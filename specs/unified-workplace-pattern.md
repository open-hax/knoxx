# Unified Workplace Pattern

**Date**: 2026-04-12
**Status**: Draft

---

## Two Navigation Layers

### Navbar (Top)
Decides the **KIND of work**:
- Chat — Agent-primary interaction
- Editor — Content creation with agent assistance
- Translation Review — Compare translations of selected context

### Context Bar (Left)
Decides the **TARGET of work**:
- Files, documents, sessions
- Search and filter across content
- Pin context for agent/workspace
- Same component across all workplaces

---

## Three Core Workflows

| Workplace | Primary (Center) | Secondary (Right) |
|-----------|------------------|-------------------|
| **Chat** | Conversation | Canvas (toggleable) |
| **Editor** | Canvas/Editor | Chat (always visible) |
| **Translation Review** | Translation comparison | Chat (always visible) |

Other workflows (Admin, Runs, Ingestion, etc.) are modes or escalated privileges over these basic components.

---

## The Context Bar (Left)

**A single shared component across all workplaces.** Provides:

1. **Session History**
   - Recent sessions for current context
   - Resume/recover session
   - New session button

2. **Search**
   - Semantic search across content
   - Filter by project/tenant
   - Results pinning

3. **Filters**
   - Visibility filters (internal, review, public, archived)
   - Kind filters (docs, code, config, data)
   - Source/domain filters
   - Path prefix filter

4. **File/Document Explorer**
   - Browse tree structure
   - Preview on select
   - Pin to context
   - Open in canvas/editor

### Implementation

Rename `ChatWorkspaceSidebar` to `ContextBar`. It is **not mode-specific** — it always provides the same capabilities:

```tsx
<ContextBar
  // Same props regardless of workplace
  sessions={sessions}
  onSessionResume={...}
  searchQuery={searchQuery}
  onSearchChange={...}
  onSemanticSearch={...}
  filters={filters}
  onFilterChange={...}
  explorerData={explorerData}
  onExplorerSelect={...}
  onPin={...}
  pinnedContext={pinnedContext}
/>
```

The Context Bar is the **shared navigation layer** for selecting work targets. The Navbar selects work type; the Context Bar selects work content.

---

## The Agent (Right)

The same agent backend, different presentation based on workplace:

### Chat (Canvas Panel - Secondary)

- Toggleable visibility (current behavior)
- Scratchpad for notes, drafts, email
- Receives content from chat messages
- Can send content back to chat context

### Editor (Condensed Chat - Secondary)

- Always visible (no toggle)
- Auto-pins current document to context
- Commands: "edit this", "rewrite section", "suggest alternatives"
- Receives canvas content automatically
- Can update canvas directly

### Translation Review (Condensed Chat - Secondary)

- Always visible
- Auto-pins source and target translations
- Commands: "explain difference", "suggest revision", "check terminology"
- Can highlight discrepancies

### Canvas Tool

The agent has a `canvas` tool that works in both contexts:

```typescript
interface CanvasTool {
  // Read current canvas content
  read(): string;
  
  // Append to canvas
  append(text: string, heading?: string): void;
  
  // Replace selection or entire content
  replace(text: string, selection?: { start: number; end: number }): void;
  
  // Insert at position
  insert(text: string, position: number): void;
  
  // Get selection
  getSelection(): { text: string; start: number; end: number } | null;
}
```

---

## The Primary Surface (Center)

### Chat (Conversation)

- Message list with assistant/user turns
- Composer for new messages
- Runtime panel for active runs
- Context hydration display

### Editor (Canvas/Editor)

- Document editor (markdown with live preview?)
- Current document title/metadata
- Save/publish actions
- Version history

### Translation Review (Comparison View)

- Side-by-side or diff view of translations
- Source text reference
- Quality indicators
- Approval/rejection actions

---

## State Sharing

Both panels share:

1. **Pinned Context** — Items pinned from left bar are available to agent
2. **Session State** — Current session ID, conversation ID, document ID
3. **Canvas Content** — Editor content available to agent for "edit this" commands

---

## Routing

| Route | Workplace | Center (Primary) | Right (Secondary) |
|-------|-----------|------------------|-------------------|
| `/chat` | Chat | Conversation | Canvas (toggle) |
| `/cms` | Editor | Editor | Chat (always) |
| `/translations` | Translation Review | Comparison | Chat (always) |
| `/workbench/*` | Workbench | View-specific | Inspection Panel |

Note: `/cms` route becomes "Editor" in this model — CMS is just the editor workflow focused on content.

---

## Other Workflows

Workflows that don't fit the 3-pane pattern are **modes or utility pages**:

- **Ingestion** — File upload/configuration (utility, not primary work)
- **Runs** — View run history/logs (utility)
- **Gardens** — Configure translation gardens (utility)
- **Admin** — Escalated privileges over all components
- **Query** — Direct semantic search (utility)

These don't need the full 3-pane layout. They may use the Context Bar for navigation but don't have the primary/secondary split.

---

## Implementation Order

1. **Extract ContextBar** from ChatWorkspaceSidebar ✅ DONE
   - Renamed component to ContextBar
   - Removed workspace-specific coupling (sync devel button, quick root buttons)
   - Added visibility and kind filters from CMS
   - Made file explorer IDE-style and compact
   - Added visibility indicators to files
   - Consolidated stats/ingestion indicators

2. **Extract CanvasPanel** from ChatMainPane
   - Make it usable as right panel in all workplaces
   - Support both "toggle" (Chat) and "always visible" (Editor, Translation Review) modes

3. **Create AgentChatPanel**
   - Condensed chat interface for secondary position
   - Auto-pins current context (document, translation, etc.)
   - Exposes canvas tool to agent

4. **Refactor CMSPage → EditorPage**
   - Replace current layout with ContextBar + Editor + AgentChatPanel
   - Remove overlay-based detail panel
   - Rename route from `/cms` to `/editor` (or keep `/cms` but think of it as Editor workflow)

5. **Create TranslationReviewPage**
   - ContextBar + TranslationComparison + AgentChatPanel
   - Uses same shared components

6. **Refactor ChatPage**
   - Use extracted ContextBar (DONE) and CanvasPanel
   - Minimal changes to behavior

---

## Acceptance Criteria

- [x] ContextBar extracted from ChatWorkspaceSidebar
- [x] ContextBar has IDE-style compact layout
- [x] ContextBar has visibility and kind filters
- [x] ContextBar shows visibility indicators on files
- [x] Workspace-specific coupling removed (no sync devel button)
- [ ] ContextBar is used across Chat, Editor, Translation Review
- [ ] Editor has editor in center, chat on right
- [ ] Editor chat auto-pins current document
- [ ] Translation Review has comparison in center, chat on right
- [ ] Canvas tool works in all contexts
- [ ] No loss of functionality from current implementations
