# Translation UI contracts and native model requests

Translation inventory and candidate-authority logic remain in `logic.cljs`;
locale/status formatting, form projections and authored detail presentation now
live in `presentation.cljs`. Compatibility exports preserve every existing
caller. The split reduces the inventory module from 498 to fewer than 400 lines without
changing candidate matching, review identity, immutable approval coordinates or
the legacy/resource compatibility rules.

Pipeline model reads and saves now use native async functions. An unavailable
model catalog still permits editing the configured model. Refused saves retain
the draft and release busy controls; read-only roles cannot edit or save. Three
new rendered tests prove those cases. The initial test run exposed a model input
whose accessible name included the current-value text. Its explicit
`Translation model` label now identifies the input independently of that value.

The entire translation source/test directory passes strict clj-kondo with zero
errors/warnings. Its runtime gate passed **77 tests / 340 assertions**, zero
failures/errors, **114 files / zero compiler warnings**, using the uncaught-error
preload and shared summary validator. This includes existing live review,
concurrency, inventory, form-sync, split-history and reconciliation tests.
Production acceptance requires the next combined frontend build and stack tour;
the isolated test output leaves the currently served browser artifacts stable.
