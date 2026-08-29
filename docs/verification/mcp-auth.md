# Verifying the live MCP authentication boundary

This walkthrough exercises the shipped `POST /mcp` authentication contract
against a live Knoxx process. It proves four things a green unit suite cannot:

1. the listener is running from the checkout under review;
2. an unauthenticated request and an unknown bearer are refused;
3. the configured shared secret crosses the production loopback gate; and
4. the resulting principal can initialize MCP and receive a non-empty,
   duplicate-free tool catalog.

## Preconditions

Start Knoxx from this checkout with the shipped contract and a secret of at
least 16 characters. The default verification mode requires a production
process because production is why `:require-non-production` is deliberately
false.

```bash
export NODE_ENV=production
export KNOXX_MCP_LOOPBACK_TOKEN='replace-with-a-long-random-secret'
pnpm -C backend start:dev
```

In a second terminal, export the same secret and run:

```bash
export KNOXX_MCP_LOOPBACK_TOKEN='replace-with-the-same-secret'
scripts/verify-mcp-auth.sh
```

The script discovers the process listening on `127.0.0.1:8000`, reads its
`/proc/<pid>/cwd`, and refuses to continue unless that directory is this
checkout (or one of its children). Set `KNOXX_SERVER_PID` when listener
discovery is unavailable. Set `KNOXX_BASE_URL` for a different loopback port.

`KNOXX_MCP_VERIFY_SELFTEST=true` exists only for a controlled fake-server test
of the script's HTTP and JSON/SSE classifiers. It prints that the checkout and
environment checks were skipped, and its output is not live verification
evidence.

For an explicitly non-production rehearsal, set
`KNOXX_EXPECT_NODE_ENV=development`; that changes only the precondition, not
the contract or the checks.

## What the script changes

Nothing in Knoxx. This authentication surface needs no domain fixture. Response
headers and bodies are held in a private `mktemp` directory so failures are
inspectable while the script runs, then a trap removes the directory on normal
exit, failure, interrupt, or termination. The token and process environment are
never printed.

## Failure modes walked

- Missing `Authorization` must be HTTP 401.
- A well-formed but unknown bearer must be HTTP 401. With OAuth enabled it may
  consult the token store; without OAuth the route refuses it earlier. The
  public contract is the 401, not which internal refusal path produced it.
- The configured bearer must produce a real JSON-RPC `initialize` result. HTTP
  200 alone is not accepted because MCP can carry protocol failure in the body.
- `tools/list` must return at least one tool and no duplicate names. This proves
  the token record survived context resolution and grant intersection instead
  of merely passing the first authentication comparison.

The script cannot impersonate an off-box peer while connecting directly to a
loopback socket. That negative invariant remains covered by
`shipped-loopback-refuses-off-box-callers-test`; the live artifact verifies the
other half by requiring a loopback URL and observing the production gate admit
the configured token.
