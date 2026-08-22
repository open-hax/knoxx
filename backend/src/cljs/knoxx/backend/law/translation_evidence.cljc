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

(defn instant?
  "True for an ISO-8601 UTC instant with millisecond precision."
  [value]
  (and (string? value)
       (boolean (re-matches iso-8601-utc-millis-pattern value))))

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
   keep authorizing bytes nobody reviewed."
  [:and
   [:map
    [:receipt/type [:= :translation/completed]]
    [:translation/document :qualified-keyword]
    [:translation/source-locale locale/Locale]
    [:translation/locale locale/Locale]
    [:translation/source-revision ConcreteRevision]
    [:translation/revision ConcreteRevision]
    [:translation/dispatch-key NonBlankString]
    [:translation/at Instant]]
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
