(fork-tax-state
  (timestamp "2026-06-03T00:00:00Z")
  (repo "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx")
  (branch "pi/fork-tax/20260529T022118Z-main-softreset-all-dirt-knoxx")
  (remote "origin" "git@github.com:open-hax/knoxx.git")
  (snapshot-base-head "283d28f1290f8a282c1de2200de277d12c76b3cb")
  (scope "Backend error observability, local password auth, trigger/action task prompt migration, async/await modernization, event normalization, provider tool disable removal, frontend auth, CI/CD workflows")
  (changes
    (frontend-local-auth
      "Frontend LoginPage/SignupPage add local password auth UI behind feature flag"
      "shadow-cljs.edn dev proxy switched from knoxx-backend container DNS to 127.0.0.1")
    (backend-local-password-auth
      "Auth routes: local password signup/login handlers with scrypt hashing"
      "DB policy: local-password-auth-record! query, upsert-actor-credential-for-context! storage")
    (error-observability-system
      "New domain/error_observatory.cljs: centralized error logging with safe JSON context"
      "New check-error-boundaries.mjs script + error-boundary-allowlist.json baseline"
      "package.json: error-boundaries:check and error-boundaries:inventory scripts")
    (error-surface-integration
      "Discord source: observe-boundary! for fetch-channel/list-channels failures"
      "Event dispatch: trigger-failure-result surfaces failed action results as 500 data"
      "Source runtime: skip-result logging, dispatch/start-source error capture"
      "Event runtime: observe-promise! for source start failures"
      "HTTP infra: 500 error-response! logs to observatory with context"
      "App routes: log-and-record-async-spawn-error! for chat turn failures"
      "Tools routes: trigger-fire-response! returns 500 when trigger actions fail")
    (agent-runtime-empty-turn
      "Agent turn: empty-turn-output? detection, finalize-empty-turn-output! with run_failed event"
      "Runner: log-and-record-async-spawn-error! records run failure events"
      "Service: uses log-and-record-async-spawn-error! for queued turn failures"
      "Models routes: redis-run-fallback reconstructs run from events + session after restart")
    (provider-tool-disable-removal
      "Removed provider-tool-disabled-models set and provider-tools-enabled-for-model?"
      "Session: always exposes policy-resolved tools, delegates model compatibility to Proxx"
      "Test: removed provider-tools-are-disabled-for-known-incompatible-models test")
    (trigger-action-task-prompt-migration
      "Action registry: extracts :trigger/task into :action/with :task"
      "Start agent session: action-task-input resolves trigger/action task text with source metadata"
      "Start agent session: render-start-message labels action vs deprecated agent task prompt"
      "Trigger normalize: preserves :trigger/task field"
      "Runner/turn: pass task-source, rendered-task-prompt, deprecated-agent-task-fallback through spec"
      "Turn: emit-action-task-rendered-event! broadcasts task audit event"
      "Contracts: moved task text from agent :prompts :task to trigger :trigger/task")
    (event-normalization
      "Event normalize: dotted JSON event type preservation (:discord.message)"
      "New tests: event_normalize_test.cljs, live_contract_policy_test.cljs")
    (async-await-modernization
      "Domain: label/audio, sandbox-container, session-mycology converted from Promise chains"
      "Infra core: initialize-mcp-gateway!, start-background-services!, prewarm-sdk-runtime!, start!"
      "Routes: MCP, models, resources, discord-scan, tools, proxy all converted"
      "Active items sort: active-item-time-ms normalizes ISO string vs numeric ms")
    (ci-cd-workflows
      "New .github/workflows/deploy-production.yml and deploy-staging.yml")
    (process-artifacts
      "Kanban: epic addendum for task prompt migration, lint task progress updates"
      "New kanban task: knoxx-trigger-action-task-prompt-migration.md"
      "New docs note: 2026.06.03.09.09.14.md"
      "Receipts: 14 new entries documenting verification sessions"))
  (concurrent-dirt
    "none; all working tree changes are owned by this snapshot")
  (blocked-paths ())
  (verification
    (secret-heuristic-scan "passed: no literal private keys / tokens / api-keys in staged additions")
    (backend-server-compile "passed: pnpm -C backend typecheck => 0 warnings")
    (backend-tests "passed: pnpm -C backend exec shadow-cljs compile test => 456 tests, 1341 assertions, 0 failures, 0 errors")
    (error-boundaries-check "passed: pnpm -C backend error-boundaries:check"))
  (destructive-cleanup false)
  (tag "pi/fork-tax/20260603T000000Z/knoxx-error-observability-auth-task-prompt-async"))
