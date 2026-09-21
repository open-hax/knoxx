(ns knoxx.backend.extern.mailbox-backpressure-test
  "Exercise mailbox delivery against native HTTP response backpressure."
  (:require ["node:http" :as http]
            [cljs.test :as test]
            [knoxx.backend.extern.mailbox-changes :as stream]
            [knoxx.backend.infra.actor-mailbox :as mailbox]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.mailbox-changes :as changes]))

(def ^:private context {:permissions ["agent.chat.use"]})
(def ^:private scope {:org-id "org-a" :actor-id "reader" :admin? false})
(def ^:private change {:org-id "org-a" :actor-ids ["reader"]})

(defn- tick! [] (js/Promise. (fn [complete _reject] (js/setTimeout complete 2))))

(defn- ^:async wait-for! [ready?]
  (let [deadline (+ (.now js/Date) 2000)]
    (loop []
      (when-not (ready?)
        (when (> (.now js/Date) deadline)
          (throw (ex-info "Native mailbox condition timed out" {})))
        (await (tick!))
        (recur)))))

(defn- native-server [raw* writes*]
  (http/createServer
   #js {:highWaterMark 1}
   (fn [_request raw]
     (reset! raw* raw)
     (.cork (.-socket raw))
     (let [write (.-write raw)]
       (set! (.-write raw)
             (fn [frame]
               (let [accepted? (.call write raw frame)]
                 (swap! writes* conj {:frame frame :accepted? accepted?})
                 accepted?))))
     (stream/open-stream! {} context #js {:raw raw :hijack (fn [] nil)}))))

(defn- ^:async with-native-stream! [verify!]
  (let [raw* (atom nil) writes* (atom []) listener* (atom nil)
        current* (atom context) unsubscribed* (atom 0)
        server (native-server raw* writes*) client* (atom nil)]
    (try
      (with-redefs [authz/current-context! (fn [_ _]
                                           (or @current* (throw (ex-info "revoked" {:status 401}))))
                    mailbox/context (fn [_ _] {:scope scope})
                    changes/subscribe! (fn [listener]
                                         (reset! listener* listener)
                                         #(swap! unsubscribed* inc))]
        (await (js/Promise. (fn [complete reject]
                              (.once server "error" reject)
                              (.listen server 0 "127.0.0.1" complete))))
        (reset! client* (http/get #js {:host "127.0.0.1" :port (.-port (.address server)) :path "/"}
                                 (fn [response] (.resume response))))
        (.on @client* "error" (fn [_error] nil))
        (await (wait-for! #(seq @writes*)))
        (await (verify! {:raw @raw* :writes* writes* :current* current*
                         :publish! #(@listener* change) :unsubscribed* unsubscribed*})))
      (finally
        (when-let [raw @raw*] (.destroy raw))
        (when-let [client @client*] (.destroy client))
        (.closeAllConnections server)
        (await (js/Promise. (fn [complete _reject] (.close server complete))))))))

(test/deftest ^:async accepted-buffered-frames-pause-until-native-drain
  (await
   (with-native-stream!
     (^:async fn [{:keys [raw writes* publish!]}]
       (test/is (instance? http/ServerResponse raw))
       (test/is (false? (:accepted? (first @writes*))) "the real response buffer is full")
       (test/is (false? (.-writableEnded raw)) "a buffered frame is still accepted")
       (dotimes [_ 90] (publish!))
       (await (tick!))
       (test/is (= 1 (count @writes*)) "pending invalidations do not write through backpressure")
       (when-not (.-writableEnded raw)
         (.uncork (.-socket raw))
         (await (wait-for! #(>= (count @writes*) 2)))
         (test/is (= 2 (count @writes*)) "the bounded pending batch emits one invalidation")
         (publish!)
         (await (wait-for! #(>= (count @writes*) 3)))
         (test/is (false? (.-writableEnded raw)))
         (test/is (every? #(= "event: mailbox-changed\ndata: {}\n\n" (:frame %)) @writes*)))))))

(test/deftest ^:async revoked-authority-is-rechecked-after-backpressure
  (await
   (with-native-stream!
     (^:async fn [{:keys [raw writes* publish! current* unsubscribed*]}]
       (test/is (false? (.-writableEnded raw)) "backpressure retains the authenticated stream")
       (publish!)
       (reset! current* nil)
       (when-let [socket (.-socket raw)] (.uncork socket))
       (await (wait-for! #(.-writableEnded raw)))
       (test/is (= 1 (count @writes*)) "the queued change is not emitted under revoked authority")
       (test/is (= 1 @unsubscribed*))
       (test/is (zero? (.listenerCount raw "drain")) "closing removes the pending drain listener")
       (.emit raw "drain")
       (await (tick!))
       (test/is (= 1 (count @writes*)))))))

(test/deftest ^:async disconnected-clients-release-an-outstanding-drain-wait
  (await
   (with-native-stream!
     (^:async fn [{:keys [raw writes* unsubscribed*]}]
       (test/is (= 1 (.listenerCount raw "drain")))
       (.destroy raw)
       (await (wait-for! #(= 1 @unsubscribed*)))
       (test/is (zero? (.listenerCount raw "drain")))
       (.emit raw "drain")
       (await (tick!))
       (test/is (= 1 (count @writes*)))))))
