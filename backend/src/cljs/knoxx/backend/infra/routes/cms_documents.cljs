(ns knoxx.backend.infra.routes.cms-documents
  (:require [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.infra.cms-store :as store]
            [knoxx.backend.law.cms-document :as law]))

(defn list! [org query]
  (law/require-id! org)
  {:documents (->> (store/list! org)
                   (filter #(and (or (not (:garden_id query)) (= (:garden_id query) (:garden_id %)))
                                  (or (not (:path_prefix query))
                                      (= (:path_prefix query) (:source_path %))
                                      (some #{(:path_prefix query)} (:source_paths %))))) vec)})

(defn read! [org id]
  (law/require-id! org) (law/require-id! id)
  (or (store/read! org id) (throw (ex-info "CMS document not found" {:status 404}))))

(defn history! [org id]
  (law/require-id! org) (law/require-id! id)
  (store/history! org id))

(defn save! [org actor id body]
  (let [record (store/save! org actor id body)]
    (resources/invalidate-sync-resource-cache!)
    record))
