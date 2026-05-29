---
uuid: "knoxx-knowledge-ops-contract-runtime-dod-restructure"
title: "Contract Runtime: Data-Oriented Restructure"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.383Z"
source: "specs/epics/knowledge-ops-contract-runtime-dod-restructure.md"
points: null
category: epics
---

# Contract Runtime: Data-Oriented Restructure

> Source: `specs/epics/knowledge-ops-contract-runtime-dod-restructure.md`

**Date:** 2026-04-19  
**Status:** Accepted  
**Supersedes:** `docs/notes/2026.04.17.10.11.17.md`

---

## Framing: Why "Actor"

The system needs a single term for any decision-making entity — human or AI. "User" is human-only by convention. "Agent" is AI-only by convention. "Actor" is semantically neutral: it denotes any entity that has agency and takes actions in the system. The term appears in the actor model of computation, in theatre (a role that acts), and in legal contexts (a party who acts). None of these connotations conflict with the intended meaning here.

- `:actor/kind :human` — a person operating through the UI or API
- `:actor/kind :ai` — an AI agent operating under a contract
- All actors have an id, roles, and capabilities. No other distinction at the data layer.

---
