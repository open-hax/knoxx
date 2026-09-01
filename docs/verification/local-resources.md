# Local resources, MongoDB, Proxx, and Ollama

Knoxx can run directly against the local Foresight workspace and the host
OpenPlanner/Proxx/Ollama stack without persisting another copy of their
credentials.

## Connection shape

- `WORKSPACE_ROOT` defaults to the directory containing the Knoxx checkout, so
  a checkout at `foresight/knoxx` exposes the Foresight tree as local resources.
- `MONGODB_URI` defaults to the application credential already injected into
  the running `openplanner-openplanner-1` container. The launcher rewrites only
  its network address to `127.0.0.1:27017` and enables direct replica-set access.
- `PROXX_BASE_URL` defaults to `http://127.0.0.1:8789`. The bearer token is read
  from `PROXX_ENV_FILE`, which defaults to the sibling `proxx/.env` checkout.
- `OLLAMA_BASE_URL` defaults to `http://127.0.0.1:11434`, with
  `gemma4:e4b` as the local model. Ollama is registered as an unauthenticated
  OpenAI-compatible provider at `/v1`.

Explicit connection values always win. Set `KNOXX_LOCAL_WORKSPACE_ROOT` to
override the launcher's workspace choice; this dedicated name prevents an
unrelated shell-level `WORKSPACE_ROOT` from silently selecting another checkout.
No discovered credential is printed or written to disk.

## Verify

```bash
./scripts/verify-local-resources.sh
```

The verifier is read-only. It checks the workspace and contracts paths, MongoDB
ping, Proxx health plus authenticated model discovery, Ollama health, and the
configured Ollama model. Every failed precondition exits non-zero.

## Run

```bash
./scripts/start-local-knoxx.sh
```

The launcher starts the backend shadow-cljs watcher and the development server
from this checkout. It defaults `KNOXX_DISABLE_EVENT_RUNTIMES=true`, so local
verification does not join Discord gateways or fire schedules/triggers. Stop it
with Ctrl-C; the launcher also terminates its watcher. On a clean checkout it
waits for both the backend entrypoint and the ClojureScript runtime artifact, so
the server never imports a half-written watch build.

To apply the same environment to another command:

```bash
./scripts/with-local-resources.sh <command> [args...]
```

Common overrides:

```bash
KNOXX_OPENPLANNER_CONTAINER=my-openplanner \
PROXX_ENV_FILE=/path/to/proxx/.env \
OLLAMA_DEFAULT_MODEL=gemma4:e4b-128k \
./scripts/verify-local-resources.sh
```
