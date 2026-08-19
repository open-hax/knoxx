(ns knoxx.backend.law.error-body
  "The one error body shape a Knoxx HTTP boundary sends.

  Three adapters on the publication/translation surface had each hand-rolled
  this map, and two of them disagreed about which key held which value —
  `{:error <message> :detail <data>}` against `{:detail <message> :error
  <data>}`. A client could not write one error handler for the surface, and
  nothing failed, because a hand-built map has no contract to violate.

  So the shape is stated once, as a law, and built through `error-body` rather
  than assembled at each call site. `:detail` is the human-readable message and
  `:error` is the structured evidence — the same sense `infra.http/error-response!`
  already uses, which is what makes this the ordering that wins.

  The point is not which ordering is better. It is that a boundary can no longer
  pick one by accident: the only reasonable way to build this map now produces
  the right one, and a wrong one fails its own contract."
  (:require [knoxx.backend.law.publication :as law]))

(def ErrorBody
  "Closed deliberately. An adapter that adds a third key — or reintroduces the
   swapped pair alongside the correct one — fails here rather than shipping a
   body that a client's single handler cannot read."
  [:map {:closed true}
   [:detail :string]
   [:error {:optional true} [:map]]])

(defn error-body
  "The canonical error body for a thrown error.

   `ex-data` is carried under `:error` when present, so structured evidence —
   blockers, conflicts, the permission that was refused — survives to the client
   instead of being flattened into prose. A plain JS error simply has none."
  [err]
  (law/assert-valid!
   :http/error-body
   ErrorBody
   (cond-> {:detail (or (ex-message err) (str err))}
     (some? (ex-data err)) (assoc :error (ex-data err)))))
