(ns knoxx.backend.domain.publication-draft-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.publication-draft :as draft]
            [knoxx.backend.law.publication :as publication]
            [malli.core :as m]))

(def input
  {:source-document-id :knoxx.docs/anchor
   :source-revision "sha256-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
   :source-locale :en
   :org-id "org-1"
   :project "knoxx-session"
   :gardens [{:garden/id :gardens/main :garden/locales [:en :es :fr]}
             {:garden/id :gardens/secondary :garden/locales [:en :de]}]
   :title "A New Post"
   :content "# A New Post\n\nGrounded prose."})

(deftest draft-resources-are-review-bound-and-never-publishable
  (let [result (draft/draft-resources input)]
    (is (m/validate publication/Document (:draft/document result)))
    (is (= true (get-in result [:draft/document :document/anchor?])))
    (is (= false (get-in result [:draft/document :document/generate-drafts?])))
    (is (= "org-1" (get-in result [:draft/document :document/org-id])))
    (is (= :private (get-in result [:draft/document :document/visibility])))
    (is (= (:source-document-id input)
           (get-in result [:draft/document :document/derived-from])))
    (is (= (:source-revision input)
           (get-in result [:draft/document :document/derived-source-revision])))
    (is (= 5 (count (:draft/publications result))))
    (doseq [intent (:draft/publications result)]
      (is (m/validate publication/PublicationIntentResource intent))
      (is (= :draft (:publication/state intent)))
      (is (= draft/publication-target (:publication/target intent)))
      (is (= (if (= (:source-locale input) (:publication/locale intent))
               :none
               :required)
             (:translation/review intent)))
      (is (false? (publication/publishes? intent))))))

(deftest draft-title-is-resilient-to-an-omitted-tool-title
  (testing "an explicitly supplied nonblank title wins"
    (is (= "Explicit title"
           (:draft/title
            (draft/draft-resources
             (assoc input :title "  Explicit title  "
                          :content "# Heading from content\n\nBody."))))))
  (testing "the first nonblank ATX heading supplies an omitted or blank title"
    (doseq [level (range 1 7)]
      (let [heading (str (apply str (repeat level "#"))
                         " Heading level " level " ###")]
        (is (= (str "Heading level " level)
               (:draft/title
                (draft/draft-resources
                 (assoc input :title nil
                              :content (str "Preface.\n\n" heading "\n\nBody."))))))))
    (is (= "Second heading"
           (:draft/title
            (draft/draft-resources
             (assoc input :title "   "
                          :content "# ###\n\n  ## Second heading  ##  \n\nBody."))))))
  (testing "a heading-free post gets a deterministic source-bound fallback"
    (is (= "Draft from knoxx.docs/anchor"
           (:draft/title
            (draft/draft-resources
             (dissoc (assoc input :content "Grounded prose without a heading.")
                     :title)))))))

(deftest source-locale-draft-needs-no-impossible-translation-receipt
  (let [publications (:draft/publications (draft/draft-resources input))
        source-intents (filterv #(= (:source-locale input)
                                    (:publication/locale %))
                                publications)
        translated-intents (remove #(= (:source-locale input)
                                       (:publication/locale %))
                                   publications)]
    (is (= 2 (count source-intents)))
    (is (every? #(= :none (:translation/review %)) source-intents))
    (is (every? #(= :required (:translation/review %)) translated-intents))))

(deftest draft-identity-is-canonical-topology-derived
  (let [first-result (draft/draft-resources input)
        changed-prose (draft/draft-resources
                       (assoc input :title "Another Answer"
                                    :content "Different model bytes."))
        reordered (draft/draft-resources
                   (assoc input
                          :gardens
                          [{:garden/id :gardens/secondary
                            :garden/locales [:de :en :de]}
                           {:garden/id :gardens/main
                            :garden/locales [:fr :es]}
                           {:garden/id :gardens/main
                            :garden/locales [:en :fr]}]))
        changed-source (draft/draft-resources
                        (assoc input :source-revision "sha256-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))]
    (testing "model wording cannot choose a new identity on replay"
      (is (= (:draft/id first-result) (:draft/id changed-prose)))
      (is (= (mapv :publication/id (:draft/publications first-result))
             (mapv :publication/id (:draft/publications changed-prose)))))
    (testing "garden/locale ordering and duplicates are not topology changes"
      (is (= (:draft/id first-result) (:draft/id reordered)))
      (is (= (:draft/policy-fingerprint first-result)
             (:draft/policy-fingerprint reordered)))
      (is (= (:draft/publications first-result)
             (:draft/publications reordered))))
    (testing "a new source revision creates new immutable draft work"
      (is (not= (:draft/id first-result) (:draft/id changed-source))))))

(deftest topology-or-scope-changes-create-new-immutable-draft-identities
  (let [original (draft/draft-id input)]
    (doseq [changed [(assoc input :source-locale :fr)
                     (assoc input :org-id "org-2")
                     (assoc input :project "another-project")
                     (assoc-in input [:gardens 0 :garden/locales]
                               [:en :es :fr :it])
                     (update input :gardens conj
                             {:garden/id :gardens/third
                              :garden/locales [:en :es]})]]
      (is (not= original (draft/draft-id changed))))))

(deftest generated-topology-adds-the-source-locale-as-a-review-free-draft
  (let [target-only (assoc input
                           :gardens [{:garden/id :gardens/main
                                      :garden/locales [:fr :es]}])
        publications (:draft/publications
                      (draft/draft-resources target-only))]
    (is (= [:en :es :fr]
           (mapv :publication/locale publications)))
    (is (= [:none :required :required]
           (mapv :translation/review publications)))
    (is (every? #(= :draft (:publication/state %)) publications))))

(deftest draft-id-can-be-resolved-before-model-generation
  (is (= (:draft/id (draft/draft-resources input))
         (draft/draft-id (dissoc input :title :content)))))

(deftest policy-fingerprint-has-a-portable-canonical-fixture
  (let [policy {:source-document-id :knoxx.verifyadmission/probe
                :source-revision "sha256-demo"
                :source-locale :en
                :org-id "org-1"
                :project "knoxx-session"
                :gardens [{:garden/id :knoxx.verifyadmission/probe-garden
                           :garden/locales [:es :fr]}]}]
    (is (= "be4b4009a1abcacff0b1e38c9c1929aaa82ae97358f97a42d3d204abb21c7c4a"
           (draft/policy-fingerprint policy)))
    (is (= :knoxx.generated/post-be4b4009a1abcacff0b1e38c
           (draft/draft-id policy)))))

(deftest draft-resources-refuse-incomplete-server-policy
  (doseq [invalid [(assoc input :source-document-id :bare)
                   (assoc input :source-revision "")
                   (assoc input :source-locale :locale/not-supported)
                   (assoc input :org-id "")
                   (assoc input :gardens [])
                   (assoc input :content "  ")]]
    (is (thrown? js/Error (draft/draft-resources invalid)))))
