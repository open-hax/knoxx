(ns knoxx.frontend.e2e.ava-runner
  "AVA registrations for shadow-cljs compiled Puppeteer e2e tests.

   The AVA CLI loads the compiled node-script output. Runtime dependencies live
   in frontend/e2e/node_modules and are discovered via NODE_PATH so this build
   does not force the main pnpm workspace to absorb browser-test tooling." )

(defn- require-runtime [module-name]
  (js/require module-name))

(def ^:private test-fn
  (require-runtime "ava"))

(def ^:private support
  (require-runtime (str (.cwd js/process) "/e2e/knoxx_e2e_support.cjs")))

(defn- then-assert
  [promise assertions]
  (.then promise assertions))

(defn main []
  ((.-serial test-fn)
   "agents audit tab bridges CLJS shell, TS session list, and TS chat UI"
   (fn [t]
     (then-assert
      (.runAuditBridgeSmoke support)
      (fn [^js result]
        (.true ^js t (.-hasAuditSessions result) "audit sessions section is mounted in the side panel")
        (.true ^js t (.-hasUnifiedActive result) "active runs appear in the unified audit session list")
        (.true ^js t (.-hasUnifiedHistory result) "history rows appear in the unified audit session list")
        (.true ^js t (.-hasTrigger result) "selected agent trigger cards still render from CLJS contract data")))))

  ((.-serial test-fn)
   "audit session cards resume historical transcript into the chat pane"
   (fn [t]
     (then-assert
      (.runAuditSessionResume support)
      (fn [^js result]
        (.true ^js t (.-resumedUserMessage result) "memory user message is rendered by ChatWorkspacePane")
        (.true ^js t (.-resumedAssistantMessage result) "memory assistant message is rendered by ChatWorkspacePane")))))

  ((.-serial test-fn)
   "contracts tab validates and saves CLJS editor state through TS bridge API mocks"
   (fn [t]
     (then-assert
      (.runContractsEditorSaveValidate support)
      (fn [^js result]
        (.true ^js t (.-validationPosted result) "validate API receives the edited contract EDN")
        (.true ^js t (.-savePosted result) "save API receives the edited contract EDN")
        (.true ^js t (.-editedIdVisible result) "CLJS editor keeps the edited contract id visible during save flow"))))))
