(ns knoxx.backend.extern.fastify.document-admission
  "Authenticated Fastify boundary for publication document admission."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.document-admission :as admission-domain]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.fastify.translation-dispatch :as translation-adapter]
            [knoxx.backend.infra.routes.document-admission :as admission]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def admission-permission "org.translations.manage")
(def draft-permission "org.publications.manage")

(def BarrierBody
  [:map {:closed true}])

(def RequestBody
  "Closed admission command. Empty means the default anchor sweep.

  `anchors: false` is only meaningful beside an exact document. Policy and
  scope coordinates are deliberately absent: they are derived from trusted
  resources and the authenticated request context."
  [:and
   [:map {:closed true}
    [:anchors {:optional true} boolean?]
    [:document {:optional true}
     [:and :string
      [:fn {:error/message "a named document may not be blank"}
       #(seq (str/trim %))]]]
    [:generateDrafts {:optional true} boolean?]]
   [:fn {:error/message "admission selects anchors or one exact document"}
    (fn [body]
      (let [document? (contains? body :document)
            anchors-present? (contains? body :anchors)
            anchors? (:anchors body)]
        (and (not (and document? (true? anchors?)))
             (or document?
                 (not anchors-present?)
                 (true? anchors?)))))]])

(defn decode-request
  "Validate the raw wire map before projecting it into domain keys."
  [request]
  (let [body (publication-law/assert-valid! :document-admission/body
                                            RequestBody
                                            (fastify/request-body request))]
    (admission-domain/normalize-selection
     (cond-> {}
       (contains? body :anchors)
       (assoc :anchors? (:anchors body))

       (contains? body :document)
       (assoc :document (resource-identity/decode-keyword (:document body)))

       (contains? body :generateDrafts)
       (assoc :generate-drafts? (:generateDrafts body))))))

(defn decode-barrier-request
  [request]
  (publication-law/assert-valid! :document-admission/barrier-body
                                 BarrierBody
                                 (fastify/request-body request)))

(defn- error-status
  [err]
  (let [data (ex-data err)]
    (or (:status data)
        (fastify/error-status err nil)
        (cond
          (contains? #{:document-admission/body
                       :document-admission/barrier-body}
                     (:contract data)) 400
          (contains? data :document/id) 404
          (or (contains? data :blockers)
              (contains? data :conflicts)
              (contains? data :conflicting-payloads)) 409
          :else 500))))

(defn- ^:async send-result!
  [reply operation]
  (try
    (fastify/send-json! reply 200
                        (resource-identity/encode-wire-values
                         (await (operation))))
    (catch :default err
      (let [status (error-status err)]
        (when-not (error-body/classified? status)
          (fastify/log-unclassified-failure! "document-admission" err))
        (fastify/send-json! reply status
                            (resource-identity/encode-wire-values
                             (error-body/error-body err status)))))))

(defn- request-dependencies
  "Close translation dispatch over this request context and resource snapshot.

  Tests and internal compositions may supply `:dispatch-document!` directly;
  production gets the same fully assembled dispatcher as the manual endpoint."
  [config ctx dependencies]
  (if (:dispatch-document! dependencies)
    dependencies
    (assoc dependencies
           :dispatch-document!
           (fn [document-id snapshot-deps]
             (translation-adapter/dispatch-selection!
              config ctx {:document document-id}
              (merge dependencies snapshot-deps))))))

(defn- ^:async handle-admission-barrier!
  [runtime handlers dependencies request reply]
  (await
   ((:with-request-context! handlers) runtime request reply
    (^:async fn [ctx]
      (await
       (send-result!
        reply
        (fn []
          ((:ensure-permission! handlers) ctx admission-permission)
          (decode-barrier-request request)
          (if-let [barrier! (:await-document-admission-barrier! dependencies)]
            (barrier!)
            (admission/await-document-admission-barrier!)))))))))

(defn register-document-admission-routes!
  "Register POST /api/publications/documents/admit.

  Dependencies are registration-scoped so app composition can reuse the same
  public infra function for generated-document admission without Vars or HTTP
  loopback."
  ([app runtime config handlers]
   (register-document-admission-routes! app runtime config handlers {}))
  ([app runtime config handlers dependencies]
   (fastify/route!
    app
    {:method "POST"
     :url "/api/publications/documents/admit"
     :handler
     (^:async fn [request reply]
       (await
        ((:with-request-context! handlers) runtime request reply
         (^:async fn [ctx]
           (await
            (send-result!
             reply
             (fn []
               ((:ensure-permission! handlers) ctx admission-permission)
               (let [selection (decode-request request)]
                 ;; Admission always creates translation work. Draft generation
                 ;; additionally creates publication resources. An omitted
                 ;; command delegates to each trusted Document's generation
                 ;; policy, so only an explicit false can prove this request is
                 ;; translation-only at the HTTP boundary.
                 (when-not (false? (:generate-drafts? selection))
                   ((:ensure-permission! handlers) ctx draft-permission))
                 (let [scope (translation-adapter/request-scope config ctx)]
                 (admission/admit-documents!
                  config (request-dependencies config ctx dependencies)
                  scope selection))))))))))})
   (fastify/route!
    app
    {:method "POST"
     :url "/api/publications/documents/admission-barrier"
     :handler
     (fn [request reply]
       (handle-admission-barrier!
        runtime handlers dependencies request reply))})
   nil))
