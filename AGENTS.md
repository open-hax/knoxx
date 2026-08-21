# Knoxx Agent Style Guide

> **Roadmap:** [`ROADMAP.md`](ROADMAP.md) — this repo's slice. The hub, with the
> seam, ownership table and sequencing rule, is [eta-mu/ROADMAP.md](https://github.com/open-hax/eta-mu/blob/main/ROADMAP.md).

# Language and Repository Ownership
- Knoxx backend, domain, persistence, and service features are ClojureScript-first. Do not add or expand TypeScript for Knoxx backend work unless the user explicitly authorizes it.
- Do not create coordinated implementation changes in OpenPlanner to deliver a Knoxx feature unless the user explicitly requests a cross-repository change.
- Prefer an existing stable boundary or a Knoxx-owned ClojureScript adapter over a companion pull request. One feature should normally be completed in one repository.
- TypeScript remains acceptable only in existing frontend surfaces or established JavaScript/TypeScript boundaries that are not being expanded into new backend domain logic.

# Architecture Paradigm: Categories vs. Contracts
When modeling domains, you must strictly differentiate between the grammar of motion and the enforcement of that motion.
- Categories: Describe the space of lawful possible transformations. They dictate "what kind of move this is" and define the state space, transition vocabulary, and general laws of composition for the runtime or a subsystem.
- Contracts: Decide whether a particular runtime entity, event, or transition is admissible under current obligations. They dictate "whether you are allowed to count it as a valid move right now" by defining guards, admissibility checks, evidence requirements, delivery expectations, and side-effect constraints.

# House Rules: The Constitution (`eta-mu-sol` style)
- Zero Warnings: Zero warnings in CI for `clj-kondo`, type checking, and tests. Warnings are failed contracts, not noise.
- Warning Ratchet: Do not allow warnings to accumulate. For every backend CLJS change, run `pnpm -C backend typecheck`; fix any warning introduced or touched by the change before moving on. If an unrelated historical warning blocks verification, record it explicitly with the owning file and do not add to the baseline.
- Linter Enforcement: Turn on optional `clj-kondo` linters for `:missing-docstring`, `:unused-value`, `:shadowed-var`, `:used-underscored-binding`, `:warn-on-reflection`, `:unsorted-required-namespaces`, and `:refer`. Ban broad `:refer :all`.
- No Junk Drawers: Do not use `utils` namespaces.
- Architecture Split: Honor a strict four-category namespace architecture:
  1. `domain.*`: Pure business logic, typed data, domain-level decisions. No I/O.
  2. `infra.*` or `http/db/queue/*`: Effectful functor layer (transport, persistence). No domain policy.
  3. `shape.*`: Structure-only morphisms over data shapes. Must be pure and domain-agnostic.
  4. `law.*` or `contract/guard/*`: Contracts, validators (e.g., Malli), assertions. No I/O.
- Boundary Contracts: Every boundary-crossing function must name or call an explicit contract/schema validator (e.g., Malli) reflecting the Contract obligations of the relevant Category.
- Extern Boundary Layer: `knoxx.backend.extern.*` is the only place raw JavaScript interop should be born, decoded, encoded, sequenced, or mutated. Non-extern namespaces must not import `knoxx.backend.extern.js` or `knoxx.backend.extern.json`; those generic helpers are implementation details for named extern adapters only.
  - Use boundary-specific adapters named for the system they own: `extern.fastify`, `extern.fetch`, `extern.multipart`, `extern.websocket`, `extern.discord`, `extern.tools`, `extern.row-extra`, `extern.extension`, etc.
  - Domain, law, shape, and ordinary infra code should receive and return CLJS maps/vectors/scalars. If a raw JS object must cross a layer, it must be treated as an opaque handle and only inspected by the owning extern adapter.
  - Do not hide boundary leaks with aliases. A call site using `js->clj`, `clj->js`, `js/JSON`, `Object.keys`, `array-seq`, `#js`, `aget`, `aset`, `js/Promise.all`, `FormData`, `Blob`, Fastify request/reply internals, WebSocket methods, or SDK-native objects is probably in the wrong namespace unless the file is an extern adapter or explicitly documented low-level wrapper.
  - Fetch/HTTP clients pass CLJS request data such as `:json`, `:body`, `:headers`, and let `extern.fetch` build native `RequestInit` and encode/decode JSON. Persistence code uses data-specific codecs such as `extern.row-extra` rather than generic JSON parsing at the store call site.
  - Before adding a new extern namespace, name the real boundary it owns, keep the public API CLJS-first, add a small regression test for the conversion, and update the boundary inventory/gate if applicable.
- Custom Macros: Macros must expand to ordinary, lintable shapes. Register custom macros in `.clj-kondo/config.edn` using `:lint-as` (e.g., `clj-kondo.lint-as/def-catch-all`) on day one. Do not invent a shadow legal system.

# Coding Directives & Clean Code Doctrine
- Optimize for the human reader's working memory: reveal intent through explicit naming, isolate responsibilities, and arrest entropy on contact.
- Continuous Truth (XP): Tighten the loop. Favor small, verifiable state changes over speculative architecture.
- Modern Asynchrony: ClojureScript 1.12.145+ supports native async/await. Always use the `^:async` metadata hint for functions and tests instead of legacy `core.async` channels, Promise chains, or shadow-cljs specific wrappers when targeting modern environments. 
  - Functions: `(defn ^:async foo [n] ... (await (Promise/resolve ...)))`
  - Tests: `(deftest ^:async foo-test ... (await (foo ...)))`
- Clojure Idioms:
  - Use `when-let` instead of nesting `let` and `if` checks.
  - Strongly prefer threading macros (`->` and `->>`) over manual nested let forms to maintain linear readability.

## Data-Oriented Design

- Pass plain maps. Return plain maps.
- Tool execute functions receive a parameter map and return a result map.
- Avoid OO-style stateful tool builders. A tool is data: `{:name ... :description ... :parameters ... :execute fn}`.
- Composition happens in the orchestration layer (`agent-hydration`) by concatenating domain tool vectors.

## Runtime Operations

- Do not restart Knoxx PM2 processes unless the user explicitly asks for a restart.
- Prefer source edits and let shadow-cljs hot reload backend CLJS changes; Vite will reload frontend changes automatically.
- If a restart seems necessary, report why and wait for the user to restart or approve it.

## Verification Requirements

- Do not report a code change as done unless the relevant test command completes with zero failures and zero errors.
- For backend ClojureScript changes, run `pnpm -C backend exec shadow-cljs compile test` and treat any reported test failure as blocking, even if the compiler exits 0.
- For production backend changes, also run `pnpm -C backend exec shadow-cljs compile server` or the narrower build command that proves the changed build target.
- If the full relevant suite is already red, either fix it before claiming completion or clearly state that the task is blocked by the failing tests; do not phrase a red suite as “verified” or “done.”
- Only use a narrower test command when it directly covers the changed code and explain why the full relevant suite was not run.

## Human Verification Artifact

A green suite tells the reviewer that the code agrees with itself. It does not
let them *see the feature work*. Every epic, and every PR that adds or changes a
user-reachable surface, must therefore ship a way for a human to run it against
a live Knoxx and watch what happens.

This is not optional and it is not a nice-to-have. Reviewing eleven stacked
diffs without ever running the thing is how a reviewer ends up approving
assertions rather than behavior.

**What to ship.** A runnable script in `scripts/`, plus a page in
`docs/verification/` that explains it. Use
`scripts/verify-publication-epic.sh` and `docs/verification/publication-epic.md`
as the reference pair.

The script must:

- **Seed and tear down its own data.** Never require the reviewer to hand-craft
  fixtures, and never leave anything behind. Write only inside a dedicated
  directory and remove it from a `trap ... EXIT INT TERM`, so a killed run is
  still clean.
- **Verify its own preconditions first.** Especially *which checkout the running
  process is serving* — the PM2 processes on this machine point at a different
  working copy, and verifying against that verifies the wrong code. Fail loudly
  and early rather than producing meaningless passes.
- **Say what each check proves, not just that it passed.** `PASS  observed =
  null — nothing has been materialized, and the wire says so` is a verification
  artifact. `PASS  test_observed_null` is not.
- **Walk the failure modes, not only the happy path.** Every surface gets an
  unauthenticated request. Every invariant that a review finding was about gets
  a check that would have caught it. If a bug was fixed because a resource could
  silently vanish, seed a resource that vanishes.
- **Exit non-zero on any failure**, and print a summary of what failed.

For anything with a UI, also ship a browser tour that captures screenshots at
each step (`scripts/verify-publication-tour.sh` is the reference). Screenshots
go in `docs/verification/screenshots/` and are gitignored — they are regenerated
per run, not reviewed as diffs.

**Be honest about what the tour skips.** If a control cannot be driven because
it is still coupled to something absent or retired, say so in the script header
and in the doc, with the reason. A tour that quietly avoids the hard part is
worse than no tour.

**Known gaps are `WARN`, not silent and not `FAIL`.** If the work leaves
something incomplete outside its own scope, print it every run so it stays
visible, but do not fail the run for it — a permanently red verification script
stops being read.

## Author's Walkthrough on Your Own PRs

Before requesting review, walk your own diff as inline PR comments. The practice
has a name: IEEE 1028 calls an author-led review a **walkthrough**, as distinct
from an *inspection*, where a moderator drives and the author stays quiet. Here
the author drives.

You are not summarizing the diff — the reviewer can read it. You are supplying
what the diff cannot show:

- **Point at the acceptance criterion.** Which line of which card does this
  satisfy, and where is that satisfied in the code.
- **Name the decision and the alternative you rejected.** "This reads the
  undeduped record list on purpose; deduping first makes the branch below
  unreachable."
- **Flag the risk you are carrying.** Anything you are unsure about, anything
  that will bite later, anything you would want to know if you were reviewing
  it cold.
- **Explain anything that looks wrong but isn't.** If a reviewer's first
  reaction will be "that's a bug", get there first.
- **Say what a card premise got wrong.** Cards are written before the code
  exists. When a premise turns out to be stale, annotate it rather than
  silently implementing something else.

Comment on the confusing parts, not every hunk. A walkthrough with six good
anchors beats one with forty.

## Modern CLJS Patterns

Always prefer modern shadow-cljs patterns over legacy verbose forms:

- Use `^:async` + `await` for async tests and top-level async functions (ClojureScript 1.12.145+)
- Use `when-let` instead of nesting `let` + `if` checks
- Prefer threading macros `->` and `->>` over manual nested let forms
- Use `some->` for optional chaining through potential nils
