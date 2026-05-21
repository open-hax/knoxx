# Knoxx Agent Style Guide

## Vertical Domain-Driven Slices

We organize agent tooling into **vertical domain-driven slices**, not horizontal layers.

Prefer:
- `knoxx.backend.tools.discord` — everything Discord (API wrappers, tool factories, formatting)
- `knoxx.backend.tools.music` — everything music/audio identification
- `knoxx.backend.tools.openplanner` — graph, memory, websearch, translation
- `knoxx.backend.tools.contracts` — contract librarian read/write tooling

Over:
- ~utils.cljs~ with scattered helpers
- ~agent_hydration.cljs~ as a 45k-token god namespace

### Why
- A domain can be understood, tested, and replaced in isolation.
- Tool factories live next to the private functions that power them.
- Shared infrastructure (media loading, path resolution, TypeBox helpers) is extracted explicitly into `tools.shared` and `tools.media`, not copy-pasted.

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

## Modern CLJS Patterns

Always prefer modern shadow-cljs patterns over legacy verbose forms:

- Use `^:async` + `await` for async tests and top-level async functions (ClojureScript 1.12.145+)
- Use `when-let` instead of nesting `let` + `if` checks
- Prefer threading macros `->` and `->>` over manual nested let forms
- Use `some->` for optional chaining through potential nils


### Why ^:async / await in deftest and defn

ClojureScript 1.12.145 supports `^:async` on `deftest` and `defn`, emitting native JS async functions. Use `await` (not `js-await`) inside them:

```cljs
;; Instead of this (breaks on rejected promises):
(deftest my-async-test
  (async done
    (-> (some-async-fn)
        (.then (fn [v] (is ...) (done)))
        (.catch (fn [e] (is false) (done))))))

;; Prefer this:
(deftest ^:async my-async-test
  (try
    (let [v (await (some-async-fn))]
      (is (= 42 v)))
    (catch :default e
      (is false (str "threw: " (.-message e))))))
```

```cljs
(defn ^:async my-async-func []
    (await (some-async-func)))

```

**Rules:**
- `await` only works inside `^:async` functions
- Rejected promises throw synchronously inside `await`; catch them with `(catch :default e ...)`
- Never return a rejected promise disguised as a success map — errors must remain errors
- `js-await` and `await` are not interchangeable: `js-await` is a macro for `let` binding; `await` is a special form inside `^:async`
