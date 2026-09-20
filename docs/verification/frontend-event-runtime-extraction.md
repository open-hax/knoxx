# Existing event runtime UI size and failure handling

The 1,117-line legacy Discord runtime section now has 268 lines. Its existing
Events page owns the request/controller hook (379 lines); the existing event
panel owns the selected job editor (387), and the schedule-review module owns
the runtime sidebar and job selection (314). Existing administrative primitives
and pure helpers own disclosure and JSON draft projection. All seven touched
source/test files pass the unchanged 400/800-line size gate with zero warnings
and errors. No new TypeScript paths, removed UI surfaces or bridge exports were
introduced.

The original public entry points and native wrapper remain available. Requests,
authorization flags, form fields, JSON conversion/errors, schedules, filter
selection and selected-job callbacks retain their original contracts. The
Events page and section use ESM function declarations; neither invokes its
controller while modules initialize. Actual rendering tests and full legacy
emission verify that module relationship rather than relying on type checking
alone.

An added failed-read regression first reproduced a real defect: initial request
failure rendered the loading state forever. The section now shows the failure
and a retry action. Write controls remain absent until a read succeeds. This
matches the previously fixed native event runtime experience.

Verification passes:

- Focused runtime and native-wrapper suite: **9 tests**, including initial
  read failure/retry, exact edited control payload, malformed JSON refusal,
  read-only controls, runtime stop/start/reset, selection and search.
- Complete frontend TypeScript typecheck and isolated legacy emission.
- Complete Vitest suite: **45 files / 225 executed tests**, zero failures.
  The **41 pre-existing TODO cases remain unexecuted** and visible in the summary.
- Scoped size gate: **7 files / zero errors / zero warnings**.

Production artifacts stayed frozen during this source-only work. The remaining
legacy frontend size findings are still part of the repository gate; this
checkpoint makes no claim that those other modules have been resolved.
