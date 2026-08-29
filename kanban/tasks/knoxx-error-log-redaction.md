---
uuid: "knoxx-error-log-redaction"
title: "Backend error logs print raw ex-data, including credentials and paths"
status: incoming
priority: P2
labels: ["tasks", "3sp", "security", "observability", "correctness"]
created_at: "2026-08-19T00:00:00Z"
points: 3
category: tasks
---

# Backend error logs print raw ex-data, including credentials and resolved paths

## The gap

Twenty-seven `js/console.error` call sites and `app-log-error!` in
`infra/core.cljs` log error objects and `ex-data` maps directly. Those maps
routinely carry a resolved filesystem path, and can carry a connection string
with a password in it.

This is separate from the HTTP response, which was fixed on #230:
`law.error-body/error-body` now withholds both the message and the ex-data for
any status it did not classify, so an unclassified failure reaches the caller
as `{:detail "internal error"}`. The log is the other half, and it is
untouched.

It matters more here than in a typical backend because this process ships
notifications outward: Discord actor gateways are connected from the shared
policy DB, and repository events already fan out to Discord via
`.github/workflows/*-discord.yml`. A log line is not reliably a private artifact.

## Why this was not fixed on #230

The three publication-surface adapters now log through
`extern.fastify/log-unclassified-failure!`, which prints the message and the
ex-data **keys** rather than the map. That is a real reduction and it is stated
in one place, but it is deliberately not the full remedy CodeRabbit asked for
(an allow-list of non-secret fields plus a correlation id for restricted
detail), because:

- Applying it to three adapters while the other twenty-seven call sites print
  raw errors buys very little and makes the codebase inconsistent about what a
  log may contain — the next person copies whichever one they saw first.
- A correlation id is only useful with somewhere to correlate *to*. That is a
  logging-transport decision, not an adapter detail.

## Outcome

One rule about what a backend log may contain, applied everywhere, with the
detail an operator needs still reachable.

## Scope

- Decide the redaction rule: an allow-list of loggable ex-data keys, or a
  denial-list of secret-shaped ones. Prefer the allow-list — a denial-list fails
  open, which is the defect class this epic hit four times.
- A single helper every error path uses, in the layer where logging is already
  allowed. `extern.fastify/log-unclassified-failure!` is the seed.
- A correlation id on the response and in the log, so an operator can find the
  full detail without it being printed. Decide where the full detail lives.
- Migrate the twenty-seven `js/console.error` sites and `app-log-error!`.
- Redact known secret-bearing shapes specifically: `:mongo-uri`, API keys,
  `Authorization` headers, session cookies.

## Non-goals

- Changing what the HTTP response contains. That is settled on #230 and the
  contract is `law.error-body`.
- A logging framework. The gap is a redaction rule, not a transport.

## Acceptance criteria

- No backend log statement prints an `ex-data` map or an error object whose
  values have not passed the rule.
- An operator can still diagnose an unclassified 500: the log names the
  surface, the message or a correlation id, and which context keys attached.
- A test seeds an error carrying a connection string and asserts it appears in no
  log line.
- The rule is stated once, and a new error path that ignores it is visible in
  review rather than silent.
