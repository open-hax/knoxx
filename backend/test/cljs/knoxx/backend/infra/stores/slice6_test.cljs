(ns knoxx.backend.infra.stores.slice6-test
  "Tests for slice 6 Mongo twins: audit_events, data_lakes, invites, studio."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-policy-audit-events :as audit]
            [knoxx.backend.infra.stores.mongo-policy-data-lakes :as dl]
            [knoxx.backend.infra.stores.mongo-policy-invites :as inv]
            [knoxx.backend.infra.stores.mongo-policy-studio :as studio]))

;; ---------------------------------------------------------------------------
;; Mock DB (same pattern as earlier slices)
;; ---------------------------------------------------------------------------

(defn- matches-clause? [actual v]
  (cond
    (and (map? v) (contains? v :$gt)) (> (compare (str actual) (str (:$gt v))) -1)
    (map? v) (= actual v)
    :else (= actual v)))

(defn- matches-query? [doc query]
  (cond
    (contains? query :$or) (some #(matches-query? doc %) (:$or query))
    :else (every? (fn [[k v]]
                    (let [kw (keyword k)
                          val (or (get doc kw) (get doc k))]
                      (matches-clause? val v)))
                  query)))

(defn- mock-collection [docs]
  #js {:insertOne (fn [doc]
                    (swap! docs conj (js->clj doc :keywordize-keys true))
                    (js/Promise.resolve #js {}))
       :findOne (fn [query]
                  (let [q (js->clj query :keywordize-keys true)]
                    (js/Promise.resolve
                     (clj->js (first (filter #(matches-query? % q) @docs))))))
       :find (fn [query opts]
               (let [q (js->clj query :keywordize-keys true)
                     sort-key (when opts (js->clj opts :keywordize-keys true))
                     sort-field (when sort-key (first (keys (:sort sort-key))))
                     sort-dir (when sort-field (get-in sort-key [:sort (keyword sort-field)]))
                     hits (if sort-dir
                            (let [sorted (sort-by (keyword sort-field) (filter #(matches-query? % q) @docs))]
                              (if (= -1 sort-dir) (reverse sorted) sorted))
                            (filter #(matches-query? % q) @docs))]
                 #js {:toArray (fn [] (js/Promise.resolve (clj->js (vec hits))))}))
       :updateOne (fn [query update opts]
                    (let [q (js->clj query :keywordize-keys true)
                          set-doc (js->clj (.-$set update) :keywordize-keys true)
                          set-on-insert (js->clj (.-$setOnInsert update) :keywordize-keys true)
                          upsert? (and opts (.-upsert opts))]
                      (if (some #(matches-query? % q) @docs)
                        (swap! docs (fn [ds] (mapv #(if (matches-query? % q) (merge % set-doc) %) ds)))
                        (when upsert?
                          (swap! docs conj (merge q set-doc set-on-insert))))
                      (js/Promise.resolve #js {})))
       :createIndex (fn [& _] (js/Promise.resolve "ok"))})

(defn- mock-db []
  (let [collections (atom {})]
    #js {:collection (fn [name]
                       (let [docs (or (get @collections name)
                                      (let [d (atom [])]
                                        (swap! collections assoc name d)
                                        d))]
                         (mock-collection docs)))}))

;; ---------------------------------------------------------------------------
;; Audit events
;; ---------------------------------------------------------------------------

(deftest ^:async audit-insert-event-test
  (testing "insert-event! stores an audit document without throwing"
    (let [db (mock-db)]
      (await (audit/insert-event!
              db {:actor-user-id "u1"
                  :actor-membership-id "m1"
                  :org-id "o1"
                  :action "invite.create"
                  :resource-kind "invite"
                  :resource-id "inv1"
                  :before-json nil
                  :after-json "{\"email\":\"test@example.com\"}"}))
      (is true "insert-event! completed without error"))))

;; ---------------------------------------------------------------------------
;; Data lakes
;; ---------------------------------------------------------------------------

(deftest ^:async data-lake-create-and-list-test
  (testing "create + list data lakes for an org"
    (let [db (mock-db)]
      (let [row (await (dl/create-data-lake!
                        db "o1" {:name "My Lake" :slug "my-lake"
                                 :kind "workspace_docs" :config-json nil :status "active"}))]
        (is (some? (:id row)) "lake has an id")
        (is (= "My Lake" (:name row)) "name present")
        (is (= "my-lake" (:slug row)) "slug present"))
      (let [rows (await (dl/list-data-lakes-by-org! db "o1"))]
        (is (= 1 (count rows)) "one lake listed")
        (is (= "My Lake" (:name (first rows))))))))

(deftest ^:async data-lake-doc->row-test
  (testing "doc->row renames lake_id to :id and drops :_id"
    (let [r (dl/data-lake-doc->row {:lake_id "l1" :_id "x" :name "test"})]
      (is (= "l1" (:id r)))
      (is (nil? (:lake_id r)))
      (is (nil? (:_id r))))
    (is (nil? (dl/data-lake-doc->row nil)) "nil-safe")))

;; ---------------------------------------------------------------------------
;; Invites
;; ---------------------------------------------------------------------------

(deftest ^:async invite-create-redeem-list-test
  (testing "invite lifecycle: create → list"
    (let [db (mock-db)]
      ;; Create
      (let [row (await (inv/insert-invite!
                        db {:org-id "o1"
                            :code "abc123"
                            :email "User@Example.com"
                            :inviter-membership-id "m1"
                            :role-slugs-json "[\"basic-user\"]"
                            :expires-at (str (js/Date. (+ (js/Date.now) (* 7 24 3600 1000))))}))]
        (is (some? (:id row)) "invite has an id")
        (is (= "abc123" (:code row)) "code present")
        (is (= "user@example.com" (:email row)) "email lowercased")
        (is (= "pending" (:status row)) "status is pending"))

      ;; List
      (let [rows (await (inv/list-invites-by-org! db "o1" nil))]
        (is (= 1 (count rows)) "one invite listed")))))

(deftest ^:async invite-doc->row-test
  (testing "doc->row renames invite_id to :id"
    (let [r (inv/invite-doc->row {:invite_id "i1" :_id "x" :code "abc"})]
      (is (= "i1" (:id r)))
      (is (nil? (:invite_id r)))
      (is (nil? (:_id r))))
    (is (nil? (inv/invite-doc->row nil)) "nil-safe")))

;; ---------------------------------------------------------------------------
;; Studio state
;; ---------------------------------------------------------------------------

(deftest ^:async studio-state-get-put-test
  (testing "put then get studio state"
    (let [db (mock-db)]
      (await (studio/put-studio-state! db "u1" "o1" "player" {:volume 80 :track "song.mp3"}))
      (let [state (await (studio/get-studio-state! db "u1" "o1" "player"))]
        (is (= 80 (:volume state)) "volume preserved")
        (is (= "song.mp3" (:track state)) "track preserved")))))

(deftest ^:async studio-playlist-test
  (testing "put then get playlist"
    (let [db (mock-db)]
      (await (studio/put-studio-playlist! db "u1" "o1" [{:id "s1" :name "Song 1"}]))
      (let [items (await (studio/get-studio-playlist! db "u1" "o1"))]
        (is (= 1 (count items)) "one item")
        (is (= "Song 1" (:name (first items))))))))

(deftest ^:async studio-audio-asset-test
  (testing "save then get audio asset"
    (let [db (mock-db)
          buf (js/Buffer.from "fake-image-data" "utf8")]
      (await (studio/save-audio-asset! db "/audio/song.mp3" "waveform" buf "image/png" 800 200))
      (let [asset (await (studio/get-audio-asset! db "/audio/song.mp3" "waveform"))]
        (is (some? asset) "asset found")
        (is (= "image/png" (:mime-type asset)) "mime type correct")
        (is (= 800 (:width asset)) "width correct")
        (is (= 200 (:height asset)) "height correct")))))
