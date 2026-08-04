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
