(ns knoxx.backend.infra.publication-migration
  "Effectful driver for the one-time publication authority transfer.

  This namespace owns the I/O the migration needs — reading legacy records,
  writing resources, appending conflict receipts — and delegates every semantic
  decision to `knoxx.backend.domain.publication-migration`. The split matters:
  a namespace that performs I/O cannot also be the pure decision layer.

  The fold awaits the legacy read, every write, and every receipt append before
  it recurs. Without those awaits `saved` would be a Promise rather than the
  written resource, the in-run index would hold Promises, and every later
  reconciliation in the run would silently compare against pending state.

  Phases run documents, then gardens, then publications, so a publication is
  always reconciled against documents and gardens already written in this run.
  A publication-only migration would leave every intent with dangling
  references."
  (:require [knoxx.backend.domain.publication-migration :as migration]
            [knoxx.backend.law.publication :as law]))

(def LegacySource
  "Contract for what the legacy reader returns. The read crosses an effect
   boundary, so its shape is named and asserted rather than destructured on
   faith: a record missing its `:document`/`:row` wrapper would otherwise
   destructure to nils and surface much later as an unrelated decode conflict."
  [:map {:closed true}
   [:documents {:optional true} [:vector [:map]]]
   [:gardens {:optional true} [:vector [:map]]]
   [:publications {:optional true}
    [:vector [:map {:closed true}
              [:document [:map]]
              [:row [:map]]]]]])

(defn migration-receipt-key
  "Stable per-source-record identity, so a rerun over unchanged legacy data
   appends no duplicate conflict receipt."
  [phase record]
  [:publication/migration
   phase
   (or (:source/collection record) phase)
   (or (:source/id record)
       (:legacy/doc-id record)
       (:garden-id record))])

(defn- ^:async append-conflict!
  "Append a conflict receipt at most once per source record.

   Cross-run idempotence is the appender's contract — hence
   `:append-receipt-once!` rather than `:append-receipt!`: reruns key off the
   stable receipt key and must not duplicate. The in-run `seen` set is only an
   optimization that avoids re-calling it for a record already handled."
  [ctx seen phase record decision]
  (let [receipt-key (migration-receipt-key phase record)]
    (when-not (contains? @seen receipt-key)
      (swap! seen conj receipt-key)
      (await ((:append-receipt-once! ctx) receipt-key decision)))))

(def ^:private empty-run
  {:conflicts [] :written [] :noops []})

(defn- ^:async apply-decision!
  "Fold one classified decision into the accumulator, performing its effect. A
   write is awaited before it reaches `index-resource`, so the in-run index only
   ever holds saved resources."
  [ctx seen phase acc decision record]
  (case (:migration/status decision)
    :write
    (let [saved (await ((:write! ctx) (:resource decision)))]
      (-> acc
          (update :index migration/index-resource saved)
          (update :written conj saved)))

    :conflict
    (do (await (append-conflict! ctx seen phase record decision))
        (update acc :conflicts conj decision))

    :noop
    (update acc :noops conj (:resource/id decision))))

(defn- ^:async run-phase!
  "Classify and apply every record of one phase.

   `classify` receives the current index and a record and returns a decision.
   `receipt-of` projects a record onto the map that carries its stable receipt
   identity — for publications that is the normalized row, not the
   `{:document :row}` wrapper the fold iterates."
  [ctx seen phase acc records classify receipt-of]
  (loop [acc acc
         remaining (seq records)]
    (if-let [record (first remaining)]
      (recur (await (apply-decision! ctx seen phase acc
                                     (classify (:index acc) record)
                                     (receipt-of record)))
             (rest remaining))
      acc)))

(defn- candidate->decision
  "Documents and gardens have no relation identity to reconcile, so a candidate
   is a write unless the index already holds exactly that resource."
  [index kind id-key decision]
  (if (migration/conflict? decision)
    decision
    (let [candidate (:resource decision)]
      (if (= candidate (get-in index [kind (id-key candidate)]))
        {:migration/status :noop :resource/id (id-key candidate)}
        {:migration/status :write :resource candidate}))))

(defn- document-classifier
  [policy]
  (fn [index document]
    (candidate->decision index :documents :document/id
                         (migration/document->decision policy document))))

(defn- garden-classifier
  [policy]
  (fn [index garden]
    (candidate->decision index :gardens :garden/id
                         (migration/garden->decision policy garden))))

(defn- publication-classifier
  [policy]
  (fn [index {:keys [document row]}]
    (migration/migrate-record policy index document row)))

(defn ^:async migrate-publication-records!
  "Run the migration fold. `ctx` supplies the effects:

     :read-records!        [] -> Promise of LegacySource
     :write!               [resource] -> Promise of the saved resource
     :append-receipt-once! [key decision] -> Promise, idempotent by key

   Returns `{:index ... :conflicts [...] :written [...] :noops [...]}`.

   `loop`/`recur` is lawful here because every `await` settles before the
   recursion point; the recursion is over resolved state, never over pending
   Promises."
  [ctx policy index]
  (migration/assert-policy! policy)
  (let [source (law/assert-valid! :migration/legacy-source
                                  LegacySource
                                  (await ((:read-records! ctx))))
        seen (atom #{})
        phases [[:documents (:documents source) (document-classifier policy) identity]
                [:gardens (:gardens source) (garden-classifier policy) identity]
                [:publications (:publications source) (publication-classifier policy) :row]]]
    (loop [acc (assoc empty-run :index index)
           remaining (seq phases)]
      (if-let [[phase records classify receipt-of] (first remaining)]
        (recur (await (run-phase! ctx seen phase acc records classify receipt-of))
               (rest remaining))
        acc))))
