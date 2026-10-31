(ns knoxx.frontend.auth.device-test
  "Exercise mounted controls with delayed browser responses and cookie application."
  (:require [cljs.test :as test]
            [knoxx.frontend.auth.device :as device]))

(defn- ^:async flush!
  "Drain promise continuations without advancing device polling timers."
  []
  (dotimes [_ 20] (await (js/Promise.resolve))))

(defn- response
  "Only public device fields cross the synthetic browser response."
  [index]
  #js {:ok true :status 200
       :json (fn [] (js/Promise.resolve
                      #js {:status "cancelled" :user_code (str "code-" index)
                           :verification_uri "https://authority.example/device"
                           :interval 600 :expires_in 600}))})

(defn- complete!
  "Apply a browser-managed cookie when a delayed response arrives, once."
  [fixture index]
  (when-let [{:keys [operation resolve-response completed]} (get @(:requests fixture) index)]
    (when (compare-and-set! completed false true)
      (reset! (:cookie fixture) (when (= "start" operation) (str "attempt-" index)))
      (resolve-response (response index)))))

(defn- mount-page!
  "Mount the real device UI; return its controls and native cleanup."
  []
  (let [root (.createElement js/document "div")
        cleanup (device/mount! root (fn [] nil))
        buttons (.querySelectorAll root "button")]
    {:start (.item buttons 0) :cancel (.item buttons 1) :cleanup cleanup}))

(defn- setup!
  "Keep response delivery under test control instead of mocking UI generations."
  []
  (let [fixture {:requests (atom []) :cookie (atom nil) :automatic (atom false)
                 :original-fetch js/fetch}]
    (set! (.-fetch js/globalThis)
      (fn [path _init]
        (js/Promise.
          (fn [resolve-response reject-response]
            (let [index (count @(:requests fixture))]
              (swap! (:requests fixture) conj
                     {:operation (last (.split path "/")) :resolve-response resolve-response
                      :reject-response reject-response :completed (atom false)})
              (when @(:automatic fixture) (complete! fixture index)))))))
    fixture))

(defn- operations
  "Observe actual fetch dispatch order, independently of queued UI clicks."
  [fixture]
  (mapv :operation @(:requests fixture)))

(defn- ^:async finish!
  "Release every request before restoring fetch, including cleanup queued behind it."
  [fixture pages]
  (reset! (:automatic fixture) true)
  (doseq [page @pages] ((:cleanup page)))
  (doseq [index (range (count @(:requests fixture)))] (complete! fixture index))
  (await (flush!))
  (set! (.-fetch js/globalThis) (:original-fetch fixture)))

(defn- ^:async verify-stale-start!
  "A cancelled delayed start must not enqueue cancellation after a newer start."
  [remount? done]
  (let [fixture (setup!) pages (atom [(mount-page!)]) old (first @pages)]
    (try
      (.click (:start old))
      (await (flush!))
      (if remount? ((:cleanup old)) (.click (:cancel old)))
      (let [current (if remount? (mount-page!) old)]
        (when remount? (swap! pages conj current))
        (.click (:start current))
        (await (flush!))
        (test/is (= ["start"] (operations fixture)))
        (complete! fixture 0)
        (await (flush!))
        (test/is (= ["start" "cancel"] (operations fixture)))
        (complete! fixture 1)
        (await (flush!))
        (complete! fixture 2)
        (await (flush!))
        (test/is (= ["start" "cancel" "start"] (operations fixture)))
        (test/is (= "attempt-2" @(:cookie fixture))))
      (catch :default error (test/is false (str error)))
      (finally (await (finish! fixture pages)) (done)))))

(test/deftest cancelled-start-cannot-cancel-its-successor
  (test/async done (verify-stale-start! false done)))

(test/deftest unmounted-start-cannot-cancel-a-remounted-control
  (test/async done (verify-stale-start! true done)))

(defn- ^:async verify-late-cancel!
  "A new start cannot overtake a cancel response that clears its browser cookie."
  [done]
  (let [fixture (setup!) page (mount-page!) pages (atom [page])]
    (try
      (.click (:start page))
      (await (flush!))
      (complete! fixture 0)
      (await (flush!))
      (.click (:cancel page))
      (.click (:start page))
      (await (flush!))
      (test/is (= ["start" "cancel"] (operations fixture)))
      ;; The broken implementation already dispatched the successor: let its
      ;; response arrive before cancellation to reproduce the cookie loss.
      (complete! fixture 2)
      (complete! fixture 1)
      (await (flush!))
      (complete! fixture 2)
      (await (flush!))
      (test/is (= "attempt-2" @(:cookie fixture)))
      (catch :default error (test/is false (str error)))
      (finally (await (finish! fixture pages)) (done)))))

(test/deftest cancel-response-cannot-clear-a-successor-cookie
  (test/async done (verify-late-cancel! done)))

(defn- ^:async verify-failed-cancel!
  "A failed HTTP request releases its queue slot instead of wedging future starts."
  [done]
  (let [fixture (setup!) page (mount-page!) pages (atom [page])]
    (try
      (.click (:start page))
      (await (flush!))
      (complete! fixture 0)
      (await (flush!))
      (.click (:cancel page))
      (.click (:start page))
      (await (flush!))
      (let [{:keys [reject-response completed]} (get @(:requests fixture) 1)]
        (reset! completed true)
        (reject-response (js/Error. "isolated cancel connection failure")))
      (await (flush!))
      (test/is (= ["start" "cancel" "start"] (operations fixture)))
      (complete! fixture 2)
      (await (flush!))
      (test/is (= "attempt-2" @(:cookie fixture)))
      (catch :default error (test/is false (str error)))
      (finally (await (finish! fixture pages)) (done)))))

(test/deftest failed-cancel-does-not-block-a-successor
  (test/async done (verify-failed-cancel! done)))

(defn- ^:async verify-cancel-during-poll!
  "An unfinished poll cannot hold the queue needed to fence server admission."
  [done]
  (let [fixture (setup!) page (mount-page!) pages (atom [page])
        original-timeout js/setTimeout poll-callback (atom nil)]
    (try
      (set! (.-setTimeout js/globalThis)
        (fn [callback interval-ms]
          (when (= 600000 interval-ms) (reset! poll-callback callback))
          (original-timeout callback interval-ms)))
      (.click (:start page))
      (await (flush!))
      (complete! fixture 0)
      (await (flush!))
      (@poll-callback)
      (await (flush!))
      (.click (:cancel page))
      (await (flush!))
      (test/is (= ["start" "poll" "cancel"] (operations fixture)))
      (catch :default error (test/is false (str error)))
      (finally
        (set! (.-setTimeout js/globalThis) original-timeout)
        (await (finish! fixture pages))
        (done)))))

(test/deftest cancellation-remains-independent-of-inflight-polling
  (test/async done (verify-cancel-during-poll! done)))
