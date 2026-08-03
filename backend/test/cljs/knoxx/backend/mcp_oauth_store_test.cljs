(ns knoxx.backend.mcp-oauth-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as store]))

;; ─────────────────────────────────────────────────────────
;; set-client! / get-client! round trip
;;
;; set-client! nests the registration under :client_data beside its own
;; bookkeeping fields. get-client! used to hand the whole envelope back, while
;; every caller reads redirect_uris off the top level of what it returns — so
;; the lookup was undefined, the allow-list was empty, and every registered
;; client's redirect_uri was rejected:
;;
;;   500 {"message":"redirect_uri not allowed for registered client"}
;;
;; No MCP OAuth flow could be completed by any client. Nothing caught it
;; because the two halves were only ever exercised against a live Mongo, and
;; the browser-auth guard 302s an unauthenticated caller away before the
;; authorize handler ever reads the client. These tests pin the two halves
;; together with a double, so the write shape and the read shape cannot drift.
;; ─────────────────────────────────────────────────────────

(defn- fake-db
  "Minimal Mongo double: one in-memory clients collection keyed by client_id.
   Models the $set / $setOnInsert split so an existing document keeps its
   original bookkeeping fields, exactly as the real upsert does."
  []
  (let [docs (atom {})
        coll #js {:updateOne
                  (fn [query update-doc _opts]
                    (let [id      (aget query "client_id")
                          set-doc (js->clj (aget update-doc "$set") :keywordize-keys true)
                          on-ins  (js->clj (aget update-doc "$setOnInsert") :keywordize-keys true)]
                      (swap! docs
                             (fn [m]
                               (let [base (or (get m id) (merge {:client_id id} on-ins))]
                                 (assoc m id (merge base set-doc)))))
                      (js/Promise.resolve #js {})))
                  :findOne
                  (fn [query]
                    (js/Promise.resolve
                     (some-> (get @docs (aget query "client_id")) clj->js)))}
        db   #js {:collection (fn [_name] coll)}]
    (aset db "docs" docs)
    db))

(def ^:private registration
  {:client_id                  "8db87cc0-ec93-4ab1-949b-181bca83b61f"
   :client_name                "ChatGPT"
   :redirect_uris              ["https://chatgpt.com/connector/oauth/uglCS2GPPUb0"]
   :token_endpoint_auth_method "none"
   :grant_types                ["authorization_code"]
   :response_types             ["code"]})

(deftest ^:async registered-client-round-trips-as-the-registration
  (testing "get-client! returns the client record, not the storage envelope"
    (let [db (fake-db)
          id (:client_id registration)]
      (await (store/set-client! db id (js/JSON.stringify (clj->js registration))))
      (let [client (js/JSON.parse (await (store/get-client! db id)))]
        (is (= id (aget client "client_id")))
        (is (= (:redirect_uris registration) (js->clj (aget client "redirect_uris")))
            "redirect_uris must be readable at the top level — this is what the allow-list checks")
        (is (= "ChatGPT" (aget client "client_name")))
        (is (nil? (aget client "client_data"))
            "the storage envelope must not leak through to callers")))))

(deftest ^:async legacy-flat-client-record-still-reads
  (testing "a document written before the envelope existed is returned as-is"
    (let [db (fake-db)
          id "legacy-client"]
      ;; Written flat, with no :client_data wrapper.
      (swap! (aget db "docs") assoc id
             {:client_id id :redirect_uris ["https://legacy.example/cb"]})
      (let [client (js/JSON.parse (await (store/get-client! db id)))]
        (is (= ["https://legacy.example/cb"] (js->clj (aget client "redirect_uris"))))))))

(deftest ^:async unknown-client-reads-as-nothing
  (testing "an unregistered client_id yields nil rather than an empty record"
    (let [db (fake-db)]
      (is (nil? (await (store/get-client! db "never-registered")))))))

;; ─────────────────────────────────────────────────────────
;; Token revocation is scoped to the owning membership
;;
;; delete-token! matches on access_token alone, so the revoke route let any
;; authenticated caller destroy another membership's token if they learned its
;; value — the listing route is per-membership, but nothing stopped a hand-made
;; DELETE. Raised by CodeRabbit on #214.
;; ─────────────────────────────────────────────────────────

(defn- fake-token-db
  "Mongo double for the tokens collection, keyed by access_token."
  [initial]
  (let [docs (atom initial)
        coll #js {:deleteOne
                  (fn [query]
                    (let [tok (aget query "access_token")
                          mid (aget query "membership_id")
                          doc (get @docs tok)
                          hit (and doc (or (nil? mid) (= mid (:membership_id doc))))]
                      (when hit (swap! docs dissoc tok))
                      (js/Promise.resolve #js {:deletedCount (if hit 1 0)})))}
        db   #js {:collection (fn [_name] coll)}]
    (aset db "docs" docs)
    db))

(def ^:private two-tokens
  {"tok-mine"     {:access_token "tok-mine"     :membership_id "m-mine"}
   "tok-somebody" {:access_token "tok-somebody" :membership_id "m-other"}})

(deftest ^:async revoking-your-own-token-succeeds
  (testing "a token belonging to the membership is deleted"
    (let [db (fake-token-db two-tokens)]
      (is (true? (await (store/delete-token-for-membership! db "tok-mine" "m-mine"))))
      (is (nil? (get @(aget db "docs") "tok-mine")) "the token is gone"))))

(deftest ^:async revoking-someone-elses-token-does-nothing
  (testing "a token belonging to another membership is neither deleted nor reported deleted"
    (let [db (fake-token-db two-tokens)]
      (is (false? (await (store/delete-token-for-membership! db "tok-somebody" "m-mine")))
          "returns false so the route can answer 404 instead of a false success")
      (is (some? (get @(aget db "docs") "tok-somebody"))
          "the other membership's token survives"))))

(deftest ^:async revoking-an-unknown-token-reports-no-deletion
  (testing "an unknown token id is not reported as revoked"
    (let [db (fake-token-db two-tokens)]
      (is (false? (await (store/delete-token-for-membership! db "tok-nonexistent" "m-mine")))))))
