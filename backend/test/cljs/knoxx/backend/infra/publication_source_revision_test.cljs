(ns knoxx.backend.infra.publication-source-revision-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.law.translation-evidence :as evidence-law]
            [malli.core :as m]))

(defn- document
  [id path]
  {:document/id id
   :document/title "Probe"
   :document/source-locale :en
   :document/source {:path path}})

(def ^:private temp-root "/tmp/knoxx-source-revision-test")

(defn- ^:async write-source!
  [name content]
  (let [path (str temp-root "/" name)]
    (await (fs/write-file-ensure-dir! path content))
    path))

(deftest content-revision-is-stable-and-content-addressed
  (testing "the same content is the same revision"
    (is (= (source-revision/content-revision "hello")
           (source-revision/content-revision "hello"))))

  (testing "different content is a different revision"
    (is (not= (source-revision/content-revision "hello")
              (source-revision/content-revision "hello "))))

  (testing "the algorithm is named inside the revision"
    (is (= "sha256-" (subs (source-revision/content-revision "hello") 0 7))))

  (testing "absent content has no revision rather than the empty digest"
    ;; Collapsing these would give a missing file a perfectly stable revision,
    ;; which the gate would then publish translations against.
    (is (nil? (source-revision/content-revision nil)))
    (is (some? (source-revision/content-revision ""))))

  (testing "a content revision is admissible wherever a concrete revision is"
    (is (m/validate evidence-law/ConcreteRevision
                    (source-revision/content-revision "hello")))))

(deftest ^:async source-revisions-read-real-files-once
  (let [path-a (await (write-source! "a.md" "# Alpha"))
        path-b (await (write-source! "b.md" "# Beta"))
        revisions (await (source-revision/source-revisions!
                          [(document :knoxx.docs/a path-a)
                           (document :knoxx.docs/b path-b)]))]
    (testing "each document gets its own content-derived revision"
      (is (= (source-revision/content-revision "# Alpha")
             (get revisions :knoxx.docs/a)))
      (is (= (source-revision/content-revision "# Beta")
             (get revisions :knoxx.docs/b)))
      (is (not= (get revisions :knoxx.docs/a) (get revisions :knoxx.docs/b))))

    (testing "an unreadable source is absent rather than present with nil"
      ;; Present-with-nil and absent read the same through `get`, but absent is
      ;; the honest shape: the gate then reports the revision unresolved instead
      ;; of proceeding on a guess.
      (let [with-missing (await (source-revision/source-revisions!
                                 [(document :knoxx.docs/gone
                                            (str temp-root "/does-not-exist.md"))]))]
        (is (not (contains? with-missing :knoxx.docs/gone)))))))

(deftest revision-facts-answer-the-gates-questions
  (let [revisions {:knoxx.docs/probe "sha256-current00000"}
        {:keys [current-source-revision source-revision-superseded?]}
        (source-revision/revision-facts revisions)]
    (testing "current-source-revision resolves a known document"
      (is (= "sha256-current00000" (current-source-revision :knoxx.docs/probe)))
      (is (nil? (current-source-revision :knoxx.docs/unknown))))

    (testing "a pinned revision is never reported superseded"
      ;; Reporting it superseded would block that publication permanently and
      ;; re-derive replacement translation work on every pass.
      (is (not (source-revision-superseded?
                {:publication/document :knoxx.docs/probe
                 :publication/revision "sha256-pinned00000"}
                "sha256-pinned00000"))))

    (testing "a source/current intent compares against the current revision"
      (is (not (source-revision-superseded?
                {:publication/document :knoxx.docs/probe
                 :publication/revision :source/current}
                "sha256-current00000")))
      (is (source-revision-superseded?
           {:publication/document :knoxx.docs/probe
            :publication/revision :source/current}
           "sha256-stale000000")))

    (testing "an unknown document is not superseded, because nothing is known"
      (is (not (source-revision-superseded?
                {:publication/document :knoxx.docs/unknown
                 :publication/revision :source/current}
                "sha256-anything000"))))))
