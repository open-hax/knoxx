# Verify the public Knoxx privacy notice

Run the script from the Knoxx checkout whose policy you intend to verify.
Point it at a running frontend built or served from that checkout. The
anonymous response at `/privacy.html` must match
`frontend/public/legal/privacy.html` byte for byte. The frontend build emits the
file as `frontend/dist/bridge/legal/privacy.html`, and production nginx maps the
public URL to that asset. This catches a stale or different frontend.

Use an explicit origin with no path or credentials. The HTTP checks need
`curl` and Python 3. The browser tour also needs `agent-browser` and an
installed browser. It starts an isolated signed-out profile. For a local
browser tour, the frontend must be able to load its app bundle and render
the signed-out page.

```bash
scripts/verify-public-privacy.sh http://127.0.0.1:5173 --browser
scripts/verify-public-privacy.sh https://knoxx.promethean.rest --browser
```

The remote command checks the named deployed origin and will fail until it
serves this checkout's policy. Omit `--browser` to run just the HTTP checks.

- The homepage and policy must each serve distinct HTML with HTTP 200 before
  sign-in. An unrelated route may fall back to the app shell or return 404,
  but it must not serve the privacy document.
- The initial homepage HTML must describe Knoxx, Google sign-in and Drive
  archiving, with a **Privacy Policy** link in `#root` that points to
  `/privacy.html`.
- The browser tour waits for the signed-out app to render, follows the visible
  link and checks the exact URL and page title. The two PNGs are written to
  `docs/verification/screenshots/`. Set `KNOXX_SHOT_DIR` to keep them elsewhere.

The script uses public requests and creates no Knoxx records. It cleans up its
temporary HTTP files and browser profile. Its final warning identifies the
separate verification for Google OAuth consent, granted scopes and the archive
worker.
