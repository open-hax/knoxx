# policyDb facade boundary violation map

Generated: 2026-05-22
Scope: `backend/src/cljs/knoxx/backend/**/*.cljs`
Purpose: document the pre-migration call sites of the `policyDb` JS facade so it can be removed instead of expanded.

## Remediation status

This report is a historical baseline for the facade removal pass completed later on 2026-05-22. After the migration, a source scan of `backend/src/cljs/knoxx/backend` finds no remaining `policyDb`, `build-facade`, `ctx->js`, `session->js`, `bootstrap->js`, `js->opts`, or facade `.camelCaseMethod` references outside the report itself. Remaining raw `.query` references are confined to `knoxx.backend.extern.pg` plus unrelated external APIs.

## Boundary invariant

JS object shape, camelCase keys, `aget`/`aset`, raw `.method` dispatch, and native `.query` belong at explicit extern boundaries only. Outside `knoxx.backend.extern.*`, callers should receive CLJS maps/vectors/scalars with kebab-case keys and should call namespace-qualified CLJS functions.

The current `policyDb` facade violates that invariant twice:

1. `infra/db/policy.cljs` builds JS objects and camelCase method names (`build-facade`, `ctx->js`, `session->js`, `bootstrap->js`, `js->opts`).
2. Non-extern callers consume that JS surface with `.camelCaseMethod`, `aget`, `clj->js`, `js->clj`, raw SQL strings, and JS result shapes.

## Facade definition to delete

Source: `backend/src/cljs/knoxx/backend/infra/db/policy.cljs`

| Lines | Boundary leak |
| --- | --- |
| 7-9 | Namespace docstring says `create-policy-db` returns `Promise<facade | nil>` and callers use a JS `policyDb` object. |
| 1213-1262 | `ctx->js`, `session->js`, and `bootstrap->js` encode CLJS maps into JS/camelCase shapes. |
| 1264-1359 | `build-facade` creates the JS method-object facade. |
| 1365-1369 | `create-policy-db` advertises and returns the facade. |
| 1417 | `create-policy-db` resolves `(build-facade pool primary-org bootstrap ...)`. |

Current facade exports:

- `close`
- `query`
- `resolveRequestContext`
- `syncUserFromActorContract`
- `setMembershipRoles`
- `getBootstrapContext`
- `createUser`
- `createInvite`
- `redeemInvite`
- `listInvites`
- `createSession`
- `getSessionByToken`
- `deleteSessionByToken`
- `cleanupExpiredSessions`

## Direct method/aget call matrix

This table is generated from direct `.method db`, `.method policyDb`, `.method @db-session-store`, and `(aget ... "method")` references. `NO` means the current route calls a method that the current `build-facade` does not export, so the facade is both a boundary leak and an incomplete compatibility surface.

| Method | Current facade exports it? | Call sites |
| --- | --- | --- |
| `close` | yes | `backend/src/cljs/knoxx/backend/infra/graceful_shutdown.cljs:86` |
| `createDataLake` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:119` |
| `createInvite` | yes | `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs:136` |
| `createOrg` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:57` |
| `createRole` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:81` |
| `createSession` | yes | `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:117` |
| `createUser` | yes | `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs:43`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:38`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:73`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:85` |
| `deleteSessionByToken` | yes | `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:195` |
| `getActorCredential` | NO | `backend/src/cljs/knoxx/backend/domain/actor/credentials.cljs:44` |
| `getBootstrapContext` | yes | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:21`; `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs:38` |
| `getMembership` | NO | `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:165`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:181` |
| `getRole` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:91` |
| `getSessionByToken` | yes | `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:123` |
| `listDataLakes` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:107` |
| `listInvites` | yes | `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs:162` |
| `listMemberships` | NO | `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:155` |
| `listOrgs` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:48` |
| `listPermissions` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:30` |
| `listRoles` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:69` |
| `listTools` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:39` |
| `listUsers` | NO | `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:25`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:48`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:61` |
| `query` | yes | `backend/src/cljs/knoxx/backend/domain/actor/mailbox.cljs:111`; `backend/src/cljs/knoxx/backend/domain/actor/mailbox.cljs:116`; `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:80`; `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:91`; `backend/src/cljs/knoxx/backend/infra/routes/app.cljs:1085`; `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs:82`; `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs:101`; `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs:115`; `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs:134`; `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs:336`; `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs:363` |
| `redeemInvite` | yes | `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs:117` |
| `resolveRequestContext` | yes | `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:358`; `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:376`; `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:453`; `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:650`; `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs:53`; `backend/src/cljs/knoxx/backend/infra/routes/mcp.cljs:406` |
| `setMembershipRoles` | yes | `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:354`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:171` |
| `setMembershipToolPolicies` | NO | `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:187` |
| `setRoleToolPolicies` | NO | `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs:97` |
| `syncActorContracts` | NO | `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:59` |
| `syncUserFromActorContract` | yes | `backend/src/cljs/knoxx/backend/infra/auth/session.cljs:366` |
| `updateUserActor` | NO | `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:99`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:113` |
| `upsertActorCredential` | NO | `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:129`; `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs:145` |

## File-by-file inventory

### `backend/src/cljs/knoxx/backend/bootstrap.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 83-84 | `start-http!` accepts `policyDb` and stores it in `runtime-state`. | Accept a CLJS policy context, e.g. `{:pool pool :primary-org primary-org :bootstrap bootstrap}`. |
| 109 | Passes `policyDb` into `auth-session/create-session-hook`. | The hook should not require a facade; session loading should use CLJS session functions/context. |
| 112 | Builds `#js {:policyDb policyDb :runtime runtime}` for auth routes. | Pass a CLJS opts map; only `extern.fastify` should decode JS route opts. |
| 161-165 | `create-policy-db` resolves `policyDb` and threads the facade through lifecycle/startup. | Rename the value to `policy-context` or similar and keep it CLJS-typed. |
| 189-191 | Hot-reload lifecycle reuses `:policyDb`. | Lifecycle state should hold CLJS context, not a JS facade. |

### `backend/src/cljs/knoxx/backend/runtime/state.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 10-21 | `policy-db*` stores whatever `create-policy-db` returned, currently a JS facade. | Store a CLJS policy context or opaque pool handle; `current-policy-db` should return CLJS data. |

### `backend/src/cljs/knoxx/backend/infra/lifecycle.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 13, 17-21, 32 | HTTP lifecycle state names/stores `:policyDb`. | Rename to `:policy-context`/`:policy-db` and keep a CLJS map. |

### `backend/src/cljs/knoxx/backend/infra/auth/session.cljs`

This is the deepest facade consumer: it stores the facade in an atom, calls facade methods, builds camelCase JS payloads, and consumes JS/camelCase result objects.

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 60-68 | `db-session-store` atom stores `policyDb`. | Store policy context or pool, not a JS method object. |
| 80-98 | Raw `.query policyDb`, SQL string, `aget result "rows"`, `aget rows 0 "value"`. | Move to `policy-db/recover-session-secret!` over `pg/query!`, returning a string. |
| 107-117 | `db-store-session` builds `clj->js` payload with `:userId`, `:membershipId`, etc.; calls `.createSession`. | Build a CLJS map with kebab-case keys and call `policy-db/create-session!`. |
| 123-137 | Calls `.getSessionByToken`; reads `result.session` and `session.userId`/`membershipId`/`orgId`. | `policy-db/get-session-by-token!` should return CLJS; use `get-in`. |
| 195 | Calls `.deleteSessionByToken @db-session-store`. | Call `policy-db/delete-session-by-token!`. |
| 348-358 | `ensure-bootstrap-admin-role!` reads JS ctx, calls `.setMembershipRoles`, then `.resolveRequestContext`. | Accept/return CLJS context and call `policy-db/set-membership-roles!` then `policy-db/resolve-request-context!`. |
| 362-376 | Uses `(aget policyDb "syncUserFromActorContract")`, JS payload, and `.resolveRequestContext`. | Call `policy-db/sync-user-from-actor-contract!` directly; then resolve CLJS context. |
| 437-453 | Cookie auth rebuilds JS headers and calls `.resolveRequestContext`. | Represent request context as CLJS data and call `policy-db/resolve-request-context!`. |
| 457-513 | `fresh-ctx`/`ctx` are JS objects; session data is `#js` with camelCase keys; response echoes JS subobjects. | Session construction should consume CLJS context (`get-in`) and emit HTTP JSON only at the route boundary. |
| 645-650 | Header-auth path calls `.resolveRequestContext policyDb (.-headers req)`. | Route/extern layer should decode headers to CLJS first; auth uses CLJS map. |

### `backend/src/cljs/knoxx/backend/infra/auth/authz.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 7-13 | `policy-db` returns `runtime-state/current-policy-db`, currently the facade. | Return CLJS policy context. |
| 36-45 | `policy-db-promise` assumes facade promises return JS and runs `js->clj`. | Promise should already resolve CLJS data; HTTP JSON encoding belongs at response boundary. |
| 51-66 | Calls `.resolveRequestContext` or delegates to auth-session, then converts with `js->clj`. | Call `policy-db/resolve-request-context!`; remove conversion. |
| 80-85 | Context accessors support camelCase flat keys and `aget` fallback (`orgId`, `userId`, etc.). | Normalize context to a single nested CLJS shape. |
| 90-95, 174-177, 220 | Additional camelCase compatibility paths (`:roleSlugs`, `:orgId`, etc.). | Remove once callers return CLJS/kebab data. |

### `backend/src/cljs/knoxx/backend/infra/routes/auth.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 10 | `(aget opts "policyDb")` extracts a JS facade from route opts. | Register routes with a CLJS opts map; extern layer owns JS opts. |
| 18-19 | Passes facade to `auth-session/set-db-session-store!`. | Pass policy context/pool. |
| 38-42 | Calls `.getBootstrapContext`; reads `bootstrap.primaryOrg.id/slug` by `aget`. | Call `policy-db/get-bootstrap-context!`; use `get-in`. |
| 43-53 | Calls `.createUser` with `clj->js` camelCase payload; then `.resolveRequestContext`. | Call `policy-db/create-user!` and `policy-db/resolve-request-context!` with CLJS maps. |
| 117-121 | Calls `.redeemInvite`; reads `result.invite` and `result.user` by `aget`. | Call `policy-db/redeem-invite!`; return `(:invite result)`/`(:user result)`. |
| 128, 157 | Auth context resolution still expects a facade. | Make `auth-session/resolve-auth-context` accept policy context. |
| 136-147 | Calls `.createInvite` with camelCase JS payload; reads `result.invite`. | Call `policy-db/create-invite!` with kebab keys. |
| 162 | Calls `.listInvites` with `clj->js {:orgId ...}`. | Call `policy-db/list-invites!` with `{:org-id ...}`. |

### `backend/src/cljs/knoxx/backend/infra/routes/admin.cljs`

All of these are facade method calls. Most are not currently exported by `build-facade`.

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 21 | `.getBootstrapContext db`. | `policy-db/get-bootstrap-context!`. |
| 30 | `.listPermissions db` (not exported by facade). | `policy-db/list-permissions!`. |
| 39 | `.listTools db` (not exported). | `policy-db/list-tools!`. |
| 48 | `.listOrgs db` (not exported). | `policy-db/list-orgs!`. |
| 57 | `.createOrg db` with JS request body (not exported). | Normalize body to CLJS; call `policy-db/create-org!`. |
| 69 | `.listRoles db` with `clj->js {:orgId ...}` (not exported). | `policy-db/list-roles!` with `{:org-id ...}`. |
| 77-81 | `Object.assign` payload plus `.createRole db` (not exported). | CLJS `merge`; `policy-db/create-role!`. |
| 91-97 | `.getRole`, `js->clj (aget result "role")`, `.setRoleToolPolicies` (not exported). | `policy-db/get-role!`; `policy-db/set-role-tool-policies!`. |
| 107 | `.listDataLakes db` with JS opts (not exported). | `policy-db/list-data-lakes!`. |
| 115-119 | `Object.assign` payload plus `.createDataLake db` (not exported). | CLJS `merge`; `policy-db/create-data-lake!`. |

### `backend/src/cljs/knoxx/backend/infra/routes/users/admin.cljs`

All of these are facade method calls. Most are not currently exported by `build-facade`.

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 25, 48, 61 | `.listUsers db` with JS opts (not exported). | `policy-db/list-users!` with CLJS opts. |
| 38, 73, 85 | `.createUser db` with JS request/payload. | Normalize request body to CLJS and call `policy-db/create-user!`. |
| 59 | `.syncActorContracts db` (not exported). | `policy-db/sync-actor-contracts!` with policy context pieces. |
| 99, 113 | `.updateUserActor db` (not exported). | `policy-db/update-user-actor!` with kebab keys. |
| 129, 145 | `.upsertActorCredential db` (not exported). | `policy-db/upsert-actor-credential!`. |
| 155 | `.listMemberships db` (not exported). | `policy-db/list-memberships!`. |
| 165-171 | `.getMembership`, `js->clj (aget result "membership")`, `.setMembershipRoles` with JS body. | `policy-db/get-membership!`; `policy-db/set-membership-roles!`. |
| 181-187 | `.getMembership`, `js->clj`, `.setMembershipToolPolicies` (not exported). | `policy-db/get-membership!`; `policy-db/set-membership-tool-policies!`. |

### `backend/src/cljs/knoxx/backend/infra/routes/studio.cljs`

These are raw `.query` facade calls and bypass `extern.pg`, HoneySQL shape builders, and typed result decoding.

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 73-83 | `studio-state-get!` calls `.query db` with raw SQL and `clj->js` params. | Add shape/db query and call `pg/query!` or policy-db public function. |
| 90-101 | `studio-state-put!` raw insert/update SQL and `js/JSON.stringify`. | Add typed CLJS input, SQL shape, and row codec. |
| 107-115 | `studio-playlist-get!` raw select. | Add studio-state read function returning CLJS. |
| 124-134 | `studio-playlist-put!` raw upsert. | Add studio-state write function returning CLJS. |
| 329-336 | `studio-audio-asset-get!` raw select. | Add studio audio asset query function/shape. |
| 350-363 | `studio-audio-asset-save!` raw insert/update. | Add studio audio asset write function/shape. |

### `backend/src/cljs/knoxx/backend/infra/routes/app.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 1084-1114 | `/api/data/pg/query` extracts `(aget db "query")` from policy-db and executes raw SELECT strings. | Either make this an explicit admin/debug extern-owned SQL endpoint or route through a named CLJS DB adapter with guarded shapes. |

### `backend/src/cljs/knoxx/backend/infra/routes/mcp.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 199-210 | Browser auth guard receives `policy-db` and delegates to `auth-session/resolve-auth-context`, which expects the facade. | Pass policy context; auth-session resolves CLJS context. |
| 404-410 | `resolve-token-context!` gets `(aget policy-db "resolveRequestContext")` and calls it with JS headers. | Call `policy-db/resolve-request-context!` with CLJS headers. |
| 694-704 | Route registration stores `runtime-state/current-policy-db` as `:policy-db` dep. | Store CLJS policy context. |

### `backend/src/cljs/knoxx/backend/domain/actor/mailbox.cljs`

This domain namespace should not know about JS runtime objects or raw SQL execution.

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 103-106 | `policy-db` reads `(aget runtime "policyDb")` and falls back to `runtime-state/current-policy-db`. | Runtime context should be CLJS; dependency should be passed explicitly. |
| 110-117 | Checks `(aget ... "query")` and calls query fn with `clj->js` params. | Move persistence to infra; domain emits/consumes CLJS mailbox data. |
| 119-145+ | Result row decoding uses JS row shapes. | Keep row decoding in infra/shape/extern row codec. |

### `backend/src/cljs/knoxx/backend/domain/actor/credentials.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 26-29 | `normalize-credential` reads `payload.credential` by `aget` then `js->clj`. | Credential resolver should return CLJS `{:credential ...}`. |
| 33-44 | Gets `db` from `authz/policy-db` and calls `.getActorCredential` (not exported by current facade). | Call a public CLJS policy function/protocol implementation. |

### `backend/src/cljs/knoxx/backend/domain/mcp/mcp_expose.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 1-8, 124-136 | Namespace documents and accepts `ctx-js` from `policyDb.resolveRequestContext`, then `js->clj`s it. | Expose either an extern-owned JS entrypoint or make internal MCP tooling accept CLJS context only. |

### `backend/src/cljs/knoxx/backend/domain/discord/source.cljs` and `backend/src/cljs/knoxx/backend/infra/core.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| `infra/core.cljs:96-100` | Pulls `:policyDb` from lifecycle context and passes it as `:policy-db`. | Lifecycle should store policy context, not facade. |
| `domain/discord/source.cljs:270-275` | Calls `policy-db/list-actor-credentials!` with the value named `policy-db`; today that value comes from the facade slot, while the function expects a pool. | Thread a real pool/policy context and make the function signature explicit. |

### `backend/src/cljs/knoxx/backend/infra/graceful_shutdown.cljs`

| Lines | Violation | Clean direction |
| --- | --- | --- |
| 85-87 | Reads `current-policy-db`, then `(aget policy-db "close")` and calls it. | Expose a CLJS shutdown function over policy context, e.g. `policy-db/close!`. |

## Migration target

Preferred shape:

```clojure
{:pool pool
 :primary-org primary-org
 :bootstrap bootstrap
 :bootstrap-user-id (get-in bootstrap [:user :id])
 :bootstrap-membership-id (get-in bootstrap [:membership :id])}
```

Then replace every facade dispatch:

```clojure
;; before
(.resolveRequestContext policyDb headers)

;; after
(policy-db/resolve-request-context! (:pool policy-context) headers)
```

Route bodies/queries/headers should be decoded at the HTTP boundary into CLJS maps. Policy DB public functions should accept and return CLJS maps with kebab-case keys. Raw PG `.query` should remain inside `knoxx.backend.extern.pg`; non-extern persistence should use `pg/query!`/`pg/query-one!` and shape/db query builders.

## Notes for implementation order

1. Stop expanding the facade. Every missing `NO` method above is pressure to remove the facade, not add methods.
2. Change `create-policy-db` to return policy context data.
3. Convert auth/session first, because it owns session persistence, cookie auth context recovery, and most JS context shape consumption.
4. Convert `authz/policy-db-promise` so route handlers can return CLJS maps without `js->clj` recovery.
5. Convert admin/users/auth route calls to namespace-qualified `policy-db/*!` functions.
6. Move raw `.query` users (`studio`, `app`, `mailbox`) behind named CLJS persistence functions.
7. Delete `build-facade`, `ctx->js`, `session->js`, `bootstrap->js`, and `js->opts`.

## Search commands used

```bash
rg -n "build-facade|ctx->js|session->js|bootstrap->js|policyDb|current-policy-db|create-policy-db|\.resolveRequestContext|\.createSession|\.getSessionByToken|\.deleteSessionByToken|\.getBootstrapContext|\.createUser|\.redeemInvite|\.createInvite|\.listInvites|\.setMembershipRoles|\.syncUserFromActorContract|aget .*policyDb|aget .*\"policyDb\"|\.query" backend/src
```

A Node script also scanned direct `.method db`, `.method policyDb`, `.method @db-session-store`, and `(aget ... "method")` forms to build the method matrix above.
