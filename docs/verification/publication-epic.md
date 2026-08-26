# Verifying the contract-owned publication epic by hand

> Epic: `kanban/epics/knoxx-contract-owned-publication-pipeline.md`
> PRs: #229 (merged), #230, #232–#237, #239, #240, #241

This is the walkthrough for a reviewer who wants to *see* the epic work before
reading eleven diffs. It takes about ten minutes: two scripts, one browser
window, a handful of screenshots.

Both scripts create their own throwaway resources and delete them on exit,
including on failure or Ctrl-C. Neither touches anything already in
`contracts/`, and neither needs a hosted OpenPlanner service — that absence is
the point of the epic, not a limitation of the test.

---

## What this epic changed, in one paragraph

Publication used to be a document-level flag owned by a hosted OpenPlanner REST
service. It is now a **relation** — a document, published into a garden, in a
locale, at a revision — declared as EDN resources in `contracts/` and resolved
by Knoxx itself. Desired state lives in the resource graph. Whether a deployment
effect actually succeeded is *observed* evidence, kept in receipts and reported
separately. The CMS shows both, because when they disagree that disagreement is
drift and hiding it would be a lie.

---

## Before you start

You need a Knoxx backend running **from this checkout**. That is the part people
get wrong: the PM2 `knoxx-backend` process points at
`/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend`, a
different working copy, so verifying against it verifies the wrong code.

```bash
pm2 describe knoxx-backend | grep cwd    # confirm which checkout is live
```

Start the backend from here however you normally do, then:

```bash
export KNOXX_API_KEY=<the same key the backend was started with>
export KNOXX_BASE_URL=http://localhost:8000     # default
```

Step 0 of the API script checks this for you: it seeds a resource and fails
immediately if the running backend cannot see it.

---

## 1. The API journey

```bash
scripts/verify-publication-epic.sh
```

Nine sections, 35 checks. Each prints what it proved, not just a green tick.

| § | What it walks | Why it is in here |
|---|---|---|
| 0 | The running backend serves this checkout | Everything below is meaningless otherwise |
| 1 | All six surfaces refuse an anonymous caller | The projection exposes document titles, garden membership and publication paths — an open route is an enumeration leak |
| 2 | The desired topology resolves from resources alone | No hosted backend is contacted at any point |
| 3 | The CMS view keeps `desired` and `observed` apart | They are separate wire fields; their disagreement is drift |
| 4 | An authorized PATCH rewrites the EDN file on disk | The resource graph is the authority, not a cache of one |
| 5 | A schema-invalid resource **blocks** the projection | See below — this is a review finding |
| 6 | A colliding canonical identity is a **conflict** | See below — this is a review finding |
| 7 | Translation config resolves with no hosted backend | Same surface the JVM ingestion worker reads |
| 8 | No shipped source calls a retired authority path | Repo-wide, unlike the backend test |
| 9 | Advisory: OpenPlanner surfaces still live elsewhere | See "Known gap" below |

### The two sections worth watching closely

**§5 — a bad resource must block, not vanish.** The loader drops a record that
fails its Malli shape. Without an explicit blocker, the projection would answer
`200` with that publication *silently absent* — the CMS would render a clean,
confident, wrong topology. The script seeds a publication whose path is missing
its leading slash and asserts the projection returns `409` naming the rejected
resource.

**§6 — a colliding identity must conflict, not resolve.** `dedup-contracts`
applies first-wins dedup on `[kind id]`. Two files declaring the same canonical
id with different payloads would collapse into whichever one `readdir` returned
first, making the published topology depend on filesystem enumeration order. The
publication facade deliberately reads the **undeduped** record list so the
resolver can see the collision and refuse. The script seeds exactly that
collision.

Both of these were unreachable through the real load path until they were fixed
— the unit tests passed because they built the index directly and bypassed the
loader.

---

## 2. The browser tour

```bash
pnpm -C frontend dev          # in another terminal
scripts/verify-publication-tour.sh
```

Screenshots land in `docs/verification/screenshots/`. The tour:

1. seeds the dev identity into `localStorage`, and signs in for a session if
   `KNOXX_DEV_EMAIL` / `KNOXX_DEV_PASSWORD` are set
2. opens `/cms` and asserts the seeded garden is on the page, having come from
   the resource graph
3. fetches the CMS surface **from inside the page**, so the request carries the
   page's own origin and auth rather than a curl that bypasses them
4. PATCHes the publication to `published`, checks the EDN file on disk changed,
   reloads, and re-reads
5. strips the identity and re-issues the request, which must be refused

### What the tour deliberately does not do

It does not click the CMS publish toggle. `handlePublishToggle` in
`frontend/src/pages/CmsPage.tsx:781` still posts to
`/api/openplanner/v1/cms/publish/:docId/:gardenId` and only calls
`setPublicationState` afterwards. With no OpenPlanner running the click cannot
complete, so driving it would prove nothing about the resource-backed path — and
would fail for a reason unrelated to what is being reviewed.

That coupling is real and worth knowing about while reviewing #239: the
publication *intent* was cut over to resources, but the surrounding CMS document
flow was not.

---

### Two auth gates, and they take different credentials

Worth knowing before you read a failing tour:

- The **API client** (`frontend/src/lib/api/core.ts`) reads `localStorage` and
  sends `x-knoxx-user-email`, which `resolve-auth-context` prefers over both the
  API key and the session cookie.
- The **app shell** guards its routes on a session cookie and does not look at
  `localStorage` at all.

So seeding `localStorage` turns every API assertion green while the page a human
would actually look at renders the login screen. The tour detects that case
specifically and says so, rather than reporting a vague "garden not visible".

Set `KNOXX_DEV_EMAIL` and `KNOXX_DEV_PASSWORD` to a user with a local-password
record to see the real CMS. Local password auth is on whenever `NODE_ENV` is not
`production`.

## Running a backend locally without side effects

Starting a Knoxx backend also arms every schedule, registers every trigger, and
connects live Discord actor gateways from the shared policy DB.

**`KNOXX_DISABLE_EVENT_RUNTIMES` ships in #243, not in this branch.** On a
checkout without it the variable has no consumer and setting it changes nothing:
`infra/core.cljs` starts the event runtime unconditionally and `bootstrap.cljs`
still builds the Discord gateway manager. Anyone who reads the command below as
"no side effects" on this branch will arm schedules and connect shared gateways
while believing they have not — so run a local backend from a branch that
carries #243, or accept the side effects knowingly.

With #243 in the tree the flag is honoured at three points — config reads it,
`event-runtime/start!` refuses outright so the `shadow-cljs` `start` export
cannot bypass the gate, and the tool routes report it as the reason they
declined — and the process prints a banner at boot plus a warning every 60s so
it cannot silently sit in that mode.

```bash
KNOXX_DISABLE_EVENT_RUNTIMES=true PORT=8000 node backend/dist/server.js
```

That flag is a stopgap either way. The real decoupling is carded as
`knoxx-event-runtime-boot-coupling`.

Two other things that will bite on a fresh checkout:

- `pnpm -C backend build` (`shadow-cljs release server`) **fails** — the
  `:server` build sets `:optimizations :none`. Use
  `pnpm -C backend exec shadow-cljs compile server`.
- `@open-hax/openplanner-sdk` is a `link:` to a sibling checkout. If that
  checkout has never been built, the server will not boot. Build it with
  `pnpm --filter "@open-hax/openplanner-sdk..." run build` from
  `spaces/openplanner`.

## What the first live run found

The scripts were written before any of this could be run against a real
backend. The first real run found three defects that the 980-test suite passed
over, all of them at the boundary the tests fake:

1. **Every publication route returned 500.** Fastify builds `request.params`
   with `Object.create(null)`; `js->clj` does not convert a null-prototype
   object and returned it raw, which the closed Malli shape rejected. Fixed in
   #243. Tests missed it because `fake-request` builds params with `clj->js`,
   which has a normal prototype.
2. **Publishing deleted the document being published.** `set-publication-state!`
   wrote the patched intent over the whole manifest file, destroying the
   document and garden declared beside it. Fixed in #243.
3. **Duplicate publication ids are never detected.** `publication-conflicts`
   keys on the relation, not on `:publication/id`, so two files claiming the
   same id with different revisions both land in the index and every lookup
   takes whichever came first. **Fixed in #230** —
   `publication-identity-conflicts` refuses a shared id with unequal payloads
   and `assert-no-identity-conflicts!` raises `conflicting canonical resource
   identity`, whose `:conflicting-payloads` key the adapter already classifies
   as `409`. §6 asserts that status and message and should now be green; it has
   not been re-run against a live backend since the fix, so treat it as derived
   from the code rather than observed.

## The translation producer (§8b)

The section added with the agent-actor composition, and the one worth reading
before the others, because its absence is what made everything else able to pass
while the site stayed monolingual.

### What it asserts, and why that and not something else

A translation only happens if four contracts line up. Three already existed and
had never been connected:

| Contract | What it supplies | Status before |
|---|---|---|
| `contracts/roles/translator.edn` | the role, and its `:cap/translation` | existed |
| `contracts/capabilities/cap_translation.edn` | the tool, `save_translation` | existed |
| `contracts/agents/publication_translator.edn` | an agent holding that role | **missing** |
| `contracts/namespaces/publication.edn` | the trigger that runs it | **missing** |

So §8b checks the *producer*, not the plumbing: a trigger subscribing to
`publication/translation-needed`, enabled, whose action starts an agent session
rather than posting a batch, naming a contract that **resolves in the deployed
catalog**. The last part is the one that earns its keep — a contract file being
present on disk proves nothing, because it only starts a session if it resolves
through role, capability and actor scope, and the catalog is the one view that
has done all three.

The role and capability halves are checked on disk instead, because they are
contract data rather than runtime state, and losing either would resolve the
agent and then hand it no way to submit anything.

### Why an agent and not the worker

`knowledge-ops-translation-mt-pipeline` landed a translation worker, and it runs
out of `ingestion/`. The production compose stack for this site builds `proxx`,
`knoxx` and `caddy` — nothing else. Dispatching to the worker there queued
batches nothing would ever pick up, which is why
`infra.routes.translation-dispatch/default-runner` is `:agent` and why
`KNOXX_TRANSLATION_RUNNER: agent` is stated explicitly in the compose file even
though it is also the code default. Set `KNOXX_TRANSLATION_RUNNER=worker` where
the ingestion worker really is deployed and owns the documents.

### What the composition did NOT invent

Worth knowing while reviewing, because the diff is smaller than it looks:

- **No new evidence model.** The sink hands a finished run to
  `infra.translation-dispatch/resolve-batch-report!` — the same completion path
  the worker's status callback goes through. The drift guard, the receipt
  minting, the claim settlement and the refusal law all stay there.
- **No new pinning mechanism.** `save_translation` has read
  `:resourcePolicies` for `document_id`, `garden_id`, `source_lang`,
  `target_lang` and `project` since long before this. What was missing was one
  hop: `:actions/start-agent-session` never forwarded resource policies. It does
  now, and only for a trigger that opts in with `:resource-policies-from-event`.
- **No new identity scheme.** The run id is carried in the session's resource
  policies, not imposed on the runtime's own session ids.

### What a live run found (2026-08-26)

The composition was tested against a local instance on the deployed contract set.
Five things were broken between "the gate derives work" and "bytes reach the
site". Every one of them failed **silently** — the dispatch surface answered 200
and reported `{:considered 5 :admissible 5 :dispatched []}`, which reads exactly
like nothing needed translating.

| # | Break | Where |
|---|---|---|
| 1 | `referenced-documents` re-extracted the id from a sequence of ids, so it returned `[]` every time. No document → no source revision → every intent short-circuits on `:publication-revision-unresolved`. **Both runners were dead.** | `infra/routes/translation_dispatch.cljs`, from #253 |
| 2 | `translation-params` required `document_id`, which the prompt tells the agent to omit because the session is pinned. The schema validator refused before the handler ran. | `infra/openplanner/tools.cljs` |
| 3 | `auth-context-for-agent-turn` never carried `:resource-policies` onto the turn's auth context, so the pin reached telemetry and stopped. `save_translation` fell through to the OpenPlanner segment path. | `infra/agent/turn.cljs` |
| 4 | That fall-through then called `translation-org-id!`, which negotiates a tenant between a *request context* and a policy — and a triggered session has none (`org_id`, `user_email`, `membership_id` all nil). | `infra/openplanner/tools.cljs` |
| 5 | `:tools/allowed` was inert on agent contracts — read only from `:policy` contracts. So `:role/translator`'s `:cap/semantic` kept handing the session `graph_query`, one of the ten operations still delegated to **OpenPlanner REST**. | `domain/contracts/resolve.cljs` |

After all five, the full chain ran: four locales dispatched, four agent sessions,
four correct translations submitted, four revision-specific receipts, one
approved, and `/de/garden/` materialized from the agent's German while the other
three stayed `translation-review-required`.

### The residual OpenPlanner surface

Worth stating plainly, because `KNOXX_OPENPLANNER_CLIENT_MODE=mongo` reads like a
removal and is not one. `infra.clients.openplanner/client` builds the direct-Mongo
record **wrapping a REST client**, and `infra.clients.openplanner-mongo` delegates
ten operations to it:

`graph-memory!` · `graph-export!` · `upsert-document!` · `documents-stats!` ·
`graph-monitoring!` · `build-semantic-edges!` · `record-labels!` ·
`record-reaction!` · `v1-json!` · `forward-v1!`

The deployed compose sets neither `OPENPLANNER_BASE_URL` nor
`OPENPLANNER_API_KEY`, so in production each of those raises "OpenPlanner is not
configured" rather than reaching a service. **A local instance that inherits
those variables from the invoking shell does not behave like production** — it
finds a live stack and fails slowly instead. `scripts/` and any local runner
should unset both.

### One thing it did strengthen

The source document travels *on the event*, embedded in the brief. The worker is
handed a document id and fetches its own input, so
`law.translation-dispatch/source-drift-refusal` can only compare repository
digests before and after and can never establish what was actually translated —
a limitation recorded on `knoxx-translation-work-dispatch`. Handed the bytes the
claim was taken for, an agent translates the dispatched revision by
construction, and the drift check becomes a redundant second one rather than the
only one.

The cost is a bound: `law.translation-agent/max-brief-chars`. An oversize
document is **refused at emit time, never truncated** — a truncated source would
be translated in full by an agent with no way to know it read a fragment, and
the receipt would then attest that the whole revision was translated.

---

## Known gap, surfaced deliberately

`law/publication_surface.cljs:74` declares `retired-authority-paths` as paths
that "must have NO shipped caller". `/api/openplanner/v1/gardens` still has
four callers:

```
frontend/src/cljs/knoxx/frontend/pages/gardens/api.cljs:16,28
frontend/src/cljs/knoxx/frontend/pages/gardens/logic.cljs:59,67
```

The backend test that guards this claim
(`publication_surface_verify_test.cljs:139`) walks an **explicit eight-file
list** that does not include them, so it passes while the callers remain. This
is not a regression introduced by the epic — the Gardens page was always outside
its CMS/translation scope — but the contract's wording overclaims relative to
what the test checks.

The verify script reports this as `WARN` in §9 rather than failing, so the run
stays green while the gap stays visible. Either the docstring should be narrowed
to the CMS/translation surfaces, or the Gardens page should be migrated and the
guard made repo-wide.

---

## Deployment side

`open-hax/services` still carries `KNOXX_EXPECT_OPENPLANNER_REST` in
`deploy-stack.yml`, `digitalocean/services/knoxx/env.template`, and a
conditional CMS skip in `digitalocean/services/knoxx/verify.sh:123`. That flag
lets a deploy skip the CMS check when no OpenPlanner answers — precisely the
condition the new surfaces are built to work under. Retiring it is a follow-up
PR against that repo, not this one.

---

## If something fails

| Symptom | Likely cause |
|---|---|
| `cannot verify against a backend that is not running this code` | The live backend serves a different checkout — see "Before you start" |
| Everything in §1 returns 200 | `KNOXX_API_KEY` unset on the backend, so auth is not being enforced at all |
| §5/§6 return 200 instead of 409 | The facade is reading deduped/filtered records again — the exact regression those sections exist to catch |
| `fixture directory already exists` | A previous run was killed; `rm -rf contracts/_verify` and retry |
| `agent-browser cannot launch a browser` | `agent-browser install --with-deps` |
| §8b: `a trigger subscribes to publication/translation-needed` fails | `contracts/namespaces/publication.edn` is absent from the contract root the live backend loaded — in production that root is `open-hax/services`' `contracts/knoxx`, not this repo's `contracts/` |
| §8b: the trigger resolves but the agent does not | the agent contract is present but its actor, role or capability does not resolve; `GET /api/knoxx/agents/catalog` lists what did |
| A translation session runs but every `save_translation` is refused | the trigger is missing `:resource-policies-from-event`, so the session started unpinned and the sink has no claim to join |
