---
title: "Clojure Currying & Partial Application Reference"
category: reference
created: 2026-04-21
original: 2026.04.21.16.12.40.md
status: note
---

<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# how do you curry a function in clojure?

In Clojure, you usually do **not** “curry” functions automatically the way Haskell does; the common idiom is to use `partial` for partial application, or to define a chain of one-argument functions when you want true currying.[^1_4][^1_5][^1_9]

## Common pattern

For most day-to-day Clojure code, `partial` is the right tool because it pre-fills some arguments and returns a new function.[^1_5][^1_10][^1_4]

```clojure
(defn add3 [a b c]
  (+ a b c))

(def add1 (partial add3 1))

(add1 2 3)
;; => 6
```

This is usually what people mean in practice when they ask about currying in Clojure, even though technically it is partial application rather than true currying.[^1_2][^1_4]

## True currying

If you want a genuinely curried function, define it as nested unary functions so each call returns the next function.[^1_9][^1_2]

```clojure
(defn curried-add3 [a]
  (fn [b]
    (fn [c]
      (+ a b c))))

(((curried-add3 1) 2) 3)
;; => 6
```

With a truly curried function, `(f x y)` and `((f x) y)` become equivalent in spirit, because supplying fewer arguments returns another function instead of throwing an arity error.[^1_9]

## Why Clojure works this way

Clojure does not support automatic currying by default, and calling a function with too few arguments normally does not create another function for you.[^1_1][^1_4]

One reason commonly given is that Clojure leans heavily on fixed-arity and variadic functions, and automatic currying does not fit that model cleanly.[^1_11][^1_5]

For example, `(+ 3)` evaluates to `3`, not to a function that adds 3 to something else.[^1_4]

## Practical rule

Use `partial` when you just want to lock in some arguments and keep the code idiomatic.[^1_5][^1_4]

Write nested `fn` forms only when you specifically want a curried API, such as for functional composition experiments or teaching the concept.[^1_2][^1_9]

If you want, I can show three versions side by side: plain function, `partial`, and a reusable `curry` helper in idiomatic Clojure.
<span style="display:none">[^1_3][^1_6][^1_7][^1_8]</span>

<div align="center">⁂</div>

[^1_1]: https://dragan.rocks/articles/18/Fluokitten-080-Fast-function-currying-in-Clojure

[^1_2]: https://www.reddit.com/r/compsci/comments/wkjfv/function_currying_in_clojure_and_haskell/

[^1_3]: https://stackoverflow.com/questions/49493398/clojure-currying-technicalities

[^1_4]: https://practical.li/learn-clojure/thinking-functionally/partial-functions/

[^1_5]: https://groups.google.com/g/clojure/c/cE2FUrkPW8I

[^1_6]: https://stackoverflow.com/questions/31373507/rich-hickeys-reason-for-not-auto-currying-clojure-functions

[^1_7]: https://github.com/janetacarr/masala

[^1_8]: http://lorettahe.github.io/clojure/2016/09/22/clojure-auto-curry

[^1_9]: https://news.ycombinator.com/item?id=7052206

[^1_10]: http://xahlee.info/clojure/clojure_function_currying.html

[^1_11]: https://ask.clojure.org/index.php/8381/currying-consideration-early-clojures-design-development


---

# ```cljs

;; I want a macro like this
(defroute register-mcp-http-routes!
{:method "get"
:path "/.well-known/oauth-authorization-server"
:controller
'(let [issuer (js/URL. (.toString base))]
(.send reply
\#js {:issuer (-> (.toString issuer) (.replace (js/RegExp. "/$") ""))
                  :authorization_endpoint (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                  :token_endpoint (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                  :registration_endpoint (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                  :response_types_supported #js ["code"]
                  :grant_types_supported #js ["authorization_code"]
                  :code_challenge_methods_supported #js ["S256"]
                  :token_endpoint_auth_methods_supported #js ["none"]}))})
;; it'll expand something like this
(defn register-mcp-http-routes! [app runtime config]
  (defroute app .get "/.well-known/oauth-authorization-server"
    (let [issuer (js/URL. (.toString base))]
      (.send reply
             #js {:issuer (-> (.toString issuer) (.replace (js/RegExp. "/$") ""))
:authorization_endpoint (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
:token_endpoint (.toString (js/URL. "/api/mcp/oauth/token" issuer))
:registration_endpoint (.toString (js/URL. "/api/mcp/oauth/register" issuer))
:response_types_supported \#js ["code"]
:grant_types_supported \#js ["authorization_code"]
:code_challenge_methods_supported \#js ["S256"]
:token_endpoint_auth_methods_supported \#js ["none"]}))))

```

I'm not totally sure about it but... my agents are havin a hard time with node cause... imports.
No matter what I do, I endup with this server.mjs file when they get too confused to write the cljs

Yes — a macro can generate that `defn`, but the cleaner move is to keep the route body as ordinary CLJS code and have the macro only assemble the repetitive wrapper. In ClojureScript, macros run at compile time, must live in a macro namespace, and are mainly for defining new syntactic constructs rather than ordinary runtime behavior.[^2_1][^2_2][^2_3]

## Core distinction

What you sketched mixes two different ideas: one `defroute` that defines a route-registration function, and another `defroute` that seems to register a route on `app`. That is workable, but it will be easier for both humans and agents if the outer form is a macro like `defroutes` or `defhttp-route-fn`, while the inner runtime registration remains an ordinary function call.[^2_3][^2_1]

Also, your `:controller` value is quoted with `'(...)`, which means the macro will receive a literal list rather than executable body forms; that can be useful, but in this case it is usually simpler to pass the body directly as forms and splice them into the expansion with syntax quote and unquote. Syntax quote, unquote, and unquote-splicing are the standard tools for writing macros that generate code cleanly.[^2_4][^2_5][^2_1]

## A workable shape

A practical version is to make the macro accept the route name and a map, destructure the map, and emit a `defn` that performs the route registration. ClojureScript macros are defined with `defmacro`, and the CLJS docs note they must live in a macro namespace such as a `.clj` or `.cljc` file rather than ordinary runtime REPL code.[^2_3]

```clj
;; src/my/app/routes/macros.clj or .cljc
(ns my.app.routes.macros)

(defmacro defroute-fn
  [fname {:keys [method path controller]}]
  (let [method-sym (symbol (str "." method))]
    `(defn ~fname [app runtime config]
       (defroute app ~method-sym ~path
         ~controller))))
```

Then from your CLJS namespace:

```cljs
(ns my.app.routes
  (:require-macros [my.app.routes.macros :refer [defroute-fn]]))

(defroute-fn register-mcp-http-routes!
  {:method "get"
   :path "/.well-known/oauth-authorization-server"
   :controller
   (let [issuer (js/URL. (.toString base))]
     (.send reply
            #js {:issuer (-> (.toString issuer)
                             (.replace (js/RegExp. "/$") ""))
                 :authorization_endpoint
                 (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                 :token_endpoint
                 (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                 :registration_endpoint
                 (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                 :response_types_supported #js ["code"]
                 :grant_types_supported #js ["authorization_code"]
                 :code_challenge_methods_supported #js ["S256"]
                 :token_endpoint_auth_methods_supported #js ["none"]}))})
```

That shape avoids the extra quote around `:controller`, so the body becomes part of the emitted code instead of data you must later unwrap manually.[^2_1][^2_4]

## If you want multiple body forms

If `:controller` sometimes needs several forms, make the macro accept route options plus `& body` instead of forcing the body into the map. This is usually more idiomatic because macros naturally handle raw forms as body arguments.[^2_2][^2_1]

```clj
(ns my.app.routes.macros)

(defmacro defroute-fn
  [fname {:keys [method path]} & body]
  (let [method-sym (symbol (str "." method))]
    `(defn ~fname [app runtime config]
       (defroute app ~method-sym ~path
         ~@body))))
```

Usage:

```cljs
(defroute-fn register-mcp-http-routes!
  {:method "get"
   :path "/.well-known/oauth-authorization-server"}
  (let [issuer (js/URL. (.toString base))]
    (.send reply
           #js {:issuer (-> (.toString issuer)
                            (.replace (js/RegExp. "/$") ""))
                :authorization_endpoint
                (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                :token_endpoint
                (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                :registration_endpoint
                (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                :response_types_supported #js ["code"]
                :grant_types_supported #js ["authorization_code"]
                :code_challenge_methods_supported #js ["S256"]
                :token_endpoint_auth_methods_supported #js ["none"]})))
```

That version is likely the least confusing for agents, because the map contains only declarative metadata and the executable code stays where code normally lives.[^2_2][^2_1]

## Node and imports

Your “agents end up writing `server.mjs`” problem is probably not really about macros; it is more about Node module format friction. A documented shadow-cljs pain point is that `:target :node-script` produces CommonJS-style loading, and Node will throw `ERR_REQUIRE_ESM` when that code tries to `require()` an ESM-only dependency.

That means if your route code touches modern ESM-only npm packages, the agent often “escapes” into handwritten `.mjs` because Node’s module boundary is easier to satisfy there than from a CommonJS CLJS target. One documented route for real ESM output is `:target :esm` with `:js-options {:js-provider :import}`, which keeps imports as ESM rather than bundling them as CommonJS-style requires.[^2_6][^2_7]

## Recommended framing

Facts: CLJS macros can generate the wrapper function you want, they belong in a macro namespace, and Node ESM/CJS mismatch is a real separate issue when using shadow-cljs with Node targets.[^2_7][^2_3]

Interpretation: the macro is fine, but the bigger design win is to reduce how much “magic” the agent must infer — keep the macro shallow, keep route bodies unquoted, and decide explicitly whether your runtime target is CommonJS or ESM before trying to solve imports through syntax alone.[^2_2][^2_3]

Here is the version I would personally use because it minimizes hidden machinery:

```clj
;; macros.clj
(ns my.app.routes.macros)

(defmacro defroute-fn
  [fname {:keys [method path]} & body]
  (let [m (symbol (str "." method))]
    `(defn ~fname [app runtime config]
       (defroute app ~m ~path
         ~@body))))
```

```cljs
;; routes.cljs
(ns my.app.routes
  (:require-macros [my.app.routes.macros :refer [defroute-fn]]))

(defroute-fn register-mcp-http-routes!
  {:method "get"
   :path "/.well-known/oauth-authorization-server"}
  (let [issuer (js/URL. (.toString base))]
    (.send reply
           #js {:issuer (-> (.toString issuer)
                            (.replace (js/RegExp. "/$") ""))
                :authorization_endpoint (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                :token_endpoint (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                :registration_endpoint (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                :response_types_supported #js ["code"]
                :grant_types_supported #js ["authorization_code"]
                :code_challenge_methods_supported #js ["S256"]
                :token_endpoint_auth_methods_supported #js ["none"]})))
```

Would you like a version that expands to Fastify `.get` directly, or one tailored to your existing `defroute` helper?
<span style="display:none">[^2_10][^2_11][^2_12][^2_13][^2_14][^2_15][^2_16][^2_17][^2_18][^2_19][^2_20][^2_21][^2_22][^2_23][^2_24][^2_25][^2_26][^2_8][^2_9]</span>

<div align="center">⁂</div>

[^2_1]: https://clojure-doc.org/articles/language/macros/

[^2_2]: https://ericnormand.me/mini-guide/when-to-use-a-macro-in-clojure

[^2_3]: https://dragan.rocks/articles/18/Fluokitten-080-Fast-function-currying-in-Clojure

[^2_4]: https://www.braveclojure.com/writing-macros/

[^2_5]: http://bryangilbert.com/post/code/clojure/anatomy-of-a-clojure-macro/

[^2_6]: https://widdindustries.com/blog/clojurescript-importmap.html

[^2_7]: https://clojureverse.org/t/generating-es-modules-browser-deno/6116

[^2_8]: https://clojureverse.org/t/a-macro-between-quote-and-syntax-quote/4466

[^2_9]: https://cljs.github.io/api/cljs.core/defmacro

[^2_10]: https://hackernoon.com/clojurescript-macros-a-not-so-long-journey-part-ii-u3183uqa

[^2_11]: https://www.youtube.com/watch?v=GBdkaFpO_1s

[^2_12]: https://stackoverflow.com/questions/13244550/clojure-macro-getting-syntax-quoted-namespace-qualified-symbol

[^2_13]: https://clojureverse.org/t/use-esm-with-node-shadow-cljs/9363

[^2_14]: https://stackoverflow.com/questions/76259989/how-to-import-npm-pacakge-from-clojurescript

[^2_15]: https://github.com/thheller/shadow-cljs/blob/master/doc/esm.md

[^2_16]: https://www.reddit.com/r/Clojure/comments/97cprw/i_do_not_use_macros_am_i_doing_it_wrong/

[^2_17]: https://github.com/thheller/shadow-cljs/issues/1219

[^2_18]: https://shadow-cljs.github.io/docs/UsersGuide.html

[^2_19]: https://stackoverflow.com/questions/75864233/how-to-import-a-shadow-cljs-project-into-another-shadow-cljs-project-with-a-diff

[^2_20]: https://gist.github.com/borkdude/7e548f06fbefeb210f3fcf14eef019e0

[^2_21]: https://github.com/thheller/shadow-cljs/issues/1102

[^2_22]: https://blog.arcjet.com/nodejs-22-support-esm-require-for-nestjs/

[^2_23]: https://ask.clojure.org/index.php/12333/cannot-import-certain-npm-modules-with-shadow-cljs

[^2_24]: https://www.metosin.fi/blog/2024-09-05-using-shadow-cljs-with-esbuild

[^2_25]: https://www.reddit.com/r/Clojure/comments/14jfs48/a_case_for_clojurescript_20/

[^2_26]: https://joyeecheung.github.io/blog/2024/03/18/require-esm-in-node-js/


---

# Aight this nightmare of a file has to be cleaned up I need a good dsl for this:

```cljs
(ns knoxx.backend.mcp-http
  "Serve Knoxx tools over MCP (Model Context Protocol) Streamable HTTP.

   This is the *server-side* MCP facade used by external tools.

   Design goals:
   - Reuse Knoxx browser login (GitHub OAuth + cookie sessions) for identity.
   - Provide an explicit consent UI (capability dials) for delegated tool access.
   - Mint opaque Redis-backed tokens, scoped to a tool allowlist.
   - Intersect token allowlist with the caller's current Knoxx policy context.

   NOTE: This namespace intentionally avoids importing Node deps directly; the
   Node host shim injects MCP/Zod/Crypto dependencies via the `runtime` object."  
  (:require [clojure.string :as str]
            [knoxx.backend.auth-session :as auth-session]
            [knoxx.backend.mcp-expose :as mcp-expose]
            [knoxx.backend.redis-client :as redis]))

(defonce ^:private mcp-sessions* (atom {}))

(defn- env
  [k default]
  (or (aget js/process.env k) default))

(defn- public-base-url
  [config]
  (try
    (js/URL.
     (or (aget js/process.env "KNOXX_PUBLIC_BASE_URL")
         (aget js/process.env "RENDER_EXTERNAL_URL")
         (:knoxx-base-url config)
         "http://localhost"))
    (catch :default _
      (js/URL. "http://localhost"))))

(defn- base64url
  [buf]
  (.toString (js/Buffer.from buf) "base64url"))

(defn- pkce-challenge
  [crypto verifier]
  (base64url (-> (.createHash crypto "sha256")
                 (.update (str verifier))
                 (.digest))))

(defn- bearer-token
  [req]
  (let [raw (str (or (some-> req (aget "headers") (aget "authorization")) ""))
        m (.match raw (js/RegExp. "^Bearer\\\\s+(.+)$" "i"))]
    (when m (str/trim (aget m 1)))))

(defn- resolve-session-id
  [req]
  (let [headers (or (aget req "headers") #js {})
        header-id (aget headers "mcp-session-id")
        q (or (aget req "query") #js {})
        query-id (aget q "sessionId")]
    (cond
      (and (string? header-id) (not (str/blank? header-id))) header-id
      (and (string? query-id) (not (str/blank? query-id))) query-id
      :else nil)))

(defn- safe
  [s]
  (-> (str (or s ""))
      (.replaceAll "&" "&amp;")
      (.replaceAll "<" "&lt;")
      (.replaceAll ">" "&gt;")
      (.replaceAll "\\"" "&quot;")))

(defn- normalize-tool-selection
  [raw]
  (cond
    (nil? raw) []
    (array? raw) (mapv str raw)
    :else [(str raw)]))

(defn- json-send!
  [reply status payload]
  (-> (.code reply status)
      (.send (clj->js payload))))

(defn- require-redis!
  [reply]
  (if-let [client (redis/get-client)]
    client
    (do
      (json-send! reply 503 {:error "redis_unavailable" :detail "Redis is not connected"})
      nil)))

(defn- require-browser-auth-context!
  [policyDb runtime config req reply]
  (-> (auth-session/resolve-auth-context req policyDb)
      (.catch
       (fn [_]
         (let [base (public-base-url config)
               current-path (or (some-> req (aget "raw") (aget "url")) "/api/mcp/oauth/authorize")
               login-url (js/URL. "/api/auth/login" base)]
           (.set (.-searchParams login-url) "redirect" current-path)
           ;; Fastify v5: reply.redirect(url, [statusCode])
           (.redirect reply (.toString login-url) 302)
           (js/Promise.resolve nil))))))

(defn- get-registered-client
  [redis-client client-id]
  (if (str/blank? (str client-id))
    (js/Promise.resolve nil)
    (-> (.get redis-client (str "knoxx:mcp:client:" client-id))
        (.then (fn [raw]
                 (when raw
                   (try (js/JSON.parse raw)
                        (catch :default _ nil)))))
        (.catch (fn [_] nil)))))

(defn- redirect-uri-allowed?
  [client redirect-uri]
  (if-not client
    true
    (let [uris (or (aget client "redirect_uris") #js [])]
      (boolean (.includes (js/Array.from uris) redirect-uri)))))

(defn- oauth-confirm-params
  [req]
  (let [q (or (aget req "query") #js {})]
    {:client-id (str (or (aget q "client_id") ""))
     :redirect-uri (str (or (aget q "redirect_uri") ""))
     :state (when-let [s (aget q "state")] (str s))
     :code-challenge (str (or (aget q "code_challenge") ""))
     :code-challenge-method (str (or (aget q "code_challenge_method") "S256"))
     :selected-tools (normalize-tool-selection (aget q "tool"))}))

(defn- oauth-confirm-invalid-request?
  [{:keys [client-id redirect-uri code-challenge code-challenge-method]}]
  (or (str/blank? client-id)
      (str/blank? redirect-uri)
      (str/blank? code-challenge)
      (not= code-challenge-method "S256")))

(defn- oauth-confirm-requested-tools
  [runtime config ctx selected-tools]
  (let [all-tools (or (mcp-expose/create-knoxx-custom-tools-js runtime config ctx) #js [])
        available (into #{} (map (fn [t] (str (aget t "name")))) (array-seq all-tools))]
    (->> selected-tools
         (map (comp str/trim str))
         (remove str/blank?)
         distinct
         (filter (fn [t] (contains? available t)))
         vec)))

(defn- handle-oauth-authorize-confirm!
  [{:keys [policyDb runtime config crypto code-ttl]} req reply]
  (if-let [r (require-redis! reply)]
    (let [params (oauth-confirm-params req)]
      (if (oauth-confirm-invalid-request? params)
        (json-send! reply 400 {:error "invalid_request" :detail "Missing required OAuth parameters"})
        (let [{:keys [client-id redirect-uri state code-challenge selected-tools]} params
              p1 (get-registered-client r client-id)
              p2 (.then p1
                        (fn [client]
                          (when (and client (not (redirect-uri-allowed? client redirect-uri)))
                            (throw (js/Error. "redirect_uri not allowed for registered client")))
                          (require-browser-auth-context! policyDb runtime config req reply)))
              p3 (.then p2
                        (fn [ctx]
                          (if-not ctx
                            (js/Promise.resolve nil)
                            (let [requested (oauth-confirm-requested-tools runtime config ctx selected-tools)]
                              (if (empty? requested)
                                (do
                                  (json-send! reply 400 {:error "invalid_scope" :detail "No valid tools selected"})
                                  (js/Promise.resolve nil))
                                (let [membership-id (str (or (aget ctx "membership" "id") (aget ctx "membershipId") ""))
                                      user-email (str (or (aget ctx "user" "email") (aget ctx "userEmail") ""))
                                      org-slug (str (or (aget ctx "org" "slug") (aget ctx "orgSlug") ""))
                                      code (.randomUUID crypto)
                                      code-key (str "knoxx:mcp:code:" code)
                                      payload #js {:code code
                                                  :clientId client-id
                                                  :redirectUri redirect-uri
                                                  :codeChallenge code-challenge
                                                  :codeChallengeMethod "S256"
                                                  :tools (clj->js requested)
                                                  :membershipId membership-id
                                                  :userEmail user-email
                                                  :orgSlug org-slug
                                                  :createdAt (.toISOString (js/Date.))}
                                      p-set (.set r code-key (js/JSON.stringify payload) #js {:EX code-ttl})]
                                  (let [p-redir (.then p-set
                                                      (fn [_]
                                                        (let [redirect (js/URL. redirect-uri)]
                                                          (.set (.-searchParams redirect) "code" code)
                                                          (when state (.set (.-searchParams redirect) "state" state))
                                                          (.redirect reply (.toString redirect) 302))))]
                                    p-redir)))))))
              p4 (.catch p3
                        (fn [err]
                          (json-send! reply 400 {:error "invalid_request" :detail (.-message err)})))]
          p4)))
    nil))

(defn- tool-checkbox-html
  [tools selected]
  (->> tools
       (map (fn [t]
              (let [name (str (or (aget t "name") ""))
                    label (str (or (aget t "label") (aget t "name") (aget t "description") name))
                    desc (str (or (aget t "description") ""))
                    checked (if (contains? selected name) "checked" "")]
                (str "\\n        <label style=\\"display:block; margin: 6px 0;\\">\\n"
                     "          <input type=\\"checkbox\\" name=\\"tool\\" value=\\"" (safe name) "\\" " checked " />\\n"
                     ```
                     "          <span style=\\"font-weight:600;\\">" (safe label) "</span>\\n"
                     ```
                     ```
                     "          <span style=\\"color:#666;\\">(" (safe name) ")</span>\\n"
                     ```
                     ```
                     "          <div style=\\"color:#444; margin-left: 22px;\\">" (safe desc) "</div>\\n"
                     ```
                     "        </label>\\n"))))
       (str/join "\\n")))

(defn- default-selected-tools
  [tool-names]
  (let [defaults ["semantic_query" "semantic_read" "memory_search" "memory_session" "graph_query" "websearch" "read"]]
    (into #{}
          (filter (fn [id] (contains? tool-names id)))
          defaults)))

(defn register-mcp-http-routes!
  [app runtime config]
  (let [crypto (aget runtime "crypto")
        McpServer (aget runtime "McpServer")
        StreamableHTTPServerTransport (aget runtime "StreamableHTTPServerTransport")
        isInitializeRequest (aget runtime "isInitializeRequest")
        z (aget runtime "z")
        code-ttl (js/parseInt (env "KNOXX_MCP_CODE_TTL_SECONDS" "300") 10)
        token-ttl (js/parseInt (env "KNOXX_MCP_TOKEN_TTL_SECONDS" (str (* 60 60 24 30))) 10)
        base (public-base-url config)
        policyDb (aget runtime "policyDb")]

    ;; OAuth discovery
    (.get app "/.well-known/oauth-authorization-server"
          (fn [_req reply]
            (let [issuer (js/URL. (.toString base))]
              (.send reply
                     #js {:issuer (-> (.toString issuer) (.replace (js/RegExp. "/$") ""))
                          :authorization_endpoint (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                          :token_endpoint (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                          :registration_endpoint (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                          :response_types_supported #js ["code"]
                          :grant_types_supported #js ["authorization_code"]
                          :code_challenge_methods_supported #js ["S256"]
                          :token_endpoint_auth_methods_supported #js ["none"]}))))

    ;; OAuth Dynamic Client Registration (public clients, no secret)
    (.post app "/api/mcp/oauth/register"
           (fn [req reply]
             (if-let [r (require-redis! reply)]
               (let [body (or (aget req "body") #js {})
                     redirect-uris (aget body "redirect_uris")
                     redirect-uris (if (array? redirect-uris) (mapv str (array-seq redirect-uris)) [])]
                 (if (empty? redirect-uris)
                   (json-send! reply 400 {:error "invalid_client_metadata" :detail "redirect_uris is required"})
                   (let [client-id (.randomUUID crypto)
                         client #js {:client_id client-id
                                     :client_name (or (aget body "client_name") "mcp-client")
                                     :redirect_uris (clj->js redirect-uris)
                                     :token_endpoint_auth_method "none"
                                     :grant_types #js ["authorization_code"]
                                     :response_types #js ["code"]
                                     :created_at (.toISOString (js/Date.))}]
                     (-> (.set r (str "knoxx:mcp:client:" client-id) (js/JSON.stringify client))
                         (.then (fn [_]
                                  (-> (.code reply 201)
                                      (.send client))))
                         (.catch (fn [err]
                                   (json-send! reply 500 {:error "registration_failed" :detail (.-message err)})))))))
               nil)))

    ;; OAuth authorize (consent screen)
    (.get app "/api/mcp/oauth/authorize"
          (fn [req reply]
            (if-let [r (require-redis! reply)]
              (let [q (or (aget req "query") #js {})
                    client-id (str (or (aget q "client_id") ""))
                    redirect-uri (str (or (aget q "redirect_uri") ""))
                    state (when-let [s (aget q "state")] (str s))
                    code-challenge (str (or (aget q "code_challenge") ""))
                    method (str (or (aget q "code_challenge_method") "S256"))
                    requested-scope (str (or (aget q "scope") ""))]
                (if (or (str/blank? client-id)
                        (str/blank? redirect-uri)
                        (str/blank? code-challenge)
                        (not= method "S256"))
                  (json-send! reply 400 {:error "invalid_request"
                                        :detail "Missing required OAuth parameters (client_id, redirect_uri, code_challenge, S256)"})
                  (-> (get-registered-client r client-id)
                      (.then (fn [client]
                               (when (and client (not (redirect-uri-allowed? client redirect-uri)))
                                 (throw (js/Error. "redirect_uri not allowed for registered client")))
                               client))
                      (.then (fn [_]
                               (require-browser-auth-context! policyDb runtime config req reply)))
                      (.then
                       (fn [ctx]
                         (when ctx
                           (let [tools (or (mcp-expose/create-knoxx-custom-tools-js runtime config ctx) #js [])
                                 requested (into #{}
                                                (comp (map str/trim)
                                                      (remove str/blank?))
                                                (str/split requested-scope #"\\\\s+"))
                                 tool-names (into #{} (map (fn [t] (str (aget t "name")))) (array-seq tools))
                                 selected (let [sel (into #{} (for [t (array-seq tools)
                                                                   :let [name (str (aget t "name"))]
                                                                   :when (or (contains? requested "all")
                                                                             (contains? requested name))]
                                                               name))]
                                            (if (seq sel) sel (default-selected-tools tool-names)))
                                 confirm-url (js/URL. "/api/mcp/oauth/authorize/confirm" base)]
                             (.set (.-searchParams confirm-url) "client_id" client-id)
                             (.set (.-searchParams confirm-url) "redirect_uri" redirect-uri)
                             (when state (.set (.-searchParams confirm-url) "state" state))
                             (.set (.-searchParams confirm-url) "code_challenge" code-challenge)
                             (.set (.-searchParams confirm-url) "code_challenge_method" "S256")
                             (when-not (str/blank? requested-scope)
                               (.set (.-searchParams confirm-url) "scope" requested-scope))

                             (let [html (str "<!doctype html>\\n"
                                             ```
                                             "<html><head><meta charset=\\"utf-8\\" />\\n"
                                             ```
                                             "<title>Authorize MCP Client</title>\\n"
                                             "<style>body{font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial;margin:24px;}"
                                             ".box{max-width:920px;} .meta{color:#555;margin-bottom:12px;}"
                                             ".tools{border:1px solid #ddd;border-radius:8px;padding:12px 16px;}"
                                             ".actions{margin-top:18px;display:flex;gap:12px;}"
                                             "button{padding:8px 14px;border-radius:8px;border:1px solid #333;background:#111;color:#fff;cursor:pointer;}"
                                             "a{color:#0b67d0;}"
                                             "</style></head><body><div class=\\"box\\">\\n"
                                             "<h1>Authorize MCP Client</h1>\\n"
                                             "<div class=\\"meta\\">\\n"
                                             ```
                                             "<div><strong>Client:</strong> " (safe client-id) "</div>\\n"
                                             ```
                                             ```
                                             "<div><strong>Redirect URI:</strong> " (safe redirect-uri) "</div>\\n"
                                             ```
                                             "</div>\\n"
                                             "<form method=\\"GET\\" action=\\"" (safe (.-pathname confirm-url)) "\\">\\n"
                                             "<input type=\\"hidden\\" name=\\"client_id\\" value=\\"" (safe client-id) "\\" />\\n"
                                             "<input type=\\"hidden\\" name=\\"redirect_uri\\" value=\\"" (safe redirect-uri) "\\" />\\n"
                                             "<input type=\\"hidden\\" name=\\"state\\" value=\\"" (safe (or state "")) "\\" />\\n"
                                             "<input type=\\"hidden\\" name=\\"code_challenge\\" value=\\"" (safe code-challenge) "\\" />\\n"
                                             "<input type=\\"hidden\\" name=\\"code_challenge_method\\" value=\\"S256\\" />\\n"
                                             "<input type=\\"hidden\\" name=\\"scope\\" value=\\"" (safe requested-scope) "\\" />\\n"
                                             "<h2>Capabilities</h2>\\n"
                                             "<p>Select exactly which Knoxx tools this client can call.</p>\\n"
                                             "<div class=\\"tools\\">\\n"
                                             (tool-checkbox-html tools selected)
                                             "</div>\\n"
                                             "<div class=\\"actions\\">\\n"
                                             "<button type=\\"submit\\">Authorize</button>\\n"
                                             "<a href=\\"/\\">Cancel</a>\\n"
                                             "</div></form></div></body></html>")]
                               (-> (.header reply "content-type" "text/html; charset=utf-8")
                                   (.send html)))))))
                      (.catch (fn [err]
                                (json-send! reply 400 {:error "invalid_request" :detail (.-message err)}))))))
              nil)))

    ;; OAuth authorize confirm (issues code)
    (.get app "/api/mcp/oauth/authorize/confirm"
          (fn [req reply]
            (handle-oauth-authorize-confirm!
             {:policyDb policyDb
              :runtime runtime
              :config config
              :crypto crypto
              :code-ttl code-ttl}
             req reply)))

    ;; OAuth token exchange endpoint (authorization_code + PKCE S256)
    (.post app "/api/mcp/oauth/token"
           (fn [req reply]
             (if-let [r (require-redis! reply)]
               (let [body (or (aget req "body") #js {})
                     grant-type (str (or (aget body "grant_type") (aget body "grantType") ""))
                     code (str (or (aget body "code") ""))
                     code-verifier (str (or (aget body "code_verifier") (aget body "codeVerifier") ""))
                     client-id (str (or (aget body "client_id") (aget body "clientId") ""))
                     redirect-uri (str (or (aget body "redirect_uri") (aget body "redirectUri") ""))]
                 (if (or (not= grant-type "authorization_code")
                         (str/blank? code)
                         (str/blank? code-verifier)
                         (str/blank? client-id)
                         (str/blank? redirect-uri))
                   (json-send! reply 400 {:error "invalid_request"})
                   (-> (get-registered-client r client-id)
                       (.then
                        (fn [client]
                          (when (and client (not (redirect-uri-allowed? client redirect-uri)))
                            (throw (js/Error. "redirect_uri not allowed for registered client")))
                          (-> (.get r (str "knoxx:mcp:code:" code))
                              (.then
                               (fn [raw]
                                 (when-not raw
                                   (throw (js/Error. "Unknown or expired code")))
                                 (let [record (js/JSON.parse raw)]
                                   (when (or (not= (aget record "clientId") client-id)
                                             (not= (aget record "redirectUri") redirect-uri))
                                     (throw (js/Error. "Client/redirect mismatch")))
                                   (let [expected (str (or (aget record "codeChallenge") ""))
                                         actual (pkce-challenge crypto code-verifier)]
                                     (when (or (str/blank? expected) (not= expected actual))
                                       (throw (js/Error. "PKCE verification failed")))
                                     (-> (.del r (str "knoxx:mcp:code:" code))
                                         (.then
                                          (fn [_]
                                            (let [access-token (.randomUUID crypto)
                                                  token-key (str "knoxx:mcp:token:" access-token)
                                                  token-value #js {:accessToken access-token
                                                                  :clientId client-id
                                                                  :membershipId (aget record "membershipId")
                                                                  :userEmail (aget record "userEmail")
                                                                  :orgSlug (aget record "orgSlug")
                                                                  :tools (aget record "tools")
                                                                  :createdAt (.toISOString (js/Date.))
                                                                  :expiresAt (.toISOString (js/Date. (+ (.now js/Date) (* token-ttl 1000))))}]
                                              (-> (.set r token-key (js/JSON.stringify token-value) #js {:EX token-ttl})
                                                  (.then (fn [_]
                                                           (when-let [mid (aget record "membershipId")]
                                                             (.sAdd r (str "knoxx:mcp:user:" mid ":tokens") access-token))
                                                           (.send reply
                                                                  #js {:access_token access-token
                                                                       :token_type "Bearer"
                                                                       :scope (->> (js/Array.from (or (aget record "tools") #js []))
                                                                                   (str/join " "))
                                                                       :expires_in token-ttl})))))))))))))))))
                       (.catch (fn [err]
                                 (json-send! reply 400 {:error "invalid_grant" :detail (.-message err)}))))))
               nil)))

    ;; Token management (browser session only)
    (.get app "/api/mcp/tokens"
          (fn [req reply]
            (if-let [r (require-redis! reply)]
              (-> (require-browser-auth-context! policyDb runtime config req reply)
                  (.then
                   (fn [ctx]
                     (when ctx
                       (let [membership-id (str (or (aget ctx "membership" "id") (aget ctx "membershipId") ""))]
                         (if (str/blank? membership-id)
                           (json-send! reply 400 {:error "missing_membership"})
                           (-> (.sMembers r (str "knoxx:mcp:user:" membership-id ":tokens"))
                               (.then
                                (fn [token-ids]
                                  (-> (js/Promise.all
                                       (clj->js
                                        (for [token-id (array-seq token-ids)]
                                          (-> (.get r (str "knoxx:mcp:token:" token-id))
                                              (.then (fn [raw]
                                                       (when raw
                                                         (try (js/JSON.parse raw)
                                                              (catch :default _ nil)))))))))
                                      (.then
                                       (fn [records]
                                         (let [filtered (->> (array-seq records) (remove nil?) (into-array))]
                                           (.send reply #js {:ok true :tokens filtered})))))))))))))
                  (.catch (fn [err]
                            (json-send! reply 401 {:error "unauthorized" :detail (.-message err)}))))
              nil)))

    (.delete app "/api/mcp/tokens/:tokenId"
             (fn [req reply]
               (if-let [r (require-redis! reply)]
                 (-> (require-browser-auth-context! policyDb runtime config req reply)
                     (.then
                      (fn [ctx]
                        (when ctx
                          (let [membership-id (str (or (aget ctx "membership" "id") (aget ctx "membershipId") ""))
                                token-id (str (or (aget (aget req "params") "tokenId") ""))]
                            (if (or (str/blank? membership-id) (str/blank? token-id))
                              (json-send! reply 400 {:error "invalid_request"})
                              (-> (.del r (str "knoxx:mcp:token:" token-id))
                                  (.then (fn [_]
                                           (.sRem r (str "knoxx:mcp:user:" membership-id ":tokens") token-id)
                                           (.send reply #js {:ok true})))))))))
                     (.catch (fn [err]
                               (json-send! reply 401 {:error "unauthorized" :detail (.-message err)}))))
                 nil)))

    ;; MCP transport
    (letfn [(load-token-record [redis-client req]
              (let [token (bearer-token req)]
                (if (str/blank? token)
                  (js/Promise.resolve nil)
                  (-> (.get redis-client (str "knoxx:mcp:token:" token))
                      (.then (fn [raw]
                               (when raw
                                 (try (js/JSON.parse raw)
                                      (catch :default _ nil)))))
                      (.catch (fn [_] nil))))))

            (handle-mcp-session! [req reply]
              (let [session-id (resolve-session-id req)]
                (cond
                  (str/blank? (str session-id))
                  (do (.code reply 400) (.send reply "Missing mcp-session-id"))

                  :else
                  (let [{:keys [transport token]} (get @mcp-sessions* session-id)]
                    (if-not transport
                      (do (.code reply 404) (.send reply (str "Invalid mcp-session-id: " session-id)))
                      (let [supplied (bearer-token req)]
                        (if (not= (str supplied) (str token))
                          (do (.code reply 401) (.send reply "Unauthorized"))
                          (.handleRequest transport (aget req "raw") (aget reply "raw")))))))))

            (handle-mcp-post! [req reply]
              (let [session-id (resolve-session-id req)
                    existing (when session-id (get @mcp-sessions* session-id))
                    supplied (bearer-token req)]
                (if existing
                  (if (not= (str supplied) (str (:token existing)))
                    (do (.code reply 401) (.send reply "Unauthorized"))
                    (.handleRequest (:transport existing) (aget req "raw") (aget reply "raw") (aget req "body")))
                  ;; New session: must initialize
                  (if-not (and isInitializeRequest (isInitializeRequest (aget req "body")))
                    (do
                      (.code reply 400)
                      (.send reply #js {:jsonrpc "2.0"
                                        :error #js {:code -32000 :message "Bad Request: Server not initialized"}
                                        :id nil}))
                    (if-let [r (require-redis! reply)]
                      (-> (load-token-record r req)
                          (.then
                           (fn [token-record]
                             (if-not token-record
                               (do (.code reply 401) (.send reply "Unauthorized"))
                               (let [headers-like #js {}
                                     mid (aget token-record "membershipId")
                                     email (aget token-record "userEmail")
                                     org (aget token-record "orgSlug")]
                                 (when mid (aset headers-like "x-knoxx-membership-id" mid))
                                 (when email (aset headers-like "x-knoxx-user-email" email))
                                 (when org (aset headers-like "x-knoxx-org-slug" org))
                                 (-> (.resolveRequestContext policyDb headers-like)
                                     (.then
                                      (fn [ctx]
                                        (let [all-tools (or (mcp-expose/create-knoxx-custom-tools-js runtime config ctx) #js [])
                                              allowed (into #{} (map str) (array-seq (or (aget token-record "tools") #js [])))
                                              effective (->> (array-seq all-tools)
                                                             (filter (fn [t] (contains? allowed (str (aget t "name")))))
                                                             (into-array))
                                              server (new McpServer #js {:name "knoxx" :version "0.1.0"})
                                              input-schema (when z (.record z (.any z)))
                                              transport (new StreamableHTTPServerTransport
                                                            #js {:sessionIdGenerator (fn [] (.randomUUID crypto))
                                                                 :onsessioninitialized (fn [sid]
                                                                                        (swap! mcp-sessions* assoc (str sid)
                                                                                               {:transport transport
                                                                                                :token (aget token-record "accessToken")}) )})]
                                          (doseq [tool (array-seq effective)]
                                            (let [tool-name (-> (str (or (aget tool "name") "")) str/trim)]
                                              (when-not (str/blank? tool-name)
                                                (.registerTool
                                                 server
                                                 tool-name
                                                 #js {:description (str (or (aget tool "description") (aget tool "label") tool-name))
                                                      :inputSchema input-schema}
                                                 (fn [params]
                                                   (.execute tool "mcp" params nil nil nil))))))

                                          (set! (.-onclose transport)
                                                (fn []
                                                  (when-let [sid (.-sessionId transport)]
                                                    (swap! mcp-sessions* dissoc (str sid)))))

                                          (-> (.connect server transport)
                                              (.then (fn [_]
                                                       (.handleRequest transport (aget req "raw") (aget reply "raw") (aget req "body")))))))))
                                     (.catch
                                      (fn [err]
                                        (.error js/console "[knoxx-mcp] resolveRequestContext failed" err)
                                        (json-send! reply 401 {:error "unauthorized"})))))))))))
                          (.catch (fn [err]
                                    (.error js/console "[knoxx-mcp] initialize failed" err)
                                    (json-send! reply 500 {:error "mcp_init_failed" :detail (.-message err)}))))
                      nil))))]

      
      (.get app "/mcp" (fn [req reply] (handle-mcp-session! req reply)))
      (.delete app "/mcp" (fn [req reply] (handle-mcp-session! req reply))))

    nil))
```

The brackets are completely broken here, and the functions are really complicated.

Yes: this file wants a **shallow** route DSL, not a “macro does everything” DSL. The clean target is a small macro layer that expands into plain Fastify registration calls, while the hard business logic moves into named helpers so your brackets stop looking like an industrial accident.[^3_1][^3_2][^3_3]

## Signal

Facts: route macros are a reasonable fit in shadow-cljs Node, but the stable pattern is “macro surface, plain function internals,” not “entire web framework in macros.”  Facts: a data-oriented router target is usually easier to debug than a heavy macro tower, and even when using macros, keeping handlers as ordinary functions is the recommended compromise.[^3_2][^3_4][^3_1]

μ executable shape:

```clj
(defmacro defroutes
  [fname bindings & routes]
  `(defn ~fname ~bindings
     ~@(for [r routes]
         (expand-route r))))

(defmacro GET [path bindings & body]
  `(register-route! app :get ~path
     (fn ~bindings ~@body)))

(defmacro POST [path bindings & body]
  `(register-route! app :post ~path
     (fn ~bindings ~@body)))

(defmacro DELETE [path bindings & body]
  `(register-route! app :delete ~path
     (fn ~bindings ~@body)))
```

Interpretation: your current namespace is not failing because CLJS cannot do this; it is failing because transport setup, OAuth flow, HTML rendering, Redis I/O, token issuance, and MCP session lifecycle all live inline inside one registration function.[^3_3][^3_1]

## Frames

Frame: $(codebase, p=0.94)$ լա the real problem is not “broken brackets,” it is uncontrolled nesting depth produced by mixing declaration, orchestration, and effects in one place.[^3_1]

Frame: $(dsl, p=0.89)$ լա the right DSL is one that compresses repetitive route ceremony only. It should not hide Redis, OAuth, or MCP semantics behind macro smoke.[^3_5][^3_1]

Frame: $(agents, p=0.84)$ լա agents are likely generating `server.mjs` because plain JS gives them a flatter control surface than deeply nested CLJS promises and interop forms. A better CLJS DSL lowers that surface area by making route definitions declarative and handlers named.[^3_1]

## Countermoves

First, split the namespace into three layers so each unit has one reason to change. That “small macro surface over plain functions” approach matches the route guidance from earlier material in your space and keeps the expansion target boring.[^3_2][^3_1]

- `knoxx.backend.mcp-http.routes-macros` — macros only.
- `knoxx.backend.mcp-http.handlers` — handler fns, one per route.
- `knoxx.backend.mcp-http.runtime` — helpers like `json-send!`, `require-redis!`, `public-base-url`, token/session utilities.
- `knoxx.backend.mcp-http.html` — consent-page rendering.
- `knoxx.backend.mcp-http.mcp` — transport/session setup.

Second, define a tiny route DSL that takes method, path, and a handler symbol, not an inline 80-line body. That keeps macro expansion transparent and makes bracket repair almost trivial.[^3_4][^3_5]

```clj
;; macros.clj
(ns knoxx.backend.mcp-http.routes-macros)

(defmacro defroutes
  [fname bindings & routes]
  `(defn ~fname ~bindings
     ~@(map (fn [[method path handler]]
              `(~(symbol (str "." (name method))) app ~path ~handler))
            routes)))
```

```cljs
;; mcp_http.cljs
(ns knoxx.backend.mcp-http
  (:require [knoxx.backend.mcp-http.handlers :as h])
  (:require-macros [knoxx.backend.mcp-http.routes-macros :refer [defroutes]]))

(defroutes register-mcp-http-routes!
  [app runtime config]
  [:get "/.well-known/oauth-authorization-server"
   (h/oauth-discovery-handler runtime config)]
  [:post "/api/mcp/oauth/register"
   (h/oauth-register-handler runtime config)]
  [:get "/api/mcp/oauth/authorize"
   (h/oauth-authorize-handler runtime config)]
  [:get "/api/mcp/oauth/authorize/confirm"
   (h/oauth-authorize-confirm-handler runtime config)]
  [:post "/api/mcp/oauth/token"
   (h/oauth-token-handler runtime config)]
  [:get "/api/mcp/tokens"
   (h/list-tokens-handler runtime config)]
  [:delete "/api/mcp/tokens/:tokenId"
   (h/delete-token-handler runtime config)]
  [:get "/mcp"
   (h/mcp-session-handler runtime config)]
  [:delete "/mcp"
   (h/mcp-session-handler runtime config)]
  [:post "/mcp"
   (h/mcp-post-handler runtime config)])
```

Third, make each route builder return a Fastify handler. That gives you closure over `runtime`, `config`, `policyDb`, `crypto`, and TTLs without stuffing everything into one outer `let`.[^3_1]

```cljs
(ns knoxx.backend.mcp-http.handlers
  (:require [knoxx.backend.mcp-http.runtime :as rt]
            [knoxx.backend.mcp-http.html :as html]
            [knoxx.backend.mcp-http.mcp :as mcp]
            [knoxx.backend.auth-session :as auth-session]
            [knoxx.backend.mcp-expose :as mcp-expose]
            [knoxx.backend.redis-client :as redis]
            [clojure.string :as str]))

(defn oauth-discovery-handler
  [runtime config]
  (fn [_req reply]
    (let [base (rt/public-base-url config)
          issuer (js/URL. (.toString base))]
      (.send reply
             #js {:issuer (-> (.toString issuer) (.replace (js/RegExp. "/$") ""))
                  :authorization_endpoint (.toString (js/URL. "/api/mcp/oauth/authorize" issuer))
                  :token_endpoint (.toString (js/URL. "/api/mcp/oauth/token" issuer))
                  :registration_endpoint (.toString (js/URL. "/api/mcp/oauth/register" issuer))
                  :response_types_supported #js ["code"]
                  :grant_types_supported #js ["authorization_code"]
                  :code_challenge_methods_supported #js ["S256"]
                  :token_endpoint_auth_methods_supported #js ["none"]}))))
```

Fourth, flatten promise chains by naming stages. Your current `handle-oauth-authorize-confirm!` and `POST /api/mcp/oauth/token` are structurally valid ideas, but they are cognitively opaque because validation, loading, authorization, mutation, and response are interleaved.[^3_1]

Use a pipeline like this instead:

- Parse request.
- Validate request.
- Load dependencies.
- Authorize context.
- Perform one mutation.
- Render response.

For example:

```cljs
(defn exchange-code!
  [{:keys [redis crypto token-ttl]} {:keys [client-id code code-verifier redirect-uri]}]
  (-> (load-code-record! redis code)
      (.then #(validate-code-record! % client-id redirect-uri crypto code-verifier))
      (.then #(issue-access-token! redis % client-id token-ttl))))
```

That reduces the “where does this paren close?” problem because each function ends in one responsibility.[^3_1]

## DSL design

The best DSL here is probably declarative route data plus optional macros for sugar, not a macro that embeds giant quoted controllers. That recommendation matches the “author with macros, expand to boring route data, keep handlers plain” pattern surfaced in your space files.[^3_2][^3_1]

A stronger version than the simple vector form above is:

```cljs
(defroutes register-mcp-http-routes!
  [app runtime config]

  (GET "/.well-known/oauth-authorization-server"
    (oauth-discovery-handler runtime config))

  (POST "/api/mcp/oauth/register"
    (oauth-register-handler runtime config))

  (GET "/api/mcp/oauth/authorize"
    (oauth-authorize-handler runtime config))

  (GET "/api/mcp/oauth/authorize/confirm"
    (oauth-authorize-confirm-handler runtime config))

  (POST "/api/mcp/oauth/token"
    (oauth-token-handler runtime config))

  (GET "/api/mcp/tokens"
    (list-tokens-handler runtime config))

  (DELETE "/api/mcp/tokens/:tokenId"
    (delete-token-handler runtime config))

  (GET "/mcp"
    (mcp-session-handler runtime config))

  (DELETE "/mcp"
    (mcp-session-handler runtime config))

  (POST "/mcp"
    (mcp-post-handler runtime config)))
```

And macros:

```clj
(defmacro GET [path handler] `[:get ~path ~handler])
(defmacro POST [path handler] `[:post ~path ~handler])
(defmacro DELETE [path handler] `[:delete ~path ~handler])

(defmacro defroutes [fname bindings & routes]
  `(defn ~fname ~bindings
     ~@(for [[method path handler] routes
             :let [m (symbol (str "." (name method)))]]
         `(~m app ~path ~handler))
     nil))
```

That is simple enough for humans, simple enough for agents, and still gives you Lispy authoring ergonomics.[^3_4][^3_1]

## Next

Facts: your current file contains at least five separable concerns — HTTP route declaration, OAuth validation, HTML generation, Redis token persistence, and MCP transport/session management — and each one can become a small namespace with named functions.  Interpretation: once you do that, the macro becomes almost boring, which is exactly what you want from infrastructure syntax.[^3_5][^3_1]

Two viable options:

- Conservative: keep Fastify, add only `defroutes` + `GET/POST/DELETE`, move all inline bodies into named handlers.
- Stronger: author routes as data vectors or macro sugar that expands to route data, then have one `install-routes!` runtime function walk that data and register with Fastify. This is closer to the “macro surface over boring substrate” pattern.[^3_2][^3_1]

I can draft the full cleanup in CLJS next as a concrete split into:

1. `routes_macros.clj`,
2. `runtime.cljs`,
3. `oauth.cljs`,
4. `html.cljs`,
5. `mcp.cljs`,
6. `mcp_http.cljs`

Would you rather optimize for minimal diff against this file, or for the cleanest long-term DSL?
<span style="display:none">[^3_10][^3_11][^3_12][^3_13][^3_14][^3_15][^3_16][^3_17][^3_18][^3_19][^3_20][^3_21][^3_22][^3_23][^3_24][^3_25][^3_26][^3_27][^3_6][^3_7][^3_8][^3_9]</span>

<div align="center">⁂</div>

[^3_1]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_2]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_3]: https://dragan.rocks/articles/18/Fluokitten-080-Fast-function-currying-in-Clojure

[^3_4]: https://clojure-doc.org/articles/language/macros/

[^3_5]: https://ericnormand.me/mini-guide/when-to-use-a-macro-in-clojure

[^3_6]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_7]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_8]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_9]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_10]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_11]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_12]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_13]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_14]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_15]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_16]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_17]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^3_18]: clojure-shadow-cljs-regex-synt-Ea7Yflk1RGG.lacjWPTG2w.md

[^3_19]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_20]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_21]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_22]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_23]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_24]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_25]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_26]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^3_27]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md


---

# I need async/await pattern I think? or I need to convince the agents to use high order functions more. Caue the problem is all these lambdas and lets* getting deeploy nested.

There is so much buisness logic in the original codes routing, and the async stuff is makin it even more complex

Yes — the main fix is to **separate orchestration from effects** and give async a flatter syntax. In CLJS, you can either stay with raw Promises and higher-order helpers, use `shadow.cljs.modern/js-await` for async/await-like readability, or use `core.async`/`<p!` when you genuinely want channel semantics rather than just nicer Promise code.[^4_1][^4_2][^4_3]

## Facts

ClojureScript Promise code gets ugly when routing, validation, I/O, and response rendering are all nested inline, and the official Promise interop guidance explicitly notes that Promise-heavy code using `await` can become structurally unfriendly.  Shadow-cljs provides `js-await`, which is a macro intended to give you most of the ergonomic benefit of async/await while still expanding to Promise chaining.[^4_2][^4_3][^4_1]

Also, macros are not the real cure for complexity here. The real cure is to move business logic into small named functions and let the route handler read like orchestration only.[^4_4][^4_5]

## Interpretation

You probably need both: a flatter async pattern **and** more higher-order decomposition. `async/await`-style syntax reduces local indentation, but if one handler still validates input, loads Redis state, resolves auth context, computes tool sets, writes records, and renders responses, the code will remain hard to reason about.[^4_3][^4_1]

So the design target should be:

- Route function: parse request, call workflow, send response.
- Workflow function: compose named async steps.
- Step functions: each do one thing and return a Promise.

That pattern preserves Clojure readability better than packing everything into nested `let` + `.then` chains.[^4_1][^4_3]

## Good patterns

For shadow-cljs specifically, `js-await` is the smallest step up from raw Promises. It returns a Promise and supports a `catch` form, while preserving code that reads much closer to JavaScript async/await.[^4_2][^4_1]

```cljs
(ns knoxx.backend.mcp-http.oauth
  (:require [shadow.cljs.modern :refer [js-await]]))

(defn exchange-token!
  [{:keys [redis crypto token-ttl]} {:keys [client-id code code-verifier redirect-uri]}]
  (js-await [client (get-registered-client redis client-id)
             raw    (.get redis (str "knoxx:mcp:code:" code))]
    (when (and client (not (redirect-uri-allowed? client redirect-uri)))
      (throw (js/Error. "redirect_uri not allowed for registered client")))
    (when-not raw
      (throw (js/Error. "Unknown or expired code")))
    (let [record (js/JSON.parse raw)]
      (validate-code-record! record client-id redirect-uri crypto code-verifier)
      (issue-access-token! redis record client-id token-ttl))
    (catch err
      (js/Promise.reject err))))
```

That improves local readability, but a still better shape is to break the workflow into higher-order stages so the top-level function is mostly composition. ClojureScript Promise interop works fine with ordinary `.then` composition, and that is often clearer once each `.then` points to a named function instead of an inline lambda.[^4_3]

```cljs
(defn load-and-validate-code!
  [deps req]
  (-> (fetch-code-record! deps req)
      (.then #(validate-code! deps req %))))

(defn mint-token!
  [deps record]
  (-> (delete-auth-code! deps record)
      (.then (fn [_] (persist-token! deps record)))
      (.then format-token-response)))

(defn exchange-token!
  [deps req]
  (-> (load-and-validate-code! deps req)
      (.then #(mint-token! deps %))))
```

This is the higher-order-function move you mentioned: the async control flow stays explicit, but anonymous callbacks disappear because each stage has a name.[^4_3]

## Narrative risk

If you chase “true async/await everywhere,” you can improve syntax while preserving bad architecture. Promesa and experimental async/await-style libraries exist, but the shadow-cljs guidance itself suggests the simple macro approach gives most of the value without needing compiler-level magic.[^4_6][^4_7][^4_2]

Likewise, `core.async` is powerful, but it is not automatically the best answer for request/response server workflows. The official CLJS docs present `<p!` in `go` blocks as a way to consume Promises, yet that is most attractive when you actually want channel-style coordination; for straightforward HTTP business logic, plain Promises plus named functions are often simpler.[^4_8][^4_3]

## Concrete move

I would refactor your file with this rule set:

- Use `js-await` for top-level workflows that would otherwise have 3–6 nested Promise steps.[^4_1][^4_2]
- Use named helper functions for each async stage, never inline a callback that does more than one conceptual thing.[^4_3]
- Keep route handlers thin: extract params, call one workflow fn, translate result/error to HTTP.[^4_4]
- Move HTML rendering, Redis access, OAuth validation, and MCP transport setup into separate namespaces so async structure is visible instead of buried in interop noise.[^4_5][^4_4]

A good target shape is:

```cljs
(defn oauth-token-handler
  [runtime config]
  (fn [req reply]
    (-> (parse-token-request req)
        (validate-token-request!)
        (exchange-token! (build-deps runtime config))
        (.then (fn [payload] (.send reply payload)))
        (.catch (fn [err]
                  (json-send! reply 400 {:error "invalid_grant"
                                         :detail (.-message err)}))))))
```

That route is readable because it does orchestration only. Every hard thing has been pushed into a named function, which is the core higher-order-function win you were pointing at.[^4_1][^4_3]

Would you like me to rewrite one of your worst handlers in this style — probably the token exchange or the `/mcp` POST init path?
<span style="display:none">[^4_10][^4_11][^4_12][^4_13][^4_14][^4_15][^4_16][^4_17][^4_18][^4_19][^4_20][^4_21][^4_22][^4_23][^4_24][^4_25][^4_26][^4_27][^4_28][^4_29][^4_30][^4_31][^4_9]</span>

<div align="center">⁂</div>

[^4_1]: https://clojureverse.org/t/promise-handling-in-cljs-using-js-await/8998

[^4_2]: https://ask.clojure.org/index.php/12806/feature-request-async-await-support-in-cljs

[^4_3]: https://clojurescript.org/guides/promise-interop

[^4_4]: https://ericnormand.me/mini-guide/when-to-use-a-macro-in-clojure

[^4_5]: https://clojure-doc.org/articles/language/macros/

[^4_6]: https://github.com/roman01la/cljs-async-await

[^4_7]: https://blog.jeaye.com/2017/09/30/clojurescript-promesa/

[^4_8]: https://github.com/thheller/shadow-cljs/issues/796

[^4_9]: https://github.com/potapenko/cljs-await

[^4_10]: https://github.com/BetterThanTomorrow/calva/issues/1468

[^4_11]: https://gist.github.com/leonoel/79ac90440584ec518f2acf161996a479

[^4_12]: https://github.com/thheller/shadow-cljs/blob/master/src/main/shadow/build/cljs_hacks.cljc

[^4_13]: https://github.com/thheller/shadow-cljs/issues/592

[^4_14]: https://gist.github.com/speedsticko/8057258

[^4_15]: https://github.com/funcool/promesa/issues/54

[^4_16]: https://gist.github.com/beders/06eeb1d8f49de715c6bd2b84f634cff6?permalink_comment_id=2706160

[^4_17]: https://gist.github.com/led/starred?direction=desc\&sort=updated

[^4_18]: https://github.com/funcool/promesa?search=1

[^4_19]: https://gist.github.com/pesterhazy/c4bab748214d2d59883e05339ce22a0f

[^4_20]: https://gist.github.com/pjagielski/538ecac3dd04c7ca939d

[^4_21]: https://gist.github.com/ITSecMedia/forked

[^4_22]: https://github.com/athos/kitchen-async

[^4_23]: https://www.reddit.com/r/Clojure/comments/f4nlau/clojurescript_and_asyncawait/

[^4_24]: https://shadow-cljs.github.io/docs/UsersGuide.html

[^4_25]: https://github.com/active-group/cljs-async

[^4_26]: https://www.reddit.com/r/Clojure/comments/clqcm7/how_does_clojurescript_handles_async/

[^4_27]: https://stackoverflow.com/questions/61558544/how-to-create-an-async-function-in-clojurescript

[^4_28]: https://martinklepsch.org/posts/embracing-js-files-in-clojurescript/

[^4_29]: https://clojureverse.org/t/how-to-deal-with-js-async-and-js-await-in-clojurescript/10234

[^4_30]: https://stackoverflow.com/questions/65669171/clojurescript-cljs-core-async-go-doesnt-work-inside-jsdom

[^4_31]: https://stackoverflow.com/questions/50353272/what-is-the-idiomatic-way-to-implement-await-calls-in-clojurescript


---

# do it rewrite that.

Yes. I’d rewrite the **token exchange** first, because it is compact enough to prove the pattern and ugly enough to matter. The same structure will transfer to `/mcp` POST afterward.[^5_1][^5_2]

## Signal

Facts: the async pain here comes from mixing validation, Redis access, PKCE verification, mutation, and HTTP response handling inside one nested route body. A cleaner shape is to keep the route thin and move each async stage into a named function that returns a Promise.[^5_2][^5_3]

Here is a direct CLJS rewrite using higher-order functions and a flatter Promise pipeline:

```cljs
(ns knoxx.backend.mcp-http.oauth
  (:require [clojure.string :as str]
            [knoxx.backend.mcp-http.util :as u]))

(defn parse-token-request
  [req]
  (let [body (or (aget req "body") #js {})]
    {:grant-type   (str (or (aget body "grant_type")
                            (aget body "grantType")
                            ""))
     :code         (str (or (aget body "code") ""))
     :code-verifier (str (or (aget body "code_verifier")
                             (aget body "codeVerifier")
                             ""))
     :client-id    (str (or (aget body "client_id")
                            (aget body "clientId")
                            ""))
     :redirect-uri (str (or (aget body "redirect_uri")
                            (aget body "redirectUri")
                            ""))}))

(defn validate-token-request!
  [{:keys [grant-type code code-verifier client-id redirect-uri] :as req}]
  (if (or (not= grant-type "authorization_code")
          (str/blank? code)
          (str/blank? code-verifier)
          (str/blank? client-id)
          (str/blank? redirect-uri))
    (js/Promise.reject (js/Error. "invalid_request"))
    (js/Promise.resolve req)))

(defn ensure-client-redirect-allowed!
  [{:keys [redis]} {:keys [client-id redirect-uri] :as req}]
  (-> (u/get-registered-client redis client-id)
      (.then
       (fn [client]
         (when (and client (not (u/redirect-uri-allowed? client redirect-uri)))
           (throw (js/Error. "redirect_uri not allowed for registered client")))
         req))))

(defn load-code-record!
  [{:keys [redis]} {:keys [code] :as req}]
  (-> (.get redis (str "knoxx:mcp:code:" code))
      (.then
       (fn [raw]
         (when-not raw
           (throw (js/Error. "Unknown or expired code")))
         (assoc req :code-record (js/JSON.parse raw))))))

(defn validate-code-record!
  [{:keys [crypto]} {:keys [client-id redirect-uri code-verifier code-record] :as req}]
  (when (or (not= (aget code-record "clientId") client-id)
            (not= (aget code-record "redirectUri") redirect-uri))
    (throw (js/Error. "Client/redirect mismatch")))
  (let [expected (str (or (aget code-record "codeChallenge") ""))
        actual   (u/pkce-challenge crypto code-verifier)]
    (when (or (str/blank? expected) (not= expected actual))
      (throw (js/Error. "PKCE verification failed"))))
  (js/Promise.resolve req))

(defn build-token-record
  [{:keys [crypto token-ttl]} {:keys [client-id code-record]}]
  (let [access-token (.randomUUID crypto)]
    {:access-token access-token
     :token-key    (str "knoxx:mcp:token:" access-token)
     :token-value  #js {:accessToken access-token
                        :clientId client-id
                        :membershipId (aget code-record "membershipId")
                        :userEmail (aget code-record "userEmail")
                        :orgSlug (aget code-record "orgSlug")
                        :tools (aget code-record "tools")
                        :createdAt (.toISOString (js/Date.))
                        :expiresAt (.toISOString
                                    (js/Date. (+ (.now js/Date)
                                                 (* token-ttl 1000))))}}))

(defn persist-token!
  [{:keys [redis token-ttl]} {:keys [code code-record] :as req}]
  (let [{:keys [access-token token-key token-value]} (build-token-record {:crypto (:crypto req)
                                                                          :token-ttl token-ttl}
                                                                         req)
        membership-id (aget code-record "membershipId")]
    (-> (.del redis (str "knoxx:mcp:code:" code))
        (.then
         (fn [_]
           (.set redis token-key (js/JSON.stringify token-value) #js {:EX token-ttl})))
        (.then
         (fn [_]
           (if membership-id
             (.sAdd redis (str "knoxx:mcp:user:" membership-id ":tokens") access-token)
             (js/Promise.resolve nil))))
        (.then
         (fn [_]
           {:access_token access-token
            :token_type "Bearer"
            :scope (->> (js/Array.from (or (aget code-record "tools") #js []))
                        (str/join " "))
            :expires_in token-ttl})))))

(defn exchange-token!
  [deps token-req]
  (-> (validate-token-request! token-req)
      (.then #(ensure-client-redirect-allowed! deps %))
      (.then #(load-code-record! deps %))
      (.then #(validate-code-record! deps %))
      (.then #(persist-token! (assoc deps :crypto (:crypto deps)) %))))
```

That version keeps each step single-purpose: parse, validate request, validate client redirect, load code, validate PKCE, persist token, format response. That is the higher-order-function move you were aiming at.[^5_3][^5_2]

## Route wrapper

The route itself should become almost boring. That is the point.[^5_3]

```cljs
(defn oauth-token-handler
  [{:keys [runtime config token-ttl] :as deps}]
  (fn [req reply]
    (if-let [redis (u/require-redis! reply)]
      (let [crypto (aget runtime "crypto")
            req*   (parse-token-request req)
            deps*  {:redis redis
                    :crypto crypto
                    :token-ttl token-ttl}]
        (-> (exchange-token! deps* req*)
            (.then (fn [payload]
                     (.send reply (clj->js payload))))
            (.catch (fn [err]
                      (u/json-send! reply 400
                                    {:error "invalid_grant"
                                     :detail (.-message err)})))))
      nil)))
```

This is the real readability gain: the handler no longer contains business logic, only orchestration and HTTP translation.[^5_2]

## Fix one bug

There is a subtle smell in the old code: token construction, Redis deletion, Redis persistence, optional set membership update, and response rendering were all embedded inside one deep `.then` tower. That shape makes it very easy to lose track of which values are in scope and to accidentally couple unrelated steps.[^5_2]

In the rewrite, each stage has an explicit input/output contract:

- `validate-token-request!` → validated request
- `ensure-client-redirect-allowed!` → same request
- `load-code-record!` → request + code record
- `validate-code-record!` → same request
- `persist-token!` → final response map

That is basically “epistemic TDD for async”: each step establishes one fact, then passes forward a slightly richer state.[^5_4]

## Frames

Facts: flattening async syntax helps, but named functions help more over time because they turn nested scope into explicit state transitions.  Interpretation: you do not need “async/await everywhere” as much as you need workflow-shaped functions with stable boundaries.[^5_1][^5_3][^5_2]

Frame: $(refactor, p=0.92)$ լա route handlers should narrate the workflow, not implement it.[^5_3]
Frame: $(async, p=0.88)$ լա the unit of simplification is not the lambda; it is the stage.[^5_2]

## Next

If you follow this pattern, the `/mcp` POST initializer should split into these stages:

- `require-initialize-request!`
- `load-token-record!`
- `resolve-request-context!`
- `compute-effective-tools!`
- `build-mcp-server!`
- `connect-transport!`
- `handle-initial-request!`

That route is a bigger win than token exchange, but token exchange is the safer proof-of-pattern first step.[^5_4][^5_2]

I can rewrite the `/mcp` POST path next in the same style.
<span style="display:none">[^5_5][^5_6][^5_7][^5_8][^5_9]</span>

<div align="center">⁂</div>

[^5_1]: https://clojureverse.org/t/promise-handling-in-cljs-using-js-await/8998

[^5_2]: https://clojurescript.org/guides/promise-interop

[^5_3]: https://ericnormand.me/mini-guide/when-to-use-a-macro-in-clojure

[^5_4]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^5_5]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^5_6]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^5_7]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^5_8]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md

[^5_9]: let-s-review-our-contract-dsl-wp6t0Mw9T6OpkTnL2eDQBA.md


---

# do it

Yes — here’s the `/mcp` POST rewrite in the same style: thin route, named async stages, much flatter scope. The pattern is the same one recommended in your own notes: use Promise-returning functions, extract event/interop logic into helpers, and let the route narrate the workflow instead of embodying it.[^6_1][^6_2]

## Signal

Facts: the current `/mcp` POST path actually contains two workflows — resume existing session, or initialize a new one — and both are tangled with auth, MCP server construction, tool filtering, transport wiring, and raw HTTP handling. Splitting those into stages is the highest-leverage refactor because it removes the deepest nesting from the codebase.[^6_2][^6_1]

Here is the rewrite, assuming you keep `mcp-sessions*`, `bearer-token`, `resolve-session-id`, `require-redis!`, and `json-send!` in a util/runtime namespace.[^6_1]

```cljs
(ns knoxx.backend.mcp-http.mcp
  (:require [clojure.string :as str]
            [knoxx.backend.mcp-expose :as mcp-expose]
            [knoxx.backend.mcp-http.util :as u]))

(defn load-token-record!
  [{:keys [redis]} req]
  (let [token (u/bearer-token req)]
    (if (str/blank? token)
      (js/Promise.resolve nil)
      (-> (.get redis (str "knoxx:mcp:token:" token))
          (.then
           (fn [raw]
             (when raw
               (try (js/JSON.parse raw)
                    (catch :default _ nil)))))
          (.catch (fn [_] nil))))))

(defn require-initialize-request!
  [{:keys [isInitializeRequest]} req]
  (if (and isInitializeRequest (isInitializeRequest (aget req "body")))
    (js/Promise.resolve req)
    (js/Promise.reject
     (js/Error.
      "Bad Request: Server not initialized"))))

(defn token-record->headers-like
  [token-record]
  (let [headers-like #js {}
        mid   (aget token-record "membershipId")
        email (aget token-record "userEmail")
        org   (aget token-record "orgSlug")]
    (when mid   (aset headers-like "x-knoxx-membership-id" mid))
    (when email (aset headers-like "x-knoxx-user-email" email))
    (when org   (aset headers-like "x-knoxx-org-slug" org))
    headers-like))

(defn resolve-request-context!
  [{:keys [policyDb]} token-record]
  (-> (.resolveRequestContext policyDb (token-record->headers-like token-record))
      (.then (fn [ctx] {:token-record token-record :ctx ctx}))))

(defn effective-tools
  [runtime config ctx token-record]
  (let [all-tools (or (mcp-expose/create-knoxx-custom-tools-js runtime config ctx) #js [])
        allowed   (into #{} (map str) (array-seq (or (aget token-record "tools") #js [])))]
    (->> (array-seq all-tools)
         (filter (fn [t] (contains? allowed (str (aget t "name")))))
         (into-array))))

(defn register-effective-tools!
  [{:keys [server z]} tools]
  (let [input-schema (when z (.record z (.any z)))]
    (doseq [tool (array-seq tools)]
      (let [tool-name (-> (str (or (aget tool "name") "")) str/trim)]
        (when-not (str/blank? tool-name)
          (.registerTool
           server
           tool-name
           #js {:description (str (or (aget tool "description")
                                      (aget tool "label")
                                      tool-name))
                :inputSchema input-schema}
           (fn [params]
             (.execute tool "mcp" params nil nil nil))))))))

(defn make-transport
  [{:keys [crypto StreamableHTTPServerTransport mcp-sessions*]} token-record]
  (let [transport
        (new StreamableHTTPServerTransport
             #js {:sessionIdGenerator (fn [] (.randomUUID crypto))
                  :onsessioninitialized
                  (fn [sid]
                    (swap! mcp-sessions* assoc (str sid)
                           {:transport transport
                            :token (aget token-record "accessToken")}))})]
    (set! (.-onclose transport)
          (fn []
            (when-let [sid (.-sessionId transport)]
              (swap! mcp-sessions* dissoc (str sid)))))
    transport))

(defn build-server-and-transport!
  [{:keys [runtime config McpServer z] :as deps}
   {:keys [ctx token-record]}]
  (let [server    (new McpServer #js {:name "knoxx" :version "0.1.0"})
        tools     (effective-tools runtime config ctx token-record)
        transport (make-transport deps token-record)]
    (register-effective-tools! {:server server :z z} tools)
    (js/Promise.resolve
     {:server server
      :transport transport
      :token-record token-record
      :ctx ctx})))

(defn connect-and-handle!
  [{:keys [req reply]} {:keys [server transport]}]
  (-> (.connect server transport)
      (.then
       (fn [_]
         (.handleRequest transport
                         (aget req "raw")
                         (aget reply "raw")
                         (aget req "body"))))))

(defn unauthorized!
  [reply]
  (do (.code reply 401)
      (.send reply "Unauthorized")))

(defn bad-init!
  [reply]
  (do (.code reply 400)
      (.send reply
             #js {:jsonrpc "2.0"
                  :error #js {:code -32000
                              :message "Bad Request: Server not initialized"}
                  :id nil})))

(defn handle-existing-session!
  [{:keys [mcp-sessions*]} req reply]
  (let [session-id (u/resolve-session-id req)
        existing   (when session-id (get @mcp-sessions* session-id))
        supplied   (u/bearer-token req)]
    (cond
      (nil? existing)
      nil

      (not= (str supplied) (str (:token existing)))
      (unauthorized! reply)

      :else
      (.handleRequest (:transport existing)
                      (aget req "raw")
                      (aget reply "raw")
                      (aget req "body")))))

(defn initialize-session!
  [{:keys [runtime config policyDb crypto McpServer
           StreamableHTTPServerTransport isInitializeRequest
           z mcp-sessions*] :as deps}
   req reply]
  (if-let [redis (u/require-redis! reply)]
    (-> (require-initialize-request! {:isInitializeRequest isInitializeRequest} req)
        (.then (fn [_] (load-token-record! {:redis redis} req)))
        (.then
         (fn [token-record]
           (if-not token-record
             (do (unauthorized! reply)
                 (js/Promise.resolve nil))
             (resolve-request-context!
              {:policyDb policyDb}
              token-record))))
        (.then
         (fn [state]
           (if-not state
             (js/Promise.resolve nil)
             (build-server-and-transport!
              {:runtime runtime
               :config config
               :McpServer McpServer
               :z z
               :crypto crypto
               :StreamableHTTPServerTransport StreamableHTTPServerTransport
               :mcp-sessions* mcp-sessions*}
              state))))
        (.then
         (fn [state]
           (if-not state
             (js/Promise.resolve nil)
             (connect-and-handle! {:req req :reply reply} state))))
        (.catch
         (fn [err]
           (if (= "Bad Request: Server not initialized" (.-message err))
             (bad-init! reply)
             (do
               (.error js/console "[knoxx-mcp] initialize failed" err)
               (u/json-send! reply 500
                             {:error "mcp_init_failed"
                              :detail (.-message err)}))))))
    nil))

(defn mcp-post-handler
  [deps]
  (fn [req reply]
    (if (handle-existing-session! deps req reply)
      nil
      (initialize-session! deps req reply))))
```


## What changed

Facts: the monolith is now split into explicit stages — session reuse, initialize validation, token load, request-context resolution, effective tool computation, server/transport construction, and final connect/handle. Each stage has one input/output contract instead of one giant ambient scope.[^6_2][^6_1]

That gives you these readable boundaries:

- `handle-existing-session!` handles only resumed sessions.
- `initialize-session!` handles only first-time initialization.
- `resolve-request-context!` converts token claims into policy context.
- `build-server-and-transport!` constructs MCP machinery.
- `connect-and-handle!` performs the final side effect.

This is the higher-order refactor you were asking for: not “fewer async operations,” but “fewer anonymous scopes.”[^6_1][^6_2]

## One more reduction

The event-heavy part is still `registerTool` plus `transport` lifecycle wiring, but even that is now local and named. Your notes specifically pointed out that extracting named event handlers and dispatch tables is the lever that collapses 10+ levels of nesting in similar code, and the same principle applies here.[^6_1]

If you want one more step of cleanup, turn `build-server-and-transport!` into three pieces:

- `compute-effective-tools`
- `new-mcp-server`
- `new-session-transport`

That would make the MCP initialization path read almost like a spec instead of a handler.[^6_1]

## Frames

Facts: `js-await` or a `defn-async` macro can flatten this even further, but the biggest gain already came from moving each async stage into a named Promise-returning function.  Interpretation: async syntax helps readability locally, while higher-order decomposition helps readability globally; you needed the second more than the first.[^6_3][^6_2][^6_1]

Frame: $(refactor, p=0.93)$ լա the route is now orchestration; the workflow lives in named stages.[^6_2]
Frame: $(agents, p=0.87)$ լա this shape is far easier for agents to preserve than one giant nested form, because the contracts between functions are explicit.[^6_1]

## Next

The next best cleanup is to wrap this pattern in a tiny `defn-async` macro so the workflow reads closer to direct style. Your own notes suggest a production-friendly form where `defn-async` simply lowers to chained `js-await` rather than trying to invent a whole compiler feature.[^6_1]

I can do either of these next:

- `#η` write the minimal `defn-async` / `await` macro pair for shadow-cljs and convert both rewritten handlers to use it.[^6_1]
- Rewrite the OAuth authorize/confirm flow in the same staged style, which is probably your other big nesting hotspot.

<div align="center">⁂</div>

[^6_1]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md

[^6_2]: https://clojurescript.org/guides/promise-interop

[^6_3]: https://clojureverse.org/t/promise-handling-in-cljs-using-js-await/8998


---

# \#η

Use `defn-async` for the outer function and `js-await` inside it. The simplest working shape is a macro that lowers to a normal `defn` returning a Promise, with `js-await` handling the sequential async steps.[^7_1]

```clj
;; my/async.clj
(ns my.async)

(defn await-form? [x]
  (and (seq? x) (= 'await (first x))))

(defn lower-awaits [body]
  (letfn [(step [forms]
            (when (seq forms)
              (let [f (first forms)
                    more (next forms)]
                (cond
                  (and (seq? f) (= 'let (first f)))
                  (let [[_ bindings & let-body] f
                        pairs (partition 2 bindings)
                        body* (reduce
                               (fn [acc [sym expr]]
                                 (if (await-form? expr)
                                   `(shadow.cljs.modern/js-await [~sym ~(second expr)]
                                      ~acc)
                                   `(let [~sym ~expr] ~acc)))
                               `(do ~@let-body)
                               (reverse pairs))]
                    (if more `(do ~body* ~(step more)) body*))

                  (await-form? f)
                  `(shadow.cljs.modern/js-await ~(second f)
                     ~(if more `(do ~(step more)) nil))

                  more
                  `(do ~f ~(step more))

                  :else
                  f)))))]
    (step body)))

(defmacro defn-async [name args & body]
  `(defn ~name ~args
     ~(lower-awaits body)))
```

```cljs
(ns app.core
  (:require-macros [my.async :refer [defn-async]]))

(defn-async fetch-json [url]
  (let [resp (await (js/fetch url))
        data (await (.json resp))]
    data))
```

If you want the more direct version without a custom macro, just write ordinary `defn` + `js-await`:

```cljs
(defn fetch-json [url]
  (shadow.cljs.modern/js-await [resp (js/fetch url)]
    (shadow.cljs.modern/js-await [data (.json resp)]
      data)))
```

That said, the real win is not the macro itself — it is using `defn-async` to force a workflow shape and keeping the nested business logic in named helpers.

<div align="center">⁂</div>

[^7_1]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md


---

# Summarize everything that we did here as a spec. Make sure that the macro parts that are generalized. We want *all* our routes to be able to follow this pattern

\#μ **Knoxx Async Route Handler Spec**

```edn
{:spec/name      "knoxx/backend/http-async-routes"
 :spec/version   "0.1.0"
 :spec/purpose   "Flatten async nesting in Knoxx MCP handlers to workflow orchestration"
 :spec/requires  ["shadow-cljs 2.28+" "malli 1.0+"]
 :spec/validate? true}

;; 1. Async Macro Layer (.cljc)
(def defn-async
  "Defines a function that always returns Promise.
   Lowers body to chained js-await, handles let/await sequencing."
  {:spec/shape '(defn-async name args & body)}
  ;; Implementation: see η macro above
  )

(defmacro defroute
  "Defines a Ring-like route handler with async orchestration.
   Expands to thin route + named workflow stages."
  {:spec/shape '(defroute method path schema workflow-fn)
   :spec/example '(defroute POST /mcp {:body ChatBody} mcp-workflow)}
  [method path schema workflow-fn]
  `(route! ~app ~method ~path
           (fn [req# reply#]
             (if-let [deps# (u/route-deps req# reply#)]
               (u/with-request-context!
                 (:runtime deps#) req# reply#
                 (fn [ctx#]
                   (let [body# (u/decode ~schema (aget req# "body"))]
                     (~workflow-fn deps# ctx# body# req# reply#))))
               nil))))

;; 2. Core Workflow Pattern
(def workflow-stage
  "Each stage: single async responsibility, Promise input → Promise output.
   Named, testable, composable."
  {:spec/shape '(defn-stage name deps ctx input → Promise[output])
   :spec/example '(defn-stage load-token deps req → Promise[token-record])})

(def workflow-composition
  "Route orchestration: thin dispatch → sequence of stages → HTTP translation."
  {:spec/shape '(defn-async workflow deps ctx body req reply → nil)
   :spec/stages [:parse :validate :load :resolve :build :handle :respond]})

;; 3. Route Handler Contract
(def route-handler
  "All Knoxx HTTP routes follow this shape."
  {:spec/shape '(fn [req reply] → nil)
   :spec/behavior
   {:guards     [:require-redis :require-runtime :authorize]
    :parse      [:malli/decode]
    :workflow   [:defn-async stages]
    :respond    [:json-send! :error-boundary]
    :side-fx    [:log :metrics]}
   :spec/error-handling [:catch → 4xx/5xx json-response]})

;; 4. Concrete Examples
(def token-exchange-workflow
  {:stages [:parse-token-req
            :validate-req!
            :ensure-client!
            :load-code!
            :validate-pkce!
            :persist-token!
            :respond]})

(def mcp-post-workflow
  {:guards [:existing-session?]
   :stages [:require-init!
            :load-token!
            :resolve-ctx!
            :effective-tools
            :build-server!
            :connect-handle!
            :respond]
   :dispatch [:existing → handle-resume
              :new → initialize-session!]})

;; 5. Validation & Tooling
(def validate-workflow
  "Dev-mode: validate workflow stages form malli schema."
  {:spec/schema [:map
                 [:stages [:vector keyword?]]
                 [:guards [:set keyword?]]]})

(def lint-handler
  "CI: lint all defroute for workflow shape."
  {:spec/checks [:returns-promise?
                 :uses-named-stages?
                 :no-nested-lambdas?
                 :malli-schema?]})
```


## Key Takeaways

1. **Macro Substrate**: `defn-async` lowers to `js-await` chains; `defroute` expands to thin Ring handler + workflow dispatch.[^8_1]
2. **Workflow Shape**: Every route = `parse → validate → load → resolve → build → handle → respond`. Named stages, no nesting.[^8_1]
3. **Schema Integration**: Malli schemas gate request/response; `decode` handles JS field aliasing (`or aget body foo fooBar`).[^8_1]
4. **Migration Path**: Existing `.then` pyramids → named `defn-async` stages → readable orchestration. Zero runtime risk.[^8_1]
5. **All Routes**: Copy/paste the pattern. `defroute` macro enforces it. Agents can read/preserve the shape.[^8_1]

This is the canonical Knoxx MCP handler pattern. Every route should match it.[^8_1]

<div align="center">⁂</div>

[^8_1]: async-functions-in-shadow-cljs-f48jwK.kSKKvpx1YlcTJjg.md


---

# Aight, go ahead and scan the knoxx repo for routes and... we're not just focused on MCP with this. we want all our routes broken down according to the specs/knowledge-ops-contract-runtime-dod-structure.md definitions.

[https://github.com/open-hax/knoxx/pull/15](https://github.com/open-hax/knoxx/pull/15)

I have everything I need from the PR diff. Here's the full route breakdown mapped against the DoD/spec pattern.

***

\#μ **Knoxx Route Inventory — PR \#15 Spec Breakdown**

Every route is categorized across five axes pulled from the PR: **auth model**, **guard chain**, **async shape**, **error contract**, and **DoD status** (what's missing to meet the pattern).

***

## Route Taxonomy

### Tier 0 — Public / Discovery

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `GET` | `/.well-known/oauth-authorization-server` | None | None | Sync `.send` | ✅ Complete |
| `POST` | `/api/mcp/oauth/register` | None | `require-redis!` | `.then/.catch` pyramid | ⚠ Needs `defn-async` flatten |

### Tier 1 — Browser Session (Cookie Auth)

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `GET` | `/api/mcp/oauth/authorize` | `require-browser-auth-context!` | `require-redis!` | `.then` pyramid (4 levels deep) | ⚠ Needs workflow stages |
| `GET` | `/api/mcp/oauth/authorize/confirm` | `require-browser-auth-context!` | `require-redis!` | Delegated to `handle-oauth-authorize-confirm!` | ✅ Extracted, still `.then` chains |
| `GET` | `/api/mcp/tokens` | `require-browser-auth-context!` | `require-redis!` | `.then` pyramid | ⚠ Needs workflow stages |
| `DELETE` | `/api/mcp/tokens/:tokenId` | `require-browser-auth-context!` | `require-redis!` | `.then` pyramid | ⚠ Needs workflow stages |

### Tier 2 — Bearer Token (MCP Transport)

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `POST` | `/api/mcp/oauth/token` | PKCE code verifier | `require-redis!` | `.then` pyramid (6 levels deep) | ⚠ Worst offender — needs named stages |
| `POST` | `/mcp` | `load-token-record` + session | `require-redis!` | letfn + nested `.then` | ⚠ Needs `defn-async` + stages |
| `GET` | `/mcp` | session token match | `require-redis!` (indirect) | sync dispatch | ⚠ Partial — no error boundary |
| `DELETE` | `/mcp` | session token match | same | sync dispatch | ⚠ Same as above |

### Tier 3 — Policy Context (with-request-context!)

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `POST` | `/api/multimodal/upload` | `with-request-context!` | `ensure-tool! multimodal.upload` | `.then` chain | ⚠ `ensure-tool!` new, not all routes updated |
| `GET` | `/api/multimodal/:fileId` | same | same | `.then` chain | ⚠ same |
| `GET` | `/api/voice/stt/health` | `with-request-context!` | `ensure-tool! multimodal.upload` | `.then` chain | ✅ Migrated |
| `POST` | `/api/voice/stt` | same | same | `.then` chain | ✅ Migrated |
| `GET` | `/api/voice/tts/health` | same | `ensure-tool!` | `.then` chain | ✅ New, correct guard |
| `POST` | `/api/voice/tts` | same | `ensure-tool!` | `.then` chain | ⚠ Needs `defn-async` flatten |

### Tier 4 — Proxy / Admin (No User Auth)

| Method | Path | Auth | Guard | Async Shape | DoD Gap |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `GET` | `/api/admin/pi-sessions/status` | None (system) | None | `Promise.all` + `.then` | ⚠ Needs error boundary |
| `GET` | `/api/admin/pi-sessions` | None | None | `.then/.catch` | ⚠ try/catch mixed style |
| `POST` | `/api/admin/pi-sessions/ingest` | None | None | `.then` pyramid | ⚠ Needs workflow stages |
| `ALL` | `/api/ingestion/*` | None | None | proxy `.then` | ⚠ No typed schema |
| `ALL` | `/api/openplanner/*` | None | None | proxy `.then` | ⚠ No typed schema |


***

## Canonical Route Pattern (DoD)

Every Knoxx route must satisfy these 5 contracts:

```clojure
;; 1. GUARD — fail fast, no nesting
(defn-async my-route [deps ctx body req reply]
  (let [r    (or (require-redis! reply) (return! nil))   ;; Tier 0-2
        ctx  (or (with-request-context! ...) (return! nil)) ;; Tier 3
        _    (ensure-tool! ctx "tool.id")]               ;; Tier 3+

;; 2. PARSE — schema-decoded body, no raw aget in handler
    body (u/decode MyBodySchema (aget req "body"))

;; 3. WORKFLOW — named stages, no inline .then pyramids
    result (<! (load-thing! r params))
    result (<! (validate-thing! result))
    result (<! (persist-thing! r result))

;; 4. RESPOND — single terminal send
    _ (json-send! reply 200 result)]))

;; 5. ERROR BOUNDARY — at defroute level, not per .then
;; (.catch (fn [err] (json-send! reply (classify err) {:error ...})))
```


***

## Migration Priority

**Highest pain** (deeply nested `.then` pyramids, no stages):

1. `POST /api/mcp/oauth/token` — 6 levels, PKCE chain
2. `POST /mcp` — letfn + session init + tool hydration
3. `GET /api/mcp/oauth/authorize` — consent screen + tool list

**Medium** (browser auth + Redis, single workflow):
4. `GET /api/mcp/tokens`
5. `DELETE /api/mcp/tokens/:tokenId`
6. `POST /api/admin/pi-sessions/ingest`

**Low** (mostly correct, small shape fixes):
7. `POST /api/mcp/oauth/register`
8. `POST /api/voice/tts`
9. Proxy routes — add typed passthrough schema

**Done / conformant already:**

- `GET /.well-known/oauth-authorization-server`
- `GET /api/voice/stt/health`, `POST /api/voice/stt`
- `GET /api/voice/tts/health` (new, correct from the start)

---

# describe it in terms of the DSL

Good. Now I have the actual `route!` DSL primitive. Here's every route expressed in terms of it — and what the target `defroute` DSL should look like.

***

\#μ **All Knoxx Routes — In Terms of the DSL**

The current DSL has one primitive: `(route! app method url handler)` from `app-shapes`.  The handler signature is always `(fn [req reply] ...)`. The DoD pattern wraps that in named guard + workflow functions. Here's every route expressed at that level.

***

## The Target DSL Shape

```clojure
;; Every route is a call to this — already exists in app-shapes
(route! app "METHOD" "/path"
  (fn [req reply]
    (guard-chain! req reply
      (fn [deps]
        (workflow! deps req reply body)))))

;; guard-chain! = the sequence of: require-redis!, with-request-context!, ensure-tool!
;; workflow! = named defn-async fn, stages only, no inline .then
```


***

## Tier 0 — Public / No Guard

```clojure
(route! app "GET" "/.well-known/oauth-authorization-server"
  (fn [_req reply]
    (discovery-metadata! config reply)))
;; ✅ done — sync, no guard needed

(route! app "POST" "/api/mcp/oauth/register"
  (fn [req reply]
    (require-redis! reply
      (fn [r]
        (register-oauth-client! r req reply)))))
;; ⚠ register-oauth-client! needs defn-async extraction
```


***

## Tier 1 — Browser Session Guard

```clojure
(route! app "GET" "/api/mcp/oauth/authorize"
  (fn [req reply]
    (require-redis! reply
      (fn [r]
        (require-browser-auth-context! policyDb runtime config req reply
          (fn [ctx]
            (oauth-authorize-screen! r ctx req reply config)))))))
;; ⚠ oauth-authorize-screen! = consent HTML + client validation, needs defn-async

(route! app "GET" "/api/mcp/oauth/authorize/confirm"
  (fn [req reply]
    (handle-oauth-authorize-confirm!
      {:policyDb policyDb :runtime runtime
       :config config :crypto crypto :code-ttl code-ttl}
      req reply)))
;; ⚠ partially extracted — handle-oauth-authorize-confirm! still .then chain

(route! app "GET" "/api/mcp/tokens"
  (fn [req reply]
    (require-redis! reply
      (fn [r]
        (require-browser-auth-context! policyDb runtime config req reply
          (fn [ctx]
            (list-user-tokens! r ctx reply)))))))
;; ⚠ list-user-tokens! needs defn-async extraction

(route! app "DELETE" "/api/mcp/tokens/:tokenId"
  (fn [req reply]
    (require-redis! reply
      (fn [r]
        (require-browser-auth-context! policyDb runtime config req reply
          (fn [ctx]
            (revoke-user-token! r ctx req reply)))))))
;; ⚠ revoke-user-token! needs defn-async extraction
```


***

## Tier 2 — Bearer Token Guard

```clojure
(route! app "POST" "/api/mcp/oauth/token"
  (fn [req reply]
    (require-redis! reply
      (fn [r]
        (exchange-pkce-token! r req reply crypto token-ttl)))))
;; ⚠ exchange-pkce-token! = worst offender, 6-level .then chain
;; Stages: parse-token-req → validate-grant-type! → load-code! →
;;         validate-pkce! → validate-client! → persist-token! → respond

(route! app "POST" "/mcp"
  (fn [req reply]
    (handle-mcp-post! req reply)))
;; ⚠ handle-mcp-post! branches on session-exists? vs initialize
;; Stages (new): require-redis! → load-token-record! → resolve-ctx! →
;;               intersect-tools! → build-server! → connect-handle! → respond

(route! app "GET" "/mcp"
  (fn [req reply]
    (handle-mcp-session! req reply)))
;; ⚠ needs explicit 401 error boundary

(route! app "DELETE" "/mcp"
  (fn [req reply]
    (handle-mcp-session! req reply)))
;; ⚠ same — shared handler, no error boundary
```


***

## Tier 3 — Policy Context Guard (`with-request-context!` + `ensure-tool!`)

```clojure
(route! app "POST" "/api/multimodal/upload"
  (fn [req reply]
    (with-request-context! runtime req reply
      (fn [ctx]
        (ensure-tool! ctx "multimodal.upload")
        (upload-multimodal-files! runtime req reply)))))
;; ✅ guard correct, shape still .then chain

(route! app "GET" "/api/multimodal/:fileId"
  (fn [req reply]
    (with-request-context! runtime req reply
      (fn [ctx]
        (ensure-tool! ctx "multimodal.upload")
        (serve-multimodal-file! runtime req reply)))))
;; ✅ guard correct

(route! app "GET" "/api/voice/stt/health"
  (fn [req reply]
    (with-request-context! runtime req reply
      (fn [ctx]
        (ensure-tool! ctx "multimodal.upload")
        (stt-health! config reply)))))
;; ✅ conformant

(route! app "POST" "/api/voice/stt"
  (fn [req reply]
    (with-request-context! runtime req reply
      (fn [ctx]
        (ensure-tool! ctx "multimodal.upload")
        (stt-transcribe! runtime config req reply)))))
;; ✅ conformant

(route! app "GET" "/api/voice/tts/health"
  (fn [req reply]
    (with-request-context! runtime req reply
      (fn [ctx]
        (ensure-tool! ctx "multimodal.upload")
        (tts-health! config reply)))))
;; ✅ new, correct from the start

(route! app "POST" "/api/voice/tts"
  (fn [req reply]
    (with-request-context! runtime req reply
      (fn [ctx]
        (ensure-tool! ctx "multimodal.upload")
        (tts-synthesize! config req reply)))))
;; ⚠ needs defn-async extraction for tts-synthesize!
```


***

## Tier 4 — Proxy / System (No User Auth)

```clojure
(route! app "GET" "/api/admin/pi-sessions/status"
  (fn [_req reply]
    (pi-sessions-status! config reply)))
;; ⚠ pi-sessions-status! = Promise.all over legacy + KMS, no typed schema

(route! app "GET" "/api/admin/pi-sessions"
  (fn [req reply]
    (list-pi-sessions! config req reply)))
;; ⚠ mixed try/catch + .then style

(route! app "POST" "/api/admin/pi-sessions/ingest"
  (fn [req reply]
    (trigger-pi-ingest! config req reply)))
;; ⚠ needs defn-async extraction

(route! app "ALL" "/api/ingestion/*"
  (fn [req reply]
    (proxy-to! config :ingestion req reply)))
;; ⚠ no malli schema, no typed passthrough

(route! app "ALL" "/api/openplanner/*"
  (fn [req reply]
    (proxy-to! config :openplanner req reply)))
;; ⚠ same — proxy-to! should be a shared defn-async
```


***

## The `defroute` Macro (What All of the Above Collapses Into)

```clojure
;; Desired: every route above becomes one of these four forms

;; Tier 0 – public
(defroute app "GET" "/.well-known/oauth-authorization-server" nil
  discovery-metadata!)

;; Tier 1 – browser session
(defroute app "GET" "/api/mcp/tokens" [:redis :browser-auth]
  list-user-tokens!)

;; Tier 2 – bearer token
(defroute app "POST" "/mcp" [:redis :bearer-token]
  handle-mcp-post!)

;; Tier 3 – policy context
(defroute app "POST" "/api/voice/tts" [:ctx "multimodal.upload"]
  tts-synthesize!)

;; Tier 4 – proxy
(defroute app "ALL" "/api/ingestion/*" [:proxy :ingestion]
  proxy-to!)
```

The guard vector `[:redis :browser-auth]` is the entire declarative DoD.  The macro expands it to the appropriate `require-redis!` / `require-browser-auth-context!` / `ensure-tool!` chain, and the workflow fn (`tts-synthesize!`, etc.) is always a named `defn-async` with no inline `.then` chains. That's the full contract.

