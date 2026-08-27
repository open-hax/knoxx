(ns knoxx.backend.shape.resource-identity-test
  (:require [cljs.test :refer [deftest is testing]]
            [malli.core :as m]
            [knoxx.backend.law.publication :as law]
            [knoxx.backend.shape.resource-identity :as identity]))

;; ── canonical-id ───────────────────────────────────────────────────────────

(deftest canonical-id-is-one-rule-for-ids-and-references
  (testing "a bare id qualifies under the declared namespace"
    (is (= :knoxx.docs/probe (identity/canonical-id :knoxx.docs :probe))))
  (testing "an already-qualified id keeps its own namespace"
    (is (= :gardens/promethean (identity/canonical-id :knoxx.docs :gardens/promethean))))
  (testing "it is idempotent"
    (is (= (identity/canonical-id :knoxx.docs :probe)
           (identity/canonical-id :knoxx.docs (identity/canonical-id :knoxx.docs :probe)))))
  (testing "with no namespace a bare id is left alone"
    (is (= :probe (identity/canonical-id nil :probe))))
  (testing "and a standalone file's qualified id is NOT stripped"
    ;; This is the case the nil-namespace guard exists for. katamorph's
    ;; qualified-id takes `(name local-id)`, so qualifying :knoxx.docs/probe
    ;; under a nil namespace yields (keyword nil "probe") — which in CLJS is
    ;; plain :probe, silently discarding the namespace. Short-circuiting on
    ;; qualified-keyword? first is what prevents that.
    (is (= :knoxx.docs/probe (identity/canonical-id nil :knoxx.docs/probe)))
    (is (qualified-keyword? (identity/canonical-id nil :knoxx.docs/probe))))
  (testing "nil id stays nil"
    (is (nil? (identity/canonical-id :knoxx.docs nil)))))

;; ── canonicalize-identity: the loader-drop regression ─────────────────────

(deftest manifest-entry-canonicalizes-before-validation
  (let [manifest-document {:namespace :knoxx.docs
                           :document/id :translation-pipeline
                           :document/title "Translation Pipeline"
                           :document/source-locale :en
                           :document/source {:path "docs/x.md"}}]
    (testing "a manifest-local id does NOT satisfy the law shape as authored"
      (is (false? (m/validate law/Document manifest-document))
          "this is why the loader dropped these definitions before the fix"))
    (testing "canonicalizing first makes it valid"
      (let [canonical (identity/canonicalize-identity :document manifest-document)]
        (is (= :knoxx.docs/translation-pipeline (:document/id canonical)))
        (is (true? (m/validate law/Document canonical)))))))

(deftest canonicalize-identity-covers-publication-references
  (let [intent {:namespace :knoxx.docs
                :publication/id :translation-pipeline-es
                :publication/document :translation-pipeline
                :publication/garden :gardens/promethean
                :publication/locale :es
                :publication/revision :source/current
                :publication/state :published
                :publication/path "/x"
                :translation/review :none}
        canonical (identity/canonicalize-identity :publication intent)]
    (is (= :knoxx.docs/translation-pipeline-es (:publication/id canonical)))
    (is (= :knoxx.docs/translation-pipeline (:publication/document canonical)))
    (testing "a reference that was already qualified is not re-namespaced"
      (is (= :gardens/promethean (:publication/garden canonical))))
    (is (true? (m/validate law/PublicationIntentResource canonical)))))

(deftest canonicalize-identity-leaves-other-kinds-alone
  (let [agent {:namespace :knoxx.docs :agent/id :helper}]
    (is (= agent (identity/canonicalize-identity :agent agent)))
    (is (false? (identity/qualified-identity-kind? :agent)))
    (doseq [kind [:document :garden :publication]]
      (is (true? (identity/qualified-identity-kind? kind))))))

(deftest canonicalize-identity-skips-absent-keys
  (testing "an entry missing an identity key is returned unchanged, not nil-keyed"
    (let [partial {:namespace :knoxx.docs :publication/id :x}]
      (is (= {:namespace :knoxx.docs :publication/id :knoxx.docs/x}
             (identity/canonicalize-identity :publication partial))))))

;; ── Wire encoding: the clj->js identity-loss regression ───────────────────

(deftest encode-keyword-preserves-namespace-without-a-colon
  (is (= "docs/probe" (identity/encode-keyword :docs/probe)))
  (is (= "published" (identity/encode-keyword :published)))
  (is (= "source/current" (identity/encode-keyword :source/current)))
  (testing "never the EDN rendering"
    (is (not= ":docs/probe" (identity/encode-keyword :docs/probe)))
    (is (not (re-find #"^:" (identity/encode-keyword :docs/probe))))))

(deftest keyword-round-trips-through-the-wire
  (doseq [value [:docs/probe :published :source/current :es :knoxx.docs/a-b-c]]
    (testing (str value)
      (is (= value (identity/decode-keyword (identity/encode-keyword value)))))))

(deftest distinct-namespaces-do-not-collide-on-the-wire
  (let [a :tenant-a/foo
        b :tenant-b/foo]
    (testing "clj->js would collapse both onto \"foo\""
      (is (= "foo" (name a) (name b))))
    (testing "the encoder keeps them distinct"
      (is (not= (identity/encode-keyword a) (identity/encode-keyword b)))
      (is (= a (identity/decode-keyword (identity/encode-keyword a))))
      (is (= b (identity/decode-keyword (identity/encode-keyword b)))))))

(deftest encode-wire-values-encodes-values-but-not-keys
  (let [projection {:documents [{:document {:document/id :knoxx.docs/probe
                                            :document/source-locale :en}
                                 :publications [{:publication/state :published
                                                 :publication/revision :source/current}]}]
                    :gardens [{:garden/id :knoxx.docs/promethean
                               :garden/status :active}]}
        encoded (identity/encode-wire-values projection)]
    (testing "keyword values are encoded namespace-preservingly"
      (is (= "knoxx.docs/probe" (get-in encoded [:documents 0 :document :document/id])))
      (is (= "en" (get-in encoded [:documents 0 :document :document/source-locale])))
      (is (= "published" (get-in encoded [:documents 0 :publications 0 :publication/state])))
      (is (= "source/current"
             (get-in encoded [:documents 0 :publications 0 :publication/revision])))
      (is (= "knoxx.docs/promethean" (get-in encoded [:gardens 0 :garden/id]))))
    (testing "map keys are left for clj->js, per the unqualified-JSON-key convention"
      (is (contains? encoded :documents))
      (is (contains? (get-in encoded [:gardens 0]) :garden/id)))
    (testing "non-keyword scalars are untouched"
      (is (= {:a "s" :b 1 :c true :d nil}
             (identity/encode-wire-values {:a "s" :b 1 :c true :d nil}))))))
