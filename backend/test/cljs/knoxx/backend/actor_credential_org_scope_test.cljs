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
  "A collection handle exposing only find/toArray over fixed documents."
  [docs]
  #js {:find (fn [query]
               #js {:toArray (fn []
                               (js/Promise.resolve
                                (clj->js (filterv #(matches? query %) docs))))})})

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
    (let [a (await (store/resolve-actor-membership! (db-double two-orgs) "open_hax" "org-a"))
          b (await (store/resolve-actor-membership! (db-double two-orgs) "open_hax" "org-b"))]
      (is (= "user-a" (:user_id a)))
      (is (= "user-b" (:user_id b)))
      (is (not= (:user_id a) (:user_id b))
          "if these ever match, one tenant is reading the other's credentials"))))

(deftest ^:async an-ambiguous-actor-with-no-org-fails-closed
  (testing "returning an arbitrary tenant's membership is the bug; refusing is
            the only safe reading"
    (is (thrown? js/Error
                 (await (store/resolve-actor-membership! (db-double two-orgs) "open_hax" nil))))))

(deftest ^:async an-unambiguous-actor-resolves-without-an-org
  (testing "the agent-spawn path passes no org and must keep working"
    (let [one [{"actor_id" "open_hax" "org_id" "org-a" "user_id" "user-a"}]
          m   (await (store/resolve-actor-membership! (db-double one) "open_hax" nil))]
      (is (= "user-a" (:user_id m))))))

(deftest ^:async an-unknown-actor-resolves-to-nothing
  (is (nil? (await (store/resolve-actor-membership! (db-double two-orgs) "nobody" nil))))
  (is (nil? (await (store/resolve-actor-membership! (db-double two-orgs) "open_hax" "org-c")))
      "an actor that exists, but not in this org"))

(deftest ^:async a-blank-org-is-treated-as-no-org
  (testing "so it cannot be used to smuggle past the ambiguity check"
    (is (thrown? js/Error
                 (await (store/resolve-actor-membership! (db-double two-orgs) "open_hax" "   "))))))
