# The Lake — AWS Deployment Spec

> *Bedrock + OpenSearch + DynamoDB. The AWS-native path.*

---

## Provider Mapping

| Logical Component | AWS Service | Config Key |
|-------------------|------------|------------|
| Search (vector + FTS + hybrid) | **Amazon OpenSearch Serverless** | `SEARCH_PROVIDER=aws-opensearch` |
| Embeddings | **Amazon Bedrock** (Titan Embeddings) or **SageMaker** | `EMBEDDING_PROVIDER=aws-bedrock` |
| Structured storage | **Amazon DynamoDB** | `STORAGE_PROVIDER=dynamodb` |
| Blob storage | **Amazon S3** | `BLOB_PROVIDER=aws-s3` |
| Job queue | **Amazon SQS** | `QUEUE_PROVIDER=aws-sqs` |
| Auth | **Amazon Cognito** | `AUTH_PROVIDER=aws-cognito` |
| App hosting | **AWS Fargate** or **ECS** | — |
| Database (labels) | **Amazon RDS PostgreSQL** | — |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  AWS Fargate / ECS                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ CMS API  │  │ Label API│  │ Widget   │               │
│  │          │  │          │  │ Proxy    │               │
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
 OpenSearch   Bedrock    DynamoDB    S3
 Serverless  (Titan)    (docs)     (blobs)
 (vectors    (embeddings
  + FTS)     + chat)
      │
      ▼
 RDS PostgreSQL
 (labels, tenants)
```

---

## Embedding Tier Configuration

| Tier | Bedrock Model | Dimensions | Cost |
|------|--------------|------------|------|
| Hot | Amazon Titan Embed Text v2 (256d) | 256 | $0.02/1M tokens |
| Warm | Amazon Titan Embed Text v2 (1024d) | 1024 | $0.02/1M tokens |
| Compact | Amazon Titan Embed Text v2 (1024d) | 1024 | $0.02/1M tokens |

Or use Qwen on Bedrock if available in the region. Same pricing model — API-based, no GPU.

---

## OpenSearch Serverless Configuration

Amazon OpenSearch Serverless gives you vector search and full-text search without managing clusters.

### Collection configuration

```json
{
  "name": "knowledge-lake-vectors",
  "type": "VECTORSEARCH",
  "standbyReplicas": "DISABLED"
}
```

### Index mapping

```json
{
  "settings": {
    "index.knn": true
  },
  "mappings": {
    "properties": {
      "tenant_id": { "type": "keyword" },
      "visibility": { "type": "keyword" },
      "domain": { "type": "keyword" },
      "title": { "type": "text" },
      "content": { "type": "text" },
      "embedding_warm": {
        "type": "knn_vector",
        "dimension": 1024,
        "method": {
          "name": "hnsw",
          "engine": "faiss",
          "parameters": { "ef_construction": 256, "m": 16 }
        }
      },
      "created_at": { "type": "date" }
    }
  }
}
```

---

## Cost Estimate (Phase 1 demo)

| Service | Tier | Monthly Cost |
|---------|------|-------------|
| OpenSearch Serverless | 2 OCUs minimum | ~$144/month |
| Bedrock Titan Embeddings | ~1M tokens/month | ~$0.02/month |
| Bedrock Claude (chat) | ~100K tokens/month | ~$1-3/month |
| DynamoDB | On-demand, low traffic | ~$1-5/month |
| S3 | <1GB | ~$0.02/month |
| RDS PostgreSQL | db.t4g.micro | ~$12/month |
| Fargate | 0.25 vCPU, 0.5GB | ~$5-10/month |
| **Total** | | **~$165-175/month** |

**Note**: OpenSearch Serverless has a 2 OCU minimum (~$144/month) which makes it expensive for demos. For cost-sensitive deployments, consider OpenSearch managed (provisioned) at ~$30/month on a `t3.small.search`.

---

## Environment Variables

```env
# Provider selection
SEARCH_PROVIDER=aws-opensearch
EMBEDDING_PROVIDER=aws-bedrock
STORAGE_PROVIDER=dynamodb
BLOB_PROVIDER=aws-s3
QUEUE_PROVIDER=aws-sqs
AUTH_PROVIDER=aws-cognito

# OpenSearch
AWS_OPENSEARCH_ENDPOINT=https://<collection>.us-east-1.aoss.amazonaws.com
AWS_OPENSEARCH_REGION=us-east-1

# Bedrock
AWS_BEDROCK_REGION=us-east-1
AWS_BEDROCK_EMBEDDING_MODEL=amazon.titan-embed-text-v2:0

# DynamoDB
AWS_DYNAMODB_TABLE=knowledge-lake-documents
AWS_REGION=us-east-1

# S3
AWS_S3_BUCKET=knowledge-lake-blobs

# PostgreSQL
DATABASE_URL=postgresql://<user>:<pass>@<endpoint>.us-east-1.rds.amazonaws.com:5432/knowledge_lake
```

---

## Status

Specified 2026-04-01. AWS provider implementation.
