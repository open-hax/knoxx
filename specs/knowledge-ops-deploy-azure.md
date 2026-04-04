# The Lake — Azure Deployment Spec

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
| Database (labels) | **Azure Database for PostgreSQL** | — |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  Azure Container Apps                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ CMS API  │  │ Label API│  │ Widget   │               │
│  │          │  │ (km_labels)│ │ Proxy    │               │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘               │
│       │              │              │                     │
│       └──────────────┼──────────────┘                     │
│                      │                                    │
│  ┌───────────────────▼────────────────────┐              │
│  │         Provider Abstraction Layer      │              │
│  └──┬──────────┬──────────┬──────────┬────┘              │
│     │          │          │          │                    │
└─────┼──────────┼──────────┼──────────┼────────────────────┘
      │          │          │          │
      ▼          ▼          ▼          ▼
 Azure AI    Azure      Cosmos DB  Azure Blob
 Search      OpenAI                Storage
 (vectors    (embeddings (docs,    (binaries,
 + FTS)      + chat)     metadata) PDFs)
      │
      ▼
 Azure PostgreSQL
 (labels, tenants, km_labels)
```

---

## Embedding Tier Configuration

| Tier | Azure OpenAI Model | Dimensions | Cost |
|------|-------------------|------------|------|
| Hot | text-embedding-3-small | 512 | $0.02/1M tokens |
| Warm | text-embedding-3-large | 3072 | $0.13/1M tokens |
| Compact | text-embedding-3-large | 3072 | $0.13/1M tokens |

**No GPU needed.** Embeddings are API calls. Cost scales with usage, not uptime.

---

## Azure AI Search Configuration

Azure AI Search gives you vector search, full-text search, and hybrid search (with RRF) in one service. This replaces MongoDB+mongot.

### Index schema (documents)

```json
{
  "name": "documents",
  "fields": [
    { "name": "id", "type": "Edm.String", "key": true },
    { "name": "tenant_id", "type": "Edm.String", "filterable": true },
    { "name": "title", "type": "Edm.String", "searchable": true },
    { "name": "content", "type": "Edm.String", "searchable": true },
    { "name": "visibility", "type": "Edm.String", "filterable": true, "facetable": true },
    { "name": "domain", "type": "Edm.String", "filterable": true, "facetable": true },
    { "name": "language", "type": "Edm.String", "filterable": true },
    { "name": "source", "type": "Edm.String", "filterable": true },
    { "name": "embedding_hot", "type": "Collection(Edm.Single)", "searchable": true, "dimensions": 512, "vectorSearchProfile": "hot-profile" },
    { "name": "embedding_warm", "type": "Collection(Edm.Single)", "searchable": true, "dimensions": 3072, "vectorSearchProfile": "warm-profile" },
    { "name": "created_at", "type": "Edm.DateTimeOffset", "filterable": true, "sortable": true }
  ],
  "vectorSearch": {
    "profiles": [
      { "name": "hot-profile", "algorithm": "hot-hnsw" },
      { "name": "warm-profile", "algorithm": "warm-hnsw" }
    ],
    "algorithms": [
      { "name": "hot-hnsw", "kind": "hnsw", "hnswParameters": { "metric": "cosine", "m": 4, "efConstruction": 400 } },
      { "name": "warm-hnsw", "kind": "hnsw", "hnswParameters": { "metric": "cosine", "m": 8, "efConstruction": 800 } }
    ]
  }
}
```

### Hybrid search query

```json
{
  "search": "deployment guide",
  "vectors": [
    {
      "value": [0.123, -0.456, ...],
      "kNearestNeighborsCount": 10,
      "fields": "embedding_warm"
    }
  ],
  "filter": "tenant_id eq 'devel' and visibility eq 'public'",
  "select": "id,title,content,domain",
  "top": 10,
  "queryType": "semantic",
  "semanticConfiguration": "default"
}
```

Azure AI Search handles RRF fusion internally when you combine `search` (text) + `vectors` (vector) in the same query.

---

## Cost Estimate (Phase 1 demo)

| Service | Tier | Monthly Cost |
|---------|------|-------------|
| Azure AI Search | Basic (1 replica, 1 partition) | ~$75/month |
| Azure OpenAI (embeddings) | ~1M tokens/month | ~$0.13/month |
| Azure OpenAI (chat) | ~100K tokens/month (demo) | ~$1/month |
| Cosmos DB | Serverless | ~$5-10/month |
| Azure Blob Storage | Hot tier, <1GB | ~$0.02/month |
| Azure PostgreSQL | Flexible, Burstable B1ms | ~$12/month |
| Azure Container Apps | Consumption plan | ~$0-5/month (scale to zero) |
| **Total** | | **~$95-105/month** |

For production with real traffic, budget $300-800/month depending on search tier and query volume.

---

## Environment Variables

```env
# Provider selection
SEARCH_PROVIDER=azure-ai-search
EMBEDDING_PROVIDER=azure-openai
STORAGE_PROVIDER=cosmos-db
BLOB_PROVIDER=azure-blob
QUEUE_PROVIDER=azure-service-bus
AUTH_PROVIDER=azure-ad

# Azure AI Search
AZURE_SEARCH_ENDPOINT=https://<service>.search.windows.net
AZURE_SEARCH_API_KEY=<admin-key>
AZURE_SEARCH_INDEX=documents

# Azure OpenAI
AZURE_OPENAI_ENDPOINT=https://<service>.openai.azure.com
AZURE_OPENAI_API_KEY=<key>
AZURE_OPENAI_EMBEDDING_DEPLOYMENT=text-embedding-3-large
AZURE_OPENAI_CHAT_DEPLOYMENT=gpt-4o-mini

# Cosmos DB
COSMOS_DB_ENDPOINT=https://<account>.documents.azure.com:443/
COSMOS_DB_KEY=<key>
COSMOS_DB_DATABASE=knowledge-lake

# Azure Blob
AZURE_STORAGE_CONNECTION_STRING=<connection-string>
AZURE_BLOB_CONTAINER=documents

# PostgreSQL
DATABASE_URL=postgresql://<user>:<pass>@<server>.postgres.database.azure.com:5432/knowledge_lake
```

---

## Status

Specified 2026-04-01. Azure provider implementation.
