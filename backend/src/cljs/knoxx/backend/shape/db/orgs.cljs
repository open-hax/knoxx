(ns knoxx.backend.shape.db.orgs
  "Pure HoneySQL query builders for orgs and data-lakes. No execution."
  (:require [honey.sql.helpers :as h]))

;; ---------------------------------------------------------------------------
;; Orgs
;; ---------------------------------------------------------------------------

(defn by-id
  [org-id]
  (-> (h/select :*)
      (h/from :orgs)
      (h/where [:= :id [:cast org-id :uuid]])))

(defn by-slug
  [slug]
  (-> (h/select :*)
      (h/from :orgs)
      (h/where [:= [:lower :slug] [:lower slug]])
      (h/limit 1)))

(defn list-with-counts
  []
  (-> (h/select :o.*
                [[:raw "COUNT(DISTINCT m.id)"] :member_count]
                [[:raw "COUNT(DISTINCT r.id)"] :role_count]
                [[:raw "COUNT(DISTINCT d.id)"] :data_lake_count])
      (h/from [:orgs :o])
      (h/left-join [:memberships :m] [:= :m.org_id :o.id])
      (h/left-join [:roles :r] [:= :r.org_id :o.id])
      (h/left-join [:data_lakes :d] [:= :d.org_id :o.id])
      (h/group-by :o.id)
      (h/order-by [:o.is_primary :desc] :o.name)))

(defn upsert-primary
  [{:keys [slug name kind]}]
  (-> (h/insert-into :orgs)
      (h/values [{:slug       slug
                  :name       name
                  :kind       kind
                  :is_primary true
                  :status     "active"}])
      (h/on-conflict :slug)
      (h/do-update-set {:name       [:raw "EXCLUDED.name"]
                         :kind       [:raw "EXCLUDED.kind"]
                         :is_primary true
                         :updated_at [:now]})
      (h/returning :*)))

(defn clear-primary-except
  [slug]
  (-> (h/update :orgs)
      (h/set {:is_primary [:case [:= :slug slug] true :else false]})))

(defn insert
  [{:keys [slug name kind status]}]
  (-> (h/insert-into :orgs)
      (h/values [{:slug       slug
                  :name       name
                  :kind       kind
                  :is_primary false
                  :status     status}])
      (h/returning :*)))

;; ---------------------------------------------------------------------------
;; Data lakes
;; ---------------------------------------------------------------------------

(defn data-lake-by-org
  [org-id]
  (-> (h/select :*)
      (h/from :data_lakes)
      (h/where [:= :org_id [:cast org-id :uuid]])
      (h/order-by :name)))

(defn insert-data-lake
  [{:keys [org-id name slug kind config-json status]}]
  (-> (h/insert-into :data_lakes)
      (h/values [{:org_id      [:cast org-id :uuid]
                  :name        name
                  :slug        slug
                  :kind        kind
                  :config_json [:raw config-json]
                  :status      status}])
      (h/returning :*)))
