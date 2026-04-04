# The Promethean Stack — Full Product Line

> *120+ open-source tools. Seven product families. One person with AI built all of it.*

---

## What This Is

A complete open-source AI infrastructure stack built over two years by one person with AI assistance. Every tool is independently useful. Together, they form a platform for deploying, managing, securing, and improving AI systems.

The commercial product is the integration, deployment, and maintenance. The code is the moat. The expertise is the business.

---

## Product Families

### 1. Gateway (proxx + voxx + codex)

**The entry point.** Route requests to the right model. Manage accounts. Handle rate limits.

| Tool | What | Status |
|------|------|--------|
| **proxx** | OpenAI-compatible multi-provider LLM gateway. OAuth, account rotation, rate-limit fallback, React dashboard, Chroma-backed session search. | **External users.** npm: `@openhax/proxx` |
| **voxx** | OpenAI/ElevenLabs-compatible voice pipeline. TTS (MeloTTS) + STT (Whisper). Streaming, WebSocket. | Working. Used by Battlebussy. |
| **@openhax/codex** | OpenCode plugin for OpenAI Codex via ChatGPT subscription. | **Published on npm.** 123+ tests. |
| **codex-ts-sdk** | TypeScript client for OpenAI Codex with NAPI bindings. | **Published on npm v0.0.7.** |

**Value prop**: "Stop paying per-model API costs. Route intelligently across providers. Own your gateway."

---

### 2. Knowledge (futuresight-kms + ragussy + openplanner + shibboleth)

**The brain.** Ingest, index, search, curate, and review knowledge at scale.

| Tool | What | Status |
|------|------|--------|
| **futuresight-kms** | Multi-tenant knowledge management. CMS, HITL labeling, training export. | Designed + specced (21 specs). |
| **ragussy** | Local-first RAG + inference. FastAPI, llama.cpp, React/Vite, BGE-M3 embeddings. | Working. Full frontend. |
| **openplanner** | Event data lake with dual-tier vector search and semantic compaction. | Working. DuckDB + ChromaDB. |
| **shibboleth** | Adversarial safety evaluation DSL + HITL labeling. 7-stage pipeline. | Working. 418 tests. |

**Value prop**: "Your knowledge is scattered. We make it searchable, curatable, and trainable."

---

### 3. Security (our-gpus + sintel + threat-radar)

**The perimeter.** Find exposed AI. Resolve contacts. Generate remediation.

| Tool | What | Status |
|------|------|--------|
| **our-gpus** | Ollama instance discovery. Shodan scanning, host probing, contact resolution, lead management. | **Tracking 3,400+ real hosts.** Full UI. |
| **ourllamas** | Passive intake helper for our-gpus. RDAP contact reports. | Working. |
| **sintel** | Infrastructure signals intelligence. Discovers, verifies, enriches public-facing evidence. | Designed. |
| **threat-radar** | MCP control plane for threat radars. Templates, sources, assessment packets, clock visualization. | Working. |
| **hormuz-clock** | Multi-model thread assessment collector. Deterministic clock reducer. | Working. |

**Value prop**: "Your AI is probably exposed. We find it before someone else does."

---

### 4. Agent Runtime (openclawssy + cephalon + pantheon)

**The hands.** Run AI agents safely. Orchestrate multi-agent systems.

| Tool | What | Status |
|------|------|--------|
| **openclawssy** | Security-first Go agent runtime. One binary, multi-channel, deny-by-default capabilities, sandboxed workspaces. | **External-facing docs.** "Launch in 3 commands." |
| **cephalon** | "Always-running mind" with vector memory. Multi-persona Discord bot system (Duck, OpenHax, OpenSkull). | Working. PM2 managed. |
| **pantheon** | Functional TypeScript agent framework. Context management, actor lifecycle, MCP integration. | Working. `@promethean-os/pantheon`. |
| **agent-actors** | OpenCode plugin for session indexing and agent orchestration. | Working. npm package. |

**Value prop**: "Agents need guardrails. We provide the runtime, the memory, and the safety."

---

### 5. Dev Tools (kanban + chronos + eta-mu-github + reconstituter)

**The workflow.** Manage work, track time, automate reviews.

| Tool | What | Status |
|------|------|--------|
| **kanban** | FSM-based markdown kanban with GitHub + Trello sync. `bin/eta-mu-board` CLI. | Working. Canonical workspace tool. |
| **chronos** | Contractor time tracker with agent integration. Web UI, REST API, SQLite. | Working. |
| **eta-mu-github** | GitHub automation. PR management, issue refinement, review coordination, auto-fix. | Working. |
| **reconstituter** | Session search and context recovery. Indexes OpenCode sessions into ChromaDB. | Working. |
| **opencode-skills** | Curated skill definitions for OpenCode agents. | Published. |

**Value prop**: "Your development workflow is the bottleneck. We automate the boring parts."

---

### 6. Safety (shibboleth + mythloom + resume-workbench)

**The mirror.** Evaluate adversarial robustness. Analyze bias. Audit AI outputs.

| Tool | What | Status |
|------|------|--------|
| **shibboleth** | Adversarial prompt evaluation DSL. Multilingual, leakage-proof, full provenance. | Working. 418 tests, 3129 assertions. |
| **mythloom** | Worker-side resume analysis. Anti-extractive, explainable fit signals. | Working. |
| **resume-workbench** | Local CLI for resume/JD analysis. Token coverage, cosine similarity, ATS scoring. | Working. |

**Value prop**: "AI safety isn't optional. We make it measurable and reproducible."

---

### 7. Social (battlebussy + social-publisher)

**The voice.** AI-generated content for social platforms.

| Tool | What | Status |
|------|------|--------|
| **battlebussy** | AI-vs-AI sports commentary bot using Voxx TTS. | Working. |
| **social-publisher** | Publish content to Bluesky, Discord, Reddit, X. | Working. MCP server. |
| **hormuz-clock→social** | Publish risk clock snapshots to social platforms. | Working. |

**Value prop**: "AI can create content faster than humans. We make it publishable."

---

## How They Connect

```
                    ┌──────────────────────────────────┐
                    │           GATEWAY                 │
                    │  proxx (LLM routing)              │
                    │  voxx (voice)                     │
                    │  codex (coding agent)             │
                    └───────────┬──────────────────────┘
                                │
                    ┌───────────▼──────────────────────┐
                    │           KNOWLEDGE               │
                    │  openplanner (data lake)          │
                    │  ragussy (RAG + inference)        │
                    │  futuresight-kms (CMS + labels)   │
                    │  shibboleth (safety evaluation)   │
                    └───────────┬──────────────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
     ┌────────────────┐ ┌──────────────┐ ┌──────────────┐
     │   SECURITY     │ │    AGENTS    │ │  DEV TOOLS   │
     │ our-gpus       │ │ openclawssy  │ │ kanban       │
     │ sintel         │ │ cephalon     │ │ chronos      │
     │ threat-radar   │ │ pantheon     │ │ eta-mu-github│
     └────────────────┘ └──────────────┘ └──────────────┘
```

---

## The FutureSight Pitch

FutureSight doesn't get one product. They get a platform.

| FutureSight Offering | Product Family | Revenue Model |
|---------------------|---------------|---------------|
| "We build your AI knowledge platform" | Knowledge | Per-tenant SaaS |
| "We audit your AI exposure" | Security | Per-scan / retainer |
| "We manage your AI infrastructure" | Gateway + Agent Runtime | Managed service |
| "We evaluate your AI safety" | Safety | Per-evaluation |

Each offering uses the same infrastructure. Each can be sold independently. Together, they make FutureSight an AI infrastructure consultancy — not just a design shop.

---

## Org Structure (already exists)

| Org | Purpose | Products |
|-----|---------|----------|
| `open-hax` | Tools I intend other people to use | proxx, voxx, codex, kanban, eta-mu-github |
| `octave-commons` | Experimental, art, research | shibboleth, promethean, gates-of-aker, mythloom |
| `riatzukiza` | My infrastructure, published anyway | threat-radar, hormuz, book-of-shadows, TANF-app |
| `shuv` | Battle stuff | our-gpus, battlebussy, codex-desktop-linux |
| `ussyverse` | Community projects | openclawssy, kanban |
| `moofone` | SDK work | codex-ts-sdk |
| `mojomast` | Creative tools | ragussy |

---

## What Exists vs What's Designed

| Family | Code exists | Designed | Specced | External users |
|--------|:-----------:|:--------:|:-------:|:--------------:|
| Gateway | ✓ | ✓ | partial | **proxx + codex** |
| Knowledge | ✓ (ragussy, openplanner, shibboleth) | ✓ | ✓ (21 specs) | — |
| Security | ✓ (our-gpus) | ✓ | partial | **3,400+ hosts** |
| Agent Runtime | ✓ (openclawssy, cephalon) | ✓ | partial | — |
| Dev Tools | ✓ (kanban, chronos) | ✓ | partial | — |
| Safety | ✓ (shibboleth, mythloom) | ✓ | partial | — |
| Social | ✓ (battlebussy, social-publisher) | ✓ | partial | — |

---

## The Asymmetric Principle

One person built all of this. With AI.

- 120+ projects
- 7 product families
- Working code in 7+ languages (TypeScript, Python, Clojure, Go, Rust, SQL, Datalog)
- Production infrastructure across 4+ cloud hosts
- External users on at least 3 projects

This is what AI-assisted development looks like when you're already competent. Not "AI replaces developers." AI makes one developer into a team.

The product line is the proof. The tools are the argument. The code is the evidence.

---

## Status

Specified 2026-04-01. Full product line definition.
