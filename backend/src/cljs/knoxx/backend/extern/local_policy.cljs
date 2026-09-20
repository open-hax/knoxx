(ns knoxx.backend.extern.local-policy
  "Node clock, identity and kernel-lock boundary for local policy commands."
  (:require ["fs-ext-extra-prebuilt" :as flock]
            ["node:crypto" :as crypto]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(defonce ^:private active-locks (atom #{}))

(defn now-ms "Read the host's current wall clock in milliseconds." [] (.now js/Date))
(defn now-iso "Read a canonical UTC timestamp." [] (.toISOString (js/Date.)))
(defn operation-id "Allocate a fresh command identity outside pure transitions." [] (crypto/randomUUID))

(defn- wait-turn [] (js/Promise. (fn [resolve _reject] (js/setTimeout resolve 10))))

(defn- ^:async acquire! [fd]
  (loop []
    (let [locked? (try (flock/flockSync fd "exnb") true
                       (catch :default error
                         (if (#{"EAGAIN" "EWOULDBLOCK"} (.-code error)) false
                             (throw error))))]
      (when-not locked? (await (wait-turn)) (recur)))))

(defn- ^:async locked! [file f]
  (fs/mkdirSync (path/dirname file) #js {:recursive true :mode 448})
  (let [fd (fs/openSync file "a+" 384)]
    (try
      (await (acquire! fd))
      (try (await (f))
           (finally (flock/flockSync fd "un")))
      (finally (fs/closeSync fd)))))

(defn- ^:async acquire-local! [file]
  (loop []
    (if (contains? @active-locks file)
      (do (await (wait-turn)) (recur))
      (swap! active-locks conj file))))

(defn ^:async with-lock!
  "Serialize this process and hold a separate kernel lock across awaited admission.
   Never pass the Clio event ledger inode as the lock file."
  [file f]
  (let [file (path/resolve file)]
    (await (acquire-local! file))
    (try (await (locked! file f))
         (finally (swap! active-locks disj file)))))
