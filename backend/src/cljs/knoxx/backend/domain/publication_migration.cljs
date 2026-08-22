(ns knoxx.backend.domain.publication-migration
  "One-time authority transfer from legacy garden/publication metadata into
  Knoxx publication resources.

  Pure decision logic only: normalization, per-field decoding, and the
  write/noop/conflict classification of a single source record. The effectful
  fold — reading legacy records, writing resources, appending receipts — lives
  in `knoxx.backend.infra.publication-migration`, because a namespace that
  performs I/O cannot also be the pure decision layer.

  Two rules shape everything here. Nothing is defaulted: a missing, malformed,
  or unrecognized legacy value becomes a conflict carrying its source evidence,
  never guessed resource data. And nothing is decoded by guessing: every legacy
  row is first normalized into a *declared source shape*, and each shape states
  which semantic fields it carries explicitly versus which the shape itself
  defines. Truthiness is never a decoder."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.domain.publication-migration-identity :as ident]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.law.publication :as law]))

;; ── Decision vocabulary ────────────────────────────────────────────────────

(defn conflict
  [reason field value row]
  {:migration/status :conflict
   :reason reason
   :field field
   :value value
   :source row})

(defn conflict?
  [decision]
  (= :conflict (:migration/status decision)))

(defn- decoded
  "First failing decode among results, or nil when all succeeded."
  [results]
  (some #(when (conflict? %) %) results))

;; ── Migration policy ───────────────────────────────────────────────────────

(def MigrationPolicy
  "Declared inputs for a migration run. The namespace generated resource ids
   are qualified under is structural and required up front. Membership review
   policy is deliberately NOT required here: its absence must surface as a
   per-row conflict carrying source evidence, not as an upfront throw that
   names no legacy row."
  [:map
   [:migration/namespace :keyword]
   [:migration/membership-review {:optional true} [:enum :required :none]]])

(defn assert-policy!
  [policy]
  (law/assert-valid! :migration/policy MigrationPolicy policy))

;; ── Declared source shapes ─────────────────────────────────────────────────

(def GardenMembershipEntry
  "A `CmsDocMetadata.garden_publications` entry. It carries only a garden id —
   membership in that array IS the legacy published fact."
  [:map [:garden_id law/NonBlankString]])

(def declared-source-shapes
  #{:garden-membership :explicit-publication-row})

(defn derive-publication-path
  "Turn a legacy repository path into a public route.

   `source_path` is a repository path — `docs/existing.md` — while
   `valid-publication-path?` requires a rooted route with no query or fragment.
   Copying it verbatim would make EVERY legacy membership conflict on path and
   the migration could never populate the topology. The route is the file's
   basename without its extension, rooted."
  [source-path]
  (when (m/validate law/NonBlankString source-path)
    (let [basename (last (str/split (str/trim source-path) #"/"))
          without-extension (str/replace (str basename) #"\.[^.]+$" "")]
      (when (seq without-extension)
        (str "/" without-extension)))))

(defn normalize-membership-entry
  "Project one legacy membership entry onto the `:garden-membership` shape.
   Locale and route come from the owning document, since the entry carries
   neither."
  [document entry]
  (cond
    (not (m/validate GardenMembershipEntry entry))
    (conflict :unresolvable-garden-membership :garden_id (:garden_id entry) entry)

    (nil? (derive-publication-path (:source_path document)))
    (conflict :undecodable-publication-path :source_path (:source_path document) entry)

    :else
    {:migration/status :normalized
     :row {:source/shape :garden-membership
           :source/collection :cms-doc-garden-publications
           :source/id [(:legacy/doc-id document) (:garden_id entry)]
           :garden-id (:garden_id entry)
           :path (derive-publication-path (:source_path document))
           :locale (:document/source-locale document)}}))

(defn normalize-publication-rows
  [document]
  (mapv #(normalize-membership-entry document %)
        (get-in document [:metadata :garden_publications] [])))

;; ── Field decoders ─────────────────────────────────────────────────────────
;;
;; Every shape-dependent decoder carries an explicit unknown-shape branch.
;; `case` throws on no match without a default, so an unrecognized shape would
;; otherwise blow up the run instead of producing the conflict receipt the
;; migration promises. `publication->decision` also short-circuits on the shape
;; decode before calling any of these, so the guard is enforced twice: once so
;; the decision never depends on decoder ordering, and once so a decoder called
;; directly still fails safely.

(defn- unknown-shape
  [row]
  (conflict :unknown-publication-source-shape :source/shape (:source/shape row) row))

(defn decode-source-shape
  [row]
  (if (contains? declared-source-shapes (:source/shape row))
    {:ok (:source/shape row)}
    (unknown-shape row)))

(defn decode-garden-status
  [row]
  (case (:status row)
    "active" {:ok :active}
    "archived" {:ok :archived}
    (conflict :unknown-garden-status :status (:status row) row)))

(defn decode-locale
  [field row]
  (let [value (get row field)
        locale (cond
                 (keyword? value) value
                 (and (string? value) (seq value)) (keyword value))]
    (if (and locale (m/validate law/Locale locale))
      {:ok locale}
       (conflict :unknown-locale field value row))))

(defn decode-garden-locales
  "Decode a legacy target's explicit locale catalog without inventing a default.
   The website owns reader-side defaults; a migration may only write locales the
   legacy target actually declared."
  [row]
  (let [locales (:locales row)]
    (if (m/validate law/LocaleCatalog locales)
      {:ok locales}
      (conflict :invalid-garden-locales :locales locales row))))

(defn decode-revision
  [row]
  (case (:source/shape row)
    ;; Legacy membership republished whatever the live document held. That
    ;; selector is what the shape MEANS, not an inference from an absent field.
    :garden-membership {:ok :source/current}

    :explicit-publication-row
    (let [value (:revision row)]
      (cond
        (= :source/current value) {:ok :source/current}
        (m/validate law/NonBlankString value) {:ok value}
        :else (conflict :invalid-publication-revision :revision value row)))

    (unknown-shape row)))

(defn decode-publication-state
  [row]
  (case (:source/shape row)
    ;; Presence in `garden_publications` is the legacy published fact.
    :garden-membership {:ok :published}

    :explicit-publication-row
    (if (boolean? (:published row))
      {:ok (if (:published row) :published :withheld)}
      (conflict :invalid-published-state :published (:published row) row))

    (unknown-shape row)))

(defn decode-review-policy
  "The one semantic field legacy membership genuinely does not represent, so it
   is neither guessed nor defaulted — the run must declare it."
  [policy row]
  (case (:source/shape row)
    :garden-membership
    (let [declared (:migration/membership-review policy)]
      (cond
        (nil? declared) (conflict :undeclared-membership-review-policy
                                  :migration/membership-review nil row)
        (contains? #{:required :none} declared) {:ok declared}
        :else (conflict :invalid-review-policy
                        :migration/membership-review declared row)))

    :explicit-publication-row
    (if (boolean? (:review-required row))
      {:ok (if (:review-required row) :required :none)}
      (conflict :invalid-review-policy :review-required (:review-required row) row))

    (unknown-shape row)))

;; ── Decisions ──────────────────────────────────────────────────────────────

(defn document->decision
  "A document may not invent its source locale: publication gating reads it, so
   an unresolvable locale must be resolved before cutover."
  [policy document]
  (let [locale (decode-locale :document/source-locale document)
        document-name (ident/id-component (:document/id document))]
    (cond
      (conflict? locale) locale

      (nil? document-name)
      (conflict :unresolvable-document-identity :document/id (:document/id document) document)

      (not (m/validate law/NonBlankString (:source_path document)))
      (conflict :invalid-document-source-path :source_path (:source_path document) document)

      :else
      {:migration/status :candidate
       :resource {:document/id (ident/canonical-document-id policy document)
                  :document/title (or (:title document) "")
                  :document/source-locale (:ok locale)
                  :document/source {:path (:source_path document)}}})))

(defn garden->decision
  [policy row]
  (let [status (decode-garden-status row)
        locales (decode-garden-locales row)
        garden-id (ident/canonical-garden-id policy row)]
    (cond
      (conflict? status) status

      (conflict? locales) locales

      (nil? garden-id)
      (conflict :unresolvable-garden-identity :garden-id (:garden-id row) row)

      :else
      {:migration/status :candidate
        :resource {:garden/id garden-id
                   :garden/title (or (:title row) "")
                   :garden/status (:ok status)
                   :garden/locales (:ok locales)}})))

(defn- identity-conflict
  "First identity component that cannot be represented faithfully, or nil.

   Checked before a resource is assembled, so a malformed legacy value becomes a
   conflict carrying its source evidence rather than an invented id written as a
   resource."
  [document row publication-id]
  (cond
    (nil? (ident/id-component (:document/id document)))
    (conflict :unresolvable-document-identity :document/id (:document/id document) row)

    (nil? (ident/id-component (:garden-id row)))
    (conflict :unresolvable-garden-identity :garden-id (:garden-id row) row)

    (nil? publication-id)
    (conflict :unrepresentable-publication-identity :publication/id nil row)))

(defn publication->decision
  "Short-circuits on the shape decode before any shape-dependent decoder runs,
   so an unrecognized shape yields exactly one conflict naming the shape rather
   than whichever field decoder happened to be called first."
  [policy document row]
  (let [shape (decode-source-shape row)]
    (if (conflict? shape)
      shape
      (let [locale (decode-locale :locale row)
            revision (decode-revision row)
            state (decode-publication-state row)
            review (decode-review-policy policy row)
            invalid (decoded [locale revision state review])
            publication-id (when-not invalid
                             (ident/canonical-publication-id policy document row (:ok revision)))]
        (or invalid
            (identity-conflict document row publication-id)
            (when-not (law/valid-publication-path? (:path row))
              (conflict :invalid-publication-path :path (:path row) row))
            {:migration/status :candidate
             :resource {:publication/id publication-id
                        ;; The same rule the document phase writes under. Copying
                        ;; the raw legacy value here left the reference
                        ;; unqualified while the document itself was written
                        ;; qualified, so PublicationIntentResource rejected it
                        ;; and the batch aborted instead of migrating.
                        :publication/document (ident/canonical-document-id policy document)
                        :publication/garden (ident/canonical-garden-id policy row)
                        :publication/locale (:ok locale)
                        :publication/revision (:ok revision)
                        :publication/state (:ok state)
                        :publication/path (:path row)
                        :translation/review (:ok review)}})))))

;; ── In-run migration index ─────────────────────────────────────────────────

(def empty-index
  {:documents {} :gardens {} :publications {}})

(defn index-resource
  "Index a *saved* resource. The fold must await its writer before calling
   this; indexing an unresolved Promise would silently poison every later
   reconciliation in the run."
  [index resource]
  (cond
    (:publication/id resource) (assoc-in index [:publications (:publication/id resource)] resource)
    (:document/id resource) (assoc-in index [:documents (:document/id resource)] resource)
    (:garden/id resource) (assoc-in index [:gardens (:garden/id resource)] resource)
    :else index))

(defn equivalent?
  [index candidate]
  (= candidate (get-in index [:publications (:publication/id candidate)])))

(defn conflicts?
  "A candidate conflicts when its own id is already held by a different
   payload, or when a *different* id already claims the same
   document × garden × locale × revision relation and both are active."
  [index candidate]
  (let [existing (get-in index [:publications (:publication/id candidate)])
        relation (resolver/publication-key candidate)]
    (boolean
     (or (and existing (not= existing candidate))
         (and (resolver/active-publication-intent? candidate)
              (some (fn [[id indexed]]
                      (and (not= id (:publication/id candidate))
                           (resolver/active-publication-intent? indexed)
                           (= relation (resolver/publication-key indexed))))
                    (:publications index)))))))

(defn migrate-record
  "Classify one normalized source record against the in-run index."
  [policy index document row]
  (let [decision (publication->decision policy document row)]
    (if (conflict? decision)
      decision
      (let [candidate (:resource decision)]
        (cond
          (equivalent? index candidate)
          {:migration/status :noop :resource/id (:publication/id candidate)}

          (conflicts? index candidate)
          {:migration/status :conflict
           :reason :publication-conflict
           :candidate candidate
           :source row}

          :else
          {:migration/status :write
           :resource (law/assert-valid! (:publication/id candidate)
                                        law/PublicationIntentResource
                                        candidate)})))))
