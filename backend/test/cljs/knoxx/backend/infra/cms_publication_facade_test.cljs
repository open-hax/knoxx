(ns knoxx.backend.infra.cms-publication-facade-test
  (:require [cljs.reader :as reader]
            [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.domain.cms-publication :as cms]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.routes.cms-publication :as facade]
            [knoxx.backend.law.cms-publication :as law]
            [open-hax.publication-wire :as wire]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def document
  {:document/id :knoxx.docs/probe
   :document/title "Probe"
   :document/source-locale :en
   :document/source {:path "docs/probe.md"}})

(def intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required
   :document/source-locale :en})

(def garden
  {:garden/id :knoxx.docs/promethean :garden/title "Promethean" :garden/status :active})

(def no-evidence {:receipts {} :blockers {}})

;; ── 1/2 the state-patch wire key convention ───────────────────────────────

(deftest state-patch-accepts-the-body-clj-js-produces
  (testing "round-tripped through clj->js / js->clj rather than hand-written, so
            this fails if api/request's serialization ever changes"
    (let [body (wire/state-patch-body :published)
          on-the-wire (js->clj (clj->js body) :keywordize-keys true)]
      (is (= {:state "published"} on-the-wire))
      (is (true? (m/validate law/PublicationStatePatchJson on-the-wire)))
      (is (= {:publication/state :published}
             (cms/decode-publication-state-patch on-the-wire))))))

(deftest state-patch-rejects-qualified-wire-key
  (testing "closed maps are what make this fail; an open map would accept the
            qualified key alongside the unqualified one and silently no-op"
    (is (false? (m/validate law/PublicationStatePatchJson {:publication/state "published"})))
    (is (thrown? js/Error (cms/decode-publication-state-patch
                           {:publication/state "published"})))
    (is (false? (m/validate law/PublicationStatePatchJson
                            {:state "published" :publication/state "withheld"})))))

(deftest frontend-request-shares-the-backend-contracts-vocabulary
  (testing "the backend contract's key and enum values ARE the ones the frontend
            builds its body from, so the two cannot drift apart"
    (is (= :state wire/state-patch-key))
    (doseq [state (keys wire/state-values)]
      (let [body (wire/state-patch-body state)]
        (is (true? (m/validate law/PublicationStatePatchJson body))
            (str (pr-str body) " must satisfy the backend contract"))
        (is (= {:publication/state state} (cms/decode-publication-state-patch body)))))))

;; ── 4 resource id round trip ──────────────────────────────────────────────

(deftest resource-id-round-trip
  (is (= "docs/probe" (wire/encode-id :docs/probe)))
  (is (= :docs/probe (wire/decode-id "docs/probe")))
  (testing "no EDN leading colon, so a PATCH URL contains no %3A"
    (is (not (str/starts-with? (wire/encode-id :docs/probe) ":")))
    (is (not (str/includes? (js/encodeURIComponent (wire/encode-id :docs/probe)) "%3A")))))

;; ── 5 explicit row encoding ───────────────────────────────────────────────

(deftest document-garden-publication-rows-encode-explicitly
  (let [document-wire (cms/document->wire document)
        garden-wire (cms/garden->wire garden)
        publication-wire (cms/publication->wire {:observed nil :blockers [:translation-missing]}
                                                intent)]
    (testing "every value is a JSON scalar"
      (is (true? (m/validate law/DocumentWireJson document-wire)))
      (is (true? (m/validate law/GardenWireJson garden-wire)))
      (is (true? (m/validate law/PublicationWireJson publication-wire))))
    (testing "ids keep their namespace"
      (is (= "knoxx.docs/probe" (:id document-wire)))
      (is (= "knoxx.docs/promethean" (:garden publication-wire))))
    (testing "enums cross as strings"
      (is (= "en" (:source-locale document-wire)))
      (is (= "active" (:status garden-wire)))
      (is (= "published" (:desired publication-wire)))
      (is (= ["translation-missing"] (:blockers publication-wire))))
    (testing "the revision selector keeps its namespace"
      (is (= "source/current" (:revision publication-wire))))
    (testing "no value carries an EDN leading colon"
      (doseq [value (filter string? (tree-seq coll? seq [document-wire garden-wire
                                                         publication-wire]))]
        (is (not (str/starts-with? value ":")))))))

(deftest document-source-is-mapped-field-by-field
  (testing "a future resource field on :document/source must NOT leak across the
            JSON boundary just because it exists"
    (let [dirty (assoc-in document [:document/source :internal-note] "secret")
          document-wire (cms/document->wire dirty)]
      (is (= {:path "docs/probe.md"} (:source document-wire)))
      (is (not (contains? (:source document-wire) :internal-note))))))

(deftest observed-and-desired-are-separate-fields
  (let [observed {:materialized/revision "abc123" :materialized/path "/probe"}
        with-evidence (cms/publication->wire {:observed observed :blockers []} intent)
        without (cms/publication->wire {:observed nil :blockers []} intent)]
    (is (= "published" (:desired with-evidence)))
    (is (= "abc123" (:observed with-evidence)))
    (testing "desired survives with no runtime evidence at all"
      (is (= "published" (:desired without)))
      (is (nil? (:observed without))))))

;; ── 6 the list view is not double-wrapped ─────────────────────────────────

(deftest list-view-is-not-double-wrapped
  (let [list-view {:documents [{:document document :publications [intent]}]
                   :gardens [garden]}
        encoded (cms/list-view->wire no-evidence list-view)]
    (is (true? (m/validate law/CmsListWireJson encoded)))
    (is (= #{:documents :gardens} (set (keys encoded))))
    (testing "a document view is {document, publications}, not nested again"
      (is (= #{:document :publications} (set (keys (first (:documents encoded))))))
      (is (= "knoxx.docs/probe" (get-in encoded [:documents 0 :document :id])))
      (is (nil? (get-in encoded [:documents 0 :document :document]))))))

;; ── 7 identity cannot move through a state edit ───────────────────────────

(deftest state-patch-cannot-move-identity
  (let [patched (cms/apply-state-patch intent {:publication/state :withheld})]
    (is (= :withheld (:publication/state patched)))
    (testing "every identity dimension is unchanged"
      (doseq [field cms/identity-keys]
        (is (= (get intent field) (get patched field))
            (str field " must be immutable through a state edit")))))
  (testing "a patch carrying identity fields is rejected by the closed contract"
    (doseq [field [:publication/document :publication/garden
                   :publication/locale :publication/revision]]
      (is (thrown? js/Error
                   (cms/apply-state-patch intent {:publication/state :published
                                                  field :knoxx.docs/elsewhere})))))
  (testing "and the patched resource still satisfies the resource contract"
    (doseq [state (keys wire/state-values)]
      (is (some? (cms/apply-state-patch intent {:publication/state state}))))))

;; ── Revision selector versus concrete revision ────────────────────────────

(deftest revision-selector-and-concrete-revision-are-distinguishable
  (testing "a selector round-trips to a keyword"
    (is (= "source/current" (cms/encode-revision :source/current)))
    (is (= :source/current (cms/decode-revision "source/current"))))
  (testing "a concrete revision stays a string — keywordizing would corrupt a sha"
    (is (= "abc123" (cms/encode-revision "abc123")))
    (is (= "abc123" (cms/decode-revision "abc123")))
    (is (string? (cms/decode-revision "abc123")))))

;; ── a state edit must not destroy the file it edits (Codex P1 on #239) ─────
;;
;; The structural half of this — one field on one entry, siblings and :namespace
;; intact, a namespace-local id matched against a canonical one — is pinned at
;; its own layer in knoxx.backend.shape.resource-manifest-test. What is only
;; reachable here is the WRITE path: that the bytes handed to the filesystem are
;; the whole manifest, and that the two refusals carry ex-data the adapter
;; classifies correctly.

(def ^:private authored-manifest
  "What a human actually writes: one namespace, several resources, and a
   publication declared beside the document and garden it relates."
  {:namespace :knoxx.docs
   :resources [{:document/id :probe
                :document/title "Probe"
                :document/source-locale :en
                :document/source {:path "docs/probe.md"}}
               {:garden/id :promethean
                :garden/title "Promethean"
                :garden/status :active}
               {:publication/id :probe-es
                :publication/document :probe
                :publication/garden :promethean
                :publication/locale :es
                :publication/revision :source/current
                :publication/state :published
                :publication/path "/probe"
                :translation/review :required}
               {:publication/id :probe-fr
                :publication/document :probe
                :publication/garden :promethean
                :publication/locale :fr
                :publication/revision :source/current
                :publication/state :published
                :publication/path "/probe-fr"
                :translation/review :required}]})

(defn- writing-to
  "Stub the file IO and hand back the atom the written string lands in."
  [written]
  {:read (fn [_] (js/Promise.resolve @written))
   :write (fn [_ contents] (reset! written contents) (js/Promise.resolve nil))})

(deftest ^:async a-state-edit-persists-the-whole-manifest
  (let [written (atom authored-manifest)
        {:keys [read write]} (writing-to written)]
    (with-redefs [resources/read-edn-file! read
                  resources/write-edn-file! write]
      (await (facade/write-publication-state! "/tmp/probe.edn"
                                              :knoxx.docs/probe-es
                                              :withheld))
      (let [persisted (reader/read-string @written)]
        (testing "what reached the filesystem is the manifest, not the resource.
                  pr-str-ing the patched intent over the file deleted :namespace,
                  :resources and every sibling, and the next projection failed
                  with unresolved references"
          (is (= :knoxx.docs (:namespace persisted)))
          (is (= 4 (count (:resources persisted)))))
        (testing "and exactly one state key differs from what was authored"
          (is (= (assoc-in authored-manifest [:resources 2 :publication/state] :withheld)
                 persisted)))
        (testing "the file ends with a newline, as a text file must"
          (is (str/ends-with? @written "\n")))))))

(deftest ^:async a-file-that-does-not-declare-the-publication-is-refused
  (let [written (atom (update authored-manifest :resources
                               #(vec (remove :publication/id %))))
        before @written
        {:keys [read write]} (writing-to written)]
    (with-redefs [resources/read-edn-file! read
                  resources/write-edn-file! write]
      (let [outcome (try (await (facade/write-publication-state!
                                 "/tmp/probe.edn" :knoxx.docs/probe-es :withheld))
                         :wrote
                         (catch :default e e))]
        (testing "it refuses rather than writing something plausible"
          (is (not= :wrote outcome))
          (is (= before @written) "nothing was persisted"))
        (testing "and carries :publication/id, which the adapter's error-status
                  reads as 404 — the resource genuinely is not there"
          (is (= :knoxx.docs/probe-es (:publication/id (ex-data outcome)))))))))

(deftest ^:async two-entries-claiming-the-id-is-a-conflict-not-a-not-found
  (let [written (atom (update authored-manifest :resources
                               #(conj % (assoc (nth % 2) :publication/path "/dupe"))))
        before @written
        {:keys [read write]} (writing-to written)]
    (with-redefs [resources/read-edn-file! read
                  resources/write-edn-file! write]
      (let [outcome (try (await (facade/write-publication-state!
                                 "/tmp/probe.edn" :knoxx.docs/probe-es :withheld))
                         :wrote
                         (catch :default e e))]
        (testing "a request naming one resource must not rewrite two"
          (is (not= :wrote outcome))
          (is (= before @written) "nothing was persisted"))
        (testing "it reports :conflicts, so error-status maps it to 409"
          (is (seq (:conflicts (ex-data outcome)))))
        (testing "and it deliberately OMITS :publication/id. error-status checks
                  that key FIRST, so including it would report 404 — file not
                  found — for a file that plainly contains the resource twice"
          (is (not (contains? (ex-data outcome) :publication/id))))))))
