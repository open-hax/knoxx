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
connects live Discord actor gateways from the shared policy DB. Set
`KNOXX_DISABLE_EVENT_RUNTIMES=true` and none of that happens; the process prints
a banner at boot and warns every 60s so it cannot silently sit in that mode.

```bash
KNOXX_DISABLE_EVENT_RUNTIMES=true PORT=8000 node backend/dist/server.js
```

That flag is a stopgap. The real decoupling is carded as
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
