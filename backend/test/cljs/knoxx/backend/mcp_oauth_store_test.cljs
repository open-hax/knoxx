(ns knoxx.backend.mcp-oauth-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.mongo :as extern-mongo]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as store]
            [knoxx.backend.law.mcp-oauth :as law]
            [knoxx.backend.law.mongo :as law-mongo]))

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

(deftest ^:async blank-identity-never-issues-a-delete
  (testing "a blank membership or token is refused instead of widening the query"
    ;; Without the contract these fall through to a query with an empty-string
    ;; field, which matches nothing today but is one schema change away from
    ;; matching everything. Refuse before the delete is issued at all.
    (let [db      (fake-token-db two-tokens)
          issued? (atom false)]
      (aset db "collection"
            (fn [_] #js {:deleteOne (fn [_] (reset! issued? true)
                                      (js/Promise.resolve #js {:deletedCount 1}))}))
      (is (false? (await (store/delete-token-for-membership! db "tok-mine" ""))))
      (is (false? (await (store/delete-token-for-membership! db "" "m-mine"))))
      (is (false? (await (store/delete-token-for-membership! db "tok-mine" nil))))
      (is (false? @issued?) "no delete reached the collection"))))

(deftest revocation-contracts-state-the-boundary-obligations
  (testing "the request contract rejects anything that would widen the delete"
    (is (law/valid-revocation-request? {:access-token "t" :membership-id "m"}))
    (is (not (law/valid-revocation-request? {:access-token "t" :membership-id ""})))
    (is (not (law/valid-revocation-request? {:access-token "t" :membership-id "   "}))
        "whitespace is not an identity")
    (is (not (law/valid-revocation-request? {:access-token "t"}))
        "a missing membership is not an absent filter"))
  (testing "the result contract requires a count"
    (is (law/valid-revocation-result? {:deleted-count 0}))
    (is (not (law/valid-revocation-result? {})))
    (is (not (law/valid-revocation-result? {:deleted-count -1})))))

(deftest ^:async an-undecodable-delete-result-throws
  (testing "a driver result with no count raises instead of reading as 'nothing deleted'"
    ;; End to end through the real adapter: a handle that acknowledges without
    ;; reporting a count must reach the contract and fail it. If delete-one!
    ;; substituted zero here, RevocationResult would accept the fabrication and
    ;; the route would answer a confident 404 for a persistence layer it could
    ;; not actually read.
    (let [db #js {:collection
                  (fn [_] #js {:deleteOne (fn [_] (js/Promise.resolve #js {:acknowledged true}))})}
          outcome (try (await (store/delete-token-for-membership! db "t" "m")) :ok
                       (catch :default e e))]
      (is (not= :ok outcome) "an undecodable result must raise, not return false"))))

;; ─────────────────────────────────────────────────────────
;; Codes and tokens survive their own round trip
;;
;; The writers store :expiresAt; the readers asked for :expires-at, a key no
;; document has ever carried, so the (… 0) default made everything read as
;; already expired. The token exchange answered "Unknown or expired code" for
;; a code it had minted seconds earlier, and no access token could ever be
;; presented successfully — ChatGPT reached the consent page and then failed.
;; Same shape as the get-client! envelope bug: a writer and a reader that were
;; only ever exercised together against a live Mongo.
;; ─────────────────────────────────────────────────────────

(defn- fake-ttl-db
  "Mongo double for a TTL collection, keyed by the given id field."
  [id-field]
  (let [docs (atom {})
        coll #js {:updateOne
                  (fn [query update-doc _opts]
                    (let [id      (aget query id-field)
                          set-doc (js->clj (aget update-doc "$set") :keywordize-keys true)
                          on-ins  (js->clj (aget update-doc "$setOnInsert") :keywordize-keys true)]
                      (swap! docs (fn [m]
                                    (let [base (or (get m id) (merge {(keyword id-field) id} on-ins))]
                                      (assoc m id (merge base set-doc)))))
                      (js/Promise.resolve #js {})))
                  :findOne
                  (fn [query]
                    (js/Promise.resolve
                     (some-> (get @docs (aget query id-field)) clj->js)))
                  :findOneAndDelete
                  (fn [query]
                    (let [id  (aget query id-field)
                          won (atom nil)]
                      (swap! docs (fn [m]
                                    (when-let [d (get m id)] (reset! won d))
                                    (dissoc m id)))
                      (js/Promise.resolve (some-> @won clj->js))))}
        db   #js {:collection (fn [_name] coll)}]
    (aset db "docs" docs)
    db))

(deftest ^:async a-freshly-written-code-reads-back
  (testing "a code minted seconds ago is not reported as expired"
    (let [db (fake-ttl-db "code")]
      (await (store/set-code! db "code-1" (js/JSON.stringify #js {:clientId "c" :tools #js ["t"]}) 300))
      (let [record (await (store/consume-code! db "code-1"))]
        (is (some? record) "the code the exchange just minted must be readable")
        (is (= "c" (:clientId record)) "and arrives as CLJS data, not a JSON string")))))

(deftest ^:async an-expired-code-does-not-read-back
  (testing "a code past its TTL is still refused"
    (let [db (fake-ttl-db "code")]
      (await (store/set-code! db "code-2" (js/JSON.stringify #js {:clientId "c"}) -1))
      (is (nil? (await (store/consume-code! db "code-2")))))))

(deftest ^:async a-freshly-written-token-reads-back
  (testing "an access token can actually be presented after it is issued"
    (let [db (fake-ttl-db "access_token")]
      (await (store/set-token! db "tok-1" (js/JSON.stringify #js {:membershipId "m" :tools #js ["t"]}) 3600 "m"))
      (let [raw (await (store/get-token! db "tok-1"))]
        (is (some? raw) "a live token must verify, or every authenticated /mcp call 401s")
        (is (= "m" (aget (js/JSON.parse raw) "membershipId")))))))

(deftest ^:async an-expired-token-does-not-read-back
  (testing "a token past its TTL is refused"
    (let [db (fake-ttl-db "access_token")]
      (await (store/set-token! db "tok-2" (js/JSON.stringify #js {:membershipId "m"}) -1 "m"))
      (is (nil? (await (store/get-token! db "tok-2")))))))

(deftest ^:async an-unreadable-expiry-fails-closed
  (testing "a document whose expiry cannot be read is treated as expired"
    (let [db (fake-ttl-db "code")]
      (swap! (aget db "docs") assoc "code-3" {:code "code-3" :code_data {:clientId "c"}})
      (is (nil? (await (store/consume-code! db "code-3")))
          "a missing expiry must not read as a live code"))))

(deftest instant-ms-decodes-what-mongo-actually-returns
  (testing "the adapter owns the driver's date shape"
    (let [d (js/Date. 1700000000000)]
      (is (= 1700000000000 (extern-mongo/instant-ms d)) "a BSON date arrives as js/Date")
      (is (= 1700000000000 (extern-mongo/instant-ms 1700000000000)) "a number passes through")
      (is (= 1700000000000 (extern-mongo/instant-ms "2023-11-14T22:13:20.000Z"))
          "an ISO string still reads, for migrated documents"))
    (is (nil? (extern-mongo/instant-ms nil)))
    (is (nil? (extern-mongo/instant-ms "not a date")))
    (is (nil? (extern-mongo/instant-ms (js/Date. "nonsense")))
        "an invalid date is unreadable, not epoch zero")
    (is (nil? (extern-mongo/instant-ms #js {}))))

  (testing "a number that cannot name a moment is not an instant"
    ;; Infinity is the dangerous one: it is a number and it compares greater
    ;; than every clock reading, so unguarded it would leave a code or token
    ;; live for good.
    (is (nil? (extern-mongo/instant-ms js/Infinity)) "Infinity must not read as live")
    (is (nil? (extern-mongo/instant-ms (- js/Infinity))))
    (is (nil? (extern-mongo/instant-ms js/NaN)))
    (is (nil? (extern-mongo/instant-ms (+ 8.64e15 1))) "past the representable range")
    (is (nil? (extern-mongo/instant-ms (- (- 8.64e15) 1))))
    (is (= 8.64e15 (extern-mongo/instant-ms 8.64e15))
        "the boundary itself is a representable instant")))

;; ── an authorization code is single use ──────────────────
;;
;; Reading a code and deleting it afterwards leaves a window: two exchanges can
;; both pass the read before either deletes, and both mint a token from one
;; code. consume-code! claims it in a single operation so exactly one wins.
;; Raised by Codex on #215.

(defn- claim-one!
  "Atomically remove and return one document — one winner however calls interleave."
  [docs id]
  (let [won (atom nil)]
    (swap! docs (fn [m]
                  (when-let [d (get m id)] (reset! won d))
                  (dissoc m id)))
    (js/Promise.resolve (some-> @won clj->js))))

(defn- fake-codes-db
  "Codes collection double.

   findOne and deleteOne are provided alongside findOneAndDelete and are
   deliberately not atomic together, so swapping the store back to a
   read-then-delete sequence reproduces the race rather than erroring."
  [initial]
  (let [docs  (atom initial)
        calls (atom 0)
        coll  #js {:findOneAndDelete (fn [q] (swap! calls inc) (claim-one! docs (aget q "code")))
                   :findOne          (fn [q] (swap! calls inc)
                                       (js/Promise.resolve
                                        (some-> (get @docs (aget q "code")) clj->js)))
                   :deleteOne        (fn [q] (swap! docs dissoc (aget q "code"))
                                       (js/Promise.resolve #js {:deletedCount 1}))}
        db    #js {:collection (fn [_name] coll)}]
    (doto db (aset "docs" docs) (aset "calls" calls))))

(def ^:private live-code
  {"code-1" {:code "code-1"
             :code_data {:clientId "c" :codeChallenge "ch"}
             :expiresAt (js/Date. (+ (.now js/Date) 300000))}})

(deftest ^:async a-code-can-be-consumed-once
  (testing "the first claim gets the data"
    (let [db     (fake-codes-db live-code)
          record (await (store/consume-code! db "code-1"))]
      (is (some? record))
      (is (= "c" (:clientId record))))))

(deftest ^:async a-consumed-code-cannot-be-claimed-again
  (testing "a second exchange with the same code gets nothing"
    (let [db (fake-codes-db live-code)]
      (is (some? (await (store/consume-code! db "code-1"))))
      (is (nil? (await (store/consume-code! db "code-1")))
          "the code is spent — a replay must not mint a second token"))))

(deftest ^:async concurrent-exchanges-yield-exactly-one-token
  (testing "two exchanges racing on one code produce a single winner"
    (let [db (fake-codes-db live-code)
          results (await (js/Promise.all
                          #js [(store/consume-code! db "code-1")
                               (store/consume-code! db "code-1")
                               (store/consume-code! db "code-1")]))
          winners (filter some? (array-seq results))]
      (is (= 3 @(aget db "calls")) "all three attempts reached the store")
      (is (= 1 (count winners)) "exactly one exchange may claim the code"))))

(deftest ^:async peeking-does-not-spend-the-code
  (testing "a read leaves the code claimable, so a rejected exchange cannot burn it"
    ;; An attacker who observes the code on the front channel but has no
    ;; verifier must not be able to destroy it. The exchange checks the
    ;; bindings against a peek and only claims once they hold.
    (let [db (fake-codes-db live-code)]
      (is (some? (await (store/peek-code! db "code-1"))))
      (is (some? (await (store/peek-code! db "code-1"))) "peeking twice is fine")
      (is (seq @(aget db "docs")) "the code is still there")
      (is (some? (await (store/consume-code! db "code-1")))
          "and the legitimate exchange can still claim it"))))

(deftest ^:async peeking-refuses-an-expired-code
  (testing "a peek applies the same liveness rule as a claim"
    (let [db (fake-codes-db {"stale" {:code "stale"
                                      :code_data {:clientId "c"}
                                      :expiresAt (js/Date. (- (.now js/Date) 1000))}})]
      (is (nil? (await (store/peek-code! db "stale")))))))

(deftest ^:async an-expired-code-is-still-consumed
  (testing "an expired code is spent rather than left readable for a retry"
    (let [db (fake-codes-db {"code-old" {:code "code-old"
                                         :code_data {:clientId "c"}
                                         :expiresAt (js/Date. (- (.now js/Date) 1000))}})]
      (is (nil? (await (store/consume-code! db "code-old"))) "expired yields nothing")
      (is (empty? @(aget db "docs")) "and the document is gone, not left behind"))))

(deftest credential-liveness-is-one-pure-rule
  (testing "an expiry after the given instant is live, before it is not"
    (is (law/credential-live? 2000 1000))
    (is (not (law/credential-live? 1000 2000)))
    (is (not (law/credential-live? 1000 1000)) "expiring exactly now is not live"))
  (testing "an unreadable instant on either side is not live"
    ;; Fails closed: a credential whose expiry cannot be named must not be
    ;; honoured, and Infinity must not outlive every clock reading.
    (is (not (law/credential-live? nil 1000)))
    (is (not (law/credential-live? js/Infinity 1000)))
    (is (not (law/credential-live? js/NaN 1000)))
    (is (not (law/credential-live? 2000 nil)))
    (is (not (law/credential-live? 2000 js/NaN)))))

(def ^:private issued
  {:clientId "c" :redirectUri "https://app/cb" :codeChallenge "ch"})

(deftest exchange-admission-is-one-pure-rule
  (testing "a code is bound to the client and redirect it was issued for"
    (is (law/code-bound-to? issued "c" "https://app/cb"))
    (is (not (law/code-bound-to? issued "other" "https://app/cb")))
    (is (not (law/code-bound-to? issued "c" "https://evil/cb")))
    (is (not (law/code-bound-to? nil "c" "https://app/cb"))))
  (testing "PKCE admits only a matching computed challenge"
    (is (law/pkce-verified? issued "ch"))
    (is (not (law/pkce-verified? issued "wrong")))
    (is (not (law/pkce-verified? issued nil)))
    (is (not (law/pkce-verified? issued "")))
    (is (not (law/pkce-verified? {:clientId "c"} "ch"))
        "a code carrying no challenge is never verified — omitting the verifier must not admit")
    (is (not (law/pkce-verified? {:codeChallenge ""} ""))
        "blank does not match blank")))

(deftest the-query-contract-refuses-a-collection-wide-delete
  (testing "an empty or non-scalar query is not a field-equality query"
    (is (law-mongo/valid-query? {:code "c"}))
    (is (law-mongo/valid-query? {:access_token "t" :membership_id "m"}))
    (is (not (law-mongo/valid-query? {}))
        "an empty query matches every document — on a delete that is the collection")
    (is (not (law-mongo/valid-query? {:code {:$ne nil}}))
        "an operator map is a different contract")
    (is (not (law-mongo/valid-query? nil)))))

(deftest ^:async an-empty-query-never-reaches-a-delete
  (testing "both delete paths refuse before issuing anything"
    (let [issued? (atom false)
          handle  #js {:findOneAndDelete (fn [_] (reset! issued? true) (js/Promise.resolve nil))
                       :deleteOne        (fn [_] (reset! issued? true)
                                           (js/Promise.resolve #js {:deletedCount 99}))}]
      (doseq [op [extern-mongo/find-one-and-delete! extern-mongo/delete-one!]]
        (let [outcome (try (await (op handle {})) :ok (catch :default e e))]
          (is (not= :ok outcome) "an empty query must raise")))
      (is (false? @issued?) "and nothing reached the collection"))))

(deftest the-epoch-millis-contract-states-what-an-instant-is
  (testing "law.mongo/EpochMillis is the named admissible shape"
    (is (law-mongo/valid-epoch-ms? 0))
    (is (law-mongo/valid-epoch-ms? 1700000000000))
    (is (law-mongo/valid-epoch-ms? law-mongo/max-time-value) "the boundary is admissible")
    (is (not (law-mongo/valid-epoch-ms? js/Infinity)))
    (is (not (law-mongo/valid-epoch-ms? (- js/Infinity))))
    (is (not (law-mongo/valid-epoch-ms? js/NaN)))
    (is (not (law-mongo/valid-epoch-ms? (+ law-mongo/max-time-value 1))))
    (is (not (law-mongo/valid-epoch-ms? "1700000000000")) "a string is not a decoded instant")
    (is (not (law-mongo/valid-epoch-ms? nil)))))

(deftest ^:async an-infinite-expiry-does-not-keep-a-token-alive
  (testing "a token stamped with Infinity is refused rather than living for good"
    (let [db (fake-ttl-db "access_token")]
      (swap! (aget db "docs") assoc "tok-inf"
             {:access_token "tok-inf" :token_data {:membershipId "m"} :expiresAt js/Infinity})
      (is (nil? (await (store/get-token! db "tok-inf")))))))

;; ── extern.mongo conversion ──────────────────────────────
;; AGENTS.md asks for a regression test on the conversion whenever an extern
;; adapter grows a new boundary. delete-one! owns decoding the driver's native
;; DeleteResult; the point is that the SDK shape stops here.

(deftest ^:async delete-one-decodes-the-native-delete-result
  (testing "the driver's DeleteResult is decoded to CLJS and never escapes"
    (let [captured (atom nil)
          handle   #js {:deleteOne (fn [q] (reset! captured q)
                                     (js/Promise.resolve #js {:deletedCount 1}))}
          result   (await (extern-mongo/delete-one! handle {:access_token "t" :membership_id "m"}))]
      (is (= {:deleted-count 1} result) "returns CLJS data, not the native result")
      (is (= "t" (aget @captured "access_token")) "the CLJS query is encoded for the driver")
      (is (= "m" (aget @captured "membership_id"))))))

(deftest ^:async delete-one-does-not-fabricate-a-missing-count
  (testing "a driver result without deletedCount decodes to nil, never to zero"
    ;; Zero would assert the driver said nothing was deleted, when it said
    ;; nothing at all — and a caller requiring a count would accept it.
    (let [handle #js {:deleteOne (fn [_] (js/Promise.resolve #js {}))}]
      (is (= {:deleted-count nil} (await (extern-mongo/delete-one! handle {:x "y"}))))))

  (testing "a non-numeric count is not coerced"
    (let [handle #js {:deleteOne (fn [_] (js/Promise.resolve #js {:deletedCount "1"}))}]
      (is (= {:deleted-count nil} (await (extern-mongo/delete-one! handle {:x "y"}))))))

  (testing "a genuine zero is preserved and still decodes as a count"
    (let [handle #js {:deleteOne (fn [_] (js/Promise.resolve #js {:deletedCount 0}))}]
      (is (= {:deleted-count 0} (await (extern-mongo/delete-one! handle {:x "y"})))
          "'nothing matched' must stay distinguishable from 'no count reported'"))))
