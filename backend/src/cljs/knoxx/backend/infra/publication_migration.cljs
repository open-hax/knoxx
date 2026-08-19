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

(defn- stable-row-key
  "Content-derived coordinate for a row with no usable identity.

   Deliberately the row's own content and not its position: cross-run
   idempotence is this key's whole purpose, and an ordinal would change the key
   whenever the reader's order changed.

   Entries are ordered by their *printed* key rather than through a sorted map,
   so the rendering is independent of the order the map was built in and stays
   total: a legacy row mixing keyword and string keys would make a sorted map
   throw, and a throw here would abort the whole batch from inside the very
   path that exists to record one row's failure."
  [record]
  (pr-str (into [] (sort-by (comp pr-str key) record))))

(defn migration-receipt-key
  "Stable per-source-record identity, so a rerun over unchanged legacy data
   appends no duplicate conflict receipt.

   The content fallback is load-bearing. Rows whose identity is *itself* the
   malformed thing have no id to key on, and every identity fallback resolving
   to nil made two different broken rows share one key — the `seen` set then
   suppressed the second and the append-once store permanently kept only one,
   so not every malformed row was available for resolution. Two rows that are
   byte-identical still share a key, which is correct: they are one fact."
  [phase record]
  [:publication/migration
   phase
   (or (:source/collection record) phase)
   (or (:source/id record)
       (:legacy/doc-id record)
       (:garden-id record)
       (stable-row-key record))])

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

(def ^:private phase-resource-schema
  "What a saved resource of each phase must satisfy."
  {:documents law/Document
   :gardens law/Garden
   :publications law/PublicationIntentResource})

(defn- ^:async apply-decision!
  "Fold one classified decision into the accumulator, performing its effect. A
   write is awaited before it reaches `index-resource`, so the in-run index only
   ever holds saved resources."
  [ctx seen phase acc decision record]
  (case (:migration/status decision)
    :write
    (let [written (await ((:write! ctx) (:resource decision)))
          ;; The writer crosses an effect boundary, so what comes back is
          ;; checked rather than trusted. A nil, an acknowledgement map, or an
          ;; altered resource would otherwise be indexed as saved state, and
          ;; every later row in the run would reconcile against it.
          saved (law/assert-valid! (keyword "migration" (str (name phase) "-written"))
                                   (phase-resource-schema phase)
                                   written)]
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
   is a write unless the index already holds that id.

   Holding the id with a *different* payload is a conflict, not a write. Two
   legacy rows whose names canonicalize to one id but disagree on source path,
   locale, title or status would otherwise let the second overwrite the first —
   last-write-wins on exactly the ambiguous rows this migration exists to
   report, and order-dependent on top of that."
  [index kind id-key row decision]
  (if (migration/conflict? decision)
    decision
    (let [candidate (:resource decision)
          existing (get-in index [kind (id-key candidate)])]
      (cond
        (nil? existing)
        {:migration/status :write :resource candidate}

        (= candidate existing)
        {:migration/status :noop :resource/id (id-key candidate)}

        :else
        {:migration/status :conflict
         :reason :resource-identity-conflict
         :resource/kind kind
         :resource/id (id-key candidate)
         :candidate candidate
         :existing existing
         :source row}))))

(defn- document-classifier
  [policy]
  (fn [index document]
    (candidate->decision index :documents :document/id document
                         (migration/document->decision policy document))))

(defn- garden-classifier
  [policy]
  (fn [index garden]
    (candidate->decision index :gardens :garden/id garden
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
