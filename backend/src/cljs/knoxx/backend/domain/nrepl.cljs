(ns knoxx.backend.tools.nrepl
  "nREPL tool for evaluating code against the live shadow-cljs runtime.

   This tool is intentionally *developer-only* and high risk.

   Implementation notes:
   - We speak the nREPL bencode protocol directly over a TCP socket.
   - For :target \"cljs\", we evaluate a CLJ form that calls
     shadow.cljs.devtools.api/cljs-eval, which forwards into the connected JS
     runtime for the given build-id.
   - If the JS runtime is not connected, shadow returns a map containing an
     :err string (No available JS runtime)."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.domain.text :refer [clip-text tool-text-result]]
            [knoxx.backend.tools.shared :refer [maybe-tool-update! create-tool-obj]]
            ["node:net" :as net]
            ["node:crypto" :as crypto]))

(def ^:private default-host "127.0.0.1")
(def ^:private default-port 4500)

(def nrepl-eval-params
  [:map
   [:target {:optional true :description "Eval target: cljs (default) forwards into shadow.cljs.devtools.api/cljs-eval; clj evaluates on the JVM nREPL host."} :string]
   [:build_id {:optional true :description "shadow-cljs build id when target=cljs (default: server)."} :string]
   [:ns {:optional true :description "CLJS namespace string when target=cljs (default: cljs.user)."} :string]
   [:code {:description "Code to evaluate. For target=cljs, this must be CLJS code as a string."} :string]
   [:timeout_ms {:optional true :description "Timeout in milliseconds (default 15000)."} [:int {:min 1}]]])

(defn- env
  [k]
  (try (aget js/process "env" k) (catch :default _ nil)))

(defn- nrepl-host [] (or (env "KNOXX_NREPL_HOST") default-host))
(defn- nrepl-port [] (js/parseInt (or (env "KNOXX_NREPL_PORT") (str default-port)) 10))

(defn- uuid-str
  []
  (try
    (.randomUUID crypto)
    (catch :default _
      (str (random-uuid)))))

(defn- bencode-bytes [s]
  (.from js/Buffer (str s) "utf8"))

(defn- bencode-encode [v]
  (cond
    (string? v)
    (let [b (bencode-bytes v)]
      (.concat js/Buffer (clj->js [(.from js/Buffer (str (.-length b) ":") "utf8") b])))

    (number? v)
    (.from js/Buffer (str "i" (js/Math.trunc v) "e") "utf8")

    (or (array? v) (vector? v) (seq? v))
    (let [items (map bencode-encode (if (array? v) (array-seq v) v))]
      (.concat js/Buffer (clj->js (into [(.from js/Buffer "l" "utf8")] (concat items [(.from js/Buffer "e" "utf8")])))))

    (or (map? v) (and v (= (goog/typeOf v) "object")))
    (let [m (cond
              (map? v) v
              :else (js->clj v))
          entries (->> m
                       (map (fn [[k vv]] [(str k) vv]))
                       (sort-by first))
          encoded (mapcat (fn [[k vv]] [(bencode-encode k) (bencode-encode vv)]) entries)]
      (.concat js/Buffer
               (clj->js (into [(.from js/Buffer "d" "utf8")] (concat encoded [(.from js/Buffer "e" "utf8")])))))

    (nil? v)
    (bencode-encode "")

    :else
    (bencode-encode (pr-str v))))

(defn- parse-int [buf start end]
  (js/parseInt (.toString buf "utf8" start end) 10))

(declare bencode-decode*)

(defn- bencode-decode-string* [buf idx]
  (let [colon (.indexOf buf (int (.charCodeAt ":" 0)) idx)]
    (when (neg? colon)
      (throw (js/Error. "Invalid bencode string: missing ':'")))
    (let [len (parse-int buf idx colon)
          start (inc colon)
          end (+ start len)
          s (.toString buf "utf8" start end)]
      [s end])))

(defn- bencode-decode-int* [buf idx]
  (let [end (.indexOf buf (int (.charCodeAt "e" 0)) idx)]
    (when (neg? end)
      (throw (js/Error. "Invalid bencode int: missing 'e'")))
    (let [n (parse-int buf (inc idx) end)]
      [n (inc end)])))

(defn- bencode-decode-list* [buf idx]
  (loop [pos (inc idx) acc []]
    (when (>= pos (.-length buf))
      (throw (js/Error. "Invalid bencode list: unexpected EOF")))
    (if (= (.toString buf "utf8" pos (inc pos)) "e")
      [acc (inc pos)]
      (let [[item next-pos] (bencode-decode* buf pos)]
        (recur next-pos (conj acc item))))))

(defn- bencode-decode-dict* [buf idx]
  (loop [pos (inc idx) acc {}]
    (when (>= pos (.-length buf))
      (throw (js/Error. "Invalid bencode dict: unexpected EOF")))
    (if (= (.toString buf "utf8" pos (inc pos)) "e")
      [acc (inc pos)]
      (let [[k next-pos] (bencode-decode* buf pos)
            [v next-pos2] (bencode-decode* buf next-pos)]
        (recur next-pos2 (assoc acc (str k) v))))))

(defn- bencode-decode* [buf idx]
  (when (>= idx (.-length buf))
    (throw (js/Error. "Invalid bencode: unexpected EOF")))
  (let [ch (.toString buf "utf8" idx (inc idx))]
    (cond
      (= ch "i") (bencode-decode-int* buf idx)
      (= ch "l") (bencode-decode-list* buf idx)
      (= ch "d") (bencode-decode-dict* buf idx)
      (re-matches #"[0-9]" ch) (bencode-decode-string* buf idx)
      :else (throw (js/Error. (str "Invalid bencode prefix: " ch))))))

(defn- bencode-decode-all [buf]
  (loop [pos 0 values []]
    (if (>= pos (.-length buf))
      [values (.subarray buf pos)]
      (let [decoded (try
                      (bencode-decode* buf pos)
                      (catch :default _ nil))]
        (if (nil? decoded)
          [values (.subarray buf pos)]
          (let [[v next-pos] decoded]
            (recur next-pos (conj values v))))))))

(defn- connect-socket! []
  (js/Promise.
   (fn [resolve reject]
     (let [socket (.createConnection net #js {:host (nrepl-host)
                                              :port (nrepl-port)})]
       (.once socket "connect" (fn [] (resolve socket)))
       (.once socket "error" reject)))))

(defn- socket-write! [socket msg]
  (.write socket (bencode-encode msg)))

(defn- collect-responses! [socket id timeout-ms]
  (js/Promise.
   (fn [resolve reject]
     (let [timeout-ms (or timeout-ms 15000)
           state (atom {:buf (.alloc js/Buffer 0)
                        :messages []
                        :done? false})
           cleanup* (atom nil)
           cleanup (fn []
                     (when-let [f @cleanup*]
                       (reset! cleanup* nil)
                       (f)))
           on-data (fn [chunk]
                     (let [buf (.concat js/Buffer (clj->js [(:buf @state) chunk]))
                           [values remaining] (bencode-decode-all buf)
                           msgs (->> values
                                     (filter map?)
                                     (filter (fn [m] (= (str (get m "id" "")) (str id))))
                                     vec)
                           statuses (->> msgs
                                         (mapcat (fn [m]
                                                   (let [st (get m "status")]
                                                     (cond
                                                       (nil? st) []
                                                       (vector? st) st
                                                       (seq? st) (vec st)
                                                       (array? st) (vec (array-seq st))
                                                       :else [st]))))
                                         (map str)
                                         set)
                           done? (contains? statuses "done")]
                       (swap! state update :messages into msgs)
                       (swap! state assoc :buf remaining :done? done?)
                       (when done?
                         (cleanup)
                         (resolve (:messages @state)))))
           on-error (fn [err]
                      (cleanup)
                      (reject err))
           on-close (fn []
                      (cleanup)
                      (reject (js/Error. "nREPL socket closed before :done")))
           timeout-handle (js/setTimeout
                           (fn []
                             (cleanup)
                             (reject (js/Error. (str "nREPL timeout after " timeout-ms "ms"))))
                           timeout-ms)]
       (reset! cleanup*
               (fn []
                 (js/clearTimeout timeout-handle)
                 (.removeListener socket "data" on-data)
                 (.removeListener socket "error" on-error)
                 (.removeListener socket "close" on-close)))
       (.on socket "data" on-data)
       (.on socket "error" on-error)
       (.on socket "close" on-close)))))

(defn- nrepl-clone! [socket timeout-ms]
  (let [id (uuid-str)
        msg {"op" "clone" "id" id}]
    (socket-write! socket msg)
    (-> (collect-responses! socket id timeout-ms)
        (.then (fn [responses]
                 (let [session (some (fn [m]
                                       (or (get m "new-session")
                                           (get m "session")))
                                     responses)]
                   (when (str/blank? (str (or session "")))
                     (throw (js/Error. (str "nREPL clone did not return a session: " (pr-str responses)))))
                   session))))))

(defn- nrepl-eval! [socket session code {:keys [timeout-ms]}]
  (let [id (uuid-str)
        msg {"op" "eval"
             "id" id
             "session" session
             "code" (str code)}]
    (socket-write! socket msg)
    (collect-responses! socket id timeout-ms)))

(defn- summarize-eval [responses]
  (let [values (->> responses (keep #(get % "value")) (map str) vec)
        out (->> responses (keep #(get % "out")) (map str) (apply str))
        err (->> responses (keep #(get % "err")) (map str) (apply str))
        ex (->> responses (keep #(get % "ex")) (map str) vec)
        statuses (->> responses
                      (mapcat (fn [m]
                                (let [st (get m "status")]
                                  (cond
                                    (nil? st) []
                                    (vector? st) st
                                    (seq? st) (vec st)
                                    (array? st) (vec (array-seq st))
                                    :else [st]))))
                      (map str)
                      set
                      sort
                      vec)
        value (last values)]
    {:value value
     :values values
     :out out
     :err err
     :ex ex
     :status statuses
     :response_count (count responses)}))

(defn- escape-clj-string [s]
  (pr-str (str s)))

(defn- shadow-cljs-eval-form [{:keys [build-id ns code]}]
  (str "(do "
       "(require (quote [shadow.cljs.devtools.api :as api])) "
       "(api/cljs-eval "
       (pr-str (keyword (or build-id "server")))
       " "
       (escape-clj-string (str code))
       " "
       "{:ns " (escape-clj-string (or ns "cljs.user")) "})"
       ")"))

(defn nrepl-eval-execute [_runtime _config _tool-call-id params a b c]
  (let [on-update (or (when (fn? a) a)
                      (when (fn? b) b)
                      (when (fn? c) c))
        target (some-> (or (aget params "target") "cljs") str str/lower-case)
        build-id (or (aget params "build_id") (aget params "buildId") "server")
        ns (or (aget params "ns") "cljs.user")
        code (or (aget params "code") "")
        timeout-ms (let [t (or (aget params "timeout_ms") (aget params "timeoutMs"))]
                     (when (some? t) (js/Math.trunc t)))]
    (when (str/blank? (str code))
      (throw (js/Error. "code is required")))
    (maybe-tool-update! on-update (str "Connecting to nREPL at " (nrepl-host) ":" (nrepl-port) "…"))
    (-> (connect-socket!)
        (.then
         (fn [socket]
           (-> (nrepl-clone! socket timeout-ms)
               (.then
                (fn [session]
                  (maybe-tool-update! on-update (str "nREPL session ready; evaluating " target "…"))
                  (let [eval-code (if (= target "clj")
                                    code
                                    (shadow-cljs-eval-form {:build-id build-id
                                                           :ns ns
                                                           :code code}))]
                    (-> (nrepl-eval! socket session eval-code {:timeout-ms timeout-ms})
                        (.then
                         (fn [responses]
                           (.end socket)
                           (let [summary (summarize-eval responses)
                                 [clipped-out _] (clip-text (:out summary) 20000)
                                 [clipped-err _] (clip-text (:err summary) 20000)
                                 headline (str "nREPL " target " eval"
                                               (when (= target "cljs") (str " build=" build-id " ns=" ns))
                                               ": " (or (:value summary) "(no value)"))]
                             (tool-text-result
                              (str headline
                                   (when-not (str/blank? clipped-out) (str "\n\n[stdout]\n" clipped-out))
                                   (when-not (str/blank? clipped-err) (str "\n\n[stderr]\n" clipped-err)))
                              (assoc summary
                                     :target target
                                     :build_id build-id
                                     :ns ns
                                     :code code)))))
                        (.catch (fn [err]
                                  (.end socket)
                                  (throw err)))))))
               (.catch (fn [err]
                         (.end socket)
                         (throw err)))))))))

(def nrepl-eval-tool
  (partial create-tool-obj
           "nrepl.eval"
           "nREPL Eval"
           "Evaluate CLJ or CLJS against the live shadow-cljs runtime via nREPL. Developer-only."
           "Evaluate code in the live Knoxx runtime via shadow-cljs nREPL (dangerous)."
           ["Use for live runtime experimentation against the running Knoxx backend."
            "Default target=cljs forwards to shadow.cljs.devtools.api/cljs-eval build=server."
            "Prefer small, reversible changes; avoid long-running loops."
            "After experimentation, persist changes into source files and commit them."]
           nrepl-eval-params
           nrepl-eval-execute))

(defn create-nrepl-custom-tools
  ([runtime config] (create-nrepl-custom-tools runtime config nil))
  ([runtime config auth-context]
   (let [allowed? (fn [tool-id]
                    (or (nil? auth-context)
                        (ctx-tool-allowed? auth-context tool-id)))]
     (clj->js
      (vec
       (remove nil?
               [(when (allowed? "nrepl.eval")
                  (nrepl-eval-tool runtime config))]))))))
