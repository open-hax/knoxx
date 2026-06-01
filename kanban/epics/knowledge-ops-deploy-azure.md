---
uuid: "knoxx-knowledge-ops-deploy-azure"
title: "The Lake — Azure Deployment Spec"
status: blocked
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.384Z"
source: "specs/epics/knowledge-ops-deploy-azure.md"
points: null
category: epics
---
# The Lake — Azure Deployment Spec

> Source: `specs/epics/knowledge-ops-deploy-azure.md`

> *Azure AI Search + OpenAI + Cosmos DB. The managed path.*

---

## Provider Mapping

| Logical Component | Azure Service | Config Key |
|-------------------|--------------|------------|
| Search (vector + FTS + hybrid) | **Azure AI Search** | `SEARCH_PROVIDER=azure-ai-search` |
| Embeddings | **Azure OpenAI Service** (text-embedding-3-small/large) | `EMBEDDING_PROVIDER=azure-openai` |
| Structured storage | **Azure Cosmos DB** (MongoDB API or NoSQL) | `STORAGE_PROVIDER=cosmos-db` |
| Blob storage | **Azure Blob Storage** | `BLOB_PROVIDER=azure-blob` |
| Job queue | **Azure Service Bus** | `QUEUE_PROVIDER=azure-service-bus` |
| Auth | **Microsoft Entra ID** (Azure AD) | `AUTH_PROVIDER=azure-ad` |
| App hosting | **Azure Container Apps** | — |
