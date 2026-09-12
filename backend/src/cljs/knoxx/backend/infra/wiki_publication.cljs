(ns knoxx.backend.infra.wiki-publication
  "Owned, revision-bound publication commands through the canonical reconciler."
  (:require [knoxx.backend.domain.cms-publication :as cms]
            [knoxx.backend.domain.publication-gate :as gate]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.domain.wiki-publication :as domain]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.infra.publication-reconciler :as reconciler]
            [knoxx.backend.infra.publication-runtime :as production]
            [knoxx.backend.infra.publication-target-registry :as registry]
            [knoxx.backend.infra.routes.cms-publication :as facade]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.infra.wiki-commands :as commands]
            [knoxx.backend.infra.wiki-runtime :as runtime]
            [knoxx.backend.law.wiki-publication :as law]
            [knoxx.backend.shape.resource-identity :as identity]
            [knoxx.backend.shape.source-review :as review-wire]
            [knoxx.backend.shape.wiki-publication :as wire]))

(defn- ^:async composition! [config scope]
  (production/make-runtime! config (select-keys scope [:org-id :project])
                            (reconciler/make-receipt-journal)))

(defn- ^:async observed-view! [reconciler ctx intent]
  (let [target (registry/resolve-target! (:registry reconciler) (:publication/target intent))
        observed (await (effects/observe! target ctx intent))
        facts ((:evidence-facts reconciler) intent)]
    (cms/publication->wire {:observed observed :blockers (:blockers (gate/gate intent facts))} intent)))

(defn ^:async list!
  "Read both desired intent and actual manifest-backed materialization."
  [config ctx document]
  (commands/ensure-command! ctx "wiki_publications" "publication/read")
  (let [scope (commands/scope config ctx document)
        {:keys [reconciler]} (await (composition! config scope))
        index (await ((:load-index! reconciler)))
        intents (resolver/desired-publications index (:document scope))
        views (atom [])]
    (doseq [intent intents]
      (swap! views conj (await (observed-view! reconciler ctx intent))))
    {:document (identity/encode-keyword (:document scope)) :publications @views}))

(defn- ^:async accepted-snapshot! [config scope expected]
  (let [snapshot (await (review/read! config scope (runtime/source-dependencies)))]
    (when-not (= expected (:revision snapshot))
      (throw (ex-info "Source changed; refresh before publishing"
                      {:status 409 :code "wiki_publication_stale_revision"})))
    (when-not (:accepted snapshot)
      (throw (ex-info "Source content must be accepted before publishing"
                      {:status 409 :code "wiki_source_review_required"})))
    snapshot))

(defn- ^:async revision-operation! [config scope expected operation]
  (let [snapshot (await (review/read! config scope (runtime/source-dependencies)))]
    (when-not (= expected (:revision snapshot))
      (throw (ex-info "Source changed; refresh before changing publication"
                      {:status 409 :code "wiki_publication_stale_revision"})))
    (await (operation))))

(defn ^:async with-owned-revision!
  "Guard compatibility writes with the same owner, revision and source lock."
  [config ctx publication expected operation]
  (commands/ensure-command! ctx "wiki_publish" "publication/publish")
  (law/assert-command! {:publication (identity/encode-keyword publication)
                        :expected_revision expected})
  (let [index (await (publications/publication-index! config))
        intent (or (some #(when (= publication (:publication/id %)) %) (:publications index))
                   (throw (ex-info "Owned publication was not found"
                                   {:status 404 :code "wiki_publication_not_found"})))
        scope (commands/scope config ctx (identity/encode-keyword (:publication/document intent)))]
    (domain/owned-intent! index scope publication)
    (await (files/with-document-lock! scope
             #(revision-operation! config scope expected operation)))))

(defn- ^:async publish-locked! [config ctx scope body]
  (let [publication (keyword (:publication body))
        index (await (publications/publication-index! config))
        _ (domain/owned-intent! index scope publication)
        snapshot (await (accepted-snapshot! config scope (:expected_revision body)))
        _ (await (facade/set-publication-state! config scope publication {:publication/state :published}))
        {:keys [reconciler]} (await (composition! config scope))
        receipt (await (reconciler/reconcile!
                        reconciler {:trigger/id :wiki/publish :trigger/origin :route
                                    :publication/id publication}))]
    (commands/changed! scope)
    {:receipt (wire/receipt->wire receipt)
     :review (commands/decorate ctx (review-wire/projection->wire snapshot))}))

(defn ^:async publish!
  "Publish exactly one owned placement under the same lock as source edits."
  [config ctx document body]
  (commands/ensure-command! ctx "wiki_publish" "publication/publish")
  (law/assert-command! body)
  (let [scope (commands/scope config ctx document)]
    (await (files/with-document-lock! scope #(publish-locked! config ctx scope body)))))
