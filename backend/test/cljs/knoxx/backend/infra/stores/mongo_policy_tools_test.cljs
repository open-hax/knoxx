(ns knoxx.backend.infra.stores.mongo-policy-tools-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-policy-tools :as tools]))

;; Mock built on the roles-test matches-query? pattern, extended with an
;; upsert-capable updateOne (ensure-tool-definitions! upserts by tool_id).

(declare matches-query?)

(defn- matches-clause? [actual v]
  (cond
    (and (map? v) (contains? v :$in)) (contains? (set (:$in v)) actual)
    (and (map? v) (contains? v :$exists)) (= (:$exists v) (some? actual))
    (and (map? v) (contains? v :$ne)) (not= actual (:$ne v))
    (map? v) (= actual v)
    :else (= actual v)))

(defn- matches-query? [doc query]
  (cond
    (contains? query :$or) (some #(matches-query? doc %) (:$or query))
    :else (every? (fn [[k v]] (matches-clause? (get doc k) v)) query)))

(defn- mock-collection [docs]
  #js {:insertOne (fn [doc]
                    (swap! docs conj (js->clj doc :keywordize-keys true))
                    (js/Promise.resolve #js {}))
       :insertMany (fn [arr]
                     (swap! docs into (js->clj arr :keywordize-keys true))
                     (js/Promise.resolve #js {}))
       :findOne (fn [query]
                  (let [q (js->clj query :keywordize-keys true)]
                    (js/Promise.resolve
                     (clj->js (first (filter #(matches-query? % q) @docs))))))
       :find (fn [query]
               (let [q (js->clj query :keywordize-keys true)
                     hits (filter #(matches-query? % q) @docs)]
                 #js {:toArray (fn [] (js/Promise.resolve (clj->js hits)))}))
       :updateOne (fn [query update opts]
                    (let [q (js->clj query :keywordize-keys true)
                          set-doc (js->clj (.-$set update) :keywordize-keys true)
                          upsert? (and opts (.-upsert opts))]
                      (if (some #(matches-query? % q) @docs)
                        (swap! docs (fn [ds] (mapv #(if (matches-query? % q) (merge % set-doc) %) ds)))
                        (when upsert?
                          (swap! docs conj (merge q set-doc))))
                      (js/Promise.resolve #js {})))
       :deleteMany (fn [query]
                     (let [q (js->clj query :keywordize-keys true)]
                       (swap! docs (fn [ds] (vec (remove #(matches-query? % q) ds))))
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

;; A real registry tool id keeps ensure-tool-definitions! from dropping it
;; (normalize-tool-id passes unknown ids through, but get-tool returns
;; descriptive metadata for a known one — exercising the registry resolution).
(def ^:private known-tool "memory.temp")

(deftest ^:async tool-definition-ensure-idempotency-test
  (testing "ensure-tool-definitions! upserts by id; re-run keeps one row, refreshes attrs"
    (let [db (mock-db)]
      (await (tools/ensure-tool-definitions! db [known-tool known-tool]))
      (let [rows (await (tools/list-tools! db))]
        (is (= 1 (count rows)) "deduped + single upserted row")
        (is (= known-tool (:id (first rows))) "tool-def-doc->row presents :id")
        (is (nil? (:tool_id (first rows))) "row-shape adapter hides tool_id")
        (is (nil? (:_id (first rows))) "row-shape adapter hides _id")
        (is (string? (:label (first rows))) "registry label resolved")
        (is (string? (:risk_level (first rows))) "registry risk_level resolved"))
      (await (tools/ensure-tool-definitions! db [known-tool]))
      (is (= 1 (count (await (tools/list-tools! db)))) "re-run does not duplicate"))))

(deftest ^:async tool-definition-empty-noop-test
  (testing "ensure-tool-definitions! is a no-op on empty input"
    (let [db (mock-db)]
      (await (tools/ensure-tool-definitions! db []))
      (is (= [] (await (tools/list-tools! db))) "no definitions written"))))

(deftest ^:async list-tools-ordering-test
  (testing "list-tools! orders by id asc (PG ORDER BY id ASC)"
    (let [db (mock-db)]
      (await (tools/ensure-tool-definitions! db ["mcp.shoedelussy.render_wav"
                                                 "memory.temp"
                                                 "mcp.shoedelussy.render_loop"]))
      (let [ids (mapv :id (await (tools/list-tools! db)))]
        (is (= (sort ids) ids) "ascending by id")))))

(deftest ^:async role-tool-policy-replace-set-test
  (testing "set-role-tool-policies! replaces (not appends); empty set clears"
    (let [db (mock-db)
          rid "r1"
          pol (fn [tid eff cj] {:tool-id tid :effect eff :constraints-json cj})]
      (await (tools/set-role-tool-policies! db rid [(pol "b.tool" "allow" "{}")
                                                    (pol "a.tool" "deny" "{}")]))
      (let [rows (await (tools/tool-policies-for-roles! db [rid]))]
        (is (= ["a.tool" "b.tool"] (mapv :tool_id rows)) "ordered by tool_id")
        (is (= rid (:role_id (first rows))) "row carries :role_id for the reducer")
        (is (= #{:role_id :tool_id :effect :constraints_json} (set (keys (first rows))))
            "projection keys match grouped-role-tool-policies destructure"))
      (await (tools/set-role-tool-policies! db rid [(pol "c.tool" "allow" "{}")]))
      (is (= ["c.tool"] (mapv :tool_id (await (tools/tool-policies-for-roles! db [rid]))))
          "replaced, not appended")
      (await (tools/set-role-tool-policies! db rid []))
      (is (= [] (await (tools/tool-policies-for-roles! db [rid]))) "empty set clears"))))

(deftest ^:async role-tool-policy-constraints-string-roundtrip-test
  (testing "constraints_json is stored + returned as the identical JSON string"
    (let [db (mock-db)
          rid "r1"
          ;; This is exactly what policy-with-constraints-json produces upstream.
          cj "{\"maxCalls\":3,\"allow\":[\"x\",\"y\"]}"]
      (await (tools/set-role-tool-policies! db rid [{:tool-id "t.x" :effect "allow"
                                                     :constraints-json cj}]))
      (let [row (first (await (tools/tool-policies-for-roles! db [rid])))]
        (is (= cj (:constraints_json row)) "string round-trips byte-for-byte")
        (is (= "allow" (:effect row)) "effect preserved")))))

(deftest ^:async role-tool-policies-multi-test
  (testing "tool-policies-for-roles! returns rows for multiple roles, tool-ordered"
    (let [db (mock-db)]
      (await (tools/set-role-tool-policies! db "ra" [{:tool-id "z.t" :effect "allow" :constraints-json "{}"}
                                                     {:tool-id "a.t" :effect "deny" :constraints-json "{}"}]))
      (await (tools/set-role-tool-policies! db "rb" [{:tool-id "m.t" :effect "allow" :constraints-json "{}"}]))
      (let [rows (await (tools/tool-policies-for-roles! db ["ra" "rb"]))]
        (is (= 3 (count rows)))
        (is (= ["a.t" "m.t" "z.t"] (mapv :tool_id rows)) "global order by tool_id")
        (is (= #{"ra" "rb"} (set (map :role_id rows)))))
      (is (= [] (await (tools/tool-policies-for-roles! db []))) "empty in, empty out"))))

(deftest ^:async membership-tool-policy-replace-set-test
  (testing "set-membership-tool-policies! replaces (not appends); empty clears"
    (let [db (mock-db)
          mid "m1"
          pol (fn [tid eff cj] {:tool-id tid :effect eff :constraints-json cj})]
      (await (tools/set-membership-tool-policies! db mid [(pol "b.tool" "allow" "{}")
                                                          (pol "a.tool" "deny" "{}")]))
      (let [rows (await (tools/tool-policies-for-memberships! db [mid]))]
        (is (= ["a.tool" "b.tool"] (mapv :tool_id rows)) "ordered by tool_id")
        (is (= mid (:membership_id (first rows))) "row carries :membership_id")
        (is (= #{:membership_id :tool_id :effect :constraints_json} (set (keys (first rows))))
            "projection keys match grouped-membership-tool-policies destructure"))
      (await (tools/set-membership-tool-policies! db mid [(pol "c.tool" "allow" "{}")]))
      (is (= ["c.tool"] (mapv :tool_id (await (tools/tool-policies-for-memberships! db [mid]))))
          "replaced, not appended")
      (await (tools/set-membership-tool-policies! db mid []))
      (is (= [] (await (tools/tool-policies-for-memberships! db [mid]))) "empty clears"))))

(deftest ^:async membership-tool-policy-constraints-string-roundtrip-test
  (testing "membership constraints_json round-trips as the identical string"
    (let [db (mock-db)
          mid "m1"
          cj "{\"scope\":\"org\",\"n\":1}"]
      (await (tools/set-membership-tool-policies! db mid [{:tool-id "t.y" :effect "deny"
                                                           :constraints-json cj}]))
      (let [row (first (await (tools/tool-policies-for-memberships! db [mid])))]
        (is (= cj (:constraints_json row)) "string round-trips byte-for-byte")
        (is (= "deny" (:effect row)) "effect preserved")))))

(deftest ^:async tool-def-doc->row-id-parity-test
  (testing "tool-def-doc->row presents :id, hides tool_id + _id; nil-safe"
    (let [r (tools/tool-def-doc->row {:tool_id "t.id" :_id "x" :label "L"
                                      :description "D" :risk_level "high"})]
      (is (= "t.id" (:id r)))
      (is (nil? (:tool_id r)))
      (is (nil? (:_id r)))
      (is (= "L" (:label r)) "non-id columns untouched")
      (is (= "high" (:risk_level r)))
      (is (nil? (tools/tool-def-doc->row nil)) "nil-safe"))))

(defn- recording-index-db
  "Mock db whose collections record every createIndex (keys, opts) call."
  [calls*]
  #js {:collection
       (fn [coll-name]
         #js {:createIndex
              (fn [keys opts]
                (swap! calls* conj {:collection coll-name
                                    :keys (js->clj keys)
                                    :opts (js->clj opts :keywordize-keys true)})
                (js/Promise.resolve "ok"))})})

(deftest ^:async setup-indexes-spec-test
  (testing "index specs avoid partialFilterExpression footguns and carry the uniques"
    (let [calls* (atom [])]
      (await (tools/setup-indexes! (recording-index-db calls*)))
      (let [calls @calls*
            spec-str (pr-str calls)]
        ;; partialFilterExpression rejects {$exists false} (server error 67,
        ;; observed live 2026-06-06) — the store must not use it at all.
        (is (not (re-find #"partialFilterExpression" spec-str))
            "no partial indexes anywhere")
        (is (some #(and (= (:collection %) tools/TOOL_DEFINITIONS_COLLECTION)
                        (= (:keys %) {"tool_id" 1})
                        (true? (get-in % [:opts :unique])))
                  calls)
            "tool_definitions unique on tool_id (PG PRIMARY KEY id)")
        (is (some #(and (= (:collection %) tools/ROLE_TOOL_POLICIES_COLLECTION)
                        (= (:keys %) {"role_id" 1 "tool_id" 1})
                        (true? (get-in % [:opts :unique])))
                  calls)
            "role_tool_policies compound unique (role_id, tool_id)")
        (is (some #(and (= (:collection %) tools/USER_TOOL_POLICIES_COLLECTION)
                        (= (:keys %) {"membership_id" 1 "tool_id" 1})
                        (true? (get-in % [:opts :unique])))
                  calls)
            "user_tool_policies compound unique (membership_id, tool_id)")))))
