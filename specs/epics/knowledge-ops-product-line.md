# The Lake — Product Line Spec

> *AI that refutes bullshit faster than anyone can write it. One person with AI is like 10, 100, or 1000 people.*

---

## Purpose

Define the complete product line built on shared infrastructure: knowledge management, security exposure monitoring, and adversarial safety testing. All open source. All multi-provider. The product is the integration, the expertise, and the ongoing maintenance.

---

## The Vision

Getting AI into as many hands as possible is how we fight information asymmetry. When one person with AI can match a hundred without, the asymmetry of resources becomes less relevant than the asymmetry of competence.

This product line is built on that principle:

- **Knowledge Ops** gives small teams the ability to manage, curate, and deploy their institutional knowledge at scale — something that used to require a 50-person content team.
- **Exposure Monitor** gives small security teams the ability to audit AI infrastructure exposure across the internet — something that used to require a SOC.
- **The Lake** gives anyone the ability to search, compact, and reason over their data — something that used to require a data engineering team.
- **Shibboleth** gives safety researchers the ability to evaluate adversarial robustness of AI systems — something that used to require a red team.

Each product alone is useful. Together, they're a platform.

---

## Product Line

### Product 1: Knowledge Ops (futuresight-kms)

**What**: Multi-tenant domain-aware knowledge management platform.
**Who**: Agencies, consultancies, managed service providers.
**What it does**: Ingest client knowledge, auto-translate, expert review, training data export, CMS curation, public chat widget.
**Deployment**: Azure, AWS, self-hosted, local.

| Layer | Function |
|-------|---------|
| 1. Public Assistant | Customer-facing chat widget |
| 2. Agent-Aware CMS | Knowledge curation, public/internal boundary |
| 3. Knowledge Worker | Employee-facing internal RAG |
| 4. Coding Agent | Dev teams, optional |
| 5. SME Review | Shibboleth-powered HITL labeling |

**Specs**: `knowledge-ops-*.md` (21 specs, 246KB)

---

### Product 2: Exposure Monitor (our-gpus)

**What**: External AI infrastructure exposure detection and lead generation platform.
**Who**: MSPs, MSSPs, security consultancies, responsible disclosure teams.
**What it does**: Scan for exposed Ollama/OpenAI/proxy instances, resolve contacts via RDAP/security.txt, score leads, generate outreach queues.

| Component | Function |
|-----------|---------|
| Discovery | Shodan queries, Tor probing, masscan integration |
| Verification | Probe hosts for GPU specs, model lists, latency, geo |
| Contact Resolution | RDAP lookups, security.txt parsing, TLS cert extraction |
| Lead Management | Score, cluster, approve, export |
| Outreach | Campaign clusters by ASN/region/org |

**Specs**: `exposure-monitor-*.md` (to be created)

---

### Product 3: The Lake (openplanner)

**What**: Event data lake with tiered vector search and semantic compaction.
**Who**: Any team that generates AI-related data (sessions, chat, documents, social feeds).
**What it does**: Ingest events/documents, embed with tiered models (hot/warm/compact), compact related content into semantic packs, search with RRF fusion.

| Tier | Model | Purpose |
|------|-------|---------|
| Hot | nomic/qwen3-0.6b | Raw events, instant recall |
| Warm | qwen3-embedding:4b | Document chunks |
| Compact | qwen3-embedding:8b | Semantic packs |

**Specs**: `knowledge-ops-the-lake.md`, `knowledge-ops-mongodb-vector-unification.md`

---

### Product 4: Shibboleth

**What**: Adversarial safety testing and dataset labeling DSL.
**Who**: AI safety teams, red teams, researchers.
**What it does**: Define attack taxonomies, generate multilingual adversarial datasets, evaluate model robustness, produce training data for safety tuning.

| Component | Function |
|-----------|---------|
| DSL | Clojure macros for attack families, harm categories, transforms |
| Pipeline | 7-stage deterministic pipeline with leakage-proof splits |
| Evaluation | LLM judges, adversarial benchmarks |
| HITL | Chat lab for human-in-the-loop labeling |

**Specs**: `knowledge-ops-shibboleth-lite-labeling.md`

---

## Shared Infrastructure

All four products share the same infrastructure layer:

```
┌──────────────────────────────────────────────────────────────┐
│                      PRODUCT LAYER                            │
│  Knowledge Ops │ Exposure Monitor │ The Lake │ Shibboleth     │
├──────────────────────────────────────────────────────────────┤
│                    SHARED INFRASTRUCTURE                      │
│                                                              │
│  packages/knowledge-lake/     Provider abstraction layer     │
│  packages/chat-ui/            Shared chat components          │
│  packages/futuresight-kms/    Schemas, bridge clients, UI    │
│                                                              │
│  Provider adapters:  Azure │ AWS │ Self-Hosted │ Local       │
├──────────────────────────────────────────────────────────────┤
│                    OPEN SOURCE TOOLS                          │
│  OpenPlanner │ Shibboleth │ proxx │ fork_tales │ our-gpus    │
└──────────────────────────────────────────────────────────────┘
```

---

## How They Connect

### Knowledge Ops ↔ Exposure Monitor

The exposure monitor finds organizations with exposed AI infrastructure. The knowledge platform helps them secure and manage it.

**Flow**: Exposure Monitor discovers exposed instance → resolves contact → Knowledge Ops delivers the remediation and secure deployment.

**Value prop to FutureSight**: "We don't just build your clients' AI. We find the ones who need help and bring them to you."

### Knowledge Ops ↔ The Lake

The Lake is the data layer underneath Knowledge Ops. It handles ingestion, search, and compaction. Knowledge Ops adds the CMS, labeling, and tenant management on top.

**Flow**: Documents ingested → Lake compacts and indexes → Knowledge Ops provides the curation and review layer → public widget serves curated knowledge.

### Knowledge Ops ↔ Shibboleth

Shibboleth powers the SME review layer in Knowledge Ops. The labeling DSL and HITL UI are the same infrastructure, pointed at corporate QA instead of adversarial safety.

**Flow**: Knowledge Ops interactions → sampled for review → Shibboleth chat lab → labeled data → training pipeline.

### Exposure Monitor ↔ The Lake

Exposure events (discovered instances, scan results, contact resolutions) are stored in The Lake for historical analysis and trend detection.

**Flow**: Shodan scan → Lake ingests as events → compaction builds knowledge packs about exposure trends → dashboard shows exposure over time.

---

## The Business Model

### Open Source (all four products)

Every tool, library, and pipeline is open source:
- OpenPlanner (data lake)
- Shibboleth (labeling DSL)
- our-gpus (exposure scanning)
- proxx (LLM gateway)
- fork_tales (graph substrate)
- All provider adapters
- All schemas and data models

### Commercial (what you sell)

| Revenue stream | What | Who pays |
|---------------|------|----------|
| **Platform integration** | Wiring pieces together for specific client domains | Agencies like FutureSight |
| **Deployment & ops** | Cloud setup, monitoring, maintenance | Enterprise clients |
| **Managed service** | Ongoing monitoring, alerting, remediation | MSPs, MSSPs |
| **Custom schemas** | Domain-specific label ontologies, training pipelines | Vertical clients |
| **Security audits** | Exposure assessment + remediation | Any org with AI infrastructure |

### The FutureSight Play

FutureSight becomes the first integration partner. They offer their clients:
1. **Knowledge platform** — "We curate your AI knowledge"
2. **Security audit** — "We find your exposed AI infrastructure"
3. **Ongoing monitoring** — "We keep you safe"

You provide the infrastructure. They provide the client relationships. You split the revenue.

---

## The Asymmetric Principle

One person with AI is like 10, 100, or 1000 people.

This workspace proves it. Hundreds of thousands of files. Thousands of specs. Multiple production-grade open-source projects. Built by one person with AI assistance, over two years.

The product line is the distillation of that proof:
- **Knowledge Ops** makes one content person as productive as ten.
- **Exposure Monitor** makes one security person as productive as a SOC.
- **The Lake** makes one data person as productive as a data engineering team.
- **Shibboleth** makes one safety researcher as productive as a red team.

When two AIs go at each other, they cancel out. The asymmetry returns to the humans. And the human with better tools, better training, and better judgment wins.

That's what this is about. Not hype. Not "AI will replace everyone." Better tools in the hands of competent people. Open source. No lock-in. No gatekeepers.

---

## Status

Specified 2026-04-01. Product line definition.
