(ns knoxx.frontend.shape.migration-test
  (:require [cljs.test :as t]
            [knoxx.frontend.shape.migration :as shape]))

(t/deftest constructs-classified-source-records
  (t/testing "preclassified contracts retain their dependencies"
    (let [record (shape/legacy-file-record
                  {:path "frontend/src/pages/ContractsPage.tsx"
                   :island :contracts
                   :blocked-by [:chat-workspace :codemirror-adapter]
                   :disposition :port
                   :tests ["frontend/src/pages/ContractsPage.test.tsx"]})]
      (t/is (= :contracts (:island record)))
      (t/is (= [:chat-workspace :codemirror-adapter] (:blocked-by record)))
      (t/is (= :port (:disposition record))))))

(t/deftest loader-shims-delete-with-their-final-consumer
  (let [record (shape/legacy-file-record
                {:path "frontend/src/pages/SettingsPage.tsx"
                 :island :routes
                 :blocked-by []
                 :disposition :delete
                 :tests ["frontend/src/pages/SettingsPage.test.tsx"]})]
    (t/is (= :delete (:disposition record)))
    (t/is (= :route (:role record)))))
