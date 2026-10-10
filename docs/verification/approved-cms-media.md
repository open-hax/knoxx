# Approved CMS artwork and music

Run the native CMS publication verifier from the checkout under review:

```bash
KNOXX_VERIFY_ART_FILE=/absolute/path/to/Sutured_Signal.svg \
  scripts/verify-approved-cms-media.sh --check
```

The artwork must be the existing selected `Sutured_Signal.svg` with SHA-256
`889bdd17806386fd1915f190e0d3a09960c9b567d561b3f9eab78f402caeb009`.
Use the staged Website `/graphics/Sutured_Signal.svg` asset or its original
Calliope source with those exact bytes. The verifier copies those bytes into its
own public reader. It never downloads an image, creates substitute art, ingests
an audio file or claims that the CMS took custody of the artwork bytes.

Prerequisites are Node 24, pnpm, Clojure/Java, Python 3, `timeout`, `rg`, and
installed backend dependencies. Compilation is bounded to four minutes and any
warning, missing compiler result or nonzero exit fails the run. The verifier
starts a real Fastify CMS and separate public reader on ephemeral loopback
ports. It owns one temporary directory, process, organization, document ledger,
resource graph and publication root. `EXIT`, `INT` and `TERM` terminate only its
process and remove that entire directory. It never attaches to an existing
deployment, PM2 process, Mongo database or model provider. Existing documents
and publications stay outside its ownership.

The fixture's native CMS and publication route handlers are production code.
The auth-context seam supplies a disposable session principal. Translation
evidence and split stores are explicitly process-local empty verification
stores; these are not a production persistence fallback. The public reader
validates the native manifest and serves only a committed route's artifact and
the exact supplied selected image. Its HTML wrapper is a disposable reader,
not a claim that the Website deployment has been qualified.

Before writing, the script verifies its physical Git root and full HEAD, hashes
the four renderer paths and fixture adapter, hashes the freshly compiled
artifact, and compares that proof with the live process. The process rechecks
the source and compiled bytes before listening. This prevents a pass against
another checkout or a stale fixture build. Source and checkout identity are
compared before and after compilation; a concurrent source change fails before
seeding. Uncommitted source is bound by its
hash; HEAD alone is never presented as the changed source identity.

The runnable checks prove these outcomes:

- Every private surface rejects an anonymous request, including create, read,
  history, save, publication topology/state, reconciliation, receipts and proof.
  The public reader does not expose the private CMS API.
- Native `POST /api/cms/documents` retains exact Markdown bytes and emits
  withheld English/Spanish intents. Saving the document alone serves no route.
- Native `PATCH /api/cms/publications/intents/:id` with `{"state":"published"}`
  requests desired state. Native `POST /api/publications/reconcile` materializes
  the English source and returns an actual materialized receipt. The verifier
  obtains each identity and public path from the native topology response.
- Anonymous public readback displays exactly one real selected artwork image
  and a canonical `We Are The Place` Suno listen destination. The served artwork
  digest equals the supplied original digest.
- Raw HTML, unsafe/private URLs and fenced Markdown examples stay literal.
  No iframe, script, autoplay or automatic remote player appears.
- Spanish reconciliation without translation and review evidence is blocked;
  it exposes no Spanish route and changes neither the English artifact nor
  manifest. The source locale's existing `:none` review policy is not described
  as whole-output approval.
- A native CMS revision changing only unsigned `metadata.blocks` retains its
  history but cannot change publication bytes. Reconciliation converges to a
  noop rather than displaying a metadata-only injected image or track.

Each passing check explains the fact it establishes. Any failure prints its
reason and exits nonzero; teardown still runs. There is no successful retained
fixture mode. `--serve` pauses after verification so a human can inspect the
live result; Ctrl-C tears it down with signal exit status 130.

For a repeatable public browser tour and screenshots:

```bash
KNOXX_VERIFY_ART_FILE=/absolute/path/to/Sutured_Signal.svg \
  scripts/verify-approved-cms-media.sh --browser
```

This mode requires `agent-browser` and an installed browser. It captures the
published document and its reload after the retained metadata revision in
gitignored `docs/verification/screenshots/approved-media-<run>/`. It checks that
the selected image has loaded and the page contains no active player/script.
It closes its browser session on exit. For a Cua or manual tour, use `--serve`,
open the printed public URL, inspect the image and canonical listen link, and
capture the page before stopping the owned fixture. The tour does not click
through to Suno: this run proves a selected listening destination, not current
remote playback.

For the native CMS source-selection step followed by public artwork/music
screenshots, use the explicit tour entry with a prebuilt native frontend:

```bash
KNOXX_VERIFY_ART_FILE=/absolute/path/to/Sutured_Signal.svg \
KNOXX_VERIFY_FRONTEND_DIST=/absolute/path/to/knoxx/frontend/dist \
  scripts/verify-approved-cms-media-tour.sh --browser
```

The tour reads that frontend build without rewriting it. It first opens the
disposable session, selects the native CMS document and captures the retained
Markdown source, then opens the anonymous public page. The frontend bundle's
source/image identity must be qualified separately; serving it here is not
deployment proof. The CMS editor's image preview origin gap remains visible.
For Cua, run this tour with `--serve`, open the `start-url` in its private
run-owned receipt, then visit its `origin` plus `/cms` and select the document.
Capture the native CMS source and the printed public document before stopping
the owned fixture. No browser run or screenshot is implied by script syntax
qualification; execution evidence must identify the actual browser and run.

## Production publication handoff

The production document syntax supports a standalone Markdown media reference
per paragraph, separated from adjacent prose by a blank line:

```markdown
Selected artwork.

![Sutured Signal](/graphics/Sutured_Signal.svg)

[We Are The Place](https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b)
```

The image must already exist on the public Website origin. A reference's URL
shape does not establish existence, asset ownership or byte custody. Labels are
escaped. Unknown/unsafe references remain literal. Same-origin `/music/` audio
links have generic controls with `preload="none"`; the current selected corpus
has zero locally staged tracks, so this feature supplies public Suno links.
No iframe or external player autoload is introduced. Ordinary Markdown prose
continues using the publication renderer's escaped paragraph presentation.

For actual publication, create separately titled art and music selection
documents through the native CMS, with `visibility: "review"` and `parents: []`.
Use `GET /api/cms/publications/documents` to recover the server-returned English
and Spanish publication identities and paths. Request desired state with the
state PATCH body above, then explicitly reconcile English. Require a
materialized/noop receipt, native manifest, exact artifact bytes and a real
Website readback; a save or state edit is not publication proof. The documents
own the reviewed reference text and publication artifacts. Their referenced
artwork remains separately staged media, and the music destinations remain
public Suno song links.

Spanish still requires the real native translation dispatch, all effective
split reviews, exact whole-output approval and reconciliation. Target labels
may translate; asset URLs and canonical song identities must stay intact.
`publication-runtime-test` separately rejects target media text whose bytes do
not match the admitted translation receipt. This disposable verifier does not
invent a translation or approval to simulate that provider journey. Use the
[source-locale walkthrough](cms-source-locale-review.md) and
[split-review tour](../../scripts/verify-translation-split-review-tour.sh), then
capture real provider/review/publication evidence in the isolated demo before
deployment qualification.

The private CMS editor's relative image preview resolves on the CMS origin,
while public document images resolve on the Website origin. This verifier's
public readback does not qualify private editor preview. Password login, Mongo
durability, real provider output, deployed image/source identity, Website
integration and current Suno audio playback are printed as visible boundaries;
they need their own live evidence.
