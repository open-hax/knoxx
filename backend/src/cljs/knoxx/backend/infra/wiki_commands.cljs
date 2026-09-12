(ns knoxx.backend.infra.wiki-commands
  "Shared authorization, commands and invalidations for Wiki humans and agents."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.wiki-capabilities :as capabilities]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.publication-admission-hook :as admission]
            [knoxx.backend.infra.source-authoring :as authoring]
            [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.infra.wiki-runtime :as runtime]
            [knoxx.backend.infra.writing-model :as model]
            [knoxx.backend.law.source-review :as review-law]
            [knoxx.backend.shape.source-authoring :as source-wire]
            [knoxx.backend.shape.source-review :as review-wire]
            [knoxx.backend.shape.wiki-commands :as shape]))

(defonce ^:private listeners* (atom {}))

(defn scope
  "Derive tenant and project from verified context and application configuration."
  ([config ctx]
   (let [org (authz/ctx-org-id ctx)]
     (when-not (and (string? org) (seq org))
       (throw (ex-info "An authenticated organization is required"
                       {:status 403 :code "wiki_context_required"})))
     {:org-id org :project (:session-project-name config)}))
  ([config ctx document]
   (review-law/assert-valid!
    :wiki/scope review-law/Scope
    (assoc (scope config ctx) :document (if (keyword? document) document (keyword document))))))

(defn actor
  "Build source attribution from the verified principal, never the command body."
  [ctx]
  (review-law/assert-valid!
   :wiki/actor review-law/Actor
   {:id (or (authz/ctx-actor-id ctx) (authz/ctx-user-id ctx))
    :kind (keyword (or (get-in ctx [:actor :kind]) (:principal-kind ctx) :human))}))

(defn ensure-command!
  "Enforce one registered command/capability pairing, with explicit deny first."
  [ctx command capability]
  (when (or (not (some #{capability} (capabilities/command-capabilities command)))
            (capabilities/explicitly-denied? (or (:tool-policies ctx) (:toolPolicies ctx)) command))
    (throw (ex-info "Wiki command is denied"
                    {:status 403 :code "wiki_command_denied"})))
  (authz/ensure-permission! ctx (capabilities/permission capability)))

(defn grants
  "Advertise exactly the capabilities and commands checked by these handlers."
  [ctx]
  (let [permissions (authz/ctx-permissions ctx)
        admin? (authz/system-admin? ctx)
        policies (or (:tool-policies ctx) (:toolPolicies ctx))]
    {:capabilities (capabilities/available permissions admin? policies)
     :commands (capabilities/available-commands permissions admin? policies)}))

(defn decorate
  "Attach current grants to the source snapshot returned to either interface."
  [ctx projection]
  (merge projection (grants ctx)))

(defn subscribe!
  "Subscribe to process-local invalidation hints; returned closure releases it."
  [listener]
  (let [id (random-uuid)]
    (swap! listeners* assoc id listener)
    #(swap! listeners* dissoc id)))

(defn changed!
  "Notify views to reload durable authority; hints never carry content or grant access."
  [change-scope]
  (doseq [listener (vals @listeners*)]
    (try
      (listener (select-keys change-scope [:org-id :project :document]))
      (catch :default error
        (fastify/log-unclassified-failure! "wiki-change-listener" error)))))

(defn- result-wire
  [ctx result encoder]
  (update (encoder result) :review #(decorate ctx %)))

(defn ^:async list!
  "List visible resources with the caller's current interface capabilities."
  [config ctx]
  (ensure-command! ctx "wiki_list" "publication/read")
  (decorate ctx (await (authoring/list! config (scope config ctx) (runtime/source-dependencies)))))

(defn ^:async read!
  "Read current content and its immutable revision-bound review history."
  [config ctx document]
  (ensure-command! ctx "wiki_read" "publication/read")
  (decorate ctx (review-wire/projection->wire
                 (await (review/read! config (scope config ctx document) (runtime/source-dependencies))))))

(defn ^:async create!
  "Create one owned document and notify views only after its durable command returns."
  [config ctx body]
  (ensure-command! ctx "wiki_create" "publication/write")
  (let [result (await (authoring/create! config (scope config ctx) (actor ctx)
                                        (source-wire/decode-create body) (runtime/source-dependencies)))]
    (changed! (scope config ctx (get-in result [:review :document])))
    (result-wire ctx result source-wire/result->wire)))

(defn ^:async save!
  "Save source at the exact revision seen by the caller, preserving stale drafts."
  [config ctx document body]
  (ensure-command! ctx "wiki_save" "publication/write")
  (let [review-scope (scope config ctx document)
        result (await (authoring/save! config review-scope (actor ctx) (source-wire/decode-save body)
                                      (runtime/source-dependencies)))]
    (changed! review-scope)
    (result-wire ctx result source-wire/result->wire)))

(defn ^:async review!
  "Apply the same action-specific authority for a human control or an agent tool."
  [config ctx document body]
  (let [command (review-wire/decode-command body)
        capability (if (contains? #{:accept :request-changes} (:action command))
                     "publication/review" "publication/write")
        _ (ensure-command! ctx "wiki_review" capability)
        review-scope (scope config ctx document)
        admission-scope (when (= :accept (:action command))
                          (let [membership (authz/ctx-membership-id ctx)]
                            (when (or (not (string? membership)) (str/blank? membership))
                              (throw (ex-info "Source acceptance requires an authenticated membership"
                                              {:status 403 :code "wiki_membership_required"})))
                            (assoc (select-keys review-scope [:org-id :project]) :membership-id membership)))
        result (await (review/command! config review-scope (actor ctx) command (runtime/source-dependencies)))]
    (changed! review-scope)
    (when admission-scope
      (await (admission/admit! admission-scope {:document (:document review-scope)})))
    (result-wire ctx result review-wire/result->wire)))

(defn ^:async assist!
  "Bind a writing proposal to the source bytes observed before model execution."
  [config ctx document body]
  (ensure-command! ctx "wiki_assist" "publication/assist")
  (review-law/assert-valid! :wiki/assist shape/AssistWire body)
  (let [input (await (authoring/assist-input! config (scope config ctx document)
                                            (runtime/source-dependencies)))]
    (when-not (= (:revision input) (:expected_revision body))
      (throw (ex-info "Source changed; refresh before requesting a proposal"
                      {:status 409 :code "wiki_assist_stale_revision"})))
    (assoc (await (model/generate! config (assoc input :instruction (:instruction body))))
           :source_revision (:revision input))))
