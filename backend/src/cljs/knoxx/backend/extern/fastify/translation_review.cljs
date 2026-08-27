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
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store-api]
            [knoxx.backend.infra.routes.translation-review :as facade]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.routes.translation-dispatch :as translation-dispatch]
            [knoxx.backend.infra.stores.translation-evidence-registry :as registry]
            [knoxx.backend.law.error-body :as error-body]
            [knoxx.backend.law.translation-evidence :as law]
            [knoxx.backend.shape.resource-identity :as resource-identity]
            [promesa.core :as p]))

(def approve-permission
  "Recording review evidence is a review action, so it takes the permission the
   `translator` role already holds — see `contracts/roles/translator.edn`.

   Deliberately NOT `org.translations.manage`: approving is what a reviewer does,
   and requiring queue-management authority would mean only admins could review."
  "org.translations.review")

(def read-permission "org.translations.read")

(def refusal-status
  "HTTP status per refusal type, as a table.

   A table rather than a cond, so a new refusal type cannot be added without a
   status being chosen for it, and so classification is never re-derived from a
   message string. Written out rather than derived from
   `law/approval-refusal-types` with a default, because a default is exactly the
   forcing function this table exists to be: it would let a new refusal reach
   the wire on a status nobody picked. `translation_review_test` asserts the key
   set equals law's, so the two cannot drift silently either.

   All 409. A missing translation is not a 404 — the request is well formed and
   the document exists, the system simply is not in a state where approving means
   anything yet. A mismatch is the same shape of problem: the caller is not wrong
   about syntax, it disagrees with recorded fact. A garden mismatch is a mismatch
   like the rest: the receipt is keyed by garden, so naming another one is a
   disagreement with recorded fact rather than a malformed request."
  {:translation-receipt-missing 409
   :translation-document-mismatch 409
   :translation-garden-mismatch 409
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
   [:garden [:and :string [:fn {:error/message "garden must not be blank"}
                           #(seq (str/trim %))]]]
   [:locale [:and :string [:fn {:error/message "locale must not be blank"}
                           #(seq (str/trim %))]]]
   [:revision [:and :string [:fn {:error/message "revision must not be blank"}
                             #(seq (str/trim %))]]]
   [:translation_revision [:and :string [:fn {:error/message "translation_revision must not be blank"}
                                         #(seq (str/trim %))]]]])

(defn decode-request
  "Validate the native body, then project it onto the approval request.

   The document, garden and locale are decoded to keywords because that is how
   receipts are keyed. The garden is required rather than inferred: a document
   published to two gardens has two translations, and picking one for the
   reviewer would be picking which bytes their approval authorizes. Revisions stay strings and are deliberately NOT decoded: a revision
   is opaque text, and `law/ConcreteRevision` is what refuses the one string shape
   that would be dangerous — `\"source/current\"`."
  [request]
  (let [body (law/assert-valid! :translation-review/body
                                RequestBody
                                (fastify/request-body request))]
    (law/assert-approval-request!
     {:review/document (resource-identity/decode-keyword (:document body))
      :review/garden (resource-identity/decode-keyword (:garden body))
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

(defn- ^:async ensure-contract-receipts!
  [config evidence-store scope]
  (let [records (await (publications/resource-records! config))
        index (publications/publication-index records)
        documents (vec (vals (:documents index)))
        roots (translation-dispatch/document-source-roots config records)
        revisions (await (source-revision/source-revisions!
                          config documents roots))]
    {:index index
     :roots roots
     :authored (await (contract-content/ensure-receipts!
                       evidence-store index roots scope revisions))}))

(defn- relation-key
  "The five coordinates that identify one translated revision.

   Shared by receipts and by the wire reviews built from them, so the two can be
   joined without either side inventing an ordering. Source revision AND output
   revision are both present because an approval is revision-specific on both
   sides — see `law.translation-evidence/approval-current?`."
  [document garden locale revision translation-revision]
  [document garden locale revision translation-revision])

(defn- receipt-relation
  [receipt]
  (relation-key (:translation/document receipt)
                (:translation/garden receipt)
                (:translation/locale receipt)
                (:translation/source-revision receipt)
                (:translation/revision receipt)))

(defn- review-relation
  [review]
  (relation-key (:document review)
                (:garden review)
                (:locale review)
                (:revision review)
                (:translation_revision review)))

(defn- ^:async hydrate-review!
  "Attach the text a reviewer has to read before approving, for any review whose
   document this deployment's contracts own.

   Previously only AUTHORED reviews were hydrated, and the consequence was that
   an agent-produced translation of a contract-backed document reached the page
   carrying no `:content_source`, no `:source_text` and no `:translated_text` —
   whereupon `pages.translations.logic/attach-publication-reviews` dropped it,
   because it joined no worker document and was not authored. Since agent
   submissions create no Mongo segments (`infra.translation-agent-sink` writes
   content and a receipt, nothing else), no worker document ever exists for one.

   So an agent translation was invisible, therefore unapprovable, therefore —
   under `:translation/review :required` — unpublishable. The publish path knew
   about it all along: `infra.publication-runtime/translated-blocks!` reads agent
   content as precedence 1. Only the review path did not.

   The precedence here mirrors that one deliberately, so a reviewer reads the
   same bytes the reconciler would publish: agent-submitted content first, keyed
   by the receipt it attests to, then the authored locale file.

   `:content_source` distinguishes the two rather than collapsing them. What a
   reviewer is looking at is not a detail — approving authored bytes and
   approving generated bytes are different acts."
  [config index roots authored-relations receipts-by-relation review]
  (let [document (get-in index [:documents (:document review)])]
    (if (nil? document)
      ;; Not a document these contracts own. It belongs to the OpenPlanner
      ;; worker path and is joined to its own document row on the client.
      review
      (let [relation (review-relation review)
            root (get roots (:document/id document))
            authored? (contains? authored-relations relation)
            receipt (get receipts-by-relation relation)
            source (await (contract-content/source-content! root document))
            submitted (when-not authored?
                        (await (agent-content/content-for-receipt!
                                (:publication-content-root config) receipt)))
            translated (if authored?
                         (await (contract-content/localized-content!
                                 root document (:locale review)))
                         submitted)]
        (cond-> (assoc review
                       :title (:document/title document)
                       :source_locale (:document/source-locale document))
          (some? source) (assoc :source_text source)
          ;; Both or neither. A review carrying a content source but no text
          ;; would render an approval control over nothing, which is the one
          ;; thing revision-specific approval exists to prevent.
          (some? translated) (assoc :translated_text translated
                                    :content_source (if authored?
                                                      :authored-contract
                                                      :agent)))))))

(defn- ^:async approve!
  [config ctx decoded]
  (if-let [evidence-store (registry/current)]
    (let [scope (review-scope config ctx)
          _ (await (ensure-contract-receipts! config evidence-store scope))]
      (facade/approve-translation!
       {:evidence-store evidence-store
        :clock (fn [] (.toISOString (js/Date.)))}
       scope
       decoded))
    ;; Approval evidence that does not survive a restart is worse than none: the
    ;; gate would admit a publication today and block it tomorrow.
    (throw (ex-info "translation evidence persistence is not configured"
                    {:status 503
                    :code "translation_evidence_unavailable"}))))

(defn- ^:async reviewable!
  [config ctx]
  (if-let [evidence-store (registry/current)]
    (let [scope (review-scope config ctx)
          {:keys [index roots authored]}
          (await (ensure-contract-receipts! config evidence-store scope))
          result (await
                  (facade/reviewable-translations!
                   {:evidence-store evidence-store
                    :publication-index index}
                   scope))
          ;; One extra scoped read so hydration can find the receipt a review
          ;; was built from. `content-for-receipt!` compares all eight
          ;; coordinates before returning bytes, and the wire review carries
          ;; only five — so the receipt itself is required, not reconstructable.
          completed (->> (await (evidence-store-api/completed-translations!
                                 evidence-store (select-keys scope [:org-id :project])))
                         ;; Scoped in the query AND filtered here, for the reason
                         ;; `facade/current-receipt!` gives about its own copy:
                         ;; the query is the optimization, the filter is the
                         ;; guarantee. A replaceable store that ignored the scope
                         ;; must not be able to widen whose bytes a reviewer is
                         ;; shown — and these bytes are what they approve.
                         (filterv #(and (= (:org-id scope) (:translation/org-id %))
                                        (= (:project scope) (:translation/project %)))))
          receipts-by-relation (into {} (map (juxt receipt-relation identity)) completed)
          authored-relations (into #{} (map receipt-relation) authored)
          reviews (await
                   (p/all
                    (mapv #(hydrate-review! config index roots
                                            authored-relations receipts-by-relation %)
                          (:reviews result))))]
      {:reviews (vec reviews)})
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

(defn- ^:async send-reviewable!
  [reply operation]
  (try
    (fastify/send-json! reply 200
                        (resource-identity/encode-wire-values (await (operation))))
    (catch :default err
      (let [status (error-status err)]
        (when-not (error-body/classified? status)
          (fastify/log-unclassified-failure! "translation-review" err))
        (fastify/send-json! reply status (error-body/error-body err status))))))

(defn register-translation-review-routes!
  [app runtime config handlers]
  (fastify/route!
   app
   {:method "GET"
    :url "/api/publications/translations/reviews"
    :handler
    (^:async fn [request reply]
      (await
       ((:with-request-context! handlers) runtime request reply
        (^:async fn [ctx]
          (await
           (send-reviewable!
            reply
            (fn []
              ((:ensure-permission! handlers) ctx read-permission)
              (reviewable! config ctx))))))))})
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
