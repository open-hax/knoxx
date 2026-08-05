(ns knoxx.backend.actor-credential-org-scope-test
  "Resolving an actor's membership when the same actor id exists in two orgs.

   knoxx_memberships indexes actor_id non-uniquely, and the credential reader
   resolved the membership with `findOne {actor_id}` alone. With one actor id
   present in two orgs that returns whichever document Mongo happens to hand
   back first, and the credential read that follows is keyed on that
   membership's user_id and org_id — so one tenant's MCP token could be served
   another tenant's Discord or Bluesky secret. Non-deterministic, unrepeatable,
   and invisible in any log.

   Exercised against a collection double rather than live Mongo, because the bug
   is in which document is selected, not in the driver."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.stores.mongo-policy-actor-credentials :as store]))

(defn- matches?
  "The subset of Mongo query semantics this store uses: equality on every key."
  [query doc]
  (every? (fn [[k v]] (= (get doc k) v)) (js->clj query)))

(defn- collection-double
  "A collection handle exposing find/toArray and findOne over fixed documents."
  [docs]
  #js {:find    (fn [query]
                  #js {:toArray (fn []
                                  (js/Promise.resolve
                                   (clj->js (filterv #(matches? query %) docs))))})
       :findOne (fn [query]
                  (js/Promise.resolve
                   (some-> (first (filterv #(matches? query %) docs)) clj->js)))})

(defn- db-double
  [memberships]
  #js {:collection (fn [name]
                     (if (= name "knoxx_memberships")
                       (collection-double memberships)
                       (collection-double [])))})

(def ^:private two-orgs
  [{"actor_id" "open_hax" "org_id" "org-a" "user_id" "user-a" "membership_id" "m-a"}
   {"actor_id" "open_hax" "org_id" "org-b" "user_id" "user-b" "membership_id" "m-b"}])

(deftest ^:async an-org-selects-its-own-membership
  (testing "each org resolves to its own membership, not to whichever is first"
    (let [a (await (store/resolve-actor-membership! (db-double two-orgs) {:actor-id "open_hax" :org-id "org-a"}))
          b (await (store/resolve-actor-membership! (db-double two-orgs) {:actor-id "open_hax" :org-id "org-b"}))]
      (is (= "user-a" (:user_id a)))
      (is (= "user-b" (:user_id b)))
      (is (not= (:user_id a) (:user_id b))
          "if these ever match, one tenant is reading the other's credentials"))))

(deftest ^:async an-ambiguous-actor-with-no-org-fails-closed
  (testing "returning an arbitrary tenant's membership is the bug; refusing is
            the only safe reading"
    (is (thrown? js/Error
                 (await (store/resolve-actor-membership! (db-double two-orgs) {:actor-id "open_hax"}))))))

(deftest ^:async an-unambiguous-actor-resolves-without-an-org
  (testing "the agent-spawn path passes no org and must keep working"
    (let [one [{"actor_id" "open_hax" "org_id" "org-a" "user_id" "user-a"}]
          m   (await (store/resolve-actor-membership! (db-double one) {:actor-id "open_hax"}))]
      (is (= "user-a" (:user_id m))))))

(deftest ^:async an-unknown-actor-resolves-to-nothing
  (is (nil? (await (store/resolve-actor-membership! (db-double two-orgs) {:actor-id "nobody"}))))
  (is (nil? (await (store/resolve-actor-membership! (db-double two-orgs) {:actor-id "open_hax" :org-id "org-c"})))
      "an actor that exists, but not in this org"))

(deftest ^:async a-blank-org-is-treated-as-no-org
  (testing "so it cannot be used to smuggle past the ambiguity check"
    (is (thrown? js/Error
                 (await (store/resolve-actor-membership! (db-double two-orgs) {:actor-id "open_hax" :org-id "   "}))))))

;; ─────────────────────────────────────────────────────────
;; Same-org duplicates, and the membership id that ends the search.
;;
;; Scoping to the org narrows the ambiguity without removing it: memberships are
;; unique only on (user_id, org_id), actor_id carries a plain lookup index, and
;; set-membership-actor-id! writes whatever it is given — so two members of ONE
;; org can share an actor id. Choosing between them means reading one member's
;; Discord or Bluesky secret for the other's request.
;; ─────────────────────────────────────────────────────────

(def ^:private two-in-one-org
  [{"actor_id" "open_hax" "org_id" "org-a" "user_id" "user-1" "membership_id" "m-1"}
   {"actor_id" "open_hax" "org_id" "org-a" "user_id" "user-2" "membership_id" "m-2"}])

(deftest ^:async duplicates-within-one-org-fail-closed
  (testing "an org scope is not enough to disambiguate, so it must refuse"
    (is (thrown? js/Error
                 (await (store/resolve-actor-membership!
                         (db-double two-in-one-org)
                         {:actor-id "open_hax" :org-id "org-a"}))))))

(deftest ^:async a-membership-id-resolves-exactly
  (testing "each member's own membership, with the duplicate present"
    (let [db (db-double two-in-one-org)
          m1 (await (store/resolve-actor-membership! db {:actor-id "open_hax" :membership-id "m-1"}))
          m2 (await (store/resolve-actor-membership! db {:actor-id "open_hax" :membership-id "m-2"}))]
      (is (= "user-1" (:user_id m1)))
      (is (= "user-2" (:user_id m2))))))

(deftest ^:async a-membership-whose-actor-disagrees-is-refused
  (testing "the actor is verified against the membership, never used to find it —
            a token whose two halves disagree is refused, not reconciled"
    (is (thrown? js/Error
                 (await (store/resolve-actor-membership!
                         (db-double two-in-one-org)
                         {:actor-id "discord_automation" :membership-id "m-1"}))))))

(deftest ^:async an-unknown-membership-resolves-to-nothing
  (is (nil? (await (store/resolve-actor-membership!
                    (db-double two-in-one-org)
                    {:actor-id "open_hax" :membership-id "m-nope"})))))

(deftest ^:async no-actor-resolves-to-nothing
  (testing "a scope with a membership but no actor must not resolve an owner"
    (is (nil? (await (store/resolve-actor-membership!
                      (db-double two-in-one-org)
                      {:actor-id nil :membership-id "m-1"}))))))
