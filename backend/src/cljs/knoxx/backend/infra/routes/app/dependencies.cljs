(ns knoxx.backend.infra.routes.app.dependencies
  "Application route dependency values captured at registration."
  (:require [knoxx.backend.domain.text :as domain-text]
            [knoxx.backend.infra.auth.authz :as auth-authz]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.shape.app-shapes :as shape-app-shapes]))

(def deps
  "Capture the dependency contract supplied to the application route macros."
  {:route! shape-app-shapes/route!
   :json-response! infra-http/json-response!
   :error-response! infra-http/error-response!
   :ensure-permission! auth-authz/ensure-permission!
   :clip-text domain-text/clip-text
   :with-request-context! auth-authz/with-request-context!
   :send-fetch-response! infra-http/send-fetch-response!
   :bearer-headers infra-http/bearer-headers
   :fetch-json infra-http/fetch-json
   :request-query-string infra-http/request-query-string
   :session-guard nil
   :optional-session-guard nil})
