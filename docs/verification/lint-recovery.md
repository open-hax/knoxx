# Lint recovery and trustworthy failures

The recovered standalone size command lacked `size-lint.config.mjs` and failed
before reading source. The restored configuration uses the existing 400-line
warning and 800-line error thresholds, across backend, frontend and shared
source. It does not exempt existing oversized files.

The two CLJS lint hooks also used `:filename` from node metadata, which is absent
in the actual clj-kondo hook API. All files shared the empty-string key. Findings
were delayed until the next namespace and could be attributed to that next file;
a large final file could be missed entirely. Hooks now use the owning filename
from the invocation and report each threshold while analyzing that file. These
are lower-bound line counts from source forms; `lint:size` reads complete files.

Run the real linter regression:

```sh
node --test backend/test/js/kondo-file-size-hook.test.mjs
```

It failed for both original hooks, then passed for both repaired hooks, checking
a large file alone and before/after a tiny file. Thresholds are unchanged.

The first complete lint after this repair remains red: backend 13 errors / 273
warnings; frontend CLJS 6 errors / 632 warnings. These include existing file size,
function size, namespace order, missing documentation, and incomplete protocol
fixture warnings. Counts are a dated recovery checkpoint, not a passing gate.
The standalone size pass separately found 8 backend errors and 8 frontend errors;
its frontend scan includes the legacy TypeScript surface too. Repairs must reduce
this debt rather than raising thresholds or suppressing findings.
