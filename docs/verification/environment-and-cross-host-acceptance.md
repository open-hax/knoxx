# Environment promotion and cross-host identity acceptance

> **Moved 2026-09-30:** Environment promotion and cross-host identity: plan, observations and the Services promotion gate now live in Foresight at [`docs/notes/ops/environment-promotion-and-cross-host-identity-plan.md`](https://github.com/open-hax/foresight/blob/main/docs/notes/ops/environment-promotion-and-cross-host-identity-plan.md) (relationship: services, axxium, Promethean hosts).

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

## Local validation

The Services promotion pipeline and mutation gate moved to Foresight with the plan above.

Local validation: 1,667 backend tests / 7,319 assertions; 212 frontend tests passed
with 41 pre-existing TODOs; warning-free backend/frontend compilation; boundary
check; six mutation-harness tests / 28 assertions. The 1,000-mutant production
campaign has not been run and no production deployment is claimed.
