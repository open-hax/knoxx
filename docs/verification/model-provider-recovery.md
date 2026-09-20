# Model providers after sandbox source recovery

The recovered adapters keep writing proposals and generated translation text separate from publication authority. Exact model contracts choose Ollama, OpenCode, or Transformers.js. The existing translation sink still owns admitted coordinates, candidate persistence, dispatch settlement and receipts; its durable-prefix retry path is unchanged.

Run the actual offline proof from the restored Foresight checkout with its shared dependencies and warmed public model cache:

```sh
FORESIGHT_MODEL_CACHE=/absolute/warmed/model-cache node knoxx/scripts/verify-local-model-providers.cjs
```

The script compiles its own current-checkout library, starts both real Node/Transformers.js services on loopback, generates a writing proposal and two translations through the production adapters, checks canonical receipt replay, and checks three real 384-dimensional embeddings. It uses local reference stores and the existing translation test emitter. It creates and removes only its own temporary content directory. It never calls a remote model or downloads weights. It requires Foresight's `devtools` package and the pinned Qwen 0.5B/MiniLM models already warmed in that cache. The full browser/Clio workflow remains a separate integration gate.

Fresh recovery checks on 2026-09-12:

- Focused guarded Node tests: 25 tests, 147 assertions, 0 failures, 0 errors. Compiler: 129 files, 0 warnings. The Node guard catches unhandled asynchronous errors even when a test harness requests an early exit 0.
- Production provider library: 122 files, 0 warnings; owned source/tests kondo: 0 errors, 0 warnings.
- Committed current-checkout verifier: compile plus actual offline run completed in 39.851 seconds, exit 0. This includes the final whole-response writing deadline change.
- Real offline execution: 17.340 seconds, exit 0, Qwen writing plus two generated translation candidates, exact receipt replay, and 3 × 384 finite MiniLM vectors. Identical inputs produced identical vectors; a different input produced a different vector. [The actual output is preserved](evidence/model-provider-offline-result.json).

The output is deliberately preserved without quality corrections. Qwen produced a useful one-sentence writing proposal, but its translations included commentary and confused a reviewed terminology example with the source. They are unreviewed candidates, not approved translations. Human revision remains necessary. A receipt proves structural admission of candidate bytes, not semantic accuracy or publication acceptance. SmolLM2-135M remains a selectable baseline; this proof used the larger, still small Qwen model.

Obstacles and remedies:

1. Scratch cleanup removed provider code and Qwen/MiniLM weights. The surviving portable writing contract, original Ollama orchestration, model pins and harness evidence guided reconstruction. Missing public weights were warmed at the original revisions; runtime inference then disabled remote loading. This is fresh reconstruction and verification, not a claim that historical commit bytes survived.
2. The initial new tests attempted to replace a protocol function. Compiled dispatch bypassed that replacement, producing 9 failures and 2 errors. Replacing the default client with a real `IHttpClient` fixture fixed interception without changing production behavior.
3. OpenCode's host configuration can be loaded after inline settings. The adapter refuses when `/etc/opencode` exists, supplies private HOME/XDG paths, allows only selected networking/trust/locale environment variables, denies tools and sharing, and disables supported ambient instruction/plugin sources. Tests execute an owned local process fixture; no new Zen request is claimed.
4. The generic HTTP client's timeout ends after response headers. Writing now also bounds the whole completion promise, including its response body. A delayed-body regression requires the classified 504 timeout. OpenCode separately has a process-group deadline and output limit.
5. Transformers.js does not enforce the translation JSON grammar. Its named HTTP transport wraps actual generated text in the one-field envelope and reports that provenance. The adapter requires this marker and an actual EOS completion. Its model instruction asks for text; Ollama/OpenCode retain their JSON instruction. Model-supplied tenant, split or attempt coordinates are never accepted.

Self-review anchors: `law/local_embedding.cljc` rejects incorrect identity, duplicate/missing indices, wrong dimensions and nonfinite values; the embedding facade remains optional until invoked. `law/writing_model.cljc` and the process extern refuse truncated/tool-bearing transcripts. `translation_agent_completion.cljs` owns provider differences; `translation_agent_structured_output.cljs` continues to enforce admitted bindings and deadlines before passing server-built pairs to the existing sink. The added local-provider integration test verifies actual sink settlement and replay with injected generation responses.
