(ns knoxx.backend.agent-hydration-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.agent-hydration :as agent-hydration]))

(deftest agent-custom-tool-suite-selects-contract-librarian-runtime
  (testing "contract librarian sessions use the dedicated contract-oriented tool suite"
    (is (= :contract-librarian
           (agent-hydration/agent-custom-tool-suite {:contract-id "contract_librarian"
                                                     :role "contract_librarian"})))
    (is (= :contract-librarian
           (agent-hydration/agent-custom-tool-suite {:contract-id "contract_librarian"})))
    (is (= :knoxx
           (agent-hydration/agent-custom-tool-suite {:contract-id "knoxx_default"
                                                     :role "knowledge_worker"})))))