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
  UTF-8 charset, provider id/version, immutable provider-config version, representation
  contract version, input artifact/version, deterministic digest, and
  static-versus-hydratable mode).
- Adapt existing Helix/React server rendering or the existing portable markup path behind
  the representation provider boundary; choose the runtime implementation based on the
  actual input contract rather than making React the architecture.
- Prove that identical input can come from the file resource repository, a fake remote CMS
  provider, or a generated fixture without representation code branching on source.
- Keep hydration/browser concerns optional and explicit.
- Contextually escape all untrusted text, attribute, URL, and serialized hydration values.
  Raw/trusted markup requires an unforgeable opaque capability minted by a trusted
  sanitizer/compiler or explicitly authorized policy boundary **outside** caller-controlled
  artifact data. It binds the exact artifact digest, representation mode, provider/config
  identity, and server-admitted render operation. The public `trusted-html` constructor and
  plain `TrustedHtml` values confer no capability. The provider verifies/consumes the
  capability before `raw-html`/`dangerouslySetInnerHTML`, rejects self-asserted markers,
  cross-operation replay, mismatches, and unsafe URL schemes, and cannot copy
  repository/provider credentials or provenance-only secrets into output.
- Produce byte-identical HTML and digest for the same canonical input plus provider/config
  version. A render failure returns no successful representation and no partial output for a
  publication consumer to mistake as authority.

The immutable provider configuration identity covers every output-affecting option, sanitizer
policy/version, template/renderer asset revision, and feature flag. Deterministic output and
its digest bind `{provider-id, provider-version, config-version,
representation-contract-version, mode, canonical-input}`; changing any member changes the
representation identity. Trusted-markup capability verification uses that same identity rather
than a looser provider label.

## Non-goals

- Reopening `uxx-shared-markup-html-helix-renderers`.
- Creating another general markup DSL.
- Requiring CMS APIs in the representation provider.
- Treating SSR as publication truth or persistence.

## Done when

- One explicit semantic/view input produces HTML through a provider contract.
- Contract tests cover text/attribute/script-closing/URL injection, self-asserted trusted
  markers, refusal of plain `TrustedHtml`, raw-markup refusal without an external
  artifact-bound capability, forgery/config/digest/mode mismatch, cross-operation replay,
  deterministic repeated rendering, config-version rotation, and failure with no successful
  output.
- Tests prove the representation path has no repository-provider dependency.
- The provider can be selected as one workflow operation whose output may be stored,
  published, returned over HTTP, or ignored by the caller.
