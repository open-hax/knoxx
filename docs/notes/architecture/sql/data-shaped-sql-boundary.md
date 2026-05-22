---
original_name: "2026.05.20.20.06.39.md"
title: "Data Shaped SQL Boundary"
summary: "Recommendation to replace raw SQL strings with HoneySQL data-shaped query builders at the boundary."
category: "architecture"
created: "2026-05-20"
---

large share of the module still builds SQL as raw strings passed to `query!` and `query-one!`. So the useful move is not “introduce HoneySQL,” but “replace the remaining string SQL with data-shaped query builders at the boundary.” 
## Facts

The attached namespace already centralizes formatting through:

```clojure
(defn- honey-sql [honey-map]
  (sql/format honey-map :numbered true))

(defn- honey-query! [pool honey-map]
  (let [[sql-str & params] (honey-sql honey-map)]
    (query! pool sql-str params)))

(defn- honey-query-one! [pool honey-map]
  (let [[sql-str & params] (honey-sql honey-map)]
    (query-one! pool sql-str params)))
```

That means your architecture is already prepared for “query as data, execution as infra boundary,” which matches the rest of the file’s separation between helpers and DB effects
## Interpretation

The best candidate to rewrite first is `find-request-membership-row`, because it has repeated `SELECT ... JOIN ... WHERE ... ORDER BY ... LIMIT` string blocks and branching variants that HoneySQL can express cleanly as pure shape transformations`find-org-by-slug`, `factory-list-data-lakes`, and several insert/update paths are also straightforward wins, but `find-request-membership-row` gives the biggest readability payoff per line changed
## Rewrite

Here is a HoneySQL rewrite of the repeated membership lookup logic using small pure builders and one effectful executor:

```clojure
(ns knoxx.backend.infra.db.policy
  (:require
    [clojure.string :as str]
    [honey.sql :as sql]
    [honey.sql.helpers :as h]))

(def membership-base-query
  (-> (h/select
        :m.*
        [:u.email]
        [:u.displayname]
        [:u.status :userstatus]
        [:o.slug :orgslug]
        [:o.name :orgname]
        [:o.status :orgstatus]
        :o.isprimary
        [:o.kind :orgkind])
      (h/from [:memberships :m])
      (h/join [:users :u] [:= :u.id :m.userid])
      (h/join [:orgs :o] [:= :o.id :m.orgid])))

(defn- membership-by-id-query [membership-id]
  (-> membership-base-query
      (h/where [:= :m.id [:cast membership-id :uuid]])))

(defn- membership-by-email-and-org-query [{:keys [user-email org-id org-slug]}]
  (cond
    (and (some? org-id) (not (str/blank? (str org-id))))
    (-> membership-base-query
        (h/where
          [:and
           [:= [:lower :u.email] [:lower user-email]]
           [:= :o.id [:cast org-id :uuid]]])
        (h/order-by [:m.isdefault :desc] [:m.createdat :asc])
        (h/limit 1))

    (and (some? org-slug) (not (str/blank? (str org-slug))))
    (-> membership-base-query
        (h/where
          [:and
           [:= [:lower :u.email] [:lower user-email]]
           [:= [:lower :o.slug] [:lower org-slug]]])
        (h/order-by [:m.isdefault :desc] [:m.createdat :asc])
        (h/limit 1))

    :else
    (-> membership-base-query
        (h/where [:= [:lower :u.email] [:lower user-email]])
        (h/order-by [:m.isdefault :desc] [:o.isprimary :desc] [:m.createdat :asc])
        (h/limit 1))))

(defn- find-request-membership-row [pool headers-like]
  (let [membership-id (header-value headers-like "x-knoxx-membership-id")
        user-email    (some-> (header-value headers-like "x-knoxx-user-email")
                              str/lower-case)
        org-id        (header-value headers-like "x-knoxx-org-id")
        org-slug      (some-> (header-value headers-like "x-knoxx-org-slug")
                              str/lower-case)]
    (cond
      (and (str/blank? (str membership-id))
           (str/blank? (str user-email)))
      (js/Promise.reject
        (http-error 401
                    "Knoxx request context is missing x-knoxx-user-email or x-knoxx-membership-id"
                    "request-context-missing"))

      (not (str/blank? (str membership-id)))
      (honey-query-one! pool (membership-by-id-query membership-id))

      :else
      (honey-query-one! pool
                        (membership-by-email-and-org-query
                          {:user-email user-email
                           :org-id org-id
                           :org-slug org-slug})))))
```

## Smaller rewrites

These simpler functions in your file are near-zero-risk HoneySQL conversions because they are single-query lookups with no unusual SQL constructs
```clojure
(defn- find-org-by-slug [pool slug]
  (if (str/blank? (str slug))
    (js/Promise.resolve nil)
    (honey-query-one!
      pool
      (-> (h/select :*)
          (h/from :orgs)
          (h/where [:= [:lower :slug] [:lower slug]])
          (h/limit 1)))))

(defn- factory-list-data-lakes [pool opts]
  (let [org-id (aget opts "orgId")]
    (if (str/blank? (str org-id))
      (js/Promise.reject (js/Error. "orgId is required"))
      (->> (-> (h/select :*)
               (h/from :datalakes)
               (h/where [:= :orgid [:cast org-id :uuid]])
               (h/order-by [:name :asc]))
           (honey-query! pool)
           (.then (fn [r]
                    (js-obj "dataLakes"
                            (into-array (map -js-data-lake (aget r "rows"))))))))))
```

## Pattern

A consistent pattern for this file is:

- `shape.db.*` style pure builders: return HoneySQL maps only.
- `infra.db.*` executors: call `honey-query!` / `honey-query-one!`.
- `law.*` validators: check incoming ids, slugs, and payload contracts before formatting or execution.

That pattern fits the file’s current structure much better than embedding more SQL strings inside factory methods, and it keeps the “query grammar” separate from the “admissibility contract” checks you’re already doing around request context, role resolution, and payload normalization
Which function should be converted next: `ensure-role!`, `factory-create-user`, or `factory-set-membership-roles`?
