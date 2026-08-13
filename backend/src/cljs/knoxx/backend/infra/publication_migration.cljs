(ns knoxx.backend.infra.publication-migration
  "Effectful driver for the one-time publication authority transfer.

  This namespace owns the I/O the migration needs — reading legacy records,
  writing resources, appending conflict receipts — and delegates every semantic
  decision to `knoxx.backend.domain.publication-migration`. The split matters:
  a namespace that performs I/O cannot also be the pure decision layer.

  The fold awaits the legacy read, every write, and every receipt append before
  it recurs. Without those awaits `written` would be a Promise rather than the
  saved resource, the in-run index would hold Promises, and every later
  reconciliation in the run would silently compare against pending state."
  (:require [knoxx.backend.domain.publication-migration :as migration]))

(defn migration-receipt-key
  "Stable per-source-record identity, so a rerun over unchanged legacy data
   appends no duplicate conflict receipt."
  [record]
  [:publication/migration
   (:source/collection record)
   (:source/id record)])

(defn- ^:async append-conflict!
  "Append a conflict receipt at most once per source record.

   Cross-run idempotence is the appender's contract — hence
   `:append-receipt-once!` rather than `:append-receipt!`: reruns key off the
   stable receipt key and must not duplicate. The in-run `seen` set is only an
   optimization that avoids re-calling it for a source record already handled
   in this run."
  [ctx seen record decision]
  (let [receipt-key (migration-receipt-key record)]
    (when-not (contains? @seen receipt-key)
      (swap! seen conj receipt-key)
      (await ((:append-receipt-once! ctx) receipt-key decision)))))

(def ^:private empty-run
  {:conflicts [] :written [] :noops []})

(defn- ^:async apply-decision!
  "Fold one classified decision into the run accumulator, performing its
   effect. A write is awaited before it reaches `index-resource`, so the
   in-run index only ever holds saved resources."
  [ctx seen acc decision row]
  (case (:migration/status decision)
    :write
    (let [saved (await ((:write! ctx) (:resource decision)))]
      (-> acc
          (update :index migration/index-resource saved)
          (update :written conj saved)))

    :conflict
    (do (await (append-conflict! ctx seen row decision))
        (update acc :conflicts conj decision))

    :noop
    (update acc :noops conj (:resource/id decision))))

(defn ^:async migrate-publication-records!
  "Run the migration fold. `ctx` supplies the effects:

     :read-records!        [] -> Promise of [{:document ... :row ...} ...]
     :write!               [resource] -> Promise of the saved resource
     :append-receipt-once! [key decision] -> Promise, idempotent by key

   Returns `{:index ... :conflicts [...] :written [...] :noops [...]}`.

   `loop`/`recur` is lawful here because every `await` settles before the
   recursion point; the recursion is over resolved state, never over pending
   Promises."
  [ctx policy index]
  (migration/assert-policy! policy)
  (let [records (await ((:read-records! ctx)))
        seen (atom #{})]
    (loop [acc (assoc empty-run :index index)
           remaining (seq records)]
      (if-let [{:keys [document row]} (first remaining)]
        (let [decision (migration/migrate-record policy (:index acc) document row)]
          (recur (await (apply-decision! ctx seen acc decision row))
                 (rest remaining)))
        acc))))
