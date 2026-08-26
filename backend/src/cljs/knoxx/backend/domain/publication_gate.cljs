(ns knoxx.backend.domain.publication-gate
  "Translation and review as a publication prerequisite.

  Desired state says what must eventually be published; receipts say what has
  actually happened; this namespace is the pure decision between them. No
  operational state — `:translating`, `:reviewing`, `:worker-failed` — is ever
  written back into a publication resource.

  The load-bearing rule is that **evidence is computed once**. A revision
  selector like `:source/current` resolves to a concrete revision, and that one
  revision must be the revision every downstream consumer sees. If blockers,
  admissibility, and queueing each recomputed evidence, `:source/current` could
  resolve differently between calls and the decision that admitted publication
  would disagree with the revision actually queued or materialized. So
  `publication-evidence` is the boundary: everything else takes its result.

  `facts` is a map of lookup functions, keeping this namespace free of any
  receipt store, worker, or HTTP dependency:

    :current-source-revision      [document] -> revision or nil
    :translated-revision?         [document garden locale revision] -> boolean
    :approved?                    [document garden locale revision] -> boolean
    :source-revision-superseded?  [intent revision] -> boolean

  Admissibility is deliberately split across two layers rather than duplicated.
  `law.publication/admissible-publication?` decides the *structural* question
  from the resource graph — is this state reconcilable, do the references
  resolve, is the garden active. `admissible?` here decides the *evidential*
  question from receipts. Neither subsumes the other, and this namespace assumes
  the structural half holds upstream.

  What is NOT restated is the state vocabulary: which state means \"publish\" is
  owned by `law.publication/publishes?`, so the two layers cannot drift on it."
  (:require [knoxx.backend.law.publication :as law]))

(defn translation-required?
  "Translation is required exactly when the target locale differs from the
   document's own source locale. The source locale is read off the hydrated
   intent — the gate never defaults a language."
  [intent]
  (not= (:publication/locale intent)
        (:document/source-locale intent)))

(defn resolve-concrete-revision
  "Resolve a revision selector to a concrete revision.

   A selector token is never handed to a receipt lookup: receipts are keyed by
   concrete revisions, so comparing `:source/current` against one would silently
   never match."
  [intent facts]
  (if (= :source/current (:publication/revision intent))
    ((:current-source-revision facts) (:publication/document intent))
    (:publication/revision intent)))

(defn publication-evidence
  "THE evidence boundary. Resolves the revision selector once, then gathers
   blockers against that single concrete revision.

   Returns `{:concrete-revision r :blockers [...]}`. An unresolvable selector
   short-circuits: no evidence lookup happens, because every lookup would be
   keyed by a revision that does not exist."
  [intent facts]
  (let [revision (resolve-concrete-revision intent facts)
        document (:publication/document intent)
        garden (:publication/garden intent)
        locale (:publication/locale intent)]
    (if (nil? revision)
      {:concrete-revision nil :blockers [:publication-revision-unresolved]}
      {:concrete-revision revision
       :blockers
       (cond-> []
         (and (translation-required? intent)
              (not ((:translated-revision? facts) document garden locale revision)))
         (conj :translation-missing)

         (and (= :required (:translation/review intent))
              (not ((:approved? facts) document garden locale revision)))
         (conj :translation-review-required)

         ((:source-revision-superseded? facts) intent revision)
         (conj :translation-stale))})))

;; ── Consumers of one evidence result ───────────────────────────────────────
;;
;; Each takes an already-computed evidence map rather than recomputing it, so the
;; concrete revision cannot drift between the decision to publish, the work
;; queued, and the artifact materialized.

(defn blockers
  [evidence]
  (:blockers evidence))

(defn admissible?
  "True when the intent wants publication and no evidence blocks it.

   The evidential half of admissibility. \"Wants publication\" is asked of
   `law.publication/publishes?` rather than compared against a literal here, so
   this decision and the contract layer's structural one read one vocabulary."
  [intent evidence]
  (and (law/publishes? intent)
       (some? (:concrete-revision evidence))
       (empty? (:blockers evidence))
       true))

(defn translation-work-eligible?
  "Only an intent that actually wants publication derives work. The resolver
   deliberately keeps `:archived` and `:withheld` intents in its projection as
   history, so an evidence-only check would queue obsolete content forever."
  [intent]
  (and (law/publishes? intent)
       (translation-required? intent)))

(defn translation-work
  "Derivative queue action, keyed to the concrete revision from `evidence` —
   never to a selector token. Stale evidence produces replacement work rather
   than blocking indefinitely with no derivable action."
  [intent evidence]
  (let [{:keys [concrete-revision]} evidence
        blocker-set (set (:blockers evidence))]
    (when (and (translation-work-eligible? intent)
               (some? concrete-revision)
               (or (contains? blocker-set :translation-missing)
                   (contains? blocker-set :translation-stale)))
      {:action/id :actions/request-translation
       :action/with {:document (:publication/document intent)
                     :locale (:publication/locale intent)
                     :revision concrete-revision
                     :replace-stale? (contains? blocker-set :translation-stale)}})))

(defn review-satisfies-intent?
  "Approval is revision-specific. A stale translation does not delete the old
   approval receipt — the receipt simply stops satisfying the new concrete
   revision, so the replacement requires a new approval."
  [approval intent concrete-revision]
  (and (= (:review/document approval) (:publication/document intent))
       (= (:review/locale approval) (:publication/locale intent))
       (= (:review/revision approval) concrete-revision)
       (= :approved (:review/state approval))
       true))

;; ── Convenience ────────────────────────────────────────────────────────────

(defn gate
  "Everything a caller needs from one evidence computation. Prefer this over
   calling the consumers separately: it is the shape that makes the
   compute-once rule the easy path."
  [intent facts]
  (let [evidence (publication-evidence intent facts)]
    (assoc evidence
           :admissible? (admissible? intent evidence)
           :translation-work (translation-work intent evidence))))
