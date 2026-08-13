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
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def read-permission "org.translations.read")
(def write-permission "org.translations.manage")

(defn decode-request
  [request]
  {:body (fastify/request-body request)
   :method (fastify/request-method request)})

(defn- error-status
  [err]
  (let [data (ex-data err)]
    (cond
      (contains? data :translation/model) 422
      (contains? data :expected) 409
      :else 500)))

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
        (json-response! reply
                        (error-status err)
                        (resource-identity/encode-wire-values
                         (cond-> {:detail (ex-message err)}
                           (some? (ex-data err)) (assoc :error (ex-data err)))))))))

(defn- ^:async guarded!
  "Authorize, then run. `ensure-permission!` throws, and the surrounding
   `respond!` turns that into the repository's standard error response — so an
   unauthorized request never reaches resource resolution or a write."
  [handlers ctx permission operation]
  (when ctx ((:ensure-permission! handlers) ctx permission))
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
                                     (translation-config/patch-config!
                                      config {:org-id (:org-id ctx)} patch)))))))))
    nil))
