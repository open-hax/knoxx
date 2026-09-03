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

   ;; Writes that can replace existing state, so destructive. Both are
   ;; idempotent: repeating with the same arguments converges on the same end
   ;; state rather than accumulating.
   ;;
   ;; save_translation upserts on a tenant-scoped identity — segments.cljs
   ;; upsert-segment! is a findOneAndUpdate with $set and :upsert true, so a
   ;; segment with the same key is overwritten when its content differs.
   ;;
   ;; create_new_file writes with fs.writeFile and never checks for an existing
   ;; path, so despite the name it truncates whatever is already there. Making
   ;; it fail on an existing path instead would be a behaviour change, and a
   ;; product decision; until then the honest hint is destructive.
   "save_translation" {:readOnlyHint false :destructiveHint true
                       :idempotentHint true :openWorldHint false}
   "create_new_file"  {:readOnlyHint false :destructiveHint true
                       :idempotentHint true :openWorldHint false}

   ;; Source-revision-derived paths are create-only. Equal replay adds nothing;
   ;; conflicting bytes are refused instead of replacing the existing draft.
   "save_publication_draft" {:readOnlyHint false :destructiveHint false
                             :idempotentHint true :openWorldHint false}

   ;; Genuinely append-only: the event id is "claim:" plus a fresh randomUUID
   ;; per call, so each call adds a claim and repeating adds another.
   "push_claim"       {:readOnlyHint false :destructiveHint false
                       :idempotentHint false :openWorldHint false}})

(defn for-tool
  "Declared annotations for a tool name, or nil when none are declared.

   nil is deliberate: it leaves the client on its conservative defaults rather
   than asserting something unverified."
  [tool-name]
  (get declared (str tool-name)))
