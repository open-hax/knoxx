# The Lake — Provider Abstraction Spec

> *The product is the integration logic. The provider is a configuration detail.*

---

## Purpose

Define the abstract interfaces that decouple the knowledge platform from any specific cloud provider or infrastructure. Every logical component maps to an interface. Every provider implements those interfaces. The product is the interfaces + the domain logic on top — not the plumbing underneath.

---

## Core Abstraction Layer

```
┌─────────────────────────────────────────────────────────┐
│                 APPLICATION LAYER                        │
│  CMS · Labels · Widget · Review · Training Export       │
├─────────────────────────────────────────────────────────┤
│                 DOMAIN LOGIC LAYER                       │
│  Tenant isolation · Document lifecycle · Visibility      │
│  Compaction · HITL labeling · PII detection              │
├─────────────────────────────────────────────────────────┤
│                 PROVIDER ABSTRACTION LAYER               │
│  SearchProvider · EmbeddingProvider · StorageProvider    │
│  DocumentProvider · QueueProvider · AuthProvider         │
├─────────────────────────────────────────────────────────┤
│                 PROVIDER IMPLEMENTATIONS                 │
│  Azure · AWS · Self-Hosted · Local/Dev                   │
└─────────────────────────────────────────────────────────┘
```

---

## Provider Interfaces

### SearchProvider

Full-text + vector + hybrid search with tenant filtering.

```typescript
interface SearchProvider {
  name: string

  // Lifecycle
  initialize(): Promise<void>
  createIndex(index: IndexDefinition): Promise<void>
  deleteIndex(indexName: string): Promise<void>

  // Indexing
  indexDocuments(indexName: string, docs: SearchDocument[]): Promise<void>
  deleteDocuments(indexName: string, filter: Record<string, unknown>): Promise<void>

  // Search
  search(options: SearchOptions): Promise<SearchResult[]>
  vectorSearch(options: VectorSearchOptions): Promise<SearchResult[]>
  hybridSearch(options: HybridSearchOptions): Promise<SearchResult[]>  // FTS + vector + RRF

  // Health
  health(): Promise<{ status: string; latency_ms: number }>
}

interface SearchOptions {
  indexName: string
  query: string
  filter?: Record<string, unknown>    // tenant_id, visibility, domain
  limit?: number
  skip?: number
}

interface VectorSearchOptions {
  indexName: string
  embedding: number[]
  filter?: Record<string, unknown>    // tenant_id, visibility
  limit?: number
  numCandidates?: number
}

interface HybridSearchOptions {
  indexName: string
  query: string
  embedding: number[]
  filter?: Record<string, unknown>
  limit?: number
  rrfK?: number                       // RRF constant, default 60
}

interface SearchResult {
  id: string
  score: number
  document: Record<string, unknown>
  highlights?: Record<string, string[]>
}
```

### EmbeddingProvider

Text-to-vector conversion with model selection.

```typescript
interface EmbeddingProvider {
  name: string

  embed(text: string, options?: EmbedOptions): Promise<number[]>
  embedBatch(texts: string[], options?: EmbedOptions): Promise<number[][]>

  // Model info
  dimensions(model?: string): number
  availableModels(): string[]

  health(): Promise<{ status: string }>
}

interface EmbedOptions {
  model?: string       // override default model
  tier?: "hot" | "warm" | "compact"  // resolve model from tier
}
```

### StorageProvider

Structured document storage with CRUD and query.

```typescript
interface StorageProvider {
  name: string

  // Lifecycle
  initialize(): Promise<void>

  // Documents
  insert(collection: string, doc: Record<string, unknown>): Promise<string>  // returns ID
  get(collection: string, id: string): Promise<Record<string, unknown> | null>
  update(collection: string, id: string, doc: Partial<Record<string, unknown>>): Promise<void>
  delete(collection: string, id: string): Promise<void>
  query(collection: string, filter: Record<string, unknown>, options?: QueryOptions): Promise<Record<string, unknown>[]>

  // Health
  health(): Promise<{ status: string }>
}

interface QueryOptions {
  sort?: Record<string, 1 | -1>
  limit?: number
  skip?: number
  projection?: string[]
}
```

### BlobProvider

Content-addressed binary storage.

```typescript
interface BlobProvider {
  name: string

  put(data: Buffer | ReadableStream, contentType: string): Promise<string>  // returns SHA-256 key
  get(key: string): Promise<Buffer | null>
  delete(key: string): Promise<void>
  exists(key: string): Promise<boolean>

  health(): Promise<{ status: string }>
}
```

### QueueProvider

Async job processing.

```typescript
interface QueueProvider {
  name: string

  enqueue(queue: string, job: JobPayload): Promise<string>  // returns job ID
  dequeue(queue: string): Promise<JobPayload | null>
  complete(jobId: string, result?: unknown): Promise<void>
  fail(jobId: string, error: string): Promise<void>
  status(jobId: string): Promise<JobStatus>

  health(): Promise<{ status: string }>
}

interface JobPayload {
  id: string
  type: string
  data: Record<string, unknown>
  createdAt: string
}
```

### AuthProvider

Tenant resolution and access control.

```typescript
interface AuthProvider {
  name: string

  resolveTenant(request: Request): Promise<TenantContext | null>
  validateAccess(context: TenantContext, resource: string, action: string): Promise<boolean>
  createApiKey(tenantId: string, scopes: string[]): Promise<string>
  revokeApiKey(key: string): Promise<void>

  health(): Promise<{ status: string }>
}

interface TenantContext {
  tenantId: string
  userId?: string
  roles: string[]
  scopes: string[]
}
```

---

## Provider Resolution

Providers are selected by environment configuration, not code changes:

```typescript
// config/providers.ts
const config = {
  search: process.env.SEARCH_PROVIDER || "self-hosted",      // azure-ai-search | aws-opensearch | mongodb | chromadb
  embedding: process.env.EMBEDDING_PROVIDER || "self-hosted", // azure-openai | aws-bedrock | ollama
  storage: process.env.STORAGE_PROVIDER || "self-hosted",     // cosmos-db | dynamodb | mongodb | duckdb
  blob: process.env.BLOB_PROVIDER || "self-hosted",           // azure-blob | aws-s3 | filesystem
  queue: process.env.QUEUE_PROVIDER || "self-hosted",         // azure-service-bus | aws-sqs | jsonl
  auth: process.env.AUTH_PROVIDER || "self-hosted",           // azure-ad | aws-cognito | bearer-token
}
```

---

## File Structure

```
packages/knowledge-lake/
├── src/
│   ├── core/
│   │   ├── interfaces.ts          # All provider interfaces
│   │   ├── types.ts               # Shared types
│   │   └── config.ts              # Provider resolution from env
│   ├── providers/
│   │   ├── azure/
│   │   │   ├── search.ts          # Azure AI Search implementation
│   │   │   ├── embedding.ts       # Azure OpenAI embeddings
│   │   │   ├── storage.ts         # Cosmos DB
│   │   │   ├── blob.ts            # Azure Blob Storage
│   │   │   ├── queue.ts           # Azure Service Bus
│   │   │   └── auth.ts            # Azure AD / Entra ID
│   │   ├── aws/
│   │   │   ├── search.ts          # OpenSearch / Kendra
│   │   │   ├── embedding.ts       # Bedrock embeddings / SageMaker
│   │   │   ├── storage.ts         # DynamoDB
│   │   │   ├── blob.ts            # S3
│   │   │   ├── queue.ts           # SQS
│   │   │   └── auth.ts            # Cognito
│   │   ├── self-hosted/
│   │   │   ├── search.ts          # MongoDB $vectorSearch + mongot
│   │   │   ├── embedding.ts       # Ollama (qwen3-embedding)
│   │   │   ├── storage.ts         # MongoDB
│   │   │   ├── blob.ts            # Filesystem (SHA-256 sharded)
│   │   │   ├── queue.ts           # JSONL job queue
│   │   │   └── auth.ts            # Bearer token
│   │   └── local/
│   │       ├── search.ts          # ChromaDB or in-memory
│   │       ├── embedding.ts       # Ollama local (nomic/qwen3-0.6b)
│   │       ├── storage.ts         # DuckDB or SQLite
│   │       ├── blob.ts            # Local filesystem
│   │       ├── queue.ts           # In-process queue
│   │       └── auth.ts            # No-auth (dev mode)
│   ├── domain/                    # Provider-independent business logic
│   │   ├── documents.ts           # Document lifecycle (draft/review/public/archive)
│   │   ├── tenants.ts             # Tenant isolation
│   │   ├── compaction.ts          # Semantic compaction pipeline
│   │   ├── labeling.ts            # HITL labeling + export
│   │   └── pii.ts                 # PII detection + handling
│   └── index.ts                   # Provider factory + domain wiring
├── package.json
└── tsconfig.json
```

---

## What Each Provider Implementor Gets

When someone implements a new provider (e.g., GCP), they fill in 6 files:
- `search.ts` — maps to Vertex AI Search or AlloyDB
- `embedding.ts` — maps to Vertex AI Embeddings or Gemini
- `storage.ts` — maps to Firestore or AlloyDB
- `blob.ts` — maps to Cloud Storage
- `queue.ts` — maps to Cloud Tasks or Pub/Sub
- `auth.ts` — maps to Firebase Auth or Cloud IAM

The domain logic never changes. The provider is a configuration detail.

---

## Status

Specified 2026-04-01. Multi-provider architecture.
