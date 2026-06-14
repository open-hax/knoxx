(ns knoxx.backend.domain.action.interpreter
  "Action interpreter: delegates to the contract-runtime action interpreter.

   This is a thin wrapper that provides backward compatibility for existing
   Knoxx callers. The actual implementation lives in the extracted
   contract-runtime package.

   The config map must contain :contract-runtime/deps (see
   knoxx.backend.contract-runtime-deps/build-deps)."
  (:require [open-hax.contract-runtime.action.interpreter :as core-interpreter]))

(defn resolve-scope-decl
  "Resolve an :action/scope declaration {:actions [...] :filters [...]
   :stores [...]} into a flat scope map keyed by the declared ids."
  [config scope-decl]
  (core-interpreter/resolve-scope-decl config scope-decl))

(defn execute!
  "Execute the action facet of a resource with scope injected into ctx.
   Returns a Promise of the action result."
  ([ctx action]
   (core-interpreter/execute! ctx action))
  ([ctx action redirects]
   (core-interpreter/execute! ctx action redirects)))
