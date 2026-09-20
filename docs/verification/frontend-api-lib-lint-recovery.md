# Frontend API and library lint recovery

The scoped scan of `frontend/src/cljs/knoxx/frontend/api` and `lib` initially
reported 50 warnings across 11 files. The corrected source reports zero errors
and zero warnings, with the same lint rules and thresholds. The public contract
wrapper delegates to a new named library boundary; shared fetch sequencing uses
native async/await; websocket connection setup is split into named operations.
Route values, local-storage behavior, CMS decoding, and publication drafting
retain their existing values and behavior. Public functions now describe their
contracts, and locals no longer shadow core names.

## Self-review decisions

The contract client still uses its existing browser bridge through
`knoxx.frontend.lib.contracts`. Inspection of the actual TypeScript transport
found three differences from the existing native HTTP helper: configured
`VITE_API_BASE`, development identity fallbacks, and error formatting. Replacing
that bridge as a lint edit would change those behaviors. The named boundary
preserves them and only changes Promise sequencing and decoded-value ownership.
A broader native transport migration must explicitly preserve or revise those
configuration contracts.

The websocket extraction retains the same reconnect delay, maximum backoff,
disposal handling, channel routing, and conversation rebinding. Its existing
behavior tests execute those paths. The shared request helper retains raw error
text and its empty-body status fallback; a new async test exercises both.

## Verification

The first combined frontend run compiled successfully but reported one test
error: the migration inventory rejects explicit `:include-macros` imports.
The new test did not need that option; it now uses the existing `cljs.test`
alias pattern. No migration rule was weakened. This failure also demonstrates
why compiler exit status alone is insufficient for this suite.

The corrected combined run passes **507 tests / 2,207 assertions**, with zero
failures or errors. The test build reports **279 files / 0 warnings**. It includes
the companion Agents extraction tests. The recovered fatal-async guard was
loaded via `NODE_OPTIONS`, and actual test counters were inspected. The scoped
API/library scan plus the new test reports **0 errors / 0 warnings**.

The frontend owner runs the production release after this shared test gate.
Production bundle and live browser evidence belong to the encompassing Wiki
walkthrough; this report does not claim a separate completed browser tour.
