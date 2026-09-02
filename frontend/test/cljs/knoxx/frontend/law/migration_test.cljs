(ns knoxx.frontend.law.migration-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(def legacy-file
  (shape/legacy-file-record
   {:path "frontend/src/pages/LegacyPage.tsx"
    :source "export default function LegacyPage() {}"
    :tests []}))

(def legacy-route
  (shape/route-record
   {:path "frontend/src/cljs/knoxx/frontend/app.cljs"
    :route "routes/legacy-route"
    :implementation "app/LegacyPage"
    :legacy? true}))

(def native-route
  (shape/route-record
   {:path "frontend/src/cljs/knoxx/frontend/app.cljs"
    :route "routes/native-route"
    :implementation "native/page"
    :legacy? false}))

(deftest manifest-contract-rejects-duplicate-identities
  (is (thrown-with-msg? js/Error #"identities must be unique"
                        (law/assert-manifest! [legacy-file legacy-file]))))

(deftest ratchet-rejects-new-legacy-and-route-regression
  (let [new-file (shape/legacy-file-record
                  {:path "frontend/src/pages/NewLegacyPage.tsx"
                   :source "export default function NewLegacyPage() {}"
                   :tests []})
        regressed-native (assoc native-route
                                :implementation "app/NativePage"
                                :status :legacy)
        violations (law/ratchet-violations
                    {:baseline [legacy-file native-route]
                     :current [legacy-file new-file regressed-native]
                     :changed-paths ["frontend/src/pages/NewLegacyPage.tsx"]
                     :infrastructure? false})
        laws (set (map :law violations))]
    (is (contains? laws :tsx-count/non-growth))
    (is (contains? laws :legacy-source/no-new-paths))
    (is (contains? laws :native-routes/no-regression))
    (is (contains? laws :migration-slice/must-progress))))

(deftest migration-slice-must-delete-or-declare-infrastructure
  (testing "an unchanged touched surface fails"
    (is (= [:migration-slice/must-progress]
           (mapv :law
                 (law/ratchet-violations
                  {:baseline [legacy-file legacy-route]
                   :current [legacy-file legacy-route]
                   :changed-paths ["frontend/src/cljs/knoxx/frontend/new.cljs"]
                   :infrastructure? false})))))
  (testing "an infrastructure declaration admits a non-shrinking bootstrap"
    (is (empty?
         (law/ratchet-violations
          {:baseline [legacy-file legacy-route]
           :current [legacy-file legacy-route]
           :changed-paths ["frontend/migration/manifest.ndedn"]
           :infrastructure? true}))))
  (testing "deleting a legacy implementation is sufficient progress"
    (is (empty?
         (law/ratchet-violations
          {:baseline [legacy-file legacy-route]
           :current [legacy-route]
           :changed-paths ["frontend/src/pages/LegacyPage.tsx"]
           :infrastructure? false})))))
