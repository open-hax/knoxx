(ns knoxx.backend.infra.routes.resources
  "Register authorized resource routes and retain established public forwarding arities."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.resources.editing :as editing]
            [knoxx.backend.domain.resources.validation :as validation]
            [knoxx.backend.extern.resource-routes :as http]
            [knoxx.backend.infra.publication-admission-hook :as publication-admission-hook]
            [knoxx.backend.infra.resources.handlers :as handlers]
            [knoxx.backend.infra.resources.index :as index]
            [knoxx.backend.infra.resources.writes :as writes]))

(defn- with-route-context
  [runtime do-ctx do-err f]
  (fn [request reply]
    (do-ctx runtime request reply
      (fn [ctx]
        (try
          (f ctx request reply)
          (catch :default err
            (do-err reply err)))))))

(defn- agent-ui-actions-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [actor-id (or (http/query-value request "actor")
                         (http/query-value request "actor_id")
                         (http/query-value request "actorId"))
            surface (or (http/query-value request "surface")
                        (http/query-value request "surface_id")
                        (http/query-value request "surfaceId"))]
        (handlers/handle-ui-actions (partial do-json reply) config actor-id surface)))))

(defn- agent-list-contracts-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [safe-kind (editing/safe-contract-class (http/request-contract-class request "agents"))]
        (if-not (:ok safe-kind)
          (http/text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (handlers/handle-agent-list-contracts (partial http/text-response! reply) config (:class safe-kind)))))))

(defn- agent-validate-contract-route
  [runtime do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [body (http/body-map request)
            safe-kind (editing/safe-contract-class (http/body-contract-class body request "agents"))]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handlers/handle-agent-validate-contract-edn (partial do-json reply) (:class safe-kind) (http/body-edn-text body)))))))

(defn- agent-get-contract-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (http/params-value request "contractId") ""))
            safe (editing/safe-contract-id contract-id)
            safe-kind (editing/safe-contract-class (http/request-contract-class request "agents"))]
        (cond
          (str/blank? contract-id) (http/text-response! reply 400 ";; contractId is required")
          (not (:ok safe-kind)) (http/text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (not (:ok safe)) (http/text-response! reply 400 (str ";; Invalid contractId: " (:error safe)))
          :else (handlers/handle-agent-get-contract-edn (partial http/text-response! reply) config (:class safe-kind) (:id safe)))))))

(defn- agent-put-contract-route
  [runtime config do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (http/params-value request "contractId") ""))
            safe (editing/safe-contract-id contract-id)
            safe-kind (editing/safe-contract-class (http/request-contract-class request "agents"))
            edn-text (str (or (http/body-value request) ""))]
        (cond
          (str/blank? contract-id) (http/text-response! reply 400 ";; contractId is required")
          (not (:ok safe-kind)) (http/text-response! reply 400 (str ";; Invalid contract class: " (:error safe-kind)))
          (not (:ok safe)) (http/text-response! reply 400 (str ";; Invalid contractId: " (:error safe)))
          :else (handlers/handle-agent-put-contract-edn (partial http/text-response! reply) config
                                               (:class safe-kind) (:id safe) edn-text
                                               ctx publication-admission-hook/admit!))))))

(defn- register-agent-contract-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/contracts/ui-actions"
              (agent-ui-actions-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/agent/contracts"
              (agent-list-contracts-route runtime config do-err do-ctx do-perm))
    (do-route app "POST" "/api/agent/contracts/validate"
              (agent-validate-contract-route runtime do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/agent/contracts/:contractId"
              (agent-get-contract-route runtime config do-err do-ctx do-perm))
    (do-route app "PUT" "/api/agent/contracts/:contractId"
              (agent-put-contract-route runtime config do-err do-ctx do-perm))))

(defn- admin-list-resources-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [kind (http/request-resource-kind request nil)
            safe-kind (if kind (editing/safe-resource-class kind) {:ok true :class nil})]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (handlers/handle-list-resources (partial do-json reply) config (:class safe-kind)))))))

(defn- admin-get-resource-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [resource-id (str (or (http/params-value request "resourceId") ""))
            safe (editing/safe-resource-id resource-id)
            safe-kind (editing/safe-resource-class (http/request-resource-kind request "agents"))]
        (cond
          (str/blank? resource-id) (do-json reply 400 {:detail "resourceId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid resourceId" :error (:error safe)})
          :else (handlers/handle-get-resource (partial do-json reply) config (:class safe-kind) (:id safe)))))))

(defn- admin-save-resource-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [resource-id (str (or (http/params-value request "resourceId") ""))
            body (http/body-map request)
            safe (editing/safe-resource-id resource-id)
            safe-kind (editing/safe-resource-class (http/body-resource-kind body request "agents"))]
        (cond
          (str/blank? resource-id) (do-json reply 400 {:detail "resourceId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid resourceId" :error (:error safe)})
          :else (handlers/handle-save-resource (partial do-json reply) config (:class safe-kind) (:id safe)
                                      (http/body-edn-text body) ctx publication-admission-hook/admit!))))))

(defn- admin-validate-resource-route
  [runtime do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [body (http/body-map request)
            safe-kind (editing/safe-resource-class (http/body-resource-kind body "agents"))]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (handlers/handle-validate-resource (partial do-json reply) (:class safe-kind) (http/body-edn-text body)))))))

(defn- admin-copy-resource-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [source-id (str (or (http/params-value request "resourceId") ""))
            body (http/body-map request)
            new-id (str (or (:newId body) ""))
            safe-kind (editing/safe-resource-class (http/body-resource-kind body "agents"))
            safe-source (editing/safe-resource-id source-id)
            safe-new (editing/safe-resource-id new-id)]
        (cond
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid resource kind" :error (:error safe-kind)})
          (or (str/blank? source-id) (str/blank? new-id)) (do-json reply 400 {:detail "source resourceId and newId are required"})
          (not (:ok safe-source)) (do-json reply 400 {:detail "Invalid source resourceId" :error (:error safe-source)})
          (not (:ok safe-new)) (do-json reply 400 {:detail "Invalid newId" :error (:error safe-new)})
          :else (handlers/handle-copy-resource (partial do-json reply) config (:class safe-kind)
                                      (:id safe-source) (:id safe-new)
                                      ctx publication-admission-hook/admit!))))))

(defn- admin-list-contracts-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [kind (http/request-contract-class request nil)
            safe-kind (if kind (editing/safe-contract-class kind) {:ok true :class nil})]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handlers/handle-list-contracts (partial do-json reply) config (:class safe-kind)))))))

(defn- admin-get-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (when ctx (do-perm ctx "agent.chat.use"))
      (let [contract-id (str (or (http/params-value request "contractId") ""))
            safe (editing/safe-contract-id contract-id)
            safe-kind (editing/safe-contract-class (http/request-contract-class request "agents"))]
        (cond
          (str/blank? contract-id) (do-json reply 400 {:detail "contractId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})
          :else (handlers/handle-get-contract (partial do-json reply) config (:class safe-kind) (:id safe)))))))

(defn- admin-save-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [contract-id (str (or (http/params-value request "contractId") ""))
            body (http/body-map request)
            safe (editing/safe-contract-id contract-id)
            safe-kind (editing/safe-contract-class (http/body-contract-class body request "agents"))]
        (cond
          (str/blank? contract-id) (do-json reply 400 {:detail "contractId is required"})
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (not (:ok safe)) (do-json reply 400 {:detail "Invalid contractId" :error (:error safe)})
          :else (handlers/handle-save-contract (partial do-json reply) config (:class safe-kind) (:id safe)
                                      (http/body-edn-text body) ctx publication-admission-hook/admit!))))))

(defn- admin-validate-contract-route
  [runtime do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [body (http/body-map request)
            safe-kind (editing/safe-contract-class (http/body-contract-class body "agents"))]
        (if-not (:ok safe-kind)
          (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (handlers/handle-validate-contract (partial do-json reply) (:class safe-kind) (http/body-edn-text body)))))))

(defn- admin-copy-contract-route
  [runtime config do-json do-err do-ctx do-perm]
  (with-route-context runtime do-ctx do-err
    (fn [ctx request reply]
      (do-perm ctx "platform.org.create")
      (let [source-id (str (or (http/params-value request "contractId") ""))
            body (http/body-map request)
            new-id (str (or (:newId body) ""))
            safe-kind (editing/safe-contract-class (http/body-contract-class body "agents"))
            safe-source (editing/safe-contract-id source-id)
            safe-new (editing/safe-contract-id new-id)]
        (cond
          (not (:ok safe-kind)) (do-json reply 400 {:detail "Invalid contract class" :error (:error safe-kind)})
          (or (str/blank? source-id) (str/blank? new-id)) (do-json reply 400 {:detail "source contractId and newId are required"})
          (not (:ok safe-source)) (do-json reply 400 {:detail "Invalid source contractId" :error (:error safe-source)})
          (not (:ok safe-new)) (do-json reply 400 {:detail "Invalid newId" :error (:error safe-new)})
          :else (handlers/handle-copy-contract (partial do-json reply) config (:class safe-kind)
                                      (:id safe-source) (:id safe-new)
                                      ctx publication-admission-hook/admit!))))))

(defn- register-admin-resource-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/admin/resources"
              (admin-list-resources-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/admin/resources/:resourceId"
              (admin-get-resource-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "PUT" "/api/admin/resources/:resourceId"
              (admin-save-resource-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/resources/validate"
              (admin-validate-resource-route runtime do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/resources/:resourceId/copy"
              (admin-copy-resource-route runtime config do-json do-err do-ctx do-perm))))

(defn- register-admin-contract-routes!
  [app runtime config helpers]
  (let [do-route (:route! helpers)
        do-json (:json-response! helpers)
        do-err (:error-response! helpers)
        do-ctx (:with-request-context! helpers)
        do-perm (:ensure-permission! helpers)]
    (do-route app "GET" "/api/admin/contracts"
              (admin-list-contracts-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "GET" "/api/admin/contracts/:contractId"
              (admin-get-contract-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "PUT" "/api/admin/contracts/:contractId"
              (admin-save-contract-route runtime config do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/contracts/validate"
              (admin-validate-contract-route runtime do-json do-err do-ctx do-perm))
    (do-route app "POST" "/api/admin/contracts/:contractId/copy"
              (admin-copy-contract-route runtime config do-json do-err do-ctx do-perm))))

(defn register-resource-routes!
  "Register human resource routes and agent compatibility routes with shared authorization."
  [app runtime config helpers]
  (register-agent-contract-routes! app runtime config helpers)
  (register-admin-resource-routes! app runtime config helpers)
  (register-admin-contract-routes! app runtime config helpers)
  nil)

(defn register-contracts-routes!
  "Compatibility alias for old route registration."
  [app runtime config helpers]
  (register-resource-routes! app runtime config helpers))

;; Explicit defn arities preserve non-blocking calls from native async callers.
;; A bare def alias loses static arity information and can introduce awaited
;; generic dispatch before the caller can finish scheduling concurrent work.
(defn validate-resource-edn
  "Forward validate-resource-edn with its established call arities."
  [resource-class edn-text]
  (validation/validate-resource-edn resource-class edn-text))

(defn validate-contract-edn
  "Forward validate-contract-edn with its established call arities."
  [contract-class edn-text]
  (validation/validate-contract-edn contract-class edn-text))

(defn update-resource-id-in-edn-text
  "Forward update-resource-id-in-edn-text with its established call arities."
  [resource-class edn-text new-id]
  (editing/update-resource-id-in-edn-text resource-class edn-text new-id))

(defn sync-resource-index!
  "Forward sync-resource-index! with its established call arities."
  [config]
  (index/sync-resource-index! config))

(defn sync-contract-index!
  "Forward sync-contract-index! with its established call arities."
  [config]
  (index/sync-contract-index! config))

(defn handle-list-resources
  "Forward handle-list-resources with its established call arities."
  [do-json config resource-kind]
  (handlers/handle-list-resources do-json config resource-kind))

(defn handle-list-contracts
  "Forward handle-list-contracts with its established call arities."
  [do-json config contract-class]
  (handlers/handle-list-contracts do-json config contract-class))

(defn admission-document-id
  "Forward admission-document-id with its established call arities."
  [resource-class resource]
  (writes/admission-document-id resource-class resource))

(defn admission-scope
  "Forward admission-scope with its established call arities."
  [config ctx]
  (writes/admission-scope config ctx))

(defn admit-saved-publication-resource!
  "Forward admit-saved-publication-resource! with its established call arities."
  [config ctx resource-class resource admit!]
  (writes/admit-saved-publication-resource! config ctx resource-class resource admit!))

(defn write-resource-and-admit!
  "Forward write-resource-and-admit! with its established call arities."
  ([config file-path edn-text admission!]
   (writes/write-resource-and-admit! config file-path edn-text admission!))
  ([config file-path edn-text admission! deps]
   (writes/write-resource-and-admit! config file-path edn-text admission! deps)))

(defn handle-save-resource
  "Forward handle-save-resource with its established call arities."
  ([do-json config resource-kind resource-id edn-text]
   (handlers/handle-save-resource do-json config resource-kind resource-id edn-text))
  ([do-json config resource-kind resource-id edn-text ctx admit!]
   (handlers/handle-save-resource do-json config resource-kind resource-id edn-text ctx admit!)))

(defn handle-agent-put-contract-edn
  "Forward handle-agent-put-contract-edn with its established call arities."
  ([do-text config contract-class contract-id edn-text]
   (handlers/handle-agent-put-contract-edn do-text config contract-class contract-id edn-text))
  ([do-text config contract-class contract-id edn-text ctx admit!]
   (handlers/handle-agent-put-contract-edn do-text config contract-class contract-id edn-text ctx admit!)))

(defn start-resource-watcher!
  "Forward start-resource-watcher! with its established call arities."
  [config]
  (index/start-resource-watcher! config))

(defn start-contract-watcher!
  "Forward start-contract-watcher! with its established call arities."
  [config]
  (index/start-resource-watcher! config))

(defn stop-resource-watcher!
  "Forward stop-resource-watcher! with its established call arities."
  []
  (index/stop-resource-watcher!))

(defn stop-contract-watcher!
  "Forward stop-contract-watcher! with its established call arities."
  []
  (index/stop-resource-watcher!))
