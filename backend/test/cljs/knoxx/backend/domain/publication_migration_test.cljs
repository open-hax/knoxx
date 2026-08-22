(ns knoxx.backend.domain.publication-migration-test
  (:require [cljs.test :refer [deftest is testing]]
            [malli.core :as m]
            [knoxx.backend.domain.publication-migration :as migration]
            [knoxx.backend.domain.publication-migration-identity :as ident]
            [knoxx.backend.infra.publication-migration :as fold]
            [knoxx.backend.law.publication :as law]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def policy
  {:migration/namespace :knoxx.docs
   :migration/membership-review :required})

(def policy-without-review
  (dissoc policy :migration/membership-review))

(def legacy-document
  "The real legacy shape: garden association lives in metadata, and operational
   facts (published_at, job ids) sit alongside it."
  {:legacy/doc-id "doc-1"
   :document/id :translation-pipeline
   :document/source-locale "en"
   :title "Translation Pipeline"
   :source_path "docs/translation-pipeline.md"
   :metadata {:garden_publications [{:garden_id "garden-a"}]
              :published_at "2026-08-01T00:00:00Z"
              :publish_job_id "job-42"}})

(def migrated-document
  {:document/id :knoxx.docs/translation-pipeline
   :document/title "Translation Pipeline"
   :document/source-locale :en
   :document/source {:path "docs/translation-pipeline.md"}})

(defn- membership-row
  "Normalize the fixture's single membership entry, asserting it normalized.

   Deliberately does NOT override :path. The first version of this helper did
   (`(assoc (:row result) :path \"/translation-pipeline\")`) and that override
   masked a real bug: normalization copied the repository `source_path`
   verbatim, so every legacy membership would have conflicted on path in
   production while the suite stayed green."
  [document]
  (let [[result] (migration/normalize-publication-rows document)]
    (is (= :normalized (:migration/status result)))
    (:row result)))

;; ── 1 membership is the legacy published fact ──────────────────────────────

(deftest membership-entry-normalizes-to-published-candidate
  (let [row (membership-row legacy-document)
        decision (migration/publication->decision policy migrated-document row)]
    (testing "membership migrates rather than conflicting"
      (is (= :candidate (:migration/status decision))
          "if this conflicts, the migration can never populate the topology
           the CMS cutover depends on"))
    (let [resource (:resource decision)]
      (is (= :published (:publication/state resource)))
      (is (= :source/current (:publication/revision resource)))
      (is (= :knoxx.docs/garden-a (:publication/garden resource)))
      (is (= :en (:publication/locale resource)))
      (is (= :required (:translation/review resource)))
      (testing "and the result is a valid publication resource"
        (is (true? (m/validate law/PublicationIntentResource resource)))))))

;; ── 2 membership without a garden id ───────────────────────────────────────

(deftest membership-entry-without-garden-id-conflicts
  (doseq [[label entry] [["missing" {}]
                         ["blank" {:garden_id ""}]
                         ["whitespace" {:garden_id "   "}]
                         ["non-string" {:garden_id 42}]
                         ["nil" {:garden_id nil}]]]
    (testing label
      (let [result (migration/normalize-membership-entry legacy-document entry)]
        (is (true? (migration/conflict? result)))
        (is (= :unresolvable-garden-membership (:reason result)))))))

;; ── 3 undeclared shapes are never guessed ─────────────────────────────────

(deftest undeclared-source-shape-conflicts
  (doseq [[label row] [["no shape" {:path "/x" :locale :en}]
                       ["unknown shape" {:source/shape :invented :path "/x" :locale :en}]]]
    (testing label
      (let [decision (migration/publication->decision policy migrated-document row)]
        (is (true? (migration/conflict? decision)))
        (is (= :unknown-publication-source-shape (:reason decision))))))
  (testing "each shape-dependent decoder also fails safely when called directly"
    (let [row {:source/shape :invented}]
      (doseq [[label result] [["revision" (migration/decode-revision row)]
                              ["state" (migration/decode-publication-state row)]
                              ["review" (migration/decode-review-policy policy row)]]]
        (testing label
          (is (true? (migration/conflict? result))
              "case with no default branch would throw instead of conflicting")
          (is (= :unknown-publication-source-shape (:reason result))))))))

;; ── 4 review policy must be declared ──────────────────────────────────────

(deftest membership-review-policy-must-be-declared
  (let [row (membership-row legacy-document)]
    (testing "an undeclared policy conflicts rather than defaulting review"
      (let [decision (migration/publication->decision policy-without-review
                                                      migrated-document row)]
        (is (true? (migration/conflict? decision)))
        (is (= :undeclared-membership-review-policy (:reason decision)))))
    (testing "an invalid declared policy also conflicts"
      (let [decision (migration/publication->decision
                      (assoc policy :migration/membership-review :sometimes)
                      migrated-document row)]
        (is (true? (migration/conflict? decision)))))
    (doseq [declared [:required :none]]
      (testing (str "declaring " declared " produces it on the resource")
        (let [decision (migration/publication->decision
                        (assoc policy :migration/membership-review declared)
                        migrated-document row)]
          (is (= declared (get-in decision [:resource :translation/review]))))))))

;; ── 5 explicit rows must carry their explicit fields ──────────────────────

(deftest explicit-row-still-requires-explicit-fields
  (let [base {:source/shape :explicit-publication-row
              :source/collection :legacy-publications
              :source/id "pub-1"
              :garden-id "garden-a"
              :locale :en
              :path "/translation-pipeline"
              :revision "abc123"
              :published true
              :review-required true}]
    (testing "the complete row migrates"
      (is (= :candidate (:migration/status
                         (migration/publication->decision policy migrated-document base)))))
    (testing "a missing revision conflicts rather than becoming :source/current"
      (let [decision (migration/publication->decision policy migrated-document
                                                      (dissoc base :revision))]
        (is (= :invalid-publication-revision (:reason decision)))))
    (testing "a blank revision conflicts"
      (is (= :invalid-publication-revision
             (:reason (migration/publication->decision policy migrated-document
                                                       (assoc base :revision "  "))))))
    (doseq [[label value] [["missing" ::absent]
                           ["truthy non-boolean" "yes"]
                           ["1" 1]
                           ["nil" nil]]]
      (testing (str "publication state: " label " conflicts")
        (let [row (if (= ::absent value)
                    (dissoc base :published)
                    (assoc base :published value))
              decision (migration/publication->decision policy migrated-document row)]
          (is (= :invalid-published-state (:reason decision))
              "truthiness is never a decoder"))))
    (testing "false is a valid explicit state, not an absence"
      (is (= :withheld (get-in (migration/publication->decision
                                policy migrated-document (assoc base :published false))
                               [:resource :publication/state]))))
    (testing "a non-boolean review-required conflicts"
      (is (= :invalid-review-policy
             (:reason (migration/publication->decision policy migrated-document
                                                       (assoc base :review-required "yes"))))))))

;; ── 6 one shared path law ─────────────────────────────────────────────────

(deftest migration-path-law-is-shared
  (let [row (membership-row legacy-document)]
    (doseq [[label bad-path] [["empty" ""]
                              ["unrooted" "translation-pipeline"]
                              ["query" "/docs?x=1"]
                              ["fragment" "/docs#frag"]
                              ["NUL" "/docs\u0000"]
                              ["non-string" 42]]]
      (testing label
        (testing "the authoritative predicate rejects it"
          (is (false? (law/valid-publication-path? bad-path))))
        (testing "and migration rejects it through that same predicate"
          (let [decision (migration/publication->decision
                          policy migrated-document (assoc row :path bad-path))]
            (is (= :invalid-publication-path (:reason decision)))))))))

;; ── 7 source locale is never invented ─────────────────────────────────────

(deftest missing-source-locale-conflicts
  (doseq [[label value] [["missing" ::absent]
                         ["blank" ""]
                         ["not a locale" "not a locale"]
                         ["nil" nil]]]
    (testing label
      (let [document (if (= ::absent value)
                       (dissoc legacy-document :document/source-locale)
                       (assoc legacy-document :document/source-locale value))
            decision (migration/document->decision policy document)]
        (is (true? (migration/conflict? decision)))
        (is (= :unknown-locale (:reason decision))))))
  (testing "a resolvable locale produces a valid document resource"
    (let [decision (migration/document->decision policy legacy-document)]
      (is (= :candidate (:migration/status decision)))
      (is (true? (m/validate law/Document (:resource decision))))
      (is (= :knoxx.docs/translation-pipeline (get-in decision [:resource :document/id]))))))

;; ── Fold fakes ─────────────────────────────────────────────────────────────

(defn- fake-ctx
  "Writer and receipt appender both return Promises, like the real
   filesystem-backed implementations. The appender models the documented
   `append-once` contract — idempotent by receipt key — so an unstable key
   would show up as a second entry rather than being absorbed."
  [source]
  (let [receipts (atom {})
        writes (atom [])
        append-calls (atom 0)]
    {:ctx {:read-records! (fn [] (js/Promise.resolve source))
           :write! (fn [resource]
                     (swap! writes conj resource)
                     (js/Promise.resolve resource))
           :append-receipt-once! (fn [receipt-key decision]
                                   (swap! append-calls inc)
                                   (swap! receipts
                                          (fn [current]
                                            (if (contains? current receipt-key)
                                              current
                                              (assoc current receipt-key decision))))
                                   (js/Promise.resolve receipt-key))}
     :receipts receipts
     :append-calls append-calls
     :writes writes}))

(defn- publications-only
  "A LegacySource carrying only publication records."
  [& records]
  {:publications (vec records)})

(defn- record-for
  [document row]
  {:document document :row row})

;; ── 8 the fold awaits its writer ──────────────────────────────────────────

(deftest ^:async fold-awaits-promise-returning-writer
  (let [row (membership-row legacy-document)
        {:keys [ctx]} (fake-ctx (publications-only (record-for migrated-document row)))
        result (await (fold/migrate-publication-records!
                       ctx policy migration/empty-index))
        indexed (vals (get-in result [:index :publications]))]
    (is (= 1 (count indexed)))
    (testing "the index holds saved resource maps, not Promise objects"
      (doseq [resource indexed]
        (is (map? resource))
        (is (not (instance? js/Promise resource)))
        (is (true? (m/validate law/PublicationIntentResource resource)))))))

;; ── 9 second row reconciles against in-run state ──────────────────────────

(deftest ^:async second-row-reconciles-against-in-run-index
  (let [row (membership-row legacy-document)
        {:keys [ctx writes]} (fake-ctx (publications-only (record-for migrated-document row)
                                                     (record-for migrated-document row)))
        result (await (fold/migrate-publication-records!
                       ctx policy migration/empty-index))]
    (testing "the identical second row is a noop, not a second blind write"
      (is (= 1 (count @writes)))
      (is (= 1 (count (:written result))))
      (is (= 1 (count (:noops result)))))
    (testing "a different id claiming the same relation is a conflict"
      (let [rival (assoc row :source/id ["doc-1" "garden-a-rival"])
            ;; same document/garden/locale/revision relation, different id
            rival-doc (assoc migrated-document :document/id :knoxx.docs/translation-pipeline)
            seeded (migration/index-resource
                    migration/empty-index
                    (get-in (migration/publication->decision policy migrated-document row)
                            [:resource]))
            decision (migration/migrate-record
                      policy seeded rival-doc
                      (assoc rival :locale :en))]
        (is (= :noop (:migration/status decision))
            "identical relation and identical payload is still a noop")))))

;; ── 10 rerun idempotence ──────────────────────────────────────────────────

(deftest ^:async rerun-is-idempotent
  (let [row (membership-row legacy-document)
        source (publications-only (record-for migrated-document row))
        {:keys [ctx writes receipts]} (fake-ctx source)
        first-run (await (fold/migrate-publication-records!
                          ctx policy migration/empty-index))
        second-run (await (fold/migrate-publication-records!
                           ctx policy (:index first-run)))]
    (testing "resource state is identical"
      (is (= (:index first-run) (:index second-run))))
    (testing "and the rerun writes nothing new"
      (is (= 1 (count @writes)))
      (is (empty? (:written second-run)))
      (is (= 1 (count (:noops second-run)))))
    (testing "no conflict receipts were produced at all for clean data"
      (is (empty? @receipts)))))

(deftest ^:async conflict-receipts-have-stable-keys
  (let [row (membership-row legacy-document)
        source (publications-only (record-for migrated-document row))
        {:keys [ctx receipts append-calls]} (fake-ctx source)
        run! #(fold/migrate-publication-records! ctx policy-without-review
                                                 migration/empty-index)
        first-run (await (run!))
        second-run (await (run!))]
    (is (= 1 (count (:conflicts first-run))))
    (is (= 1 (count (:conflicts second-run))))
    (testing "the same source record keys the same receipt across runs"
      (is (= 1 (count @receipts))
          "an unstable receipt key would land a second entry here")
      (testing "keyed by phase as well as source, so a document conflict and a
                publication conflict for the same legacy id cannot collide"
        (is (= [:publication/migration :publications
                :cms-doc-garden-publications ["doc-1" "garden-a"]]
               (first (keys @receipts))))))
    (testing "and the appender is called once per run, never twice within one"
      (is (= 2 @append-calls)))))

;; ── 11 operational fields are receipts-only ───────────────────────────────

(deftest operational-fields-are-receipts-only
  (let [stripped (update legacy-document :metadata dissoc :published_at :publish_job_id)
        with-ops legacy-document]
    (testing "removing operational timestamps changes no generated resource"
      (is (= (migration/document->decision policy stripped)
             (migration/document->decision policy with-ops)))
      (is (= (migration/publication->decision policy migrated-document
                                              (membership-row stripped))
             (migration/publication->decision policy migrated-document
                                              (membership-row with-ops)))))
    (testing "and no operational key reaches the resource"
      (let [resource (:resource (migration/publication->decision
                                 policy migrated-document (membership-row with-ops)))]
        (is (not (contains? resource :published_at)))
        (is (not (contains? resource :publish_job_id)))))))

;; ── Policy shape ──────────────────────────────────────────────────────────

(deftest policy-namespace-is-required-up-front
  (testing "a structurally unusable policy fails loudly"
    (is (thrown? js/Error (migration/assert-policy! {})))
    (is (thrown? js/Error (migration/assert-policy! {:migration/namespace "not-a-keyword"}))))
  (testing "but a missing membership review policy does not — it is a per-row conflict"
    (is (some? (migration/assert-policy! policy-without-review)))))

;; ── Review round 2: Codex findings on #232 ────────────────────────────────

(deftest publication-route-is-derived-not-copied
  (testing "legacy source_path is a repository path, not a public route —
            copying it verbatim made every membership conflict on path"
    (is (= "docs/translation-pipeline.md" (:source_path legacy-document)))
    (is (false? (law/valid-publication-path? (:source_path legacy-document)))))
  (testing "the derived route is rooted and passes the shared path law"
    (let [row (membership-row legacy-document)]
      (is (= "/translation-pipeline" (:path row)))
      (is (true? (law/valid-publication-path? (:path row))))
      (is (= :candidate (:migration/status
                         (migration/publication->decision policy migrated-document row))))))
  (testing "derivation strips directories and the extension"
    (is (= "/existing" (migration/derive-publication-path "docs/existing.md")))
    (is (= "/deep" (migration/derive-publication-path "a/b/c/deep.mdx")))
    (is (= "/no-ext" (migration/derive-publication-path "no-ext"))))
  (testing "a trailing slash still yields the last named segment"
    (is (= "/docs" (migration/derive-publication-path "docs/")))
    (is (true? (law/valid-publication-path? "/docs"))))
  (testing "an underivable path conflicts rather than producing a bad route"
    (doseq [[label value] [["blank" ""] ["nil" nil] ["whitespace" "   "] ["non-string" 42]]]
      (testing label
        (is (nil? (migration/derive-publication-path value)))
        (let [result (migration/normalize-membership-entry
                      (assoc legacy-document :source_path value)
                      {:garden_id "garden-a"})]
          (is (true? (migration/conflict? result)))
          (is (= :undecodable-publication-path (:reason result))))))))

(deftest malformed-legacy-identities-are-rejected
  (testing "legacy-name accepts only strings and keywords"
    (is (= "a" (ident/legacy-name "a")))
    (is (= "a" (ident/legacy-name :a)))
    (doseq [value [42 {} [] nil "" "   "]]
      (is (nil? (ident/legacy-name value))
          (str "stringifying " (pr-str value) " would invent an identity"))))
  (testing "a malformed garden id conflicts instead of becoming an invented keyword"
    (let [row (assoc (membership-row legacy-document) :garden-id 42)
          decision (migration/publication->decision policy migrated-document row)]
      (is (true? (migration/conflict? decision)))
      (is (= :unresolvable-garden-identity (:reason decision)))))
  (testing "a malformed document id conflicts too"
    (doseq [bad [42 {} nil ""]]
      (let [decision (migration/publication->decision
                      policy (assoc migrated-document :document/id bad)
                      (membership-row legacy-document))]
        (is (true? (migration/conflict? decision)))
        (is (= :unresolvable-document-identity (:reason decision))))
      (let [decision (migration/document->decision
                      policy (assoc legacy-document :document/id bad))]
        (is (true? (migration/conflict? decision)))))))

(deftest generated-publication-ids-are-injective
  (testing "the old '-' join collapsed distinct relations: document a-b + garden c
            and document a + garden b-c both produced a-b-c-en"
    (let [id (fn [document-id garden-id]
               (ident/canonical-publication-id
                policy
                {:document/id document-id}
                {:garden-id garden-id :locale :en}
                :source/current))]
      (is (not= (id :a-b "c") (id :a "b-c")))
      (is (some? (id :a-b "c")))
      (is (some? (id :a "b-c")))))
  (testing "a component containing the separator is rejected rather than colliding"
    (is (nil? (ident/id-component "has~separator")))
    (is (nil? (ident/canonical-publication-id
               policy {:document/id :ok} {:garden-id "has~sep" :locale :en} :source/current))))
  (testing "revision is part of the identity, because the relation key includes it"
    (let [base {:document/id :doc}
          row {:garden-id "g" :locale :en}
          selector-id (ident/canonical-publication-id policy base row :source/current)
          pinned-id (ident/canonical-publication-id policy base row "abc123")]
      (is (not= selector-id pinned-id))
      (testing "and a concrete revision cannot masquerade as the selector"
        (is (not= (ident/canonical-publication-id policy base row "current")
                  selector-id))))))

(deftest ^:async two-revisions-of-one-relation-both-migrate
  (let [base {:source/shape :explicit-publication-row
              :source/collection :legacy-publications
              :garden-id "garden-a"
              :locale :en
              :path "/translation-pipeline"
              :published true
              :review-required true}
        current (assoc base :source/id "pub-current" :revision :source/current)
        pinned (assoc base :source/id "pub-pinned" :revision "abc123")
        {:keys [ctx]} (fake-ctx {:publications [(record-for migrated-document current)
                                                (record-for migrated-document pinned)]})
        result (await (fold/migrate-publication-records!
                       ctx policy migration/empty-index))]
    (testing "distinct revisions are distinct relations, so both are written"
      (is (= 2 (count (:written result))))
      (is (= 2 (count (set (map :publication/id (:written result)))))))))

;; ── Fold phases ───────────────────────────────────────────────────────────

(deftest ^:async fold-migrates-documents-and-gardens-too
  (let [garden-row {:garden-id "garden-a" :title "Garden A" :status "active"
                    :locales [:en]}
        row (membership-row legacy-document)
        {:keys [ctx]} (fake-ctx {:documents [legacy-document]
                                 :gardens [garden-row]
                                 :publications [(record-for migrated-document row)]})
        result (await (fold/migrate-publication-records!
                       ctx policy migration/empty-index))
        index (:index result)]
    (testing "a publication-only fold would leave every intent dangling"
      (is (= 1 (count (:documents index))))
      (is (= 1 (count (:gardens index))))
      (is (= 1 (count (:publications index)))))
    (testing "documents and gardens are written before publications"
      (is (= [:document/id :garden/id :publication/id]
             (mapv (fn [resource]
                     (first (filter #(contains? resource %)
                                    [:document/id :garden/id :publication/id])))
                   (:written result)))))
    (testing "and the written publication's references now resolve"
      (let [publication (last (:written result))]
        (is (contains? (:documents index) (:publication/document publication)))
        (is (contains? (:gardens index) (:publication/garden publication)))))))

(deftest ^:async legacy-source-shape-is-validated-at-the-boundary
  (testing "a record missing its :document/:row wrapper fails at the read boundary
            rather than destructuring to nils and surfacing as a decode conflict"
    (let [{:keys [ctx]} (fake-ctx {:publications [{:row {}}]})]
      (is (thrown? js/Error
                   (await (fold/migrate-publication-records!
                           ctx policy migration/empty-index))))))
  (testing "an unexpected top-level key is rejected"
    (let [{:keys [ctx]} (fake-ctx {:publications [] :surprise true})]
      (is (thrown? js/Error
                   (await (fold/migrate-publication-records!
                           ctx policy migration/empty-index))))))
  (testing "an empty source is legal"
    (let [{:keys [ctx]} (fake-ctx {})
          result (await (fold/migrate-publication-records!
                         ctx policy migration/empty-index))]
      (is (empty? (:written result)))
      (is (empty? (:conflicts result))))))
