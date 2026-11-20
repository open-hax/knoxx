# Native invite verification

Run this from the Knoxx checkout with the backend dependencies installed and a
MongoDB 7 executable available. The script prints the checkout path and commit
and compiles that checkout before testing.

```bash
./scripts/verify-native-invites.sh /absolute/path/to/mongod
```

The test starts its own MongoDB database and HTTP listener on loopback. It
seeds a local administrator session using Knoxx's session code and calls the
same invite endpoints the application serves. No existing database, account,
or provider credential is needed. The fixture removes the database, temporary
actor contracts, and listener when it exits.

The HTTP checks cover authenticated invite creation, a refused unauthenticated
request, email matching, successful redemption with a session cookie, and
single use under concurrent requests. The winning cookie authenticates a fresh
request as the invited member with the default role. The normal writer creates
an ISO string expiry. The isolated fixture also seeds a future BSON Date,
expired values, and invalid values to check how previously stored rows behave.
