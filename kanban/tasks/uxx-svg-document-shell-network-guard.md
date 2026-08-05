---
uuid: "uxx-svg-document-shell-network-guard"
title: "Render SVG browser documents through UXX and block resource loading"
status: in_progress
priority: "P1"
labels: ["tasks", "backend", "security", "svg", "puppeteer", "uxx", "5sp"]
created_at: "2026-08-05T15:59:00Z"
source: "uxx-shared-markup-html-helix-renderers"
points: 5
category: "tasks"
---
# Render SVG browser documents through UXX and block resource loading

## Context

The first UXX shared-markup slice migrated the MCP consent page, but
`backend/src/cljs/knoxx/backend/infra/svg_render.cljs` still concatenates an
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
  `xml:base`, external URL attributes, CSS imports, and non-fragment CSS URLs.
- Enable Puppeteer request interception and abort every resource request before
  setting page content.
- Extend focused shared-markup lint and renderer tests.

## Definition of Done

- No document-shell string concatenation remains in `svg_render.cljs`.
- Dynamic SVG crosses one reviewed validation function before `:raw-html`.
- Browser JavaScript is disabled and network requests are aborted.
- Existing gradients, filters, fonts, and fragment references still render.
- Negative tests cover script/event injection and browser resource loading
  spellings.
- Focused lint, backend compilation/tests, frontend checks, and Rheos drift
  validation pass.
