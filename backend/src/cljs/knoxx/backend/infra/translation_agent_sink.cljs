(ns knoxx.backend.infra.translation-agent-sink
  "The contract-backed half of `save_translation`.

  `save_translation` has always had one sink: an OpenPlanner translation
  segment. That is the right sink for a CMS document whose truth lives in
  OpenPlanner, and it is the wrong one for a publication document declared by a
  deployment contract — writing there would make the transport this cutover
  removes a hard dependency of the workflow replacing it.

  So the tool gains a second sink, chosen by the session's own resource
  policies rather than by configuration. A session started by the publication
  translation trigger carries `dispatch_key` and `run_id`; nothing else does.
  That makes the discriminator a property of *why this session exists*, which is
  the only thing that could correctly decide it — a config flag would have made
  one deployment able to send CMS translations into publication evidence.

  ## Nothing about translation evidence is decided here

  This namespace joins, validates, writes bytes, and then hands the run to
  `infra.translation-dispatch/resolve-batch-report!` — the same completion path
  the OpenPlanner worker's status callback goes through. The drift guard, the
  receipt minting, the claim settlement and the refusal law all stay there,
  unduplicated. That is deliberate and it is the whole shape of the composition:
  an agent actor is a different *producer* of translations, not a different
  *kind of evidence*.

  The one thing that happens before the handoff is the content write, and the
  order matters in the same way `record-completion!`'s does. Bytes are written
  first, so a crash between the two leaves content with no receipt — reconciled
  as 'not translated yet'. An equal retry reuses that immutable orphan; changed
  bytes conflict instead of replacing content behind the same output revision.
  The reverse order would leave a receipt the gate believes, pointing at bytes
  that were never stored, and the reconciler would then publish the authored
  fallback under an approval granted for agent output."
  (:require [knoxx.backend.infra.translation-agent-content :as content]
            [knoxx.backend.infra.translation-content-integrity :as content-integrity]
            [knoxx.backend.infra.translation-dispatch :as dispatch]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-agent-submission :as submission-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]))

(defn- claim-refusal
  "Why the claim found for `run-id` may not accept this submission, or nil.

   The dispatch key is rechecked even though the run id already found the
   record. The run id is derived from the key, so agreement is expected — which
   is exactly why disagreement means something has gone wrong that no other
   check would catch: a store returning a record it was not asked for, or a
   trigger that built its overlay from one claim and its session id from
   another. Cheap, and it fails at the boundary instead of minting a receipt
   against the wrong revision."
  [record policies]
  (cond
    (nil? record)
    {:refusal/type :dispatch-record-missing
     :refusal/actual {:run_id (:run_id policies)}}

    (not= (:dispatch/key record) (:dispatch_key policies))
    {:refusal/type :worker-batch-mismatch
     :refusal/expected (:dispatch/key record)
     :refusal/actual (:dispatch_key policies)}

    (not= :dispatch/accepted (:dispatch/outcome record))
    {:refusal/type :dispatch-already-resolved
     :refusal/expected :dispatch/accepted
     :refusal/actual (:dispatch/outcome record)}))

(defn ^:async submit-pair!
  "Record one agent-submitted translation pair as publication evidence.

   Returns `{:translation/receipt r}` on success, or `{:translation/refusal f}`
   with a typed refusal the caller turns into the agent's error message. Never
   throws for an ordinary bad submission: this is an untrusted boundary whose
   caller is a language model, and a refusal it can read and correct is worth
   more than a stack trace.

   `deps` needs `:content-root`, `:evidence-store`, `:clock` and
   `:observe-source-revision`. The last is not defaulted, for the reason
   `resolve-batch-report!` gives about its own copy: a caller who forgot to
   supply the drift observer must fail, not quietly receive a version with the
   only check that proves what was translated missing."
  [{:keys [content-root evidence-store] :as deps} policies pair]
  (if-let [pair-refusal (submission-law/pair-refusal policies pair)]
    {:translation/refusal pair-refusal}
    (let [record (await (store/dispatch-for-batch! evidence-store
                                                   (:run_id policies)))]
      (if-let [refusal (claim-refusal record policies)]
        {:translation/refusal refusal}
        (let [output-revision (dispatch-law/output-revision record)]
          (await (content/write! content-root
                                 record
                                 output-revision
                                 (:translated_text pair)))
          (await (dispatch/resolve-batch-report!
                  (assoc deps :translation-content-digest
                         (content-integrity/content-digest
                          (:translated_text pair)))
                  (submission-law/completion-report
                   (:run_id policies)
                   (:dispatch/document-wire-id record)))))))))

(defn refusal-error
  "The error a refused submission becomes at the tool boundary.

   An `ex-info` rather than a bare `js/Error`, so the refusal type survives for
   a route or a test that wants to classify it, while the message is the
   sentence the agent reads. `law.translation-agent-submission/refusal-message` owns the
   wording for its own refusals; a dispatch-layer refusal reaching here is a
   misconfiguration rather than something an agent can fix, and says so."
  [refusal]
  (let [type (:refusal/type refusal)]
    (ex-info (if (contains? submission-law/pair-refusal-types type)
               (submission-law/refusal-message refusal)
               (str "this translation session is not bound to a live publication"
                    " dispatch claim (" (name type) "); it cannot record"
                    " translation evidence"))
             refusal)))
