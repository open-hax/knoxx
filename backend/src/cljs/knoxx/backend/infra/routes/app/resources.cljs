(ns knoxx.backend.infra.routes.app.resources
  "Compose resource, media, document and translation route groups."
  (:require [knoxx.backend.domain.text :as domain-text]
            [knoxx.backend.infra.auth.authz :as auth-authz]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.openplanner.memory :as openplanner-memory]
            [knoxx.backend.infra.routes.app.publication :as app-publication]
            [knoxx.backend.infra.routes.documents :as document-routes]
            [knoxx.backend.infra.routes.models :as model-routes]
            [knoxx.backend.infra.routes.resources :as resource-routes]
            [knoxx.backend.infra.routes.studio :as studio-routes]
            [knoxx.backend.infra.routes.translation :as translation-routes]
            [knoxx.backend.infra.routes.voice :as voice-routes]
            [knoxx.backend.infra.routes.workspace-media :as workspace-media-routes]
            [knoxx.backend.shape.app-shapes :as shape-app-shapes]))

(defn- register-document-and-workspace-routes!
  [app runtime config]
  (document-routes/register-document-routes! app runtime config
                                             {:route! shape-app-shapes/route!
                                              :json-response! infra-http/json-response!
                                              :error-response! infra-http/error-response!
                                              :with-request-context! auth-authz/with-request-context!
                                              :ensure-permission! auth-authz/ensure-permission!
                                              :clip-text domain-text/clip-text
                                              :openplanner-graph-export! openplanner-memory/openplanner-graph-export!
                                              :send-fetch-response! infra-http/send-fetch-response!
                                              :bearer-headers infra-http/bearer-headers
                                              :fetch-json infra-http/fetch-json
                                              :openai-auth-error infra-http/openai-auth-error
                                              :request-query-string infra-http/request-query-string})
  (workspace-media-routes/register-workspace-media-routes! app runtime config
                                                           {:route! shape-app-shapes/route!
                                                            :json-response! infra-http/json-response!
                                                            :error-response! infra-http/error-response!
                                                            :with-request-context! auth-authz/with-request-context!
                                                            :ensure-tool! auth-authz/ensure-tool!}))

(defn- register-studio-route!
  [app runtime config]
  (studio-routes/register-studio-routes! app runtime config
                                         {:route! shape-app-shapes/route!
                                          :json-response! infra-http/json-response!
                                          :error-response! infra-http/error-response!
                                          :with-request-context! auth-authz/with-request-context!
                                          :ensure-tool! auth-authz/ensure-tool!
                                          :ensure-permission! auth-authz/ensure-permission!
                                          :policy-db auth-authz/policy-db
                                          :policy-db-promise auth-authz/policy-db-promise}))

(defn register-resource-and-media-routes!
  "Compose the resource and media routes with their existing dependencies."
  [app runtime config]
  (resource-routes/register-resource-routes! app runtime config
                                             {:route! shape-app-shapes/route!
                                              :json-response! infra-http/json-response!
                                              :error-response! infra-http/error-response!
                                              :with-request-context! auth-authz/with-request-context!
                                              :ensure-permission! auth-authz/ensure-permission!})
  (app-publication/register-publication-surface-routes! app runtime config)
  (model-routes/register-model-routes! app runtime config)
  (voice-routes/register-voice-routes! app runtime config
                                       {:route! shape-app-shapes/route!
                                        :json-response! infra-http/json-response!
                                        :with-request-context! auth-authz/with-request-context!
                                        :ensure-tool! auth-authz/ensure-tool!})
  (register-document-and-workspace-routes! app runtime config)
  (register-studio-route! app runtime config))

(defn register-translation-route-group!
  "Compose the translation route group with their existing dependencies."
  [app runtime config]
  (translation-routes/register-translation-routes! app runtime config
                                                   {:json-response! infra-http/json-response!
                                                    :error-response! infra-http/error-response!
                                                    :with-request-context! auth-authz/with-request-context!
                                                    :ensure-permission! auth-authz/ensure-permission!
                                                    :ctx-user-id auth-authz/ctx-user-id
                                                    :ctx-user-email auth-authz/ctx-user-email
                                                    :ctx-org-id auth-authz/ctx-org-id}))
