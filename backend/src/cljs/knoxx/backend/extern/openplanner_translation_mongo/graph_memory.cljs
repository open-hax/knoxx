(ns knoxx.backend.extern.openplanner-translation-mongo.graph-memory
  "Graph-memory writes for approved translation segments.

  A segment contributes both a node and an edge. Mongo gives no cross-collection
  atomicity without a replica-set session, so the two writes are ordered and the
  first is rolled back when the second fails. Graph memory must never hold a
  node without its edge, or an edge pointing at a node that was never written."
  (:require [knoxx.backend.extern.openplanner-translation-mongo.common :as common]
            [openplanner.translations.core :as translation]))

(defn- graph-memory-plan-for
  "Map a raw Mongo segment and its correction into the shared graph-memory plan input."
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

(defn- duplicate-key-error?
  [err]
  (or (= 11000 (.-code err))
      (boolean (some-> (.-message err) (.includes "E11000")))))

(defn- ^:async upsert-element-once!
  [collection element now write-token]
  (let [result (await
                (.findOneAndUpdate collection
                                   #js {"id" (:id element)}
                                   #js {"$set" (clj->js (assoc element
                                                               :updated_at now
                                                               :graph_write_token write-token))
                                        "$setOnInsert" #js {"created_at" now}}
                                   #js {"upsert" true
                                        "returnDocument" "before"
                                        "includeResultMetadata" true}))]
    (common/jget result "value")))

(defn- ^:async upsert-graph-element!
  "Upsert one planned graph node or edge; returns the document as it was before.

  A nil prior document means this call inserted it, which is what the rollback
  path needs in order to distinguish a delete from a restore. `write-token`
  stamps the document so a rollback can recognize its own write.

  Two upserts racing on an absent `id` both try to insert, and the unique index
  rejects the loser with a duplicate key error. Retrying once resolves it: the
  document now exists, so the retry takes the update branch."
  [collection element now write-token]
  (try
    (await (upsert-element-once! collection element now write-token))
    (catch :default err
      (if (duplicate-key-error? err)
        (await (upsert-element-once! collection element now write-token))
        (throw err)))))

(defn- ^:async restore-graph-element!
  "Undo one upsert, but only while the document still holds this write.

  Two approvals of the same segment can overlap: A upserts the node, B
  overwrites it and lands its edge, then A's edge fails. An unconditional
  rollback would erase B's newer node and strand B's edge, so both the restore
  and the delete are guarded on `write-token`. Returns true when this write was
  still the current one and the rollback applied."
  [collection element-id prior write-token]
  (let [selector #js {"id" element-id "graph_write_token" write-token}]
    (if prior
      (let [replacement (js/Object.assign #js {} prior)]
        (js-delete replacement "_id")
        (let [result (await (.replaceOne collection selector replacement))]
          (pos? (or (common/jget result "matchedCount") 0))))
      (let [result (await (.deleteOne collection selector))]
        (pos? (or (common/jget result "deletedCount") 0))))))

(defn- ^:async rollback-graph-node!
  "Best-effort rollback of the node write; never masks the originating error."
  [collection node-id prior write-token]
  (try
    (when-not (await (restore-graph-element! collection node-id prior write-token))
      (js/console.warn "[translation] graph node" node-id
                       "was rewritten concurrently; leaving the newer write in place"))
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
              write-token (.randomUUID js/crypto)
              nodes (:graph-nodes collection-map)
              node-id (get-in plan [:node :id])
              node-prior (await (upsert-graph-element! nodes (:node plan) now write-token))]
          (try
            (await (upsert-graph-element! (:graph-edges collection-map)
                                          (:edge plan) now write-token))
            {:success true}
            (catch :default err
              (await (rollback-graph-node! nodes node-id node-prior write-token))
              {:success false :error (or (.-message err) (str err))})))
        (catch :default err
          {:success false :error (or (.-message err) (str err))})))))
