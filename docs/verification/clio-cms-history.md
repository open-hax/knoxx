# CMS history backed by Clio

Knoxx persists CMS revisions through `@eta-mu/document-history` at eta-mu commit
`61a60f0e9d19768475ef5abd5251bbdccf1d3015`. That package calls Clio directly for
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
  seeds/<document-id>.lock           # stable migration lock inode
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

The editor's History view lets a reader inspect every version. Review each current
head, compose the desired body/title in the editor, then choose **Save resolution from
editor**. This appends a revision naming the reviewed heads. A newly arriving branch
remains a conflict. Publication and translation refuse unresolved CMS conflicts.

Legacy JSON records import once under the package's Clio migration lock. Original
JSON and Markdown remain untouched. Existing publication intent stays in its authored
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

The read-only browser tour is `scripts/verify-cms-history-tour.sh`. Supply
`KNOXX_FRONTEND_URL`, the exact `KNOXX_DOCUMENT_TITLE`, and an already authenticated
`KNOXX_BROWSER_SESSION`. It captures document and History screenshots under the
ignored `docs/verification/screenshots/` directory. It modifies no application data;
the fixture verifier covers writes. The deployed acceptance walkthrough separately
uses fresh recipient login while the source Axxium service is stopped.

Local validation: shared package NBB and compiled CLJS each pass 8 tests/54 assertions;
Knoxx backend passes 1,669 tests; frontend passes 219 tests with 41 existing TODOs,
plus the final 13-test CMS regression suite. Backend and frontend compilers report
zero warnings. Existing unrelated Router test warnings remain in five frontend suites.
