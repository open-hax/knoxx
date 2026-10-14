(ns knoxx.frontend.lib.ws-test
  "Written FIRST (TDD) — contract for the CLJS port of src/lib/ws.ts
  connectStream: channel routing, status callbacks, malformed packets,
  disconnect suppressing reconnects, and conversation switching. A mock
  WebSocket class is installed on js/globalThis."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.ws :as ws]))

(def sockets
  "WebSocket instances owned by the current test fixture."
  (atom []))

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
                                  :on-token (fn [token metadata] (swap! tokens conj [token metadata]))
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


(t/deftest ordinary-token-deltas-keep-repeat-whitespace-and-string-only-metadata
  (let [tokens (atom [])
        connection (ws/connect-stream {:on-token (fn [token metadata] (swap! tokens conj [token metadata]))}
                                      "fixture" nil)
        socket (first @sockets)]
    (doseq [token ["ha" "ha" "\n\n"]]
      (message! socket {:channel "tokens" :payload {:token token :run_id "run-1" :kind "assistant_message"}}))
    (t/is (= ["ha" "ha" "\n\n"] (mapv first @tokens)))
    (message! socket {:channel "tokens" :payload {:token false :run_id 42 :kind false}})
    (t/is (= ["false" {:run-id nil :kind nil}] (last @tokens))
        "ordinary JS coercion and string-only optional metadata match the active TS client")
    ((:disconnect connection))))

(t/deftest explicit-replacement-preserves-kind-empty-token-and-utf16-offset
  (doseq [kind ["assistant_message" "reasoning"]]
    (let [tokens (atom [])
          connection (ws/connect-stream {:on-token (fn [token metadata] (swap! tokens conj [token metadata]))}
                                        "fixture" nil)
          socket (last @sockets)
          text "🌱 prior. Corrected" offset (count "🌱 prior. ")]
      (message! socket {:channel "tokens" :payload {:token text :run_id "run-1" :kind kind
                                                    :operation "replace" :offset offset}})
      (t/is (= [[text {:run-id "run-1" :kind kind :operation "replace" :offset offset}]] @tokens))
      (message! socket {:channel "tokens" :payload {:token "" :kind kind :operation "replace" :offset 0}})
      (t/is (= ["" {:run-id nil :kind kind :operation "replace" :offset 0}] (last @tokens)))
      ((:disconnect connection)))))

(t/deftest malformed-replacement-never-becomes-an-ordinary-delta
  ;; Preserve every legacy hooks.test.ts transport case. Missing keys represent
  ;; JS undefined properties omitted by JSON.stringify, not JSON null.
  (doseq [[override omitted] [[{:offset -1} nil] [{:offset 0.5} nil] [{:offset "0"} nil]
                             [{:offset nil} nil] [{:offset 4} nil]
                             [{:offset 9007199254740992} nil] [{} :offset]
                             [{:kind "tool_call"} nil] [{} :kind] [{:token 123} nil]
                             [{:operation "unknown"} nil] [{:operation nil} nil]
                             [{:offset 0} :operation]]]
    (let [tokens (atom []) console (atom [])
          connection (ws/connect-stream {:on-token (fn [token metadata] (swap! tokens conj [token metadata]))
                                         :on-console #(swap! console conj %)} "fixture" nil)
          packet (cond-> (merge {:token "new" :kind "assistant_message" :operation "replace" :offset 0} override)
                   omitted (dissoc omitted))]
      (message! (last @sockets) {:channel "tokens" :payload packet})
      (t/is (= [] @tokens) (str "malformed correction must not append: " packet))
      (t/is (= ["Malformed token packet"] @console))
      ((:disconnect connection)))))

(t/deftest named-js-adapter-preserves-base-callbacks-and-callable-cleanup
  (doseq [[base expected] [["https://example.test:9443/api?old=1" "wss://example.test:9443/ws/stream?session_id=s%2F+1&conversation_id=c%26+2"]
                          ["http://example.test:8080" "ws://example.test:8080/ws/stream?session_id=s%2F+1&conversation_id=c%26+2"]]]
    (let [tokens (atom []) statuses (atom []) stats (atom [])
          connection (ws/connect-stream-js
                      #js {:onToken (fn [token metadata] (swap! tokens conj [token metadata]))
                           :onStatus #(swap! statuses conj %)
                           :onStats #(swap! stats conj %)}
                      "s/ 1" "c& 2" base)
          socket (last @sockets)]
      (t/is (= expected (.-url ^js socket)))
      (t/is (fn? connection))
      (t/is (identical? connection (js/Reflect.get connection "disconnect")))
      (open! socket)
      (t/is (= ["connected"] @statuses))
      (message! socket {:channel "tokens" :payload {:token "🌱 corrected" :kind "reasoning"
                                                    :operation "replace" :offset 2}})
      (let [[token metadata] (last @tokens)]
        (t/is (= "🌱 corrected" token))
        (t/is (identical? js/undefined (aget metadata "runId")))
        (t/is (= "reasoning" (aget metadata "kind")))
        (t/is (= "replace" (aget metadata "operation")))
        (t/is (= 2 (aget metadata "offset"))))
      (message! socket {:channel "stats" :payload {:cpu_percent 12}})
      (t/is (= 12 (aget (last @stats) "cpu_percent")))
      ((js/Reflect.get connection "setConversationId") nil)
      ((js/Reflect.get connection "setConversationId") nil)
      (t/is (= [{"type" "set_conversation" "conversation_id" nil}]
             (mapv #(js->clj (js/JSON.parse %)) (.-sent ^js socket))))
      (connection)
      (t/is (true? (.-closed ^js socket))))))
