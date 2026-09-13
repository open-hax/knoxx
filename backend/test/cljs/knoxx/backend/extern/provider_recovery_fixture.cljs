(ns knoxx.backend.extern.provider-recovery-fixture
  "Temporary-directory, clock and concurrent-Promise boundary for provider proofs."
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(defn temporary-directory "Allocate private test-only fixture storage." []
  (fs/mkdtempSync (path/join (os/tmpdir) "knoxx-provider-recovery-")))
(defn remove! "Remove only the fixture directory allocated by this suite." [directory]
  (fs/rmSync directory #js {:recursive true :force true}))
(defn ledger-text "Inspect the actual accepted EDN bytes." [directory]
  (fs/readFileSync (path/join directory "events.edn") "utf8"))
(defn corrupt! "Append malformed EDN to prove replay fails closed." [directory]
  (fs/appendFileSync (path/join directory "events.edn") "\n}\n"))
(defn now-ms "Read the admission clock." [] (.now js/Date))
(defn instant "Encode a supplied epoch sample." [at] (.toISOString (js/Date. at)))
(defn drain! "Give asynchronous observer errors their delivery turn." []
  (js/Promise. (fn [resolve _reject] (js/setImmediate resolve))))
(defn ^:async settled
  "Return outcome data from independent concurrent provider operations."
  [tasks]
  (mapv (fn [result]
          (if (= "fulfilled" (.-status result))
            {:status :fulfilled :value (.-value result)}
            {:status :rejected :error (ex-data (.-reason result))}))
        (array-seq (await (js/Promise.allSettled (into-array tasks))))))

(defn mock-thread-db
  "Expose a minimal native Mongo handle for explicit-provider cache isolation checks."
  []
  (let [rows (atom {})
        collection #js {:findOne (fn [query] (clj->js (get @rows (aget query "session_id"))))
                        :findOneAndUpdate (fn [query update _options]
                                            (let [id (aget query "session_id")
                                                  patch (js->clj (aget update "$set") :keywordize-keys true)
                                                  row (merge (get @rows id) patch)]
                                              (swap! rows assoc id row)
                                              (clj->js row)))
                        :deleteOne (fn [query] (swap! rows dissoc (aget query "session_id")) #js {})}]
    #js {:collection (fn [_] collection)}))
