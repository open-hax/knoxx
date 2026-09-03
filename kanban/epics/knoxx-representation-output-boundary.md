---
uuid: "knoxx-representation-output-boundary"
title: "Representation outputs — semantic artifacts to concrete forms"
status: incoming
priority: P3
labels: ["epics", "representation", "rendering", "ssr", "contracts", "providers"]
created_at: "2026-08-13T00:00:00Z"
points: 0
category: epics
---
# Representation outputs — semantic artifacts to concrete forms

## Signal

Server-side rendering is not a CMS capability and should not be named as if HTML or a
visual UI were the only possible output. The stable concern is **representation**:
expressing a semantic artifact in some concrete representation through a replaceable
provider.

```text
semantic artifact
      -> representation operation/provider
      -> HTML / React tree / Markdown / PDF / feed / plain text / ...
```

The operation name may remain `representation`; concrete implementations can use
ordinary local names such as `react-ssr`, `html`, or `markdown` without encoding the
entire taxonomy into class names.

## Existing foundation

`uxx-shared-markup-html-helix-renderers` already landed the useful lower-level primitive:
a portable markup AST, a React-free HTML renderer, and a Helix adapter. That work remains
valid. This epic is about the **provider/operation boundary above those implementations**,
not replacing the AST or inventing another markup DSL.

## Ownership rule

A representation provider owns conversion from a resolved semantic/view artifact to a
concrete representation. It does not own:

- resource persistence/CMS authority;
- publication intent/reconciliation;
- transduction of content semantics;
- evaluation/review judgments.

Every provider contract names its input and provider/config versions, output media metadata,
and deterministic digest. Concrete renderers own contextual escaping and explicit trusted
markup capabilities minted outside caller-controlled artifacts and bound to an exact artifact
digest, representation mode, provider/config identity, and admitted render operation. The
public `trusted-html` constructor and its `TrustedHtml` value are data wrappers, not proof of
that authority. A representation boundary validates and consumes the unforgeable capability
before raw rendering; it may not honor plain/self-asserted trust, replay authority across
operations, turn untrusted content into executable HTML, or leak repository/provider
credentials into output.

## Candidate children

- `knoxx-react-ssr-representation-provider` (P3/deferred) — expose the existing HTML/Helix
  foundation as a representation provider over explicit view/artifact inputs and prove a
  repository-agnostic server/static HTML path.
- Future: define the representation operation contract (`requires` / `provides`) once the upstream typed workflow/action vocabulary is available.

## Non-goals

- Reopening the completed shared-markup AST work.
- Making React a backend dependency.
- Building a visual CMS.
- Treating `projection` as the public name for this subsystem; that vocabulary is already overloaded by graph/event projections elsewhere.

## Done when

- A representation operation consumes an explicit semantic/view contract and produces an explicit output contract.
- HTML/SSR/static rendering can be selected as providers without CMS-specific dependencies.
- The same semantic input may be represented by another provider without changing its resource or publication identity.
