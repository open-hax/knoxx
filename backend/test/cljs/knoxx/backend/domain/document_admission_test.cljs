(ns knoxx.backend.domain.document-admission-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.document-admission :as admission]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.law.publication :as publication-law]))

(def anchor
  {:document/id :knoxx.docs/anchor
   :document/title "Anchor"
   :document/source-locale :en
   :document/visibility :public
   :document/source {:path "docs/anchor.md"}
   :document/anchor? true
   :document/generate-drafts? true})

(def exact
  {:document/id :knoxx.docs/exact
   :document/title "Exact"
   :document/source-locale :en
   :document/visibility :public
   :document/source {:path "docs/exact.md"}})

(def scope
  {:org-id "org-1"})

(def garden
  {:garden/id :knoxx.gardens/main
   :garden/title "Main"
   :garden/status :active
   :garden/locales [:en :es :fr]})

(defn publication
  [id document locale state]
  {:publication/id id
   :publication/document document
   :publication/garden :knoxx.gardens/main
   :publication/locale locale
   :publication/revision :source/current
   :publication/state state
   :publication/path (str "/" (name id))
   :translation/review :required})

(def index
  (resolver/publication-index
   [anchor exact garden
    (publication :knoxx.publications/anchor-es :knoxx.docs/anchor :es :published)
    (publication :knoxx.publications/anchor-fr :knoxx.docs/anchor :fr :draft)
    (publication :knoxx.publications/anchor-withheld :knoxx.docs/anchor :en :withheld)]))

(defn digest
  [value]
  (str "digest-" (hash value)))

(deftest resource-projection-retains-admission-policy
  (testing "anchor and draft flags survive canonical publication indexing"
    (is (true? (get-in index [:documents :knoxx.docs/anchor
                              :document/anchor?])))
    (is (true? (get-in index [:documents :knoxx.docs/anchor
                              :document/generate-drafts?])))))

(deftest selection-is-explicit-and-stable
  (testing "the default sweep selects only anchors"
    (is (= [:knoxx.docs/anchor]
           (mapv :document/id (admission/select-documents index scope {})))))

  (testing "an exact non-anchor remains admissible"
    (is (= [:knoxx.docs/exact]
           (mapv :document/id
                 (admission/select-documents
                  index scope {:document :knoxx.docs/exact})))))

  (testing "an absent exact identity is a visible 404"
    (let [err (try
                (admission/select-documents
                 index scope {:document :knoxx.docs/missing})
                nil
                (catch :default error error))]
      (is (= 404 (:status (ex-data err))))
      (is (= "document_admission_document_not_found"
             (:code (ex-data err)))))))

(deftest selection-requires-explicit-public-or-matching-organization
  (let [owned (assoc anchor
                     :document/id :knoxx.generated/owned
                     :document/org-id "org-1"
                     :document/visibility :private)
        other (assoc owned
                     :document/id :knoxx.generated/other
                     :document/org-id "org-2")
        legacy (-> owned
                   (assoc :document/id :knoxx.generated/legacy)
                   (dissoc :document/org-id :document/visibility))
        private-unowned (-> owned
                            (assoc :document/id :knoxx.generated/unowned)
                            (dissoc :document/org-id))
        scoped-index {:documents (into {}
                                       (map (juxt :document/id identity))
                                       [anchor owned other legacy private-unowned])}]
    (testing "the anchor sweep includes only explicit public and exact-owner rows"
      (is (= [:knoxx.docs/anchor :knoxx.generated/owned]
             (mapv :document/id
                   (admission/select-documents scoped-index scope {})))))

    (testing "other-tenant and legacy unowned documents are indistinguishable from absence"
      (doseq [document-id [:knoxx.generated/other
                           :knoxx.generated/legacy
                           :knoxx.generated/unowned]]
        (let [err (try
                    (admission/select-documents
                     scoped-index scope {:document document-id})
                    nil
                    (catch :default error error))]
          (is (= 404 (:status (ex-data err))))
          (is (= "document_admission_document_not_found"
                 (:code (ex-data err)))))))))

(deftest visible-index-removes-hidden-publication-relations-with-their-documents
  (let [owned (assoc exact
                     :document/id :knoxx.generated/owned
                     :document/org-id "org-1"
                     :document/visibility :private)
        other (assoc owned
                     :document/id :knoxx.generated/other
                     :document/org-id "org-2")
        relation (fn [id document]
                   {:publication/id id
                    :publication/document document})
        scoped (admission/visible-publication-index
                {:documents {(:document/id anchor) anchor
                             (:document/id owned) owned
                             (:document/id other) other}
                 :gardens {:shared {:garden/id :shared}}
                 :publications [(relation :public-relation (:document/id anchor))
                                (relation :owned-relation (:document/id owned))
                                (relation :hidden-relation (:document/id other))]}
                scope)]
    (is (= #{:knoxx.docs/anchor :knoxx.generated/owned}
           (set (keys (:documents scoped)))))
    (is (= [:public-relation :owned-relation]
           (mapv :publication/id (:publications scoped))))
    (is (= {:shared {:garden/id :shared}} (:gardens scoped)))))

(deftest derived-documents-never-request-another-draft
  (let [derived (assoc exact
                       :document/derived-from :knoxx.docs/anchor
                       :document/derived-source-revision "sha256-source"
                       :document/generate-drafts? true)]
    (testing "neither document policy nor a deployment override can recurse"
      (is (false? (admission/generate-drafts? {} derived)))
      (is (false? (admission/generate-drafts?
                   {:generate-drafts? true} derived))))))

(deftest gardens-come-only-from-structurally-translatable-relations
  (is (= [{:garden/id :knoxx.gardens/main
           :garden/locales [:es :fr]}]
         (admission/document-gardens
          index :knoxx.docs/anchor
          #(publication-law/translatable-publication? index %)))))

(deftest event-identities-address-the-trusted-snapshot
  (let [scope {:org-id "org-1"
               :membership-id "member-1"
               :project "knoxx-local"}
        provenance {:source-path "/workspace/docs/anchor.md"
                    :resource-path "/workspace/contracts/publication.edn"}
        gardens [{:garden/id :knoxx.gardens/main
                  :garden/locales [:es :fr]}]
        args [digest "2026-09-02T12:00:00.000Z" scope anchor provenance
              "# Anchor" "sha256-revision" gardens true]
        first-events (apply admission/admission-events args)
        retry-events (apply admission/admission-events
                            (assoc args 1 "2026-09-02T12:00:01.000Z"))
        changed-events (admission/admission-events
                        digest "2026-09-02T12:00:01.000Z" scope anchor provenance
                        "# Changed" "sha256-changed" gardens true)
        payload (get-in first-events [:runtime-event :event/payload])]
    (testing "timestamps do not perturb unchanged retry identities"
      (is (= (get-in first-events [:snapshot-event :id])
             (get-in retry-events [:snapshot-event :id])))
      (is (= (get-in first-events [:indexed-event :id])
             (get-in retry-events [:indexed-event :id]))))

    (testing "a changed revision mints new immutable event identities"
      (is (not= (get-in first-events [:snapshot-event :id])
                (get-in changed-events [:snapshot-event :id])))
      (is (not= (get-in first-events [:indexed-event :id])
                (get-in changed-events [:indexed-event :id]))))

    (testing "the trigger sees pinned content, scope, gardens and policy"
      (is (= :publication/document-indexed
             (get-in first-events [:runtime-event :event/type])))
      (is (= "# Anchor" (:content payload)))
      (is (= "# Anchor" (:document/source-content payload)))
      (is (= "member-1" (get-in payload [:resource-policies
                                          :membership-id])))
      (is (= gardens (get-in payload [:resource-policies :gardens])))
      (is (true? (get-in payload [:resource-policies
                                  :publication-draft?]))))

    (testing "the durable signal names its searchable source snapshot"
      (is (= "docs" (get-in first-events [:snapshot-event :kind])))
      (is (= "publication.document.indexed"
             (get-in first-events [:indexed-event :kind])))
      (is (= (get-in first-events [:snapshot-event :id])
             (get-in first-events [:indexed-event :extra
                                   :source_event_id]))))))
