(ns knoxx.backend.extern.publication-draft-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.publication-draft-store :as draft-store]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(deftest ^:async generated-draft-filesystem-boundary-returns-cljs-values
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os)
                                             "knoxx-draft-extern-")))
        contracts-root (.join path temp-root "contracts")
        paths (draft-store/draft-paths contracts-root "draft-id"
                                       "generated/draft-id.md")
        content-path (:content-path paths)]
    (try
      (testing "path derivation returns a plain CLJS map of strings"
        (is (= {:content-path (.join path temp-root "generated/draft-id.md")
                :manifest-path (.join path contracts-root "namespaces"
                                      "draft-id.edn")
                :completion-path (.join path temp-root ".knoxx"
                                       "draft-admission-completions"
                                       "draft-id.edn")}
               paths)))
      (testing "absence, immutable installation, and UTF-8 reads stay scalar"
        (is (false? (await (draft-store/file-exists? content-path))))
        (is (nil? (await (draft-store/read-text-or-nil! content-path))))
        (is (true? (await (draft-store/install-text-exclusive!
                           content-path "complete bytes"))))
        (is (true? (await (draft-store/file-exists? content-path))))
        (is (= "complete bytes" (await (draft-store/read-text! content-path))))
        (is (false? (await (draft-store/install-text-exclusive!
                            content-path "replacement bytes")))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))
