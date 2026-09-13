(ns knoxx.backend.extern.fastify.cms-documents
  (:require [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.routes.cms-documents :as documents]
            [knoxx.backend.law.error-body :as error-body]))
(defn register! [app runtime handlers]
  (doseq [[method path permission operation]
          [["GET" "/api/cms/documents" "org.publications.read" (fn [org req] (documents/list! org (into {} (map (fn [[k v]] [(keyword k) v])) (fastify/request-query-string-map req))))]
           ["GET" "/api/cms/documents/:id" "org.publications.read" (fn [org req] (documents/read! org (:id (fastify/request-params req))))]
           ["POST" "/api/cms/documents" "org.publications.manage" (fn [org req] (documents/save! org nil (fastify/request-body req)))]
           ["PATCH" "/api/cms/documents/:id" "org.publications.manage" (fn [org req] (documents/save! org (:id (fastify/request-params req)) (fastify/request-body req)))]]]
    ((:route! handlers) app method path
     (fn [request reply]
       ((:with-request-context! handlers) runtime request reply
        (^:async fn [ctx]
          (try
            ((:ensure-permission! handlers) ctx permission)
            (let [org (authz/ctx-org-id ctx)]
              (when-not org (throw (ex-info "Organization is required" {:status 403})))
              (fastify/send-json! reply 200 (await (operation (str org) request))))
            (catch :default e
              (let [status (or (:status (ex-data e)) (fastify/error-status e 500))]
                (when (= status 500) (fastify/log-unclassified-failure! "cms-documents" e))
                (fastify/send-json! reply status (error-body/error-body e status)))))))))))
