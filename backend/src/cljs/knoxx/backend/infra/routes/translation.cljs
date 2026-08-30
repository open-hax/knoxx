(ns knoxx.backend.infra.routes.translation
  ;; NOTE: We import route! directly from app-shapes instead of receiving it as a parameter
  ;; to avoid a shadow-cljs :simple optimization bug where local bindings ending with `!`
  ;; get incorrectly compiled as namespace property references instead of closure captures.
  ;;
  ;; BUG: shadow-cljs :optimizations :simple generates buggy code for local bindings named
  ;; with `!` suffix. When route! is passed as a destructured parameter, shadow-cljs generates
  ;; calls OUTSIDE the function body that reference undefined namespace properties like
  ;; `knoxx.backend.translation_routes.route_BANG_` instead of the local variable.
  ;;
  ;; WORKAROUND: Import `route!` directly via :refer instead of passing through parameter maps.
  ;; See backend/README.md "Cannot read properties of undefined" section for full diagnosis.
  (:require [clojure.string :as str]
            [knoxx.backend.shape.app-shapes :refer [route!]]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.http :refer [http-error]]
            [knoxx.backend.infra.routes.translation-dispatch :as translation-dispatch]
            [knoxx.backend.infra.stores.translation-evidence-registry :as evidence-registry]))

(defn- op-client
  [config]
  (openplanner-client/client config))

(defn- openplanner-ready?
  [config]
  (openplanner-client/enabled? (op-client config)))

(defn- reply-header!
  [^js reply name value]
  (.header reply name value))

(defn- query
  [request]
  (or (aget request "query") (js/Object.)))

(defn- params
  [request]
  (or (aget request "params") (js/Object.)))

(defn- body-clj
  [request]
  (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true))

(defn- org-id!
  [ctx ctx-org-id]
  (or (some-> (ctx-org-id ctx) str str/trim not-empty)
      (throw (http-error 403
                         "organization_context_required"
                         "A nonblank organization context is required for translation operations"))))

(defn- membership-id!
  [ctx]
  (or (some-> (or (:membership-id ctx)
                   (:membershipId ctx)
                   (get-in ctx [:membership :id]))
               str str/trim not-empty)
      (throw (http-error 403
                         "membership_context_required"
                         "A nonblank membership context is required for translation batches"))))

(defn- org-scope
  [ctx ctx-org-id]
  {:org_id (org-id! ctx ctx-org-id)})

(defn- unavailable!
  [{:keys [json-response!]} reply]
  (json-response! reply 503 {:detail "OpenPlanner is not configured"}))

(defn ^:async execute-json-route!
  [request reply ctx handlers permission operation]
  (let [{:keys [json-response! error-response! ensure-permission!]} handlers]
    (try
      (when ctx (ensure-permission! ctx permission))
      (json-response! reply 200 (await (operation request ctx handlers)))
      (catch :default err
        (error-response! reply err)))))

(defn ^:async execute-ndjson-route!
  [request reply ctx handlers permission operation]
  (let [{:keys [error-response! ensure-permission!]} handlers]
    (try
      (when ctx (ensure-permission! ctx permission))
      (let [text (await (operation request ctx handlers))]
        (reply-header! reply "Content-Type" "application/x-ndjson")
        (.send reply text))
      (catch :default err
        (error-response! reply err)))))

(defn- register-json-route!
  [app method path runtime config handlers permission operation]
  (route! app method path
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (unavailable! handlers reply)
        ((:with-request-context! handlers) runtime request reply
         (fn [ctx]
           (execute-json-route! request reply ctx handlers permission operation)))))))

(defn- register-ndjson-route!
  [app method path runtime config handlers permission operation]
  (route! app method path
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (unavailable! handlers reply)
        ((:with-request-context! handlers) runtime request reply
         (fn [ctx]
           (execute-ndjson-route! request reply ctx handlers permission operation)))))))

(defn- read-org-id!
  "Organization a read is scoped to.

  A system admin may name an explicit organization, which is what
  `save_translation` already allows through agent resource policy. Without the
  same latitude on reads, a system-admin worker running a legacy batch in
  another tenant writes segments there and reads them back from its own
  membership's tenant. Every other principal stays pinned to its membership."
  [request ctx ctx-org-id]
  (let [requested (some-> (aget (query request) "org_id") str str/trim not-empty)]
    (if (and requested (authz/system-admin? ctx))
      requested
      (org-id! ctx ctx-org-id))))

(defn- translation-segments-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (let [q (query request)]
      (openplanner-client/translation-segments!
       (op-client config)
       {:project (or (aget q "project") (:session-project-name config))
        :org_id (read-org-id! request ctx ctx-org-id)
        :limit (or (aget q "limit") "50")
        :offset (or (aget q "offset") "0")
        :status (aget q "status")
        :source_lang (aget q "source_lang")
        :target_lang (aget q "target_lang")
        :domain (aget q "domain")}))))

(defn- label-segment-op
  [config]
  (fn [request ctx {:keys [ctx-user-id ctx-user-email ctx-org-id]}]
    (let [body (body-clj request)
          body-with-auth (merge body
                                {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                 :labeler_email (str (or (ctx-user-email ctx) "unknown"))
                                 :org_id (org-id! ctx ctx-org-id)
                                 :project (or (:project body)
                                              (:session-project-name config))})]
      (openplanner-client/label-translation-segment!
       (op-client config) (aget (params request) "id") body-with-auth))))

(defn- export-sft-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (let [q (query request)]
      (openplanner-client/translation-export-sft!
       (op-client config)
       {:project (or (aget q "project") (:session-project-name config))
        :org_id (org-id! ctx ctx-org-id)
        :target_lang (aget q "target_lang")
        :include_corrected (aget q "include_corrected")}))))

(defn- create-segments-batch-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (openplanner-client/create-translation-segments-batch!
     (op-client config)
     (assoc (body-clj request) :org_id (org-id! ctx ctx-org-id)))))

(defn- documents-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (let [q (query request)]
      (openplanner-client/translation-documents!
       (op-client config)
       {:project (or (aget q "project") (:session-project-name config))
        :org_id (org-id! ctx ctx-org-id)
        :target_lang (aget q "target_lang")
        :source_lang (aget q "source_lang")
        :garden_id (aget q "garden_id")}))))

(defn- review-document-op
  [config]
  (fn [request ctx {:keys [ctx-user-id ctx-user-email ctx-org-id]}]
    (let [p (params request)
          body (body-clj request)
          body-with-auth (merge body
                                {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                 :labeler_email (str (or (ctx-user-email ctx) "unknown"))
                                 :org_id (org-id! ctx ctx-org-id)
                                 :project (or (:project body)
                                              (:session-project-name config))})]
      (openplanner-client/review-translation-document!
       (op-client config) (aget p "documentId") (aget p "targetLang") body-with-auth))))

(defn- batches-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (let [q (query request)]
      (openplanner-client/translation-batches!
       (op-client config)
       {:status (aget q "status")
        :garden_id (aget q "garden_id")
        :target_lang (aget q "target_lang")
        :org_id (org-id! ctx ctx-org-id)}))))

(defn- register-segment-routes!
  [app runtime config handlers]
  (register-json-route! app "GET" "/api/translations/segments" runtime config handlers
                        "org.translations.read" (translation-segments-op config))
  (register-json-route! app "GET" "/api/translations/segments/:id" runtime config handlers
                        "org.translations.read"
                        (fn [request ctx {:keys [ctx-org-id]}]
                          (openplanner-client/translation-segment!
                           (op-client config)
                           (aget (params request) "id")
                           (org-scope ctx ctx-org-id))))
  (register-json-route! app "POST" "/api/translations/segments/:id/labels" runtime config handlers
                        "org.translations.review" (label-segment-op config))
  (register-json-route! app "POST" "/api/translations/segments/batch" runtime config handlers
                        "org.translations.manage" (create-segments-batch-op config)))

(defn- register-export-routes!
  [app runtime config handlers]
  (register-json-route! app "GET" "/api/translations/export/manifest" runtime config handlers
                        "org.translations.export"
                        (fn [request ctx {:keys [ctx-org-id]}]
                          (openplanner-client/translation-export-manifest!
                           (op-client config)
                           {:project (or (aget (query request) "project") (:session-project-name config))
                            :org_id (org-id! ctx ctx-org-id)})))
  (register-ndjson-route! app "GET" "/api/translations/export/sft" runtime config handlers
                          "org.translations.export" (export-sft-op config)))

(defn- register-document-routes!
  [app runtime config handlers]
  (register-json-route! app "GET" "/api/translations/documents" runtime config handlers
                        "org.translations.read" (documents-op config))
  (register-json-route! app "GET" "/api/translations/documents/:documentId/:targetLang" runtime config handlers
                        "org.translations.read"
                        (fn [request ctx {:keys [ctx-org-id]}]
                          (let [q (query request)]
                            (openplanner-client/translation-document!
                             (op-client config)
                             (aget (params request) "documentId")
                             (aget (params request) "targetLang")
                             {:org_id (org-id! ctx ctx-org-id)
                              :project (or (aget q "project")
                                           (:session-project-name config))
                              :garden_id (aget q "garden_id")}))))
  (register-json-route! app "POST" "/api/translations/documents/:documentId/:targetLang/review" runtime config handlers
                        "org.translations.review" (review-document-op config)))

(defn- create-batch-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (openplanner-client/create-translation-batch!
     (op-client config)
     (merge (body-clj request)
            {:org_id (org-id! ctx ctx-org-id)
             :membership_id (membership-id! ctx)}))))

(defn- next-batch-op
  "Claim the next queued batch. Worker-only.

  This is a mutation: it moves the oldest batch from `queued` to `processing`,
  and no status transition back to `queued` exists. `org.translations.manage` is
  held by org admins as well as system admins, so leaving the route open to the
  permission alone let any org admin permanently remove a batch from worker
  pickup — hiding the membership from the response addressed disclosure but not
  the queue mutation. Require a system-admin principal, which is also the only
  principal the owning `membership_id` is projected for."
  [config]
  (fn [_request ctx {:keys [ctx-org-id]}]
    (when-not (authz/system-admin? ctx)
      (throw (http-error 403
                         "worker_principal_required"
                         "Claiming a translation batch requires a system-admin worker principal")))
    (openplanner-client/next-translation-batch!
     (op-client config)
     (assoc (org-scope ctx ctx-org-id) :include_membership true))))

(defn- batch-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (openplanner-client/translation-batch!
     (op-client config)
     (aget (params request) "id")
     (org-scope ctx ctx-org-id))))

(defn worker-principal?
  "Whether `ctx` is the worker identity that may produce translation evidence.

  `org.translations.manage` guards this route, and org admins hold it too — the
  same gap `next-batch-op` already closed for batch claiming. Without this check
  an org admin could dispatch work and immediately POST `completed_document` for
  it, minting a completed-translation receipt for a translation that was never
  produced, and that fabricated receipt is exactly what a publication gate is
  waiting on. Evidence production is therefore restricted to a system-admin
  worker principal, which is the identity the ingestion worker actually runs as.

  Deliberately narrower than the route: an org admin may still update batch
  status, because that is the worker queue's own business. They simply cannot
  make Knoxx believe a translation exists."
  [ctx]
  (boolean (and ctx (authz/system-admin? ctx))))

(defn- ^:async resolve-evidence-safely!
  "Resolve the report, turning any failure into reportable evidence.

  The catch is what keeps this step from failing the status update it hangs off:
  see `resolve-translation-evidence!` for why that update must not be retried."
  [config evidence-store report]
  (try
    (await (translation-dispatch/resolve-batch-status!
            {:evidence-store evidence-store
             :clock (fn [] (.toISOString (js/Date.)))
             ;; Built here rather than inside the facade: the observer needs the
             ;; runtime config, and a facade that constructs its own
             ;; dependencies cannot be given a different one by a test.
             :observe-source-revision (translation-dispatch/source-revision-observer! config)}
            report))
    (catch :default err
      {:translation/error (or (not-empty (str (ex-message err)))
                              "translation evidence resolution failed")})))

(defn ^:async resolve-translation-evidence!
  "Turn the worker's status report into Knoxx-side translation evidence.

  This is the completion half of `knoxx-translation-work-dispatch`, and it hangs
  off this route deliberately rather than off a new endpoint. The worker already
  calls here after every document; a dedicated Knoxx callback would have meant
  teaching another repository a new call for no behavioral gain.

  Never allowed to fail the status update. The batch status belongs to the
  worker's own queue and has already been recorded by the time this runs, so
  throwing would make the worker retry a status transition that already
  succeeded — and `next` has no path back from `processing` to `queued`, so a
  retry storm here could strand the batch. The outcome travels in the response
  instead, which keeps it observable rather than silent: an operator debugging a
  translation that never produced a receipt can see the refusal that stopped it."
  [config ctx report]
  (let [evidence-store (evidence-registry/current)]
    (cond
      (not (worker-principal? ctx))
      {:translation/skipped {:reason :worker-principal-required}}

      (nil? evidence-store)
      {:translation/skipped {:reason :translation-evidence-unavailable}}

      :else
      (await (resolve-evidence-safely! config evidence-store report)))))

(defn- ^:async update-batch-status!
  "Record the batch status with its owner, then resolve Knoxx's own evidence.

  Strictly in that order. The status transition belongs to the worker's queue
  and must not be made conditional on Knoxx's bookkeeping succeeding."
  [config request ctx ctx-org-id]
  (let [batch-id (aget (params request) "id")
        body (body-clj request)
        response (await (openplanner-client/update-translation-batch-status!
                         (op-client config)
                         batch-id
                         (assoc body :org_id (org-id! ctx ctx-org-id))))
        ;; The report carries the batch id from the route, not the body: the
        ;; worker does not repeat it, and the binding is keyed by it.
        evidence (await (resolve-translation-evidence!
                         config ctx (assoc body :batch_id batch-id)))]
    (assoc response :translation evidence)))

(defn- update-batch-status-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (update-batch-status! config request ctx ctx-org-id)))

(defn- register-translation-batch-routes!
  [app runtime config handlers]
  (register-json-route! app "POST" "/api/translations/batches" runtime config handlers
                        "org.translations.manage" (create-batch-op config))
  (register-json-route! app "GET" "/api/translations/batches" runtime config handlers
                        "org.translations.read" (batches-op config))
  (register-json-route! app "GET" "/api/translations/batches/next" runtime config handlers
                        "org.translations.manage" (next-batch-op config))
  (register-json-route! app "GET" "/api/translations/batches/:id" runtime config handlers
                        "org.translations.read" (batch-op config))
  (register-json-route! app "POST" "/api/translations/batches/:id/status" runtime config handlers
                        "org.translations.manage" (update-batch-status-op config)))

(defn register-translation-routes!
  [app runtime config handlers]
  (register-segment-routes! app runtime config handlers)
  (register-export-routes! app runtime config handlers)
  (register-document-routes! app runtime config handlers)
  (register-translation-batch-routes! app runtime config handlers))
