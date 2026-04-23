(ns knoxx.backend.macros)

(defmacro defroute
  "Register a Fastify route whose handler is a single workflow fn taking a
   context map.

   Expands to:
   - a thin Fastify handler
   - delegation to a local `run-route!` function supplied by the caller ns

   Signature:
   (defroute app method path guards workflow-fn)"
  [app method path guards workflow-sym]
  `(~'route! ~app ~method ~path
             (fn [req# reply#]
               (~'run-route! ~guards ~workflow-sym req# reply#))))

(defmacro defn-async
  "Compatibility macro for async workflow fns.

   Today it expands to a plain defn so callers can return either nil or a
   Promise. Keeping the macro lets us evolve toward a lowered js-await DSL
   without rewriting call sites."
  [name args & body]
  `(defn ~name ~args ~@body))
