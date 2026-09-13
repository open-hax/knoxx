# Layout persistence and route boundaries

The workbench's side and bottom panels share one hook for persisted open state
and bounded dimensions. Expanded sidebar rendering is a separate component,
keeping the resize handle on the inner edge. Existing storage keys, defaults,
drag directions and public component props are retained.

Three new rendered interaction tests exercise collapsed-state restoration,
clamping saved dimensions, resizing and remounting, reversed right-edge drag,
bottom-panel height, and cursor/selection cleanup after drag completion. The
existing storage tests now restore `globalThis.localStorage` after every test;
previously their injected storage leaked into later suites.

The routing helpers also document their existing paths and access rules and use
unambiguous local names. The browser entry-point comment now reflects the actual
implementation: `app/mount!` owns the retained React root. The old comment
incorrectly said this bundle must never mount it.

Strict clj-kondo over all changed source and tests reports **zero errors and zero
warnings**. The isolated layout, route and storage gate passed **18 tests / 72
assertions**, zero failures/errors, **81 files / zero compiler warnings**, with
the uncaught-error preload and shared summary validator enabled. Generated test
output stays outside production `dist` during the stack's browser tour. The next
full app compilation remains necessary before claiming production acceptance of
this layout refactor.

The next bounded cleanup covers operations status and native HTTP tests. Its
existing rendered tests still prove that streamed statistics update metric rows,
ingestion state appears and unmount disconnects the stream. HTTP tests now await
requests directly and restore both injected fetch and localStorage globals after
each test. That gate passed **11 tests / 25 assertions**, zero failures/errors,
with **82 files / zero compiler warnings** and strict source/test lint at zero.

Settings and Gardens now also use qualified components and native async reads.
Settings retains endpoint failure handling and ignores obsolete status results;
Gardens retains explicit single-placement and sequential bulk reconciliation.
All existing tests in those two areas passed: **16 tests / 69 assertions**, zero
failures/errors, **83 files / zero compiler warnings**. Strict source/test lint
is zero for both areas. The full exposed frontend lint inventory is down to
102 warnings outside these completed slices; its fail level remains unchanged.

Shared UI primitives keep their public `:type` props while binding them to
explicit button/input names. The remaining library and renderer tests now use
qualified test/component aliases and private fixture definitions. Their
existing coverage passed **62 tests / 261 assertions**, zero failures/errors,
**103 files / zero compiler warnings**, with strict scoped lint at zero. This
includes actual rendered controls, publication request wires, portable markup
parity, EDN parsing, media/document links and WebSocket channel handling.

The Mail cleanup changes tests only: qualified aliases, private fixtures and
native awaited filter/acknowledgment interactions. All **23 Mail tests / 104
assertions** passed with zero failures/errors and **87 files / zero compiler
warnings**. The full Mail test directory is lint-clean. Production Mail code
and the active browser artifacts were left unchanged during this batch.
