# Knowledge Ops — Translation Review Epic

Date: 2026-04-06
Status: epic wrapper
Total Points: 15
Parent: `knowledge-ops-shibboleth-lite-labeling.md`
Depends on: OpenPlanner MongoDB migration (complete)

---

## Purpose

This is an **epic wrapper**, not a direct execution spec.
The implementation work is split into child specs capped at 5 story points.

Deliver end-to-end translation review capability:
- MT pipeline translates devel docs corpus (en → es, en → de)
- Knoxx frontend provides human review workflow (Knoxx owns all UI)
- Corrections stored in OpenPlanner as canonical truth
- Training data export for SFT fine-tuning

**Shibboleth provides backend pipeline mechanics only. Knoxx owns the product UI.**

---

## Story Point Summary

| Child Spec | Description | Points | Dependencies |
|------------|-------------|--------|--------------|
| `knowledge-ops-translation-routes.md` | OpenPlanner translation segment CRUD + permissions | 5 | MongoDB migration |
| `knowledge-ops-translation-export.md` | SFT export pipeline + manifest | 2 | Translation routes |
| `knowledge-ops-translation-review-ui.md` | Shibboleth UI wiring + auth context | 5 | Translation routes, Export |
| `knowledge-ops-translation-mt-pipeline.md` | GLM-5 MT pipeline for devel docs | 3 | Translation routes |
| **Total** | | **15** | |

---

## Dependency Graph

```
MongoDB Migration (external)
         │
         ▼
┌─────────────────────┐
│ Translation Routes  │ (5 pts)
└─────────┬───────────┘
          │
    ┌─────┴─────┬─────────────┐
    ▼           ▼             ▼
┌───────┐ ┌───────────┐ ┌─────────────┐
│ Export│ │ Review UI │ │ MT Pipeline │
│ (2 pt)│ │  (5 pts)  │ │   (3 pts)   │
└───────┘ └───────────┘ └─────────────┘
```

Translation Routes is the critical path. Export and Review UI can proceed in parallel after routes land. MT Pipeline can be deferred if time is short (manual seeding suffices for demo).

---

## Architectural Boundary

```
┌─────────────────────────────────────────────────────────────────┐
│                         OpenPlanner                              │
│  (Canonical data truth — all content, sessions, translations)   │
│                                                                  │
│  Collections:                                                    │
│  - events (translation segments, labels, corrections)           │
│  - documents (source content, published translations)           │
│  - graph nodes/edges (knowledge graph)                          │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ HTTP API
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                       Knoxx Backend                              │
│  (Request routing, auth, API proxy, agent runtime)              │
│                                                                  │
│  - Proxies translation API requests to OpenPlanner              │
│  - Enforces tenant/org scope on all requests                    │
│  - Injects auth context headers                                 │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ HTTP API
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                       Knoxx Frontend                             │
│  (Product UI — translation review workbench)                    │
│                                                                  │
│  - Translation review page at /translations                      │
│  - Three-panel layout: source, translation, labels              │
│  - Calls Knoxx backend API                                       │
└─────────────────────────────────────────────────────────────────┘
```

**What lives where:**

| Data | Home | Reason |
|------|------|--------|
| Source documents | OpenPlanner | Content truth |
| Translation segments | OpenPlanner | Content truth |
| Labels + corrections | OpenPlanner | Content truth |
| Training exports | OpenPlanner | Derived from content truth |
| Review UI | Knoxx Frontend | Product surface |
| Tenant/Org/User/RBAC | Knoxx Postgres (policy-db) | Access control |
| Pipeline definitions | Shibboleth (backend only) | DSL orchestration |

---

## Resolved Questions

1. **MT Provider**: GLM-5 (via existing OpenAI-compatible proxy stack)
2. **Seed Data**: devel docs corpus — this makes the system immediately useful for workspace operations
3. **Language Pairs**:
   - Priority 1: en → es (friend in Spain can assist with review)
   - Priority 2: en → de (friends in Germany can assist)
4. **Reviewer Accounts**: At least 2 users initially; recruiting multilingual collaborators via Discord

---

## Demo Readiness Checklist

For the client demo in 9 days:

- [ ] OpenPlanner migration to MongoDB complete and stable
- [ ] Translation segment routes implemented
- [ ] devel docs corpus seeded for translation (en → es, en → de)
- [ ] Shibboleth UI wired to load segments
- [ ] Label submission working end-to-end
- [ ] One complete review workflow shown (pending → approved)
- [ ] SFT export demonstrated (downloadable JSONL)

**Nice to have:**
- [ ] Corrected text editing in UI
- [ ] Manifest export with statistics
- [ ] Both language pairs (es, de) working

**Recruiting:**
- [ ] Spain-based reviewer confirmed (es)
- [ ] Germany-based reviewer confirmed (de)

---

## Child Specs

- [ ] `knowledge-ops-translation-routes.md` (5 pts) — OpenPlanner CRUD + permissions
- [ ] `knowledge-ops-translation-export.md` (2 pts) — SFT export + manifest
- [ ] `knowledge-ops-translation-review-ui.md` (5 pts) — Knoxx frontend review page
- [ ] `knowledge-ops-translation-mt-pipeline.md` (3 pts) — GLM-5 translation pipeline

---

## Status

Epic wrapper — pull child specs for execution.
