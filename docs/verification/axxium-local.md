# Local Axxium authority in Knoxx

Run `node scripts/verify-axxium-local.mjs` from this Knoxx checkout with Docker
and the `mongo:7` image available. The script first checks the actual Knoxx PM2
backend child and sibling Axxium checkout. It launches this checkout's backend
against a disposable MongoDB replica set and copied role contracts, so a first
delegated login can create an identity without touching the live Knoxx store.
It uses the private local administrator created by Axxium's bootstrap; no
reviewer has to handcraft fixtures. It rejects anonymous access and bad
credentials, confirms Knoxx disables local signup, checks the delegated user
context and frontend proxy, logs out, confirms the cookie returns 401, then
stops the backend and removes its MongoDB container and temporary contracts.
Knoxx revokes the transient Axxium verification session before it issues its
own session, so the password check does not leave a provider session behind.
It exits nonzero on any failed check and does not print passwords or tokens.

Supply `AXXIUM_ADMIN_EMAIL` and `AXXIUM_ADMIN_PASSWORD` in the environment, or
set `AXXIUM_ADMIN_ENV_FILE` to a private env file. If neither is set, the
verifier looks for `~/.secrets/axxium/admin.env`.

The browser tour is `scripts/verify-axxium-local-tour.sh`. It captures the
unauthenticated login view and its Axxium label to ignored
`docs/verification/screenshots/`. The tour does not type a private password
through a command-line browser driver; the live verifier covers authenticated
delegation and context. The exact Google callback is registered in Google Cloud
Console and the private client JSON on this machine. Automated verification
checks that Axxium enables the flow; completing account consent needs a human
browser session.
