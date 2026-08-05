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

### Dynamic SVG

Dynamic SVG is not considered trusted merely because it is rendered with
JavaScript disabled. SVG can still initiate browser requests through images,
styles, fonts, filters, base URLs, and related resource references.

Pure acceptance policy lives in `knoxx.backend.law.svg`; the
`knoxx.backend.infra.svg-render` namespace contains only browser lifecycle and
Puppeteer effects. The law requires exactly one balanced `<svg>` document root
with no leading or trailing markup or text. It permits local fragment references
such as `url(#glow)` and `href="#symbol"`, while rejecting active or mutating
content, event attributes, declarations/entities, processing instructions,
base URLs, CSS imports, and non-fragment resource references.

Only after this law accepts the document may the infra adapter construct a UXX
trusted raw-markup value. Validation is not the only network boundary: the
Puppeteer page also enables request interception and aborts every request before
document content is set. This makes the law the reviewed capability boundary
and browser interception the defense-in-depth runtime boundary.

## First fixtures

The MCP OAuth consent page is the first migrated document. It keeps its existing
fields, actor warning, tool selections, hidden OAuth parameters, styling, and
copy while replacing local escaping and string concatenation with AST view
functions plus the shared HTML renderer.

The Puppeteer SVG document shell is the second fixture. Its outer HTML structure
uses the same AST and deterministic renderer, while law-validated SVG crosses
one explicit raw-markup boundary inside the body.

## Consequences

- Server and browser structure can be tested for normalized-DOM parity.
- Security policy is centralized instead of repeated in templates.
- React lifecycle, reconciliation, hooks, and context remain outside the AST.
- Dynamic raw-markup domains must define pure law/guard validation before
  constructing a trusted value.
- Effectful `infra.*` adapters invoke policy but do not own it.
- Browser renderers must enforce network policy independently of string
  validation.
- CSS-in-JS and a general template language are explicitly not introduced.
