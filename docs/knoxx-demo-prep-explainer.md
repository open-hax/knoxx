# Knoxx: Client Demo Preparation

**Prepared for:** [Client Name]
**Demo Date:** [Insert Date]
**Document Date:** 2026-04-06

---

## What Is Knoxx?

Knoxx is a **secure knowledge vault and intelligent workbench** — a system that transforms how your organization captures, organizes, and retrieves institutional knowledge.

Think of it as a private, self-hosted AI assistant that:
- Knows your documents, codebases, and institutional memory
- Respects organizational boundaries and access controls
- Runs entirely on infrastructure you control
- Learns from every conversation and project session

The name comes from Fort Knox: a secure vault for your knowledge. The "garden" concept draws from the idea of cultivating living, growing knowledge spaces rather than static archives.

---

## Core Capabilities

### Document Intelligence
Knoxx ingests your documents and makes them queryable through natural language. Upload PDFs, markdown files, code repositories, or connect it to existing data sources. Ask questions and get grounded answers with citations back to source documents.

### Knowledge Lakes
Documents are automatically classified into themed "lakes":
- **Docs Lake**: Policies, procedures, documentation
- **Code Lake**: Source code and technical artifacts
- **Data Lake**: Structured data, spreadsheets, databases
- **Config Lake**: Configuration files, infrastructure definitions

Each lake can be queried independently or federated across your organization.

### Session Memory
Every conversation with Knoxx is preserved. The system builds a **graph of knowledge** connecting:
- Questions asked and answers given
- Documents referenced
- Decisions made
- Topics explored

This means Knoxx can "remember" prior conversations and draw connections across sessions — not just retrieve isolated document fragments.

### Agent Runtime
Knoxx isn't just a chat interface. It's a **live agent runtime** that can:
- Execute multi-step research tasks
- Draft and revise documents
- Run controlled commands with oversight
- Integrate with your existing tools (Discord, Bluesky, email)

### Organizational Control
Every action is scoped by:
- **Organization**: Your company's tenant boundary
- **Role**: What actions each user can perform
- **Lake Access**: Which knowledge lakes each user can query
- **Tool Permissions**: Which agent capabilities each user can invoke

---

## Current Status

### What's Working Now
- **Backend Runtime**: Modern, stable architecture (ClojureScript + Node.js)
- **Document Ingestion**: Upload and classify documents into knowledge lakes
- **Query Interface**: Natural language search across your corpus
- **Session Management**: Resume prior conversations, search historical sessions
- **Admin Dashboard**: Manage organizations, users, roles, and permissions
- **Live Agent Controls**: Real-time visibility into running tasks with intervention capabilities

### What's Actively Being Hardened
- **Tenant Enforcement**: Ensuring every request is properly scoped to the right organization
- **Policy Enforcement**: Tool permissions and access controls applied at execution time
- **Graph Memory Coherence**: Ensuring the knowledge graph accurately reflects all ingested content

These are foundational reliability investments — not new features, but guarantees that the system behaves correctly under multi-tenant, multi-user loads.

---

## What You'll See in the Demo

### 1. Workbench Overview
The main Knoxx interface showing:
- **Context Bar**: Your current organizational context and active knowledge lake
- **Agent Runtime**: Live view of running tasks and recent sessions
- **Scratchpad**: Quick notes and drafts that can be promoted to documents

### 2. Document Ingestion
- Browse your connected data sources
- See how documents are classified into lakes
- Watch ingestion progress and health

### 3. Natural Language Query
- Ask questions across your knowledge corpus
- See grounded answers with source citations
- Filter queries by lake, date, or topic

### 4. Session Memory
- Resume a prior conversation
- See how Knoxx connects related topics across sessions
- Explore the knowledge graph visually

### 5. Administrative Controls
- User and role management
- Permission and tool policy configuration
- Organization and lake settings

### 6. (If Ready) Live Agent Task
- Demonstrate a multi-step research task
- Show real-time progress and intervention
- Export results to document or external platform

---

## Roadmap Highlights

### Near-Term (Next 30 Days)
- Hardened tenant isolation and access enforcement
- Reliable cross-session memory and graph coherence
- Enhanced query performance across large corpora

### Medium-Term (Next Quarter)
- CMS-style publication workflows (draft → review → publish)
- External knowledge source connectors (Notion, Confluence, GitHub)
- Advanced graph traversal and relationship discovery

### Long-Term Vision
- Multi-cloud and self-hosted deployment options
- Provider portability (run on AWS, Azure, GCP, or bare metal)
- Advanced compliance and audit capabilities (PII handling, retention policies)

---

## Technical Architecture (For Your Technical Team)

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Frontend       │────▶│  Knoxx Backend   │────▶│  OpenPlanner    │
│  (React)        │     │  (CLJS/Node)     │     │  (Data Lake)    │
│                 │◀────│                  │◀────│                 │
│  Workbench UI   │     │  Agent Runtime   │     │  Vector Search  │
│  Admin Panel    │     │  Query Engine    │     │  Graph Memory   │
└─────────────────┘     └──────────────────┘     └─────────────────┘
```

**Data Flow:**
1. Frontend provides operator UI for ingestion, query, and administration
2. Backend handles auth, routing, agent orchestration, and tool execution
3. OpenPlanner stores documents and events with full-text + vector search

**Infrastructure:**
- PostgreSQL: User accounts, organizations, roles, policies
- MongoDB: Document storage and event logs
- Redis: Session state and real-time coordination
- ChromaDB: Vector embeddings for semantic search

**Deployment:**
- Docker Compose for local and self-hosted deployments
- Designed for Kubernetes on major cloud providers
- Air-gapped deployment possible for sensitive environments

---

## Questions to Prepare

To make the demo most valuable, consider:

1. **Knowledge Domains**: What types of documents and knowledge are most critical to your organization?

2. **User Roles**: How are your teams structured? Who needs read-only access vs. authoring vs. administration?

3. **Data Sources**: Where does your institutional knowledge currently live? (File shares, wikis, code repos, email archives)

4. **Use Cases**: What questions do people ask repeatedly? What decisions require synthesizing information from multiple sources?

5. **Integration Points**: Does Knoxx need to connect to any existing systems? (Ticketing, documentation platforms, communication tools)

6. **Compliance Requirements**: Any specific regulatory or security constraints? (SOC 2, HIPAA, data residency)

---

## Contact

For questions before the demo:
- **Technical**: [Your technical contact]
- **Scheduling**: [Your scheduling contact]

We look forward to showing you what Knoxx can do for your organization.
