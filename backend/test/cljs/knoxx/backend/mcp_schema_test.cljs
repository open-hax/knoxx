(ns knoxx.backend.mcp-schema-test
  "TypeBox -> zod conversion for MCP tool parameters.

   A nested object property used to be handed to the parent as the bare field
   shape rather than a zod schema, so marking it optional threw

     TypeError: fschema.optional is not a function

   inside registerTool. That failed the whole registration, and every MCP POST
   that registers tools answered 400 — which is what a ChatGPT connector does
   immediately after obtaining a token (Mcp-Method: server/discover,
   2026-08-04 19:01). Exercised against real zod, not a double, because the
   defect was that the value lacked zod's own methods."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.routes.mcp :as mcp]
            [knoxx.backend.law.mcp-tool-annotations :as ann]
            ["zod" :refer [z]]))

(def ^:private nested-schema
  #js {:type "object"
       :properties #js {:name  #js {:type "string"}
                        :outer #js {:type "object"
                                    :properties #js {:inner #js {:type "string"}}
                                    :required #js ["inner"]}}
       :required #js ["name"]})

(deftest a-nested-object-property-becomes-a-zod-schema
  (testing "converting a schema with a nested object does not throw"
    (let [shape (mcp/typebox->zod-shape z nested-schema)]
      (is (some? shape))
      (is (some? (aget shape "outer")) "the nested field is present")
      (testing "and the nested field is a usable zod schema, not a bare shape"
        (let [field (aget shape "outer")]
          (is (fn? (aget field "safeParse"))
              "a bare field shape has no safeParse — that was the defect")
          (is (true? (aget (.safeParse field #js {:inner "x"}) "success"))
              "it accepts a matching object")
          (is (false? (aget (.safeParse field #js {:inner 1}) "success"))
              "and still enforces the nested field's type")
          (is (false? (aget (.safeParse field #js {}) "success"))
              "and the nested :required list still applies inside it"))))))

(deftest optionality-follows-the-required-list
  (testing "a required field is not optional and an unlisted one is"
    (let [shape (mcp/typebox->zod-shape z nested-schema)]
      (is (true? (aget (.safeParse (aget shape "name") "hello") "success")))
      (is (false? (aget (.safeParse (aget shape "name") js/undefined) "success"))
          "name is in :required, so undefined must be rejected")
      (is (true? (aget (.safeParse (aget shape "outer") js/undefined) "success"))
          "outer is not required, so it is optional"))))

(deftest an-empty-object-still-converts
  (testing "an object schema with no properties yields a zod object"
    (let [node (mcp/typebox->zod-node z #js {:type "object" :properties #js {}})]
      (is (fn? (aget node "safeParse")))
      (is (true? (aget (.safeParse node #js {}) "success"))))))

(deftest scalar-and-array-nodes-convert
  (testing "the other branches still behave"
    (is (true? (aget (.safeParse (mcp/typebox->zod-node z #js {:type "string"}) "s") "success")))
    (is (true? (aget (.safeParse (mcp/typebox->zod-node z #js {:type "integer"}) 3) "success")))
    (is (false? (aget (.safeParse (mcp/typebox->zod-node z #js {:type "integer"}) 3.5) "success")))
    (is (true? (aget (.safeParse (mcp/typebox->zod-node z #js {:type "array" :items #js {:type "string"}})
                                 #js ["a"]) "success")))))

;; ── stateless transport options ──────────────────────────
;;
;; The SDK selects stateless mode only when sessionIdGenerator is === undefined.
;; (clj->js {:sessionIdGenerator js/undefined}) emits null, which selects
;; STATEFUL mode — and with no session storage, everything after initialize is
;; rejected with "Bad Request: Server not initialized". That is what a client
;; saw as "connected, but advertises no tools". Verified against SDK 1.18, 1.24,
;; 1.29 and 1.30: all four treat null as stateful, so this is ours to get right.

(deftest stateless-options-omit-the-session-generator
  (testing "the key is absent, not null"
    (let [opts (mcp/stateless-transport-options)]
      (is (false? (.hasOwnProperty opts "sessionIdGenerator"))
          "present-but-null is what selected stateful mode")
      (is (undefined? (aget opts "sessionIdGenerator")))
      (is (not (nil? opts)) "still an options object")))

  (testing "clj->js cannot express this, which is why it is built with #js"
    ;; Documents the trap rather than trusting a comment: the obvious spelling
    ;; produces exactly the value that broke it.
    (let [via-clj->js (clj->js {:sessionIdGenerator js/undefined})]
      (is (true? (.hasOwnProperty via-clj->js "sessionIdGenerator"))
          "clj->js keeps the key")
      (is (nil? (aget via-clj->js "sessionIdGenerator"))
          "and its value is null — the SDK reads that as stateful"))))

;; ── declared tool annotations ────────────────────────────
;;
;; MCP's ToolAnnotations defaults are pessimistic when absent: destructiveHint
;; and openWorldHint default to true, readOnlyHint to false. So an unannotated
;; read is presented as a destructive open-world write — which is how a client
;; described graph_query.

(deftest reads-are-declared-read-only
  (testing "graph_query is a read of our own graph"
    (let [a (ann/for-tool "graph_query")]
      (is (true? (:readOnlyHint a)))
      (is (false? (:openWorldHint a)) "it queries our graph, not the internet")))
  (testing "the other corpus reads match"
    (doseq [t ["semantic_query" "memory_search" "memory_session"]]
      (is (true? (:readOnlyHint (ann/for-tool t))) t)
      (is (false? (:openWorldHint (ann/for-tool t))) t))))

(deftest web-reads-stay-open-world
  (testing "reading the internet is still read-only, but not closed-world"
    (doseq [t ["websearch" "web.read"]]
      (is (true? (:readOnlyHint (ann/for-tool t))) t)
      (is (true? (:openWorldHint (ann/for-tool t))) t))))

(deftest overwriting-writes-are-declared-destructive
  (testing "a write that can replace existing state must say so"
    ;; save_translation upserts with $set on a tenant-scoped key, and
    ;; create_new_file calls fs.writeFile with no existence check — both
    ;; replace what is already there. Advertising them as non-destructive would
    ;; suppress a client's warning while state is overwritten.
    (doseq [t ["save_translation" "create_new_file"]]
      (let [a (ann/for-tool t)]
        (is (false? (:readOnlyHint a)) t)
        (is (true? (:destructiveHint a)) (str t " can replace existing state"))
        (is (true? (:idempotentHint a)) (str t " converges on one end state"))))))

(deftest genuinely-append-only-writes-say-so
  (testing "push_claim mints a fresh id per call, so it adds and never replaces"
    (let [a (ann/for-tool "push_claim")]
      (is (false? (:readOnlyHint a)))
      (is (false? (:destructiveHint a)) "it only appends")
      (is (false? (:idempotentHint a)) "repeating adds another claim"))))

(deftest an-undeclared-tool-gets-no-annotations
  (testing "nil leaves the client on its conservative defaults"
    ;; Deliberate: asserting readOnly for a tool nobody has checked would be
    ;; worse than the warning it removes.
    (is (nil? (ann/for-tool "some_tool_nobody_has_reviewed")))
    (is (nil? (ann/for-tool "")))
    (is (nil? (ann/for-tool nil)))))
