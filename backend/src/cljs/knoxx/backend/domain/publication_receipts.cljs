(ns knoxx.backend.domain.publication-receipts
  "Observed publication facts and the projection reconciliation reads.

  Receipts describe what an effect *did*. They are never desired-state
  authority, and desired state never carries them. This namespace holds only
  shapes and pure projections — the effect protocol and its in-memory
  implementation live in `infra.*`, because a domain namespace that also owned an
  adapter would blur exactly the boundary this card exists to prove.

  `law.publication-receipts` carries the *minimum* an adapter must return at the
  effect boundary. The shape here is the *full observation* used for drift
  analysis and deploy verification: it additionally pins which publication, on
  which adapter, under which key, so a receipt is explainable without joining it
  back to anything."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication :as publication]))

(def ConcreteRevision
  "A materialization receipt records what actually shipped, so a selector token
   like `:source/current` can never appear — only a concrete revision."
  publication/NonBlankString)

(def PublicationMaterializedReceipt
  [:map
   [:receipt/type [:= :publication/materialized]]
   [:publication/id :qualified-keyword]
   [:adapter/id :keyword]
   [:idempotency/key publication/NonBlankString]
   [:document/id :qualified-keyword]
   [:target :qualified-keyword]
   [:locale publication/Locale]
   [:revision ConcreteRevision]
   [:path publication/PublicationPath]
   [:materialized/revision ConcreteRevision]
   [:materialized/path publication/PublicationPath]])

(def drift-keys
  "The exact fields the planner compares to decide convergence. Named here and
   consumed by both the planner and this projection so the two cannot drift
   apart — a projection that returned a different key set would make every
   comparison silently fail.

   `:materialized/title` is here because the manifest route's title is DERIVED
   from the Document contract, and derived state that convergence ignores can
   never be corrected. Without it a route materialized before titles existed
   kept `:route/title` absent forever: same revision, same path, so `converge`
   answered `:noop` on every attempt and the site listed the document as
   untitled with no way to fix it short of deleting the route. The same hole
   would swallow any later rename."
  [:materialized/revision :materialized/path :materialized/title])

(defn canonical-title
  "Canonical materialized title, or nil when the value carries no title."
  [title]
  (when-not (str/blank? title)
    title))

(defn canonical-materialization
  "Select convergence fields and spell a missing or blank title as absence."
  [materialization]
  (cond-> (select-keys materialization drift-keys)
    (nil? (canonical-title (:materialized/title materialization)))
    (dissoc :materialized/title)))

(defn materialized?
  [receipt]
  (= :publication/materialized (:receipt/type receipt)))

(def Observation
  "Exactly what the planner compares, and nothing else. Closed, because an
   observation carrying extra fields would compare unequal to a desired
   materialization that is otherwise identical."
  [:map {:closed true}
   [:materialized/revision ConcreteRevision]
   [:materialized/path publication/PublicationPath]
   ;; Optional, because a route materialized before titles existed genuinely
   ;; has none and its receipt must stay readable. Present in `drift-keys`
   ;; regardless: absent-versus-present is exactly the drift that makes such a
   ;; route republish and acquire its title.
   [:materialized/title {:optional true} [:maybe :string]]])

(defn observed-materialization
  "The observation the planner compares, or nil for any receipt that is not a
   successful materialization. A blocked or failed receipt must never be
   mistaken for something being public.

   A receipt that *claims* to be a materialization is held to it. Checking only
   the discriminator meant `{:receipt/type :publication/materialized}` projected
   to `{}` — a truthy observation asserting something is public while naming
   neither revision nor path, which the planner then compared unequal to every
   desired state. A receipt that cannot say what it materialized is corrupt, and
   corruption is surfaced rather than read as absence: returning nil here would
   quietly claim nothing is public and republish over whatever is."
  [receipt]
  (when (materialized? receipt)
    (->> (canonical-materialization receipt)
         (publication/assert-valid! :publication/observation Observation))))

(defn observed-for
  "Observation for one publication id, from a collection of receipts. Later
   receipts win, so a removal after a publish leaves nothing observed."
  [receipts publication-id]
  (->> receipts
       (filter #(= publication-id (:publication/id %)))
       (reduce (fn [_observed receipt]
                 (observed-materialization receipt))
               nil)))
