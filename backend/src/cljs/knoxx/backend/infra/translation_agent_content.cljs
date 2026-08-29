(ns knoxx.backend.infra.translation-agent-content
  "Where an agent-submitted translation's bytes live.

  A translation receipt is *evidence* — `law.translation-evidence` says so in as
  many words, and it carries no content on purpose. But a static site serves
  bytes, so something has to hold what the agent actually wrote. Until now two
  things did: authored locale files on disk, and OpenPlanner translation
  segments. Neither is where an agent actor's output belongs — the first is
  human-authored content, and the second is the transport this cutover exists to
  remove.

  ## Keyed by the output revision, never by the source revision

  This is the whole reason the namespace is shaped the way it is. Two agent runs
  for one source revision produce two different translations, and
  `law.translation-dispatch/output-revision` is precisely what tells them apart.
  Keyed by `[document garden locale source-revision]` instead, the second run's
  bytes would silently replace the first's — and since an approval is pinned to
  the output revision it reviewed, the site would then serve bytes carrying an
  approval that was granted for different ones. That is the exact
  transplanted-review failure `law.translation-evidence` builds two revisions to
  prevent, reintroduced one layer down.

  So the reader takes an output revision, and the only supported way to obtain
  one is `domain.translation-evidence/receipt-for` — the receipt the gate itself
  matched. A caller who has no receipt has no business reading content.

  ## Filesystem, under the content root deployment already configures

  The rejected alternative was Mongo, alongside the translation evidence. Evidence
  and content have genuinely different durability requirements: evidence must
  survive because it is the only record that work happened, while content is
  reproducible by re-running the agent. More concretely, the static-site target
  already owns a content root, the deploy contract already mounts it, and adding
  a second persistence dependency to the publication path would mean a Mongo
  outage stops the site from rendering content it already has.

  Files are named by a digest of the output revision rather than by the revision
  itself. An output revision embeds the producing run id, which is an agent
  session id — not a value with any filesystem-safety guarantee. Digesting it
  makes the name total, and the file records the identity it was written under so
  a human reading the directory can still tell what each one is."
  (:require [clojure.edn :as edn]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-evidence :as evidence-law]))

(def dirname
  "The subdirectory of the content root that holds agent-submitted translations.

   Dot-prefixed for the reason the static-site target's `.idempotency` is: this
   is runtime bookkeeping inside a directory whose visible contents are the
   published site, and a `translations/` sibling would read as a published path."
  ".translations")

(def ^:private extension ".edn")

(defn store-dir
  [content-root]
  (fs/join content-root dirname))

(defn entry-path
  "Where the content for one output revision is written.

   Total over any output revision, including one whose run id contains a path
   separator — see the ns docstring."
  [content-root output-revision]
  (fs/join (store-dir content-root)
           (str (crypto/sha256-hex (str output-revision)) extension)))

(def Entry
  "One stored translation: its full identity, plus the bytes.

   The identity is duplicated here rather than trusted from the filename,
   because the filename is a digest and a digest cannot be read back. On the way
   out it is checked against what the caller asked for, so a hash collision or a
   hand-edited file cannot hand back another document's content."
  [:map {:closed true}
   [:translation/document :qualified-keyword]
   [:translation/garden :qualified-keyword]
   [:translation/locale :keyword]
   [:translation/source-locale :keyword]
   [:translation/source-revision agent-law/NonBlankString]
   [:translation/revision agent-law/NonBlankString]
   [:translation/org-id agent-law/NonBlankString]
   [:translation/project {:optional true} [:maybe agent-law/NonBlankString]]
   [:translation/content :string]])

(defn entry
  "The value written for one submitted translation.

   Built from the dispatch `record` and the minted `output-revision`, never from
   the agent's submission: the submission supplies bytes and nothing else. Every
   coordinate the reader later matches on therefore comes from the claim that
   was reserved before the agent ran, which is the same both-directions rule
   `law.translation-dispatch/translation-receipt` follows."
  [record output-revision content]
  (agent-law/assert-valid!
   :translation-agent-content/entry
   Entry
   (cond-> {:translation/document (:dispatch/document record)
            :translation/garden (:dispatch/garden record)
            :translation/locale (:dispatch/locale record)
            :translation/source-locale (:dispatch/source-locale record)
            :translation/source-revision (:dispatch/revision record)
            :translation/revision output-revision
            :translation/org-id (:dispatch/org-id record)
            :translation/content content}
     (some? (:dispatch/project record))
     (assoc :translation/project (:dispatch/project record)))))

(defn ^:async write!
  "Persist one submitted translation. Returns the entry that was written.

   Overwrites, and that is correct rather than merely convenient: the key is the
   output revision, so the same key can only ever be rewritten by the same run
   re-submitting. A second run has a different run id, therefore a different
   output revision, therefore a different file — the two never contend."
  [content-root record output-revision content]
  (let [value (entry record output-revision content)]
    (await (fs/write-file-ensure-dir!
            (entry-path content-root output-revision)
            (pr-str value)))
    value))

(def ^:private compared-coordinates
  "Every field a stored entry and a receipt must agree about.

   Every coordinate, not just the revision. The revision alone would be enough
   if digests were trustworthy identifiers of intent, but the file is on a
   filesystem an operator can edit, and reading content the receipt does not
   describe is the one outcome this namespace exists to prevent."
  [:translation/document
   :translation/garden
   :translation/locale
   :translation/source-locale
   :translation/source-revision
   :translation/revision
   :translation/org-id
   :translation/project])

(defn- coordinates
  "The compared fields of a receipt or entry, with absent and nil made equal.

   Projected with `get` rather than `select-keys`, and that is not a style
   choice. An untenanted translation reaches this comparison spelled two ways:
   `law.translation-dispatch/translation-receipt` always assocs
   `:translation/project`, so the receipt carries an explicit nil, while `entry`
   omits the key entirely because the `Entry` contract has it optional. Under
   `select-keys` those two produce different maps, so a project-less
   translation's content was written and then never readable — the site fell
   through to the authored fallback while a perfectly good agent translation sat
   on disk."
  [value]
  (mapv #(get value %) compared-coordinates))

(defn- matches-receipt?
  "Whether a stored entry is the content the receipt attests to."
  [value receipt]
  (= (coordinates value) (coordinates receipt)))

(defn ^:async content-for-receipt!
  "The agent-submitted bytes `receipt` attests to, or nil.

   Nil for every ordinary absence — no such file, an unreadable one, or one
   describing something else — because absence has a legitimate meaning here:
   this translation came from the authored fallback or from OpenPlanner
   segments, and the caller falls through to those. What it must never do is
   return the *wrong* bytes, which is why the identity is rechecked rather than
   assumed from the path.

   A malformed file is treated as absent rather than thrown. The alternative
   would let one corrupt entry take down reconciliation for every locale of
   every document, and the correct behavior for content that cannot be read is
   the same as for content that was never written."
  [content-root receipt]
  (when (and (some? content-root) (some? receipt))
    (let [raw (await (fs/read-file-or-nil!
                      (entry-path content-root (:translation/revision receipt))))
          value (when raw
                  (try
                    (let [parsed (edn/read-string raw)]
                      (when (and (map? parsed)
                                 (evidence-law/nonblank-string?
                                  (:translation/revision parsed)))
                        parsed))
                    (catch :default _ nil)))]
      (when (and value (matches-receipt? value receipt))
        (:translation/content value)))))
