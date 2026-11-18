# Verify the Big Pi local compatibility lab

This guide covers a local test lab, not certified Optimizely compatibility. Run it from this checkout with Node.js 24+ and npm:

```bash
npm ci --ignore-scripts --prefix packages/big-pi-optimizely
bash scripts/verify-big-pi-local.sh
```

The verification script is the source of truth for its actual checks and exit status. The package also has `npm test --prefix packages/big-pi-optimizely` for domain and injected-client bridge tests and `npm run test:integration --prefix packages/big-pi-optimizely` for real loopback HTTP/MCP tests. Do not treat fixture bridge tests as verification against the Knoxx application or a vendor tenant.

## Browser tour

```bash
cd packages/big-pi-optimizely
npx playwright install chromium
npm run test:browser
```

The tour starts its own isolated server and data directory. It checks authoring, language drafts, retained technical values and unrelated properties, stale-save rejection without losing unsaved text, refusal of structured fields, disconnect, and mobile overflow. It captures desktop and mobile screenshots in the package's ignored `screenshots/` directory (override with `BIG_PI_SCREENSHOTS_DIR`). Use a system Chromium or set `PLAYWRIGHT_CHROMIUM_EXECUTABLE` to its executable. `npm run test:all` includes the browser gate; `.github/workflows/big-pi-optimizely.yml` installs Chromium and runs it on pull requests.

The first managed-workspace attempt on 2026-11-18 failed before loading the page: Chromium could not create its process socket (`Operation not permitted`). The connected browser was unavailable and the separate cloud browser could not reach the workspace loopback server. A Playwright headless-shell download also failed to produce a valid archive. These are unavailable UI verification, not a passing tour. Check the latest CI evidence before treating the UI as verified.

For manual exploration, start with `bash scripts/start-big-pi-local.sh`, open the displayed loopback address, and use the generated local token. The example is synthetic. The optional configured Knoxx bridge uses an existing CMS account and its organization permissions; see the [package README](../../packages/big-pi-optimizely/README.md) for settings, exact-revision import and draft return. It creates no SME approval, and it does not publish. Mu owns review and acceptance. Existing Knoxx history permits branches; the bridge reports conflict and cannot provide a cross-system transaction. A write whose response is lost is uncertain; inspect the destination before retrying.

Known gaps: the local API and tools are an explicit subset; vendor schema/tenant parity, real Knoxx end-to-end authorization and live tenant OAuth, remote deployment, full editor, publication and delivery, and the generic provider contract remain unqualified. The browser tour and frozen verification gates must be assessed from their run results; a planned or unavailable test is not a pass.

## Runtime and check boundaries

This is a separate NBB package; it does not change the existing Knoxx backend build. Domain and law namespaces remain pure ClojureScript. The named extern boundary owns Node, HTTP, disk, SDK and JSON interop. NBB's bundled runtime uses Promesa for the asynchronous coordinator; native compiled CLJS `^:async` is not available in this runner. The transport-neutral coordinator accepts only CLJS data and injected request functions.

The backend's shadow-cljs/typecheck gates are not claimed as passing. An initial pnpm invocation encountered the backend's dependency build approval gate before typecheck ran; no backend source or dependency manifests are part of this change. Package tests directly exercise the new build target. Live Knoxx behavior, its organization isolation and the future provider laws remain separate required qualification stories.
