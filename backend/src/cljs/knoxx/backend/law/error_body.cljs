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

(def opaque-detail
  "The detail sent for a failure the surface could not classify."
  "internal error")

(defn classified?
  "Whether the boundary recognized this failure well enough to describe it.

   An allow-list of 4xx, not a denial-list on 500. A status that is nil, 500,
   502, or something a future adapter invents all fall to the safe side, which
   is the direction this predicate has to fail: forgetting to add a status here
   withholds detail, whereas forgetting to exclude one would publish it."
  [status]
  (and (integer? status) (<= 400 status 499)))

(defn error-body
  "The canonical error body for a thrown error, given the status being sent.

   For a classified failure, `ex-data` is carried under `:error`, so structured
   evidence — blockers, conflicts, the permission that was refused — survives to
   the client instead of being flattened into prose. That evidence is the point
   of a 409 here.

   For anything else the body is opaque, and BOTH halves are withheld. A 500 is
   the boundary saying it does not know what happened, so nothing about the
   error is known to be safe to publish: `ex-data` at that point can hold a
   config map or a resolved filesystem path, and the message is no better — an
   ENOENT names the path it could not read and a driver error names the host it
   dialled. The caller can do nothing with either, so it gets neither.

   The status is a required argument rather than an option with a permissive
   default. A caller that forgets it gets an arity error at compile time, not a
   body that quietly leaks."
  [err status]
  (law/assert-valid!
   :http/error-body
   ErrorBody
   (if (classified? status)
     (cond-> {:detail (or (ex-message err) (str err))}
       (some? (ex-data err)) (assoc :error (ex-data err)))
     {:detail opaque-detail})))
