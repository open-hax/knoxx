# OpenPlanner caller scope and event projection recovery

The recovered caller adapters bind `:openplanner-org-id` from the verified
request context or the actual run. An ambient configuration value and a query
string cannot select another organization. Session pages, session detail,
message hydration, semantic/memory search and title queries carry explicit
protocol scope. Session pages use the named service operation for every driver;
the earlier Mongo-only optimization is no longer a route dependency.

`GET /api/openplanner/v1/sessions` resolves authentication and checks
`agent.memory.read` before reading the selected provider. Its pagination is
decoded separately from authoritative tenant/project options. The remaining
generic proxy requires a verified system administrator, rewrites tenant scope
and forwards no legacy identity headers. The local driver explicitly rejects
that generic operation.

The optional diagnostic event sink is installed once at bootstrap. Every event
uses its own run's organization and conversation; ending a different turn must
not clear the sink. Canonical run persistence is independently awaited by the
run lifecycle. `index-run-memory!` now projects searchable memory only and never
writes another authoritative run snapshot containing transient `:events`.

The guarded focused recovery suite executed **54 tests, 295 assertions, zero
failures and errors**. It included two tenants sharing the same conversation
identifier, malicious query scope, omitted authority, a denied reader and
interleaved diagnostic events. The last run also reported an unrelated warning
in the concurrently reconstructed policy membership adapter (`opts` was not
bound). That run is not claimed to be a clean production build; the combined
server gate must cover the repaired policy adapter and the complete assembly.

New scope/proxy/sink namespaces and tests lint without warnings. Existing size
warnings remain in untouched functions in `infra/openplanner/memory.cljs`
(`batch-upsert-openplanner-documents!`, `run-scope-extra`,
`session-text-graph-events`, `legacy-base-events`,
`legacy-message-graph-events`), `infra/core_memory.cljs`
(`session-summary-scope-from-rows`), `infra/agent/hydration.cljs`
(`build-agent-multimodal-message`), and `infra/openplanner/tools.cljs`
(`publication-translation-pair`), plus the pre-existing size of the semantic
tool file. The touched document-ingestion function was split into a readable
payload helper; its warning and a misplaced memory-search docstring were fixed.
