# Verify the frontend WebSocket migration

This artifact runs the actual optimized frontend against its existing isolated e2e fixture. It exercises the named CLJS adapter, the active chat consumer and the Ops status consumer without a model, provider, production service or database.

Build the frontend from the checkout being reviewed. Install only its declared locked e2e dependencies if absent:

```bash
npm --prefix frontend/e2e ci --ignore-scripts --no-audit --no-fund
pnpm -C frontend run build
```

The script requires an explicitly resolved existing Playwright-core **1.58.2** runtime. It checks that runtime's package version and SHA256 `16a25dbd7368a87af8b58a45c6216b9cec5a91a72cf704533344701997ebde22`. This is an operator precondition, not an implicit package or hosted-CI dependency. It fails when that runtime, the existing Puppeteer fixture, Chromium or the optimized output is unavailable. It never installs a new runtime.

```bash
KNOXX_WS_EXPECTED_HEAD="$(git rev-parse HEAD)" \
KNOXX_WS_PLAYWRIGHT_ROOT=/absolute/path/to/existing/playwright-core \
KNOXX_WS_EVIDENCE_DIR=/absolute/path/to/new/ws-evidence \
  scripts/verify-frontend-ws-migration.sh
```

Optionally set `KNOXX_WS_NODE_BIN` to the checkout's qualified Node executable and `PUPPETEER_EXECUTABLE_PATH` to an existing Chromium binary. The expected head and unchanged runtime-source checks bind the observation to this checkout. Emitted Shadow/Vite source-map contents must match every exercised source file, so a stale optimized build refuses before starting a browser. `proof.json` records source and built-output hashes, runtime identity, every fixture HTTP destination, packets, actual controls and cleanup. The evidence directory must be new so earlier captures cannot be overwritten.

The existing `frontend/e2e/knoxx_e2e_support.cjs` owns the temporary loopback HTTP fixture and Puppeteer browser. Playwright attaches by CDP only to that owned browser. The script registers `routeWebSocket` before navigation; it does not call `connectToServer` and does not add a WebSocket server. HTTP requests must remain GETs to the owned fixture. A pending chat message is seeded only in disposable browser storage, and fixture status reads keep it active. No send button or model endpoint is invoked.

The screenshots show literal repeated text followed by a replacement and a real Ops metrics update. The proof also checks exact whitespace in persisted chat content, the optimized replacement/refusal matrix, UTF-16 offsets, empty replacements, undefined optional metadata, configured HTTP/HTTPS API bases and encoded IDs, conversation clearing, existing reconnect behavior and disposal/navigation cleanup. Chat/Ops transitions click the real SPA links and must preserve `performance.timeOrigin`, distinguishing component cleanup from automatic page teardown. It exercises the existing lifecycle rather than adding a lifecycle policy.

The shell removes its temporary script directory on exit or interruption. The Node fixture closes its owned browser and server in `finally` and on signals. Screenshots and `proof.json` remain as intentional evidence. This fixture establishes local packet delivery; it supplies no production, provider, native-review, real CMS persistence or translation qualification. The registered AVA cases and other frontend gates retain their independent obligations. Existing Router warnings and TODO tests remain visible.

Unmocked ancillary fixture routes remain visible as local 404 console diagnostics. A passing packet check does not qualify those API surfaces or establish a warning-free browser session. The unchanged registered AVA cases remain separately required; a timeout or missing audit UI is a failing exit, never covered by this artifact.
