(ns knoxx.backend.mcp-http-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.redis-client :as redis]))

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