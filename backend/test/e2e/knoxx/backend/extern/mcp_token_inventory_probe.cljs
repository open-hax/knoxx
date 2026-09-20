(ns knoxx.backend.extern.mcp-token-inventory-probe
  "Destructive only within a unique temporary database on an explicit native Mongo URI."
  (:require ["mongodb" :as mongodb]
            ["node:crypto" :as crypto]
            [clojure.string :as str]
            [knoxx.backend.extern.mcp-oauth-store :as codec]
            [knoxx.backend.infra.mongo-client :as mongo]
            [knoxx.backend.infra.stores.mcp-oauth-dispatch :as dispatch]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as store]))

(defn- assert! [condition message]
  (when-not condition (throw (ex-info message {}))))

(defn- ^:async seed! [db token member ttl]
  (await (store/mongo-set-token! db token
                                (codec/encode {:clientId "probe-client" :membershipId member
                                               :accessToken token :tools ["read"]})
                                ttl member)))

(defn- ^:async exercise! [db]
  (let [own "native-own-secret" foreign "native-foreign-secret" id (codec/digest own)]
    (await (seed! db own "member-a" 3600))
    (await (seed! db foreign "member-b" 3600))
    (await (seed! db "expired-secret" "member-a" -1))
    (with-redefs [dispatch/current (fn [] nil) mongo/get-db (fn [] db)]
      (let [inventory (mapv codec/decode (await (store/list-tokens-for-membership! "member-a")))]
        (assert! (= [id] (mapv :tokenId inventory)) "Inventory must contain only the live member token digest")
        (assert! (not (str/includes? (pr-str inventory) own)) "Inventory leaked bearer bytes")
        (assert! (false? (await (store/delete-token-id-for-membership! (codec/digest foreign) "member-a")))
                 "Foreign token revocation succeeded")
        (assert! (false? (await (store/delete-token-id-for-membership! (codec/digest "missing") "member-a")))
                 "Unknown token revocation succeeded")
        (assert! (true? (await (store/delete-token-id-for-membership! id "member-a"))) "Own token was not revoked")
        (assert! (nil? (await (store/get-token! db own))) "Revoked bearer still authorizes")
        (assert! (some? (await (store/get-token! db foreign))) "Foreign bearer was changed")
        (assert! (false? (await (store/delete-token-id-for-membership! id "member-a"))) "A repeated delete reported success")
        (assert! (empty? (await (store/list-tokens-for-membership! "member-a"))) "Inventory retained a revoked or expired grant")
        {:checks 9 :native-mongo true :membership-isolation true :safe-inventory true}))))

(defn ^:async verify!
  "Run the real driver against MONGO_URI and always remove the unique probe database."
  []
  (let [uri (aget js/process.env "MONGO_URI")]
    (assert! (and (string? uri) (not (str/blank? uri))) "MONGO_URI is required for the native probe")
    (let [client (mongodb/MongoClient. uri #js {:serverSelectionTimeoutMS 5000})
          database (str "knoxx_mcp_probe_" (str/replace (crypto/randomUUID) "-" ""))]
      (try
        (await (.connect client))
        (let [db (.db client database)]
          (try (clj->js (await (exercise! db)))
               (finally (await (.dropDatabase db)))))
        (finally (await (.close client)))))))
