---
original_name: "2026.05.05.13.13.33.md"
title: "Materialize Image Content Parts Boundary"
summary: "Plan to hoist inline JavaScript image materialization out of domain logic behind a clean boundary."
category: "architecture"
created: "2026-05-05"
---

Yeah, that whole block is exactly the kind of “JS blob inline in domain logic” we want to excise. The good news is it’s tightly scoped: it’s just about materializing image content parts into a `SessionManager` after tool calls. 

Let me name what it’s doing and then how to hoist it behind a clean boundary.

***

## What this inline JS is doing

The chunk at 520–571 wires into `SessionManager.setAfterToolCall` and does:

- Pulls `result.details.content_parts` / `contentParts` out of the tool result context as a JS array. 
- Filters them down to `"image"` parts. 
- Defines `fetch-b64!` that:
  - Uses `js/fetch` on a URL.  
  - Checks `response.ok`, throws if not.  
  - Reads an `ArrayBuffer`, turns it into a `js/Buffer`, and then into a base64 data URL string. 
- Defines `materialize!` that:
  - If `data` is already a `data:` URI, strips the header and returns `#js {:type "image" :data <base64> :mimeType ...}`.
  - If `data` is non-HTTP, treats it as raw base64 and wraps it.
  - If only `url` exists, calls `fetch-b64!` and wraps the result into the same shape.
  - Otherwise, returns a resolved `Promise` with `nil`. 
- If there are any image parts:
  - Runs `Promise.all` over `materialize! img-parts`.
  - Filters out `nil`, then:
    - Grabs existing `result.content` (a JS array or `#js []`).
    - Concatenates existing and new images into a JS array and returns `#js {:content merged}`. 

All of that is baked directly into `ensure-agent-session!` via an inline lambda passed into `.setAfterToolCall`. 

This is doing three different jobs:

1. **Wire contract with the `SessionManager` JS API** (`setAfterToolCall`, `Promise`, `fetch`).
2. **Wire protocol for tool results** (`result.details.content_parts` / `contentParts`).
3. **Pure transformation**: “tool image parts → normalized `{type :image, :data base64, :mimeType ...}` content vector”.

Only (1) should be JS-boundary; (2) & (3) should be EDN / CLJS domain logic.

***

## How to factor it cleanly

Here’s the refactor shape I’d use (you can apply it incrementally):

### 1. Extract EDN-centric helpers

In a domain-y namespace (e.g. `knoxx.backend.agent-model` or `knoxx.backend.agents.content`), define the pure transformation on EDN data:

- Input: a **sequence of EDN maps** like:

  ```clojure
  {:type "image"
   :url  "https://..."
   :data nil
   :mimeType "image/png"}
  ```

- Output: a **vector of normalized EDN content parts** like:

  ```clojure
  {:type :image
   :data "<base64-no-header>"
   :mime-type "image/png"}
  ```

plus maybe a helper to splice them into an existing content vector.

This isolates the “merge content parts” semantics from any JS or `SessionManager`. 

### 2. Create a small codec around the JS result shape

In a new boundary ns, say `knoxx.backend.codecs.model-images`:

- Add a function that takes the JS `ctx`/`result` and **extracts EDN image parts**:

  ```clojure
  (defn js-result->image-parts-edn [ctx]
    ;; use aget/array-seq here, but only here
    ;; return a Clojure vector of EDN maps
    )
  ```

- Add the inverse if you need: EDN parts → JS `#js {:type "image" ...}`.

Now the rules are explicit:

- This ns is allowed to touch `aget`, `array-seq`, `js/Buffer`.
- Everything it returns to callers is EDN.

### 3. Wrap `SessionManager.setAfterToolCall` in its own helper

Still at the boundary layer (same ns or a nearby “runtime binding” ns), encapsulate the nasty interop:

```clojure
(defn install-image-materializer!
  [session-manager config]
  (when-let [agent (some-> session-manager (aget "agent"))]
    (when (fn? (aget agent "setAfterToolCall"))
      (.setAfterToolCall
        agent
        (fn [ctx _signal]
          (let [img-parts-edn (js-result->image-parts-edn ctx)
                ;; maybe call out to pure EDN-normalizer here
                ]
            ;; this function can still return a JS promise to satisfy the
            ;; SessionManager contract, but its core logic now lives in
            ;; your codec + domain helpers
            ))))))
```

Then in `ensure-agent-session!` you just call:

```clojure
(install-image-materializer! session-manager config)
```

instead of inlining the whole JS expression tree. 

That gives you:

- A single, small namespace that you can mark as “JS interop allowed.”
- A clean “knoxx backend domain” layer that never sees `js/Buffer` or `data:` string surgery.

### 4. Later: consider a pure CLJS “materialize images” path

If you want to go further, you can also pull `fetch-b64!` behind a CLJS-interop boundary:

- Provide an abstract protocol: “given a URL, return a base64 string”.
- Implement it for Node using `js/fetch` + `js/Buffer`.
- If you ever end up with a browser runtime variant, you could implement it with `FileReader` etc.

But the critical first move is just to **extract this blob into its own interop ns** and have your core agent runtime call it as a function, not embed it as an opaque inline lambda.

***

If you want, I can sketch the proposed `knoxx.backend.codecs.model-images` with concrete functions (EDN signatures, `js-await` async style, etc.) so you can drop it in and then incrementally move callers over.
