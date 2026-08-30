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
            [knoxx.backend.domain.translation-review-inventory :as inventory]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-candidate-content :as candidate-content]
            [knoxx.backend.infra.translation-content-integrity :as content-integrity]
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
   :translation-content-unbound 409
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
                          config documents roots))
        work (mapv #(assoc %
                           :translation/org-id (:org-id scope)
                           :translation/project (:project scope))
                   (inventory/desired-work index revisions))
        authored (await (contract-content/ensure-receipts!
                         evidence-store index roots scope revisions))]
    {:index index
     :roots roots
     ;; The inventory resolves every desired `:source/current` relation from
     ;; this same snapshot.  Discarding the map here forced the old read path to
     ;; start from completed receipts, because a missing candidate otherwise had
     ;; no concrete revision to put on the wire.  That inverted authority: one
     ;; receipt yielded one row even when the resource graph declared eighteen
     ;; pieces of translation work.
     :source-revisions revisions
     :work work
     :authored authored}))

(defn- ^:async authenticated-contract-receipts!
  "One content-authenticated receipt snapshot for review and approval.

  Authored history is first normalized against *desired work*, so deleting a
  declared locale file retires its old receipt instead of leaving a ready row
  nobody can inspect. Agent receipts then have to resolve to their exact
  digest-bound content entry."
  [config evidence-store scope {:keys [index roots work authored]}]
  (let [stored (await (evidence-store-api/completed-translations!
                       evidence-store (select-keys scope [:org-id :project])))
        normalized (contract-content/current-authored-receipts
                    stored authored work)
        desired-relations (set (map inventory/work-key work))]
    (await (candidate-content/authenticated-receipts!
            (:publication-content-root config)
            roots
            (:documents index)
            authored
            (filterv #(and (= (:org-id scope) (:translation/org-id %))
                            (= (:project scope) (:translation/project %))
                            (contains? desired-relations
                                       (inventory/work-key %)))
                     normalized)))))

(defn- relation-key
  "The six coordinates that identify one translated revision.

   Shared by receipts and by the wire reviews built from them, so the two can be
   joined without either side inventing an ordering. Source revision AND output
   revision are both present because an approval is revision-specific on both
   sides — see `law.translation-evidence/approval-current?`."
  [document garden source-locale locale revision translation-revision]
  [document garden source-locale locale revision translation-revision])

(defn- receipt-relation
  [receipt]
  (relation-key (:translation/document receipt)
                (:translation/garden receipt)
                (:translation/source-locale receipt)
                (:translation/locale receipt)
                (:translation/source-revision receipt)
                (:translation/revision receipt)))

(defn- review-relation
  [review]
  (relation-key (:document review)
                (:garden review)
                (:source_locale review)
                (:locale review)
                (:revision review)
                (:translation_revision review)))

(defn- ^:async contract-candidate-content!
  "Load candidate columns and independently digest the source bytes read now."
  [config roots document locale authored? receipt]
  (let [root (get roots (:document/id document))
        source (await (contract-content/source-content! root document))
        translated (if authored?
                     (await (contract-content/localized-content!
                             root document locale))
                     (await (agent-content/content-for-receipt!
                             (:publication-content-root config) receipt)))]
    {:source source
     :source-revision (source-revision/content-revision source)
     :translated translated
     :translated-current? (content-integrity/authenticated-content?
                           receipt translated)
     :content-source (if authored? :authored-contract :agent)}))

(defn- displayable-content?
  "Whether a review column contains bytes a person can actually inspect."
  [content]
  (and (string? content) (not (str/blank? content))))

(defn- hydrated-contract-review
  "Project loaded candidate bytes without inventing reviewability."
  [review document {:keys [source source-revision translated translated-current?
                           content-source]}]
  (let [source-present? (displayable-content? source)
        translated-present? (displayable-content? translated)
        source-current? (and source-present?
                             (= (:revision review) source-revision))
        displayable? (and source-current? translated-present?
                          translated-current?)
        hydration-state (cond
                          (not source-present?) :content_missing
                          (not source-current?) :source_moved
                          (not translated-present?) :content_missing
                          (not translated-current?) :content_moved
                          :else :displayable)]
    (cond-> (assoc review
                   :title (:document/title document)
                   :source_locale (:document/source-locale document)
                   :contract_candidate true
                   :reviewable displayable?
                   :hydration_state hydration-state)
      displayable? (assoc :source_text source
                          :translated_text translated
                          :content_source content-source))))

(defn- ^:async hydrate-review!
  "Attach the exact bytes a reviewer must see before approval.

   Precedence mirrors publication rendering: receipt-bound agent content, then
   authored locale content. `:content_source` preserves that distinction, and
   `:reviewable` is reduced to false whenever either display column is missing."
  [config index roots authored-relations receipts-by-relation review]
  (let [document (get-in index [:documents (:document review)])]
    (if (or (nil? document)
            (not (:candidate_present review)))
      ;; Not a document these contracts own. It belongs to the OpenPlanner
      ;; worker path and is joined to its own document row on the client.  A
      ;; resource work item with no candidate also stops here: it must remain in
      ;; the inventory, but there is no receipt-bound output to hydrate and no
      ;; approval control may be rendered over it.
      review
      (let [relation (review-relation review)
            authored? (contains? authored-relations relation)
            receipt (get receipts-by-relation relation)
            content (await (contract-candidate-content!
                            config roots document (:locale review)
                            authored? receipt))]
        ;; Resource ownership stays explicit even when bytes are missing, so
        ;; the client cannot fall through to a same-named legacy Mongo row.
        (hydrated-contract-review review document content)))))

(defn- matching-resource-work
  "Find the exact desired resource relation attested by `receipt`."
  [index source-revisions receipt]
  (let [relation (inventory/work-key receipt)]
    (some #(when (= relation (inventory/work-key %)) %)
          (mapv (fn [work]
                  (assoc work
                         :translation/org-id (:translation/org-id receipt)
                         :translation/project (:translation/project receipt)))
                (inventory/desired-work index source-revisions)))))

(defn- matching-request-work
  "Resolve a client approval onto server-owned source-locale work."
  [index source-revisions scope request]
  (some (fn [work]
          (when (= [(:review/document request)
                    (:review/garden request)
                    (:review/locale request)
                    (:review/revision request)]
                   [(:translation/document work)
                    (:translation/garden work)
                    (:translation/locale work)
                    (:translation/source-revision work)])
            work))
        (mapv #(assoc %
                      :translation/org-id (:org-id scope)
                      :translation/project (:project scope))
              (inventory/desired-work index source-revisions))))

(defn- ^:async authorize-resource-approval!
  "Require declared resource work and both exact display columns before write."
  [config {:keys [index roots source-revisions authored]} _request receipt]
  (let [work (matching-resource-work index source-revisions receipt)]
    (when-not work
      (throw (ex-info "translation candidate is not declared resource work"
                      {:status 409
                       :code "translation_candidate_not_declared"})))
    (let [document (get-in index [:documents (:translation/document work)])
          authored? (contains? (into #{} (map receipt-relation) authored)
                               (receipt-relation receipt))
          content (await (contract-candidate-content!
                          config roots document (:translation/locale work)
                          authored? receipt))]
      (when-not (and (displayable-content? (:source content))
                     (= (:translation/source-revision receipt)
                        (:source-revision content))
                     (displayable-content? (:translated content))
                     (:translated-current? content))
        (throw (ex-info "translation candidate content is unavailable for review"
                        {:status 409
                         :code "translation_candidate_content_unavailable"})))
      true)))

(defn- ^:async approve!
  [config ctx decoded]
  (if-let [evidence-store (registry/current)]
    (let [scope (review-scope config ctx)
          contract-snapshot (await (ensure-contract-receipts!
                                    config evidence-store scope))
          receipts (await (authenticated-contract-receipts!
                           config evidence-store scope contract-snapshot))
          requested-work (matching-request-work
                          (:index contract-snapshot)
                          (:source-revisions contract-snapshot)
                          scope
                          decoded)]
      (facade/approve-translation!
       {:evidence-store evidence-store
        :clock (fn [] (.toISOString (js/Date.)))
        ;; The request cannot supply source locale. Resolve it from current
        ;; resource work before receipts are indexed, or an A→B locale history
        ;; with equal revisions can make the wrong receipt win the 4-D index.
        :receipt-admissible?
        (fn [receipt]
          (and requested-work
               (= (inventory/work-key requested-work)
                  (inventory/work-key receipt))))
        :receipts-snapshot receipts
        :authorize-approval!
        (partial authorize-resource-approval! config contract-snapshot)}
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
          {:keys [index roots source-revisions authored] :as contract-snapshot}
          (await (ensure-contract-receipts! config evidence-store scope))
          completed (await (authenticated-contract-receipts!
                            config evidence-store scope contract-snapshot))
          result (await
                  (facade/reviewable-translations!
                   {:evidence-store evidence-store
                    :publication-index index
                    :source-revisions source-revisions
                    :receipts-snapshot completed}
                   scope))
          receipts-by-relation (into {} (map (juxt receipt-relation identity)) completed)
          authored-relations (into #{} (map receipt-relation) authored)
          reviews (await
                   (p/all
                    (mapv #(hydrate-review! config index roots
                                            authored-relations receipts-by-relation %)
                          (:reviews result))))]
      {:project (:project result)
       :reviews (vec reviews)})
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
