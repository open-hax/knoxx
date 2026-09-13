(ns knoxx.backend.infra.routes.app.publication
  "Compose provider-selected publication and translation admission routes."
  (:require [knoxx.backend.extern.fastify.cms-publication :as cms-publication-routes]
            [knoxx.backend.extern.fastify.document-admission :as document-admission-routes]
            [knoxx.backend.extern.fastify.publications :as publication-routes]
            [knoxx.backend.extern.fastify.translation-config :as translation-config-routes]
            [knoxx.backend.extern.fastify.translation-dispatch :as translation-dispatch-routes]
            [knoxx.backend.extern.fastify.translation-review :as translation-review-routes]
            [knoxx.backend.extern.fastify.wiki :as wiki-routes]
            [knoxx.backend.extern.wiki-changes :as wiki-changes]
            [knoxx.backend.infra.auth.authz :as auth-authz]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.publication-admission-hook :as publication-admission-hook]
            [knoxx.backend.infra.routes.document-admission :as document-admission]
            [knoxx.backend.infra.routes.publication-reconcile :as reconcile-routes]
            [knoxx.backend.infra.stores.translation-evidence-registry :as translation-evidence-registry]
            [knoxx.backend.infra.stores.translation-split-registry :as translation-split-registry]
            [knoxx.backend.infra.translation-event-writer :as translation-event-writer]
            [knoxx.backend.shape.app-shapes :as shape-app-shapes]))

(defn- ^:async repair-publication-translation-events!
  [client scope]
  (let [evidence-store (translation-evidence-registry/current)
        split-store (translation-split-registry/current)]
    (when-not evidence-store
      (throw (ex-info "translation evidence persistence is not configured"
                      {:status 503
                       :code "translation_evidence_unavailable"})))
    (when-not split-store
      (throw (ex-info "translation split persistence is not configured"
                      {:status 503
                       :code "translation_split_persistence_unavailable"})))
    (await
     (translation-event-writer/repair-completed-event-projections!
      {:evidence-store evidence-store
       :openplanner-client client
       :split-store split-store}
      (select-keys scope [:org-id :project])))))

(defn- internal-admission
  [config client admission-dependencies]
        (fn [scope selection]
          (document-admission/admit-documents!
           config
           (assoc admission-dependencies
                  :dispatch-document!
                  (fn [document-id snapshot-deps]
                    (translation-dispatch-routes/dispatch-selection-for-scope!
                     config scope {:document document-id}
                     (assoc snapshot-deps :client client))))
           scope selection)))

(defn- register-translation-review-routes!
  [app runtime config helpers admission-dependencies]
    (translation-config-routes/register-translation-config-routes!
     app runtime config helpers)
    (document-admission-routes/register-document-admission-routes!
     app runtime config (select-keys helpers [:with-request-context!
                                              :ensure-permission!])
     admission-dependencies)
    (translation-dispatch-routes/register-translation-dispatch-routes!
     app runtime config (select-keys helpers [:with-request-context!
                                              :ensure-permission!]))
    (translation-review-routes/register-translation-review-routes!
     app runtime config (select-keys helpers [:with-request-context!
                                              :ensure-permission!])))

(defn- publication-route-dependencies []
  {:route! shape-app-shapes/route!
                 :json-response! infra-http/json-response!
                 :with-request-context! auth-authz/with-request-context!
                 :ensure-permission! auth-authz/ensure-permission!})

(defn register-publication-surface-routes!
  "The contract-owned publication surface: the resource projection, the CMS
   editor's view of it, and translation configuration. None of these is gated on
   a hosted publishing backend being reachable — resolving desired state with
   that backend absent is the whole point."
  [app runtime config]
  (let [client (openplanner-client/client config)
        helpers (publication-route-dependencies)
        admission-dependencies
        {:client client
         :repair-translation-events!
         (partial repair-publication-translation-events! client)}
        internal-admission!
        (internal-admission config client admission-dependencies)]
    ;; Generated draft tools call this cycle-free port. The handler derives all
    ;; model work from the server-pinned scope carried on the originating
    ;; admission event; no HTTP credential or agent-supplied identity is used.
    (publication-admission-hook/register! internal-admission!)
    ;; Fastify interop is owned by each extern adapter, which authorizes before
    ;; touching the filesystem-backed projection.
    (publication-routes/register-publication-routes!
     app runtime config (select-keys helpers [:with-request-context!
                                              :ensure-permission!]))
    (cms-publication-routes/register-cms-publication-routes! app runtime config helpers)
    (wiki-routes/register-wiki-routes! app runtime config)
    (wiki-changes/register! app runtime config)
    (reconcile-routes/register-publication-reconcile-routes!
     app runtime config (select-keys helpers [:with-request-context!
                                              :ensure-permission!]))
    (register-translation-review-routes! app runtime config helpers admission-dependencies)))
