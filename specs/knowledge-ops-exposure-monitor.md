# Exposure Monitor — Product Spec

> *Find exposed AI. Resolve contacts. Generate outreach. Defend the perimeter.*

---

## Purpose

Define the Exposure Monitor product — an external AI infrastructure exposure detection and lead generation platform built on the existing `our-gpus` codebase.

---

## What It Is

A platform that:
1. **Discovers** exposed AI endpoints across the internet (Ollama, OpenAI-compatible proxies, custom inference servers)
2. **Verifies** each endpoint (GPU specs, model lists, latency, geo data)
3. **Resolves** contact information (RDAP, security.txt, TLS certs, ASN lookups)
4. **Scores** leads based on exposure severity, contact quality, and remediation feasibility
5. **Generates** outreach queues for responsible disclosure and business development

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  EXPOSURE MONITOR                         │
│                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  Discovery   │  │ Verification│  │  Contact     │     │
│  │  Engine      │──│  Engine     │──│  Resolution  │     │
│  │             │  │             │  │  Engine      │     │
│  │  - Shodan   │  │  - Probe    │  │              │     │
│  │  - Tor      │  │  - GPU spec │  │  - RDAP      │     │
│  │  - Masscan  │  │  - Models   │  │  - sec.txt   │     │
│  └─────────────┘  └─────────────┘  │  - TLS cert  │     │
│                                     └──────┬──────┘     │
│                                            │             │
│  ┌─────────────────────────────────────────▼──────────┐ │
│  │              Lead Management                        │ │
│  │                                                     │ │
│  │  Score → Cluster → Review → Approve → Export        │ │
│  │                                                     │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐ │
│  │              Outreach Engine                        │ │
│  │                                                     │ │
│  │  Campaign clusters by ASN / region / org            │ │
│  │  Contact templates                                  │ │
│  │  Disclosure tracking                                │ │
│  └─────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

---

## Existing Code

| Component | File | Status |
|-----------|------|--------|
| FastAPI app | `orgs/shuv/our-gpus/app/main.py` | Working |
| Shodan queries | `orgs/shuv/our-gpus/app/shodan_queries.py` | Working |
| Scan strategies | `orgs/shuv/our-gpus/app/masscan.py` | Working (masscan + tor + shodan) |
| Lead enrichment | `orgs/shuv/our-gpus/app/lead_services.py` | Working |
| Config | `orgs/shuv/our-gpus/app/config.py` | Working |
| React frontend | `orgs/shuv/our-gpus/web/` | Working |
| Leads page | `orgs/shuv/our-gpus/web/src/pages/Leads.tsx` | Working |
| Admin workbench | `orgs/shuv/our-gpus/web/src/components/ScannerWorkbench.tsx` | Working |
| Docker Compose | `services/our-gpus/compose.yaml` | Working |
| Tor probing | `services/our-gpus/README.md` | Working |
| Intake helper | `services/ourllamas/` | Working |

---

## Specs

| Spec | File | Status |
|------|------|--------|
| Discovery strategies | `orgs/shuv/our-gpus/specs/discovery-strategies.md` | Written |
| Shodan strategy | `orgs/shuv/our-gpus/specs/shodan-strategy.md` | Written |
| Leads management | `orgs/shuv/our-gpus/specs/leads-management.md` | Written |
| WireGuard strategy | `orgs/shuv/our-gpus/specs/wireguard-connect-strategy.md` | Written |

---

## Research Notes

| Note | File |
|------|------|
| Product pivot | `~/docs/notes/research/our-gpus-product-pivot.md` |
| UI design | `~/docs/notes/dev/our-gpus-ui-design.md` |
| Shodan country exclusion | `~/docs/notes/research/shodan-query-country-exclusion.md` |
| Exposed LLM article | `~/docs/notes/research/exposed-llm-servers-linkedin-draft.md` |
| Responsible disclosure | `~/docs/notes/research/responsible-disclosing-aligned-countries.md` |
| MSP brainstorm | `~/docs/notes/research/msp-contact-resolution-brainstorm.md` |
| Contact PRD v0.1 | `~/docs/notes/dev/contact-resolution-prd-v0.1.md` |
| Contact MVP spec | `~/docs/notes/dev/contact-resolution-mvp-spec.md` |
| Contact tech spec | `~/docs/notes/dev/contact-info-acquisition-technical-spec.md` |
| Postgres + FastAPI spec | `~/docs/notes/dev/contact-resolution-postgres-fastapi-spec.md` |
| Investor pitch | `~/docs/notes/personal/investor-pitch-linkedin-article.md` |
| Wedge strategy | `~/docs/notes/personal/category-positioning-wedge-strategy.md` |

---

## Integration with Product Line

### → Knowledge Ops
Exposure Monitor finds orgs with exposed AI → Knowledge Ops provides the secure deployment and management platform. "We found your exposure. Here's how we fix it."

### → The Lake
Scan results, probe data, and contact resolutions stored in The Lake for historical analysis and trend detection. Compaction builds knowledge packs about exposure patterns.

### → Provider Abstraction
Exposure Monitor uses the same provider layer for storage (MongoDB/DynamoDB/Cosmos DB) and search (Azure AI Search/OpenSearch/ChromaDB). Deployable on any cloud.

---

## Lead Scoring Model

| Factor | Weight | Source |
|--------|--------|--------|
| Exposure severity | 30% | Open auth, public endpoint, sensitive models |
| Contact quality | 25% | RDAP match, security.txt present, TLS cert owner |
| Organization size | 15% | ASN analysis, IP block ownership |
| Geographic risk | 15% | Country, jurisdiction, data sovereignty |
| Remediation feasibility | 15% | Can we reach the right person? |

**Outreach threshold**: Score ≥ 70 → recommend outreach.

---

## Outreach Workflow

```
Lead discovered
    │
    ├─ Score ≥ 70? → Queue for review
    │
    ├─ Human reviews: APPROVE or SUPPRESS
    │
    ├─ APPROVED → Generate contact package:
    │      - Organization info (RDAP, ASN, geo)
    │      - Exposure details (what's open, what's running)
    │      - Recommended remediation steps
    │      - Contact channel (security.txt email, RDAP abuse)
    │
    ├─ Export to outreach tool (email, LinkedIn, direct)
    │
    └─ Track: disclosure status, response, remediation
```

---

## Status

Specified 2026-04-01. Product line integration.
