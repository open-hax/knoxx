---
uuid: "knoxx-react-ssr-representation-provider"
title: "Expose React SSR/static HTML as a representation provider over explicit inputs"
status: incoming
priority: P3
labels: ["tasks", "3sp", "has-parent", "representation", "ssr", "helix", "contracts"]
created_at: "2026-08-13T00:00:00Z"
points: 3
category: tasks
---
# Expose React SSR/static HTML as a representation provider over explicit inputs

> Parent epic: `knoxx-representation-output-boundary`

## Purpose

Keep server/static rendering independent from CMS/resource authority. Reuse the shared
markup/Helix foundation that already landed, but place an explicit representation
operation boundary above it so callers provide a semantic/view artifact and receive an
HTML representation without the provider knowing where the input came from.

## Scope

- Define the concrete input/view contract this provider accepts.
- Define HTML output metadata/contract needed by callers (body/document, content type,
  UTF-8 charset, representation/provider version, input artifact/version, deterministic
  digest, and static-versus-hydratable mode).
- Adapt existing Helix/React server rendering or the existing portable markup path behind
  the representation provider boundary; choose the runtime implementation based on the
  actual input contract rather than making React the architecture.
- Prove that identical input can come from the file resource repository, a fake remote CMS
  provider, or a generated fixture without representation code branching on source.
- Keep hydration/browser concerns optional and explicit.
- Contextually escape all untrusted text, attribute, URL, and serialized hydration values.
  Raw/trusted markup is a separate validated capability in the input contract, never a string
  convention; the provider rejects unsafe URL schemes and cannot copy repository/provider
  credentials or provenance-only secrets into output.
- Produce byte-identical HTML and digest for the same canonical input plus provider/config
  version. A render failure returns no successful representation and no partial output for a
  publication consumer to mistake as authority.

## Non-goals

- Reopening `uxx-shared-markup-html-helix-renderers`.
- Creating another general markup DSL.
- Requiring CMS APIs in the representation provider.
- Treating SSR as publication truth or persistence.

## Done when

- One explicit semantic/view input produces HTML through a provider contract.
- Contract tests cover text/attribute/script-closing/URL injection, raw-markup refusal,
  deterministic repeated rendering, and failure with no successful output.
- Tests prove the representation path has no repository-provider dependency.
- The provider can be selected as one workflow operation whose output may be stored,
  published, returned over HTTP, or ignored by the caller.
