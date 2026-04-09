# Knowledge Ops — Shared Chat UI Library Spec

> *One library, five layers. Configure by layer, don't reimplement per layer.*

---

## Problem

Chat UI code is duplicated across 4+ implementations with incompatible types, different streaming approaches, and no shared components. Every layer would add another copy of the same bugs.

| Implementation | Framework | Streaming | Source Citations | Duplicated patterns |
|---------------|-----------|-----------|-----------------|-------------------|
| Ragussy ChatPage.tsx | React + Tailwind | REST only | Collapsible cards | Message bubbles, composer, error handling |
| Ragussy ChatLabPage.tsx | React + Tailwind | WebSocket (`ws.ts`) | None | Message bubbles, composer, error handling |
| Shibboleth ChatLab.tsx | React + CSS | Polling (2.5s interval) | None | Message bubbles, error handling |
| futuresight-kms KnowledgeLabeler.tsx | React + CSS | None | Context chunk cards | Error banner, loading spinner |
| fork-tales app.js | Vanilla JS | REST | Citation chips | Message bubbles, composer |

**3 incompatible `ChatMessage` types. Zero shared components.**

---

## Solution

Build the shared chat layer **on top of** `orgs/open-hax/uxx/` rather than inventing another visual system.

That means:
- `@open-hax/uxx/tokens` provides the Monokai palette, spacing, typography, motion, and chord system
- `@open-hax/uxx` provides the primitives and composites
- the chat package provides chat-specific behavior and composites

Extract a `@workspace/chat-ui` package in `packages/chat-ui/` that provides:

1. **Unified types** — one `ChatMessage`, one `ChatConfig`, one `SourceChunk`
2. **Core components** — `ChatPanel`, `MessageBubble`, `ChatComposer`, `SourceCitation`, `TypingIndicator`, `ErrorBanner`
3. **Hook API** — `useChat` with pluggable transport (REST, WebSocket, polling)
4. **Layer configs** — each layer passes a config object, not custom code

---

## Package Structure

```
packages/chat-ui/
├── package.json
├── tsconfig.json
├── README.md
├── AGENTS.md
├── src/
│   ├── index.ts                    # Re-exports everything
│   ├── types/
│   │   └── index.ts                # ChatMessage, ChatConfig, SourceChunk, etc.
│   ├── hooks/
│   │   ├── useChat.ts              # Main hook: send, receive, state
│   │   ├── useChatStream.ts        # WebSocket/SSE streaming transport
│   │   └── useChatPoll.ts          # Polling transport (for shibboleth-style backends)
│   ├── components/
│   │   ├── ChatPanel.tsx           # Full chat panel: transcript + composer + status
│   │   ├── ChatPanel.css
│   │   ├── MessageBubble.tsx       # Single message: role styling, icon, timestamp
│   │   ├── MessageBubble.css
│   │   ├── ChatComposer.tsx        # Input: textarea + send + Enter/Shift+Enter
│   │   ├── ChatComposer.css
│   │   ├── SourceCitation.tsx      # Source pill: title + link + section
│   │   ├── SourceCitation.css
│   │   ├── TypingIndicator.tsx     # Animated dots during streaming
│   │   ├── TypingIndicator.css
│   │   ├── ErrorBanner.tsx         # Dismissible error display
│   │   └── ErrorBanner.css
│   └── utils/
│       ├── scroll.ts               # Auto-scroll to bottom on new messages
│       └── persist.ts              # localStorage save/restore chat state
└── tests/
    └── useChat.test.ts
```

---

## Unified Types

```ts
// --- Core message type (superset of all existing shapes) ---

interface ChatMessage {
  id: string
  role: "system" | "user" | "assistant" | "tool"
  content: string
  timestamp?: string
  sources?: SourceChunk[]        // RAG citations attached to assistant messages
  toolCalls?: ToolCall[]         // for layer 4 (coding agent)
  toolName?: string              // for tool-role messages
  error?: { message: string }    // inline error display
  labels?: Record<string, string | null>  // for layer 5 (shibboleth labeling)
}

interface SourceChunk {
  id: string
  title: string
  source?: string               // file path or URL
  section?: string              // heading or section reference
  score?: number                // relevance score
  text?: string                 // optional snippet
}

interface ToolCall {
  id: string
  name: string
  arguments: string
}

// --- Layer configuration ---

interface ChatConfig {
  // Identity
  title: string                  // "Ask Us Anything" | "Knowledge Worker" | etc.
  placeholder?: string           // input placeholder

  // Transport
  transport: "rest" | "websocket" | "polling"
  endpoint: string               // base URL for chat API
  chatPath: string               // e.g. "/api/ragussy/chat" or "/v1/chat/completions"
  pollInterval?: number          // ms, for polling transport
  wsPath?: string                // WebSocket path, for streaming

  // Auth
  apiKey?: string
  headers?: Record<string, string>

  // Behavior
  maxMessages?: number           // cap message history
  systemPrompt?: string          // prepended system message
  showSources?: boolean          // render SourceCitation on assistant messages
  showTimestamps?: boolean
  showRoleIcons?: boolean
  enterToSend?: boolean          // Enter sends, Shift+Enter newline
  streaming?: boolean            // expect token-by-token response

  // Request shaping
  collection?: string            // RAG collection name
  model?: string                 // model identifier
  extraBody?: Record<string, unknown>  // additional fields sent with each request

  // Appearance
  theme?: "dark" | "light" | "auto"
  className?: string             // wrapper class override
}

// --- Transport adapter ---

interface ChatTransport {
  send(config: ChatConfig, messages: ChatMessage[]): Promise<ChatMessage>
  stream?(
    config: ChatConfig,
    messages: ChatMessage[],
    onToken: (token: string) => void,
    onDone: () => void,
    onError: (err: Error) => void
  ): () => void  // returns disconnect function
}
```

---

## Hook API

### `useChat(config: ChatConfig)`

Main hook. Manages state, delegates to transport.

```ts
const {
  messages,        // ChatMessage[]
  isStreaming,     // boolean — currently receiving tokens
  error,           // string | null
  send,            // (content: string) => Promise<void>
  clear,           // () => void — reset conversation
  retry,           // () => void — resend last user message
} = useChat(config)
```

### Transport selection

`useChat` internally selects transport based on `config.transport`:

| Transport | When | Implementation |
|-----------|------|---------------|
| `rest` | Default. Full response per request. | `fetch()` → parse JSON → append message |
| `websocket` | Streaming token-by-token. | Connect to `wsPath`, append tokens as they arrive |
| `polling` | Shibboleth-style backends. | POST to create, poll for results on interval |

### Example configs per layer

```tsx
// Layer 1: Public Assistant (widget)
const publicConfig: ChatConfig = {
  title: "Ask Us Anything",
  placeholder: "How can we help?",
  transport: "rest",
  endpoint: "http://localhost",
  chatPath: "/api/ragussy/chat",
  collection: "public_docs",
  model: "glm-4.7-flash",
  showSources: true,
  showTimestamps: false,
  enterToSend: true,
  streaming: false,
  theme: "dark",
}

// Layer 2: CMS Draft Assistant
const cmsConfig: ChatConfig = {
  title: "AI Draft Assistant",
  placeholder: "Describe what you want to draft...",
  transport: "rest",
  endpoint: "http://localhost",
  chatPath: "/v1/chat/completions",
  collection: "devel_docs",
  model: "glm-4.7-flash",
  showSources: true,
  enterToSend: true,
  streaming: true,
  wsPath: "/ws/stream",
  theme: "dark",
}

// Layer 3: Knowledge Worker
const knowledgeConfig: ChatConfig = {
  title: "Knowledge Assistant",
  placeholder: "Search internal docs, draft content...",
  transport: "websocket",
  endpoint: "http://localhost",
  chatPath: "/v1/chat/completions",
  wsPath: "/ws/stream",
  collection: "devel_docs",
  model: "glm-4.7-flash",
  showSources: true,
  showTimestamps: true,
  enterToSend: true,
  streaming: true,
  theme: "dark",
}

// Layer 4: Coding Agent
const codingConfig: ChatConfig = {
  title: "Coding Agent",
  placeholder: "What should we build?",
  transport: "rest",
  endpoint: "http://localhost",
  chatPath: "/v1/chat/completions",
  model: "qwen-14b",
  showSources: false,
  showTimestamps: true,
  enterToSend: true,
  streaming: true,
  wsPath: "/ws/stream",
  extraBody: { tools: [...] },
  theme: "dark",
}

// Layer 5: SME Review (polling, not chat — but uses same library)
const reviewConfig: ChatConfig = {
  title: "Review Session",
  transport: "polling",
  endpoint: "http://localhost:8788",
  chatPath: "/api/chat-lab/sessions/{id}/send",
  pollInterval: 2500,
  showSources: false,
  showTimestamps: true,
  enterToSend: true,
  streaming: false,
  theme: "dark",
}
```

---

## Component API

### `<ChatPanel />`

The top-level component. Wires `useChat` to the visual components.

All visual primitives used by `ChatPanel` come from `@open-hax/uxx`.

```tsx
<ChatPanel
  config={layerConfig}
  headerSlot={<LayerTabs active={activeLayer} onChange={setActiveLayer} />}
  footerSlot={<StatusBadge model={config.model} collection={config.collection} />}
/>
```

Props:
| Prop | Type | Purpose |
|------|------|---------|
| `config` | `ChatConfig` | Layer configuration |
| `className` | `string?` | Wrapper override |
| `headerSlot` | `ReactNode?` | Custom header (layer tabs, title, close button) |
| `footerSlot` | `ReactNode?` | Custom footer (status, model badge) |
| `messageSlot` | `(msg: ChatMessage) => ReactNode?` | Custom message renderer override |
| `onSend` | `(content: string) => void?` | Intercept sends (for logging, analytics) |

### `<MessageBubble />`

```tsx
<MessageBubble message={msg} showTimestamp showRoleIcon />
```

### `<ChatComposer />`

```tsx
<ChatComposer
  placeholder="Ask a question..."
  enterToSend={true}
  disabled={isStreaming}
  onSend={(content) => send(content)}
/>
```

### `<SourceCitation />`

```tsx
<SourceCitation chunk={sourceChunk} onClick={() => navigate(chunk.source)} />
```

---

## How Each Layer Uses It

| Layer | Import | Config | Extra components |
|-------|--------|--------|-----------------|
| 1. Widget | `<ChatPanel config={publicConfig} />` | REST, `public_docs` | None (simple) |
| 2. CMS | `<ChatPanel config={cmsConfig} headerSlot={...} />` | REST+stream, `devel_docs` | CMS header with publish/archive actions |
| 3. Knowledge | `<ChatPanel config={knowledgeConfig} />` | WebSocket, `devel_docs` | Collection selector dropdown |
| 4. Coding | `<ChatPanel config={codingConfig} messageSlot={toolRenderer} />` | REST+stream, tools | Tool call renderer (file read, shell output) |
| 5. Review | `<ChatPanel config={reviewConfig} />` | Polling | Label overlay on assistant messages |

Each layer is a **thin wrapper** that passes config and optional slots. The chat library handles all shared behavior.

---

## Migration Path

### Phase 1: Extract from Ragussy ChatLabPage.tsx

This has the most complete chat implementation (WebSocket streaming, message lifecycle, error handling).

1. Extract `ChatMessage` type → `packages/chat-ui/src/types/`
2. Extract `connectStream()` from `ws.ts` → `packages/chat-ui/src/hooks/useChatStream.ts`
3. Extract message rendering → `MessageBubble.tsx`
4. Extract composer → `ChatComposer.tsx`
5. Build `useChat` hook combining transport + state
6. Build `ChatPanel` composing all sub-components
7. Publish as `@workspace/chat-ui`

### Phase 2: Migrate existing consumers

1. **Ragussy ChatPage.tsx** — replace inline chat with `<ChatPanel config={knowledgeConfig} />`
2. **Ragussy ChatLabPage.tsx** — replace inline chat with `<ChatPanel config={...} />`
3. **futuresight-kms KnowledgeLabeler.tsx** — import `MessageBubble`, `SourceCitation`, `ErrorBanner` from chat-ui
4. **futuresight-kms ChatWidget.tsx** — use `<ChatPanel config={publicConfig} />`

### Phase 3: New consumers

1. **CMS Dashboard** — `<ChatPanel config={cmsConfig} />` inside draft assistant panel
2. **Coding agent** — `<ChatPanel config={codingConfig} messageSlot={...} />`

### Shibboleth ChatLab.tsx

Shibboleth's ChatLab is different enough (labeling-focused, not chat-focused) that it uses `ChatPanel` for the conversation view but wraps it with its own label overlay. The polling transport adapter makes this possible without custom streaming code.

---

## Dependencies

| Dep | Version | Purpose |
|-----|---------|---------|
| `react` | `^18.0.0` | Peer dependency |
| `react-dom` | `^18.0.0` | Peer dependency |

Visual dependency:
- `@open-hax/uxx`
- `@open-hax/uxx/tokens`

No Tailwind dependency at the chat package layer. Consumers should share the same component library rather than re-theme the chat surface independently.

## Design System Rules

- default theme is Monokai
- mode and chord state are shown via `ModeIndicator` and `ChordOverlay`
- every chat action has a visible button first
- every button can advertise a chord
- mouse users are first-class; chord users are accelerated, not privileged

---

## Files to Create

| File | Size est. | Purpose |
|------|-----------|---------|
| `packages/chat-ui/package.json` | 1K | Package config, `@workspace/chat-ui` |
| `packages/chat-ui/tsconfig.json` | 0.5K | TypeScript config |
| `packages/chat-ui/src/index.ts` | 0.5K | Barrel exports |
| `packages/chat-ui/src/types/index.ts` | 2K | ChatMessage, ChatConfig, SourceChunk, ChatTransport |
| `packages/chat-ui/src/hooks/useChat.ts` | 3K | Main hook: state, send, transport dispatch |
| `packages/chat-ui/src/hooks/useChatStream.ts` | 2K | WebSocket streaming adapter |
| `packages/chat-ui/src/hooks/useChatPoll.ts` | 1.5K | Polling adapter |
| `packages/chat-ui/src/components/ChatPanel.tsx` | 2K | Top-level panel composition |
| `packages/chat-ui/src/components/ChatPanel.css` | 2K | Panel styles |
| `packages/chat-ui/src/components/MessageBubble.tsx` | 1.5K | Role-styled message |
| `packages/chat-ui/src/components/MessageBubble.css` | 1.5K | Bubble styles |
| `packages/chat-ui/src/components/ChatComposer.tsx` | 1.5K | Textarea + send |
| `packages/chat-ui/src/components/ChatComposer.css` | 1K | Composer styles |
| `packages/chat-ui/src/components/SourceCitation.tsx` | 1K | Source pill |
| `packages/chat-ui/src/components/SourceCitation.css` | 0.5K | Citation styles |
| `packages/chat-ui/src/components/TypingIndicator.tsx` | 0.5K | Animated dots |
| `packages/chat-ui/src/components/TypingIndicator.css` | 0.5K | Dots animation |
| `packages/chat-ui/src/components/ErrorBanner.tsx` | 0.5K | Dismissible error |
| `packages/chat-ui/src/components/ErrorBanner.css` | 0.3K | Error styles |
| `packages/chat-ui/src/utils/scroll.ts` | 0.5K | Auto-scroll helper |
| `packages/chat-ui/src/utils/persist.ts` | 1K | localStorage save/restore |

**Total: ~25K of shared code replacing ~15K of duplicated code across 4+ implementations, with more features (streaming, citations, typing indicator) than any single implementation has today.**

---

## Status

Draft — specified 2026-04-01.
