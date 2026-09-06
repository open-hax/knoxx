(ns knoxx.frontend.law.migration-manifest-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.law.migration-manifest :as manifest]))

(deftest source-record-contract
  (testing "a legacy source names its language, island, disposition, and test status"
    (is (manifest/valid-record?
         {:migration/kind :legacy-source
          :migration/path "frontend/src/pages/CmsPage.tsx"
          :migration/language :tsx
          :migration/island :cms
          :migration/disposition :assess
          :migration/test? false})))
  (testing "closed maps reject silent manifest drift"
    (is (false?
         (manifest/valid-record?
          {:migration/kind :legacy-source
           :migration/path "frontend/src/pages/CmsPage.tsx"
           :migration/language :tsx
           :migration/island :cms
           :migration/disposition :assess
           :migration/test? false
           :migration/unknown true})))))

(deftest kind-specific-contracts
  (is (manifest/valid-record?
       {:migration/kind :legacy-test
        :migration/path "frontend/src/pages/CmsPage.test.tsx"
        :migration/island :cms}))
  (is (manifest/valid-record?
       {:migration/kind :bridge-export
        :migration/path "frontend/src/bridge/app.ts"
        :migration/bridge :app
        :migration/export "ChatPage"}))
  (is (manifest/valid-record?
       {:migration/kind :bridge-route
        :migration/path "frontend/src/cljs/knoxx/frontend/app.cljs"
        :migration/component "ChatPage"
        :migration/index 0}))
  (is (false?
       (manifest/valid-record?
        {:migration/kind :bridge-export
         :migration/path "frontend/src/bridge/app.ts"
         :migration/bridge :app}))))

(deftest assertion-carries-malli-evidence
  (let [record {:migration/kind :bridge-route
                :migration/path "frontend/src/cljs/knoxx/frontend/app.cljs"
                :migration/component "ChatPage"
                :migration/index -1}
        error (try
                (manifest/assert-record! record)
                nil
                (catch :default e e))]
    (is (some? error))
    (is (= record (:record (ex-data error))))
    (is (some? (:explain (ex-data error))))))
