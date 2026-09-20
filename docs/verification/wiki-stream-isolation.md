# Wiki change-stream isolation

The Wiki SSE handler selects only `knoxx/source-authoring` and
`knoxx/source-review` appends in its authenticated organization and project.
Clio performs that scope match privately through the provider's `change-scope`
declaration. Selected callbacks receive only the stream name; existing
one-argument subscriptions keep their zero-payload callback contract. Unknown
streams and providers without a scope declaration cannot notify the scoped
Wiki subscription.

Scope extraction happens after durable admission, inside the same failure
boundary as notification delivery. An extractor that throws or returns a
non-map skips that observer and reports the failure; it cannot turn an accepted
write into a rejected write. No operation arguments, results, source text,
review facts, or scope values are sent to global subscribers.

Run the existing backend gates from the repository root:

```sh
pnpm -C backend test
pnpm -C backend typecheck
```

`infra/clio_subscription_test.cljs` uses real filesystem ledgers to verify
scope and stream selection, legacy callback compatibility, no-op/retry silence,
unsubscribe, and durable writes despite synchronous/asynchronous observers or
scope extractors failing. Real source/review adapters admit identical domain
operation IDs in different scopes without binding those IDs globally.

`extern/wiki_changes_test.cljs` runs the registered handler against real source
and review providers, with controlled identity refresh. Unrelated OAuth writes,
foreign organizations and other projects cause neither authority refresh nor
SSE frames. Revoked permissions and changed identity scope close the stream
before its next write. Both subscriptions are released on close, including
when close/error signals repeat. Real Fastify injection also exercises initial
401/403 refusals, a successful initial frame, and close cleanup.

The command completion notification is intentionally retained. A source append
is durable before content and manifest projection completes. The test pauses
real filesystem projection, observes the earlier generic invalidation, releases
projection, then requires the document-specific completion hint. Removing the
second hint would omit the notification that the projected resource is usable.
The reviewer [confirmed this ordering and withdrew the duplicate suggestion](https://github.com/open-hax/knoxx/pull/329#discussion_r4056341429).
The unrelated-stream portion still needs verification of the core filter and
this deferred handler integration together.

This is an automated, disposable handler proof. It does not qualify live browser
behavior, production identity startup, or another process's writes. Existing
periodic refresh remains unchanged; this observer bus is process-local. The
deferred integration task still owns live qualification and reviewer adjudication.
