---
uuid: "knoxx-knowledge-ops-deploy-aws"
title: "The Lake — AWS Deployment Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.383Z"
source: "specs/epics/knowledge-ops-deploy-aws.md"
points: null
category: epics
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
