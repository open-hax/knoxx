(ns knoxx.backend.realtime
  (:require [clojure.string :as str]
            [knoxx.backend.runtime-config :as runtime-config]))

(defonce ws-clients* (atom {}))
(defonce ws-stats-interval* (atom nil))

(defn ws-envelope
  [channel payload]
  {:channel channel
   :timestamp (runtime-config/now-iso)
   :payload payload})

(defn safe-ws-send!
  [socket payload]
  (when (= (aget socket "readyState") 1)
    (.send socket (.stringify js/JSON (clj->js payload)))))

(def ^:private nvidia-smi-query-args
  #js ["--query-gpu=index,name,utilization.gpu,utilization.memory,memory.used,memory.total,temperature.gpu,power.draw"
       "--format=csv,noheader,nounits"])

(defn parse-float-safe
  [value]
  (let [parsed (js/parseFloat (str (or value "")))]
    (when-not (js/isNaN parsed)
      parsed)))

(defn mib->bytes
  [value]
  (when-let [parsed (parse-float-safe value)]
    (* parsed 1024 1024)))

(defn parse-nvidia-smi-line
  [line]
  (let [[index name util-gpu util-mem mem-used mem-total temp-c power-w]
        (map str/trim (str/split line #","))]
    {:index (or (some-> index js/parseInt) 0)
     :name (or name "NVIDIA GPU")
     :util_gpu (or (parse-float-safe util-gpu) 0)
     :util_mem (or (parse-float-safe util-mem) 0)
     :mem_used_bytes (or (mib->bytes mem-used) 0)
     :mem_total_bytes (or (mib->bytes mem-total) 0)
     :temp_c (parse-float-safe temp-c)
     :power_w (parse-float-safe power-w)}))

(defn collect-nvidia-gpu-stats!
  [runtime]
  (if-let [exec-file-async (aget runtime "execFileAsync")]
    (-> (exec-file-async "nvidia-smi" nvidia-smi-query-args #js {:timeout 1200})
        (.then (fn [result]
                 (->> (str/split-lines (or (aget result "stdout") ""))
                      (map str/trim)
                      (remove str/blank?)
                      (mapv parse-nvidia-smi-line))))
        (.catch (fn [_]
                  (js/Promise.resolve []))))
    (js/Promise.resolve [])))

(defn system-stats!
  [runtime active-runs-count]
  (let [node-os (aget runtime "os")
        cpu-count (max 1 (.availableParallelism node-os))
        load1 (or (aget (.loadavg node-os) 0) 0)
        total-mem (or (.totalmem node-os) 1)
        free-mem (or (.freemem node-os) 0)
        used-mem (max 0 (- total-mem free-mem))
        cpu-percent (min 100 (* 100 (/ load1 cpu-count)))
        mem-percent (min 100 (* 100 (- 1 (/ free-mem total-mem))))]
    (-> (collect-nvidia-gpu-stats! runtime)
        (.then (fn [gpu]
                 {:timestamp (runtime-config/now-iso)
                  :cpu_percent cpu-percent
                  :memory_percent mem-percent
                  :memory_used_bytes used-mem
                  :memory_total_bytes total-mem
                  :active_clients (count @ws-clients*)
                  :active_runs (active-runs-count)
                  :gpu gpu
                  :network {:total_bytes_per_sec 0
                            :rx_bytes_per_sec 0
                            :tx_bytes_per_sec 0}})))))

(defn broadcast-ws!
  [channel payload]
  (doseq [[client-id client] @ws-clients*]
    (try
      (safe-ws-send! (aget client "socket") (ws-envelope channel payload))
      (catch :default _
        (swap! ws-clients* dissoc client-id)))))

(defn broadcast-ws-session!
  "Broadcast to clients scoped by conversation-id for isolation.
   Falls back to session-id matching for backwards compatibility.
   Never broadcasts to all clients - requires explicit conversation or session match."
  [session-id channel payload]
  (let [payload-conversation-id (str (or (:conversation_id payload) (aget payload "conversation_id") ""))]
    (doseq [[client-id client] @ws-clients*]
      (let [client-session-id (or (aget client "sessionId") "")
            client-conversation-id (or (aget client "conversationId") "")
            ;; Match by conversation-id (primary) or session-id (fallback)
            ;; Never match blank-to-blank to prevent cross-session contamination
            matches? (cond
                       (not (str/blank? payload-conversation-id))
                       (and (not (str/blank? client-conversation-id))
                            (= payload-conversation-id client-conversation-id))

                       (not (str/blank? session-id))
                       (and (not (str/blank? client-session-id))
                            (= session-id client-session-id))

                       :else false)]
        (when matches?
          (try
            (safe-ws-send! (aget client "socket") (ws-envelope channel payload))
            (catch :default _
              (swap! ws-clients* dissoc client-id))))))))

(defn ensure-ws-stats-loop!
  [runtime active-runs-count]
  (when-not @ws-stats-interval*
    (reset! ws-stats-interval*
            (js/setInterval
             (fn []
               (when (seq @ws-clients*)
                 (-> (system-stats! runtime active-runs-count)
                     (.then (fn [stats]
                              (broadcast-ws! "stats" stats)))
                     (.catch (fn [_] nil)))))
             5000))))

(defn register-ws-routes!
  [runtime app active-runs-count lounge-messages*]
  (ensure-ws-stats-loop! runtime active-runs-count)
  (.route app
          #js {:method "GET"
               :url "/ws/stream"
               :handler (fn [_request reply]
                          (-> (.code reply 426)
                              (.type "application/json")
                              (.send #js {:error "WebSocket upgrade required"})))
               :wsHandler (fn [socket request]
                            (let [ws (or (aget socket "socket") socket)
                                  client-id (.randomUUID (aget runtime "crypto"))
                                  url-params (try
                                               (js/URL. (str "http://localhost" (or (aget request "url") "/ws/stream")))
                                               (catch :default _ nil))
                                  session-id (try
                                               (or (.get (.-searchParams url-params) "session_id") "")
                                               (catch :default _ ""))
                                  conversation-id (try
                                                    (or (.get (.-searchParams url-params) "conversation_id") "")
                                                    (catch :default _ ""))]
                              (swap! ws-clients* assoc client-id #js {:socket ws :sessionId session-id :conversationId conversation-id})
                              (.on ws "close" (fn [] (swap! ws-clients* dissoc client-id)))
                              (.on ws "error" (fn [] (swap! ws-clients* dissoc client-id)))
                              (.on ws "message" (fn [data]
                                                  (try
                                                    (let [msg (.parse js/JSON (str data))]
                                                      (when (= (aget msg "type") "set_conversation")
                                                        (let [new-cid (str (or (aget msg "conversation_id") ""))]
                                                          (swap! ws-clients* update client-id
                                                                 (fn [c] (when c (js/Object.assign #js {} c #js {:conversationId new-cid})))))))
                                                    (catch :default _ nil))))
                              (-> (system-stats! runtime active-runs-count)
                                  (.then (fn [stats]
                                           (safe-ws-send! ws (ws-envelope "stats" stats))))
                                  (.catch (fn [_] nil)))
                              (doseq [msg (take-last 20 @lounge-messages*)]
                                (safe-ws-send! ws (ws-envelope "lounge" msg)))))}))
