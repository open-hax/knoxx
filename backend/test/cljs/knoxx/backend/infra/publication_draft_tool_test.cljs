(ns knoxx.backend.infra.publication-draft-tool-test
  (:require [cljs.reader :as reader]
            [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.node.fs :as node-fs]
            [knoxx.backend.domain.publication-draft :as draft]
            [knoxx.backend.infra.publication-draft-tool :as tool]
            [knoxx.backend.infra.publication-draft-store :as draft-store]
            ["node:fs/promises" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(def policies
  {:publication-draft? true
   :source-document-id :knoxx.docs/anchor
   :source-revision "sha256-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
   :source-locale :en
   :gardens [{:garden/id :gardens/main :garden/locales [:en :es]}]
   :org-id "org-1"
   :membership-id "member-1"
   :project "workspace"})

(def params
  {:title "Generated Post"
   :content "# Generated Post\n\nA grounded draft."})

(def draft-policy
  (select-keys policies [:source-document-id :source-revision :source-locale
                         :gardens :org-id :project]))

(def draft-input
  (assoc draft-policy
         :title "Generated Post"
         :content "# Generated Post\n\nA grounded draft."))

(defn- tool-names
  [tools]
  (into #{} (map #(aget % "name")) (array-seq tools)))

(deftest publication-draft-tool-exposure-intersects-membership-policy
  (testing "legacy nil context remains available to trusted in-process callers"
    (is (= #{"save_publication_draft"}
           (tool-names (tool/create-publication-draft-tools nil {} nil)))))
  (testing "an explicit membership allow exposes the draft tool"
    (is (= #{"save_publication_draft"}
           (tool-names
            (tool/create-publication-draft-tools
             nil {} {:toolPolicies [{:toolId "save_publication_draft"
                                      :effect "allow"}]})))))
  (testing "an unrelated or denied membership cannot discover the draft tool"
    (doseq [auth-context [{:toolPolicies [{:toolId "semantic_query"
                                           :effect "allow"}]}
                          {:toolPolicies [{:toolId "save_publication_draft"
                                           :effect "deny"}]}]]
      (is (empty? (tool-names
                   (tool/create-publication-draft-tools
                    nil {} auth-context)))))))

(deftest ^:async every-generated-draft-file-uses-the-crash-safe-installer
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os)
                                             "knoxx-draft-atomic-")))
        config {:generated-contracts-dir (.join path temp-root "contracts")}
        installed* (atom [])
        install! node-fs/install-file-exclusive-sync!]
    (try
      (with-redefs [node-fs/install-file-exclusive-sync!
                    (fn [file-path content]
                      (swap! installed* conj file-path)
                      (install! file-path content))]
        (let [saved (await (draft-store/persist! config draft-input))
              completion (await (draft-store/mark-complete! config saved))]
          (testing "content, manifest, and admission marker publish atomically"
            (is (= #{(:content-path saved)
                     (:manifest-path saved)
                     (:draft/completion-path completion)}
                   (set @installed*))))
          (testing "the installed final paths contain complete immutable bytes"
            (is (= (:content draft-input)
                   (await (.readFile fs (:content-path saved) "utf8"))))
            (is (= (str (pr-str (:draft/manifest saved)) "\n")
                   (await (.readFile fs (:manifest-path saved) "utf8"))))
            (is (true? (await (draft-store/draft-complete?
                               config draft-policy)))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async atomic-installer-failure-cannot-leave-a-final-draft-file
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os)
                                             "knoxx-draft-atomic-failure-")))
        config {:generated-contracts-dir (.join path temp-root "contracts")}
        paths (draft-store/draft-paths config
                                       (draft/draft-identity draft-input))]
    (try
      (let [error (with-redefs [node-fs/install-file-exclusive-sync!
                                (fn [_file-path _content]
                                  (throw (ex-info "simulated interrupted install"
                                                  {:code :simulated-enospc})))]
                    (try
                      (await (draft-store/persist! config draft-input))
                      nil
                      (catch :default cause cause)))]
        (testing "the storage error remains visible to the caller"
          (is (= :simulated-enospc (:code (ex-data error)))))
        (testing "no final path is published before the atomic installer succeeds"
          (is (false? (await (draft-store/draft-materialized?
                              config draft-policy))))
          (is (false? (node-fs/exists? (:content-path paths))))
          (is (false? (node-fs/exists? (:manifest-path paths))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async content-only-tool-arguments-derive-and-persist-the-heading-title
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-draft-title-")))
        config {:generated-contracts-dir (.join path temp-root "contracts")}
        draft-tool (aget (tool/create-publication-draft-tools
                          nil config {:resourcePolicies policies
                                      :toolPolicies
                                      [{:toolId "save_publication_draft"
                                        :effect "allow"}]})
                         0)
        required-fields (set (js->clj (aget draft-tool "parameters" "required")))
        content-only {:content "### Content-only title ###\n\nA grounded draft."}
        admit! (fn [_scope _selection]
                 (js/Promise.resolve
                  {:ok true :admitted 1 :failed 0 :results [{:ok true}]}))]
    (try
      (testing "the provider schema accepts the exact content-only argument shape"
        (is (= #{"content"} required-fields)))
      (let [saved (await (tool/save-draft! config policies content-only admit!))
            manifest (reader/read-string
                      (await (.readFile fs (:draft/manifest-path saved) "utf8")))
            document (first (:resources manifest))]
        (testing "the omitted title is derived before immutable resources are saved"
          (is (= "Content-only title" (:document/title document)))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async save-draft-persists-create-only-resources-and-re-admits
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-draft-")))
        config {:generated-contracts-dir (.join path temp-root "contracts")}
        admissions* (atom [])
        admit! (fn [scope selection]
                 (swap! admissions* conj [scope selection])
                 (js/Promise.resolve {:ok true :admitted 1 :failed 0 :results []}))]
    (try
      (is (false? (await (draft-store/draft-materialized?
                          config draft-policy))))
      (is (false? (await (draft-store/draft-complete?
                          config draft-policy))))
      (let [first-result (await (tool/save-draft! config policies params admit!))
            replay (await (tool/save-draft! config policies params admit!))
            manifest (reader/read-string
                      (await (.readFile fs (:draft/manifest-path first-result) "utf8")))]
        (testing "the post is a generated document plus draft locale relations"
          (is (true? (await (draft-store/draft-materialized?
                             config draft-policy))))
          (is (true? (await (draft-store/draft-complete?
                             config draft-policy))))
          (is (:draft/created? first-result))
          (is (false? (:draft/created? replay)))
          (is (true? (:draft/completion-created? first-result)))
          (is (false? (:draft/completion-created? replay)))
          (is (true? (:draft/admission-complete? first-result)))
          (is (= 2 (:draft/publication-count first-result)))
          (is (= 3 (count (:resources manifest))))
          (is (= "# Generated Post\n\nA grounded draft."
                 (await (.readFile fs (:draft/content-path first-result) "utf8")))))
        (testing "each byte-equal replay rechecks admission without recursive drafting"
          (is (= 2 (count @admissions*)))
          (doseq [[scope selection] @admissions*]
            (is (= {:org-id "org-1" :membership-id "member-1" :project "workspace"}
                   scope))
            (is (= {:document (:draft/id first-result)
                    :generate-drafts? false}
                   selection))))
        (testing "different bytes at the same source identity are refused"
          (let [error (try
                        (await (tool/save-draft!
                                config policies
                                {:title "Changed" :content "Different bytes"}
                                admit!))
                        nil
                        (catch :default cause cause))]
            (is (= :generated-draft-conflict (:code (ex-data error)))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async failed-recursive-admission-leaves-materialized-bytes-retriable
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-draft-retry-")))
        config {:generated-contracts-dir (.join path temp-root "contracts")}
        changed-params {:title "A later model answer"
                        :content "# Different\n\nNondeterministic retry bytes."}
        attempts* (atom 0)
        admit! (fn [_scope _selection]
                 (js/Promise.resolve
                  (if (= 1 (swap! attempts* inc))
                    {:ok false :admitted 0 :failed 1}
                    {:ok true :admitted 1 :failed 0 :results [{:ok true}]})))]
    (try
      (let [error (try
                    (await (tool/save-draft! config policies params admit!))
                    nil
                    (catch :default cause cause))
            identity (draft/draft-identity draft-policy)
            paths (draft-store/draft-paths config identity)
            content-before (await (.readFile fs (:content-path paths) "utf8"))
            manifest-before (await (.readFile fs (:manifest-path paths) "utf8"))]
        (testing "materialization without coherent admission is not completion"
          (is (= :generated-draft-admission-failed (:code (ex-data error))))
          (is (true? (await (draft-store/draft-materialized?
                             config draft-policy))))
          (is (false? (await (draft-store/draft-complete?
                              config draft-policy)))))
        (let [retry (await (tool/save-draft! config policies changed-params admit!))]
          (testing "retry re-admits persisted bytes rather than later model bytes"
            (is (= 2 @attempts*))
            (is (false? (:draft/created? retry)))
            (is (true? (:draft/completion-created? retry)))
            (is (= content-before
                   (await (.readFile fs (:content-path paths) "utf8"))))
            (is (= manifest-before
                   (await (.readFile fs (:manifest-path paths) "utf8"))))
            (is (true? (await (draft-store/draft-complete?
                               config draft-policy)))))
          (testing "different bytes conflict again after admission completed"
            (let [error (try
                          (await (tool/save-draft! config policies
                                                   changed-params admit!))
                          nil
                          (catch :default cause cause))]
              (is (= :generated-draft-conflict (:code (ex-data error))))
              (is (= 2 @attempts*))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async content-only-crash-prefix-reconstructs-the-manifest
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os)
                                             "knoxx-draft-content-prefix-")))
        config {:generated-contracts-dir (.join path temp-root "contracts")}
        identity (draft/draft-identity draft-policy)
        paths (draft-store/draft-paths config identity)
        persisted-content "# First durable title\n\nPersisted model bytes."
        later-params {:title "Later nondeterministic title"
                      :content "# Later answer\n\nDifferent model bytes."}
        admissions* (atom [])
        admit! (fn [scope selection]
                 (swap! admissions* conj [scope selection])
                 (js/Promise.resolve
                  {:ok true :admitted 1 :failed 0 :results [{:ok true}]}))]
    (try
      ;; This is the exact prefix left when content's create-only write succeeds
      ;; and the process dies before it can write the resource manifest.
      (await (.mkdir fs (.dirname path (:content-path paths))
                     #js {:recursive true}))
      (await (.writeFile fs (:content-path paths) persisted-content
                         #js {:encoding "utf8" :flag "wx"}))
      (let [saved (await (tool/save-draft! config policies later-params admit!))
            manifest (reader/read-string
                      (await (.readFile fs (:manifest-path paths) "utf8")))
            document (first (:resources manifest))]
        (testing "the first durable content wins over every later model field"
          (is (= persisted-content
                 (await (.readFile fs (:content-path paths) "utf8"))))
          (is (= "First durable title" (:document/title document)))
          (is (not= "Later nondeterministic title" (:document/title document))))
        (testing "the reconstructed manifest is admitted and marked complete"
          (is (= 1 (count @admissions*)))
          (is (false? (:draft/created? saved)))
          (is (true? (:draft/completion-created? saved)))
          (is (true? (await (draft-store/draft-complete?
                             config draft-policy))))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async completion-marker-requires-both-immutable-files
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-draft-complete-")))
        config {:generated-contracts-dir (.join path temp-root "contracts")}
        admit! (fn [_scope _selection]
                 (js/Promise.resolve
                  {:ok true :admitted 1 :failed 0 :results [{:ok true}]}))]
    (try
      (let [saved (await (tool/save-draft! config policies params admit!))
            content-path (:draft/content-path saved)
            manifest-path (:draft/manifest-path saved)
            content (await (.readFile fs content-path "utf8"))
            manifest (await (.readFile fs manifest-path "utf8"))]
        (is (true? (await (draft-store/draft-complete?
                           config draft-policy))))
        (await (.rm fs content-path #js {:force true}))
        (testing "a marker without content is not completion"
          (is (false? (await (draft-store/draft-complete?
                              config draft-policy)))))
        (await (.writeFile fs content-path content "utf8"))
        (is (true? (await (draft-store/draft-complete?
                           config draft-policy))))
        (await (.rm fs manifest-path #js {:force true}))
        (testing "a marker without its manifest is not completion"
          (is (false? (await (draft-store/draft-complete?
                              config draft-policy)))))
        (await (.writeFile fs manifest-path manifest "utf8"))
        (is (true? (await (draft-store/draft-complete?
                           config draft-policy)))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async incoherent-admission-results-never-write-completion
  (let [temp-root (await (.mkdtemp fs (.join path (.tmpdir os) "knoxx-draft-result-")))
        config {:generated-contracts-dir (.join path temp-root "contracts")}]
    (try
      (doseq [result [{:ok true :admitted 1}
                      {:ok true :admitted 1 :failed "0"}
                      {:ok true :admitted 0 :failed 0}
                      {:ok false :admitted 1 :failed 0}]]
        (let [error (try
                      (await
                       (tool/save-draft!
                        config policies params
                        (fn [_scope _selection] (js/Promise.resolve result))))
                      nil
                      (catch :default cause cause))]
          (is (= :generated-draft-admission-failed (:code (ex-data error))))))
      (is (true? (await (draft-store/draft-materialized?
                         config draft-policy))))
      (is (false? (await (draft-store/draft-complete?
                          config draft-policy))))
      (finally
        (await (.rm fs temp-root #js {:recursive true :force true}))))))

(deftest ^:async save-draft-requires-a-pinned-session
  (let [error (try
                (await (tool/save-draft!
                        {:generated-contracts-dir "/tmp/unused"}
                        (dissoc policies :publication-draft?)
                        params
                        (fn [_ _] (js/Promise.resolve nil))))
                nil
                (catch :default cause cause))]
    (is (= :publication-draft-policy-required (:code (ex-data error))))))
