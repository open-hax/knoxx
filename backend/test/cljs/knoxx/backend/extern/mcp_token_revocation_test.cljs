(ns knoxx.backend.extern.mcp-token-revocation-test
  "Mongo compatibility checks through native driver handles and Fastify routes."
  (:require ["fastify" :as fastify]
            [cljs.test :as test]
            [clojure.string :as str]
            [knoxx.backend.extern.mcp-oauth :as routes]
            [knoxx.backend.extern.mcp-oauth-store :as codec]
            [knoxx.backend.infra.mongo-client :as mongo]
            [knoxx.backend.infra.stores.mcp-oauth-dispatch :as dispatch]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as store]
            [knoxx.backend.shape.app-shapes :as http]))

(defn- matching? [query row]
  (every? (fn [[field value]] (= value (get row field))) query))

(defn- token-db []
  (let [rows* (atom {}) calls* (atom []) before-delete* (atom (fn [] nil))
        collection
        #js {:find (fn [native-query]
                     (let [query (js->clj native-query :keywordize-keys true)]
                       (swap! calls* conj [:find query])
                       #js {:toArray (fn [] (js/Promise.resolve
                                             (clj->js (filterv #(matching? query %) (vals @rows*)))))}))
             :findOne (fn [native-query]
                        (let [query (js->clj native-query :keywordize-keys true)]
                          (js/Promise.resolve (clj->js (first (filter #(matching? query %) (vals @rows*)))))))
             :updateOne (fn [native-query native-update _options]
                          (let [query (js->clj native-query :keywordize-keys true)
                                update-document (js->clj native-update :keywordize-keys true)
                                token-key (:access_token query)]
                            (swap! rows* update-in [token-key] #(merge query (:$setOnInsert update-document) % (:$set update-document)))
                            (js/Promise.resolve #js {:acknowledged true})))
             :deleteOne (fn [native-query]
                          (@before-delete*)
                          (let [query (js->clj native-query :keywordize-keys true)
                                hit (first (filter #(matching? query %) (vals @rows*)))]
                            (swap! calls* conj [:delete query])
                            (when hit (swap! rows* dissoc (:access_token hit)))
                            (js/Promise.resolve #js {:deletedCount (if hit 1 0)})))}]
    {:db #js {:collection (fn [_name] collection)} :rows* rows* :calls* calls*
     :before-delete* before-delete*}))

(defn- ^:async seed! [db token member]
  (await (store/set-token! db token
                          (codec/encode {:membershipId member :accessToken token :clientId "client-a"
                                         :tools ["read"] :createdAt "2026-09-12T00:00:00Z"})
                          3600 member)))

(defn- ^:async outcome! [operation]
  (try (await (operation)) (catch :default error {:error (:code (ex-data error))})))

(defn- ^:async request! [method token-id member]
  (let [app (fastify #js {})
        guard (fn [request reply done]
                (if member
                  (do (aset request "authContext" {:membership-id member}) (done))
                  (.send (.code reply 401) #js {:error "unauthenticated"})))
        dependencies {:route! http/route! :browser-auth-guard guard}]
    (try
      (.setErrorHandler app (fn [error _request reply]
                             (.send (.code reply (or (:status (ex-data error)) 500))
                                    #js {:error (or (:code (ex-data error)) "request_failed")})))
      (routes/mcp-list-user-tokens! app {} {} dependencies)
      (routes/mcp-revoke-user-token! app {} {} dependencies)
      (let [response (await (.inject app #js {:method method :url (str "/api/mcp/tokens" (when token-id (str "/" token-id)))}))]
        {:status (.-statusCode response) :body (js->clj (.json response) :keywordize-keys true)})
      (finally (await (.close app))))))

(test/deftest ^:async mongo-inventory-token-id-can-be-revoked-without-exposing-bearer
  (let [{:keys [db calls*]} (token-db) token "legacy-secret-a" id (codec/digest token)]
    (await (seed! db token "member-a"))
    (with-redefs [dispatch/current (fn [] nil) mongo/get-db (fn [] db)]
      (let [inventory (await (request! "GET" nil "member-a"))
            entries (mapv codec/decode (get-in inventory [:body :tokens]))]
        (test/is (= 200 (:status inventory)))
        (test/is (= [id] (mapv :tokenId entries)))
        (test/is (not (str/includes? (pr-str inventory) token)) "legacy raw token data stays private")
        (test/is (= 200 (:status (await (request! "DELETE" id "member-a")))))
        (test/is (nil? (await (store/get-token! db token))))
        (test/is (= 404 (:status (await (request! "DELETE" id "member-a")))))
        (test/is (every? #(= "member-a" (:membership_id (second %))) @calls*))))))

(test/deftest ^:async mongo-revocation-refuses-foreign-unknown-and-unauthenticated-callers
  (let [{:keys [db]} (token-db) id (codec/digest "other-secret")]
    (await (seed! db "other-secret" "member-b"))
    (with-redefs [dispatch/current (fn [] nil) mongo/get-db (fn [] db)]
      (test/is (= 401 (:status (await (request! "DELETE" id nil)))))
      (test/is (= 404 (:status (await (request! "DELETE" id "member-a")))))
      (test/is (= 404 (:status (await (request! "DELETE" (codec/digest "missing") "member-a")))))
      (test/is (some? (await (store/get-token! db "other-secret")))))))

(test/deftest ^:async mongo-revocation-rechecks-membership-at-the-atomic-delete
  (let [{:keys [db rows* before-delete*]} (token-db) token "moving-secret"]
    (await (seed! db token "member-a"))
    (reset! before-delete* #(swap! rows* assoc-in [token :membership_id] "member-b"))
    (with-redefs [dispatch/current (fn [] nil) mongo/get-db (fn [] db)]
      (test/is (false? (await (outcome! #(store/delete-token-id-for-membership! (codec/digest token) "member-a")))))
      (test/is (= "member-b" (get-in @rows* [token :membership_id]))))))

(test/deftest ^:async invalid-token-identifiers-never-open-a-mongo-collection
  (let [opened* (atom 0)]
    (with-redefs [dispatch/current (fn [] nil) mongo/get-db (fn [] (swap! opened* inc))]
      (doseq [[id member] [["raw-token" "member-a"] [(codec/digest "token") " "]]]
        (test/is (= "mcp_oauth_invalid_input"
                    (:error (await (outcome! #(store/delete-token-id-for-membership! id member)))))))
      (test/is (zero? @opened*)))))
