(ns knoxx.backend.domain.translation-evidence
  "The gate's translation facts, assembled from already-loaded evidence.

  `domain.publication-gate` takes a `facts` map of *synchronous* predicates:

    :translated-revision?  [document garden locale revision] -> boolean
    :approved?             [document garden locale revision] -> boolean

  That signature is not incidental, and it is the reason this namespace exists
  as its own step. The gate computes evidence exactly once so a revision
  selector cannot resolve differently between the decision to publish and the
  artifact materialized. Predicates that each went and awaited a store would
  reintroduce precisely the drift that rule forbids. So a runtime loads evidence
  first, and then builds predicates that only read it.

  Pure by mandate — no store, no clock, no I/O — which is why this is `.cljc`
  while the store feeding it is not. It is the first portable `domain.*`
  namespace in this backend; its requires are laws only, so it loads anywhere.

  Both evidential predicates live here now, and `approved?` is a *join* rather
  than a lookup: an approval only counts while it still names the translation
  output the receipt currently holds. `gate-facts` still returns a partial map —
  the two source-revision facts are owned elsewhere — see its docstring."
  (:require [knoxx.backend.law.translation-evidence :as law]))

(defn evidence-key
  "The four coordinates every lookup is keyed by.

   A vector rather than nested maps: the four coordinates always travel
   together, and a partial key is never a meaningful thing to hold."
  [document garden locale revision]
  [document garden locale revision])

(defn index-receipts
  "Index completed translation receipts by
   `[document garden locale source-revision]`.

   Every receipt is validated on the way in. A store is replaceable, so what it
   returns is untrusted input — the rule `law.publication-receipts` already
   applies to an adapter's output.

   Which receipt is current is decided by `law.translation-evidence/supersedes?`,
   a total order — never by position in the sequence. Arrival order would have
   made correctness depend on every store preserving insertion order across a
   query: easy for one implementation to honor by accident and for a later one to
   break silently."
  [receipts]
  (reduce (fn [index receipt]
            (let [checked (law/assert-receipt! receipt)
              entry-key (evidence-key (:translation/document checked)
                                          (:translation/garden checked)
                                          (:translation/locale checked)
                                          (:translation/source-revision checked))]
              (if (law/supersedes? checked (get index entry-key))
                (assoc index entry-key checked)
                index)))
          {}
          receipts))

(defn index-approvals
  "Index approvals by `[document garden locale revision]`.

   Validated on the way in for the same reason as receipts. Unlike receipts,
   *all* approvals for a key are retained rather than collapsed to the newest:
   whether an approval is current depends on the receipt it is joined against, and
   discarding the older ones here would decide that question in the wrong place —
   before the join, and without the receipt in hand.

   Concretely: approve output A, re-translate to B, then re-translate to
   something byte-identical to A. Keeping only the newest approval would have
   discarded the one that legitimately matches."
  [approvals]
  (reduce (fn [index approval]
            (let [checked (law/assert-approval! approval)]
              (update index
                      (evidence-key (:review/document checked)
                                    (:review/garden checked)
                                    (:review/locale checked)
                                    (:review/revision checked))
                      (fnil conj [])
                      checked)))
          {}
          approvals))

(defn evidence
  "The index a runtime loads once and then reads repeatedly.

   Order-insensitive: `index-receipts` resolves a re-translated source revision
   by comparing timestamps, so a store is free to return receipts in whatever
   order its query produced."
  [{:keys [receipts approvals]}]
  {:receipts (index-receipts receipts)
   :approvals (index-approvals approvals)})

(defn translated-revision?
  "Whether a completed translation exists for this document, target locale and
   concrete source revision, with target bytes bound by digest.

   Receipt schemas retain pre-binding history, but those rows must derive new
   work rather than permanently satisfying the gate with bytes no current
   runtime can authenticate."
  [evidence-index document garden locale revision]
  (law/content-bound?
   (get-in evidence-index [:receipts (evidence-key document garden locale revision)])))

(defn receipt-for
  "The completed translation receipt for these four coordinates, or nil.

   Distinct from `translated-revision?` on purpose: approval evidence has to
   join against the receipt's *output* revision, not merely learn that one
   exists."
  [evidence-index document garden locale revision]
  (get-in evidence-index [:receipts (evidence-key document garden locale revision)]))

(defn approval-for
  "The *current* approval for this document, garden, target locale and concrete
   source revision, or nil.

   Both halves are required. Without a receipt there is nothing to approve, so an
   approval alone is never enough; and an approval that does not name the
   receipt's current output revision has been superseded by a re-translation and
   stops counting. Neither case is an error — see
   `law.translation-evidence/approval-current?`.

   This is why `:approved?` cannot be a plain store lookup. The gate can only
   tell it the source revision, and the source revision alone does not identify
   which bytes were reviewed.

   Returning the approval rather than only a boolean lets read projections carry
   its attribution and timestamp without reimplementing this join."
  [evidence-index document garden locale revision]
  (let [entry-key (evidence-key document garden locale revision)
        receipt (get-in evidence-index [:receipts entry-key])]
    (when (some? receipt)
      (some (fn [approval]
              (when (and (law/approval-matches? approval document garden locale revision)
                         (law/approval-current? approval receipt))
                approval))
            (get-in evidence-index [:approvals entry-key])))))

(defn approved?
  "Whether a *current* approval exists for this document, garden, target locale
   and concrete source revision.

   A predicate view over `approval-for`, so the gate and review inventory share
   one receipt-bound approval join."
  [evidence-index document garden locale revision]
  (boolean (approval-for evidence-index document garden locale revision)))

(defn gate-facts
  "The two evidential `facts` entries this namespace owns, closed over loaded
   evidence.

   Returned as a *partial* facts map. The gate also needs
   `:current-source-revision` and `:source-revision-superseded?`, which are
   facts about the source document rather than about translation, and
   `:approved?`, which is the approval card's. Merging a fabricated default for
   any of them here would let a caller forget to supply the real thing and still
   get a gate that answers — which is the failure mode where a publication is
   admitted on evidence nobody produced."
  [evidence-index]
  {:translated-revision? (fn [document garden locale revision]
                           (translated-revision? evidence-index document garden locale revision))
   :approved? (fn [document garden locale revision]
                (approved? evidence-index document garden locale revision))})
