(ns knoxx.backend.extern.resource-watcher-test
  "Native resource notifications retain filtering, coalescing, and cleanup."
  (:require ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.test :as test]
            [knoxx.backend.extern.resource-watcher :as watcher]))

(defn- pause [milliseconds]
  (js/Promise. (fn [complete!] (js/setTimeout complete! milliseconds))))

(defn- ^:async await-refresh [notifications]
  (let [deadline (+ (js/Date.now) 5000)]
    (loop []
      (when (zero? @notifications)
        (when (> (js/Date.now) deadline)
          (throw (js/Error. "The resource watcher did not deliver its EDN refresh")))
        (await (pause 20))
        (recur)))))

(test/deftest ^:async native-watcher-filters-debounces-and-cancels-pending-refresh
  (let [directory (await (.mkdtemp fs (path/join (os/tmpdir) "knoxx-resource-watch-")))
        notifications (atom 0)
        watching (watcher/start! [directory directory] #(swap! notifications inc))]
    (try
      (test/is (= 1 (:count watching)))
      (await (.writeFile fs (path/join directory "ignored.txt") "prose"))
      (await (pause 450))
      (test/is (= 0 @notifications) "Prose changes do not reload resource EDN")
      (await (.writeFile fs (path/join directory "agent.edn") "{:revision 1}"))
      (await (.writeFile fs (path/join directory "agent.edn") "{:revision 2}"))
      (await (await-refresh notifications))
      (test/is (= 1 @notifications) "A short write burst schedules one refresh")
      (await (.writeFile fs (path/join directory "agent.edn") "{:revision 3}"))
      (await (pause 100))
      ((:close! watching))
      (await (pause 450))
      (test/is (= 1 @notifications) "Cleanup cancels the pending refresh")
      (finally
        ((:close! watching))
        (await (.rm fs directory #js {:recursive true :force true}))))))
