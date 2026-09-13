(ns knoxx.backend.infra.routes.cms-documents
  (:require [knoxx.backend.domain.cms-document :as domain]
            [knoxx.backend.domain.resources.loader :as resources]
            [knoxx.backend.extern.cms-store :as store]
            [knoxx.backend.law.cms-document :as law]))
(defn ^:async list! [org query]
  (law/require-id! org)
  {:documents (->> (await (store/list-records! org))
                   (filter #(and (or (not (:garden_id query)) (= (:garden_id query) (:garden_id %)))
                                  (or (not (:path_prefix query)) (= (:path_prefix query) (:source_path %))))) vec)})
(defn ^:async read! [org id]
  (law/require-id! org) (law/require-id! id)
  (or (await (store/read-record! org id)) (throw (ex-info "CMS document not found" {:status 404}))))
(defn ^:async save! [org id body]
  (law/require-id! org) (law/require-body! body)
  (let [previous (when id (await (read! org id)))
        id (or id (str (random-uuid)))
        source (:source (store/paths org id))
        record (domain/record org id source body previous)]
    (await (store/save! org record
                       (when-not previous (str (pr-str (domain/manifest org id source (:title record))) "\n"))
                       (when-not previous (str (pr-str (domain/garden org)) "\n"))))
    (resources/invalidate-sync-resource-cache!) record))
