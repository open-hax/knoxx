(ns knoxx.backend.extern.fastify.translation-dispatch
  "Fastify boundary for translation dispatch.

  Native request and reply handles are born and die here; the facade this
  adapter calls receives decoded CLJS data and returns CLJS data.

  One route: an authorized operator asks Knoxx to dispatch whatever translation
  work the publication gate currently derives. The other direction — the
  worker's answer becoming evidence — is not a route of its own. The worker
  already reports batch status to `POST /api/translations/batches/:id/status`,
  and adding a second endpoint it would have to learn to call would be a
  coordinated change to another repository for no gain. See
  `infra.routes.translation` for where that report is resolved."
  (:require [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.routes.translation-dispatch :as facade]
            [knoxx.backend.infra.stores.translation-evidence-registry :as registry]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def dispatch-permission
  "Creating translation work is a queue mutation on a shared worker, so it takes
   the same permission as creating a batch directly —
   `infra.routes.translation`'s batch routes already gate on this."
  "org.translations.manage")

(def DecodedRequest
  "Contract for the data this adapter hands inward.

   `:document` is optional: absent means every document, which is the ordinary
   operator action. Closed, so a body carrying an unexpected field is refused
   rather than silently reinterpreted as a whole-corpus sweep."
  [:map {:closed true}
   [:document {:optional true} [:maybe :qualified-keyword]]])

(defn decode-request
  "Project the native request body onto the CLJS data the facade reads.

   The document arrives as a wire string and is decoded to a qualified keyword,
   because that is the identity the resource index is keyed by. A bare name with
   no namespace decodes to an unqualified keyword, which the contract refuses —
   `:probe` and `:knoxx.docs/probe` are different documents, and silently
   accepting the former would sweep nothing while reporting success."
  [request]
  (let [body (fastify/request-body request)
        document (:document body)]
    (publication-law/assert-valid!
     :translation-dispatch/request
     DecodedRequest
     (cond-> {}
       (and (some? document) (not= "" document))
       (assoc :document (resource-identity/decode-keyword document))))))

(defn- scope
  "The acting principal's dispatch scope.

   Both fields are required by `law.translation-dispatch/DispatchContext`, and
   neither is defaulted here: a batch filed under an invented organization or a
   blank membership is unattributable, and the worker's own batch contract
   requires both to be non-blank."
  [ctx]
  (let [org-id (some-> (authz/ctx-org-id ctx) str not-empty)
        membership-id (some-> (authz/ctx-membership-id ctx) str not-empty)]
    (when-not (and org-id membership-id)
      (throw (ex-info "translation dispatch requires an organization and membership context"
                      {:status 403
                       :code "dispatch_context_required"})))
    {:org-id org-id :membership-id membership-id}))

(defn- ^:async dispatch!
  [config ctx decoded]
  (if-let [evidence-store (registry/current)]
    (facade/dispatch-translations!
     config
     {:evidence-store evidence-store
      :client (openplanner-client/client config)
      :clock (fn [] (.toISOString (js/Date.)))}
     (scope ctx)
     (:document decoded))
    ;; No durable store means a dispatch whose revision binding would be lost.
    ;; Refusing is the only honest answer — see the registry's docstring.
    (throw (ex-info "translation evidence persistence is not configured"
                    {:status 503
                     :code "translation_evidence_unavailable"}))))

(defn- error-status
  "A status the error already carries wins; a contract violation is a 400,
   because the caller sent something inadmissible and a retry will not help."
  [err]
  (let [data (ex-data err)]
    (or (:status data)
        (fastify/error-status err nil)
        (cond
          (contains? data :contract) 400
          (contains? data :blockers) 409
          :else 500))))

(defn- ^:async send-result!
  [reply operation]
  (try
    (fastify/send-json! reply 200 (resource-identity/encode-wire-values
                                  (await (operation))))
    (catch :default err
      (let [status (error-status err)]
        ;; Withholding the detail from the caller must not withhold it from us.
        (when-not (error-body/classified? status)
          (fastify/log-unclassified-failure! "translation-dispatch" err))
        (fastify/send-json! reply status (error-body/error-body err status))))))

(defn register-translation-dispatch-routes!
  [app runtime config handlers]
  (fastify/route!
   app
   {:method "POST"
    :url "/api/publications/translations/dispatch"
    :handler
    (^:async fn [request reply]
      (await
       ((:with-request-context! handlers) runtime request reply
        (^:async fn [ctx]
          (await
           (send-result!
            reply
            (fn []
              ;; Authorize first, and unconditionally. `with-request-context!`
              ;; hands down a nil context when the policy database is disabled,
              ;; and reading that as permission would let an anonymous caller
              ;; enqueue translation work on a shared worker.
              ((:ensure-permission! handlers) ctx dispatch-permission)
              (dispatch! config ctx (decode-request request)))))))))})
  nil)
