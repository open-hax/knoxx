# Knoxx Agent Session Registry and Provider Ports

Date: 2026-05-21
Status: todo
Parent epic: `knoxx-agent-service-protocol-split-epic.md`
Source report: `docs/notes/architecture/agent-service-protocol-split.md`
Story points: 5

## Purpose

Extract active-session cache behavior and eta-mu/provider interop from `session.cljs` into explicit ports with focused implementations.

## Problem

`session.cljs` currently combines at least these unrelated concerns:

- `sessions*` in-process cache
- TTL sweep and eviction
- `active-agent-session`
- session removal
- eta-mu runtime setup
- model registry/model lookup
- provider session creation
- tool signatures and visible session signatures
- history/media/lifecycle side effects

This makes provider changes and cache behavior difficult to test independently.

## Target ports

```clojure
(defprotocol IActiveSessionRegistry
  (get-active-session [registry conversation-id])
  (put-active-session! [registry conversation-id entry])
  (touch-active-session! [registry conversation-id])
  (remove-active-session! [registry conversation-id])
  (sweep-expired-sessions! [registry now-ms]))

(defprotocol IAgentProviderAdapter
  (ensure-runtime! [provider])
  (resolve-model [provider model-id auth-context])
  (create-session! [provider session-request])
  (send-message! [provider provider-session message-request])
  (subscribe-stream! [provider provider-session handlers]))
```

Exact names may change, but the behavioral split should remain.

## Deliverables

1. `knoxx.backend.infra.agent.session_registry` namespace implementing `IActiveSessionRegistry` for the existing atom-backed registry.
2. `knoxx.backend.infra.agent.provider.eta_mu` or equivalent namespace implementing `IAgentProviderAdapter` over existing eta-mu runtime code.
3. Tests for registry TTL, touch, removal, and active lookup semantics.
4. Tests or fakes proving provider adapter calls can be mocked without eta-mu runtime startup.
5. Transitional functions in `session.cljs` delegating to the new registry/provider implementations.

## Non-goals

1. Changing the provider from eta-mu/proxx to another runtime.
2. Replacing session persistence or Redis state.
3. Moving tool catalog/content/history code in this slice.
4. Changing model policy semantics.

## Implementation notes

- Keep atom-backed registry behavior identical unless a bug is explicitly documented.
- Separate provider model resolution from policy authorization; provider resolves available provider models, policy decides admissibility.
- Avoid importing domain tool/content/history namespaces into the provider adapter unless unavoidable.

## Acceptance criteria

1. Active-session registry operations are testable without eta-mu runtime.
2. Eta-mu runtime setup and provider session creation are isolated behind an adapter.
3. `session.cljs` loses direct ownership of either registry internals or provider runtime setup, or delegates those concerns clearly.
4. Existing agent turn behavior remains compatible.

## Verification

```bash
pnpm -C backend exec shadow-cljs compile test
```
