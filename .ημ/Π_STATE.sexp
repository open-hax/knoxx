(fork-tax-state
  (timestamp "2026-06-08T00:00:00Z")
  (repo "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx")
  (branch "fix/frontend-es2022-lib")
  (remote "origin" "git@github.com:open-hax/knoxx.git")
  (base "origin/fix/frontend-es2022-lib")
  (scope "Backend infra fixes, frontend ES2022 lib + source doc refactor, contract updates, kanban task hygiene, ingestion clj-kondo imports")
  (changes
    (backend-infra
      "Modified backend/src/cljs/knoxx/backend/infra/agent/turn.cljs"
      "Modified backend/src/cljs/knoxx/backend/infra/openplanner/memory.cljs"
      "Modified backend/src/cljs/knoxx/backend/infra/stores/mongo_memory_sessions.cljs"
      "Modified backend/src/cljs/knoxx/backend/infra/stores/mongo_session_store.cljs"
      "Modified backend/src/cljs/knoxx/backend/infra/stores/mongo_session_titles.cljs")
    (contracts
      "Modified contracts/agents/ussyverse_social_creative.edn"
      "Modified contracts/agents/ussyverse_social_replies.edn"
      "Modified contracts/capabilities/cap_music.edn"
      "Modified contracts/capabilities/cap_music_composition.edn"
      "Modified contracts/fork-tales/MANIFEST.md"
      "Modified contracts/fork-tales/agents/fork_tales_deep_composer.edn"
      "Modified contracts/fork-tales/agents/fork_tales_instrumentalist.edn"
      "Modified contracts/roles/fork-tales-composer.edn")
    (frontend
      "Modified frontend/package.json"
      "Modified frontend/shadow-cljs.edn"
      "Modified frontend/src/bridge/index.ts"
      "Modified frontend/src/cljs/knoxx/frontend/core.cljs"
      "Modified frontend/src/components/admin-page/UsersMembershipsSection.tsx"
      "New frontend/src/components/workspace-context/utils.test.ts"
      "Modified frontend/src/components/workspace-context/utils.ts"
      "Modified frontend/src/lib/api/admin.test.ts"
      "Modified frontend/src/lib/api/admin.ts"
      "Deleted frontend/src/lib/document-links.test.ts"
      "Deleted frontend/src/lib/document-links.ts"
      "Modified frontend/src/lib/types.ts"
      "Modified frontend/src/pages/AdminLayout.tsx"
      "Modified frontend/src/pages/SourceDocPage.tsx"
      "Deleted frontend/src/pages/source-doc-page/ForumThreadView.tsx"
      "New frontend/src/cljs/knoxx/frontend/lib/"
      "New frontend/src/cljs/knoxx/frontend/pages/source_doc/"
      "New frontend/src/pages/SourceDocPage.test.tsx"
      "New frontend/test/")
    (kanban
      "Modified kanban/tasks/*.md (33 task files)")
    (ingestion-clj-kondo
      "New ingestion/.clj-kondo/imports/ (malli, next.jdbc, babashka/fs, rewrite-clj configs)"))
  (concurrent-dirt
    "none; all working tree changes are owned by this snapshot")
  (blocked-paths
    ".claude/ (agent runtime state — scheduled_tasks.lock)")
  (verification
    (skipped "No backend test command run this session; prior Π commit (4a241e71) verified tests passing"))
  (destructive-cleanup false)
  (deployment
    "pending: branch fix/frontend-es2022-lib is ahead of origin by 2 commits + this Π commit"))
