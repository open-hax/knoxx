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

(defn- register-segment-read-routes!
  [app runtime config {:keys [json-response! error-response! with-request-context! ensure-permission!]}]
  (route! app "GET" "/api/translations/segments"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [query (or (aget request "query") (js/Object.))]
              (-> (openplanner-client/translation-segments!
                   (op-client config)
                   {:project (or (aget query "project") (:session-project-name config))
                    :limit (or (aget query "limit") "50")
                    :offset (or (aget query "offset") "0")
                    :status (aget query "status")
                    :source_lang (aget query "source_lang")
                    :target_lang (aget query "target_lang")
                    :domain (aget query "domain")})
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))
  (route! app "GET" "/api/translations/segments/:id"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (-> (openplanner-client/translation-segment! (op-client config) (aget request "params" "id"))
                (.then (fn [body] (json-response! reply 200 body)))
                (.catch (fn [err] (error-response! reply err))))))))))

(defn- register-segment-write-export-routes!
  [app runtime config {:keys [json-response! error-response! with-request-context! ensure-permission! ctx-user-id ctx-user-email ctx-org-id]}]
  (route! app "POST" "/api/translations/segments/:id/labels"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.review"))
            (let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
                  body-with-auth (merge body {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                              :labeler_email (str (or (ctx-user-email ctx) "unknown"))
                                              :org_id (str (or (ctx-org-id ctx) ""))})]
              (-> (openplanner-client/label-translation-segment! (op-client config) (aget request "params" "id") body-with-auth)
                  (.then (fn [resp] (json-response! reply 200 resp)))
                  (.catch (fn [err] (error-response! reply err))))))))))
  (route! app "GET" "/api/translations/export/manifest"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.export"))
            (let [query (or (aget request "query") (js/Object.))]
              (-> (openplanner-client/translation-export-manifest! (op-client config) (or (aget query "project") (:session-project-name config)))
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))
  (route! app "GET" "/api/translations/export/sft"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.export"))
            (let [query (or (aget request "query") (js/Object.))]
              (-> (openplanner-client/translation-export-sft!
                   (op-client config)
                   {:project (or (aget query "project") (:session-project-name config))
                    :target_lang (aget query "target_lang")
                    :include_corrected (aget query "include_corrected")})
                  (.then (fn [text]
                           (reply-header! reply "Content-Type" "application/x-ndjson")
                           (.send reply text)))
                  (.catch (fn [err] (error-response! reply err)))))))))))

(defn- register-segment-batch-route!
  [app runtime config {:keys [json-response! error-response! with-request-context! ensure-permission! ctx-org-id]}]
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
                  (.catch (fn [err] (error-response! reply err)))))))))))

(defn- register-document-routes!
  [app runtime config {:keys [json-response! error-response! with-request-context! ensure-permission! ctx-user-id ctx-user-email]}]
  (route! app "GET" "/api/translations/documents"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [query (or (aget request "query") (js/Object.))]
              (-> (openplanner-client/translation-documents!
                   (op-client config)
                   {:project (or (aget query "project") (:session-project-name config))
                    :target_lang (aget query "target_lang")
                    :source_lang (aget query "source_lang")
                    :garden_id (aget query "garden_id")})
                  (.then (fn [body] (json-response! reply 200 body)))
                  (.catch (fn [err] (error-response! reply err))))))))))
  (route! app "GET" "/api/translations/documents/:documentId/:targetLang"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (-> (openplanner-client/translation-document! (op-client config) (aget request "params" "documentId") (aget request "params" "targetLang"))
                (.then (fn [body] (json-response! reply 200 body)))
                (.catch (fn [err] (error-response! reply err)))))))))
  (route! app "POST" "/api/translations/documents/:documentId/:targetLang/review"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.review"))
            (let [body (js->clj (or (aget request "body") (js/Object.)) :keywordize-keys true)
                  body-with-auth (merge body {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                              :labeler_email (str (or (ctx-user-email ctx) "unknown"))})]
              (-> (openplanner-client/review-translation-document! (op-client config) (aget request "params" "documentId") (aget request "params" "targetLang") body-with-auth)
                  (.then (fn [resp] (json-response! reply 200 resp)))
                  (.catch (fn [err] (error-response! reply err)))))))))))

(defn- register-translation-batch-routes!
  [app runtime config {:keys [json-response! error-response! with-request-context! ensure-permission!]}]
  (route! app "POST" "/api/translations/batches"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.manage"))
            (-> (openplanner-client/create-translation-batch! (op-client config) (aget request "body"))
                (.then (fn [resp] (json-response! reply 200 resp)))
                (.catch (fn [err] (error-response! reply err)))))))))
  (route! app "GET" "/api/translations/batches"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.read"))
            (let [query (or (aget request "query") (js/Object.))]
              (-> (openplanner-client/translation-batches!
                   (op-client config)
                   {:status (aget query "status")
                    :garden_id (aget query "garden_id")
                    :target_lang (aget query "target_lang")})
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
            (-> (openplanner-client/translation-batch! (op-client config) (aget request "params" "id"))
                (.then (fn [body] (json-response! reply 200 body)))
                (.catch (fn [err] (error-response! reply err)))))))))
  (route! app "POST" "/api/translations/batches/:id/status"
    (fn [request reply]
      (if-not (openplanner-ready? config)
        (json-response! reply 503 {:detail "OpenPlanner is not configured"})
        (with-request-context! runtime request reply
          (fn [ctx]
            (when ctx (ensure-permission! ctx "org.translations.manage"))
            (-> (openplanner-client/update-translation-batch-status! (op-client config) (aget request "params" "id") (aget request "body"))
                (.then (fn [resp] (json-response! reply 200 resp)))
                (.catch (fn [err] (error-response! reply err))))))))))

(defn register-translation-routes!
  [app runtime config handlers]
  (register-segment-read-routes! app runtime config handlers)
  (register-segment-write-export-routes! app runtime config handlers)
  (register-segment-batch-route! app runtime config handlers)
  (register-document-routes! app runtime config handlers)
  (register-translation-batch-routes! app runtime config handlers))
