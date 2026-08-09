(ns knoxx.backend.e2e.nrepl-double
  "A real TCP server speaking bencode, standing in for nREPL.

   Deliberately an independent implementation rather than a reuse of
   domain.nrepl's own codec. Sharing the codec would make the test tautological
   for exactly the class of bug it should catch: a framing error would encode
   and decode symmetrically and pass.

   It is a double, not a mock — the tool opens a genuine socket, writes genuine
   bencode, and reads a genuine reply. What is faked is only what is on the
   other end."
  (:require [clojure.string :as str]
            ["node:net" :as net]))

;; ── bencode, minimal and independent ────────────────────────────────────────

(defn encode
  "Bencode a CLJS value. Strings, ints, vectors and string-keyed maps."
  [value]
  (cond
    (string? value) (str (.-length (js/Buffer.from value "utf8")) ":" value)
    (integer? value) (str "i" value "e")
    (sequential? value) (str "l" (str/join (map encode value)) "e")
    (map? value) (str "d"
                      (str/join (mapcat (fn [[k v]] [(encode (str k)) (encode v)])
                                        (sort-by key value)))
                      "e")
    (nil? value) (encode "")
    :else (encode (str value))))

(declare decode-at)

(defn- decode-string-at
  [^js buf idx]
  (let [colon (.indexOf buf ":" idx "utf8")
        len   (js/parseInt (.toString buf "utf8" idx colon) 10)
        start (inc colon)
        end   (+ start len)]
    [(.toString buf "utf8" start end) end]))

(defn- decode-collection-at
  "Decode a list or dict body, returning [items end-index]."
  [^js buf idx]
  (loop [pos (inc idx) acc []]
    (cond
      (>= pos (.-length buf)) nil
      (= "e" (.toString buf "utf8" pos (inc pos))) [acc (inc pos)]
      :else (when-let [[item next-pos] (decode-at buf pos)]
              (recur next-pos (conj acc item))))))

(defn decode-at
  "Decode one bencode value starting at `idx`, or nil when incomplete."
  [^js buf idx]
  (when (< idx (.-length buf))
    (let [ch (.toString buf "utf8" idx (inc idx))]
      (case ch
        "i" (let [end (.indexOf buf "e" idx "utf8")]
              (when-not (neg? end)
                [(js/parseInt (.toString buf "utf8" (inc idx) end) 10) (inc end)]))
        "l" (decode-collection-at buf idx)
        "d" (when-let [[items end] (decode-collection-at buf idx)]
              [(apply hash-map items) end])
        (when (re-matches #"[0-9]" ch)
          (try (decode-string-at buf idx) (catch :default _ nil)))))))

(defn decode-all
  "Every complete value in `buf`, plus the undecoded remainder."
  [^js buf]
  (loop [pos 0 values []]
    (if-let [[value next-pos] (and (< pos (.-length buf)) (decode-at buf pos))]
      (recur next-pos (conj values value))
      [values (.subarray buf pos)])))

;; ── the server ──────────────────────────────────────────────────────────────

(def ^:private session-id "e2e-nrepl-session")

(defn- reply-for
  "The bencode frames answering one nREPL request.

   `eval-value` is what the double claims the evaluation produced. Every reply
   carries the request's own id, because domain.nrepl filters responses by it —
   a double that echoed the wrong id would hang until the tool's timeout."
  [request eval-value]
  (let [id (str (get request "id"))]
    (case (str (get request "op"))
      "clone" [{"id" id "new-session" session-id "status" ["done"]}]
      "eval"  [{"id" id "session" session-id "value" eval-value}
               {"id" id "status" ["done"]}]
      [{"id" id "status" ["done" "unknown-op"]}])))

(defn ^:async start!
  "Listen on an ephemeral port and answer nREPL ops.

   Returns {:port :requests :stop!}, where :requests reads every decoded
   request the tool sent — which is what the assertions are actually about."
  [{:keys [eval-value] :or {eval-value "2"}}]
  (let [requests* (atom [])
        server (.createServer
                net
                (fn [^js socket]
                  (let [buffered* (atom (js/Buffer.alloc 0))]
                    (.on socket "data"
                         (fn [chunk]
                           (let [[values remaining]
                                 (decode-all (js/Buffer.concat #js [@buffered* chunk]))]
                             (reset! buffered* remaining)
                             (doseq [request values :when (map? request)]
                               (swap! requests* conj request)
                               (doseq [frame (reply-for request eval-value)]
                                 (.write socket (encode frame))))))))))]
    (await (js/Promise.
            (fn [resolve _reject]
              (.listen server 0 "127.0.0.1" (fn [] (resolve nil))))))
    (let [port (aget (.address server) "port")]
      {:port port
       :requests (fn [] @requests*)
       :stop! (fn [] (js/Promise. (fn [resolve _] (.close server (fn [] (resolve nil))))))})))

(defn ^:async with-nrepl!
  "Run `f` with the double listening and KNOXX_NREPL_* pointed at it.

   The env vars are restored afterwards so a later test cannot inherit a port
   that has stopped listening."
  [opts f]
  (let [previous-host (aget js/process.env "KNOXX_NREPL_HOST")
        previous-port (aget js/process.env "KNOXX_NREPL_PORT")
        server (await (start! opts))]
    (try
      (aset js/process.env "KNOXX_NREPL_HOST" "127.0.0.1")
      (aset js/process.env "KNOXX_NREPL_PORT" (str (:port server)))
      (await (f server))
      (finally
        (if previous-host
          (aset js/process.env "KNOXX_NREPL_HOST" previous-host)
          (js-delete js/process.env "KNOXX_NREPL_HOST"))
        (if previous-port
          (aset js/process.env "KNOXX_NREPL_PORT" previous-port)
          (js-delete js/process.env "KNOXX_NREPL_PORT"))
        (await ((:stop! server)))))))
