(ns knoxx.backend.extern.clio-store-fixture
  "Disposable Node filesystem fixtures for durable application-provider tests."
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [knoxx.backend.extern.clio-store :as paths]))

(defn temp-directory!
  "Create a private OS temporary directory for one isolated ledger test."
  []
  (fs/mkdtempSync (path/join (os/tmpdir) "knoxx-clio-store-")))

(defn resolved-directory?
  "Exercise the named path conversion with the platform's real path semantics."
  [value]
  (and (path/isAbsolute (paths/resolve-directory value))
       (= (path/resolve value) (paths/resolve-directory value))))

(defn deferred
  "Expose a controllable promise for admission-order proofs."
  []
  (let [settlers (atom nil)
        pending (js/Promise. (fn [resolve reject]
                               (reset! settlers {:resolve! resolve :reject! reject})))]
    (assoc @settlers :promise pending)))

(defn drain!
  "Let pending write and observer continuations run without a wall-clock delay."
  []
  (js/Promise. (fn [resolve _reject] (js/setImmediate resolve))))
