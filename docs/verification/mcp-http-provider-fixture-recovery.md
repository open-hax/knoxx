# MCP token inventory fixture recovery

Full backend08 compiled 1,029 inputs with zero compiler warnings. Its 873
captured source hashes were unchanged when compilation finished. The guarded
Node run completed 1,851 tests and 8,397 assertions, with two failures in the
same token inventory test. The expected HTTP 200 instead raised
`mcp_oauth_provider_unavailable` with status 503: the fixture installed no OAuth
provider and had depended on an unavailable Mongo store returning an empty
inventory. The production refusal remains intact.

The repaired fixture installs an isolated real Clio OAuth store, registers an
actual public OAuth client, and issues separate grants for two memberships.
The existing route test still verifies HTTP 200 and membership lookup from a
ClojureScript authentication context. It now also requires the exact token
digest for that membership, excluding the other membership's grant. The route's
existing encoded inventory envelope is decoded by its named test boundary.
The fixture restores the previous provider and removes its temporary directory
in `finally`. No production persistence or authorization behavior changed.

The first focused attempt correctly failed after the nominal test summary:
the new fixture had issued tokens before registering its client. The fatal
asynchronous guard caught `mcp-oauth-client-missing` and forced exit 1. Registering
the client through the same selected-provider facade fixed the fixture; the
Clio requirement was not relaxed. An initial fixture edit had an unmatched
delimiter, caught by lint and fixed before compilation.

Final focused verification used frozen eta identity initialization
`fc3b6a09c6cd90ca200023cbc1fc54ec57a630a0`. The distinct
`mcp-http-provider-proof` build compiled 463 inputs, three compiled, with zero
warnings in 9.42 seconds. Its native run passed **10 tests / 54 assertions**,
zero failures and errors, and the fatal asynchronous guard exited zero.
All three source, fixture and compiler files pass the configured size rules
and seven optional lint rules with zero findings.

Run the focused compiler from `backend` with the same dependency overrides as
the sandbox's full gate, then execute its isolated output:

```sh
clojure -M:cljs scripts/compile-mcp-http-provider-proof.clj
CONTRACTS_DIR=test/fixtures/empty-contracts node \
  --require ./scripts/shadow-test-error-guard.cjs \
  target/mcp-http-provider-proof/tests.cjs
```

This proof does not turn full08 into a pass. A subsequent complete guarded run
must include this fixture and the concurrent projection repair found by the
browser tour. Production browser artifacts were not modified by these tests.
