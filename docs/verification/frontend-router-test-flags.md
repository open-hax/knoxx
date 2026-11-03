# Legacy router test configuration

The six existing React Router test wrappers now explicitly enable
`v7_startTransition` and `v7_relativeSplatPath`. This exercises the framework's
announced routing behavior instead of leaving its future-flag warnings in every
test run. No console warning filter or assertion suppression was added.

All **six files / 40 tests** pass, covering visual CMS navigation, content editing,
review queues, broadcast studio and dashboard attention links. Their runtime log
contains no warning output. The full frontend TypeScript check also passes.

The separate legacy UI/backend surface matrix still declares 41 TODO tests.
Those are planned coverage, not executed successes, and remain visible in the
full Vitest report until their acceptance scenarios are implemented.
