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
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fastify :as fastify]
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

(def RequestBody
  "Contract for the raw wire body, checked BEFORE anything is reshaped.

   This is the closed one, and it has to be. Validating only the map this
   adapter *constructs* cannot refuse an unknown field, because an unknown field
   is simply never copied into it: `{\"documnet\": \"knoxx.docs/probe\"}` produced
   an empty decoded map, which is a valid request to translate the entire
   corpus. The typo silently became the most expensive operation this route has.

   So the body is checked first. `:document` is optional — absence is how an
   operator asks for a whole-corpus sweep — and when present it must be a
   non-blank string."
  [:map {:closed true}
   [:document {:optional true} [:and :string [:fn {:error/message "a named document may not be blank"}
                                              #(seq (str/trim %))]]]])

(def DecodedRequest
  "Contract for the data this adapter hands inward, after decoding.

   `:document` is optional and, when present, must be a qualified keyword. Not
   `[:maybe ...]`: a nil document is not a way of saying 'all documents', it is a
   caller that meant to name one and sent nothing. Absence is the only way to ask
   for a whole-corpus sweep."
  [:map {:closed true}
   [:document {:optional true} :qualified-keyword]])

(defn decode-request
  "Validate the native request body, then project it onto what the facade reads.

   Both contracts run, and they catch different things. `RequestBody` refuses an
   unknown or blank field on the wire; `DecodedRequest` refuses an identity that
   decoded to something the resource index is not keyed by — a bare `probe`
   becomes the unqualified `:probe`, which is a different document from
   `:knoxx.docs/probe` and would sweep nothing while reporting success."
  [request]
  (let [body (publication-law/assert-valid! :translation-dispatch/body
                                            RequestBody
                                            (fastify/request-body request))]
    (publication-law/assert-valid!
     :translation-dispatch/request
     DecodedRequest
     (cond-> {}
       (contains? body :document)
       (assoc :document (resource-identity/decode-keyword (:document body)))))))

(defn- scope
  "The acting principal's dispatch scope, plus the project batches are filed in.

   The organization and membership are required by
   `law.translation-dispatch/DispatchContext` and neither is defaulted: a batch
   filed under an invented organization or a blank membership is unattributable,
   and the worker's own batch contract requires both to be non-blank.

   `:project` is the configured `:session-project-name`, and omitting it was a
   real defect rather than a tidiness issue. With no project the OpenPlanner
   batch store defaults to `\"devel\"`, while every existing translation surface —
   segments, documents, export — filters by `:session-project-name`, whose
   default is `\"knoxx-session\"`. A dispatched translation therefore succeeded and
   then vanished from the review and export flow, because it had been written to
   a project nothing reads."
  [config ctx]
  (let [org-id (some-> (authz/ctx-org-id ctx) str not-empty)
        membership-id (some-> (authz/ctx-membership-id ctx) str not-empty)]
    (when-not (and org-id membership-id)
      (throw (ex-info "translation dispatch requires an organization and membership context"
                      {:status 403
                       :code "dispatch_context_required"})))
    (cond-> {:org-id org-id :membership-id membership-id}
      (some-> (:session-project-name config) str not-empty)
      (assoc :project (str (:session-project-name config))))))

(defn- ^:async dispatch!
  [config ctx decoded]
  (if-let [evidence-store (registry/current)]
    (facade/dispatch-translations!
     config
     {:evidence-store evidence-store
      :client (openplanner-client/client config)
      :clock (fn [] (.toISOString (js/Date.)))}
     (scope config ctx)
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
