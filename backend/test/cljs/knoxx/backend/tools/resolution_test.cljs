(ns knoxx.backend.tools.resolution-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.set :as set]
            [knoxx.backend.tools.resolution :as resolution]
            [knoxx.backend.tools.dispatch :as dispatch]
            [knoxx.backend.tools.policies :as policies]))

(defn no-policy-grants?
  "Invariant 1: No tool in :tools was granted by a policy contract."
  [suite]
  (every? #(not= :policy/contract (get-in % [:provenance :granted-by]))
          (vals (:tools suite))))

(defn denied-reasons-complete?
  "Invariant 2: Every :denied tool has a :denied-reasons entry."
  [suite]
  (every? #(contains? (:denied-reasons suite) %)
          (:denied suite)))

(defn no-legacy-keys?
  "Invariant 3: No FORBIDDEN_INPUTS key survived into a ResolvedToolSuite."
  [suite]
  (empty? (set/intersection (set (keys suite) resolution/FORBIDDEN_INPUTS))))

(defn grant-refs-traceable?
  "Invariant 4: Every tool's grant-ref resolves to a loaded contract or actor id."
  [suite loaded-contract-ids actor-id]
  (every? (fn [{:keys [provenance]}]
            (or (= (:grant-ref provenance) actor-id)
                (contains? (set loaded-contract-ids) (:grant-ref provenance))))
          (vals (:tools suite))))

(deftest legacy-input-rejection
  (testing "FORBIDDEN_INPUTS keys are rejected at resolution time"
    (doseq [forbidden-key resolution/FORBIDDEN_INPUTS]
      (let [result (resolution/resolve-tool-suite
                    {:actor/id "test_actor"
                     forbidden-key ["something"]})]
        (is (false? (:ok result))
            (str "Should reject " (name forbidden-key)))
        (is (= :error/legacy-input (:error/kind result)))
        (is (some-> (:error/msg result) str/includes? "Legacy"))))))

(deftest invariant-no-policy-grants
  (let [result (resolution/resolve-tool-suite
                {:actor/id "workspace_user"
                 :roles []
                 :capabilities []})]
    (testing "No tool is granted by :policy/contract"
      (is (true? (:ok result)))
      (is (no-policy-grants? (:suite result))))))

(deftest invariant-denied-reasons-complete
  (let [result (resolution/resolve-tool-suite
                {:actor/id "workspace_user"
                 :roles ["knowledge_worker"]
                 :capabilities []
                 :policy/contracts ["nonexistent_policy"]})]
    (testing "Every denied tool has a :denied-reasons entry"
      (is (true? (:ok result)))
      (let [suite (:suite result)]
        (when (pos? (count (:denied suite)))
          (is (denied-reasons-complete? suite)))))))

(deftest invariant-no-legacy-keys
  (testing "ResolvedToolSuite contains no FORBIDDEN_INPUTS keys"
    (let [result (resolution/resolve-tool-suite
                  {:actor/id "workspace_user"
                   :roles []
                   :capabilities []})]
      (is (true? (:ok result)))
      (is (no-legacy-keys? (:suite result))))))

(deftest invariant-grant-refs-traceable
  (let [result (resolution/resolve-tool-suite
                {:actor/id "workspace_user"
                 :roles ["knowledge_worker"]
                 :capabilities []})]
    (testing "All grant-refs trace to actor or loaded contracts"
      (is (true? (:ok result)))
      (let [suite (:suite result)]
        (is (grant-refs-traceable? suite [] (:actor/id suite)))
        (is (grant-refs-traceable? suite ["cap_read"] (:actor/id suite)))))))

(deftest tool-call-contract-denies-unlisted
  (let [result (with-redefs [policies/load-tool-call-contracts!
                              (fn [_config ids]
                                (when (some #{"read_only"} ids)
                                  [{:contract/id "read_only"
                                    :contract/kind :tool-call
                                    :tools/allowed ["read"]}]))]
                  (resolution/resolve-tool-suite
                   {:actor/id "test"
                    :roles []
                    :capabilities []
                    :contract/uses ["read_only"]
                    :actor/defaults {:tools ["read" "write" "edit"]}}))]
    (testing "Tools not in tool-call contract's :tools/allowed are denied"
      (is (true? (:ok result)))
      (let [suite (:suite result)]
        (is (contains? (:tools suite) "read"))
        (is (nil? (get (:tools suite) "write")))
        (is (nil? (get (:tools suite) "edit")))
        (is (contains? (set (:denied suite)) "write"))
        (is (contains? (set (:denied suite)) "edit"))
        (is (not (contains? (set (:denied suite)) "read")))
        (is (contains? (:denied-reasons suite) "write"))
        (is (contains? (:denied-reasons suite) "edit"))))))

(deftest policy-denies-specified-tools
  (let [result (with-redefs [policies/load-policy-contracts!
                              (fn [_config ids]
                                (when (some #{"no_nrepl"} ids)
                                  [{:contract/id "no_nrepl"
                                    :contract/kind :policy
                                    :policy/denied ["nrepl.eval"]}]))]
                  (resolution/resolve-tool-suite
                   {:actor/id "restricted"
                    :roles ["knowledge_worker"]
                    :capabilities []
                    :policy/contracts ["no_nrepl"]}))]
    (testing "Tools listed in policy's :policy/denied are removed"
      (is (true? (:ok result)))
      (let [suite (:suite result)]
        (is (not (contains? (:tools suite) "nrepl.eval")))
        (is (contains? (set (:denied suite)) "nrepl.eval"))
        (is (some? (get (:denied-reasons suite) "nrepl.eval")))))))

(deftest actor-baseline-tools-included
  (let [result (resolution/resolve-tool-suite
                {:actor/id "test"
                 :roles []
                 :capabilities []
                 :actor/defaults {:tools ["session_mycology"]}})]
    (testing "Tools in :actor/defaults are included even without roles/capabilities"
      (is (true? (:ok result)))
      (let [suite (:suite result)]
        (is (contains? (:tools suite) "session_mycology"))
        (is (= :actor/baseline (get-in suite [:tools "session_mycology" :provenance :granted-by])))))))

(deftest empty-inputs-produce-empty-suite
  (let [result (resolution/resolve-tool-suite
                {:actor/id "test"
                 :roles []
                 :capabilities []})]
    (testing "No roles or capabilities yields an empty tool suite"
      (is (true? (:ok result)))
      (let [suite (:suite result)]
        (is (map? (:tools suite)))
        (is (empty? (:denied suite)))))))

(deftest resolved-suite-contains-required-keys
  (let [result (resolution/resolve-tool-suite
                {:actor/id "test_user"
                 :contract/id "test_contract"
                 :roles []
                 :capabilities []})]
    (testing "ResolvedToolSuite has all required keys"
      (is (true? (:ok result)))
      (let [suite (:suite result)]
        (is (string? (:run/id suite)))
        (is (string? (:contract/id suite)))
        (is (string? (:actor/id suite)))
        (is (inst? (:resolved-at suite)))
        (is (map? (:tools suite)))
        (is (vector? (:denied suite)))
        (is (map? (:denied-reasons suite)))))))

(deftest dispatch-tool-allowed
  (let [suite {:run/id "r1"
               :contract/id "c1"
               :actor/id "a1"
               :resolved-at (js/Date.)
               :tools {"read" {:tool/id "read"
                               :provenance {:granted-by :capability/explicit :grant-ref "cap/read"}}
                       "write" {:tool/id "write"
                                :provenance {:granted-by :actor/baseline :grant-ref "a1"}}}
               :denied ["email.send"]
               :denied-reasons {"email.send" "Denied by policy contract no_email_send"}}]
    (testing "dispatch/tool-allowed? uses resolved suite"
      (is (true? (dispatch/tool-allowed? suite "read")))
      (is (true? (dispatch/tool-allowed? suite "write")))
      (is (false? (dispatch/tool-allowed? suite "email.send")))
      (is (false? (dispatch/tool-allowed? suite "unknown_tool"))))
    (testing "dispatch/denied-tool? identifies denied tools"
      (is (true? (dispatch/denied-tool? suite "email.send")))
      (is (false? (dispatch/denied-tool? suite "read"))))
    (testing "dispatch/denied-reason returns reason string"
      (is (= "Denied by policy contract no_email_send"
             (dispatch/denied-reason suite "email.send")))
      (is (nil? (dispatch/denied-reason suite "read"))))
    (testing "dispatch/validate-call enforces suite boundary"
      (is (= {:ok true} (dispatch/validate-call suite "read")))
      (is (= {:ok false :error "Tool denied: Denied by policy contract no_email_send"}
             (dispatch/validate-call suite "email.send")))
      (is (= {:ok false :error "Tool unknown: totally_unknown"}
             (dispatch/validate-call suite "totally_unknown")))))
  (let [result (resolution/resolve-tool-suite {:actor/id "u1" :roles [] :capabilities []})]
    (is (true? (:ok result)))
    (let [suite (:suite result)]
      (is (empty? (dispatch/allowed-tool-ids suite)))
      (is (empty? (dispatch/filter-tool-ids suite ["read" "write"]))))))