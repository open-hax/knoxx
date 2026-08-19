(ns knoxx.backend.extern.fastify.translation-config
  "Fastify boundary for the Knoxx-owned translation config routes.

  Deliberately NOT gated on legacy-backend readiness, unlike the routes in
  `infra.routes.translation`: the whole point of this card is that translation
  configuration resolves with the legacy backend absent.

  Both routes authorize before doing any work, using the repository's
  `ensure-permission!` convention and the existing `org.translations.*`
  permission vocabulary. Read and write are deliberately separate permissions —
  reviewers read the pipeline config, but changing the authoritative model is a
  manage-level act."
  (:require [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.routes.translation-config :as translation-config]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def read-permission "org.translations.read")

(def write-permission
  "Deliberately a `platform.*` permission, not `org.translations.manage`.

   The resource this route writes is the *global* pipeline default, set by
   contract files and otherwise changed only by redeployment. An org-scoped
   permission would have let any tenant's administrator rewrite the default for
   every other tenant — and `platform.*` is already this repository's vocabulary
   for acts that are not scoped to one organization, held only by system_admin.

   Reading stays org-scoped: reviewers need the resolved config, which is theirs."
  "platform.translations.manage")

(defn decode-request
  [request]
  {:body (fastify/request-body request)
   :method (fastify/request-method request)})

(def ^:private patch-contract-ids
  "The two contracts a request body is decoded through. A violation of either is
   caused entirely by the caller, so it must not be reported as a server fault."
  #{:translation/patch-wire :translation/patch})

(defn- error-status
  "A status the error already carries wins over everything else: a denied request
   is a 403, and calling it a 500 tells the caller to retry something that can
   never succeed, while hiding access denial from monitoring.

   Otherwise: a malformed body is a 400, an unknown model is a 422 — the shape
   was fine, the value is not — a missing authoritative resource is a 409, and
   only a genuine resolution or write failure is a 500."
  [err]
  (let [data (ex-data err)]
    (or (:status data)
        (fastify/error-status err nil)
        (cond
          (contains? patch-contract-ids (:contract data)) 400
          (contains? data :translation/model) 422
          (contains? data :expected) 409
          :else 500))))

(defn ^:async respond!
  "Await an operation and send its CLJS result as JSON.

   `encode-wire-values` runs even though the wire contract is already
   JSON-scalar, so an error body's keyword values keep their namespace instead
   of being flattened by `clj->js`."
  [handlers reply operation]
  (let [{:keys [json-response!]} handlers]
    (try
      (json-response! reply 200 (resource-identity/encode-wire-values (await (operation))))
      (catch :default err
        (let [status (error-status err)]
          ;; See the note on the same catch in extern/fastify/publications.cljs:
          ;; the caller gets an opaque body for an unclassified failure, so the
          ;; detail has to be logged here or it is lost entirely.
          (when-not (error-body/classified? status)
            (js/console.error "[translations] unclassified failure:"
                              (or (ex-message err) (str err))
                              (pr-str (ex-data err))))
          (json-response! reply
                          status
                          (resource-identity/encode-wire-values
                           (error-body/error-body err status))))))))

(defn- ^:async guarded!
  "Authorize, then run. `ensure-permission!` throws, and the surrounding
   `respond!` turns that into the repository's standard error response — so an
   unauthorized request never reaches resource resolution or a write.

   The check is unconditional. `with-request-context!` hands down a nil context
   when the policy database is disabled, and skipping the check for that case
   would let an anonymous caller read the pipeline config and rewrite the
   authoritative model — the same hole that was closed on the publication routes
   in #230. A nil context fails closed instead of reading as permission."
  [handlers ctx permission operation]
  ((:ensure-permission! handlers) ctx permission)
  (await (operation)))

(defn register-translation-config-routes!
  [app runtime config handlers]
  (let [{:keys [route! with-request-context!]} handlers]
    (route! app "GET" "/api/translations/config"
      (fn [request reply]
        (with-request-context! runtime request reply
          (fn [ctx]
            (respond! handlers reply
                      #(guarded! handlers ctx read-permission
                                 (fn [] (translation-config/config-response!
                                         config {:org-id (:org-id ctx)}))))))))
    (route! app "PATCH" "/api/translations/config"
      (fn [request reply]
        (with-request-context! runtime request reply
          (fn [ctx]
            (respond! handlers reply
                      #(guarded! handlers ctx write-permission
                                 (fn []
                                   (let [wire (:body (decode-request request))
                                         patch (translation-config/decode-patch wire)]
                                     (translation-config/patch-config! config patch)))))))))
    nil))
