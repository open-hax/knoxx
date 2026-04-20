(ns kms-ingestion.drivers.local-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [kms-ingestion.drivers.local :as local]))

;; ─────────────────────────────────────────────────────────────────────────────
;; Temp filesystem fixture
;; ─────────────────────────────────────────────────────────────────────────────

(def ^:dynamic *root* nil)

(defn- write! [rel content]
  (let [f (io/file *root* rel)]
    (.mkdirs (.getParentFile f))
    (spit f content)))

(defn with-temp-root [f]
  (let [tmp (java.io.File/createTempFile "local-driver-test" nil)]
    (.delete tmp)
    (.mkdirs tmp)
    (binding [*root* tmp]
      (try (f) (finally
                 (doseq [^java.io.File c (reverse (file-seq tmp))]
                   (.delete c)))))))

(use-fixtures :each with-temp-root)

;; ─────────────────────────────────────────────────────────────────────────────
;; skip-directory-name?
;; ─────────────────────────────────────────────────────────────────────────────

(deftest skip-directory-name-hardcoded-defaults
  (testing "node_modules and .git are skipped with nil contract"
    (is (true?  (local/skip-directory-name? "node_modules" nil)))
    (is (true?  (local/skip-directory-name? ".git" nil)))
    (is (false? (local/skip-directory-name? ".github" nil)))
    (is (false? (local/skip-directory-name? "src" nil)))))

(deftest skip-directory-name-contract-override
  (testing "contract-supplied skip-dirs wins over defaults"
    (let [contract {:source/discovery {:skip-dirs #{"custom-skip"}}}]
      (is (true?  (local/skip-directory-name? "custom-skip" contract)))
      ;; node_modules still skipped because runtime floor supplies it
      ;; when contract is threaded through the resolver
      (is (false? (local/skip-directory-name? "src" contract))))))

;; ─────────────────────────────────────────────────────────────────────────────
;; text-like-file?
;; ─────────────────────────────────────────────────────────────────────────────

(deftest text-like-file-defaults
  (testing "well-known text extensions pass without a contract"
    (doseq [name ["README.md" "config.yaml" "main.clj" "main.py" "Makefile" "Dockerfile"]]
      (is (true? (local/text-like-file? name nil))
          (str name " should be text-like"))))
  (testing "binary extensions are rejected"
    (doseq [name ["lib.so" "image.png" "bundle.min.js" "types.d.ts"]]
      (is (false? (local/text-like-file? name nil))
          (str name " should NOT be text-like")))))

;; ─────────────────────────────────────────────────────────────────────────────
;; path-matches-glob?
;; ─────────────────────────────────────────────────────────────────────────────

(deftest path-matches-glob
  (testing "*.ext suffix patterns"
    (is (true?  (local/path-matches-glob? "notes.md" "*.md")))
    (is (false? (local/path-matches-glob? "notes.txt" "*.md"))))
  (testing "exact match"
    (is (true?  (local/path-matches-glob? "Makefile" "Makefile")))
    (is (false? (local/path-matches-glob? "makefile" "Makefile"))))
  (testing "**/ prefix"
    (is (true?  (local/path-matches-glob? "foo/bar.md" "**/.md")))))

;; ─────────────────────────────────────────────────────────────────────────────
;; stream-files — integration over a real temp tree
;; ─────────────────────────────────────────────────────────────────────────────

(deftest stream-files-finds-text-files
  (write! "docs/README.md" "# Hello")
  (write! "src/main.clj" "(ns main)")
  (write! "src/image.png" "not-really-png")
  (let [results (local/stream-files (.getAbsolutePath *root*) {})]
    (is (seq results) "should find at least one file")
    (let [paths (set (map :path results))]
      (is (contains? paths "docs/README.md"))
      (is (contains? paths "src/main.clj"))
      (is (not (contains? paths "src/image.png"))
          ".png should be filtered out"))))

(deftest stream-files-skips-node-modules
  (write! "src/index.js" "const x = 1")
  (write! "node_modules/pkg/index.js" "module.exports = {}")
  (let [results (local/stream-files (.getAbsolutePath *root*) {})
        paths (set (map :path results))]
    (is (contains? paths "src/index.js"))
    (is (not (some #(.startsWith % "node_modules") paths))
        "node_modules must be pruned entirely")))

(deftest stream-files-respects-existing-state
  (testing "unchanged files (same size+mtime) are not re-emitted"
    (write! "notes.md" "content")
    (let [f (io/file *root* "notes.md")
          size (.length f)
          mtime (str (-> (java.nio.file.Files/getLastModifiedTime
                           (.toPath f)
                           (make-array java.nio.file.LinkOption 0))
                         .toInstant))
          existing-state {(.getAbsolutePath f)
                          {:metadata {:size size :modified_at mtime}}}]
      (let [results (local/stream-files (.getAbsolutePath *root*)
                                        {:existing-state existing-state})]
        (is (empty? results)
            "file with matching size+mtime must be suppressed")))))

(deftest stream-files-file-types-filter
  (write! "a.md" "markdown")
  (write! "b.clj" "clojure")
  (let [results (local/stream-files (.getAbsolutePath *root*)
                                    {:file-types [".md"]})
        paths (set (map :path results))]
    (is (contains? paths "a.md"))
    (is (not (contains? paths "b.clj"))
        "b.clj must be excluded when file-types=[.md]")))

(deftest stream-files-exclude-patterns
  (write! "docs/keep.md" "keep")
  (write! "archive/old.md" "old")
  (let [results (local/stream-files (.getAbsolutePath *root*)
                                    {:exclude-patterns ["archive/*"]})
        paths (set (map :path results))]
    (is (contains? paths "docs/keep.md"))
    (is (not (contains? paths "archive/old.md")))))

(deftest stream-files-contract-skip-dirs
  (testing "contract-supplied skip-dirs are honoured by the walker"
    (write! "generated/output.md" "gen")
    (write! "src/real.md" "real")
    (let [contract {:source/discovery {:skip-dirs #{"generated"}}}
          results  (local/stream-files (.getAbsolutePath *root*)
                                       {:contract contract})
          paths    (set (map :path results))]
      (is (contains? paths "src/real.md"))
      (is (not (some #(.startsWith % "generated") paths))
          "contract skip-dirs must prune the generated/ subtree"))))
