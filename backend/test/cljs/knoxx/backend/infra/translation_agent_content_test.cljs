(ns knoxx.backend.infra.translation-agent-content-test
  (:require [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.extern.provider-recovery-fixture :as disk]
            [knoxx.backend.infra.translation-agent-content :as content]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]))

(defn- record
  []
  (dispatch-law/dispatch-record
   {:document :open-hax.documents/start-here
    :locale :de
    :revision "sha256-source"
    :replace-stale? false}
   {:dispatch/garden "open-hax.gardens/promethean"
    :dispatch/document-wire-id "open-hax.documents/start-here"
    :dispatch/source-locale :en
    :dispatch/org-id "open-hax"
    :dispatch/membership-id "content-member"
    :dispatch/project "promethean"
    :dispatch/source-digest "sha256-source"}
   :dispatch/accepted
   "2026-08-30T12:00:00.000Z"
   :attempt-id "content-attempt-1"
   :batch-id "content-run-1"))

(t/deftest ^:async immutable-content-is-installed-from-a-complete-temp-file
  (let [root (disk/temporary-directory)
        output-revision (dispatch-law/output-revision (record))
        final-path (content/entry-path root output-revision)
        orphan-path (str final-path ".tmp-interrupted-writer")
        expected "Vollständige Übersetzung.\n"]
    (try
      (fs/mkdir-sync! (content/store-dir root))
    ;; A killed writer may leave an arbitrary sibling temp file. It must not
    ;; claim or corrupt the immutable final path on the next attempt.
    (fs/write-file-sync! orphan-path "partial")

    (let [first-value (await (content/write! root (record) output-revision expected))
          retry-value (await (content/write! root (record) output-revision expected))]
      (t/testing "complete bytes are installed and equal retries reuse them"
        (t/is (= first-value retry-value))
        (t/is (= expected (:translation/content first-value)))
        (t/is (= (pr-str first-value) (fs/read-file-sync final-path))))

      (t/testing "changed bytes cannot replace the installed inode"
        (let [error (try
                      (await (content/write! root (record) output-revision
                                             "Andere Bytes.\n"))
                      nil
                      (catch :default err err))]
          (t/is (some? error))
          (t/is (= (pr-str first-value) (fs/read-file-sync final-path)))))

      (t/testing "successful installs clean their own random temp link"
        (t/is (= #{(fs/join (content/store-dir root)
                           (last (str/split final-path #"/")))
                 orphan-path}
               (into #{}
                     (map #(fs/join (content/store-dir root) %))
                     (fs/readdir-sync (content/store-dir root)))))))
      (finally (disk/remove! root)))))
