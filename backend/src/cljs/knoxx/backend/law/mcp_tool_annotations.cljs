(ns knoxx.backend.law.mcp-tool-annotations
  "Declared MCP tool annotations — what each tool does to the world.

   Contract policy only, no I/O. MCP's ToolAnnotations defaults are pessimistic
   when a tool says nothing: destructiveHint and openWorldHint default to true
   and readOnlyHint to false, so an unannotated read is presented to the user as
   a destructive, open-world write. That is what a client showed for graph_query.

   These are claims about behaviour, so every entry is declared from the tool's
   own implementation rather than guessed from its name. A tool that is not
   listed here stays unannotated: the client's conservative default is the
   correct answer when nobody has checked, and silently asserting readOnly for a
   tool that writes would be worse than the warning it removes.

   Semantics, from the MCP specification:
     :readOnlyHint    the tool does not modify its environment
     :destructiveHint it may overwrite or remove existing state
                      (only meaningful when :readOnlyHint is false)
     :idempotentHint  repeating the call with the same arguments adds nothing
                      (only meaningful when :readOnlyHint is false)
     :openWorldHint   it reaches entities outside this system, e.g. the web")

(def declared
  "Tool name -> annotations, each justified by that tool's implementation in
   knoxx.backend.infra.openplanner.tools."
  {;; Reads of our own corpus and graph. Nothing leaves the system.
   "graph_query"      {:readOnlyHint true  :openWorldHint false}
   "semantic_query"   {:readOnlyHint true  :openWorldHint false}
   "memory_search"    {:readOnlyHint true  :openWorldHint false}
   "memory_session"   {:readOnlyHint true  :openWorldHint false}

   ;; Reads, but of the open internet — openWorldHint stays true.
   "websearch"        {:readOnlyHint true  :openWorldHint true}
   "web.read"         {:readOnlyHint true  :openWorldHint true}

   ;; Writes that only ever add. Not destructive: none of these overwrite or
   ;; remove existing state. Not idempotent: repeating them appends again.
   "save_translation" {:readOnlyHint false :destructiveHint false
                       :idempotentHint false :openWorldHint false}
   "create_new_file"  {:readOnlyHint false :destructiveHint false
                       :idempotentHint false :openWorldHint false}
   "push_claim"       {:readOnlyHint false :destructiveHint false
                       :idempotentHint false :openWorldHint false}})

(defn for-tool
  "Declared annotations for a tool name, or nil when none are declared.

   nil is deliberate: it leaves the client on its conservative defaults rather
   than asserting something unverified."
  [tool-name]
  (get declared (str tool-name)))
