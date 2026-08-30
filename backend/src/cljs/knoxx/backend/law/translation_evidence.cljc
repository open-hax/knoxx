(ns knoxx.backend.law.translation-evidence
  "Contracts for translation facts the publication gate reads.

  `domain.publication-gate` asks two evidential questions — has this document
  been translated into this locale at this concrete revision, and has that
  translation been approved — and until now nothing answered either in
  production. Only test stubs supplied `:translated-revision?` and `:approved?`.
  This namespace declares the shape of the facts that answer the first, and is
  where the second's approval evidence will join it.

  A *completed translation receipt* is an observed execution fact, in the same
  sense `law.publication-receipts` means it: it says what happened, never what
  should happen. No translation receipt is ever desired state, and no
  publication resource ever carries one.

  Portable by mandate: contracts and pure decisions are `.cljc`, while stores,
  workers and clocks stay at the runtime edge. As in
  `law.publication-manifest`, small predicates are restated rather than required
  from a `.cljs` law, so nothing here drags a runtime namespace onto the JVM."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication-locale :as locale]
            [malli.core :as m]
            [malli.error :as me]))

;; ── Restated primitives ────────────────────────────────────────────────────
;;
;; `law.publication` owns these for the resource graph. They are duplicated
;; rather than required because that namespace is `.cljs`: requiring it would
;; make this contract unloadable on the JVM and defeat the portability the
;; mandate asks for. The duplication is small, and a test pins the two
;; definitions to the same behavior.

(defn nonblank-string?
  "True for a string carrying at least one non-whitespace character."
  [value]
  (and (string? value) (boolean (seq (str/trim value)))))

(def NonBlankString
  "A string with content. Restated from `law.publication` for portability."
  [:fn nonblank-string?])

(def revision-selector-namespace
  "The keyword namespace every revision *selector* lives in.

   Named as a namespace rather than as the single member `:source/current`, so a
   sibling selector cannot be introduced without inheriting the refusal below.
   Restated from `law.publication` for portability."
  "source")

(defn revision-selector?
  "True for a revision selector in either shape *this* boundary can receive it:
   the keyword `:source/current`, or that same token spelled as a string.

   The keyword half is already refused by `ConcreteRevision` — a keyword is not
   a string. The string half is not, and that distinction earns its keep here
   specifically: this is the first publication-domain boundary where a revision
   can arrive as decoded *wire input* — a request body, or a worker's JSON
   response — rather than as resource data an operator wrote. A worker replying
   with `\"source/current\"` would otherwise satisfy a nonblank-string revision
   law and record a translation fact against a moving target, which is the one
   outcome the gate's compute-once rule exists to prevent."
  [value]
  (let [selector-namespace (cond
                             (keyword? value) (namespace value)
                             (string? value) (second (re-matches #":?([^/]+)/.+"
                                                                 (str/trim value)))
                             :else nil)]
    (= revision-selector-namespace selector-namespace)))

(def ConcreteRevision
  "One immutable source state, never a selector — in either shape it can arrive.

   `law.publication/ConcreteRevision` is a nonblank string, which already
   refuses the `:source/current` keyword. It does not refuse the *string*
   `\"source/current\"`, because nothing upstream of it ever decoded a revision
   out of untrusted input. This boundary does."
  [:and
   NonBlankString
   [:fn {:error/message "a concrete revision may not be a revision selector"}
    (complement revision-selector?)]])

(def iso-8601-utc-millis-pattern
  "Exactly what `Date.prototype.toISOString` emits.

   Pinned as a pattern rather than accepting any parseable instant, because
   these timestamps are *compared*, and comparison is the whole reason the
   format is constrained. Two instants in one fixed-width UTC format order
   correctly as plain strings; mix in an offset like `+02:00` or drop the
   milliseconds and lexicographic order silently stops agreeing with
   chronological order — at which point a re-translation can lose to the
   translation it replaced."
  #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z")

(defn- leap-year?
  [year]
  (and (zero? (mod year 4))
       (or (not (zero? (mod year 100)))
           (zero? (mod year 400)))))

(defn- days-in-month
  [year month]
  (case month
    2 (if (leap-year? year) 29 28)
    (4 6 9 11) 30
    31))

(defn instant?
  "True for a real ISO-8601 UTC instant with millisecond precision.

   The shape is checked and then so is the calendar. The pattern alone admits
   `\"2026-19-99T99:99:99.999Z\"`, which is not a moment — and because these
   strings are compared lexically to decide which translation is newest, a
   nonsense value like that would sort above every real one and win. February 30
   is refused too: a contract that accepts an impossible date is not describing
   an instant."
  [value]
  (boolean
   (when (and (string? value) (re-matches iso-8601-utc-millis-pattern value))
     (let [year (parse-long (subs value 0 4))
           month (parse-long (subs value 5 7))
           day (parse-long (subs value 8 10))
           hour (parse-long (subs value 11 13))
           minute (parse-long (subs value 14 16))
           second (parse-long (subs value 17 19))]
       (and (<= 1 month 12)
            (<= 1 day (days-in-month year month))
            (<= 0 hour 23)
            (<= 0 minute 59)
            ;; 60 admits a leap second, which a real timestamp source can emit.
            (<= 0 second 60))))))

(def Instant
  "A comparable instant. A string rather than a host date object, because this
   contract is portable and a date is the first thing that stops being."
  [:and :string [:fn {:error/message "an instant must be an ISO-8601 UTC timestamp with milliseconds"}
                 instant?]])

(defn later-instant?
  "True when `a` is strictly later than `b`, with nil earlier than everything.

   String comparison is correct here *because* `Instant` pins one fixed-width
   UTC format; it would not be for arbitrary ISO-8601."
  [a b]
  (cond
    (nil? a) false
    (nil? b) true
    :else (pos? (compare a b))))

(defn- normalized-review-order
  "Make optional review coordinates homogeneously comparable.

   New projections carry `[recorded-at operation-id review-id]`, exactly matching
   effective-review selection. Already-persisted rollout rows may carry the old
   `[recorded-at review-id]` pair; it remains readable and receives an empty
   operation rank. Missing receipts are represented by all nils.

   Normalizing each nil to the empty string avoids relying on a host runtime's
   ordering between nil and strings while preserving the intended rule that a
   real review sorts after a missing one."
  [receipt]
  (mapv (fn [coordinate]
          (let [[recorded-at second third] coordinate
                [operation-id review-id]
                (if (= 3 (count coordinate))
                  [second third]
                  [nil second])]
            [(or recorded-at "")
             (or operation-id "")
             (or review-id "")]))
        (or (:translation/split-review-order receipt) [])))

(defn receipt-order-key
  "One context-free total-order key for current-translation selection.

   The key must be computed from one receipt at a time. A pair-sensitive
   comparator is not transitive when legacy, raw split, and reviewed split
   receipts coexist: A can beat B, B beat C, and C beat A. A single
   lexicographic key makes that cycle impossible.

   Split work is ordered by turn admission rather than review wall time, so a
   delayed review of an older candidate set cannot resurrect it. Within one
   admitted generation the candidate-set id is considered before review state;
   this keeps two same-millisecond generations deterministic. Within one set,
   the manifest-order review vector makes every projection sort after its raw
   receipt and later review state sort after earlier state. Historical receipts
   have neither coordinate and retain their completion-time ordering."
  [receipt]
  (let [candidate-set-id (:translation/candidate-set-id receipt)
        split-backed? (some? candidate-set-id)]
    [(or (:translation/split-turn-admitted-at receipt)
         (:translation/at receipt))
     (if split-backed? 1 0)
     (or candidate-set-id "")
     (normalized-review-order receipt)
     (:translation/at receipt)
     (:translation/revision receipt)
     (or (:translation/content-digest receipt) "")
     (:translation/dispatch-key receipt)]))

(defn supersedes?
  "Whether `candidate` should replace `incumbent` as the current translation.

   Comparison is a strict lexicographic order over `receipt-order-key`. Because
   each key depends only on its own receipt, the relation is transitive and
   independent of store return order. Nil incumbent is superseded by anything,
   so this also covers the empty-index case."
  [candidate incumbent]
  (or (nil? incumbent)
      (pos? (compare (receipt-order-key candidate)
                     (receipt-order-key incumbent)))))

(defn assert-valid!
  "Return `value` when it satisfies `schema`; otherwise throw a named contract
   violation. Mirrors `law.publication/assert-valid!`, restated for portability."
  [contract-id schema value]
  (if (m/validate schema value)
    value
    (throw
     (ex-info (str "Translation evidence contract violation: " contract-id)
              {:contract contract-id
               :errors (me/humanize (m/explain schema value))}))))

;; ── Completed translation ──────────────────────────────────────────────────

(def CompletedTranslationReceipt
  "Evidence that a translation was produced, and from exactly what.

   Only the completed outcome lives in this contract. The unsuccessful outcomes
   — failed, rejected, duplicate — are *dispatch* facts rather than translation
   facts: they say something about an attempt, not about a translation that
   exists, and `law.translation-dispatch` owns them. Keeping them out is what
   lets the gate's `:translated-revision?` be a simple lookup instead of a
   filter that has to know which outcomes count.

   Both revisions are required and they do not mean the same thing.
   `:translation/source-revision` is the concrete revision the translation was
   produced FROM. It is the revision the gate keys evidence by, because that is
   what a publication intent's revision selector resolves to.
   `:translation/revision` identifies the produced output.

   Keeping both is what makes downstream review evidence untransplantable:
   re-running a translation for the same source revision yields a new output
   revision, and an approval pinned only to the source revision would silently
   keep authorizing bytes nobody reviewed.

   `:translation/org-id` is required because the translation itself is
   tenant-scoped: the worker keys segments by organization and every document
   read requires one, so a translation produced for org A does not exist for
   org B. An unscoped receipt would let org B's gate report a document
   translated when the segments live only in org A's tenant."
  [:and
   [:map
    [:receipt/type [:= :translation/completed]]
    [:translation/document :qualified-keyword]
    [:translation/garden :qualified-keyword]
    [:translation/source-locale locale/Locale]
    [:translation/locale locale/Locale]
    [:translation/source-revision ConcreteRevision]
    [:translation/revision ConcreteRevision]
    ;; Optional only so historical receipts remain readable. A candidate must
    ;; carry this digest before current bytes can be shown, approved, or
    ;; published; absence is history, not content authority.
    [:translation/content-digest {:optional true} NonBlankString]
    [:translation/dispatch-key NonBlankString]
    ;; Optional only for receipts written before dispatch attempts had their own
    ;; immutable identity. New receipts carry it so evidence readers can hide a
    ;; receipt until that exact attempt is durably completed.
    [:translation/dispatch-attempt-id {:optional true} NonBlankString]
    [:translation/org-id NonBlankString]
    [:translation/project {:optional true} [:maybe NonBlankString]]
    ;; Optional as a group so receipts written before split-backed translation
    ;; remain readable. A split-backed receipt must carry the complete lineage:
    ;; a partial chain cannot authenticate which candidate set supplied the
    ;; published bytes.
    [:translation/split-manifest-id {:optional true} NonBlankString]
    [:translation/candidate-claim-id {:optional true} NonBlankString]
    [:translation/candidate-set-id {:optional true} NonBlankString]
    [:translation/candidate-set-digest {:optional true} NonBlankString]
    [:translation/split-count {:optional true} [:int {:min 1}]]
    ;; The admission clock orders distinct candidate sets independently of when
    ;; a delayed human review happens to be submitted.
    [:translation/split-turn-admitted-at {:optional true} Instant]
    ;; Deterministic current-review coordinates in manifest order. New writers
    ;; emit [recorded-at operation-id review-id]. The pair alternative preserves
    ;; rollout readability for already-written [recorded-at review-id] rows.
    ;; Present only on derived projections, never the initial raw receipt.
    [:translation/split-review-order
     {:optional true}
     [:vector
      [:or
       [:tuple [:maybe Instant] [:maybe NonBlankString]]
       [:tuple [:maybe Instant] [:maybe NonBlankString]
        [:maybe NonBlankString]]]]]
    [:translation/at Instant]]
   [:fn {:error/message "translation split lineage must be wholly absent or complete"}
    (fn [receipt]
      (let [lineage-keys [:translation/split-manifest-id
                          :translation/candidate-claim-id
                          :translation/candidate-set-id
                          :translation/candidate-set-digest
                          :translation/split-count
                          :translation/split-turn-admitted-at]
            present-count (count (filter #(contains? receipt %) lineage-keys))]
        (or (zero? present-count)
            (= (count lineage-keys) present-count))))]
   [:fn {:error/message "split review order requires complete lineage and one coordinate per split"}
    (fn [receipt]
      (if-let [order (:translation/split-review-order receipt)]
        (and (contains? receipt :translation/candidate-set-id)
             (= (:translation/split-count receipt) (count order)))
        true))]
   [:fn {:error/message "a translation receipt's target locale must differ from its source locale"}
    ;; A receipt claiming a translation from `:en` to `:en` is not evidence of
    ;; anything: `publication-gate/translation-required?` decides translation is
    ;; needed exactly when those two differ, so such a receipt could only ever
    ;; satisfy a gate that never asked for it.
    (fn [receipt]
      (not= (:translation/source-locale receipt) (:translation/locale receipt)))]])

(defn assert-receipt!
  "Validate a completed translation receipt.

   Applied in both directions — before a receipt is persisted, and again when
   one is read back. A store is replaceable, so what it returns is untrusted
   input rather than a promise; this is the rule `law.publication-receipts`
   already applies across the effect boundary."
  [receipt]
  (assert-valid! :translation/receipt CompletedTranslationReceipt receipt))

(defn content-bound?
  "Whether a receipt names the digest of the target bytes it proves.

   The digest is optional in the schema solely so pre-binding ledger rows stay
   readable during rollout. Such rows are history, not current translation
   evidence: they cannot authenticate content, be reviewed, or authorize a
   publication."
  [receipt]
  (nonblank-string? (:translation/content-digest receipt)))

;; ── Approval ───────────────────────────────────────────────────────────────

(def Principal
  "Who approved, as at least one durable identity.

   Attribution that cannot be resolved back to a person is not attribution. A
   principal carrying only blank strings would satisfy a shape made of optional
   keys, so the presence rule is a law rather than a convention."
  [:and
   [:map
    [:principal/user-id {:optional true} [:maybe :string]]
    [:principal/user-email {:optional true} [:maybe :string]]
    [:principal/membership-id {:optional true} [:maybe :string]]]
   [:fn {:error/message "an approval principal requires at least one durable identity"}
    (fn [principal]
      (boolean (some nonblank-string?
                     ((juxt :principal/user-id
                            :principal/user-email
                            :principal/membership-id)
                      principal))))]])

(def ApprovalRequest
  "What a caller may submit.

   No principal and no timestamp: both are attributed by the server from the
   authenticated context and the clock, so a caller can neither claim to be
   someone else nor backdate review evidence. No organization or project either —
   those come from the receipt being approved, which is already scoped.

   Closed, and that is load-bearing rather than tidy: an extra key on an approval
   request is precisely the shape a caller would use to try to smuggle
   `:review/principal` or `:review/at` past attribution."
  [:map {:closed true}
   [:review/document :qualified-keyword]
   [:review/garden :qualified-keyword]
   [:review/locale locale/Locale]
   [:review/revision ConcreteRevision]
   [:review/translation-revision ConcreteRevision]])

(def Approval
  "Immutable review evidence, in the vocabulary the gate already speaks.

   `domain.publication-gate/review-satisfies-intent?` compares `:review/state`,
   `:review/document`, `:review/locale` and `:review/revision`. Those four are
   named identically here on purpose: the gate is not modified to accommodate
   approval evidence, approval evidence is declared in the words the gate
   already uses.

   `:review/revision` is the concrete *source* revision, not the translated one.
   That is what a publication intent resolves to and therefore what the gate can
   ask about; the card's phrase 'concrete translated revision' reads the other
   way and taken literally would produce approvals the gate could never match.
   `:review/translation-revision` carries the produced output alongside, and is
   the field the gate does not read — see `approval-current?`.

   `:review/org-id` and `:review/project` are required for the same reason
   `CompletedTranslationReceipt` carries them: the translation an approval
   attests to exists in one tenant and one project, so review evidence that
   named neither would be admissible everywhere. That lesson was learned the hard
   way one card earlier.

   `:review/garden` is required for the third instance of that same lesson. The
   worker builds its prompt with `garden_id`, so the same document translated
   into the same locale for two gardens is two different outputs — and a
   garden-agnostic approval would let a reviewer who read one garden's bytes
   admit publication of the other's. It is not carried as scope-that-happens-to-
   travel: it is a coordinate of the receipt being approved, and it is taken from
   that receipt rather than from the request."
  [:map
   [:review/state [:= :approved]]
   [:review/document :qualified-keyword]
   [:review/garden :qualified-keyword]
   ;; Optional only so pre-migration approvals remain readable as history.
   ;; `approval-current?` deliberately refuses one that lacks this coordinate.
   [:review/source-locale {:optional true} locale/Locale]
   [:review/locale locale/Locale]
   [:review/revision ConcreteRevision]
   [:review/translation-revision ConcreteRevision]
   ;; Optional only so pre-migration approvals remain readable as history.
   ;; `approval-current?` requires it before authorizing any current bytes.
   [:review/content-digest {:optional true} NonBlankString]
   [:review/org-id NonBlankString]
   [:review/project {:optional true} [:maybe NonBlankString]]
   [:review/principal Principal]
   [:review/at Instant]])

(def approval-refusal-types
  "Every reason an approval request is refused.

   Enumerated as data so an HTTP adapter maps refusal to status by lookup rather
   than by re-deriving the classification from a message string, and so a new
   refusal cannot be introduced without appearing here."
  #{:translation-receipt-missing
    :translation-content-unbound
    :translation-document-mismatch
    :translation-garden-mismatch
    :translation-locale-mismatch
    :translation-source-revision-mismatch
    :translation-revision-mismatch})

(def ApprovalRefusal
  "A typed refusal. Both sides of a mismatch travel on it: told only that a
   revision disagreed, a caller cannot see whether its own request or the
   recorded translation was the stale one."
  [:map
   [:refusal/type (into [:enum] (sort approval-refusal-types))]
   [:refusal/requested {:optional true} :any]
   [:refusal/recorded {:optional true} :any]])

(defn assert-approval-request!
  "Validate a decoded approval request before any lookup or write."
  [request]
  (assert-valid! :translation-review/request ApprovalRequest request))

(defn assert-approval!
  "Validate approval evidence before it is persisted or returned, and again when
   it is read back — a store is replaceable, so its output is untrusted input."
  [approval]
  (assert-valid! :translation-review/approval Approval approval))

(def ^:private approval-coordinate-checks
  "Ordered receipt/request comparisons for whole-output approval.

   Garden remains explicit even though the facade normally looks the receipt up
   by garden: this law is a reusable boundary, and approving a garden mismatch
   would admit bytes nobody read. Output revision remains last so a more useful
   coordinate mismatch wins when more than one field disagrees."
  [[:translation/document :review/document :translation-document-mismatch]
   [:translation/garden :review/garden :translation-garden-mismatch]
   [:translation/locale :review/locale :translation-locale-mismatch]
   [:translation/source-revision :review/revision
    :translation-source-revision-mismatch]
   [:translation/revision :review/translation-revision
    :translation-revision-mismatch]])

(defn- approval-coordinate-refusal
  [request receipt]
  (some (fn [[receipt-key request-key refusal-type]]
          (let [requested (get request request-key)
                recorded (get receipt receipt-key)]
            (when (not= recorded requested)
              {:refusal/type refusal-type
               :refusal/requested requested
               :refusal/recorded recorded})))
        approval-coordinate-checks))

(defn approval-refusal
  "Why `request` may not be approved against `receipt`, or nil when it may.

   Data rather than a throw. The caller is an HTTP adapter that must answer with
   a status and a body either way, and a refusal here is an ordinary outcome of a
   well-formed request rather than an exceptional one.

   A nil receipt is the first branch because every comparison below would
   otherwise read fields off nothing and report a mismatch against nil, which
   describes the wrong problem: there is no translation to approve, rather than a
   translation that disagrees."
  [request receipt]
  (cond
    (nil? receipt)
    {:refusal/type :translation-receipt-missing
     :refusal/requested (select-keys request [:review/document
                                              :review/garden
                                              :review/locale
                                              :review/revision])}

    (not (content-bound? receipt))
    {:refusal/type :translation-content-unbound
     :refusal/recorded (:translation/revision receipt)}

    :else
    (approval-coordinate-refusal request receipt)))

(defn approve
  "Build immutable approval evidence from a validated request, the completed
   receipt it is recorded against, the acting principal, and a clock reading.

   Pure: `at` is supplied rather than read. Every identity field is taken from
   the *receipt* rather than from the request — including the tenant and project,
   which the request never carries — so the persisted evidence describes what the
   store actually holds. `approval-refusal` has already established that the two
   agree on everything the request did name."
  [_request receipt principal at]
  (assert-approval!
   (cond-> {:review/state :approved
            :review/document (:translation/document receipt)
            :review/garden (:translation/garden receipt)
            :review/source-locale (:translation/source-locale receipt)
            :review/locale (:translation/locale receipt)
            :review/revision (:translation/source-revision receipt)
            :review/translation-revision (:translation/revision receipt)
            :review/org-id (:translation/org-id receipt)
            :review/principal principal
            :review/at at}
     (some? (:translation/content-digest receipt))
     (assoc :review/content-digest (:translation/content-digest receipt))

     (some? (:translation/project receipt))
     (assoc :review/project (:translation/project receipt)))))

(defn approval-matches?
  "Whether `approval` is evidence about the document, garden, locale and
   concrete source revision the gate is asking about.

   The gate's own question restated as a predicate over one approval, so a fact
   provider need not re-derive it. It deliberately does NOT consider
   `:review/translation-revision` — see `approval-current?`."
  [approval document garden locale revision]
  (and (= :approved (:review/state approval))
       (= document (:review/document approval))
       (= garden (:review/garden approval))
       (= locale (:review/locale approval))
       (= revision (:review/revision approval))
       true))

(defn approval-current?
  "Whether `approval` still describes the translation output `receipt` holds.

   The gate asks `approved?` with a document, a locale and a concrete *source*
   revision, which is all a publication intent can tell it. That triple is not
   enough on its own: re-running a translation for the same source revision
   produces new bytes under a new output revision, and an approval matching only
   the triple would keep authorizing them.

   So the fact provider joins the two facts, and this is the join condition. An
   approval whose `:review/translation-revision` no longer matches the receipt's
   `:translation/revision` is not deleted and not an error — it simply stops
   being current, exactly as `domain.publication-gate` says a stale
   translation's approval stops satisfying the new revision."
  [approval receipt]
  (and (some? receipt)
       ;; Historical approvals did not carry source locale. They remain valid
       ;; stored history, but cannot authorize current bytes: equal source and
       ;; output revisions across a later source-locale change are possible,
       ;; especially for authored content, and absence is not proof of equality.
       (some? (:review/source-locale approval))
       (= (:review/source-locale approval)
          (:translation/source-locale receipt))
       (some? (:review/content-digest approval))
       (= (:review/content-digest approval)
          (:translation/content-digest receipt))
       (= (:review/translation-revision approval)
          (:translation/revision receipt))
       true))
