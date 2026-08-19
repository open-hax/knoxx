(ns knoxx.backend.extern.fastify.cms-publication
  "Fastify boundary for the CMS publication surface.

  Native request and reply handles are born and die here; the facade sees only
  decoded CLJS data. Reads and writes carry distinct permissions — seeing the
  publication topology must not imply authority to change what is public."
  (:require [knoxx.backend.domain.cms-publication :as cms]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.routes.cms-publication :as facade]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.publication :as law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def read-permission "org.publications.read")
(def write-permission "org.publications.manage")

(def DecodedRequest
  [:map {:closed true}
   [:params [:map-of :keyword [:maybe :string]]]
   [:body [:maybe :map]]
   [:method :string]])

(defn decode-request
  "Decode once, and validate. Route identity and body are read from this map,
   never from the raw handle."
  [request]
  (law/assert-valid!
   :cms/request
   DecodedRequest
   {:params (fastify/request-params request)
    :body (let [body (fastify/request-body request)]
            (when (map? body) body))
    :method (fastify/request-method request)}))

(defn- http-error-status
  "The status a recognized HTTP error already carries, or nil.

   `ensure-permission!` throws `http/http-error`, which records its status both
   in ex-data (`:status`) and on the JS error object (`statusCode`). Both are
   read, because a fake permission check in a test throws plain `ex-info` while
   the real one carries the JS property."
  [err]
  (or (:status (ex-data err))
      (fastify/error-status err nil)))

(defn- error-status
  "A carried status wins over every guess below it.

   `guarded!` runs inside `respond!`'s operation, so a permission denial is
   caught here rather than escaping the adapter — and without this the 403 fell
   through to 500, telling the caller to retry a request that will never
   succeed. Third instance of this in the epic, after #230 and #233."
  [err]
  (let [data (ex-data err)]
    (or (http-error-status err)
        (cond
          (contains? data :publication/id) 404
          (contains? data :document/id) 404
          (or (contains? data :conflicts) (contains? data :blockers)) 409
          (contains? data :errors) 422
          :else 500))))

(defn ^:async respond!
  [handlers reply operation]
  (let [{:keys [json-response!]} handlers]
    (try
      (json-response! reply 200
                      (resource-identity/encode-wire-values (await (operation))))
      (catch :default err
        (let [status (error-status err)]
          ;; See the note on the same catch in extern/fastify/publications.cljs:
          ;; the caller gets an opaque body for an unclassified failure, so the
          ;; detail has to be logged here or it is lost entirely.
          (when-not (error-body/classified? status)
            (js/console.error "[cms-publications] unclassified failure:"
                              (or (ex-message err) (str err))
                              (pr-str (ex-data err))))
          (json-response! reply status
                          (resource-identity/encode-wire-values
                           (error-body/error-body err status))))))))

(defn- ^:async guarded!
  "Authorize, then run.

   The check is unconditional. `with-request-context!` hands down a nil context
   when the policy database is disabled, and skipping it there would let an
   anonymous caller read the publication topology and flip publication state —
   the third instance of this same hole in the epic, after the publication routes
   in #230 and the translation config routes in #233."
  [handlers ctx permission operation]
  ((:ensure-permission! handlers) ctx permission)
  (await (operation)))

(defn- route-handler
  [runtime handlers permission operation]
  (fn [request reply]
    (let [decoded (decode-request request)]
      ((:with-request-context! handlers) runtime request reply
       (^:async fn [ctx]
         (await
          (respond! handlers reply
                    #(guarded! handlers ctx permission
                               (fn [] (operation decoded))))))))))

(defn register-cms-publication-routes!
  [app runtime config handlers]
  (let [{:keys [route!]} handlers]
    (route! app "GET" "/api/cms/publications/documents"
            (route-handler runtime handlers read-permission
                           (fn [_decoded] (facade/list-documents! config))))
    (route! app "GET" "/api/cms/publications/documents/:documentId"
            (route-handler runtime handlers read-permission
                           (fn [decoded]
                             (facade/document-view!
                              config (get-in decoded [:params :documentId])))))
    (route! app "PATCH" "/api/cms/publications/intents/:publicationId"
            (route-handler runtime handlers write-permission
                           (fn [decoded]
                             (facade/set-publication-state!
                              config
                              (resource-identity/decode-keyword
                               (get-in decoded [:params :publicationId]))
                              (cms/decode-publication-state-patch (:body decoded))))))
    nil))
