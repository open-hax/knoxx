(ns knoxx.backend.law.publication-reconciler
  "Contracts for the publication reconciler runtime: the trigger that demands a
   reconciliation and the correlation stamped onto every receipt one trigger
   produces.

   Portable (`.cljc`) so any channel — an authorized route, an event listener,
   a scheduled job — validates the same trigger shape before the runtime runs,
   and the runtime never has to trust the channel's decoding. No I/O lives
   here."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(defn nonblank-string?
  "True for a string carrying at least one non-whitespace character. Restated
   locally: `law.publication`'s copy lives in `.cljs` (its bytes predicate has
   no portable spelling) and this contract must stay portable."
  [value]
  (and (string? value) (seq (str/trim value))))

(def TriggerOrigin
  "Every channel a reconciliation demand may arrive by. The origin is fixed by
   the channel that decoded the trigger, never read from caller-supplied data,
   so a request cannot claim its demand arrived by another channel."
  [:enum :route :event :schedule :manual])

(def ReconcileTrigger
  "One reconciliation demand: run plan → effects for one publication and record
   what happened. Closed — a trigger carrying undeclared fields is a decoding
   defect to surface, not extra data to quietly drop."
  [:map {:closed true}
   [:trigger/id :qualified-keyword]
   [:trigger/origin TriggerOrigin]
   [:publication/id :qualified-keyword]])

(def ReceiptCorrelation
  "The trace every emitted receipt carries: which trigger demanded the
   reconciliation, for which publication, at which concrete revision. The
   revision is maybe — a blocked plan with an unresolvable selector has none,
   and recording that honestly beats inventing one."
  [:map {:closed true}
   [:correlation/trigger :qualified-keyword]
   [:correlation/origin TriggerOrigin]
   [:correlation/publication :qualified-keyword]
   [:correlation/revision [:maybe [:fn nonblank-string?]]]])

(defn- assert-valid!
  [contract-id schema value]
  (if (m/validate schema value)
    value
    (throw
     (ex-info (str "Publication reconciler contract violation: " contract-id)
              {:contract contract-id
               :errors (me/humanize (m/explain schema value))}))))

(defn assert-trigger!
  "Return `trigger` when it is a lawful reconciliation demand; otherwise throw
   before any resource is loaded or any effect is attempted."
  [trigger]
  (assert-valid! :publication/reconcile-trigger ReconcileTrigger trigger))

(defn assert-correlation!
  "Return `correlation` when it satisfies the receipt-correlation contract;
   otherwise throw."
  [correlation]
  (assert-valid! :publication/receipt-correlation ReceiptCorrelation correlation))

(defn correlation
  "Build the validated correlation for `trigger` at `concrete-revision` (nil
   when the plan never resolved one)."
  [trigger concrete-revision]
  (assert-correlation! {:correlation/trigger (:trigger/id trigger)
                        :correlation/origin (:trigger/origin trigger)
                        :correlation/publication (:publication/id trigger)
                        :correlation/revision concrete-revision}))

(defn correlate
  "Return `receipt` carrying `correlation`. Receipt law maps are open, so the
   correlation survives receipt validation; it is asserted here so the runtime
   can never emit an untraceable receipt."
  [receipt correlation]
  (merge receipt (assert-correlation! correlation)))
