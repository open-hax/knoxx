(ns knoxx.backend.extern.fastify.translation-dispatch
  "Fastify boundary for translation dispatch.

  Native request and reply handles are born and die here; the facade this
  adapter calls receives decoded CLJS data and returns CLJS data.

  One route lets an authorized operator dispatch whatever translation work the
  publication gate currently derives. A second, process-local route reports
  whether the exact event turns named by a deployment verifier have released
  their settlement owners. The worker's answer becoming durable evidence is
  not a route of its own: the worker already reports batch status to
  `POST /api/translations/batches/:id/status`. See `infra.routes.translation`
  for where that report is resolved."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.contracts.resolve :as contracts-resolve]
            [knoxx.backend.domain.event.dispatch :as event-dispatch]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.agent.runner :as agent-runner]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.routes.translation-dispatch :as facade]
            [knoxx.backend.infra.stores.translation-evidence-registry :as evidence-registry]
            [knoxx.backend.infra.stores.translation-split-registry :as split-registry]
            [knoxx.backend.infra.translation-agent-structured-output :as structured-output]
            [knoxx.backend.infra.translation-event-writer :as translation-event-writer]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.publication :as publication-law]
            [knoxx.backend.law.translation-split :as split-law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def dispatch-permission
  "Creating translation work is a queue mutation on a shared worker, so it takes
   the same permission as creating a batch directly —
   `infra.routes.translation`'s batch routes already gate on this."
  "org.translations.manage")

(def ^:private event-turn-id-pattern
  #"^(translation-needed-translation-run|knoxx-publication-document-indexed)-[0-9a-f]{64}$")

(defn- exact-event-turn-id?
  [value]
  (and (string? value)
       (boolean (re-matches event-turn-id-pattern value))))

(def EventTurnStatusRequestBody
  "Closed wire contract for the process-local event-turn settlement query.

   Only event identities emitted by publication translation and document
   admission are queryable. Bounding the vector prevents this observability
   endpoint from becoming an unscoped process-state scanner."
  [:map {:closed true}
   [:event_ids
    [:vector {:min 1 :max 16}
     [:and
      :string
      [:fn {:error/message "must be an exact publication event-turn id"}
       exact-event-turn-id?]]]]])

(def EventTurnStatusResponse
  "Wire response for exact process-local event-turn owner states."
  [:map {:closed true}
   [:settled :boolean]
   [:events
    [:vector
     [:map {:closed true}
      [:event_id :string]
      [:state [:enum "in_flight" "redelivery_pending" "released"]]]]]])

(def RequestBody
  "Contract for the raw wire body, checked BEFORE anything is reshaped.

   This is the closed one, and it has to be. Validating only the map this
   adapter *constructs* cannot refuse an unknown field, because an unknown field
   is simply never copied into it: `{\"documnet\": \"knoxx.docs/probe\"}` produced
   an empty decoded map, which is a valid request to translate the entire
   corpus. The typo silently became the most expensive operation this route has.

   So the body is checked first. A document selects all of its publication
   relations; a publication selects exactly one inventory row; absence of both
   asks for a whole-corpus sweep. The two selectors are mutually exclusive so a
   caller never has to guess which one wins."
  [:and
   [:map {:closed true}
    [:document {:optional true} [:and :string [:fn {:error/message "a named document may not be blank"}
                                               #(seq (str/trim %))]]]
    [:publication {:optional true} [:and :string [:fn {:error/message "a named publication may not be blank"}
                                                  #(seq (str/trim %))]]]]
   [:fn {:error/message "dispatch may select a document or publication, not both"}
    #(not (and (contains? % :document) (contains? % :publication)))]])

(def DecodedRequest
  "Contract for the data this adapter hands inward, after decoding.

   Either selector is optional and, when present, must be a qualified keyword.
   Not `[:maybe ...]`: nil is not a way of saying 'all', it is a caller that
   meant to name one and sent nothing. Absence is the only whole-corpus form."
  [:and
   [:map {:closed true}
    [:document {:optional true} :qualified-keyword]
    [:publication {:optional true} :qualified-keyword]]
   [:fn #(not (and (contains? % :document) (contains? % :publication)))]])

(defn decode-request
  "Validate the native request body, then project it onto what the facade reads.

   Both contracts run, and they catch different things. `RequestBody` refuses an
   unknown or blank field on the wire; `DecodedRequest` refuses an identity that
   decoded to something the resource index is not keyed by — a bare `probe`
   becomes the unqualified `:probe`, which is a different document from
   `:knoxx.docs/probe` and would sweep nothing while reporting success."
  [request]
  (let [body (publication-law/assert-valid! :translation-dispatch/body
                                            RequestBody
                                            (fastify/request-body request))]
    (publication-law/assert-valid!
     :translation-dispatch/request
     DecodedRequest
     (cond-> {}
       (contains? body :document)
       (assoc :document (resource-identity/decode-keyword (:document body)))

       (contains? body :publication)
       (assoc :publication (resource-identity/decode-keyword (:publication body)))))))

(defn decode-event-turn-status-request
  "Validate an exact, bounded list of event-turn identities from the wire."
  [request]
  (:event_ids
   (publication-law/assert-valid!
    :translation-event-turn-status/body
    EventTurnStatusRequestBody
    (fastify/request-body request))))

(defn- owner-state->wire-status
  [event-id owner-state]
  (case owner-state
    :in-flight "in_flight"
    :settled "redelivery_pending"
    nil "released"
    (throw
     (ex-info "event turn owner returned an unknown state"
              {:event-id event-id
               :owner-state owner-state}))))

(defn event-turn-status
  "Return the process-local settlement state for each supplied event id.

   An absent owner is released. A retained terminal result is not released: its
   callback still needs redelivery before cleanup may delete durable fixtures."
  [event-ids dependencies]
  (let [owner-state (or (:event-turn-owner-state dependencies)
                        agent-runner/event-turn-owner-state)
        states (mapv (fn [event-id]
                       [event-id (owner-state event-id)])
                     event-ids)]
    (publication-law/assert-valid!
     :translation-event-turn-status/response
     EventTurnStatusResponse
     {:settled (every? (comp nil? second) states)
      :events (mapv (fn [[event-id state]]
                      {:event_id event-id
                       :state (owner-state->wire-status event-id state)})
                    states)})))

(defn- scope
  "The acting principal's dispatch scope, plus the project batches are filed in.

   The organization and membership are required by
   `law.translation-dispatch/DispatchContext` and neither is defaulted: a batch
   filed under an invented organization or a blank membership is unattributable,
   and the worker's own batch contract requires both to be non-blank.

   `:project` is the configured `:session-project-name`, and omitting it was a
   real defect rather than a tidiness issue. With no project the OpenPlanner
   batch store defaults to `\"devel\"`, while every existing translation surface —
   segments, documents, export — filters by `:session-project-name`, whose
   default is `\"knoxx-session\"`. A dispatched translation therefore succeeded and
   then vanished from the review and export flow, because it had been written to
   a project nothing reads."
  [config ctx]
  (let [org-id (some-> (authz/ctx-org-id ctx) str not-empty)
        membership-id (some-> (authz/ctx-membership-id ctx) str not-empty)]
    (when-not (and org-id membership-id)
      (throw (ex-info "translation dispatch requires an organization and membership context"
                      {:status 403
                       :code "dispatch_context_required"})))
    (cond-> {:org-id org-id :membership-id membership-id}
      (some-> (:session-project-name config) str not-empty)
      (assoc :project (str (:session-project-name config))))))

(defn request-scope
  "Public scope projection for app-owned commands composed beside this route.

  Document admission uses the identical authenticated organization,
  membership, and project coordinates before invoking translation dispatch;
  keeping this projection here prevents the two HTTP adapters from drifting."
  [config ctx]
  (scope config ctx))

(def translation-agent-id
  "The contract the publication translation trigger executes."
  "publication_translator")

(defn- translation-execution
  "Snapshot the exact resolved agent policy the opted-in trigger must execute."
  [config dependencies]
  (let [resolve-contract (or (:resolve-agent-contract dependencies)
                             contracts-resolve/resolve-agent-contract)
        resolved (or (resolve-contract config translation-agent-id)
                     (throw (ex-info "publication translation agent is unavailable"
                                     {:status 503
                                      :code "translation_agent_unavailable"})))]
    (split-law/execution-snapshot
     crypto/sha256-hex
     {:agent-id translation-agent-id
      :model (:model resolved)
      :thinking (:thinking-level resolved)
      :system-prompt (:system-prompt resolved)
      :tool-ids (:tool-ids resolved)
      :tools-choice (:tools-choice resolved)})))

(defn- candidate-event-emitter
  [dependencies client]
  (or (:emit-candidate-events! dependencies)
      (do
        (openplanner-client/assert-event-projection-repair-supported! client)
        (partial translation-event-writer/emit-candidate-events! client))))

(defn- default-dispatch-dependencies
  [config dependencies evidence-store split-store client]
  {:evidence-store evidence-store
   :split-store split-store
   :content-root (:publication-content-root config)
   :emit-candidate-events! (candidate-event-emitter dependencies client)
   :translation-execution (or (:translation-execution dependencies)
                              (translation-execution config dependencies))
   :client client
   :clock (or (:clock dependencies) (fn [] (.toISOString (js/Date.))))
   :observe-source-revision (or (:observe-source-revision dependencies)
                                (facade/source-revision-observer! config))
   :emit! (or (:emit! dependencies)
              (fn [event] (event-dispatch/dispatch! config event)))
   :register-turn-settler! (or (:register-turn-settler! dependencies)
                               agent-runner/register-event-turn-settler!)
   :unregister-turn-settler! (or (:unregister-turn-settler! dependencies)
                                 agent-runner/unregister-event-turn-settler!)
   :complete-turn! (if (contains? dependencies :complete-turn!)
                     (:complete-turn! dependencies)
                     (fn [runtime-deps record turn]
                       (structured-output/complete-turn!
                        config runtime-deps record turn)))
   :digest-hex (or (:digest-hex dependencies) crypto/sha256-hex)})

(defn- assembled-dispatch-dependencies
  [config dependencies evidence-store split-store]
  (let [client (or (:client dependencies)
                   (openplanner-client/client config))]
    (merge dependencies
           (default-dispatch-dependencies
            config dependencies evidence-store split-store client))))

(defn ^:async dispatch-selection-for-scope!
  "Invoke translation dispatch from an already trusted tenant scope.

   HTTP callers must use `dispatch-selection!`, which derives this scope from
   the authenticated request. This sibling exists for generated-resource
   admission: its scope was server-pinned on the originating admission event,
   so forcing an HTTP loopback would add no authority and would create a
   tools/Fastify dependency cycle."
  [config dispatch-scope decoded dependencies]
  (let [evidence-store (or (:evidence-store dependencies)
                           (evidence-registry/current))
        split-store (or (:split-store dependencies)
                        (split-registry/current))
        dispatch-translations! (or (:dispatch-translations! dependencies)
                                   facade/dispatch-translations!)]
    (when-not evidence-store
      (throw (ex-info "translation evidence persistence is not configured"
                      {:status 503
                       :code "translation_evidence_unavailable"})))
    (when-not split-store
      (throw (ex-info "translation split persistence is not configured"
                      {:status 503
                       :code "translation_split_persistence_unavailable"})))
    ;; This outer adapter owns the runner/event dependencies because requiring
    ;; them from the facade would close a tools -> sink -> facade cycle.
    (dispatch-translations!
     config
     (assembled-dispatch-dependencies
      config dependencies evidence-store split-store)
     dispatch-scope decoded)))

(defn- ^:async dispatch!
  [config ctx decoded dependencies]
  (await (dispatch-selection-for-scope! config (scope config ctx)
                                        decoded dependencies)))

(defn ^:async dispatch-selection!
  "Invoke the fully composed translation dispatcher for an already decoded
  selection. Intended for sibling app commands such as document admission that
  must reuse the same request context without an HTTP loopback."
  [config ctx selection dependencies]
  (await (dispatch! config ctx selection dependencies)))

(defn- error-status
  "A status the error already carries wins; a contract violation is a 400,
   because the caller sent something inadmissible and a retry will not help."
  [err]
  (let [data (ex-data err)]
    (or (:status data)
        (fastify/error-status err nil)
        (cond
          (contains? data :contract) 400
          (contains? data :blockers) 409
          :else 500))))

(defn- ^:async send-result!
  [reply operation]
  (try
    (fastify/send-json! reply 200 (resource-identity/encode-wire-values
                                  (await (operation))))
    (catch :default err
      (let [status (error-status err)]
        ;; Withholding the detail from the caller must not withhold it from us.
        (when-not (error-body/classified? status)
          (fastify/log-unclassified-failure! "translation-dispatch" err))
        (fastify/send-json! reply status (error-body/error-body err status))))))

(defn- dispatch-route
  [runtime config handlers dependencies]
  {:method "POST"
   :url "/api/publications/translations/dispatch"
   :handler
   (^:async fn [request reply]
     (await
      ((:with-request-context! handlers) runtime request reply
       (^:async fn [ctx]
         (await
          (send-result!
           reply
           (fn []
             ;; Authorization is unconditional; a nil context is not access.
             ((:ensure-permission! handlers) ctx dispatch-permission)
             (dispatch-selection! config ctx (decode-request request)
                                  dependencies))))))))})

(defn- event-turn-status-route
  [runtime handlers dependencies]
  {:method "POST"
   :url "/api/publications/translations/event-turn-status"
   :handler
   (^:async fn [request reply]
     (await
      ((:with-request-context! handlers) runtime request reply
       (^:async fn [ctx]
         (await
          (send-result!
           reply
           (fn []
             ((:ensure-permission! handlers) ctx dispatch-permission)
             (event-turn-status
              (decode-event-turn-status-request request)
              dependencies))))))))})

(defn register-translation-dispatch-routes!
  "Register the route with optional registration-scoped dependencies.

   Production uses the four-argument form. Tests and alternate compositions may
   supply stable CLJS dependencies in the fifth argument; the registered handler
   closes over them, so concurrent async requests never mutate global Vars."
  ([app runtime config handlers]
   (register-translation-dispatch-routes! app runtime config handlers {}))
  ([app runtime config handlers dependencies]
   (fastify/route! app (dispatch-route runtime config handlers dependencies))
   (fastify/route! app (event-turn-status-route runtime handlers dependencies))
   nil))
