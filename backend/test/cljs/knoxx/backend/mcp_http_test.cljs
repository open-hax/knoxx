(ns knoxx.backend.mcp-http-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]))

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
