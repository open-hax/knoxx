(ns knoxx.backend.infra.publication-surface-verify
  "Shared smoke verifier for the required publication surface.

  Used by deploy verification and by the contract-publication E2E, so both
  exercise the same list with the same expectations.

  Each surface is checked twice: once authorized and once unauthorized. A surface
  that answers 200 to an anonymous caller is not a working surface even though it
  responds, so verifying only the happy path would pass a wide-open route.

  Both requests are awaited BEFORE either status is read. That ordering is the
  point of the `^:async` shape here: reading a status off an unresolved promise
  yields `undefined`, which compares unequal to every expected status and would
  make the verifier report a failure it never actually observed — or, worse with
  a looser comparison, pass without having checked anything."
  (:require [knoxx.backend.law.publication-surface :as surface]))

(defn- expected-unauthorized-statuses
  "An unauthorized caller must be refused. Both 401 and 403 are acceptable —
   which one depends on whether the request carried an identity at all — but a
   2xx never is."
  []
  #{401 403})

(defn ^:async verify-surface!
  "Check one surface. `request!` takes `{:method :path :authorized?}` and returns
   a Promise of `{:status n}`."
  [request! {:keys [method path permission access] :as required}]
  (let [authorized (await (request! {:method method :path path :authorized? true}))
        unauthorized (await (request! {:method method :path path :authorized? false}))
        authorized-status (:status authorized)
        unauthorized-status (:status unauthorized)]
    {:surface required
     :method method
     :path path
     :permission permission
     :access access
     :authorized-status authorized-status
     :unauthorized-status unauthorized-status
     :ok? (boolean (and (< authorized-status 400)
                        (contains? (expected-unauthorized-statuses)
                                   unauthorized-status)))}))

(defn ^:async verify-required-surface!
  "Verify every required surface. Returns
   `{:results [...] :ok? bool :failures [...]}`.

   Unconditional by construction: it iterates `surface/required-surfaces` with no
   flag, environment read, or skip branch, so a deploy cannot be configured to
   check less."
  [request!]
  (surface/assert-surfaces!)
  (loop [remaining (seq surface/required-surfaces)
         results []]
    (if-let [required (first remaining)]
      (recur (rest remaining)
             (conj results (await (verify-surface! request! required))))
      (let [failures (filterv #(not (:ok? %)) results)]
        {:results results
         :failures failures
         :ok? (empty? failures)}))))
