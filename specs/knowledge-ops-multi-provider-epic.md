# The Lake — Multi-Provider Epic

> *One platform. Four deployment targets. The provider is a configuration detail.*

---

## Purpose

Define the epic that delivers the multi-provider knowledge platform: abstract interfaces + domain logic + four provider implementations. The product is the integration layer and domain logic. The provider is selected at deploy time, not at build time.

---

## Why Multi-Provider

The platform is open source. You don't control where it gets deployed. The client chooses the cloud. Your job is to make it work on all of them.

| Client type | Provider | Why |
|-------------|----------|-----|
| Enterprise on Microsoft stack (FutureSight) | **Azure** | Already on Azure. Azure AI Search + OpenAI + Cosmos DB. |
| AWS-native startups | **AWS** | Bedrock + OpenSearch + DynamoDB. |
| Privacy-first / sovereignty | **Self-hosted** | MongoDB + mongot + Ollama. No cloud dependency. |
| Developers / demos / OSS contributors | **Local/Dev** | ChromaDB + DuckDB + Ollama. $0, runs anywhere. |

---

## Epic Structure

### Epic 0: Provider Abstraction Layer

Define the interfaces. This is the most important epic — everything else depends on it.

| Story | Points | Files |
|-------|--------|-------|
| 0.1 Define SearchProvider interface | 3 | `packages/knowledge-lake/src/core/interfaces.ts` |
| 0.2 Define EmbeddingProvider interface | 2 | same file |
| 0.3 Define StorageProvider interface | 2 | same file |
| 0.4 Define BlobProvider interface | 1 | same file |
| 0.5 Define QueueProvider interface | 1 | same file |
| 0.6 Define AuthProvider interface | 2 | same file |
| 0.7 Provider factory from env config | 3 | `packages/knowledge-lake/src/core/config.ts` |
| 0.8 Domain logic layer (provider-independent) | 8 | `packages/knowledge-lake/src/domain/` |

**Total: 22 points**

Spec: `knowledge-ops-provider-abstraction.md`

---

### Epic 1: Local/Dev Provider (ship first)

The demo path. $0 cost. No GPU required. Works on any developer machine.

| Story | Points | Files |
|-------|--------|-------|
| 1.1 ChromaDB SearchProvider implementation | 5 | `providers/local/search.ts` |
| 1.2 Ollama EmbeddingProvider (nomic/qwen3-0.6b) | 3 | `providers/local/embedding.ts` |
| 1.3 DuckDB StorageProvider | 5 | `providers/local/storage.ts` |
| 1.4 Filesystem BlobProvider | 2 | `providers/local/blob.ts` |
| 1.5 In-memory QueueProvider | 2 | `providers/local/queue.ts` |
| 1.6 No-auth AuthProvider (dev mode) | 1 | `providers/local/auth.ts` |
| 1.7 Docker Compose for local dev | 2 | `docker-compose.dev.yml` |
| 1.8 Seed script for demo data | 3 | `scripts/seed-demo.ts` |

**Total: 23 points**

Spec: `knowledge-ops-deploy-local.md`

---

### Epic 2: Self-Hosted Provider (MongoDB path)

The sovereignty path. No cloud dependency. MongoDB 8.2 + mongot + Ollama on your own hardware.

| Story | Points | Files |
|-------|--------|-------|
| 2.1 MongoDB $vectorSearch + $search SearchProvider | 8 | `providers/self-hosted/search.ts` |
| 2.2 Ollama EmbeddingProvider (full tier support) | 5 | `providers/self-hosted/embedding.ts` |
| 2.3 MongoDB StorageProvider | 3 | `providers/self-hosted/storage.ts` |
| 2.4 Filesystem BlobProvider (SHA-256 sharded) | 2 | `providers/self-hosted/blob.ts` |
| 2.5 JSONL QueueProvider | 2 | `providers/self-hosted/queue.ts` |
| 2.6 Bearer token AuthProvider | 2 | `providers/self-hosted/auth.ts` |
| 2.7 Docker Compose (mongodb + mongot + ollama + postgres) | 3 | `docker-compose.yml` |
| 2.8 Search index creation and management | 3 | `providers/self-hosted/indexes.ts` |

**Total: 28 points**

Spec: `knowledge-ops-deploy-self-hosted.md`
Related: `knowledge-ops-mongodb-vector-unification.md`, `knowledge-ops-the-lake.md`

---

### Epic 3: Azure Provider

The managed cloud path for Microsoft-stack clients.

| Story | Points | Files |
|-------|--------|-------|
| 3.1 Azure AI Search SearchProvider | 8 | `providers/azure/search.ts` |
| 3.2 Azure OpenAI EmbeddingProvider | 3 | `providers/azure/embedding.ts` |
| 3.3 Cosmos DB StorageProvider | 5 | `providers/azure/storage.ts` |
| 3.4 Azure Blob Storage BlobProvider | 2 | `providers/azure/blob.ts` |
| 3.5 Azure Service Bus QueueProvider | 3 | `providers/azure/queue.ts` |
| 3.6 Entra ID (Azure AD) AuthProvider | 5 | `providers/azure/auth.ts` |
| 3.7 Azure Container Apps deployment config | 3 | `deploy/azure/` |
| 3.8 Azure AI Search index schema + hybrid query | 3 | `providers/azure/search-indexes.ts` |

**Total: 32 points**

Spec: `knowledge-ops-deploy-azure.md`

---

### Epic 4: AWS Provider

The managed cloud path for AWS-native clients.

| Story | Points | Files |
|-------|--------|-------|
| 4.1 OpenSearch Serverless SearchProvider | 8 | `providers/aws/search.ts` |
| 4.2 Bedrock Titan EmbeddingProvider | 3 | `providers/aws/embedding.ts` |
| 4.3 DynamoDB StorageProvider | 5 | `providers/aws/storage.ts` |
| 4.4 S3 BlobProvider | 2 | `providers/aws/blob.ts` |
| 4.5 SQS QueueProvider | 2 | `providers/aws/queue.ts` |
| 4.6 Cognito AuthProvider | 5 | `providers/aws/auth.ts` |
| 4.7 Fargate/ECS deployment config | 3 | `deploy/aws/` |
| 4.8 OpenSearch index mapping + hybrid query | 3 | `providers/aws/search-indexes.ts` |

**Total: 31 points**

Spec: `knowledge-ops-deploy-aws.md`

---

### Epic 5: Provider Integration Tests

Verify the same domain logic works across all providers.

| Story | Points | Files |
|-------|--------|-------|
| 5.1 Conformance test suite (search, embed, store, blob) | 8 | `tests/conformance/` |
| 5.2 Local provider integration tests | 3 | `tests/providers/local.test.ts` |
| 5.3 Self-hosted provider integration tests (Docker) | 5 | `tests/providers/self-hosted.test.ts` |
| 5.4 Azure provider integration tests (test subscription) | 5 | `tests/providers/azure.test.ts` |
| 5.5 AWS provider integration tests (test account) | 5 | `tests/providers/aws.test.ts` |

**Total: 26 points**

---

## Build Order

```
Epic 0 (Abstraction) ──────► Epic 1 (Local/Dev) ──► Ship demo
                                    │
                          ┌─────────┼─────────┐
                          ▼         ▼         ▼
                    Epic 2     Epic 3     Epic 4
                  (Self-host)  (Azure)    (AWS)
                          │         │         │
                          └─────────┼─────────┘
                                    ▼
                            Epic 5 (Tests)
```

1. **Epic 0 first** — interfaces define the contract. Everything else implements it.
2. **Epic 1 second** — local/dev is the fastest to build and test. Ship the demo here.
3. **Epic 2, 3, 4 in parallel** — once interfaces are stable, provider implementations can be built independently.
4. **Epic 5 last** — conformance tests verify cross-provider consistency.

---

## Total Effort

| Epic | Points | Relative Size |
|------|--------|---------------|
| 0. Abstraction | 22 | ~2 weeks |
| 1. Local/Dev | 23 | ~2 weeks |
| 2. Self-Hosted | 28 | ~2.5 weeks |
| 3. Azure | 32 | ~3 weeks |
| 4. AWS | 31 | ~3 weeks |
| 5. Tests | 26 | ~2 weeks |
| **Total** | **162** | **~14-16 weeks (sequential)** |

With parallelization (Epics 2/3/4 built simultaneously after Epic 1):
**~8-10 weeks with 2-3 people, or ~12-14 weeks solo.**

---

## What's Already Done

| Piece | Status | Where |
|-------|--------|-------|
| Domain logic (document lifecycle, tenant isolation, labeling, export) | Designed + specced | `orgs/open-hax/knoxx/specs/knowledge-ops-*.md` |
| Label schemas (Zod + Pydantic) | Implemented | `packages/futuresight-kms/src/schema/`, `packages/futuresight-kms/python/km_labels/models.py` |
| Label CRUD API | Implemented | `packages/futuresight-kms/python/km_labels/routers/labels.py` |
| SFT/RLHF export | Implemented | `packages/futuresight-kms/python/km_labels/routers/export.py` |
| KnowledgeLabeler UI | Partially implemented | `packages/futuresight-kms/frontend/components/KnowledgeLabeler.tsx` |
| Bridge clients (Ragussy, Shibboleth, KmLabels) | Implemented | `packages/futuresight-kms/src/bridge/index.ts` |
| OpenPlanner data lake (self-hosted) | Working | `services/openplanner/` |
| Shibboleth labeling DSL + UI | Working | `orgs/octave-commons/shibboleth/` |
| Chat UI patterns | Documented | `orgs/open-hax/knoxx/specs/knowledge-ops-chat-ui-library.md` |
| Provider abstraction interfaces | **Not yet implemented** | Epic 0 |
| Azure/AWS provider adapters | **Not yet implemented** | Epics 3/4 |

---

## The Open Source Pitch

"The knowledge platform runs on your cloud. Azure, AWS, your own servers, or a developer laptop. Same code, different provider config. No vendor lock-in. No proprietary runtime. The only thing we sell is the expertise to deploy and maintain it."

---

## Related Specs

| Spec | What |
|------|------|
| `knowledge-ops-provider-abstraction.md` | Core interfaces — Epic 0 |
| `knowledge-ops-deploy-local.md` | Local/dev — Epic 1 |
| `knowledge-ops-deploy-self-hosted.md` | Self-hosted — Epic 2 |
| `knowledge-ops-deploy-azure.md` | Azure — Epic 3 |
| `knowledge-ops-deploy-aws.md` | AWS — Epic 4 |
| `knowledge-ops-the-lake.md` | OpenPlanner architecture |
| `knowledge-ops-mongodb-vector-unification.md` | MongoDB vector search |
| `knowledge-ops-cms-data-model.md` | CMS layer |
| `knowledge-ops-multi-tenant-control-plane.md` | Tenant isolation |
| `knowledge-ops-pii-handling-protocol.md` | PII protocol |
| `knowledge-ops-shibboleth-lite-labeling.md` | HITL labeling |
| `knowledge-ops-chat-ui-library.md` | Shared UI |
| `knowledge-ops-integration.md` | Service contracts |
| `knowledge-ops-chat-widget-layers.md` | Five-layer stack |
| `knowledge-ops-gap-analysis-prior-art.md` | Research |

---

## Status

Specified 2026-04-01. Multi-provider epic.
