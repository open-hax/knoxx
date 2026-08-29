# Bootstrap credential rotation — live verification

Script: [`scripts/verify-bootstrap-credential-rotation.sh`](../../scripts/verify-bootstrap-credential-rotation.sh)

This verifier exercises the browser-login credential through a real built Knoxx
process and a real transaction-capable Mongo deployment. It is the human proof
for bootstrap administrator rotation; unit doubles remain the faster regression
layer, but they do not prove that startup, HTTP auth, Mongo transactions, and
teardown work together.

## Preconditions

- Use a clean checkout at the revision being reviewed. The verifier refuses
  tracked or untracked source changes.
- Provide `git`, `pnpm`, `mongosh`, `node`, `curl`, and `jq`.
- Provide a Mongo replica set or sharded cluster that the verifier may create
  and drop uniquely named databases on. A standalone `mongod` is deliberately
  refused because it cannot uphold atomic credential replacement.

Run:

```bash
KNOXX_BOOTSTRAP_VERIFY_MONGODB_URI='mongodb://127.0.0.1:27017/?replicaSet=rs0' \
  scripts/verify-bootstrap-credential-rotation.sh
```

The script records `git rev-parse HEAD`, archives exactly that revision into its
private temporary directory, and rebuilds and executes
`backend/dist/server.js` only inside that snapshot. A stale ignored bundle
therefore cannot masquerade as evidence for the reviewed revision, and the
verification build cannot overwrite or leave artifacts in the checkout. It
chooses fresh loopback ports and starts the process with production local-
password policy, event runtimes disabled, and an isolated database name.

## Evidence paths

The run demonstrates four externally distinguishable states:

1. A prior bootstrap email/password authenticates through
   `POST /api/auth/local/login` and has one active Mongo credential.
2. Restarting with a new email/password plus the prior email revokes the old
   login, admits the replacement login, and leaves exactly one replacement.
3. Restarting with a blank password refuses both managed logins and leaves no
   active managed bootstrap credential.
4. In a second database, a collection validator rejects only the replacement
   credential. Knoxx exits nonzero without binding `/health`; Mongo then shows
   the prior credential still active and no active replacement. That is the
   rollback invariant—deactivation never commits by itself.

The archived source, build output, HTTP bodies, and server logs live only in a
private temporary directory for the duration of the run. On success, failure,
interrupt, or termination, the script stops its child process, drops only
databases whose generated names match
`knoxx_bootstrap_verify_rotation_*` or
`knoxx_bootstrap_verify_failure_*`, and removes the temporary evidence.
Every HTTP request disables user curl configuration and proxy routing so the
generated administrator credentials remain on the intended loopback transport.

## Deployment contract

Knoxx now validates Mongo topology before policy seeding. Replica sets and
sharded clusters are admitted; standalone servers fail startup before protected
routes or the HTTP listener are composed. Configure local development Mongo as
a single-node replica set if only one host is needed.
