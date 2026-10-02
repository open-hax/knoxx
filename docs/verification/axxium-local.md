# Local Axxium authority in Knoxx

Run `node scripts/verify-axxium-local.mjs` from this Knoxx checkout. The script
first checks that PM2 is serving this checkout and the sibling Axxium checkout.
It uses the private local administrator created by Axxium's bootstrap, so no
reviewer has to handcraft fixtures. It rejects anonymous access and bad
credentials, confirms Knoxx disables local signup, signs in through Axxium,
checks the delegated user context and frontend proxy, and logs out its session.
It exits nonzero on any failed check. The script does not print passwords or
tokens.

The browser tour is `scripts/verify-axxium-local-tour.sh`. It captures the
unauthenticated login view and its Axxium label to ignored
`docs/verification/screenshots/`. The tour does not type a private password
through a command-line browser driver; the live verifier covers authenticated
delegation and context. The exact Google callback is registered in Google Cloud
Console and the private client JSON on this machine. Automated verification
checks that Axxium enables the flow; completing account consent needs a human
browser session.
