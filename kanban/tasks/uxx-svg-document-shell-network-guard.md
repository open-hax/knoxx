---
uuid: "uxx-svg-document-shell-network-guard"
title: "Render SVG browser documents through UXX and block resource loading"
status: done
priority: "P1"
labels: ["tasks", "backend", "security", "svg", "puppeteer", "uxx", "5sp"]
created_at: "2026-08-05T15:59:00Z"
completed_at: "2026-08-05T16:17:00Z"
source: "uxx-shared-markup-html-helix-renderers"
points: 5
category: "tasks"
---
# Render SVG browser documents through UXX and block resource loading

## Context

The first UXX shared-markup slice migrated the MCP consent page, but
`backend/src/cljs/knoxx/backend/infra/svg_render.cljs` still concatenated an
HTML document around dynamic SVG before handing it to Chromium.

That raw SVG originates from agent and attachment paths. JavaScript is disabled,
but SVG can still request remote images, fonts, stylesheets, filter resources,
or other URLs. A browser renderer therefore needs both an explicit raw-markup
capability boundary and a network-deny boundary.

## Work

- Build the Puppeteer document shell through `open-hax.uxx.render.html`.
- Validate SVG before converting it into a trusted raw-markup value.
- Preserve local fragment references such as `url(#glow)` and `href="#id"`.
- Reject active HTML/SVG elements, event attributes, doctypes/entities,
  processing instructions, declarative mutation elements, `xml:base`, external
  URL attributes, CSS imports, and non-fragment CSS URLs.
- Enable Puppeteer request interception and abort every resource request before
  setting page content.
- Extend focused shared-markup lint and renderer tests.

## Definition of Done

- No document-shell string concatenation remains in `svg_render.cljs`.
- Dynamic SVG crosses one reviewed validation function before `:raw-html`.
- Browser JavaScript is disabled and network requests are aborted.
- Existing gradients, filters, fonts, and fragment references still render.
- Negative tests cover script/event injection, declarative mutation, and browser
  resource-loading spellings.
- Focused lint, backend compilation/tests, frontend checks, and Rheos drift
  validation pass.

## Completion

Implemented in Knoxx PR #223.

- Replaced the Puppeteer HTML shell concatenation with a shared UXX AST and
  deterministic HTML rendering.
- Added `validate-svg!` as the sole capability crossing into trusted raw markup.
- Preserved local fragment filters, gradients, masks, symbols, and `use`
  references.
- Rejected scripts, `foreignObject`, event attributes, doctypes/entities,
  processing instructions, active HTML elements, SVG animation/mutation
  elements, base URLs, external/data resource references, CSS imports, and
  non-fragment CSS URLs.
- Disabled JavaScript, enabled Puppeteer request interception, and aborted every
  request before document content is installed.
- Added deterministic-shell, negative security, fake-page interception, and
  Chromium PNG regression tests.
- Extended the focused shared-markup lint gate and architecture decision.

Verification evidence:

- Knoxx CI run `31023999070`: focused lint, backend typecheck/tests/coverage,
  frontend TypeScript, frontend CLJS parity, and Vitest coverage all passed.
- Rheos run `31024001928`: board snapshot generation, validation, and drift
  ledger checks passed.
- Review Resolution Gate run `31024000107` passed.
- CodeRabbit status passed on implementation head
  `7b42e34020e432225ab27834107234b25b3d47af`.

---
Triage 2026-08-05: Completed by PR #223. Dynamic SVG now crosses an explicit
validated raw-markup capability, while Chromium independently denies JavaScript
and all resource requests. Verdict: done (P1, 5sp).
---
