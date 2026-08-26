(ns knoxx.backend.infra.publication-source-revision
  "What a document's *current* source revision actually is.

  `domain.publication-gate` has always declared this fact:

    :current-source-revision  [document] -> revision or nil

  and until now only test stubs supplied it. That made the whole gate inert in
  production for the ordinary case: a publication intent declaring
  `:source/current` resolved to nil, which short-circuits evidence with
  `:publication-revision-unresolved` and derives no translation work at all. So a
  translation-dispatch card cannot demonstrate 'a gated translation work item
  reaches the ingestion worker with a concrete revision' without this — it is a
  precondition the card assumed existed, not a widening of it.

  ## The revision is a content digest

  A `law.publication/Document` carries `:document/source {:path ...}` and nothing
  version-like. The honest concrete revision of a file is therefore a digest of
  its bytes: it is stable while the content is, it changes exactly when the
  content does, and it needs no external version authority to be correct.

  The rejected alternative was mtime. It is cheaper and it is wrong in both
  directions — a touched file reports a new revision with identical content, so
  translations are re-dispatched for nothing, and a restored backup reports an
  old revision for new content, so a stale translation satisfies the gate.

  ## Relative paths resolve against the checkout, not the process

  A document's `:document/source :path` is repository-relative — `docs/foo.md`.
  Handed straight to Node it would resolve against the process working
  directory, and the backend does not run at the repository root: the PM2
  configuration runs it in `backend/`, which is exactly why
  `domain.contracts.loader` tries `../contracts` *before* `contracts`. Read from
  there, `docs/foo.md` becomes `backend/docs/foo.md`, the read returns nil,
  `:source/current` never resolves, and no translation work is ever derived —
  silently, because a missing source is a legitimate state.

  So the root is derived from the contract root the resources were actually
  loaded from: if they came from `<root>/contracts`, then `<root>` is the
  checkout. That ties the two together by construction rather than by a second
  configuration value that could disagree with the first.

  Digests are read at the runtime edge, which is why this namespace is `.cljs`.
  The pure fact-shaping below is the part the gate consumes."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.law.publication :as law]
            [promesa.core :as p]))

(def digest-prefix
  "Names the algorithm inside the revision itself.

   A bare hex string would be indistinguishable from any other opaque revision,
   so a later change of algorithm could silently produce a *different* revision
   for identical content — re-translating every document once, invisibly. With
   the algorithm in the token, the change is legible in the data."
  "sha256-")

(defn content-revision
  "The concrete revision of `content`: the full SHA-256 digest.

   The whole digest, not a prefix. A truncated one reads better in a receipt,
   and it was the wrong trade: a revision is the key for translation evidence
   AND for the publication idempotency binding, so a collision does not merely
   confuse a human — it makes changed source content reuse the prior
   translation's evidence and the prior materialization's key, which admits
   untranslated content after an edit. There is no volume of documents at which
   that is an acceptable risk, and the only cost of avoiding it is a longer
   string.

   Pure, and returns nil for absent content rather than digesting the empty
   string. An unreadable source and an empty source are different facts, and
   collapsing them would give a missing file a perfectly stable revision — which
   the gate would then happily publish translations against."
  [content]
  (when (some? content)
    (str digest-prefix (crypto/sha256-hex content))))

(defn source-root
  "The directory a document's relative source path resolves against.

   The parent of the contract root the resources were loaded from. Nil when no
   contract root resolves, in which case `document-path` leaves the path alone
   and the read fails visibly rather than against a guessed root."
  [config]
  (some-> (contract-loader/contracts-dir-path config) fs/parent))

(defn resource-source-root
  "The checkout root owning one loaded resource file.

   Contract loading may scan several roots. The resource file's provenance,
   rather than the legacy first-root view, decides which checkout a relative
   document source belongs to."
  [config resource-file-path]
  (some (fn [contract-root]
          (when (or (= resource-file-path contract-root)
                    (str/starts-with? resource-file-path (str contract-root "/")))
            (fs/parent contract-root)))
        (contract-loader/contract-root-paths config)))

(defn document-path
  "Where to actually read `document`'s source from.

   An absolute path is respected as written — an operator who wrote one meant
   it. A relative path is joined to `root`. With no root, the path is returned
   unchanged: reading it against process cwd and failing is more honest than
   inventing a root, and it is the behavior that existed before."
  [root document]
  (let [path (get-in document [:document/source :path])]
    (cond
      (nil? path) nil
      (str/starts-with? path "/") path
      (nil? root) path
      :else (fs/join root path))))

(defn- ^:async document-revision!
  "Read one document's source and digest it, or nil when it cannot be read.

   Validated as a concrete revision before it leaves: everything downstream
   keys receipts and idempotency by this value, and `law.publication`'s own
   contract is the one that decides what is admissible there."
  [root document]
  (let [content (await (fs/read-file-or-nil! (document-path root document)))]
    (some->> (content-revision content)
             (law/assert-valid! :publication/concrete-revision law/ConcreteRevision))))

(defn- ^:async document-revision-entry!
  "One `[document-id revision]` pair, or `[document-id nil]` when unreadable."
  [default-root document-roots document]
  (let [document-id (:document/id document)
        root (or (get document-roots document-id) default-root)]
    [document-id (await (document-revision! root document))]))

(defn ^:async source-revisions!
  "Current revision per document id, for every document in `documents`.

   Read once, up front, for the reason `domain.publication-gate` states about
   computing evidence once: a predicate that digested a file on each call could
   return two different revisions inside one reconciliation, and the revision
   that admitted publication would disagree with the revision actually
   published.

   A document whose source cannot be read is deliberately absent from the map
   rather than present with nil, so `:current-source-revision` returns nil for
   it and the gate reports `:publication-revision-unresolved` instead of
   proceeding on a guess.

   `config` supplies the checkout root — see the namespace docstring for why the
   process working directory is the wrong answer."
  ([config documents]
   (source-revisions! config documents {}))
  ([config documents document-roots]
   (let [root (source-root config)]
     (reduce (fn [revisions [document-id revision]]
               (cond-> revisions
                 (some? revision) (assoc document-id revision)))
             {}
             (await (p/all (mapv #(document-revision-entry! root document-roots %)
                                 documents)))))))

(defn revision-facts
  "The two source-revision entries `domain.publication-gate` needs, closed over
   an already-read revision map.

   `:source-revision-superseded?` answers only for an intent that tracks
   `:source/current`. A pinned concrete revision is pinned deliberately, and
   reporting it superseded the moment the document moves on would block that
   publication permanently *and* re-derive replacement translation work on every
   pass — a live defect rather than a conservative default.

   That leaves the predicate unreachable in the current gate flow: the gate
   resolves `:source/current` to the current revision and then asks whether that
   same revision is superseded, which it cannot be. The blocker it feeds,
   `:translation-stale`, is really about the revision an existing *translation*
   was made from, and the gate's fact signature `[intent revision]` does not
   carry that. Recorded here rather than papered over with a policy nobody
   asked for; closing it needs a gate-level change and its own card."
  [revisions]
  {:current-source-revision (fn [document] (get revisions document))
   :source-revision-superseded?
   (fn [intent revision]
     (boolean (and (= :source/current (:publication/revision intent))
                   (when-let [current (get revisions (:publication/document intent))]
                     (not= revision current)))))})
