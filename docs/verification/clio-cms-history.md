# CMS history backed by Clio

Knoxx persists CMS revisions through `@eta-mu/document-history` at eta-mu commit
`a2f428afd7623dcd188d535512525ee521a5ba61`. That package calls Clio directly for
creation, locked append, validation, schema history, canonical replay and hashes.
Knoxx retains organization authorization and publication policy.

Each accepted event contains EDN metadata, complete Markdown, authenticated actor,
Clio timestamp, and the editor-observed parent revisions. Independent saves use
sealed ledger partitions. One, ten or one hundred partitions form the same logical
history; duplicate records deduplicate through Clio. Concurrent edits remain separate
heads until an explicit revision consumes them. Wall-clock timestamps are observations;
causal parents and Clio's deterministic order handle timestamp ties and clock skew.

With the installed content root, the layout is:

```text
/state/content/.ημ/cms/<organization>/
  schemas/                          # retain alongside the ledger
  ledgers/<event-uuid>.edn           # accepted immutable events
  seeds/<document-id>.lock           # stable initialization lock inode
  operations/<document-id>.lock      # CMS save/publication operation lock
  snapshots/<document-id>/<hash>/
    metadata.edn
    document.md
    snapshot.edn
```

Reads replay the ledger and derive immutable snapshots. Removing snapshots rebuilds
the same content; stale snapshots cannot select the current revision. Copy schemas
and finalized ledger partitions when transferring a history. Pending hidden files
are unaccepted interrupted work. Snapshots may be regenerated. Do not rewrite or
remove accepted ledgers to undo an edit: append a revision with the desired content.
The native `fs-ext-extra-prebuilt` dependency is pinned at 2.2.9 and must run its
install/rebuild step for the deployment's Node ABI.

GET `/api/cms/documents/:id/history` exposes every revision. POST accepts empty
`parents`; PATCH requires the revisions observed by the editor and rejects an absent
base with 428. Stale saves succeed as preserved sibling revisions. Responses include
`revision`, `revision_heads`, and `conflicted`; the write response contains the writer's
own branch. JSON remains the HTTP wire encoding; persisted metadata is EDN.

Document lists accept decimal `limit` (1–1000, default 100) and `offset`
(0–2147483647, default 0). Invalid values return 400 before storage is opened.
Garden/source-path filters run before stable document-ID ordering and pagination.
Responses include `total`, `limit`, `offset` and `has_more`. This bounds the
response; the underlying store still replays the organization's ledger.

The editor's History view lets a reader inspect every version. Review each current
head, compose the desired body/title in the editor, then choose **Save resolution from
editor**. This appends a revision naming the reviewed heads. A newly arriving branch
remains a conflict. Publication and translation refuse unresolved CMS conflicts.
Publication intent edits validate the immutable source path and ledger head while
holding a synchronous Clio kernel operation lock shared with CMS saves. No await
occurs under that lock. Source digest reads recheck that path/head after asynchronous
I/O, refusing stale evidence. This coordinates Knoxx writers sharing this filesystem;
it is not a claim of cross-host consensus or a migration of publication manifests.

A validated relative logical source identifier is separate from the immutable
snapshot path. New documents at the same organization/path share one deterministic
ID, so simultaneous creates retain independent root revisions in the same history.
Listing includes the logical path for rediscovery. Descriptive document metadata is
a bounded map, retained with each body; it never grants organization permissions or
publication intent.

Legacy JSON records import once under the package's Clio migration lock. Original
JSON and Markdown remain untouched. Historical public/archived editor visibility
is retained in EDN metadata while the editor uses internal visibility. Existing publication intent stays in its authored
EDN manifest; document reads project the current title/source into the resource view
without rewriting publication intent. Already approved translations continue to match
an unchanged source-content digest. No signing keys, databases or identity ownership
are moved by this migration.

Rheos was inspected: its Markdown-first writes and incomplete mutation events cannot
currently reconstruct all task state. Its existing canonical-fold and Markdown-sync
cards own adopting the shared package. The eta-mu design note records that seam; this
change does not claim Rheos itself has been migrated.

## Verification

Run `scripts/verify-cms-history.sh` from this checkout. It compiles a fresh production
CMS adapter, serves a real loopback Fastify listener, seeds test principals at the
existing auth-context seam, writes only beneath its temporary fixture directory,
and removes that directory on exit or interruption. It tests anonymous and
cross-organization refusal, read-only permissions, stale concurrent saves, actor
provenance, explicit resolution, snapshot reconstruction, and migration preserving
publication intent. It requires passing test counters even if Shadow exits zero.
It does not verify deployed image identity or password login.

Run `scripts/verify-cms-history-tour.sh` with `agent-browser`, Babashka (`bb`),
`pnpm`, Clojure, Node, curl and ripgrep installed. It builds this checkout's
frontend and a test-only loopback server containing the production CMS routes.
It seeds a unique document with two revisions in a new temporary directory,
checks anonymous refusal and retained history over HTTP, opens a fresh browser
session, and captures the current document and its initial revision. No existing
document, logged-in session or deployed-service configuration is required.

The script records the checkout path, Git head and tracked diff digest alongside
the fixture in `.ημ/tour.edn`. It stops its server, closes its browser session and
removes its temporary data on normal exit, failure, INT or TERM. Only screenshots
remain under the ignored `docs/verification/screenshots/cms-history-<run>/`
directory (or `KNOXX_SHOT_DIR`). Fixture receipt/session data must not be committed.

`--check` builds and verifies the HTTP fixture, then cleans up without opening a
browser. `--serve` keeps that same fixture running for a manual browser tour;
its private receipt contains the session-start URL. Stop with Ctrl-C to clean up.
The default browser mode additionally checks that the UI shows each expected body.

The identity and publication-topology responses are explicit fixture seams.
The tour does not prove password authentication, publication execution, unrelated
services, or the identity of an already deployed image. The deployed acceptance
walkthrough separately uses fresh recipient login while the source Axxium
service is stopped.

Validation counts are recorded in the PR and deployment receipt for their exact revisions.
