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

(defn default-request
  "The request for one surface, before any deployment-specific materialization.

   `:permission` is passed through deliberately. Each surface declares the exact
   capability it should require, and a probe that only knows \"authorized or
   not\" cannot tell a correctly-guarded route from one guarded by a broader
   capability — a wide credential satisfies both, and the anonymous probe refuses
   both. A caller that mints a credential scoped to `:permission` verifies the
   authorization contract this list actually advertises."
  [{:keys [method path permission]} authorized?]
  {:method method
   :path path
   :permission permission
   :authorized? authorized?})

(defn ^:async verify-surface!
  "Check one surface. `request!` takes the request map and returns a Promise of
   `{:status n}`.

   `build-request` materializes a surface into a request, defaulting to
   `default-request`. It exists because several paths are templates —
   `/api/publications/documents/:documentId`,
   `/api/cms/publications/intents/:publicationId` — and the two PATCH surfaces
   need a body. A shared contract cannot know a real document id or a safe
   payload for a given deployment, and sending the literal template answers 404
   or fails validation, which would fail this gate while every route is healthy.
   So the list stays deployment-agnostic and the caller supplies the knowledge it
   has."
  ([request! required] (verify-surface! request! required default-request))
  ([request! {:keys [method path permission access] :as required} build-request]
  (let [authorized (await (request! (build-request required true)))
        unauthorized (await (request! (build-request required false)))
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
                                   unauthorized-status)))})))

(defn ^:async verify-required-surface!
  "Verify every required surface. Returns
   `{:results [...] :ok? bool :failures [...]}`.

   Unconditional by construction: it iterates `surface/required-surfaces` with no
   flag, environment read, or skip branch, so a deploy cannot be configured to
   check less."
  ([request!] (verify-required-surface! request! default-request))
  ([request! build-request]
  (surface/assert-surfaces!)
  (loop [remaining (seq surface/required-surfaces)
         results []]
    (if-let [required (first remaining)]
      (recur (rest remaining)
             (conj results (await (verify-surface! request! required build-request))))
      (let [failures (filterv #(not (:ok? %)) results)]
        {:results results
         :failures failures
         :ok? (empty? failures)})))))
