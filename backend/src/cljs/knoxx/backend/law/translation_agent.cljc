(ns knoxx.backend.law.translation-agent
  "Contracts for translating a publication document with an agent actor.

  There is no new translation model here, and that is the point. The publication
  gate already derives work, `law.translation-dispatch` already owns dispatch
  identity and mints receipts, `law.translation-evidence` already owns the facts
  the gate reads, and the event/trigger/action runtime already knows how to run
  an agent session against an actor context. What was missing was the seam that
  lets *that* runtime be the thing which produces a translation, instead of an
  HTTP worker the deployment does not contain.

  So this namespace declares only what a translation session is *asked* for:

  - the run identity one dispatch attempt carries, so the answer can be joined
    back to the claim that knows its revision;
  - the resource-policy overlay that pins one agent session to one derived work
    item, so the session cannot translate a document nobody asked about;
  - the event a trigger observes, carrying that overlay and the source bytes.

  What may be *accepted back* from such a session is
  `law.translation-agent-submission`. The two are split because they are read by
  different callers: the dispatch path never validates a submission, and the tool
  sink never builds an event.

  `law.translation-dispatch/output-revision` needs a producing-run identifier
  whose only required property is that it *changes when the work is redone* —
  that is what stops an approval of one translation from authorizing its
  replacement. The worker path satisfies that with a batch id; this path mints a
  run id per dispatch attempt and carries it in the session's resource policies.
  See `run-id` for why it is not the runtime's own session id.

  Portable by mandate: this is contract and pure decision, so it is `.cljc`.
  Small predicates are restated rather than required from a `.cljs` law for the
  reason `law.translation-evidence` gives — requiring one would make this
  unloadable on the JVM and defeat the portability the mandate asks for."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication-locale :as locale]
            [knoxx.backend.law.translation-split-schema :as split-schema]
            [malli.core :as m]
            [malli.error :as me]))

;; ── Restated primitives ────────────────────────────────────────────────────

(defn nonblank-string?
  "True for a string carrying at least one non-whitespace character."
  [value]
  (and (string? value) (boolean (seq (str/trim value)))))

(def NonBlankString
  "A string with content. Restated for portability; see the ns docstring."
  [:fn nonblank-string?])

(defn assert-valid!
  "Validate `value` against `schema`, or throw with a humanized explanation."
  [what schema value]
  (if (m/validate schema value)
    value
    (throw (ex-info (str "invalid " (name what))
                    {:what what
                     :errors (-> (m/explain schema value) me/humanize)}))))

;; ── The run identity ────────────────────────────────────────────────────────

(def run-id-prefix
  "Names what a run id is, inside the value itself.

   The same reasoning as `infra.publication-source-revision/digest-prefix`: a
   bare digest is indistinguishable from any other opaque token, and this one
   ends up embedded in an output revision that a human has to be able to explain
   when an approval stops matching."
  "translation-run-")

(defn run-id
  "The correlation value one dispatch attempt's agent run carries.

   Minted by Knoxx *before* the session starts, which is what closes a race the
   obvious design has. `law.translation-dispatch/output-revision` requires the
   producing run id to already be bound to the claim, so if the id could only be
   learned from the runtime's answer, an agent that submitted a pair quickly
   would arrive before the binding and be refused as `:dispatch-record-missing`
   — losing work that had actually been done.

   Deliberately *not* the runtime's own session id. `domain.action.start-agent-session`
   derives session and conversation ids from the trigger and the event, and
   taking that over would couple this composition to identifiers whose format is
   the runtime's business. The run id travels in the session's resource policies
   instead, so the two identity schemes stay independent and the claim is joined
   by the value Knoxx bound it to.

   Derived from the dispatch key *and the immutable attempt id*, not from the key
   alone. Historical records without an attempt id fall back to the claim instant.
   The key is deliberately stable across attempts — that is what makes duplicate
   dispatch a duplicate — so a run id derived from it would be identical on
   re-translation, the output revision would not change, and an approval of the
   first translation would silently authorize the second. That is the exact
   failure `output-revision` exists to prevent, so the discriminator has to be
   something a *replaced* claim changes. `reserve-dispatch!` replaces a retriable
   claim wholesale with a fresh `:dispatch/attempt-id`, even when two attempts
   are admitted in the same millisecond.

   Digested rather than concatenated because a dispatch key contains `pr-str`ed
   keywords and pipes, while this value ends up inside an output revision that
   `infra.translation-agent-content` turns into a filename and a human has to be
   able to read back."
  [record digest-hex]
  (let [key (:dispatch/key record)
        attempt (or (:dispatch/attempt-id record) (:dispatch/at record))]
    (when-not (m/validate NonBlankString key)
      (throw (ex-info "a translation run id requires a dispatch key" {:record record})))
    (when-not (m/validate NonBlankString attempt)
      (throw (ex-info "a translation run id requires attempt identity"
                      {:dispatch/key key})))
    (assert-valid! :translation-agent/run-id
                   NonBlankString
                   (str run-id-prefix (digest-hex (str key "@" attempt))))))

;; ── Pinning one session to one derived work item ────────────────────────────

(def SessionPolicies
  "The resource-policy overlay a trigger applies to a translation session.

   Every key here already has a reader: `infra.openplanner.tools`'
   `save-translation-segment` falls back to `:resourcePolicies` for exactly
   `document_id`, `garden_id`, `source_lang`, `target_lang` and `project`. That
   is why the pinning mechanism is this map and not a new one — the tool has
   read agent-resource policy since before this composition existed, and
   `infra.agent.turn` already turns an agent spec's `:resource-policies` into
   the `:resourcePolicies` the tool sees.

   `:dispatch_key` and `:run_id` are the two additions, and they are what makes
   a submission joinable. Without them the sink would have to guess which claim
   a pair belongs to from the document and locale alone, which is precisely the
   correlation gap `knoxx-translation-work-dispatch` recorded as its known
   operational hole. Here the trigger knows the claim it is running for, so the
   binding is carried rather than inferred."
  [:map {:closed false}
   [:document_id NonBlankString]
   [:garden_id NonBlankString]
   [:source_lang NonBlankString]
   [:target_lang NonBlankString]
   [:org_id NonBlankString]
   [:dispatch_key NonBlankString]
   [:run_id NonBlankString]
   [:translation_turn_id NonBlankString]
   [:split_manifest_id NonBlankString]
   [:candidate_claim_id NonBlankString]
   [:execution_digest NonBlankString]
   [:project {:optional true} [:maybe NonBlankString]]])

(defn contract-backed?
  "Whether these session policies pin the session to a publication claim.

   This is the discriminator `save_translation` chooses its sink by, and it is
   deliberately a property of *why the session exists* rather than of
   configuration. A config flag would have made one deployment able to send
   ordinary CMS translations into publication evidence, and the tool has no way
   to tell those apart other than by the pin its session was started with.

   Both keys, not either. A `run_id` without a `dispatch_key` cannot be checked
   against the claim it finds, and a `dispatch_key` without a `run_id` cannot
   find one — so a half-populated overlay is a misconfigured trigger, and
   quietly treating it as a CMS translation would send publication content into
   the transport this composition exists to leave."
  [policies]
  (boolean (and (nonblank-string? (:dispatch_key policies))
                (nonblank-string? (:run_id policies)))))

(defn split-backed?
  "Whether a contract-backed session also names its atomic split authority."
  [policies]
  (boolean (and (contract-backed? policies)
                (nonblank-string? (:translation_turn_id policies))
                (nonblank-string? (:split_manifest_id policies))
                (nonblank-string? (:candidate_claim_id policies))
                (nonblank-string? (:execution_digest policies)))))

(defn- wire-id
  "A qualified keyword spelled the way the wire spells it: `namespace/name`.

   The inverse of `law.translation-dispatch/wire-resource-id`, which is what put
   the garden on the record as a keyword in the first place. Needed because
   `save_translation`'s `garden_id` is a string — the same string
   `:dispatch/document-wire-id` already holds for the document — so an overlay
   carrying the raw keyword would be compared against a string and mismatch on
   every submission.

   Restated rather than required from `shape.resource-identity/encode-keyword`
   because that namespace is `.cljs`: requiring it would make this contract
   unloadable on the JVM, which is the portability the mandate asks for."
  [value]
  (when (qualified-keyword? value)
    (str (namespace value) "/" (name value))))

(defn session-policies
  "The policy overlay pinning a session to `record`'s claim.

   Derived from the dispatch record rather than from the publication intent,
   because the record is the value that already survived
   `law.translation-dispatch/assert-record!` — it carries the concrete revision,
   the tenant, the garden and the document wire id as one validated whole. Built
   from the intent instead, a caller could pin a session to a locale pair no
   claim was ever taken for, and the sink's join would then find nothing while
   the agent had already done the work.

   The run id is supplied rather than read off the record, because at the moment
   a session is started the claim is not yet bound to it — binding is what the
   caller does with the value this map carries."
  [record turn]
  (let [run-id (:translation-turn/run-id turn)
        manifest (:translation-turn/manifest turn)
        claim (:translation-turn/candidate-claim turn)
        execution (:translation-turn/execution turn)]
    (assert-valid!
     :translation-agent/session-policies
     SessionPolicies
     (cond-> {:document_id (:dispatch/document-wire-id record)
              :garden_id (wire-id (:dispatch/garden record))
              :source_lang (name (:dispatch/source-locale record))
              :target_lang (name (:dispatch/locale record))
              :org_id (:dispatch/org-id record)
              :dispatch_key (:dispatch/key record)
              :run_id run-id
              :translation_turn_id (:translation-turn/id turn)
              :split_manifest_id (:split-manifest/id manifest)
              :candidate_claim_id (:candidate-claim/id claim)
              :execution_digest (:translation-execution/digest execution)}
       (some? (:dispatch/project record))
       (assoc :project (:dispatch/project record))))))

;; ── The event a trigger observes ────────────────────────────────────────────

(def max-brief-chars
  "The largest translation brief that may travel on a translation event.

   Bounded rather than unbounded, and refused rather than truncated. A truncated
   source would be translated in full by an agent that had no way to know it was
   reading a fragment, and the receipt minted from the claim would then attest
   that the *whole* dispatched revision had been translated — a false receipt,
   which `law.translation-dispatch` refuses everywhere else it can.

   So an oversize document fails payload validation at emit time. The claim
   becomes a retriable failure carrying the explanation, which is visible to an
   operator whose fix is to split the document; the alternative was a silent
   partial publication.

   Sized against the smallest allowlisted model's context window
   (`contracts/models/gemma4_31b.edn`, 128k tokens) with room for the system
   prompt, the task, and the translation itself in the same turn."
  120000)

(defn- memory-example-brief
  "Render one exact reviewed example without discarding its evidence identity."
  [index example]
  (str "MEMORY EXAMPLE " index "\n"
       "memory_id: " (:translation-memory/id example) "\n"
       "review_receipt_id: " (:translation-memory/review-receipt-id example) "\n"
       "SOURCE MEMORY\n" (:translation-memory/source-text example) "\n"
       "TARGET MEMORY\n" (:translation-memory/target-text example) "\n"
       "END MEMORY EXAMPLE"))

(defn- memory-brief
  "Render the exact memory snapshot pinned before provider execution."
  [memory]
  (let [status (:translation-memory-snapshot/status memory)
        examples (:translation-memory-snapshot/examples memory)]
    (str "Translation memory status: " (name status) ".\n"
         (when (= :failed status)
           (str "Memory retrieval failed before this turn: "
                (:translation-memory-snapshot/error memory) "\n"))
         (when (seq examples)
           (str "Use these reviewed examples as terminology guidance only.\n\n"
                (str/join "\n\n"
                          (map-indexed memory-example-brief examples)))))))

(defn- split-member-brief
  "Render one server-issued source member and the exact identifiers to echo."
  [claim-members split-member]
  (let [index (:split/index split-member)
        claim-member (nth claim-members index)]
    (str "TRANSLATION SPLIT " index "\n"
         "split_id: " (:split/id split-member) "\n"
         "attempt_id: " (:candidate-claim-member/attempt-id claim-member) "\n"
         "segment_index: " index "\n"
         "SOURCE SPLIT\n" (:split/source-text split-member) "\n"
         "END TRANSLATION SPLIT")))

(defn translation-brief
  "The message the translating agent is actually handed.

   Carried in the payload's `:content`, which is the field
   `domain.action.start-agent-session/render-start-message` already renders into
   a triggered session's opening message. Using the existing convention is what
   makes this composition need *no* change to that generic action — the
   alternative was teaching it a translation-shaped payload field, which is
   domain knowledge a generic action must not hold.

   The source document is embedded verbatim rather than referenced by path. That
   is what makes the receipt honest: the agent translates the exact bytes the
   claim was taken for, so `law.translation-dispatch/source-drift-refusal` stops
   being the only available evidence of what was translated and becomes a
   redundant second check. Referenced by path, the agent would read the file
   *now*, which is the worker path's weakness restated.

   Fenced with a long delimiter rather than triple backticks because the source
   is Markdown and very often contains a fenced code block of its own; a
   three-backtick fence would be closed early by the document's own content and
   the tail would read as instructions."
  [turn]
  (let [manifest (:translation-turn/manifest turn)
        claim (:translation-turn/candidate-claim turn)
        source-locale (:split-manifest/source-locale manifest)
        locale (:split-manifest/target-locale manifest)
        splits (:split-manifest/splits manifest)
        claim-members (:candidate-claim/members claim)]
    (str "Translate each server-issued source split below from "
         (name source-locale) " into " (name locale) ".\n\n"
         "Call save_translation exactly once per split. Echo that split's "
         "split_id, attempt_id, and segment_index. Do not invent, merge, or "
         "omit boundaries.\n\n"
         (memory-brief (:translation-turn/memory turn)) "\n\n"
         (str/join "\n\n"
                   (map (partial split-member-brief claim-members) splits)))))

(def event-type
  "The event type a translation trigger subscribes to.

   One type rather than one per locale. `resource-architecture.md` is explicit
   that fan-out belongs to the dispatcher matching many trigger contracts, not
   to an event carrying many kinds — and a per-locale event type would have made
   adding a language a contract change in two places."
  :publication/translation-needed)

(def TranslationNeededEvent
  "The payload that says one document needs one locale at one revision.

   The `:resource-policies` overlay travels *on the event* rather than being
   written into the trigger's static `:trigger/with`, and that is the load-bearing
   decision here. A trigger contract is one EDN value reused for every document
   and every locale; the pin is per-attempt. Written into the contract it could
   only ever name one document, so either every document needed its own trigger
   or the pin had to be inferred downstream from the payload — and inferring it
   would put translation-specific knowledge inside
   `domain.action.start-agent-session`, which is a generic action.

   Carrying it on the event keeps both sides generic: the emitter knows the
   claim, the action forwards an overlay it does not interpret, and the trigger
   states in one field that it accepts one from this event type.

   The four fields beside it are the legible coordinates — everything else is
   inside the overlay, so nothing is stated twice and the two cannot disagree.
   They exist for a trigger predicate that wants to filter by locale, and for a
   human reading the event log.

   Closed. An event is the input to a trigger predicate, and an open payload
   here would let an emitter smuggle a field a predicate then keys on, which is
   how event shape stops being reviewable."
  [:map {:closed true}
   [:event/type [:= event-type]]
   [:event/payload
    [:map {:closed true}
     [:document :qualified-keyword]
     [:source-locale locale/Locale]
     [:locale locale/Locale]
     [:revision NonBlankString]
     [:turn-id NonBlankString]
     [:manifest-id NonBlankString]
     [:candidate-claim-id NonBlankString]
     [:execution-digest NonBlankString]
     [:execution split-schema/TranslationExecutionSnapshot]
     [:split-count [:int {:min 1}]]
     [:content [:and NonBlankString [:string {:max max-brief-chars}]]]
     [:resource-policies SessionPolicies]]]])

(defn translation-needed-event
  "The event `record`'s claim emits when it wants an agent run.

   `run-id` is supplied rather than derived here so that the caller which bound
   it to the claim is the same one that announces it. Deriving it again would be
   a second chance to derive it differently, and the failure would be silent: the
   agent would work, submit, and be refused for a claim nobody could find.

   `source-content` is embedded in the brief rather than left for the agent to
   fetch — see `translation-brief` for why that strengthens the receipt rather
   than merely saving a tool call."
  [record turn]
  (let [checked-turn (split-schema/assert-valid!
                      :translation-agent/turn
                      split-schema/TranslationTurnAdmission turn)
        manifest (:translation-turn/manifest checked-turn)
        claim (:translation-turn/candidate-claim checked-turn)
        execution (:translation-turn/execution checked-turn)
        source-locale (:dispatch/source-locale record)
        locale (:dispatch/locale record)]
    (assert-valid!
     :translation-agent/translation-needed-event
     TranslationNeededEvent
     {:event/type event-type
      :event/payload {:document (:dispatch/document record)
                      :source-locale source-locale
                      :locale locale
                      :revision (:dispatch/revision record)
                      :turn-id (:translation-turn/id checked-turn)
                      :manifest-id (:split-manifest/id manifest)
                      :candidate-claim-id (:candidate-claim/id claim)
                      :execution-digest (:translation-execution/digest execution)
                      :execution execution
                      :split-count (count (:split-manifest/splits manifest))
                      :content (translation-brief checked-turn)
                      :resource-policies (session-policies record checked-turn)}})))
