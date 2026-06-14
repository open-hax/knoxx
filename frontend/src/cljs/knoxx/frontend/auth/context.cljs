(ns knoxx.frontend.auth.context
  "Auth context access. The React context INSTANCE is shared with the
   remaining TS consumers (useAuth.ts reads the same instance), so the
   app wires the bridge's AuthContextInstance in at startup via
   set-context-instance!; node tests run on a locally created context."
  (:require ["react" :as react]
            [helix.hooks :as hooks]))

(defonce ^:private instance (atom nil))

(defn set-context-instance!
  "Install the shared React context instance (from the app bridge)."
  [ctx]
  (reset! instance ctx))

(defn context-instance ^js []
  (or @instance
      (reset! instance (react/createContext nil))))

(defn use-auth
  "The auth context JS object ({user actor org membership roleSlugs
   permissions isSystemAdmin authProvider loading error refresh logout})."
  ^js []
  (let [^js ctx (hooks/use-context (context-instance))]
    (when-not ctx
      (throw (js/Error. "use-auth must be used within auth-boundary")))
    ctx))
