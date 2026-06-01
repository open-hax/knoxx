---
uuid: "knoxx-knowledge-ops-deploy-aws"
title: "The Lake — AWS Deployment Spec"
status: "icebox"
priority: "P3"
labels: ["epics"]
created_at: "2026-05-28T22:40:14.383Z"
source: "specs/epics/knowledge-ops-deploy-aws.md"
points: null
category: "epics"
---

# The Lake — AWS Deployment Spec

> Source: `specs/epics/knowledge-ops-deploy-aws.md`

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

Triage 2026-05-29: This is an unpointed epic (epics label, epics directory) describing the full AWS-native deployment stack (OpenSearch, Bedrock, DynamoDB, S3, SQS, Cognito, Fargate) — a pure architecture/spec with no bounded subtasks split out and no urgency signal distinguishing it from the parallel Azure and self-hosted tracks. Verdict: icebox (P3) — needs breakdown into ≤5sp subtasks before it can be actioned; revisit when AWS becomes the prioritised deployment target for a sprint. --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
