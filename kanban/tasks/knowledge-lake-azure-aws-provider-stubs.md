---
uuid: "knoxx-knowledge-lake-azure-aws-provider-stubs"
title: "knowledge-lake: Azure and AWS provider stubs"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# knowledge-lake: Azure and AWS provider stubs

> Parent epic: `knoxx-knowledge-ops-provider-abstraction`
> Points: 2

## Purpose

Add typed stub implementations for all twelve Azure and AWS provider files so the package directory structure matches the spec and future implementors have a clear, compilable starting point with correct interface signatures and `NotImplementedError` throws.

## Scope

Azure stubs (`src/providers/azure/`): `search.ts`, `embedding.ts`, `storage.ts`, `blob.ts`, `queue.ts`, `auth.ts`
AWS stubs (`src/providers/aws/`): `search.ts`, `embedding.ts`, `storage.ts`, `blob.ts`, `queue.ts`, `auth.ts`

Each stub file must:
- Declare a class that `implements` the corresponding provider interface from `src/core/interfaces.ts`
- Have the correct method signatures matching the interface exactly
- Throw `new Error("Not implemented: <ProviderName>.<methodName> — set <RELEVANT_ENV_VAR> and install the cloud SDK")` in every method body
- Include a JSDoc comment on the class indicating which cloud service it maps to (e.g., `/** Azure AI Search */`, `/** AWS OpenSearch / Kendra */`)
- Export the class as the default export

The provider factory in `src/index.ts` must be updated to import and register these stubs for the `"azure"` and `"aws"` config values so the switch statement is exhaustive.

## Definition of done

- All 12 stub files exist at the paths specified in the epic's file structure diagram
- `tsc --noEmit` passes across the entire package with all stubs present
- The factory in `src/index.ts` handles `"azure"` and `"aws"` provider config values without a TypeScript type error or runtime crash at the switch level (stubs instantiate fine; individual method calls throw)
- Each stub class has at least one JSDoc line identifying the backing cloud service

## Notes

Split from parent epic `knoxx-knowledge-ops-provider-abstraction` on 2026-05-30.
