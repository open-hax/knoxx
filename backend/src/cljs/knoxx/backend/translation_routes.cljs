(ns knoxx.backend.translation-routes)

(defn register-translation-routes!
  [app runtime config {:keys [route!
                              json-response!
                              error-response!
                              with-request-context!
                              ensure-permission!
                              openplanner-enabled?
                              openplanner-request!
                              openplanner-url
                              openplanner-headers
                              ctx-user-id
                              ctx-user-email
                              ctx-org-id]}]
  (route! app "GET" "/api/translations/segments"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-permission! ctx "org.translations.read"))
                  (let [query (or (aget request "query") #js {})
                        project (or (aget query "project") (:session-project-name config))
                        status (aget query "status")
                        source-lang (aget query "source_lang")
                        target-lang (aget query "target_lang")
                        domain (aget query "domain")
                        limit (or (aget query "limit") "50")
                        offset (or (aget query "offset") "0")
                        params (str "project=" (js/encodeURIComponent project)
                                    "&limit=" limit
                                    "&offset=" offset
                                    (when status (str "&status=" (js/encodeURIComponent status)))
                                    (when source-lang (str "&source_lang=" (js/encodeURIComponent source-lang)))
                                    (when target-lang (str "&target_lang=" (js/encodeURIComponent target-lang)))
                                    (when domain (str "&domain=" (js/encodeURIComponent domain))))]
                    (-> (openplanner-request! config "GET" (str "/v1/translations/segments?" params))
                        (.then (fn [body]
                                 (json-response! reply 200 body)))
                        (.catch (fn [err]
                                  (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/segments/:id"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-permission! ctx "org.translations.read"))
                  (let [segment-id (aget request "params" "id")]
                    (-> (openplanner-request! config "GET" (str "/v1/translations/segments/" segment-id))
                        (.then (fn [body]
                                 (json-response! reply 200 body)))
                        (.catch (fn [err]
                                  (error-response! reply err))))))))))

  (route! app "POST" "/api/translations/segments/:id/labels"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-permission! ctx "org.translations.review"))
                  (let [segment-id (aget request "params" "id")
                        body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                        body-with-auth (merge body
                                              {:labeler_id (str (or (ctx-user-id ctx) "unknown"))
                                               :labeler_email (str (or (ctx-user-email ctx) "unknown"))
                                               :org_id (str (or (ctx-org-id ctx) ""))})]
                    (-> (openplanner-request! config
                                              "POST"
                                              (str "/v1/translations/segments/" segment-id "/labels")
                                              (clj->js body-with-auth))
                        (.then (fn [resp]
                                 (json-response! reply 200 resp)))
                        (.catch (fn [err]
                                  (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/export/manifest"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-permission! ctx "org.translations.export"))
                  (let [query (or (aget request "query") #js {})
                        project (or (aget query "project") (:session-project-name config))]
                    (-> (openplanner-request! config "GET" (str "/v1/translations/export/manifest?project=" (js/encodeURIComponent project)))
                        (.then (fn [body]
                                 (json-response! reply 200 body)))
                        (.catch (fn [err]
                                  (error-response! reply err))))))))))

  (route! app "GET" "/api/translations/export/sft"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-permission! ctx "org.translations.export"))
                  (let [query (or (aget request "query") #js {})
                        project (or (aget query "project") (:session-project-name config))
                        target-lang (aget query "target_lang")
                        include-corrected (aget query "include_corrected")
                        suffix (str "/v1/translations/export/sft?project=" (js/encodeURIComponent project)
                                    (when target-lang (str "&target_lang=" (js/encodeURIComponent target-lang)))
                                    (when include-corrected (str "&include_corrected=" (js/encodeURIComponent include-corrected))))]
                    (-> (js/fetch (openplanner-url config suffix)
                                  #js {:method "GET"
                                       :headers (openplanner-headers config)})
                        (.then (fn [resp]
                                 (-> (.text resp)
                                     (.then (fn [text]
                                              (if (aget resp "ok")
                                                (do
                                                  (.header reply "Content-Type" "application/x-ndjson")
                                                  (.send reply text))
                                                (json-response! reply (or (aget resp "status") 502)
                                                                {:detail text})))))))
                        (.catch (fn [err]
                                  (error-response! reply err))))))))))

  (route! app "POST" "/api/translations/segments/batch"
          (fn [request reply]
            (if-not (openplanner-enabled? config)
              (json-response! reply 503 {:detail "OpenPlanner is not configured"})
              (with-request-context! runtime request reply
                (fn [ctx]
                  (when ctx (ensure-permission! ctx "org.translations.manage"))
                  (let [body (js->clj (or (aget request "body") #js {}) :keywordize-keys true)
                        body-with-auth (assoc body
                                              :org_id (str (or (ctx-org-id ctx) "")))]
                    (-> (openplanner-request! config
                                              "POST"
                                              "/v1/translations/segments/batch"
                                              (clj->js body-with-auth))
                        (.then (fn [resp]
                                 (json-response! reply 200 resp)))
                        (.catch (fn [err]
                                  (error-response! reply err)))))))))))
