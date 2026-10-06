# Verify native music generation

This hotfix restores the existing `music.generate` result contract. Node's
promisified `execFile` resolves to `{stdout, stderr}`; the engine metadata is
JSON in **stdout**. The native engine remains
`backend/scripts/synthesize-music.mjs`. The backend Docker context now copies
that existing file into `/app/scripts/synthesize-music.mjs`.

## Source proof

From the checkout under review, with backend dependencies installed:

```bash
scripts/verify-native-music.sh --source
```

The script prints the checkout's Git revision and dirty state, compiles and executes only
`knoxx.backend.extern.native-music-test`, and requires a nonempty test summary
with zero failures and zero errors. A compiler exit code alone cannot pass it.
The test invokes the real native engine through `music-generate!`, checks the
RIFF/WAVE file and nonzero PCM, and checks the decoded metadata and workspace
paths. It also proves Node's real process result shape and rejects malformed
engine stdout independently of stderr. Its WAV fixture directory is removed.

This mode verifies source execution. It emits `WARN` that deployment is
unverified. It starts no server and requires no credentials. The backend must
be the compiler's working directory; the wrapper selects it automatically.
Node must satisfy the backend's declared `>=22.19.0` requirement. This worktree
was verified with Node 24.14.1 and Java 21.

## Live proof after an operator deploys this revision

Build the production backend from the committed revision being reviewed. The
operator must label the resulting image with
`org.opencontainers.image.revision=<full Git SHA>` when building it. Supply an
already authorized MCP bearer through `KNOXX_MCP_TOKEN`, using the existing
credential mechanism. Neither verification script reads `.env` or prints the
token.

```bash
scripts/verify-native-music.sh --live-container \
  knoxx-social-local-backend-1 http://127.0.0.1:18881
```

The script aborts before any tool call unless all these facts are established:

- The checkout is committed and the running image revision equals its HEAD.
- Docker's published `8000/tcp` port selects this container, whose command is
  `node dist/server.js` and working directory is `/app`.
- Every deployed JavaScript artifact under `dist/`, plus the synthesis script,
  hashes identically to the local build. Runtime code is not replaced by mounts
  and its timestamps precede container startup.
- Inside that validated container, the effective workspace root follows the
  backend's precedence: `WORKSPACE_ROOT`, `WORKSPACE_PATH`,
  `KNOXX_WORKSPACE_ROOT`, then `/app/workspace`. Only this nonsecret path is
  printed. Its real path must be absolute, an existing writable directory, and
  inside a writable durable bind mount or Docker volume.
- The operator supplied an MCP token.

It then checks unauthenticated `POST /mcp` returns 401, finds the granted
`music_generate` (canonical `music.generate`) tool, and executes this input:

```json
{
  "spec_json": {
    "bpm": 120,
    "duration": 0.5,
    "tracks": [{
      "instrument": "synth",
      "waveform": "sine",
      "notes": [{"note": "A4", "time": 0, "duration": 0.25}]
    }]
  },
  "output_path": "Music/generated/.verify-native-music-<unique-id>/proof.wav"
}
```

The unique directory is generated under the validated workspace root; the tool
receives the workspace-relative `Music/generated/.../proof.wav` path. The same
derived root is used for reading the WAV and cleaning up. Expected native metadata is
`ok=true`, `durationSec=0.5`, `sampleRate=44100`, `channels=2`, and
`samples=22050`, plus matching `workspace-path` and `absolute-path` fields.
The script reads the actual WAV from the container, checks its header against
the metadata, its PCM byte count and nonzero audio, and prints its SHA-256.
It removes only its dedicated directory in `finally` and on SIGINT/SIGTERM.
Cleanup failure exits nonzero. SIGKILL cannot run cleanup handlers.

The cephalon deployment's `WORKSPACE_ROOT=/state/workspace` is supported through
this derivation. The script refuses an ephemeral container workspace. It checks the native tool over the
existing authenticated MCP surface. There is no generic music execution route
under `/api/tools` in this source revision. It does not publish or invoke an
agent, and it does not start, rebuild, deploy or restart a running service.

## Recorded hotfix evidence

Base server source revision: `3409977bca4ba35e09967f9a99d50867a679a73c`.
RED test commit: `1afb85991bd5d7ff4a6134c15da0dff77d98e15d`.

- RED: full `shadow-cljs compile test` ran 1,847 tests / 9,102 assertions,
  with six failures and zero errors. All six failures were native generation
  metadata/path assertions after a real nonzero RIFF/WAVE file was written.
  Error: `"[object Object]" is not valid JSON`. Shadow exited zero; the counter
  guard rejects this output.
- GREEN native source proof: three tests / 18 assertions, zero failures and
  errors, zero compiler warnings. The real engine was loaded and executed from
  this isolated checkout.
- The initial adapter check caught an unsupported Malli `:number` schema; it
  was replaced with the supported `number?` predicate before the GREEN proof.
- Full suite, production compile/release and lint results are recorded in the
  [gate evidence](native-music-gates.md). A red required gate remains a blocker.
- A live server and rebuilt Docker image are not verified by source evidence.
  The audit performed no deployment or restart.

## Dependency setup used here

`pnpm -C backend install --frozen-lockfile --offline --ignore-scripts` populated
a real worktree-local `node_modules` using the existing cache. The manifest's
`link:../../openplanner/packages/openplanner-sdk` dependency was satisfied by a
sibling link to the existing built SDK at
`/home/err/spaces/foresight/openplanner`, matching the layout used by CI. No
package manifest, lockfile, original source, or SDK source was rewritten.
