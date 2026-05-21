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
  (:require [knoxx.backend.shape.app-shapes :refer [route!]]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]))

(defn- op-client
  [config]
  (openplanner-client/client config))

(defn- openplanner-ready?
  [config]
  (openplanner-client/enabled? (op-client config)))

(defn- reply-header!
  [^js reply name value]
  (.header reply name value))

(defn register-translation-routes!
  [app runtime config {:keys [json-response!
                              error-response!
                              with-request-context!
                              ensure-permission!
                              ctx-user-id
                              ctx-user-email
                              ctx-org-id]}]
  (route! app "GET" "/api/translations/segments"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [query (or (aget request "query") (js/Object.))
                  project (or (aget query "project") (:session-project-name config))
                  status (aget query "status")
                  source-lang (aget query "source_lang")
                  target-lang (aget query "target_lang")
                  domain (aget query "domain")
                  limit (or (aget query "limit") "50")
                  offset (or (aget query "offset") "0")]
              (-> (openplanner-client/translation-segments!
                   (op-client config)
                   {:project project
                    :limit limit
                    :offset offset
                    :status status
                    :source_lang source-lang
                    :target_lang target-lang
                    :domain domain})
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/segments/:id"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [segment-id (aget request "params" "id")]
              (-> (openplanner-client/translation-segment! (op-client config) segment-id)
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "POST" "/api/translations/segments/:id/labels"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.review"))
            (let [segment-id (aget request "params" "id")
                  body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
                  body-with-auth (merge body
                                        {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                         :labeler_email (str (or (ctx-user-email ctx) "unknown"))
                                         :org_id (str (or (ctx-org-id ctx) ""))})]
              (-> (openplanner-client/label-translation-segment! (op-client config) segment-id body-with-auth)
                  (.then (fn [resp] (json-response! reply 200 resp)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/export/manifest"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.export"))
            (let [query (or (aget request "query") (js/Object.))
                  project (or (aget query "project") (:session-project-name config))]
              (-> (openplanner-client/translation-export-manifest! (op-client config) project)
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/export/sft"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.export"))
            (let [query (or (aget request "query") (js/Object.))
                  project (or (aget query "project") (:session-project-name config))
                  target-lang (aget query "target_lang")
                  include-corrected (aget query "include_corrected")]
              (-> (openplanner-client/translation-export-sft!
                   (op-client config)
                   {:project project
                    :target_lang target-lang
                    :include_corrected include-corrected})
                  (.then (fn [text]
                           (reply-header! reply "Content-Type" "application/x-ndjson")
                           (.send reply text)))
                  (.catch (fn [err] (error-response! reply err)))))))))))

  (route! app "POST" "/api/translations/segments/batch"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.manage"))
            (let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
                  body-with-auth (assoc body :org_id (str (or (ctx-org-id ctx) "")))]
              (-> (openplanner-client/create-translation-segments-batch! (op-client config) body-with-auth)
                  (.then (fn [resp] (json-response! reply 200 resp)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  ;; Document-level routes
  (route! app "GET" "/api/translations/documents"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [query (or (aget request "query") (js/Object.))
                  project (or (aget query "project") (:session-project-name config))
                  target-lang (aget query "target_lang")
                  source-lang (aget query "source_lang")
                  garden-id (aget query "garden_id")]
              (-> (openplanner-client/translation-documents!
                   (op-client config)
                   {:project project
                    :target_lang target-lang
                    :source_lang source-lang
                    :garden_id garden-id})
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/documents/:documentId/:targetLang"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [doc-id (aget request "params" "documentId")
                  target-lang (aget request "params" "targetLang")]
              (-> (openplanner-client/translation-document! (op-client config) doc-id target-lang)
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "POST" "/api/translations/documents/:documentId/:targetLang/review"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.review"))
            (let [doc-id (aget request "params" "documentId")
                  target-lang (aget request "params" "targetLang")
                  body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
                  body-with-auth (merge body
                                        {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                         :labeler_email (str (or (ctx-user-email ctx) "unknown"))})]
              (-> (openplanner-client/review-translation-document! (op-client config) doc-id target-lang body-with-auth)
                  (.then (fn [resp] (json-response! reply 200 resp)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  ;; Batch routes
  (route! app "POST" "/api/translations/batches"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.manage"))
            (let [body (aget request "body")]
              (-> (openplanner-client/create-translation-batch! (op-client config) body)
                  (.then (fn [resp] (json-response! reply 200 resp)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/batches"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [query (or (aget request "query") (js/Object.))
                  status (aget query "status")
                  garden-id (aget query "garden_id")
                  target-lang (aget query "target_lang")]
              (-> (openplanner-client/translation-batches!
                   (op-client config)
                   {:status status
                    :garden_id garden-id
                    :target_lang target-lang})
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/batches/next"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.manage"))
            (-> (openplanner-client/next-translation-batch! (op-client config))
                (.then (fn [body] (json-response! reply 200 body)))
                (.catch (fn [err] (error-response! reply err)))))))))

  (route! app "GET" "/api/translations/batches/:id"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [batch-id (aget request "params" "id")]
              (-> (openplanner-client/translation-batch! (op-client config) batch-id)
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))

  (route! app "POST" "/api/translations/batches/:id/status"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.manage"))
            (let [batch-id (aget request "params" "id")
                  body (aget request "body")]
              (-> (openplanner-client/update-translation-batch-status! (op-client config) batch-id body)
                  (.then (fn [resp] (json-response! reply 200 resp)))
                  (.catch (fn [err] (error-response! reply err))))))))))
