# Environment promotion and cross-host identity acceptance

## Requested contract

- Stealth is this machine (`192.168.12.128` observed); Yoga is `192.168.12.68`.
- Service URLs follow `<env>.<service-name>.promethean.rest`.
- An authorized code owner can label a PR targeting `main` with `testing` to claim its service's testing slot.
- A competing live testing claim blocks takeover for two hours. The working interpretation is a two-hour incumbent lease; confirmation was requested.
- A successfully merged main revision deploys to staging, not directly to production.
- Production promotion requires integration, end-to-end, and several batches of hundreds of actual source mutations, all tied to the same immutable revision/artifact.
- Deploy first on Stealth, then Yoga. Register/copy a user identity across the two Axxium instances in a browser.
- Stop only the original identity-provider service after registration; verify fresh authentication and Knoxx content-management, review, and translation workflows on the second machine without dependence on that issuer.
- Capture browser screenshots of actual actions and resulting state. Do not substitute API-only or simulated screenshots.

## Execution phases

1. Inventory current source, runtime paths, deployment triggers, identity capabilities, and existing test harnesses.
2. Implement code-owner testing-slot admission, main-to-staging delivery, immutable production qualification, and shared environment instructions.
3. Implement/repair Axxium portable identity and the Knoxx authentication boundary; validate real integration and negative cases.
4. Deploy isolated service stacks on Stealth then Yoga, with persistent data and HTTPS ingress.
5. Run the browser acceptance flow, stop the original Axxium service, authenticate anew and complete content/review/translation workflows; capture evidence.
6. Run production qualification, save honest outcomes and source changes, and update reusable skills.

## Initial observations

- Both machines are reachable over SSH/LAN; Yoga's existing Axxium submodule git metadata is broken. Use an isolated deployed checkout rather than repairing unrelated workspace state.
- Neither machine currently has a running Axxium container.
- Current upstream Axxium has signup/login APIs and a static landing page; it has no implemented identity export/import flow or registration UI.
- Knoxx has existing CMS, publication, review and translation browser verifiers, and a source-mutation harness to extend/reuse.
- Stealth's Tailscale client is logged out; public ingress needs an existing authenticated path or a service-specific SSH tunnel without requiring a new login.

This file records acceptance criteria and progress, not a completion claim.

## Implemented host runtime

`compose.environment.yaml` runs an isolated Mongo replica set, a non-root backend
and a non-root frontend at loopback port 18880. Prepare it with
`node scripts/prepare-environment.mjs DIRECTORY ENV BACKEND_IMAGE FRONTEND_IMAGE`.
Axxium sign-in is bound to `https://<env>.axxium.promethean.rest`; passwords are
verified by that recipient instance, and Knoxx keeps its own session and local
organization membership. An email alone cannot link an existing local account.
New imported identities receive the basic-user role. To exercise CMS, replace
that chat-only role with an organization-local role carrying publication and
translation read/manage/review permissions; never import source host privileges.

CMS documents now use `/api/cms/documents`, not legacy OpenPlanner/ingestion
proxies. The server constructs organization-scoped file paths and generates
private document resources and withheld English/Spanish publication intents.
Saving enters review. The CMS publication button changes only the resource
intent; actual materialization remains the Gardens publication action. Documents
from the old external CMS store are not implicitly migrated by this change.

Before local translation, set `OLLAMA_BASE_URL`, `OLLAMA_DEFAULT_MODEL`,
`EMBED_PROVIDER_BASE_URL`, `EMBED_PROVIDER_MODEL` and the matching positive
`EMBED_PROVIDER_DIMENSIONS`. Match the `publication_translator` agent and pipeline
policy to an available catalog model. Yoga uses local `gemma4:e4b` and
`nomic-embed-text:latest` at 768 dimensions. Background event runtimes remain
explicitly disabled; manually dispatched publication translation still runs.

The acceptance browser registered on Stealth, copied its signed identity to Yoga,
registered with a new Yoga password, and then signed in freshly on Yoga after
`axxium-stealth-axxium-1` was stopped. That same user signed in to Yoga Knoxx,
authored and reloaded a CMS document, approved publication intent, dispatched a
real local Spanish translation, submitted a correction with review notes, and
approved the three generated sections. Screenshots are held in the operator's
acceptance outputs; they are real browser captures, not mockups.

Run `scripts/verify-cross-host-workflows.mjs` after the browser walkthrough with
`KNOXX_VERIFY_ORIGIN`, `AXXIUM_VERIFY_SOURCE_ORIGIN`, `KNOXX_VERIFY_EMAIL`,
`KNOXX_VERIFY_PASSWORD` and optional `KNOXX_VERIFY_DOCUMENT_TITLE` in the process
environment. It checks fresh recipient authentication, source endpoint failure,
persisted CMS content, anonymous refusal and invalid visibility refusal. Do not
put credentials into command arguments or commit its private input file.

## Promotion and validation

The pinned Services workflow owns code-owner testing admission and exact-main-
merge staging. A newer main commit supersedes an older queued staging candidate.
CODEOWNERS initially names the three existing repository administrators. App
callers activate when reviewed changes reach main. Production qualification
requires a merged immutable commit, integration and real e2e checks, then four
non-overlapping batches of at least 250 source mutations. Only completed tests
with assertions can kill a mutant. Invalid compilation, timeouts, survivors,
duplicates and stale evidence fail the gate.

Local validation: 1,667 backend tests / 7,319 assertions; 212 frontend tests passed
with 41 pre-existing TODOs; warning-free backend/frontend compilation; boundary
check; six mutation-harness tests / 28 assertions. The 1,000-mutant production
campaign has not been run and no production deployment is claimed.
