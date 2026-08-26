(ns knoxx.backend.domain.translation-evidence
  "The gate's translation facts, assembled from already-loaded evidence.

  `domain.publication-gate` takes a `facts` map of *synchronous* predicates:

    :translated-revision?  [document locale revision] -> boolean
    :approved?             [document locale revision] -> boolean

  That signature is not incidental, and it is the reason this namespace exists
  as its own step. The gate computes evidence exactly once so a revision
  selector cannot resolve differently between the decision to publish and the
  artifact materialized. Predicates that each went and awaited a store would
  reintroduce precisely the drift that rule forbids. So a runtime loads evidence
  first, and then builds predicates that only read it.

  Pure by mandate — no store, no clock, no I/O — which is why this is `.cljc`
  while the store feeding it is not. It is the first portable `domain.*`
  namespace in this backend; its requires are laws only, so it loads anywhere.

  This card supplies the *translation* half. `:approved?` is
  `knoxx-translation-approval-surface`, and `gate-facts` deliberately returns a
  partial map rather than defaulting the half it does not own — see its
  docstring."
  (:require [knoxx.backend.law.translation-evidence :as law]))

(defn evidence-key
  "The four coordinates every lookup is keyed by.

   A vector rather than nested maps: the three coordinates always travel
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

(defn evidence
  "The index a runtime loads once and then reads repeatedly.

   Order-insensitive: `index-receipts` resolves a re-translated source revision
   by comparing timestamps, so a store is free to return receipts in whatever
   order its query produced."
  [{:keys [receipts]}]
  {:receipts (index-receipts receipts)})

(defn translated-revision?
  "Whether a completed translation exists for this document, target locale and
   concrete source revision."
  [evidence document garden locale revision]
  (some? (get-in evidence [:receipts (evidence-key document garden locale revision)])))

(defn receipt-for
  "The completed translation receipt for this triple, or nil.

   Distinct from `translated-revision?` on purpose: approval evidence has to
   join against the receipt's *output* revision, not merely learn that one
   exists."
  [evidence document garden locale revision]
  (get-in evidence [:receipts (evidence-key document garden locale revision)]))

(defn gate-facts
  "The `facts` entry this card owns, closed over loaded evidence.

   Returned as a *partial* facts map. The gate also needs
   `:current-source-revision` and `:source-revision-superseded?`, which are
   facts about the source document rather than about translation, and
   `:approved?`, which is the approval card's. Merging a fabricated default for
   any of them here would let a caller forget to supply the real thing and still
   get a gate that answers — which is the failure mode where a publication is
   admitted on evidence nobody produced."
  [evidence]
  {:translated-revision? (fn [document garden locale revision]
                           (translated-revision? evidence document garden locale revision))})
