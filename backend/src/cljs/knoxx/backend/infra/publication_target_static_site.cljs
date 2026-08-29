(ns knoxx.backend.infra.publication-target-static-site
  "The static-site `IPublicationTarget`: bytes on a filesystem content root,
   committed by an atomically replaced EDN manifest.

   THE MANIFEST IS THE PUBLISHED FACT. A file on disk that no manifest entry
   names is not public, so every publish writes the artifact first and updates
   `manifest.edn` as the commit point. Both artifact and manifest writes are
   write-beside-then-rename, so a reader — a static file server has no read
   lock — observes either the whole old file or the whole new one, never a
   half-written one.

   Concurrency is real here in a way it is not in the memory target: two
   reconciler runs, or a run overlapping a deploy, touch the same directory.
   Manifest read-modify-write is serialized by a lock file created with
   O_EXCL (`domain.node.fs/write-file-exclusive-sync!` — the create IS the
   check; there is no await between reading and claiming). A holder that
   crashes leaves its lock behind, so a lock older than `lock-stale-ms` is
   unlinked and retried; the post-acquire token check keeps a takeover from
   letting two holders believe they hold it. Temp files are unique per write
   (`*.tmp-<random>`), so overlapping writers never share a temp name.

   ORPHANS AND GROWTH. Publishing a new revision reclaims the superseded
   revision's artifact file after the manifest commit, and removal reclaims
   the removed route's file — so live content is bounded by one file per
   (document, locale, revision) actually referenced by the manifest. What is
   deliberately retained: crash-orphaned `*.tmp-*` files, bounded by crash
   frequency and sweepable by age, and artifact files whose manifest entries
   were removed by an editor other than this adapter. Neither is public —
   the manifest names everything that serves.

   This adapter TRANSPORTS. It contains no admissibility, gating, or planning
   logic: it validates the artifact it is handed (an adapter is reachable
   without the effect boundary, so the boundary's check is not this adapter's
   check), and every other decision — what may publish, where it converges,
   whether a locale is admitted — was made above it."
  (:require [clojure.edn :as edn]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.law.publication-manifest :as manifest-law]
            [knoxx.backend.law.publication-receipts :as law]))

(def kind
  "The target-registry kind this adapter answers to."
  :publication-target/static-site)

(def ^:private manifest-filename "manifest.edn")
(def ^:private lock-filename "manifest.lock")
(def ^:private idempotency-dirname ".idempotency")

(def ^:private lock-stale-ms
  "Age at which a held lock is presumed abandoned by a crashed writer."
  30000)
(def ^:private lock-wait-ms
  "How long a writer waits for a live lock before failing the effect. A
   failure surfaces as drift on the receipt, so a slow peer degrades to a
   retriable miss, never a corrupted manifest."
  5000)
(def ^:private lock-poll-ms 10)

;; ── Manifest lock ──────────────────────────────────────────────────────────

(defn- sleep!
  "Promise<nil> resolving after `ms`. Polling the lock must yield the event
   loop, or a contended in-process peer could never finish its critical
   section."
  [ms]
  (js/Promise. (fn [resolve] (js/setTimeout resolve ms))))

(defn- ^:async acquire-lock!
  "Take the manifest lock, returning this holder's token.

   The lock file's content is a random token: after a stale takeover unlinks
   and recreates the file, the previous would-be holder's post-acquire read
   no longer matches its token and it keeps waiting instead of entering the
   critical section it does not hold."
  [root]
  (let [lock-path (fs/join root lock-filename)
        token (crypto/random-hex 8)
        deadline (+ (js/Date.now) lock-wait-ms)]
    (loop []
      (if (fs/write-file-exclusive-sync! lock-path token)
        (if (= token (fs/read-file-sync lock-path))
          token
          (do (await (sleep! lock-poll-ms))
              (recur)))
        (do (when-let [stat (await (fs/stat-or-nil! lock-path))]
              (when (< (:mtime-ms stat) (- (js/Date.now) lock-stale-ms))
                (fs/unlink-sync! lock-path)))
            (when (<= deadline (js/Date.now))
              (throw (ex-info "publication manifest lock unavailable"
                              {:content-root root})))
            (await (sleep! lock-poll-ms))
            (recur))))))

(defn- release-lock!
  "Release the manifest lock, but only if this holder's token is still the
   one on disk — after a stale takeover the file belongs to someone else."
  [root token]
  (let [lock-path (fs/join root lock-filename)]
    (when (= token (fs/read-file-sync lock-path))
      (fs/unlink-sync! lock-path))))

(defn- ^:async with-manifest-lock!
  "Run zero-argument async `f` holding the manifest lock."
  [root f]
  (let [token (await (acquire-lock! root))]
    (try
      (await (f))
      (finally
        (release-lock! root token)))))

;; ── Manifest and artifact I/O ──────────────────────────────────────────────

(defn- ^:async read-manifest!
  "The current manifest, or nil when absent. Absent is the normal state of
   every content root before the first publication — never an error. A
   manifest that EXISTS but does not parse or validate throws: that is a
   writer defect, and reading it as empty would publish over routes nobody
   can account for."
  [root]
  (when-let [edn-string (await (fs/read-file-or-nil!
                                (fs/join root manifest-filename)))]
    (manifest-law/edn->manifest edn-string)))

(defn- ^:async write-manifest-atomic!
  "The commit point. Write beside and rename, so a reader observes either the
   whole old manifest or the whole new one."
  [root manifest]
  (let [final-path (fs/join root manifest-filename)
        temp-path (str final-path ".tmp-" (crypto/random-hex 8))]
    (await (fs/write-file-encoded! temp-path
                                   (manifest-law/manifest->edn manifest)
                                   "utf-8"))
    (await (fs/rename! temp-path final-path))))

(defn- ^:async write-artifact-atomic!
  "Write the artifact bytes beside their final name and rename into place.
   A string is written under the artifact's declared `:artifact/encoding`;
   bytes are written verbatim. Nothing is re-encoded or re-decided."
  [root relative-path artifact]
  (let [final-path (fs/join root relative-path)
        temp-path (str final-path ".tmp-" (crypto/random-hex 8))]
    (await (fs/mkdir! (fs/parent final-path)))
    (if (string? (:artifact/content artifact))
      (await (fs/write-file-encoded! temp-path
                                     (:artifact/content artifact)
                                     (:artifact/encoding artifact)))
      (await (fs/write-bytes! temp-path (:artifact/content artifact))))
    (await (fs/rename! temp-path final-path))))

(defn- ^:async reclaim-artifact!
  "Best-effort unlink of a superseded or removed route's artifact file.

   Runs AFTER the manifest commit, so its failure can never retract a
   committed publication: the route is already gone (or already replaced)
   and the file is already unreferenced. A failure leaves an orphan, which
   the namespace docstring bounds — it must not leave a failed receipt for
   a publication that succeeded."
  [root relative-path]
  (when relative-path
    (try
      (await (fs/unlink! (fs/join root relative-path)))
      (catch :default err
        (js/console.warn "[publication-static-site] could not reclaim artifact"
                         relative-path (ex-message err))))))

;; ── Receipts ───────────────────────────────────────────────────────────────

(defn- materialized-receipt
  "Evidence of the materialization the op requested. Same shape the memory
   target returns, so adapters stay interchangeable upward."
  [adapter-id op]
  (let [intent (:intent op)
        revision (:concrete-revision op)
        path (:publication/path intent)]
    {:receipt/type :publication/materialized
     :publication/id (:publication/id intent)
     :adapter/id adapter-id
     :idempotency/key (:idempotency/key op)
     :document/id (:publication/document intent)
     :target (:publication/garden intent)
     :locale (:publication/locale intent)
     :revision revision
     :path path
     :materialized/revision revision
     :materialized/path path}))

;; ── Protocol methods ───────────────────────────────────────────────────────

(defn- ^:async commit-route!
  "The critical section of a publish, run holding the manifest lock: write
   the artifact bytes, commit the manifest route, then reclaim the displaced
   route's bytes. Replay — manifest route identical AND artifact file
   present — rewrites nothing: the bytes on disk are already exactly what
   this op was asked to serve."
  [root route artifact]
  (let [manifest (or (await (read-manifest! root))
                     (manifest-law/empty-manifest))
        existing (manifest-law/find-route manifest (:publication/id route))
        artifact-path (:route/artifact route)
        unchanged? (and (= existing route)
                        (fs/exists? (fs/join root artifact-path)))]
    (when-not unchanged?
      (await (write-artifact-atomic! root artifact-path artifact))
      (await (write-manifest-atomic! root
                                     (-> manifest
                                         (manifest-law/upsert-route route)
                                         (manifest-law/touch))))
      (let [previous-path (:route/artifact existing)]
        (when (and previous-path (not= previous-path artifact-path))
          (await (reclaim-artifact! root previous-path)))))))

(defn- ^:async publish-artifact!
  "Materialize `op`: validate the artifact, write its bytes, then commit the
   manifest route."
  [root adapter-id op]
  (let [intent (:intent op)
        artifact (law/assert-artifact! (:artifact op) intent (:concrete-revision op))
        route (manifest-law/route-for-artifact intent artifact)]
    (await (with-manifest-lock! root
                                (^:async fn []
                                  (await (commit-route! root route artifact)))))
    (materialized-receipt adapter-id op)))

(defn- ^:async remove-publication!
  "Take the publication's route out of the manifest (the commit), then
   reclaim its artifact file. Keyed on `:publication/id`, so a removal finds
   the route wherever a path move left it. Idempotent: nothing manifested
   is nothing to remove, and the receipt is the same either way."
  [root intent observed]
  (await (with-manifest-lock!
          root
          (^:async fn []
            (when-let [manifest (await (read-manifest! root))]
              (when-let [route (manifest-law/find-route manifest
                                                        (:publication/id intent))]
                (await (write-manifest-atomic!
                        root
                        (-> manifest
                            (manifest-law/remove-route (:publication/id intent))
                            (manifest-law/touch))))
                (await (reclaim-artifact! root (:route/artifact route))))))))
  {:receipt/type :publication/removed
   :publication/id (:publication/id intent)
   :removed/path (:materialized/path observed)})

(defn- ^:async observe-publication!
  "The observed materialization for `intent`, or nil.

   Read from the manifest and keyed on `:publication/id`, never on the
   desired path — keying on path is what makes a path move leave both routes
   public, because the route being replaced is invisible at its old address.

   The manifest is the published fact, but a route only SERVES when its
   bytes exist. A deploy overlap or external deletion can remove the file
   without touching the manifest; reporting that route as materialized would
   block convergence, so an absent artifact reads as not published and the
   planner republishes."
  [root intent]
  (when-let [manifest (await (read-manifest! root))]
    (when-let [route (manifest-law/find-route manifest (:publication/id intent))]
      (when (fs/exists? (fs/join root (:route/artifact route)))
        {:materialized/revision (:route/revision route)
         :materialized/path (:route/path route)
         :publication/id (:publication/id route)
         :locale (:route/locale route)
         :route/artifact (:route/artifact route)}))))

;; ── Construction ───────────────────────────────────────────────────────────

(defn static-site-target
  "An `IPublicationTarget` persisting under the content root named by the
   declaration's `:publication-target/config` `:content-root`.

   Takes the whole validated declaration so the registry can use this
   function directly as a kind factory; the adapter's identity IS the
   declared target id, which is what keeps idempotency keys resource-owned."
  [declaration]
  (let [target-id (:publication-target/id declaration)
        content-root (get-in declaration
                             [:publication-target/config :content-root])]
    (when-not (and (string? content-root) (seq content-root))
      (throw (ex-info "static-site publication target requires a :content-root"
                      {:publication-target/id target-id})))
    (reify effects/IPublicationTarget
      (target-id [_] target-id)
      (publish! [_ _ctx op]
        (publish-artifact! content-root target-id op))
      (remove! [_ _ctx intent observed]
        (remove-publication! content-root intent observed))
      (observe! [_ _ctx intent]
        (observe-publication! content-root intent)))))

;; ── Idempotency store ──────────────────────────────────────────────────────

(defn- read-edn-safe
  "Parse `edn-string`, nil on any failure. A reservation file whose content
   cannot be read is a claim whose outcome is unknown, which is exactly what
   `:in-flight` means — never a reason to throw past the store."
  [edn-string]
  (try
    (edn/read-string edn-string)
    (catch :default _ nil)))

(defn- reserve-key!
  "Atomically claim the reservation file for `key`, or report what the
   existing claim knows. The O_EXCL create IS the check: no await and no
   separate existence test for a concurrent caller or a crash to slip
   between."
  [key-file key]
  (let [file (key-file key)]
    (if (fs/write-file-exclusive-sync!
         file (pr-str {:idempotency/status :in-flight}))
      {:reservation/status :reserved}
      (let [entry (read-edn-safe (fs/read-file-sync file))]
        (if (:receipt entry)
          {:reservation/status :done :receipt (:receipt entry)}
          {:reservation/status :in-flight})))))

(defn- complete-key!
  "Record `receipt` for a claimed key, written beside the claim and renamed
   over it, so a concurrent `reserve!` reads either the in-flight marker or
   the whole receipt, never a torn write."
  [key-file key receipt]
  (let [file (key-file key)
        temp-path (str file ".tmp-" (crypto/random-hex 8))]
    (fs/write-file-sync! temp-path (pr-str {:receipt receipt}))
    (fs/rename-sync! temp-path file))
  nil)

(defn static-site-store
  "An `IIdempotencyStore` persisted under `<content-root>/.idempotency/`,
   one file per key, named by the key's SHA-256 so any key content and length
   maps to a safe, deterministic filename.

   All three methods are synchronous: `publish-once!` reads the reservation
   map directly off the call, and a promise here would fall through its
   `case` as an unrecognized status."
  [content-root]
  (let [dir (fs/join content-root idempotency-dirname)
        ensured? (atom false)
        ensure-dir! (fn []
                      (when-not @ensured?
                        (fs/mkdir-sync! dir)
                        (reset! ensured? true)))
        key-file (fn [key] (fs/join dir (str (crypto/sha256-hex key) ".edn")))]
    (reify effects/IIdempotencyStore
      (reserve! [_ key]
        (ensure-dir!)
        (reserve-key! key-file key))
      (complete! [_ key receipt]
        (ensure-dir!)
        (complete-key! key-file key receipt))
      (release! [_ key]
        (fs/unlink-sync! (key-file key))
        nil))))
