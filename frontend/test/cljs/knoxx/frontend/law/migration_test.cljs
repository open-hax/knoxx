(ns knoxx.frontend.law.migration-test
  (:require [cljs.test :as t]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(def legacy-file
  "Representative legacy file fixture."
  (shape/legacy-file-record
   {:path "frontend/src/pages/LegacyPage.tsx"
    :disposition :port
    :tests []}))

(def legacy-route
  "Representative legacy route fixture."
  (shape/route-record
   {:path "frontend/src/cljs/knoxx/frontend/app.cljs"
    :route "routes/legacy-route"
    :implementation "app/LegacyPage"
    :legacy? true}))

(def native-route
  "Representative native route fixture."
  (shape/route-record
   {:path "frontend/src/cljs/knoxx/frontend/app.cljs"
    :route "routes/native-route"
    :implementation "native/page"
    :legacy? false}))

(t/deftest manifest-contract-rejects-duplicate-identities
  (t/is (try
          (law/assert-manifest! [legacy-file legacy-file])
          false
          (catch js/Error error
            (boolean (re-find #"identities must be unique"
                              (.-message error)))))))

(t/deftest ratchet-rejects-new-legacy-and-route-regression
  (let [new-file (shape/legacy-file-record
                  {:path "frontend/src/pages/NewLegacyPage.tsx"
                   :disposition :port
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
    (t/is (contains? laws :tsx-count/non-growth))
    (t/is (contains? laws :legacy-source/no-new-paths))
    (t/is (contains? laws :native-routes/no-regression))
    (t/is (contains? laws :migration-slice/must-progress))))

(t/deftest removing-a-native-route-is-not-a-regression
  (let [laws (->> (law/ratchet-violations
                   {:baseline [native-route]
                    :current []
                    :changed-paths []
                    :infrastructure? false})
                  (map :law)
                  set)]
    (t/is (not (contains? laws :native-routes/no-regression)))))

(t/deftest migration-slice-must-delete-or-declare-infrastructure
  (t/testing "an unchanged touched surface fails"
    (t/is (= [:migration-slice/must-progress]
             (mapv :law
                   (law/ratchet-violations
                    {:baseline [legacy-file legacy-route]
                     :current [legacy-file legacy-route]
                     :changed-paths ["frontend/src/cljs/knoxx/frontend/new.cljs"]
                     :infrastructure? false})))))
  (t/testing "an infrastructure declaration admits a non-shrinking bootstrap"
    (t/is (empty?
           (law/ratchet-violations
            {:baseline [legacy-file legacy-route]
             :current [legacy-file legacy-route]
             :changed-paths ["frontend/migration/manifest.ndedn"]
             :infrastructure? true}))))
  (t/testing "deleting a legacy implementation is sufficient progress"
    (t/is (empty?
           (law/ratchet-violations
            {:baseline [legacy-file legacy-route]
             :current [legacy-route]
             :changed-paths ["frontend/src/pages/LegacyPage.tsx"]
             :infrastructure? false})))))
