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
            [knoxx.backend.infra.http :refer [http-error]]))

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

(defn- translation-segments-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (let [q (query request)]
      (openplanner-client/translation-segments!
       (op-client config)
       {:project (or (aget q "project") (:session-project-name config))
        :org_id (org-id! ctx ctx-org-id)
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
          body-with-auth (merge body {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                      :labeler_email (str (or (ctx-user-email ctx) "unknown"))
                                      :org_id (org-id! ctx ctx-org-id)})]
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
          body-with-auth (merge (body-clj request)
                                {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                 :labeler_email (str (or (ctx-user-email ctx) "unknown"))
                                 :org_id (org-id! ctx ctx-org-id)})]
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
                          (openplanner-client/translation-document!
                           (op-client config)
                           (aget (params request) "documentId")
                           (aget (params request) "targetLang")
                           (org-scope ctx ctx-org-id))))
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

(defn- update-batch-status-op
  [config]
  (fn [request ctx {:keys [ctx-org-id]}]
    (openplanner-client/update-translation-batch-status!
     (op-client config)
     (aget (params request) "id")
     (assoc (body-clj request) :org_id (org-id! ctx ctx-org-id)))))

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