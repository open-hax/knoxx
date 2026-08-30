(ns knoxx.frontend.pages.translations.logic
  "Pure logic for the translation review page. CLJS port of the helpers
   in src/pages/TranslationReviewPage.tsx."
  (:require [clojure.string :as str]
            [knoxx.frontend.pages.translations.review-contract :as review-contract]
            [knoxx.frontend.pages.translations.split-review :as split-review]))

(def ^:private lang-names
  {"en" "English" "es" "Español" "fr" "Français" "de" "Deutsch"
   "ja" "日本語" "zh" "中文" "ko" "한국어" "pt" "Português"
   "ru" "Русский" "it" "Italiano"})

(defn lang-name
  "Return the display name for a locale code, preserving unknown codes."
  [code]
  (get lang-names code code))

(def default-label
  "Empty review form values shown before a persisted judgment is selected."
  (assoc review-contract/default-form :overall "approve"))

(def ^:private status-classes
  {"approved" "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"
   "rejected" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "in_review" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "pending" "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300"
   "fully_approved" "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"
   "pending_review" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "partial_review" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "fully_rejected" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "mixed" "bg-purple-100 text-purple-700 dark:bg-purple-500/15 dark:text-purple-300"
   "missing" "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300"
   "queued" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "running" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "in_flight" "bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"
   "failed" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "stale" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "evidence_missing" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "evidence_unbound" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "revision_unresolved" "bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300"
   "ready" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "changes_requested" "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
   "published" "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"})

(def ^:private status-icons
  {"approved" "✅" "rejected" "❌" "in_review" "📝" "pending" "⏳"
   "fully_approved" "✅" "pending_review" "⏳" "partial_review" "🔄"
   "fully_rejected" "❌" "mixed" "🔀" "missing" "⏳" "queued" "⏳"
   "running" "🔄" "in_flight" "🔄" "failed" "❌" "stale" "❌"
   "evidence_missing" "❌" "evidence_unbound" "❌" "revision_unresolved" "❌" "ready" "📝" "changes_requested" "📝"
   "published" "✅"})

(defn status-class
  "Return presentation classes for a review or work status."
  [status]
  (get status-classes status (get status-classes "pending")))

(defn status-icon
  "Return the compact icon for a review or work status."
  [status]
  (get status-icons status "⏳"))

(defn status-label
  "Turn an underscore-delimited wire status into a display label."
  [status]
  (str/replace status "_" " "))

(defn progress-pct
  "Return a safe completion percentage for a progress summary."
  [approved total]
  (if (pos? total) (* 100 (/ approved total)) 0))

(defn field-options
  "Return the closed option vocabulary for one review score field."
  [field]
  (review-contract/field-options field))

(defn prepare-label-payload
  "Label POST payload: preserves nonblank correction bytes, trims notes,
   omits blank optional text, and applies the chosen overall verdict."
  [form overall]
  (let [corrected (review-contract/correction-text (:corrected_text form))
        notes (review-contract/trimmed-optional-text (:editor_notes form))]
    (cond-> (-> form
                (assoc :overall overall)
                (dissoc :corrected_text :editor_notes))
      corrected (assoc :corrected_text corrected)
      notes (assoc :editor_notes notes))))

(defn find-segment
  "Find one split projection by its manifest ordinal."
  [detail segment-index]
  (when (and detail (some? segment-index))
    (first (filter #(= segment-index (:segment_index %)) (:segments detail)))))

(def ^:private fallback-langs ["es" "de" "ko" "fr" "ja" "zh" "it" "pt" "ru"])

(defn available-langs
  "Return manifest locales, or the historical fallback locale list."
  [manifest]
  (if manifest
    (mapv name (keys (:languages manifest)))
    fallback-langs))

(defn- document-id [value]
  (or (:document value) (:document_id value)))

(defn- garden-id [value]
  (or (:garden value) (:garden_id value)))

(defn- target-locale [value]
  (or (:locale value) (:target_lang value)))

(defn- source-locale [value]
  (or (:source_locale value) (:source_lang value)))

(defn- review-key [value]
  [(document-id value) (garden-id value) (target-locale value)])

(defn work-row-id
  "Stable identity for one desired publication work row.

   Resource-backed inventory is keyed by publication intent. The coordinate
   fallback keeps the legacy Mongo segment list usable while that older surface
   is still joined into this page. It includes garden deliberately: one document
   and locale may be desired in more than one garden."
  [row]
  (when row
    (or (:publication row)
        (:work_id row)
        (let [[document _garden locale :as coordinates] (review-key row)]
          (when (and (some? document) (some? locale))
            coordinates)))))

(defn same-work?
  "Whether two rows describe the same desired publication work item."
  [left right]
  (let [left-id (work-row-id left)
        right-id (work-row-id right)]
    (and (some? left-id)
         (some? right-id)
         (= left-id right-id))))

(defn work-row?
  "Whether a row came from the resource-derived work inventory."
  [row]
  (and (some? (:publication row))
       (some? (:work_state row))))

(defn candidate-present?
  "Whether a work value carries completed candidate evidence.

   Supports the resource-first nested shape and the flattened compatibility
   shape consumed by the current page. A legacy-only document summary is marked
   explicitly when no resource inventory exists, because its persisted segments
   are the candidate in that compatibility mode. Source revision or aggregate
   segment counts alone never count as candidate evidence."
  [value]
  (or (some? (:translation_revision value))
      (some? (get-in value [:candidate :translation_revision]))
      (some? (split-review/review value))
      (some? (split-review/review (:candidate value)))
      (some? (:publication_review value))
      (true? (:legacy_candidate value))))

(defn legacy-candidate?
  "Whether this row names a persisted legacy split set admitted for review."
  [row]
  (true? (:legacy_candidate row)))

(defn- wire-name [value]
  (if (keyword? value) (name value) value))

(def ^:private authenticated-content-sources
  "Resource content origins the backend authenticates against receipt bytes."
  #{"agent" "authored-contract"})

(defn- authenticated-content-source?
  [row]
  (contains? authenticated-content-sources
             (wire-name (:content_source row))))

(defn blocked-resource-candidate?
  "Whether a resource candidate failed the server's exact-byte hydration gate.

   A contract candidate is displayable only when the server says both that it
   is reviewable, that hydration produced the exact source/target pair, and
   which authenticated content store supplied those bytes. Missing evidence,
   including a future/unknown hydration state or source, fails closed."
  [row]
  (and (nil? (split-review/review row))
       (true? (:contract_candidate row))
       (or (not (true? (:reviewable row)))
           (not= "displayable" (wire-name (:hydration_state row)))
           (not (authenticated-content-source? row)))))

(defn allowed-action?
  "Whether the server explicitly admitted `action` for this work item.

   No work-state inference belongs here. Retryability is a dispatch law and the
   resource projection must carry its decision across the boundary."
  [row action]
  (let [wanted (if (keyword? action) (name action) (str action))]
    (boolean
     (some #(= wanted (if (keyword? %) (name %) (str %)))
           (:allowed_actions row)))))

(defn still-listed?
  "Whether the selected publication work identity remains in a refreshed list."
  [documents selected]
  (boolean (some #(same-work? % selected) documents)))

(defn legacy-review-scope
  "Scope an older segment API call to the selected work row.

   Resource inventory owns project when the server supplies it; the page-level
   project is only a compatibility fallback for legacy-only documents. Garden
   is always row-specific, because the same document and locale may be present
   in multiple gardens."
  [row fallback-project]
  (let [project (or (some-> (:project row) str not-empty)
                    (some-> fallback-project str not-empty))
        garden (some-> (garden-id row) str not-empty)]
    (cond-> {}
      project (assoc :project project)
      garden (assoc :garden-id garden))))

(defn effective-project
  "Prefer the server-authored inventory project over the page fallback.

   The response envelope remains authoritative when the desired work relation
   is empty, so an empty inventory cannot silently send the legacy query back
   to the UI's historical `devel` default."
  [inventory fallback-project]
  (let [work-items (if (map? inventory) (:reviews inventory) inventory)]
    (or (when (map? inventory)
          (some-> (:project inventory) str not-empty))
        (some #(some-> (:project %) str not-empty) work-items)
        (some-> fallback-project str not-empty))))

(defn legacy-mutation-admitted?
  "Whether legacy mutation controls have a persisted split and project scope.

   Garden is optional because the backend treats an omitted garden as the exact
   legacy nil-garden coordinate, never as a wildcard."
  [row fallback-project]
  (let [{:keys [project]} (legacy-review-scope row fallback-project)]
    (and (legacy-candidate? row)
         (some? project))))

(defn- candidate-fields [work]
  (if (map? (:candidate work))
    (merge work (:candidate work))
    work))

(defn- work-state [work]
  (or (:work_state work)
      (when (candidate-present? work)
        (if (:approved work) "approved" "ready"))))

(defn- candidate-summary
  [flat candidate? approved? explicit-state]
  (let [split-progress (when (split-review/review flat)
                         (split-review/progress flat))
        total (or (:total split-progress)
                  (:total_segments flat)
                  (:split_count flat)
                  (if candidate? 1 0))]
    {:approved (if split-progress
                 (:approved split-progress)
                 (if approved? total 0))
     :total total
     :overall-status (or (when split-progress
                           (split-review/overall-status flat))
                         explicit-state
                         (when candidate?
                           (if approved? "fully_approved" "pending_review"))
                         "pending")}))

(defn- document-row-value
  [work flat state review {:keys [approved total overall-status]}]
  (assoc flat
         :publication (:publication work)
         :document_id (document-id work)
         :garden_id (garden-id work)
         :source_lang (or (:source_locale work) (:source_lang work))
         :target_lang (target-locale work)
         :title (:title work)
         :work_state state
         :allowed_actions (vec (or (:allowed_actions work) []))
         :approved approved
         :total_segments total
         :overall_status overall-status
         :publication_review review))

(defn- work-item->document-row [work]
  (let [flat (candidate-fields work)
        candidate? (candidate-present? flat)
        approved? (true? (:approved work))
        explicit-state (:work_state work)
        state (work-state work)
        summary (candidate-summary flat candidate? approved? explicit-state)
        review (when candidate? flat)]
    (cond-> (document-row-value work flat state review summary)
      (and candidate?
           (or (true? (:contract_candidate flat))
               (authenticated-content-source? flat)
               (some? (split-review/review flat))))
      (assoc :contract_content true))))

(def ^:private legacy-summary-fields
  "Legacy aggregate fields that may safely decorate authoritative work.

   Candidate identity, text, revisions, status and actions are intentionally
   absent. Copying the whole legacy row here lets old Mongo evidence overwrite
   the resource candidate merely because both share document coordinates."
  [:title :source_lang :project :pending :rejected :in_review :visibility])

(def ^:private legacy-review-fields
  "Aggregate split-review facts that may decorate candidate-less resource work.

   These fields describe the persisted legacy split set, not publication
   authority. Resource work state/actions and `publication_review` therefore
   remain untouched."
  [:total_segments :approved :pending :rejected :in_review :overall_status])

(defn- legacy-split-candidate?
  [document]
  (pos? (or (:total_segments document) 0)))

(defn- compatible-project?
  "Require the project coordinate to be present and equal on both relations."
  [document work]
  (let [document-project (some-> (:project document) str not-empty)
        work-project (some-> (:project work) str not-empty)]
    (and (some? document-project)
         (some? work-project)
         (= document-project work-project))))

(defn- compatible-source-locale?
  "Require an explicit equal source locale before exposing legacy splits."
  [document work]
  (let [document-locale (some-> (source-locale document) str not-empty)
        work-locale (some-> (source-locale work) str not-empty)]
    (and (some? document-locale)
         (some? work-locale)
         (= document-locale work-locale))))

(defn- matching-legacy-document
  [documents work]
  (first (filter #(and (= (review-key %) (review-key work))
                       (compatible-project? % work)
                       (compatible-source-locale? % work))
                 documents)))

(defn- legacy-compatibility-evidence
  "Record what the compatibility bridge proved and what it cannot prove.

   Legacy segments predate resource revisions, so coordinate equality is useful
   evidence for restoring the old review UI but is never revision authority."
  [document work]
  {:basis :exact-coordinate-match
   :document (document-id work)
   :garden (garden-id work)
   :source_locale (source-locale work)
   :legacy_source_locale (source-locale document)
   :locale (target-locale work)
   :project (:project work)
   :legacy_project (:project document)
   :source_revision :unavailable})

(defn- attach-work-item [document work]
  (let [resource-candidate? (candidate-present? work)
        decorated (reduce (fn [row field]
                            (if (or (some? (get row field))
                                    (not (contains? document field)))
                              row
                              (assoc row field (get document field))))
                          (work-item->document-row work)
                          legacy-summary-fields)]
    (if (or resource-candidate?
            (not (legacy-split-candidate? document)))
      decorated
      (reduce (fn [row field]
                (if (contains? document field)
                  (assoc row field (get document field))
                  row))
              (assoc decorated
                     :legacy_candidate true
                     :legacy_compatibility
                     (legacy-compatibility-evidence document work))
              legacy-review-fields))))

(defn- legacy-document->row
  "Make persisted legacy segments explicit candidate evidence.

   This adapter is used only when the resource inventory is empty. Once desired
   resource work exists, its candidate/evidence projection is authoritative."
  [document]
  (assoc document :legacy_candidate true))

(defn normalize-work-inventory
  "Join legacy segment summaries onto a resource-first translation work list.

   Desired publication work is authoritative for cardinality. Candidate content,
   dispatch records and Mongo segments are optional evidence about those rows;
   their absence must never erase a desired document/locale pair, and an orphan
   legacy row must never add a nineteenth card to an eighteen-item resource
   inventory. When there is no resource inventory at all, legacy summaries are
   retained as candidate-bearing rows so the pre-resource review flow continues
   to work."
  [documents work-items]
  (if (seq work-items)
    (mapv (fn [work]
            (if-let [document (matching-legacy-document documents work)]
              (attach-work-item document work)
              (work-item->document-row work)))
          work-items)
    (mapv legacy-document->row documents)))

(defn attach-publication-reviews
  "Compatibility name for `normalize-work-inventory`.

   The second argument historically contained completed publication reviews.
   It now accepts the resource-derived work inventory, of which a completed
   review is only one possible state."
  [documents reviews]
  (normalize-work-inventory documents reviews))

(defn authored-detail
  "Synthesize a reviewable detail from a contract-backed review's text.

   Named for the case it was written for; it now serves agent-produced content
   too, which arrives through the same wire fields."
  [selected]
  (let [review (:publication_review selected)
        source-blocks (str/split (or (:source_text review) "") #"\n\s*\n")
        translated-blocks (str/split (or (:translated_text review) "") #"\n\s*\n")
        segments (mapv (fn [index translated]
                         {:id (str (:document_id selected) "-" (:target_lang selected) "-" index)
                          :segment_index index
                          :status (if (:approved review) "approved" "pending")
                          :source_lang (:source_lang selected)
                          :target_lang (:target_lang selected)
                          :source_text (get source-blocks index "")
                          :translated_text translated})
                       (range (count translated-blocks))
                       translated-blocks)]
    {:document {:title (:title selected)
                :source_lang (:source_lang selected)}
     :target_lang (:target_lang selected)
     :summary {:total_segments (count segments)
               :approved (if (:approved review) (count segments) 0)
               :overall_status (if (:approved review)
                                 "fully_approved"
                                 "pending_review")}
     :segments segments}))

(def ^:private review-form-fields
  (conj review-contract/review-form-fields :overall))

(defn segment-review-form
  "Hydrate the editor from the newest persisted label for one segment.

   Document detail returns labels newest first. A segment without labels gets a
  fresh form, which also prevents corrections typed for another split from
  leaking across a selection change."
  [segment]
  (if (:resource_split segment)
    (split-review/review-form segment)
    (merge default-label
           (select-keys (first (:labels segment)) review-form-fields))))

(defn segment-review-identity
  "Return the newest immutable review fact that owns a selected form.

   Resource splits expose their effective receipt directly; the first history
   label is a rollout-safe fallback. Legacy document detail already orders
   labels newest first. The scalar identity is safe in a React dependency
   vector and changes when the same split is reviewed elsewhere."
  [segment]
  (let [newest-label (first (:labels segment))]
    (if (:resource_split segment)
      (or (:review_id segment)
          (:review_id newest-label)
          (:id newest-label))
      (or (:id newest-label)
          (:review_id newest-label)))))

(defn approval-request
  "The exact immutable coordinates the server exposed for approval."
  [review]
  (select-keys review [:document :garden :locale :revision :translation_revision]))

(defn sft-filename
  "Return the stable filename for one translation export selection."
  [project target-lang]
  (str project "-" (or (not-empty target-lang) "all") "-translations.jsonl"))
