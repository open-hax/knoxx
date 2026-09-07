(ns knoxx.backend.extern.fastify.publications
  "Fastify boundary for the publication projection routes.

  Native request and reply handles are born and die here. The facade this
  adapter calls receives decoded CLJS data and returns CLJS data; it never sees
  a Fastify object. Handlers are awaited rather than chained, so a currently
  synchronous facade stays compatible if it later becomes effectful without
  introducing a `.then`.

  Every route authorizes. The projection reads document titles, garden
  membership, and publication paths off the filesystem, so an unauthenticated
  caller must not be able to enumerate it."
  (:require [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.publication :as law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def read-permission "org.publications.read")

(def DecodedRequest
  "Contract for the data this adapter hands inward. Named and asserted so a
   changed Fastify parameter shape fails at the boundary rather than flowing on
   into document lookup."
  [:map {:closed true}
   [:params [:map-of :keyword [:maybe :string]]]
   [:method :string]])

(defn decode-request
  "Project the native request onto the CLJS data the facade actually reads, and
   validate it. Everything downstream reads route identity from this map, never
   from the raw handle."
  [request]
  (law/assert-valid!
   :publication/request
   DecodedRequest
   {:params (fastify/request-params request)
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
  "A projection failure caused by resource data is a 409 — the desired state is
   genuinely contradictory and a retry will not help. An unknown document is a
   404. Anything else is a 500.

   A status the error already carries wins over all of them: a denied request
   is a 403, and classifying it as an internal failure would tell the caller to
   retry something that will never succeed."
  [err]
  (let [data (ex-data err)]
    (or (http-error-status err)
        (cond
          (contains? data :document/id) 404
          (or (contains? data :conflicts)
              (contains? data :conflicting-payloads)
              (contains? data :blockers)) 409
          :else 500))))

(defn encode-body
  "Encode keyword values so identity survives JSON.

   `send-json!` serializes with `clj->js`, which renders a keyword using `name`
   — so `:knoxx.docs/translation-pipeline` would reach the CMS as
   `\"translation-pipeline\"`, collapsing distinct namespaces onto one wire id
   and defeating the canonical identity the projection just established. Every
   keyword value is encoded as `namespace/name`, with no EDN leading colon.

   Map keys are left to `clj->js`: unqualified JSON keys are this codebase's
   wire convention, with qualified domain keys rebuilt by explicit adapter
   mapping on the client side."
  [body]
  (resource-identity/encode-wire-values body))

(defn ^:async send-projection!
  "Await an operation and send its CLJS result as JSON, translating a projection
   failure into a status the CMS can act on."
  [reply operation]
  (try
    (fastify/send-json! reply 200 (encode-body (await (operation))))
    (catch :default err
      (let [status (error-status err)]
        ;; Withholding the detail from the caller must not withhold it from us.
        ;; What the log may contain is decided once, in `log-unclassified-failure!`.
        (when-not (error-body/classified? status)
          (fastify/log-unclassified-failure! "publications" err))
        (fastify/send-json! reply status
                            (encode-body (error-body/error-body err status)))))))

(defn- ^:async guarded!
  "Authorize, then run. `ensure-permission!` throws, and `send-projection!`
   turns that into an error response — so an unauthorized caller never reaches
   the filesystem-backed projection.

   The check is unconditional. `with-request-context!` hands down a nil context
   when the policy database is disabled, and treating that as a reason to skip
   the check would serve document titles, garden membership and publication
   paths to an anonymous caller — the exact enumeration this adapter's
   docstring promises to refuse. A nil context therefore fails closed, as it
   already does on the admin routes, rather than being read as permission."
  [handlers ctx operation]
  ((:ensure-permission! handlers) ctx read-permission)
  (await (operation)))

(defn- request-scope
  [ctx]
  {:org-id (some-> (authz/ctx-org-id ctx) str not-empty)})

(defn- authorized-route
  "One shape for both routes: enter the request context, authorize, run the
   projection, send it. `operation` receives the decoded request."
  [runtime handlers operation]
  (^:async fn [request reply]
    (let [decoded (decode-request request)]
      (await
       ((:with-request-context! handlers) runtime request reply
        (^:async fn [ctx]
          (await
           (send-projection!
            reply
            #(guarded! handlers ctx
                        (fn [] (operation (request-scope ctx) decoded)))))))))))

(defn register-publication-routes!
  [app runtime config handlers]
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/documents"
    :handler (authorized-route runtime handlers
                               (fn [scope _decoded]
                                 (publications/list-publication-documents!
                                  config scope)))})
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/documents/:documentId"
    :handler (authorized-route runtime handlers
                               (fn [scope decoded]
                                 (publications/publication-document-view!
                                  config scope
                                  (get-in decoded [:params :documentId]))))})
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/gardens"
    :handler (authorized-route runtime handlers
                               (fn [scope _decoded]
                                 (publications/list-publication-gardens!
                                  config scope)))})
  nil)
