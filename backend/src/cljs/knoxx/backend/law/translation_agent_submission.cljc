(ns knoxx.backend.law.translation-agent-submission
  "Contracts for what a translating agent hands back.

  The other half of `law.translation-agent`, split from it along the line the
  work actually has: that namespace declares what a translation session is
  *asked* for, this one declares what may be *accepted* from it. The two are
  read by different callers — the dispatch path never validates a submission,
  and the tool sink never builds an event — and a reviewer checking whether agent
  output can be trusted should not have to read the pinning law to find out.

  A submitted pair is untrusted in exactly the way a worker's status report is,
  and for a sharper reason: the producer is a language model, so the failure to
  guard against is not a stale answer but a confident wrong one. Every refusal
  here therefore compares the submission against the pin the session was started
  with, rather than against anything the submission itself claims.

  Portable by mandate, and restating its primitives for the same portability
  reason `law.translation-agent` gives."
  (:require [clojure.string :as str]
            [knoxx.backend.law.translation-agent :as agent-law]))

(def NonBlankString
  "A string with content. Re-exported so a caller validating a submission does
   not have to require the pinning law to name a primitive."
  agent-law/NonBlankString)

(def SubmittedPair
  "One source/translation pair as `save_translation` received it.

   Open, because the tool's parameter surface is shared with the OpenPlanner
   segment path and carries fields this composition does not read. Closing it
   would make an unrelated addition there a failure here."
  [:map {:closed false}
   [:source_text {:optional true} [:maybe :string]]
   [:translated_text {:optional true} [:maybe :string]]
   [:source_lang {:optional true} [:maybe :string]]
   [:target_lang {:optional true} [:maybe :string]]
   [:document_id {:optional true} [:maybe :string]]
   [:garden_id {:optional true} [:maybe :string]]
   [:org_id {:optional true} [:maybe :string]]
   [:segment_index {:optional true} :any]])

(def pair-refusal-types
  "Every reason a submitted pair may not become translation content.

   Enumerated as data for the reason `law.translation-dispatch/refusal-types`
   gives: a caller classifies by lookup rather than by parsing a message, and a
   new refusal cannot be introduced without appearing here."
  #{:pair-document-mismatch
    :pair-garden-mismatch
    :pair-locale-mismatch
    :pair-source-locale-mismatch
    :pair-org-mismatch
    :pair-translation-missing
    :pair-translation-untranslated
    :pair-segmented-document})

(def PairRefusal
  "A typed refusal carrying both sides, so a caller can see which was stale."
  [:map
   [:refusal/type (into [:enum] (sort pair-refusal-types))]
   [:refusal/expected {:optional true} :any]
   [:refusal/actual {:optional true} :any]])

(defn- text
  [value]
  (str/trim (str (or value ""))))

(defn- prose-like?
  "Whether a source string is long enough that an identical translation is
   evidence of no translation rather than of a shared token.

   Restated from `infra.openplanner.tools/assert-translated!` with the same
   thresholds, deliberately: the two boundaries must agree about what counts as
   untranslated, and a document whose bytes the site will serve is exactly where
   disagreement would be worst."
  [source]
  (or (> (count source) 24) (str/includes? source " ")))

(defn- identity-refusal
  "Refusal because the pair and the pinned binding are not about the same thing.

   Each field is compared only when the submission actually carried it. The tool
   defaults every one of them from the session's own resource policies, so an
   absent field means the agent accepted the pin rather than that it disagreed —
   refusing absence would refuse the ordinary case."
  [policies pair]
  (let [mismatch (fn [key policy-key type]
                   (let [submitted (text (get pair key))
                         pinned (text (get policies policy-key))]
                     (when (and (seq submitted) (not= submitted pinned))
                       {:refusal/type type
                        :refusal/expected pinned
                        :refusal/actual submitted})))]
    (or (mismatch :document_id :document_id :pair-document-mismatch)
        (mismatch :garden_id :garden_id :pair-garden-mismatch)
        (mismatch :target_lang :target_lang :pair-locale-mismatch)
        (mismatch :source_lang :source_lang :pair-source-locale-mismatch)
        (mismatch :org_id :org_id :pair-org-mismatch))))

(defn- content-refusal
  "Refusal because of what the pair contains rather than what it claims to be.

   The segmentation refusal is a deliberate narrowing, not an oversight. A
   contract-backed document's unit of content is a file, and the digest the
   whole publication chain keys on — `infra.publication-source-revision`'s
   `content-revision` — is a digest of that file. Accepting numbered segments
   here would require reassembling them into one file, and concatenation order
   is not guaranteed to reproduce it: that is the open design question already
   recorded on `knoxx-translation-work-dispatch`, and inventing an answer to it
   inside a tool handler is how a wrong answer becomes load-bearing.

   So a segmented submission is refused with a message that tells the agent what
   to do instead. The OpenPlanner segment path is untouched and still segments."
  [pair]
  (let [source (text (:source_text pair))
        translated (text (:translated_text pair))
        index (:segment_index pair)]
    (cond
      (empty? translated)
      {:refusal/type :pair-translation-missing}

      (and (seq source)
           (prose-like? source)
           (= source translated))
      {:refusal/type :pair-translation-untranslated
       :refusal/actual (subs translated 0 (min 80 (count translated)))}

      (and (some? index)
           (not= 0 index)
           (not= "0" (str index)))
      {:refusal/type :pair-segmented-document
       :refusal/expected 0
       :refusal/actual index})))

(defn pair-refusal
  "Why `pair` may not become translation content for `policies`, or nil.

   Data rather than a throw, for the reason `completion-refusal` is: this is an
   untrusted boundary, the caller has to answer the agent either way, and a
   typed refusal is what lets the answer tell the agent how to correct itself.

   Identity before content, so a pair about the wrong document is never
   reported as the right document having bad content."
  [policies pair]
  (or (identity-refusal policies pair)
      (content-refusal pair)))

(def refusal-messages
  "What the agent is told, per refusal type.

   Kept as data beside the types so a refusal cannot be added without deciding
   what the agent should do about it. The tool surface is the only consumer;
   nothing decides anything from these strings."
  {:pair-document-mismatch
   "document_id does not match the document this session was started to translate"
   :pair-garden-mismatch
   "garden_id does not match the garden this session was started to translate for"
   :pair-locale-mismatch
   "target_lang does not match the locale this session was started to translate into"
   :pair-source-locale-mismatch
   "source_lang does not match the source locale of the pinned document"
   :pair-org-mismatch
   "org_id does not match the organization this session was started under"
   :pair-translation-missing
   "translated_text is required and must not be blank"
   :pair-translation-untranslated
   "translated_text matches source_text; provide an actual translation"
   :pair-segmented-document
   (str "this document is published as a whole file, so submit the complete"
        " localized document in one save_translation call with segment_index 0")})

(defn refusal-message
  "The sentence a refusal becomes at the tool boundary."
  [refusal]
  (or (get refusal-messages (:refusal/type refusal))
      (str "the submitted translation was refused: " (name (:refusal/type refusal)))))

;; ── Handing a finished run back to the dispatch completion path ─────────────

(def CompletionReport
  "The status report a finished agent run becomes.

   Deliberately shaped as `law.translation-dispatch/BatchStatusReport` rather
   than as something new. That contract's completion path already does the four
   things this composition must not reimplement: it joins the report to the
   claim that knows the revision, refuses a stale or mismatched answer, verifies
   the source has not drifted since dispatch, and mints the receipt from the
   *record* rather than from the answer. A parallel agent-shaped completion path
   would have been a second place for those four rules to live, and the first
   time they diverged the weaker one would be the one that published."
  [:map {:closed true}
   [:status [:= "complete"]]
   [:batch_id NonBlankString]
   [:completed_document NonBlankString]])

(defn completion-report
  "The report that resolves `run-id`'s claim for one document.

   `completed_document` is the document *wire* id, matching what
   `store/dispatch-for-batch-document!` indexes claims by — the qualified
   contract keyword would find nothing and be refused as
   `:dispatch-record-missing`, which reads like lost evidence rather than like
   the caller using the wrong id."
  [run-id document-wire-id]
  (agent-law/assert-valid!
   :translation-agent/completion-report
   CompletionReport
   {:status "complete"
    :batch_id run-id
    :completed_document document-wire-id}))
