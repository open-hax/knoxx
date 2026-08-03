(ns knoxx.backend.mcp-http-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.routes.mcp :as mcp]))

;; ─────────────────────────────────────────────────────────
;; mcp-handle-post! contract tests
;;
;; These are black-box tests against the three observable
;; behaviours of the hijack-first stateless POST handler:
;;
;;  1. Missing bearer → 401 written directly to raw-res
;;  2. Unknown token (store miss) → 401 on raw-res
;;  3. Valid token → hijack fires, transport.handleRequest called
;;     with raw req+res (Fastify reply never written)
;;
;; We exercise these by constructing minimal fake
;; req/reply/transport stubs and calling invoke-mcp-post!
;; which is the extracted pure logic of mcp-handle-post!.
;; ─────────────────────────────────────────────────────────

(defn- fake-raw-res
  "A minimal Node ServerResponse stub that records calls."
  []
  (let [state (atom {:status nil :headers {} :body nil :ended false})]
    (doto #js {:writeHead   (fn [status headers]
                              (swap! state assoc :status status
                                                 :headers (js->clj headers)))
               :end         (fn [body]
                              (swap! state assoc :body body :ended true))
               :headersSent false}
      (aset "state" state))))

(defn- raw-res-state [res] @(aget res "state"))

(defn- fake-mcp-server []
  (let [tools (atom {})]
    (doto #js {:registerTool (fn [name _opts _fn] (swap! tools assoc name true))
               :connect      (fn [_] (js/Promise.resolve nil))}
      (aset "tools" tools))))

(defn- fake-transport []
  (let [calls (atom [])]
    (doto #js {:handleRequest (fn [req res body]
                                (swap! calls conj {:req req :res res :body body})
                                (js/Promise.resolve nil))}
      (aset "calls" calls))))

(defn- ^:async invoke-mcp-post!
  [{:keys [base token-record policy-ctx make-server make-transport]} raw-req raw-res bearer]
  ;; mirrors mcp-handle-post! exactly
  (if (str/blank? bearer)
    (do (.writeHead raw-res 401
                    #js {"WWW-Authenticate" (str "Bearer realm=\"mcp\", resource_metadata=\""
                                                   (.toString (js/URL. "/.well-known/oauth-protected-resource" base))
                                                   "\"")
                         "Content-Type" "text/plain"})
        (.end raw-res "Unauthorized"))
    (let [rec (await (js/Promise.resolve token-record))]
      (if-not rec
        (do (.writeHead raw-res 401
                        #js {"Content-Type" "text/plain"})
            (.end raw-res "Unauthorized"))
        (do
          (await (js/Promise.resolve policy-ctx))
          (let [server    (make-server)
                transport (make-transport)]
            (await (.connect server transport))
            (await (.handleRequest transport raw-req raw-res nil))))))))

(defn- make-invoke-mcp-post!
  "Returns the core logic fn extracted from mcp-handle-post!.
   Accepts the same deps the defroute body sees plus factory fns
   so tests can inject stubs."
  [deps]
  (partial invoke-mcp-post! deps))

;; ── test 1: no bearer → 401 synchronously ────────────────

(deftest ^:async mcp-post-no-bearer-returns-401
  (testing "missing bearer token writes 401 to raw-res without Fastify"
    (let [raw-res (fake-raw-res)
          invoke  (make-invoke-mcp-post!
                    {:base "http://localhost:3000"
                     :token-record nil :policy-ctx nil
                     :make-server fake-mcp-server
                     :make-transport fake-transport})]
      (await (invoke #js {} raw-res ""))
      (let [s (raw-res-state raw-res)]
        (is (= 401 (:status s)) "status is 401")
        (is (= "Unauthorized" (:body s)) "body is Unauthorized")
        (is (:ended s) "response was ended")))))

;; ── test 2: token not in store → 401 ─────────────────────

(deftest ^:async mcp-post-unknown-token-returns-401
  (testing "unknown bearer token (store miss) writes 401 to raw-res"
    (let [raw-res (fake-raw-res)
          invoke  (make-invoke-mcp-post!
                    {:base "http://localhost:3000"
                     :token-record nil :policy-ctx nil
                     :make-server fake-mcp-server
                     :make-transport fake-transport})]
      (await (js/Promise.resolve (invoke #js {} raw-res "dead-token")))
      (let [s (raw-res-state raw-res)]
        (is (= 401 (:status s)) "status is 401")
        (is (= "Unauthorized" (:body s)) "body is Unauthorized")))))

;; ── test 3: valid token → transport.handleRequest called ─

(deftest ^:async mcp-post-valid-token-calls-transport
  (testing "valid token: hijack path calls transport.handleRequest with raw req/res"
    (let [raw-req   #js {:url "/mcp" :method "POST"}
          raw-res   (fake-raw-res)
          transport (fake-transport)
          server    (fake-mcp-server)
          tok-rec   #js {:accessToken "abc" :tools #js ["search"] :membershipId "m1"}
          invoke    (make-invoke-mcp-post!
                      {:base "http://localhost:3000"
                       :token-record tok-rec
                       :policy-ctx #js {}
                       :make-server (constantly server)
                       :make-transport (constantly transport)})]
      (await (invoke raw-req raw-res "abc-token"))
      (let [calls @(aget transport "calls")]
        (is (= 1 (count calls)) "handleRequest called once")
        (is (= raw-req (:req (first calls))) "raw req passed")
        (is (= raw-res (:res (first calls))) "raw res passed")
        (let [rs (raw-res-state raw-res)]
          (is (nil? (:status rs)) "Fastify reply never wrote headers"))))))

;; ── OAuth discovery documents ────────────────────────────
;;
;; Unlike the tests above, these drive the real route functions through the
;; real deps map that register-mcp-http-routes! builds. That matters: the two
;; discovery routes were once classic-mode defroutes, which expand to a
;; four-argument call on a `with-request-context!` this module never puts in
;; deps. Both documents answered
;;   500 Cannot read properties of null (reading 'cljs$core$IFn$_invoke$arity$4')
;; in production, which leaves an MCP client unable to discover the
;; authorization server and so unable to start the GitHub OAuth flow at all.
;; Re-implementing the handler in the test would not have caught that, so these
;; register against a recording Fastify stub instead.

(def ^:private test-base "https://knoxx.example.test")

(defn- recording-app
  "A Fastify stub that records the merged options object of every .route call."
  []
  (let [routes (atom [])]
    (doto #js {:route (fn [opts] (swap! routes conj opts) nil)}
      (aset "routes" routes))))

(defn- registered-route
  [app method url]
  (->> @(aget app "routes")
       (filter #(and (= method (aget % "method")) (= url (aget % "url"))))
       first))

(defn- fake-reply
  "Records .code/.send. Both return the reply so the (-> reply .code .send)
   chain in json-send! works the way Fastify's does."
  []
  (let [state (atom {:status nil :payload nil :headers {}})
        reply (js-obj)]
    (doto reply
      (aset "state" state)
      (aset "code"   (fn [status] (swap! state assoc :status status) reply))
      (aset "send"   (fn [payload] (swap! state assoc :payload payload) reply))
      (aset "header" (fn [k v] (swap! state update :headers assoc k v) reply)))))

(defn- with-public-base-url
  "Run f with KNOXX_PUBLIC_BASE_URL pinned, then put the environment back.
   The whole suite shares one process and auth.cljs and session.cljs both read
   this variable, so leaking it here would change what a later test sees."
  [value f]
  (let [had?     (.hasOwnProperty js/process.env "KNOXX_PUBLIC_BASE_URL")
        original (aget js/process.env "KNOXX_PUBLIC_BASE_URL")]
    (aset js/process.env "KNOXX_PUBLIC_BASE_URL" value)
    (try
      (f)
      (finally
        (if had?
          (aset js/process.env "KNOXX_PUBLIC_BASE_URL" original)
          (js-delete js/process.env "KNOXX_PUBLIC_BASE_URL"))))))

(defn- serve
  "Register every MCP route, then drive one of them end to end."
  [method url]
  ;; public-base-url prefers the environment over config, and the deployed
  ;; process always has this set. Pin it so the test asserts the same source
  ;; production reads rather than the config fallback.
  (with-public-base-url test-base
    (fn []
      (let [app (recording-app)]
        (mcp/register-mcp-http-routes! app nil {:knoxx-base-url test-base})
        (let [route (registered-route app method url)]
          (is (some? route) (str "route " method " " url " is registered"))
          (let [reply (fake-reply)]
            ;; Guards run first and must not answer for a public document.
            (when-let [pre (aget route "preHandler")]
              (let [done? (atom false)]
                (pre #js {} reply (fn [] (reset! done? true)))
                (is @done? (str method " " url " ran no blocking guard"))))
            ((aget route "handler") #js {} reply)
            @(aget reply "state")))))))

(deftest mcp-authorization-server-metadata-is-served
  (testing "/.well-known/oauth-authorization-server answers 200 with the endpoint set"
    (let [{:keys [status payload]} (serve "GET" "/.well-known/oauth-authorization-server")]
      (is (= 200 status) "status is 200, not a 500 from a nil dep")
      (is (= test-base (aget payload "issuer")) "issuer has no trailing slash")
      (is (= (str test-base "/api/mcp/oauth/authorize")
             (aget payload "authorization_endpoint")))
      (is (= (str test-base "/api/mcp/oauth/token")
             (aget payload "token_endpoint")))
      (is (= (str test-base "/api/mcp/oauth/register")
             (aget payload "registration_endpoint")))
      (is (= ["S256"] (js->clj (aget payload "code_challenge_methods_supported")))))))

(deftest mcp-protected-resource-metadata-is-served
  (testing "/.well-known/oauth-protected-resource answers 200 and points at /mcp"
    (let [{:keys [status payload]} (serve "GET" "/.well-known/oauth-protected-resource")]
      (is (= 200 status) "status is 200, not a 500 from a nil dep")
      (is (= (str test-base "/mcp") (aget payload "resource"))
          "names the resource the 401 challenge protects")
      (is (= [test-base] (js->clj (aget payload "authorization_servers"))))
      (is (= ["header"] (js->clj (aget payload "bearer_methods_supported")))))))

;; ── the consent page reads a CLJS auth context ───────────
;;
;; resolve-auth-context returns a ClojureScript map (policy-db/resolve-context!
;; builds it with keyword keys). The consent page reached into it with
;; (aget ctx "user" "email"), which compiles to ctx["user"]["email"] — nil on a
;; CLJS map, so the nested read threw before the `or` fallback could run:
;;
;;   500 Cannot read properties of undefined (reading 'email')
;;
;; This is the first authenticated step of the flow, so it could only be hit
;; with a real session — which is exactly why the unauthenticated probes on
;; #212 and #213 sailed past it. Drive the real route with the real context
;; shape instead.

(def ^:private cljs-auth-context
  {:membership {:id "m-1" :actor-id "a-1"}
   :user       {:id "u-1" :email "someone@example.test"}
   :org        {:id "o-1" :slug "acme"}
   :role-slugs ["knowledge_worker"]})

(deftest ^:async consent-page-renders-from-a-cljs-auth-context
  (testing "GET /api/mcp/oauth/authorize renders rather than 500ing"
    (let [app (recording-app)]
      (with-public-base-url test-base
        (fn [] (mcp/register-mcp-http-routes! app nil {:knoxx-base-url test-base})))
      (let [route (registered-route app "GET" "/api/mcp/oauth/authorize")
            reply (fake-reply)
            req   #js {:authContext cljs-auth-context
                       :query #js {"client_id"             "client-1"
                                   "redirect_uri"          "https://chatgpt.com/connector/oauth/abc"
                                   "code_challenge"        "challenge"
                                   "code_challenge_method" "S256"
                                   "state"                 "st"}}]
        (is (some? route) "the authorize route is registered")
        ;; Caught rather than awaited bare: the regression throws, and an
        ;; unhandled rejection here takes the whole runner down mid-suite
        ;; instead of reporting one failed test.
        (let [outcome (try (await ((aget route "handler") req reply)) :ok
                           (catch :default e e))]
          (is (= :ok outcome)
              (str "the authorize handler must not throw: " outcome))
          (when (= :ok outcome)
            (let [html (str (:payload @(aget reply "state")))]
              (is (str/includes? html "Authorize MCP Client")
                  "the consent page rendered")
              (is (str/includes? html "someone@example.test")
                  "the signed-in user's email is read off the CLJS map, not aget-ed off a JS object")
              (is (str/includes? html "acme")
                  "the org slug is read the same way"))))))))

(deftest ^:async client-errors-carry-their-http-status
  (testing "a rejected registration surfaces as 400, not 500"
    ;; Fastify's default error handler reads statusCode off the thrown object
    ;; and falls back to 500. A bare ex-info keeps its status in ex-data, where
    ;; Fastify cannot see it, so every client error in these routes was
    ;; reported as 500 Internal Server Error with the real message attached —
    ;; which is how a rejected redirect_uri came to look like a server crash.
    (let [app (recording-app)]
      (with-public-base-url test-base
        (fn [] (mcp/register-mcp-http-routes! app nil {:knoxx-base-url test-base})))
      (let [route (registered-route app "POST" "/api/mcp/oauth/register")]
        (is (some? route) "the registration route is registered")
        (let [outcome (try
                        (await ((aget route "handler") #js {:body #js {}} (fake-reply)))
                        :resolved
                        (catch :default e e))]
          (is (not= :resolved outcome) "a body with no redirect_uris must be rejected")
          (when (not= :resolved outcome)
            (is (= 400 (aget outcome "statusCode"))
                "the status Fastify actually reads must be the intended one")))))))

(deftest mcp-discovery-tests-leave-the-environment-alone
  (testing "the pinned KNOXX_PUBLIC_BASE_URL does not leak into later tests"
    (let [had?     (.hasOwnProperty js/process.env "KNOXX_PUBLIC_BASE_URL")
          original (aget js/process.env "KNOXX_PUBLIC_BASE_URL")]
      (with-public-base-url "https://leaked.example.test" (fn [] nil))
      (is (= had? (.hasOwnProperty js/process.env "KNOXX_PUBLIC_BASE_URL"))
          "presence of the variable is restored")
      (is (= original (aget js/process.env "KNOXX_PUBLIC_BASE_URL"))
          "value of the variable is restored"))
    (testing "even when the body throws"
      (let [original (aget js/process.env "KNOXX_PUBLIC_BASE_URL")]
        (try
          (with-public-base-url "https://leaked.example.test"
            (fn [] (throw (js/Error. "boom"))))
          (catch :default _ nil))
        (is (= original (aget js/process.env "KNOXX_PUBLIC_BASE_URL")))))))
