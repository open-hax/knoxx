# Testing MCP tools locally

Knoxx exposes its agent tools over MCP at `POST /mcp`, behind OAuth. That is
correct for production and expensive for development: verifying that one tool
still worked meant registering a client, walking a browser through the consent
page, exchanging a code — or asking a deployed connector to try the tool and
reading what it said back. Neither loop is fast, and neither can run in CI.

Two things replace it: an **authentication contract** that says which methods a
surface accepts, and a **CLJS e2e suite** that boots a real server and drives
every tool through it.

## The authentication contract

`contracts/authentication/mcp_http.edn` is the whole answer to "who may call
`/mcp`". Adding a method there is a reviewable change to a file; the thing it
replaced was an environment variable, invisible to anyone reading the repo and
impossible to assert on.

```clojure
{:contract/kind :authentication
 :contract/id "mcp_http"
 :auth/surface :mcp
 :auth/methods
 [{:auth-method/id :oauth-bearer      :auth-method/enabled true}
  {:auth-method/id :trusted-loopback  :auth-method/enabled false
   :auth-method/require-loopback true
   :auth-method/require-non-production true
   :auth-method/token-env "KNOXX_MCP_LOOPBACK_TOKEN"
   :auth-method/min-token-length 8
   :auth-method/grants {:grant/user-email "system-admin@open-hax.local"
                        :grant/tools :all}}]}
```

Everything fails closed. A missing contract, a method that is not
`:auth-method/enabled`, an enabled method that grants no identity, a guard the
request does not satisfy — each is a refusal.

### `:trusted-loopback`

A caller on this machine holding the secret named by `:auth-method/token-env`
reaches the tool surface with no OAuth round trip.

**The secret is the off switch.** Read `:auth-method/enabled true` as "this
deployment may use this method", not "this method is live". `law.auth-methods`
refuses unless a token of at least `:min-token-length` is configured in the
named variable, so an image deployed without `KNOXX_MCP_LOOPBACK_TOKEN` has no
second way in. `shipped-loopback-is-inert-without-a-secret-test` holds that,
including the empty-string case an unset variable reads as.

`:require-non-production` is `false` on the shipped contract, unlike the e2e
one — the post-deploy gate has to verify *production*. `:require-loopback` is
then the only guard, and it is load-bearing:
`shipped-loopback-refuses-off-box-callers-test` asserts a caller at `10.0.0.4`
holding the correct production secret is still refused.

It is not an authorization bypass. `:grant/tools :all` is intersected with what
the resolved membership can actually reach, so a tool that user's roles deny
stays denied — `grant-is-not-authorization-test` holds that line.

`:grant/actor-id` decides which actor calls run as, and therefore which Discord
or Bluesky credentials they resolve. A method that names none produces a token
carrying no actor, and credential-backed tools then fail for want of one rather
than borrowing whatever actor the membership happens to hold.

### Where the code lives

| Concern | Namespace |
| --- | --- |
| The rule — pure, no env, no sockets | `law.auth-methods` |
| Reading the contract and the environment | `infra.auth.method-config` |
| The schema | `law.contracts/AuthenticationContract` |
| Consulting it | `infra.routes.mcp/resolve-post-token-record!` |

Nothing downstream of `resolve-post-token-record!` knows which method admitted
a request. That is deliberate: the e2e suite exercises the shipped serving path
rather than a parallel one that could pass while production fails.

### Driving it by hand

Enable `:trusted-loopback` in the contract, set the token, and any MCP client
works — including Claude Code:

```bash
KNOXX_MCP_LOOPBACK_TOKEN=some-long-local-token pnpm -C backend start:dev

claude mcp add --transport http knoxx-dev http://127.0.0.1:8000/mcp \
  --header "Authorization: Bearer ${KNOXX_MCP_LOOPBACK_TOKEN}"
```

The backend logs a warning at startup whenever a surface accepts anything but
OAuth. A silently open authentication method is a bug even when every guard
around it holds.

## The e2e suite

```bash
pnpm -C backend test:e2e        # shadow-cljs compile e2e
```

Namespaces live in `backend/test/e2e/knoxx/backend/e2e/` and end in `-e2e`
rather than `-test`, so the unit builds — whose pattern is `-test$` — cannot
pick them up. A suite that opens sockets should be something you choose to run,
and the filename should say so.

| File | What it covers |
| --- | --- |
| `harness.cljs` | Boots a real Fastify app on an ephemeral port; supplies the policy context, contract root and fetch |
| `mcp_client.cljs` | A minimal Streamable HTTP client; parses SSE, keeps wire payloads |
| `tool_fixtures.cljs` | Sample args per tool, plus recorded reasons for absence |
| `mcp_auth_e2e.cljs` | Who gets in, who does not, grant vs. authorization |
| `mcp_tools_e2e.cljs` | The catalog, the sweep, credential wiring, coverage |
| `sandbox_e2e.cljs` | The sandbox lifecycle against real docker |
| `nrepl_e2e.cljs` | `nrepl.eval` against a real socket speaking real bencode |
| `discord_identity_e2e.cljs` | Which bot a Discord call goes out as |
| `nrepl_double.cljs` | An independent bencode nREPL stand-in |
| `discord_double.cljs` | A recording gateway manager |

### The three tools the sweep cannot cover

A fixture sweep proves a tool accepts arguments and returns. For these it
would prove almost nothing, so each has its own namespace.

**Sandbox** (`sandbox_e2e.cljs`) runs the real chain — create → status →
write → read → exec → commit → destroy — threading the `sandbox_id` each call
returns, against a real docker daemon. Calling the tools individually only ever
proves they reject a missing id. It asserts a written file reads back byte for
byte, that `exec` sees that file, and that `exec` really is inside a container
(the hostname it reports is not this host's). `destroy` runs in a `finally`, so
a failing test leaves no TTL-bound container behind.

Skipped with a loud log when no docker daemon answers, and hard-failed when
`KNOXX_E2E_REQUIRE_DOCKER=true` — set that where docker is guaranteed, so a
silently skipped sandbox test can never read as a passing one.

**nREPL** (`nrepl_e2e.cljs`) is the highest-risk tool on the surface —
arbitrary evaluation in the live runtime — and it hand-rolls the bencode
protocol. The harness starts a real TCP server speaking bencode on an ephemeral
port. The double is a deliberately **independent** bencode implementation:
sharing the tool's own codec would make the test tautological, since a framing
bug would encode and decode symmetrically and pass. It asserts the clone comes
before the eval, that the eval reuses the cloned session, that `target=cljs`
routes through `shadow.cljs.devtools.api` carrying the build id and namespace,
and that `target=clj` evaluates the caller's code verbatim instead.

**Discord** (`discord_identity_e2e.cljs`) answers the question "Gateway not
started" never could: does a call go out *as the actor the token was issued
for*? Knoxx reaches Discord two ways, and they resolve identity differently —
which is the finding this file exists to pin down:

- **REST** (`discord.list.servers` and friends) is actor-scoped. The test
  asserts the outbound request hits `/users/@me/guilds` carrying
  `Authorization: Bot <the seeded actor's token>`, that no other identity
  reached Discord, and that the same tool under an actor owning no credential
  is refused *before* any request goes out.
- **The gateway** (`discord.voice.*`) is not. It calls `(dg/gateway-manager)`
  with no arguments — the process-wide default — so a voice call speaks as
  whichever bot that manager was started with, regardless of the token's actor.
  Asserted as it stands, three ways, so the claim cannot rot quietly.

### What the harness substitutes, and what it does not

Everything it replaces is a seam production already has. The Fastify app, the
route registration, the MCP transport and the tool factories are the shipped
ones. Three things are supplied:

- **The policy context** — `:resolve-context!` returns a seeded system-admin
  membership; `:get-actor-credential!` returns seeded Bluesky and Discord
  credentials for actor `e2e_actor`. No Mongo.
- **The contract root** — `test/e2e/fixtures/contracts`, where
  `:trusted-loopback` is on.
- **`fetch`** — outbound calls are recorded and answered with an empty JSON
  `Response`; calls to the harness's own port pass through.

That credential seam is new. `policy/get-actor-credential!` used to ignore its
policy-context and call Mongo directly, which is why every Discord and Bluesky
tool — most of the surface — had no test at all. It now dispatches through the
context when one carries `:get-actor-credential!`, the same seam
`:resolve-context!` and `:query!` already used, and which
`mongo-policy-actor-credentials` documents as the correct dispatch point.

### What the sweep asserts

1. **The catalog is well formed.** Legal names, no duplicates, an object
   `inputSchema`, a description. A tool that arrives with no schema is present
   and unusable, and nothing logs an error when that happens.
2. **Every fixture-covered tool accepts its arguments and runs.** A JSON-RPC
   error fails the suite — the server refused or threw. A *tool-level* error
   does not: the tool ran and reported a problem, which is a legitimate answer
   when the thing it needs is not configured here.
3. **A credential-backed tool reaches the wire with the seeded credential.**
   `bluesky_profile` must resolve `e2e_actor`'s credential and put it on an
   outbound request.
4. **Coverage does not collapse**, no fixture names a tool that vanished, and
   no recorded absence is still claimed once the tool appears.

Writes are skipped. `tool_fixtures.cljs` marks each tool `:args`, `:needs` (a
live handle no fixture can invent) or `:absent` (with the reason it is off the
MCP surface at all).

### Annotation ratchet

73 of the 82 served tools declare no MCP annotations. MCP's defaults are
pessimistic when a tool says nothing — `destructiveHint` and `openWorldHint`
default to true — so every one of them is presented to a user as a destructive,
open-world write regardless of what it does.

`unannotated-baseline` in `mcp_tools_e2e.cljs` holds that number. It must never
rise; lower it as entries are added to `law.mcp-tool-annotations`, which
deliberately refuses to guess an annotation from a tool's name.

## The deploy gate

`services/knoxx/verify.sh` in the DigitalOcean deployment repo gains an MCP
section. A healthy backend with a broken tool surface is a real and previously
undetectable failure — a schema conversion producing nothing callable, a tool
vanishing from the catalog, an actor credential that no longer resolves — and
none of it moves `/health`.

The gate runs `probe-mcp.js` inside the backend container with
`docker compose exec`, so it is on `127.0.0.1`, which `:require-loopback`
permits and nothing off-box can reach. The backend port is not published to the
host.

It fails the deploy on: an unauthorized or unavailable surface, a catalog under
`KNOXX_MCP_MIN_TOOLS` (default 20), any degraded tool, duplicate tool names, a
probed tool missing from the catalog, or any `rpc-error`. A **tool-error** is
reported and not fatal — the tool ran and said its dependency is not configured
on that host, which is a legitimate answer.

Three read-only tools are probed by default, each proving a different
subsystem end to end: `semantic_query` the corpus data plane, `events_status`
the events runtime, and `discord_list_servers` that the `deploy_verifier`
actor's credential still resolves. No writes — a deploy gate must not publish
anything.

The section skips itself when `KNOXX_MCP_LOOPBACK_TOKEN` is unset, unless
`KNOXX_EXPECT_MCP_VERIFY=true`, which turns a missing secret into a failure so
the only check covering the tool catalog cannot be silently lost.

`probe-mcp.js` carries a `PROBE_SELFTEST=1` classifier matrix that
`code-quality.yml` runs on every PR, following `probe-openplanner.js`. It
covers the two mistakes that ship green: reading a `text/event-stream` body as
JSON (a working surface looks empty) and treating a 200 as a pass (MCP reports
a tool's own failure as a *successful* result carrying `isError`).

## Known gaps the first run surfaced

Recorded as `:absent` fixtures, with reasons, rather than as failures:

- **`contract.*`** ship only in the contract-librarian tool suite, so they are
  not on the MCP surface at all.
- **`openplanner.*`** (the seven epistemic-kernel tools) are named in
  `infra/registry/tools.cljs` but built by no factory.
- **`email.send`** is an HTTP route; there is no MCP tool.
- **`mcp.shoedelussy.*`** are bridged from an external MCP server and appear
  only when `MCP_SERVERS` names it.
- Allowing `semantic_query` on a membership also exposes `graph_query`: one
  factory is authorized by either id.
