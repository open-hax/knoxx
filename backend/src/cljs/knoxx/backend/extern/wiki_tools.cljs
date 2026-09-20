(ns knoxx.backend.extern.wiki-tools
  "Native agent tool adapter for the shared, freshly authorized Wiki commands."
  (:require [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.identity :as identity]
            [knoxx.backend.infra.wiki-commands :as commands]
            [knoxx.backend.infra.wiki-publication :as publication]
            [knoxx.backend.law.source-review :as law]
            [knoxx.backend.law.wiki-publication :as publish-law]
            [knoxx.backend.shape.source-authoring :as source]
            [knoxx.backend.shape.source-review :as review]
            [knoxx.backend.shape.wiki-commands :as shape]
            [malli.json-schema :as json-schema]))

(defn- document-command [schema]
  (into [:map {:closed true} [:document publish-law/QualifiedWireId]] (drop 2 schema)))

(def ^:private definitions
  [{:name "wiki_list" :description "List visible Wiki documents and the commands your role permits."
    :schema [:map {:closed true}]
    :run (fn [config ctx _input] (commands/list! config ctx))}
   {:name "wiki_read" :description "Read source, revision, review history and accepted writing lessons."
    :schema (document-command [:map {:closed true}])
    :run (fn [config ctx input] (commands/read! config ctx (:document input)))}
   {:name "wiki_create" :description "Create an owned Wiki article; it remains review-bound and unpublished."
    :schema source/WireCreate :run commands/create!}
   {:name "wiki_save" :description "Save complete source against expected_revision; stale updates are refused."
    :schema (document-command source/WireSave)
    :run (fn [config ctx input] (commands/save! config ctx (:document input) (dissoc input :document)))}
   {:name "wiki_review" :description "Comment, submit, request revision or accept the exact current source."
    :schema (document-command review/WireCommand)
    :run (fn [config ctx input] (commands/review! config ctx (:document input) (dissoc input :document)))}
   {:name "wiki_assist" :description "Request a writing proposal; applying it requires a separate revision-bound save."
    :schema (document-command shape/AssistWire)
    :run (fn [config ctx input] (commands/assist! config ctx (:document input) (dissoc input :document)))}
   {:name "wiki_publications" :description "Read desired publication placements and actual materialized revisions."
    :schema (document-command [:map {:closed true}])
    :run (fn [config ctx input] (publication/list! config ctx (:document input)))}
   {:name "wiki_publish" :description "Publish one accepted, owned placement; all source and translation gates apply."
    :schema (document-command publish-law/PublishWire)
    :run (fn [config ctx input] (publication/publish! config ctx (:document input) (dissoc input :document)))}])

(defn- execute [server-runtime config captured {:keys [schema run]}]
  (^:async fn [_call-id params & _options]
    (let [input (if (map? params) params (js->clj params :keywordize-keys true))
          _ (law/assert-valid! :wiki/tool-input schema input)
          ctx (await (identity/current-context! (authz/policy-db server-runtime) captured))
          result (await (run config ctx input))
          details (clj->js result)]
      #js {:content #js [#js {:type "text" :text (js/JSON.stringify details)}]
           :details details})))

(defn create-wiki-tools
  "Offer the same commands as human controls; every invocation re-resolves credentials."
  [server-runtime config ctx]
  (let [available (set (:commands (commands/grants ctx)))]
    (into-array
     (map (fn [{:keys [name description schema] :as definition}]
            #js {:name name :label name :description description
                 :parameters (clj->js (json-schema/transform schema))
                 :execute (execute server-runtime config ctx definition)})
          (filter #(contains? available (:name %)) definitions)))))

(defn append-wiki-tools
  "Join native tool collections at their owning JavaScript boundary."
  [existing server-runtime config ctx]
  (.concat existing (create-wiki-tools server-runtime config ctx)))
