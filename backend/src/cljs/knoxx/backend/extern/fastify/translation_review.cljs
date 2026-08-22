(ns knoxx.backend.extern.fastify.translation-review
  "Fastify boundary for recording translation approvals.

  Native request and reply handles are born and die here; the facade receives
  decoded CLJS data and returns CLJS data.

  `POST /api/publications/translations/approvals` records that the authenticated
  principal accepted one translated revision. Three properties are enforced here
  rather than below, because this is where an untrusted caller meets the system:

  1. **The principal comes from the auth context, never from the body.** The
     request contract is closed and carries no principal field, so a caller
     cannot claim to be someone else. The same goes for the timestamp.
  2. **The tenant and project come from context and config**, not from the
     request, so a reviewer cannot approve into another organization's evidence.
  3. **Authorization is unconditional.** `with-request-context!` hands down a nil
     context when the policy database is disabled, and reading that as permission
     would let an anonymous caller manufacture the review evidence a publication
     gate is waiting on."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.routes.translation-review :as facade]
            [knoxx.backend.infra.stores.translation-evidence-registry :as registry]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.translation-evidence :as law]
            [knoxx.backend.shape.resource-identity :as resource-identity]))

(def approve-permission
  "Recording review evidence is a review action, so it takes the permission the
   `translator` role already holds — see `contracts/roles/translator.edn`.

   Deliberately NOT `org.translations.manage`: approving is what a reviewer does,
   and requiring queue-management authority would mean only admins could review."
  "org.translations.review")

(def refusal-status
  "HTTP status per refusal type, as a table.

   A table rather than a cond, so a new refusal type cannot be added without a
   status being chosen for it, and so classification is never re-derived from a
   message string.

   All 409. A missing translation is not a 404 — the request is well formed and
   the document exists, the system simply is not in a state where approving means
   anything yet. A mismatch is the same shape of problem: the caller is not wrong
   about syntax, it disagrees with recorded fact."
  {:translation-receipt-missing 409
   :translation-document-mismatch 409
   :translation-locale-mismatch 409
   :translation-source-revision-mismatch 409
   :translation-revision-mismatch 409})

(def RequestBody
  "Contract for the raw wire body, checked before anything is reshaped.

   Closed, and that has to be checked against the *body* rather than against the
   map this adapter builds: an unknown field is never copied into that map, so
   validating only the result would silently accept a typo. The dispatch adapter
   learned this the same way.

   Every field is required. Unlike dispatch, there is no meaningful whole-corpus
   default here — an approval is inherently about one revision."
  [:map {:closed true}
   [:document [:and :string [:fn {:error/message "document must not be blank"}
                             #(seq (str/trim %))]]]
   [:locale [:and :string [:fn {:error/message "locale must not be blank"}
                           #(seq (str/trim %))]]]
   [:revision [:and :string [:fn {:error/message "revision must not be blank"}
                             #(seq (str/trim %))]]]
   [:translation_revision [:and :string [:fn {:error/message "translation_revision must not be blank"}
                                         #(seq (str/trim %))]]]])

(defn decode-request
  "Validate the native body, then project it onto the approval request.

   The document and locale are decoded to keywords because that is how receipts
   are keyed. Revisions stay strings and are deliberately NOT decoded: a revision
   is opaque text, and `law/ConcreteRevision` is what refuses the one string shape
   that would be dangerous — `\"source/current\"`."
  [request]
  (let [body (law/assert-valid! :translation-review/body
                                RequestBody
                                (fastify/request-body request))]
    (law/assert-approval-request!
     {:review/document (resource-identity/decode-keyword (:document body))
      :review/locale (resource-identity/decode-keyword (:locale body))
      :review/revision (:revision body)
      :review/translation-revision (:translation_revision body)})))

(defn principal-of
  "The acting principal, from the auth context only.

   At least one durable identity must be present — `law/Principal` requires it,
   and this is where that requirement bites: a context carrying nothing
   identifiable would produce review evidence attributable to nobody, which is
   indistinguishable from evidence nobody produced."
  [ctx]
  (law/assert-valid!
   :translation-review/principal
   law/Principal
   (cond-> {}
     (some? (authz/ctx-user-id ctx))
     (assoc :principal/user-id (str (authz/ctx-user-id ctx)))

     (some? (authz/ctx-user-email ctx))
     (assoc :principal/user-email (str (authz/ctx-user-email ctx)))

     (some? (authz/ctx-membership-id ctx))
     (assoc :principal/membership-id (str (authz/ctx-membership-id ctx))))))

(defn review-scope
  "The tenant, project and principal an approval is recorded under.

   The organization is required and never defaulted: an approval filed under an
   invented tenant would be evidence in a scope nobody named. The project comes
   from configuration and matches what the dispatch path files batches under, so
   a reviewer sees the receipts that were actually produced."
  [config ctx]
  (let [org-id (some-> (authz/ctx-org-id ctx) str not-empty)]
    (when-not org-id
      (throw (ex-info "recording an approval requires an organization context"
                      {:status 403
                       :code "review_context_required"})))
    {:org-id org-id
     :project (some-> (:session-project-name config) str not-empty)
     :principal (principal-of ctx)}))

(defn- ^:async approve!
  [config ctx decoded]
  (if-let [evidence-store (registry/current)]
    (facade/approve-translation!
     {:evidence-store evidence-store
      :clock (fn [] (.toISOString (js/Date.)))}
     (review-scope config ctx)
     decoded)
    ;; Approval evidence that does not survive a restart is worse than none: the
    ;; gate would admit a publication today and block it tomorrow.
    (throw (ex-info "translation evidence persistence is not configured"
                    {:status 503
                     :code "translation_evidence_unavailable"}))))

(defn response-for
  "Status and body for one facade result.

   A refusal is a *response*, not an error, so it carries its typed evidence to
   the caller rather than being flattened into a message. Told only that
   something mismatched, a reviewer cannot see whether their request or the
   recorded translation was the stale one.

   An already-recorded approval answers 200 rather than 201, and neither is a
   409: the caller's intent is satisfied and the evidence exists. Answering 409
   would make an honest double-click look like a conflict to resolve."
  [result]
  (if-let [refusal (:approval/refusal result)]
    {:status (get refusal-status (:refusal/type refusal) 409)
     :body {:refused true :refusal refusal}}
    {:status (if (= :recorded (:approval/status result)) 201 200)
     :body {:approved true
            :status (:approval/status result)
            :approval (:approval result)}}))

(defn- error-status
  [err]
  (let [data (ex-data err)]
    (or (:status data)
        (fastify/error-status err nil)
        (if (contains? data :contract) 400 500))))

(defn- ^:async send-result!
  [reply operation]
  (try
    (let [{:keys [status body]} (response-for (await (operation)))]
      (fastify/send-json! reply status (resource-identity/encode-wire-values body)))
    (catch :default err
      (let [status (error-status err)]
        ;; Withholding the detail from the caller must not withhold it from us.
        (when-not (error-body/classified? status)
          (fastify/log-unclassified-failure! "translation-review" err))
        (fastify/send-json! reply status (error-body/error-body err status))))))

(defn register-translation-review-routes!
  [app runtime config handlers]
  (fastify/route!
   app
   {:method "POST"
    :url "/api/publications/translations/approvals"
    :handler
    (^:async fn [request reply]
      (await
       ((:with-request-context! handlers) runtime request reply
        (^:async fn [ctx]
          (await
           (send-result!
            reply
            (fn []
              ((:ensure-permission! handlers) ctx approve-permission)
              (approve! config ctx (decode-request request)))))))))})
  nil)
