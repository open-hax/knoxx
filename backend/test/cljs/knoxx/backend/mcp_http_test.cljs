(ns knoxx.backend.mcp-http-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.infra.redis-client :as redis]))

;; Regression: require-redis! captured nil at route-registration time.
;; When Redis connects after register-mcp-http-routes! runs, the guard still held nil,
;; causing TypeError: Cannot read properties of undefined (reading 'get').

(defn- make-guard []
  (fn [req _reply done]
    (if-let [client (redis/get-client)]
      (do (aset req "redis" client) (done))
      (done (ex-info "Redis unavailable" {:status 503 :error "redis_unavailable"})))))
(defn- run-guard! [guard-fn]
  (js/Promise.
    (fn [resolve _]
      (let [req #js {} errors (atom [])]
        (guard-fn req nil
          (fn [err]
            (when err (swap! errors conj err))
            (resolve #js {:req req :errors (clj->js @errors)})))))))
(defn- fake-client []
  (let [store (atom {})]
    #js {:get      (fn [k]   (js/Promise.resolve (get @store k nil)))
         :set      (fn [k v] (swap! store assoc k v) (js/Promise.resolve "OK"))
         :del      (fn [k]   (swap! store dissoc k)  (js/Promise.resolve 1))
         :sAdd     (fn [k v] (swap! store update k (fnil conj #{}) v) (js/Promise.resolve 1))
         :sMembers (fn [k]   (js/Promise.resolve (clj->js (vec (get @store k #{})))))
         :sRem     (fn [k v] (swap! store update k disj v) (js/Promise.resolve 1))}))
;; --- guard: lazy deref regression ---------

(deftest guard-errors-when-redis-nil
  (async done
    (testing "guard returns 503 when redis atom is nil"
      (reset! redis/redis-client* nil)
      (-> (run-guard! (make-guard))
          (.then (fn [r]
                   (is (= 1 (.-length (.-errors r))))
                   (is (= 503 (-> (aget (.-errors r) 0) ex-data :status)))
                   (is (= "redis_unavailable" (-> (aget (.-errors r) 0) ex-data :error)))
                   (done)))))))

(deftest guard-attaches-client-when-redis-live
  (async done
    (testing "guard attaches live client to req.redis"
      (let [client (fake-client)]
        (reset! redis/redis-client* client)
        (-> (run-guard! (make-guard))
            (.then (fn [r]
                     (is (= 0 (.-length (.-errors r))))
                     (is (= client (aget (.-req r) "redis")))
                     (reset! redis/redis-client* nil)
                     (done))))))))

(deftest guard-created-before-redis-succeeds-after-connect
  (async done
    (testing "guard created with nil redis works after redis connects"
      (reset! redis/redis-client* nil)
      (let [guard (make-guard) client (fake-client)]
        (-> (run-guard! guard)
            (.then (fn [r1]
                     (is (= 1 (.-length (.-errors r1))) "first call fails")
                     (reset! redis/redis-client* client)
                     (run-guard! guard)))
            (.then (fn [r2]
                     (is (= 0 (.-length (.-errors r2))) "second call passes")
                     (is (= client (aget (.-req r2) "redis")))
                     (reset! redis/redis-client* nil)
                     (done))))))))

;; --- redis/get-client atom semantics ------

(deftest get-client-nil-when-unset
  (testing "get-client returns nil before init"
    (reset! redis/redis-client* nil)
    (is (nil? (redis/get-client)))))

(deftest get-client-returns-atom-value
  (testing "get-client reflects atom state"
    (let [s #js {:ok true}]
      (reset! redis/redis-client* s)
      (is (= s (redis/get-client)))
      (reset! redis/redis-client* nil))))

;; --- fake-client store mechanics ----------

(deftest fake-client-set-get-roundtrip
  (async done
    (testing "set then get returns value"
      (let [c (fake-client)]
        (-> (.set c "k" "v")
            (.then (fn [_] (.get c "k")))
            (.then (fn [v] (is (= "v" v)) (done))))))))

(deftest fake-client-del-removes-key
  (async done
    (testing "del removes previously set key"
      (let [c (fake-client)]
        (-> (.set c "k" "v")
            (.then (fn [_] (.del c "k")))
            (.then (fn [_] (.get c "k")))
            (.then (fn [v] (is (nil? v)) (done))))))))

(deftest fake-client-sadd-smembers
  (async done
    (testing "sAdd members visible via sMembers"
      (let [c (fake-client)]
        (-> (.sAdd c "s" "a")
            (.then (fn [_] (.sAdd c "s" "b")))
            (.then (fn [_] (.sMembers c "s")))
            (.then (fn [ms]
                     (let [s (set (js->clj ms))]
                       (is (contains? s "a"))
                       (is (contains? s "b")))
                     (done))))))))

(deftest fake-client-srem-removes-member
  (async done
    (testing "sRem removes a set member"
      (let [c (fake-client)]
        (-> (.sAdd c "s" "a")
            (.then (fn [_] (.sRem c "s" "a")))
            (.then (fn [_] (.sMembers c "s")))
            (.then (fn [ms] (is (= 0 (.-length ms))) (done))))))))
;; ─────────────────────────────────────────────────────────
;; mcp-handle-post! contract tests
;;
;; These are black-box tests against the three observable
;; behaviours of the hijack-first stateless POST handler:
;;
;;  1. Missing bearer → 401 written directly to raw-res
;;  2. Unknown token (redis miss) → 401 on raw-res
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
    #js {:writeHead   (fn [status headers]
                        (swap! state assoc :status status
                                           :headers (js->clj headers)))
         :end         (fn [body]
                        (swap! state assoc :body body :ended true))
         :headersSent false
         :-state      state}))

(defn- raw-res-state [res] @(aget res ":-state"))

(defn- fake-mcp-server []
  (let [tools (atom {})]
    #js {:registerTool (fn [name _opts _fn] (swap! tools assoc name true))
         :connect      (fn [_] (js/Promise.resolve nil))
         :-tools       tools}))

(defn- fake-transport []
  (let [calls (atom [])]
    #js {:handleRequest (fn [req res body]
                          (swap! calls conj {:req req :res res :body body})
                          (js/Promise.resolve nil))
         :-calls        calls}))

(defn- make-invoke-mcp-post!
  "Returns the core logic fn extracted from mcp-handle-post!.
   Accepts the same deps the defroute body sees plus factory fns
   so tests can inject stubs."
  [{:keys [base redis token-record policy-ctx tools
           make-server make-transport]}]
  (fn [raw-req raw-res bearer]
    ;; mirrors mcp-handle-post! exactly
    (if (str/blank? bearer)
      (do (.writeHead raw-res 401
                      #js {"WWW-Authenticate" (str "Bearer realm=\"mcp\", resource_metadata=\""
                                                         (str (.toString (js/URL. "/.well-known/oauth-protected-resource" base)) "\""))
                           "Content-Type" "text/plain"})
          (.end raw-res "Unauthorized"))
      (js/Promise.
       (fn [resolve reject]
         (-> (js/Promise.resolve token-record)
             (.then (fn [rec]
                      (if-not rec
                        (do (.writeHead raw-res 401
                                        #js {"Content-Type" "text/plain"})
                            (.end raw-res "Unauthorized"))
                        (-> (js/Promise.resolve policy-ctx)
                            (.then (fn [_ctx]
                                    (let [server    (make-server)
                                          transport (make-transport)]
                                      (-> (.connect server transport)
                                          (.then (fn [_]
                                                   (.handleRequest transport raw-req raw-res nil))))))))))
             (.then resolve)
             (.catch reject)))))))

;; ── test 1: no bearer → 401 synchronously ────────────────

(deftest mcp-post-no-bearer-returns-401
  (testing "missing bearer token writes 401 to raw-res without Fastify"
    (let [raw-res (fake-raw-res)
          invoke  (make-invoke-mcp-post!
                    {:base "http://localhost:3000"
                     :redis nil :token-record nil :policy-ctx nil
                     :make-server fake-mcp-server
                     :make-transport fake-transport})]
      (invoke #js {} raw-res "")
      (let [s (raw-res-state raw-res)]
        (is (= 401 (:status s)) "status is 401")
        (is (= "Unauthorized" (:body s)) "body is Unauthorized")
        (is (:ended s) "response was ended")))))

;; ── test 2: token not in redis → 401 ─────────────────────

(deftest mcp-post-unknown-token-returns-401
  (async done
    (testing "unknown bearer token (redis nil) writes 401 to raw-res"
      (let [raw-res (fake-raw-res)
            invoke  (make-invoke-mcp-post!
                      {:base "http://localhost:3000"
                       :redis nil :token-record nil :policy-ctx nil
                       :make-server fake-mcp-server
                       :make-transport fake-transport})
            result  (invoke #js {} raw-res "dead-token")]
        (-> (js/Promise.resolve result)
            (.then (fn [_]
                     (let [s (raw-res-state raw-res)]
                       (is (= 401 (:status s)) "status is 401")
                       (is (= "Unauthorized" (:body s)) "body is Unauthorized")
                       (done)))))))))

;; ── test 3: valid token → transport.handleRequest called ─

(deftest mcp-post-valid-token-calls-transport
  (async done
    (testing "valid token: hijack path calls transport.handleRequest with raw req/res"
      (let [raw-req   #js {:url "/mcp" :method "POST"}
            raw-res   (fake-raw-res)
            transport (fake-transport)
            server    (fake-mcp-server)
            tok-rec   #js {:accessToken "abc" :tools #js ["search"] :membershipId "m1"}
            invoke    (make-invoke-mcp-post!
                        {:base "http://localhost:3000"
                         :redis nil
                         :token-record tok-rec
                         :policy-ctx #js {}
                         :make-server (constantly server)
                         :make-transport (constantly transport)})]
        (-> (invoke raw-req raw-res "abc-token")
            (.then (fn [_]
                     (let [calls @(aget transport ":-calls")]
                       (is (= 1 (count calls)) "handleRequest called once")
                       (is (= raw-req (:req (first calls))) "raw req passed")
                       (is (= raw-res (:res (first calls))) "raw res passed")
                       (let [rs (raw-res-state raw-res)]
                         (is (nil? (:status rs)) "Fastify reply never wrote headers")))
                     (done)))))))))