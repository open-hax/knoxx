(ns knoxx.backend.infra.routes.publication-reconcile
  "The authorized runtime route that triggers publication reconciliation — the
   explicit trigger integration for `infra.publication-reconciler`, so the
   runtime is something a production path actually calls.

   POST /api/publications/reconcile decodes a trigger, runs the reconciler,
   and answers with the correlated receipt. GET /api/publications/receipts
   answers with the receipt journal the runtime emitted into. Both authorize:
   reconciliation changes what is public (`org.publications.manage`), while
   receipts only name what happened (`org.publications.read`).

   The reconciler is built once per config from `:publication/reconciliation`:

     :targets            registry declarations (data)
     :evidence-facts     [intent] -> gate evidence map
     :artifact-source    [intent concrete-revision] -> Promise<artifact|nil>
     :locale-admissible? [declaration intent artifact] -> boolean
     :store              optional IIdempotencyStore (default: process-local
                         memory store — a restart forgets reservations, which
                         the effect layer already tolerates by observing first)
     :target-factories   optional extra kind -> factory merges over the
                         built-in static-site factory
     :load-index!        optional desired-state loader (default: the resource
                         projection over this config)
     :emit-receipt!      optional receipt sink (default: the in-memory journal
                         the receipts route serves)

   With no spec the routes answer 503 — loudly unconfigured, rather than a
   reconciler that silently cannot publish. Fastify handles are decoded
   through `extern.fastify`; no native object crosses into the runtime."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.publication-reconciler :as reconciler]
            [knoxx.backend.infra.publication-target-memory :as memory]
            [knoxx.backend.infra.publication-target-registry :as registry]
            [knoxx.backend.infra.publication-target-static-site :as static-site]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.publication-reconciler :as trigger-law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def reconcile-permission
  "Reconciliation changes what is public, so it holds the publication write
   permission rather than inventing a parallel one."
  "org.publications.manage")

(def receipts-permission
  "Receipts name what was materialized where — publication read authority."
  "org.publications.read")

(def default-trigger-id
  "The trigger id a route demand carries when the caller named none: the route
   itself, so the receipt still says which channel asked."
  :publication.reconcile/http-request)

;; ── Trigger decoding ───────────────────────────────────────────────────────

(defn- body-keyword
  "A `\"namespace/name\"` string (or keyword) from a decoded body as a keyword,
   or nil. Bare values keywordize as-is, matching the resolver's `query-id`
   convention so a standalone unqualified id stays reachable."
  [value]
  (cond
    (keyword? value) value
    (and (string? value) (str/includes? value "/"))
    (let [[ns-part name-part] (str/split value #"/" 2)]
      (keyword ns-part name-part))
    (string? value) (keyword value)
    :else nil))

(defn decode-trigger
  "Decode a request body into a validated reconciliation trigger. The origin is
   fixed to `:route` here — a caller may not claim its demand arrived by
   another channel."
  [body]
  (trigger-law/assert-trigger!
   {:trigger/id (or (body-keyword (:triggerId body)) default-trigger-id)
    :trigger/origin :route
    :publication/id (body-keyword (:publicationId body))}))

;; ── Reconciler construction ────────────────────────────────────────────────

(defonce ^:private runtimes* (atom {}))

(defn reset-cached-runtimes!
  "Forget every constructed reconciler. Test support: construction is memoized
   per config so idempotency reservations and the receipt journal survive
   across requests, and a test needs a clean memo."
  []
  (reset! runtimes* {}))

(defn- build-runtime
  "Construct the reconciler and its default journal one config spec describes.
   An incomplete spec throws at the first request, not at registration — an
   unconfigured route is a 503, but a MISconfigured one is an operator defect
   and must fail loudly."
  [config spec]
  (let [journal (reconciler/make-receipt-journal)]
    {:journal journal
     :reconciler
     (reconciler/make-reconciler
      {:registry (registry/make-registry
                  (:targets spec)
                  (merge {:publication-target/static-site
                          static-site/static-site-target}
                         (:target-factories spec)))
       :store (or (:store spec) (:store (memory/memory-store)))
       :load-index! (or (:load-index! spec)
                        (fn [] (publications/publication-index! config)))
       :evidence-facts (:evidence-facts spec)
       :artifact-source (:artifact-source spec)
       :locale-admissible? (:locale-admissible? spec)
       :emit-receipt! (or (:emit-receipt! spec) (:emit! journal))})}))

(defn- runtime-for
  "The `{:reconciler ... :journal ...}` for `config`, built once per distinct
   config value. nil when the config declares no `:publication/reconciliation`
   spec."
  [config]
  (or (get @runtimes* config)
      (when-let [spec (:publication/reconciliation config)]
        (let [built (build-runtime config spec)]
          (swap! runtimes* assoc config built)
          built))))

;; ── Responses ──────────────────────────────────────────────────────────────

(defn- error-status
  "The HTTP status for a thrown reconcile failure. A denied request keeps the
   status it already carries; a malformed trigger is a 400; an unknown
   publication is a 404; contradictory desired state is a 409; anything else
   is a 500 with an opaque body."
  [err]
  (let [data (ex-data err)]
    (or (:status data)
        (fastify/error-status err nil)
        (cond
          (= :publication/reconcile-trigger (:contract data)) 400
          (contains? data :publication/id) 404
          (or (contains? data :blockers)
              (contains? data :conflicts)
              (contains? data :conflicting-payloads)) 409
          :else 500))))

(defn- ^:async send-result!
  "Answer with the operation's wire-encoded result, or with a classified error
   body. Keyword values are encoded as `namespace/name` so publication and
   trigger identity survive JSON."
  [reply operation]
  (try
    (fastify/send-json! reply 200
                        (resource-identity/encode-wire-values (await (operation))))
    (catch :default err
      (let [status (error-status err)]
        (when-not (error-body/classified? status)
          (fastify/log-unclassified-failure! "publication-reconcile" err))
        (fastify/send-json! reply status
                            (resource-identity/encode-wire-values
                             (error-body/error-body err status)))))))

(defn- ^:async handle-reconcile!
  "Run one decoded trigger and answer with its correlated receipt. 503 when no
   reconciler is configured: the demand is lawful, the capability is absent."
  [config request reply]
  (if-let [{:keys [reconciler]} (runtime-for config)]
    (await (send-result! reply
                         #(reconciler/reconcile!
                           reconciler
                           (decode-trigger (fastify/request-body request)))))
    (fastify/send-json! reply 503
                        {:detail "publication reconciliation is not configured"})))

(defn- ^:async handle-receipts!
  "Answer with the receipt journal the configured reconciler has emitted into."
  [config _request reply]
  (if-let [{:keys [journal]} (runtime-for config)]
    (await (send-result! reply (fn [] {:receipts ((:receipts journal))})))
    (fastify/send-json! reply 503
                        {:detail "publication reconciliation is not configured"})))

;; ── Registration ───────────────────────────────────────────────────────────

(defn register-publication-reconcile-routes!
  "Register the reconciliation trigger routes. Both enter the request context
   and authorize BEFORE touching resources, the registry, or the journal — a
   nil context fails closed, as on the other publication surfaces."
  [app runtime config {:keys [with-request-context! ensure-permission!]}]
  (fastify/route!
   app
   {:method "POST"
    :url "/api/publications/reconcile"
    :handler
    (^:async fn [request reply]
      (await (with-request-context!
              runtime request reply
              (^:async fn [ctx]
                (ensure-permission! ctx reconcile-permission)
                (await (handle-reconcile! config request reply))))))})
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/receipts"
    :handler
    (^:async fn [request reply]
      (await (with-request-context!
              runtime request reply
              (^:async fn [ctx]
                (ensure-permission! ctx receipts-permission)
                (await (handle-receipts! config request reply))))))})
  nil)
