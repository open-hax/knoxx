(ns knoxx.backend.extern.mcp-sdk
  "Native MCP SDK schema, registration and response-lifetime boundary."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.actor.acting :as actor-acting]
            [knoxx.backend.law.mcp-tool-annotations :as tool-annotations]))

(declare typebox->zod-shape)

(defn- tool-execute! [^js tool params] (.execute tool "mcp" params nil nil nil))

(defn- apply-zod-description [^js schema-node ^js schema-json]
  (let [description (some-> (aget schema-json "description") str str/trim not-empty)]
    (if description (.describe schema-node description) schema-node)))

(defn typebox->zod-node
  "Convert one TypeBox schema node to a zod schema. Pure."
  [^js z ^js schema-json]
  (let [schema-type (aget schema-json "type")
        node (case schema-type
               "string"  (.string z)
               "number"  (.number z)
               "integer" (-> (.number z) (.int))
               "boolean" (.boolean z)
               "array"   (.array z (or (typebox->zod-node z (aget schema-json "items")) (.any z)))
               ;; A nested object must become a zod schema, not the bare field
               ;; shape typebox->zod-shape builds. Returning the shape gave the
               ;; parent a plain JS object with no zod methods, so marking the
               ;; field optional threw "fschema.optional is not a function" and
               ;; the whole tool registration failed — every MCP POST that
               ;; registers tools answered 400.
               "object"  (.object z (or (typebox->zod-shape z schema-json) (js-obj)))
               (.any z))]
    (-> node
        (apply-zod-description schema-json)
        ((fn [n] (if-let [min (aget schema-json "minimum")] (.min n min) n)))
        ((fn [n] (if-let [max (aget schema-json "maximum")] (.max n max) n))))))

(defn typebox->zod-shape
  "Convert a TypeBox object schema to a zod *field shape* — the map of field
   name to zod schema that registerTool wants. Not itself a zod schema; a
   nested object goes through typebox->zod-node, which wraps it. Pure."
  [^js z ^js schema-json]
  (let [properties   (or (aget schema-json "properties") (js/Object.))
        required-set (into #{} (map str) (array-seq (or (aget schema-json "required") (js/Array.))))
        entries      (.entries js/Object properties)]
    (when (seq (array-seq entries))
      (reduce (fn [shape entry]
                (let [fname  (aget entry 0)
                      fschema (typebox->zod-node z (aget entry 1))
                      final   (if (contains? required-set (str fname)) fschema (.optional fschema))]
                  (aset shape fname final)
                  shape))
              (js-obj)
              (array-seq entries)))))

(defn- tool-config-js
  "The MCP registerTool config for one tool object. Pure interop assembly."
  [z ^js tool name]
  (let [shape  (or (when z (typebox->zod-shape z (or (aget tool "parameters") (js/Object.)))) (js-obj))
        config (clj->js {:description (str (or (aget tool "description") (aget tool "label") name))
                         :inputSchema shape})]
    (when-let [title (some-> (or (aget tool "label") (aget tool "title")) str str/trim not-empty)]
      (aset config "title" title))
    ;; A tool's own annotations win, else the declared table.
    ;; See law.mcp-tool-annotations for why absence is bad.
    (if-let [annotations (aget tool "annotations")]
      (aset config "annotations" annotations)
      ;; name may be sanitized (web.read -> web_read).
      (when-let [declared (or (tool-annotations/for-tool name)
                              (tool-annotations/for-tool (aget tool "originalName")))]
        (aset config "annotations" (clj->js declared))))
    (when-let [meta (aget tool "_meta")]
      (aset config "_meta" meta))
    config))

(defn register-tools!
  "Register every granted tool on a fresh MCP server, bound to one actor.

   Each handler runs inside that actor's scope, so domain.actor.credentials
   resolves the actor without the MCP surface having to impersonate an agent
   spawn. The scope is entered per call rather than per request because the SDK
   invokes handlers itself: wrapping the request would put the awaits that
   matter outside it.

   A nil actor-id still enters a scope — one that says there is no actor. It has
   to: without it a credential read falls back to the process-global
   agent-context, and an actor-less token would borrow whatever actor a
   concurrent agent turn is running as. See actor-acting/run-as!."
  [^js server z tools acting]
  (doseq [^js tool (array-seq tools)]
    (when-let [name (some-> (aget tool "name") str str/trim not-empty)]
      (.registerTool server name
                     (tool-config-js z tool name)
                     (fn [params]
                       (actor-acting/run-as! acting #(tool-execute! tool params)))))))

(defn close-when-response-ends!
  "Tear down a per-request MCP server once its response is finished.

   Stateless mode requires a fresh server and transport per exchange, so without
   this every POST leaves both behind for the process lifetime.

   Closing the *server* is enough and closing the transport as well would be a
   double close: in SDK 1.29 McpServer.close -> Server.close ->
   Protocol.close -> transport.close.

   Bound to the response's close event rather than run after handle-request!
   returns, because a response may still be streaming when the handler resolves;
   closing then would cut it off. Failures are swallowed deliberately — the
   client already has its response, and a teardown error must not replace it."
  [^js raw-res ^js server]
  (.once raw-res "close"
         (^:async fn []
           (try
             (await (.close server))
             (catch :default _ nil)))))
