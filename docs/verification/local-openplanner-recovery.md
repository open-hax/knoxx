# Canonical local OpenPlanner recovery

This driver uses `eta-mu/packages/clio` and the existing ClojureScript service
protocol. It was reconstructed after the sandbox source loss; it is not claimed
to be byte-identical to the earlier, unpushed implementation.

Set `KNOXX_OPENPLANNER_CLIENT_MODE=edn`. The explicit ledger directory is
`KNOXX_OPENPLANNER_DIRECTORY`, falling back to `KNOXX_WIKI_DIRECTORY/openplanner`.
Mongo and REST remain separately selectable drivers. The local driver never
silently delegates an unsupported operation to either service.

The model settings are `EMBED_PROVIDER_BASE_URL`, `EMBED_PROVIDER_MODEL`, and
`EMBED_PROVIDER_DIMENSIONS`. All three may be absent for login, session reads,
source authoring and ledger inspection. Partial configuration fails at startup.
An embedding request without a model returns `503
openplanner_embedding_not_configured`. Events are already durable at that point;
the configured driver can repair their missing vectors on the next search or an
explicit `ensure-event-vectors!` call. Real model output must have exactly one
finite, correctly dimensioned vector per input and the configured model name.

The protocol currently supports events and stable event lookup, session views,
vector search, document ingestion, translation segments and labels, document
reviews, manifests/SFT exports, and translation batch admission/claim/progress.
The ledger records finite operations, not Mongo query expressions. Every
tenant-facing read validates explicit organization scope, including empty
histories. Translation labels stay attached to their original candidate
generation; a new candidate cannot inherit an earlier acceptance.

Graph expansion/export, graph monitoring/edge construction, record reactions,
unscoped document statistics, and generic Mongo/HTTP proxy methods return a named
503 unsupported-operation response. Document reviews report graph projection
failures explicitly. These are remaining service implementations, not successful
mock responses. The full recovered website/browser gate is a separate pending
integration check.

## Fresh verification

The recovered provider's eight tests ran with the fatal asynchronous test guard
together with source review, source authoring repair, publication gates and the
translation receipt writer: **50 tests, 274 assertions, zero failures/errors;
187 compiler files, zero warnings**. Evidence is
`knoxx-recovered-openplanner-focused.log` in the task evidence directory.

The provider tests use an explicit deterministic embedding fixture to exercise
protocol validation and durable failure paths. They do not claim model quality
or replace the real model process used by the browser walkthrough. They create
and remove dedicated temporary directories. Unexpected rejected promises fail
an assertion and the Node test guard exits nonzero.

Covered failures include tenant omission before model access, tenant isolation,
changed immutable event bodies, malformed/partial model settings, invalid clocks
before files are created, nonfinite vectors, stale candidate reviews, changed
batch dispatch retries and two workers claiming one queued batch.
