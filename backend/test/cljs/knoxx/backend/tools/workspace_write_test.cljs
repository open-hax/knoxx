(ns knoxx.backend.tools.workspace-write-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.infra.routes.tools :as routes]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(deftest ^:async exclusive-write-preserves-existing-content
  (let [dir (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-write-test-")))
        file (.join path dir "fixture.txt")]
    (try
      (await (routes/write-workspace-file! file "original" false))
      (let [rejected? (try
                        (await (routes/write-workspace-file! file "replacement" false))
                        false
                        (catch :default err (= "EEXIST" (.-code err))))]
        (is rejected?)
        (is (= "original" (await (.readFile fs file "utf8")))))
      (await (routes/write-workspace-file! file "replacement" true))
      (is (= "replacement" (await (.readFile fs file "utf8"))))
      (finally (await (.rm fs dir #js {:recursive true :force true}))))))
