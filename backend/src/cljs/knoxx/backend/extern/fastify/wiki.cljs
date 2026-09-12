(ns knoxx.backend.extern.fastify.wiki
  "Authenticated HTTP adapter for the same Wiki commands exposed to agents."
  (:require [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.wiki-commands :as commands]
            [knoxx.backend.infra.wiki-publication :as publication]
            [knoxx.backend.law.error-body :as errors]
            [knoxx.backend.shape.resource-identity :as identity]
))

(defn- ^:async respond! [reply operation]
  (try
    (fastify/send-json! reply 200 (identity/encode-wire-values (await (operation))))
    (catch :default error
      (let [status (or (:status (ex-data error)) (fastify/error-status error nil) 500)]
        (when-not (errors/classified? status)
          (fastify/log-unclassified-failure! "wiki" error))
        (fastify/send-json! reply status (errors/error-body error status))))))

(defn- handler [server-runtime config operation]
  (^:async fn [request reply]
    (await (authz/with-request-context!
            server-runtime request reply
            (fn [ctx]
              (respond! reply #(operation config ctx
                                          (fastify/request-param request :documentId)
                                          (fastify/request-body request))))))))

(defn register-wiki-routes!
  "Register source editing, review, assistance and explicit publication controls."
  [app server-runtime config]
  (doseq [[method suffix operation]
          [["GET" "/review" (fn [cfg ctx document _body] (commands/read! cfg ctx document))]
           ["PATCH" "/source" commands/save!]
           ["POST" "/review" commands/review!]
           ["POST" "/assist" commands/assist!]
           ["GET" "/publications" (fn [cfg ctx document _body] (publication/list! cfg ctx document))]
           ["POST" "/publish" publication/publish!]]]
    (fastify/route! app {:method method :url (str "/api/publications/documents/:documentId" suffix)
                         :handler (handler server-runtime config operation)}))
  (fastify/route! app {:method "POST" :url "/api/publications/documents"
                       :handler (handler server-runtime config
                                         (fn [cfg ctx _document body] (commands/create! cfg ctx body)))}))
