(ns knoxx.backend.extern.resource-watcher
  "Own native resource watchers and their debounced callback lifetime."
  (:require ["node:fs" :as fs]
            [clojure.string :as str]))

(def ^:private debounce-ms 350)

(defn- watchable-change? [filename]
  (or (nil? filename) (str/ends-with? (str/lower-case (str filename)) ".edn")))

(defn- clear-timer! [timer]
  (when-let [handle @timer]
    (js/clearTimeout handle)
    (reset! timer nil)))

(defn- schedule! [timer notify! reason]
  (clear-timer! timer)
  (reset! timer (js/setTimeout
                 (fn []
                   (reset! timer nil)
                   (println "[resources] watcher refresh triggered by" reason)
                   (notify!))
                 debounce-ms)))

(defn- watch-root! [root timer notify!]
  (try
    (.watch fs root #js {:recursive true}
            (fn [event-type filename]
              (let [filename-str (some-> filename str)]
                (when (watchable-change? filename-str)
                  (schedule! timer notify! (str root " :: " event-type " :: " (or filename-str "<unknown>")))))))
    (catch :default error
      (println "[resources] failed to watch" root ":" (ex-message error))
      nil)))

(defn- close-watchers! [watchers timer]
  (clear-timer! timer)
  (doseq [watcher watchers]
    (try (.close watcher)
         ;; knoxx-lint/allow-silent-catch — best-effort closing retains the existing watcher cleanup contract.
         (catch :default _ nil))))

(defn start!
  "Watch existing distinct roots; return a count and an opaque cleanup operation."
  [roots notify!]
  (let [timer (atom nil)
        watchers (->> roots
                      (filter #(.existsSync fs %))
                      distinct
                      (map #(watch-root! % timer notify!))
                      (remove nil?)
                      vec)]
    {:count (count watchers)
     :close! #(close-watchers! watchers timer)}))
