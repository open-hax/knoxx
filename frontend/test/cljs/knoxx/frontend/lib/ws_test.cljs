(ns knoxx.frontend.lib.ws-test
  "Written FIRST (TDD) — contract for the CLJS port of src/lib/ws.ts
  connectStream: channel routing, status callbacks, malformed packets,
  disconnect suppressing reconnects, and conversation switching. A mock
  WebSocket class is installed on js/globalThis."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.ws :as ws]))

(def ^:private sockets (atom []))

(defn- make-mock-websocket []
  (let [ctor (fn mock-ws [url]
               (this-as this
                 (let [^js socket this]
                   (set! (.-url socket) url)
                   (set! (.-readyState socket) 0)
                   (set! (.-listeners socket) #js {})
                   (set! (.-sent socket) #js [])
                   (set! (.-closed socket) false)
                   (set! (.-addEventListener socket)
                         (fn [event-name handler]
                           (aset (.-listeners socket) event-name handler)))
                   (set! (.-send socket) (fn [data] (.push (.-sent socket) data)))
                   (set! (.-close socket)
                         (fn []
                           (set! (.-closed socket) true)
                           (set! (.-readyState socket) 3)))
                   (swap! sockets conj socket)
                   socket)))]
    (set! (.-OPEN ctor) 1)
    (set! (.-CLOSED ctor) 3)
    ctor))

(def ^:private real-websocket (.-WebSocket js/globalThis))

(t/use-fixtures :each
  {:before (fn []
             (reset! sockets [])
             (set! (.-WebSocket js/globalThis) (make-mock-websocket)))
   :after (fn []
            (set! (.-WebSocket js/globalThis) real-websocket))})

(defn- fire! [^js socket event-name payload]
  (when-let [handler (aget (.-listeners socket) event-name)]
    (handler payload)))

(defn- open! [^js socket]
  (set! (.-readyState socket) 1)
  (fire! socket "open" nil))

(defn- message! [socket data]
  (fire! socket "message" #js {:data (js/JSON.stringify (clj->js data))}))

(t/deftest connects-and-routes-channels
  (let [statuses (atom [])
        tokens (atom [])
        stats (atom [])
        consoles (atom [])
        _conn (ws/connect-stream {:on-status #(swap! statuses conj %)
                                  :on-token (fn [token token-metadata] (swap! tokens conj [token token-metadata]))
                                  :on-stats #(swap! stats conj (js->clj % :keywordize-keys true))
                                  :on-console #(swap! consoles conj %)}
                                 "sess-1" "conv-1")
        socket (first @sockets)]
    (t/is (some? socket))
    (t/is (re-find #"/ws/stream\?session_id=sess-1&conversation_id=conv-1" (.-url ^js socket)))
    (open! socket)
    (t/is (= [:connected] @statuses))
    (message! socket {:channel "tokens" :payload {:token "hi" :run_id "r1" :kind "text"}})
    (t/is (= [["hi" {:run-id "r1" :kind "text"}]] @tokens))
    (message! socket {:channel "stats" :payload {:cpu_percent 12}})
    (t/is (= [{:cpu_percent 12}] @stats))
    (message! socket {:channel "console" :payload {:stream "out" :line "ready"}})
    (t/is (= ["[out] ready"] @consoles))
    (fire! socket "message" #js {:data "not json"})
    (t/is (= ["[out] ready" "Malformed websocket packet"] @consoles))))

(t/deftest disconnect-closes-and-suppresses-reconnect
  (t/async done
    (let [conn (ws/connect-stream {} nil nil)
          socket (first @sockets)]
      (open! socket)
      ((:disconnect conn))
      (t/is (true? (.-closed ^js socket)))
      (fire! socket "close" #js {:code 1000 :reason "bye"})
      ;; reconnect would fire after ~1s; give it time and assert nothing new
      (js/setTimeout (fn []
                       (t/is (= 1 (count @sockets)) "no reconnect after dispose")
                       (done))
                     1300))))

(t/deftest close-without-dispose-reconnects
  (t/async done
    (let [_conn (ws/connect-stream {} nil nil)
          socket (first @sockets)]
      (open! socket)
      (fire! socket "close" #js {:code 1006 :reason ""})
      (js/setTimeout (fn []
                       (t/is (= 2 (count @sockets)) "reconnected after close")
                       (done))
                     1300))))

(t/deftest set-conversation-id-sends-when-open
  (let [conn (ws/connect-stream {} "sess-1" nil)
        socket (first @sockets)]
    (open! socket)
    ((:set-conversation-id conn) "conv-9")
    (t/is (= [{"type" "set_conversation" "conversation_id" "conv-9"}]
           (mapv #(js->clj (js/JSON.parse %)) (.-sent ^js socket))))
    ;; same id again → no duplicate send
    ((:set-conversation-id conn) "conv-9")
    (t/is (= 1 (.-length (.-sent ^js socket))))))
