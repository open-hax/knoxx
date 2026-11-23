# Verify Knoxx MCP run controls

This change covers an MCP client's event dispatch, one-off spawn, exact-run
status, and message delivery. Knoxx admits each operation under the
authorized membership and stored actor. The supported live model routes in
this verification are `openai/gpt-6.1-sol` through Proxx and
`claude-code/haiku` through Claude Code.

## Verify a checkout

Run `scripts/verify-mcp-run-controls.sh` from the checkout with its backend
dependencies installed. The suite starts Fastify on ephemeral loopback
using compiled checkout sources and removes its temporary Clio data. It
supplies fixture OAuth records and provider sessions. It checks the
following.

- Anonymous requests, a missing tool grant, and missing run-read permission
  are refused.
- A granted event reaches its listener with the caller actor, event type,
  generator kind, and payload intact. An actor event reaches its fixture
  recipient.
- The returned run, session, and conversation IDs identify a live turn.
  An exact steer or follow-up reaches that turn. A foreign, stale, or
  replaced target receives no message.
- Run status reads the durable record and reports up to 20 event headers.
  Completed status and provider usage come from the persisted settlement.
- The local password form preserves the OAuth request and rejects
  cross-origin form submissions. A fresh login returns to the consent page.

Run `CHROME_BIN=/path/to/chromium scripts/verify-mcp-local-login-tour.sh`
to capture the password form and actor consent page from the same isolated
fixture. The script prints the screenshot directory.

Use `scripts/verify-mcp-subscription.sh` for the optional real Claude Code
canary. It needs `claude` on the runtime path and a signed-in, backend-owned
`KNOXX_CLAUDE_HOME` with mode 0700 and no symlink path. The canary uses a
fixture MCP grant and temporary Clio data. A real connector login and
Proxx account verification are described below.

## Verify a deployed instance

Confirm the MCP protected-resource metadata and advertised authorization
server match the deployment URL. An anonymous MCP POST should return
401. Use that deployment's configured login method, then grant a dedicated
test actor `agents_spawn`, `agents_status`, and `actors_send-message`. Its
role needs chat access, own-run viewing, and live steering. Use a separate
event grant for `events_status` and `events_dispatch` when qualifying event
delivery. Dispatch only to a dedicated fixture listener.

GPT uses the deployment's Proxx configuration. Before a real GPT test,
check the deployment's Proxx model catalog for `openai/gpt-6.1-sol` and
verify that its intended, isolated provider account is available. After the
run, match its provider and usage to Proxx's request record.

A deployment with a shared Proxx service can bind this model's contract to
the dedicated provider `proxx-subscription`. Set
`KNOXX_PROVIDER_BASE_URLS=proxx-subscription=<private-relay-origin>` and
`KNOXX_PROVIDER_AUTH_TOKENS=proxx-subscription=<relay-bearer-env-name>`.
The bearer must come from protected deployment configuration. With this
binding, the same verified MCP membership and actor required for Claude
controls access to the relay. Only its admitted sessions load the relay
credential. The relay should accept only the chosen subscription model
and pin the intended Proxx account. The local MCP suite checks refusal
before enqueue for another membership and for missing relay configuration,
plus admission for the enrolled actor. Qualify real relay availability and
usage on the deployed endpoint.

This admission check applies to Knoxx requests. Run subscription workers
and keep their credentials in an execution boundary that ordinary agent
file and shell tools cannot enter. An actor policy or a separate model
registry does not isolate files or environment variables from a tool
running as the same OS user.

For Claude, configure `KNOXX_NATIVE_SUBSCRIPTION_ORG_ID`,
`KNOXX_NATIVE_SUBSCRIPTION_MEMBERSHIP_ID`, and
`KNOXX_NATIVE_SUBSCRIPTION_ACTOR_ID` to bind one deployed Knoxx membership
and actor. Install a pinned Claude Code executable for the backend runtime
and mount a host-local `KNOXX_CLAUDE_HOME` owned by the backend OS user.
Use a dedicated subscription profile. Keep account and provider
credentials in the deployment's approved local configuration.

1. Call `agents_spawn` with the exact provider model ID and a harmless
   prompt that allows time to steer.
2. Poll `agents_status` with the returned `run_id` until it reports
   `running` and `can_steer: true`.
3. Send a new, unique reply marker with `actors_send-message`, using
   `mode: "steer"`, `target: "session:<session_id>"`, and the exact
   `run_id`, `session_id`, and `conversation_id`.
4. Poll the same run to `completed`. Verify that the run record contains
   the marker sent only through steer, a queued steering event, and
   positive provider usage. Check that the returned authorization binds
   the intended test actor. Revoke a purpose-created test grant when done.

## Stage through Services

Use the existing [environment promotion controller](https://github.com/open-hax/services/blob/main/docs/environments/promotion.md).
An authorized premerge testing lease uses the exact PR head. The staging
workflow uses the exact reviewed merge commit on Knoxx main. Before
running the deployed verification, compare the receiver's release
receipt and running image IDs with the selected revision. Reconcile the
stage receiver key and verify its private ingress listener before relying
on public route tests. A hosted canary also needs the dedicated provider
configuration above and an explicitly isolated event listener for event
delivery. Do not reuse production deployment state or the workstation's
private profile in staging. Capture route, authorization, provider, and
exact-run evidence on that deployed revision.
