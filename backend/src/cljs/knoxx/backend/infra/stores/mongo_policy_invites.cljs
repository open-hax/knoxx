(ns knoxx.backend.infra.stores.mongo-policy-invites
  "Mongo-backed invite storage — slice 6 of the PG policy DB migration
   (kanban 14-04): the invites table.

   Row-shape adapter contract: documents store snake_case keys matching the
   PG columns. invite-doc->row re-presents :invite_id as :id and drops
   the Mongo :_id, matching the PG invite-row->map adapter in policy.cljs.

   DISPATCH SEAM: these functions are ROW-LEVEL twins of the
   shape.db.invites query builders. Future flag-dispatch must route at
   the query seam inside policy/create-invite!, policy/redeem-invite!,
   and policy/list-invites!.

   Documents are stamped with :system_instance_id like other policy twins."
  (:require
    [clojure.string :as str]
    [knoxx.backend.infra.mongo-client :as mongo-client]
    [knoxx.backend.infra.system-instance :as system-instance]))

(def INVITES_COLLECTION "knoxx_invites")

(defn- invites-coll [db] (.collection db INVITES_COLLECTION))

(defn- keywordize [doc]
  (when doc (js->clj doc :keywordize-keys true)))

(defn invite-doc->row
  "Adapt a knoxx_invites document into a PG-shaped invites row.
   :invite_id → :id, drops :_id."
  [doc]
  (when doc
    (-> doc
        (assoc :id (:invite_id doc))
        (dissoc :invite_id :_id))))

(defn ^:async setup-indexes!
  "Create invite indexes. Idempotent.
   Unique on code mirrors PG's UNIQUE (code).
   Lookup indexes on (org_id, status) and (org_id) support list queries."
  [db]
  (let [coll (invites-coll db)]
    (await (.createIndex coll #js {"code" 1} #js {"unique" true}))
    (await (.createIndex coll #js {"org_id" 1 "status" 1 "created_at" -1}))
    (await (.createIndex coll #js {"org_id" 1 "created_at" -1}))
    true))

(defn ^:async insert-invite!
  "Insert a new invite. Returns the created row.
   Mirrors PG shape.db.invites/insert."
  ([invite] (insert-invite! (mongo-client/get-db) invite))
  ([db {:keys [org-id code email inviter-membership-id role-slugs-json expires-at]}]
   (let [coll (invites-coll db)
         now (js/Date.)
         role-slugs (when role-slugs-json
                      (if (string? role-slugs-json)
                        (js/JSON.parse role-slugs-json)
                        (clj->js role-slugs-json)))]
     (await (.insertOne coll
                        (clj->js {:invite_id            (str (random-uuid))
                                  :org_id               (str org-id)
                                  :code                 code
                                  :email                (str/lower-case (str email))
                                  :inviter_membership_id inviter-membership-id
                                  :role_slugs           role-slugs
                                  :status               "pending"
                                  :expires_at           expires-at
                                  :created_at           now
                                  :system_instance_id   (system-instance/current-id)})))
     (let [doc (keywordize (await (.findOne coll #js {"code" code})))]
       (invite-doc->row doc)))))

(defn- pending-invite-query
  [identifier now]
  ;; The native invite writer uses ISO strings. Older rows can hold BSON dates.
  (clj->js (assoc identifier
                 "status" "pending"
                 "$expr" {"$and" [{"$in" [{"$type" "$expires_at"} ["date" "string"]]}
                                  {"$gt" [{"$convert" {"input" "$expires_at"
                                                        "to" "date"
                                                        "onError" nil
                                                        "onNull" nil}}
                                          now]}]})))

(defn ^:async pending-by-code!
  "Find a pending, non-expired invite by code. Returns the row or nil.
   Mirrors PG shape.db.invites/pending-by-code."
  ([code] (pending-by-code! (mongo-client/get-db) code))
  ([db code]
   (let [coll (invites-coll db)
         query (pending-invite-query {"code" code} (js/Date.))
         doc (keywordize (await (.findOne coll query)))]
     (invite-doc->row doc))))

(defn ^:async redeem-invite!
  "Claim a pending, non-expired invite. Returns the updated row or nil.
   Mirrors PG shape.db.invites/redeem."
  ([invite-id] (redeem-invite! (mongo-client/get-db) invite-id))
  ([db invite-id]
   (let [coll (invites-coll db)
         now (js/Date.)
         query (pending-invite-query {"invite_id" (str invite-id)} now)
         doc (await (.findOneAndUpdate coll query
                                      #js {"$set" #js {"status" "redeemed" "redeemed_at" now}}
                                      #js {"returnDocument" "after"}))]
     (invite-doc->row (keywordize doc)))))

(defn ^:async list-invites-by-org!
  "Return invites for an org, optionally filtered by status, ordered by created_at desc.
   Mirrors PG shape.db.invites/list-by-org and list-by-org-and-status."
  ([org-id] (list-invites-by-org! (mongo-client/get-db) org-id nil))
  ([org-id status] (list-invites-by-org! (mongo-client/get-db) org-id status))
  ([db org-id status]
   (let [coll (invites-coll db)
         query (cond-> {"org_id" (str org-id)}
                 status (assoc "status" (str status)))
         cursor (.find coll (clj->js query)
                       #js {"sort" #js {"created_at" -1}})
         docs (keywordize (await (.toArray cursor)))]
     (mapv invite-doc->row docs))))
