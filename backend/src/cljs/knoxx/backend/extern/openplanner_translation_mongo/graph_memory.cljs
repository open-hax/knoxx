(ns knoxx.backend.extern.openplanner-translation-mongo.graph-memory
  "Graph-memory writes for approved translation segments.

  A segment contributes both a node and an edge. Mongo gives no cross-collection
  atomicity without a replica-set session, so the two writes are ordered and the
  first is rolled back when the second fails. Graph memory must never hold a
  node without its edge, or an edge pointing at a node that was never written."
  (:require [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [openplanner.translations.core :as translation]))

(defn- graph-memory-plan-for
  [segment corrected-text]
  (translation/graph-memory-plan
   {:segment-id (common/string-id segment)
    :source-text (common/jget segment "source_text")
    :translated-text (common/jget segment "translated_text")
    :corrected-text corrected-text
    :source-lang (common/jget segment "source_lang")
    :target-lang (common/jget segment "target_lang")
    :document-id (common/jget segment "document_id")
    :domain (common/jget segment "domain")
    :content-type (common/jget segment "content_type")}))

(defn- ^:async upsert-graph-element!
  "Upsert one planned graph node or edge; returns the document as it was before.

  A nil prior document means this call inserted it, which is what the rollback
  path needs in order to distinguish a delete from a restore."
  [collection element now]
  (let [result (await
                (.findOneAndUpdate collection
                                   #js {"id" (:id element)}
                                   #js {"$set" (clj->js (assoc element :updated_at now))
                                        "$setOnInsert" #js {"created_at" now}}
                                   #js {"upsert" true
                                        "returnDocument" "before"
                                        "includeResultMetadata" true}))]
    (common/jget result "value")))

(defn- ^:async restore-graph-element!
  "Undo one upsert: put the prior document back, or remove one we inserted."
  [collection element-id prior]
  (if prior
    (let [replacement (js/Object.assign #js {} prior)]
      (js-delete replacement "_id")
      (await (.replaceOne collection #js {"id" element-id} replacement)))
    (await (.deleteOne collection #js {"id" element-id}))))

(defn- ^:async rollback-graph-node!
  "Best-effort rollback of the node write; never masks the originating error."
  [collection node-id prior]
  (try
    (await (restore-graph-element! collection node-id prior))
    (catch :default err
      (js/console.error "[translation] graph node rollback failed for" node-id
                        (or (.-message err) (str err))))))

(defn ^:async upsert-graph-memory!
  "Upsert an approved segment into graph memory, returning {:success ...}.

  Either both the node and the edge are present, or neither is."
  [collection-map segment corrected-text]
  (let [plan (graph-memory-plan-for segment corrected-text)]
    (if-not (:ok? plan)
      {:success false :error (:error plan)}
      (try
        (let [now (js/Date.)
              nodes (:graph-nodes collection-map)
              node-id (get-in plan [:node :id])
              node-prior (await (upsert-graph-element! nodes (:node plan) now))]
          (try
            (await (upsert-graph-element! (:graph-edges collection-map) (:edge plan) now))
            {:success true}
            (catch :default err
              (await (rollback-graph-node! nodes node-id node-prior))
              {:success false :error (or (.-message err) (str err))})))
        (catch :default err
          {:success false :error (or (.-message err) (str err))})))))
