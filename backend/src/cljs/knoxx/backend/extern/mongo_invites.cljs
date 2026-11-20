(ns knoxx.backend.extern.mongo-invites
  "Own the MongoDB boundary for pending invite lookup and atomic redemption."
  (:require [knoxx.backend.extern.mongo :as mongo]))

(def collection-name "knoxx_invites")

(defn- pending-query
  [identifier]
  ;; Current invites hold ISO strings and earlier rows can hold BSON dates.
  (clj->js (assoc identifier
                 "status" "pending"
                 "$expr" {"$and" [{"$in" [{"$type" "$expires_at"} ["date" "string"]]}
                                  {"$gt" [{"$convert" {"input" "$expires_at"
                                                        "to" "date"
                                                        "onError" nil
                                                        "onNull" nil}}
                                          "$$NOW"]}]})))

(defn ^:async pending-by-code!
  "Return the pending invite document if its expiry is later than MongoDB's clock."
  [db code]
  (let [coll (mongo/collection db collection-name)
        doc (await (.findOne coll (pending-query {"code" code})))]
    (when doc (js->clj doc :keywordize-keys true))))

(defn ^:async claim!
  "Atomically claim an eligible invite, returning the updated document or nil."
  [db invite-id]
  (let [coll (mongo/collection db collection-name)
        query (pending-query {"invite_id" (str invite-id)})
        update #js [#js {"$set" #js {"status" "redeemed" "redeemed_at" "$$NOW"}}]
        doc (await (.findOneAndUpdate coll query update #js {"returnDocument" "after"}))]
    (when doc (js->clj doc :keywordize-keys true))))
