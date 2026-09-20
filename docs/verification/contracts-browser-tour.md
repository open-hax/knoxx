# Contracts browser walkthrough

`scripts/contracts-browser-tour.mjs` exports:

```js
await contractsTour(page, {
  baseUrl: services.baseUrl,
  shot, // async (name, caption, CSS selectors) => annotated screenshot
  verifyCheckout: services.verifyBuilds,
});
```

The caller must be the disposable stack supervisor, with an authenticated admin
browser. `verify-wiki-stack.mjs` owns the temporary contracts directory, actual
service startup, build-hash verification, screenshots and cleanup on success,
failure or signals. The tour deliberately has no standalone invocation against
an arbitrary running server. Its unique agent, trigger and clone are created
through authenticated HTTP inside that directory and removed with the directory
by the supervisor. Both initial fixtures are disabled. No user contract is edited.

The tour verifies the running checkout before writing and after all checks. It
then observes actual browser responses and drives visible controls; it does not
intercept requests, fabricate responses, or change React state directly.

| Step | Evidence |
| --- | --- |
| Anonymous request | The real contract inventory refuses access with HTTP 401. |
| Class selection | Authenticated inventory contains the fixtures; trigger selection requests `kind=triggers` and shows the matching identity, kind and disabled state. |
| Validate and save | CodeMirror receives revised EDN; validation and saving send that exact EDN and selected class. A fresh HTTP read confirms persisted bytes. |
| Clone | The visible Clone controls submit a new ID in the selected class; a fresh read confirms separate identity and retained version. |
| Failure | A string in `:enabled` fails real schema validation. Saving returns HTTP 400, preserves stored bytes and retains the editable draft and visible failure notice. |
| Responsive controls | The narrow overlay opens and closes, the librarian toggles, and auto-focus can be changed and restored. |

Five annotated captures show selection, saved/normalized content, cloning, the
rejected save and the narrow library. Captions describe only assertions already
completed against the real browser/server. Failed assertions propagate to the
supervisor's nonzero exit and failure evidence. Response observers are cancelled
in `finally`, the anonymous context is always closed and viewport changes restore
the original viewport.

The librarian's model generation is explicitly not exercised by this module;
the Wiki writing tour owns a real model-response assertion. CodeMirror and the
chat controller remain the existing legacy boundaries, while the Contracts view
is ClojureScript. The existing regex-based metadata editor is not a general EDN
structural editor.

Source verification: `node --check scripts/contracts-browser-tour.mjs` checks
syntax without touching production build outputs. A syntax check is not browser
evidence. The integrated supervisor must run against freshly rebuilt source and
record its actual results and screenshots before the browser work is complete.

Author and independent bounded source reviews found no confirmed introduced
defect in the request assertions, class selection, persisted-byte checks or
cleanup. Selection intentionally moves trigger → agent, so each selection
causes a new request rather than waiting for a request from an unchanged
selection. These reviews do not substitute for the pending browser gate.
# First real browser attempt

Browser13 passed the real identity, administration and Mail flows and proved
anonymous contract access returns401. Its authenticated fixture seed PUT was
correctly refused403 because Playwright's direct request API does not add the
Origin header that browser writes provide. The harness now supplies the actual
supervised frontend origin for that one direct write. Runtime origin validation
is unchanged. The complete Contracts browser proof remains pending the rerun.
