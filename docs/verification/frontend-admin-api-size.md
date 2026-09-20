# Administration API size recovery

The existing 569-line administration API mixed HTTP operations, generic wire decoding, and request/response type declarations. Generic scalar, array, record, and alias decoders now live in the existing API core; administration request/response types live in the existing administration types module. The endpoint module retains the named administrative normalizers and transport operations. Its established exported response type names remain available through type reexports.

All normalizer and HTTP request bodies are preserved, including explicit null/false values, alias priority, encoded identifiers, authenticated identity binding fields, credentials, and error handling. The moved type declarations are erased at runtime. No TypeScript/TSX path or dependency was added, and no size threshold changed.

The four owned source/test paths pass the existing size gate at **0 errors, 0 warnings**: API **383 lines**, core **173**, administration types **356**, and API tests **298**.

Verification obstacles:

- The existing API suite mocked the entire core module as a request-only object. Moving real decoders into their shared owner exposed that stale fixture as **7 failed / 9 passed** tests. The fixture now imports the actual core and replaces only its request operation; production decoding is exercised directly.
- One additional public API case proves that explicit null/false values outrank alternate wire aliases, malformed tool policies are refused, and the organization path remains encoded.
- A first full TypeScript check encountered three concurrent missing-import errors in the separately owned chat sidebar. Its owner restored the remaining `formatMaybeDate` import; this lane did not modify the sidebar.

Final focused API verification passes **4 files / 26 tests, no failures or skipped tests**. The final advertised `pnpm typecheck` exits **0**. Self-review compared the moved declarations, untouched normalizer bodies, retained API type exports, and unchanged request/auth/error implementation. Production output remained frozen for the coordinating browser walkthrough and was not written by this lane.
