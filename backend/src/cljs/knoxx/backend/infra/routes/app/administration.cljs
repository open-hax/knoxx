(ns knoxx.backend.infra.routes.app.administration
  "Compose administration, memory, tooling and actor route groups."
  (:require [knoxx.backend.domain.realtime :as domain-realtime]
            [knoxx.backend.domain.text :as domain-text]
            [knoxx.backend.domain.time :as domain-time]
            [knoxx.backend.infra.agent.runtime :as agent-runtime]
            [knoxx.backend.infra.auth.authz :as auth-authz]
            [knoxx.backend.infra.auth.session-guards :as guards]
            [knoxx.backend.infra.core-memory :as infra-core-memory]
            [knoxx.backend.infra.http :as infra-http]
            [knoxx.backend.infra.openplanner.memory :as openplanner-memory]
            [knoxx.backend.infra.routes.actors :as actor-routes]
            [knoxx.backend.infra.routes.admin :as admin-routes]
            [knoxx.backend.infra.routes.memory :as memory-routes]
            [knoxx.backend.infra.routes.tools :as tool-routes]
            [knoxx.backend.infra.stores.session-titles :as stores-session-titles]
            [knoxx.backend.infra.tooling :as infra-tooling]
            [knoxx.backend.shape.app-shapes :as shape-app-shapes]
            [knoxx.backend.shape.parse :as shape-parse]))

(defn- session-title-dependencies []
  {                                          :start-session-title-backfill! stores-session-titles/start-session-title-backfill!
                                          :session-title-backfill* stores-session-titles/session-title-backfill*
                                          :session-titles* stores-session-titles/session-titles*
                                          :get-cached-session-title! stores-session-titles/get-cached-session-title!
                                          :fetch-openplanner-session-rows! infra-core-memory/fetch-openplanner-session-rows!
                                          :session-title-seed-text stores-session-titles/session-title-seed-text
                                          :heuristic-session-title stores-session-titles/heuristic-session-title
                                          :stored-session-title-entry stores-session-titles/stored-session-title-entry
                                          :cache-session-title-entry! stores-session-titles/cache-session-title-entry!
                                          :resolve-session-title! stores-session-titles/resolve-session-title!
                                          :cache-session-title! stores-session-titles/cache-session-title!
                                          :normalize-session-title stores-session-titles/normalize-session-title
})

(defn- memory-route-dependencies [lounge-messages*]
  (merge (session-title-dependencies)
         {:route! shape-app-shapes/route!
                                          :json-response! infra-http/json-response!
                                          :error-response! infra-http/error-response!
                                          :with-request-context! auth-authz/with-request-context!
                                          :ensure-permission! auth-authz/ensure-permission!
                                          :parse-positive-int shape-parse/parse-positive-int
                                          :truthy-param? shape-parse/truthy-param?
                                          :session-visible? infra-core-memory/session-visible?
                                          :session-matches-page-actor-filter? infra-core-memory/session-matches-page-actor-filter?
                                          :openplanner-memory-search! openplanner-memory/openplanner-memory-search!
                                          :filter-authorized-memory-hits! infra-core-memory/filter-authorized-memory-hits!
                                          :ctx-permitted? auth-authz/ctx-permitted?
                                          :system-admin? auth-authz/system-admin?
                                          :http-error infra-http/http-error
                                          :now-iso domain-time/now-iso
                                          :broadcast-ws! domain-realtime/broadcast-ws!
                                          :lounge-messages* lounge-messages*
                                          :authorized-session-ids! infra-core-memory/authorized-session-ids!}))

(defn register-admin-and-memory-routes!
  "Compose the admin and memory routes with their existing dependencies."
  [app runtime config lounge-messages*]
  (admin-routes/register-admin-routes! app runtime
                                       {:route! shape-app-shapes/route!
                                        :json-response! infra-http/json-response!
                                        :with-request-context! auth-authz/with-request-context!
                                        :ensure-permission! auth-authz/ensure-permission!
                                        :ensure-any-permission! auth-authz/ensure-any-permission!
                                        :ensure-org-scope! auth-authz/ensure-org-scope!
                                        :policy-db auth-authz/policy-db
                                        :policy-db-promise auth-authz/policy-db-promise
                                        :http-error infra-http/http-error})
  (memory-routes/register-memory-routes!
   app runtime config (memory-route-dependencies lounge-messages*)))

(defn register-tooling-route-groups!
  "Compose the tooling route groups with their existing dependencies."
  [app runtime config]
  (let [session-guard          (guards/make-session-guard runtime)
        optional-session-guard (guards/make-optional-session-guard runtime)]
    (tool-routes/register-tool-routes! app runtime config
                                       {:route! shape-app-shapes/route!
                                        :json-response! infra-http/json-response!
                                        :error-response! infra-http/error-response!
                                        :with-request-context! auth-authz/with-request-context!
                                        :ensure-permission! auth-authz/ensure-permission!
                                        :tool-catalog infra-tooling/tool-catalog
                                        :ensure-role-can-use! infra-tooling/ensure-role-can-use!
                                        :resolve-workspace-path agent-runtime/resolve-workspace-path
                                        :count-occurrences domain-text/count-occurrences
                                        :replace-first domain-text/replace-first
                                        :clip-text domain-text/clip-text
                                        :session-guard session-guard
                                        :optional-session-guard optional-session-guard})
    (actor-routes/register-actor-routes! app runtime config
                                         {:route! shape-app-shapes/route!
                                          :json-response! infra-http/json-response!
                                          :error-response! infra-http/error-response!
                                          :with-request-context! auth-authz/with-request-context!
                                          :ensure-permission! auth-authz/ensure-permission!
                                          :session-guard session-guard})))
