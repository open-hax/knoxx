# Audit sidebar request and rendering boundaries

The audit sidebar keeps its existing contract-scoped memory/active-run merge,
search, resume action, refresh, 60-second polling and 20-row pagination. Native
async functions replace Promise chains in the request and component layers.
Page installation still checks the request sequence before changing rows,
errors or busy state, so a response from the previous contract cannot replace
the current view. The active-run request retains its administrator-to-caller
fallback and the existing endpoint/query wire.

The rendering component now composes state, filtering, scrolling and row
rendering through named helpers. Existing public component props are retained.
Pure projection functions describe their alias, ranking and matching contracts.
No lint threshold or warning rule changed.

Strict clj-kondo over the entire audit source and test directories reports
**zero errors / zero warnings**, down from 57 warnings. The focused runtime
gate passed **12 tests / 52 assertions**, zero failures or errors, with
**80 files / zero compiler warnings**. Existing interaction tests cover search,
resuming and pagination. New tests exercise recovery from a failed request and
out-of-order completion after switching contract scopes. The runtime used the
shared uncaught-error preload and test-summary validator.

This gate writes to the ignored `frontend/dist-verification/audit-tests.cjs`
while the root browser tour holds the production `frontend/dist` files stable.
The next full frontend test and production build remain required after the
current browser run; this focused result does not certify uncompiled changes
in other namespaces.

## Combined frontend gate

After browser08 released its artifact lease, the advertised `pnpm test:cljs`
completed with **512 tests / 2,231 assertions**, zero failures/errors and
**281 files / zero compiler warnings**. This includes the Documents transport
and interaction changes, the admin read-failure retry, audit ordering tests and
existing publication-wire/portable-renderer parity tests.

The four shared source namespaces now also pass strict frontend clj-kondo with
zero warnings/errors. Their 38 warnings were removed by documenting the shared
wire and markup contracts, qualifying attribute binding names and sorting the
React import. Wire enum values, row keys, selectors, markup validation and
renderer behavior are unchanged.
