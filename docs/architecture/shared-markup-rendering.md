# Shared markup rendering

Status: accepted
Date: 2026-08-05

## Decision

Knoxx uses one portable, Hiccup-shaped markup AST for server-owned documents
and browser-compatible static structure:

```clojure
[:tag attrs child ...]
[:<> child ...]
[:raw-html trusted-value]
```

The data contract lives under the `open-hax.uxx.*` namespace in
`shared/src/cljs`. Both backend and frontend shadow builds consume that source
root.

Two runtimes interpret the same tree:

- `open-hax.uxx.render.html` produces deterministic escaped HTML without React;
- `open-hax.uxx.render.helix` converts portable nodes into React elements.

The location is Knoxx-local for the first slice. The namespace and public API
are intentionally the final extraction boundary: moving the source into
`@open-hax/uxx` must not require caller rewrites.

## Boundaries

Business rules, request access, authorization, and database reads happen before
view construction. Markup functions receive normalized view models and remain
pure.

The portable subset does not contain event handlers or function-valued
attributes. Interactive Helix components wrap portable output rather than
smuggling browser behavior into the shared document model.

Raw HTML requires an explicit trusted wrapper. Text and attributes are escaped
by default. URL-bearing attributes use an allowlisted scheme policy. The HTML
renderer remains React-free so OAuth, error, email, and static protocol pages do
not require a browser runtime.

## First fixture

The MCP OAuth consent page is the first migrated document. It keeps its existing
fields, actor warning, tool selections, hidden OAuth parameters, styling, and
copy while replacing local escaping and string concatenation with AST view
functions plus the shared HTML renderer.

## Consequences

- Server and browser structure can be tested for normalized-DOM parity.
- Security policy is centralized instead of repeated in templates.
- React lifecycle, reconciliation, hooks, and context remain outside the AST.
- CSS-in-JS and a general template language are explicitly not introduced.
