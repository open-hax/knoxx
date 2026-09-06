(ns knoxx.frontend.law.migration-test
  (:require [cljs.test :as t]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(t/deftest active-bridge-resolutions-establish-only-their-build-obligations
  (t/is (= [] (law/required-bridge-builds #{"react"})))
  (t/is (= ["build:app-bridge"]
           (law/required-bridge-builds #{"@open-hax/knoxx-app-bridge"})))
  (t/is (= ["build:bridge" "build:app-bridge"]
           (law/required-bridge-builds #{"@open-hax/knoxx-app-bridge"
                                        "@open-hax/knoxx-frontend-bridge"}))))

(t/deftest production-build-contract-admits-active-and-retired-bridge-states
  (doseq [[phases required]
          [[["build:bridge" "build:app-bridge" :shadow :css] ["build:bridge" "build:app-bridge"]]
           [["build:app-bridge" :shadow] ["build:app-bridge"]]
           [[:shadow] []]]]
    (let [facts {:phases phases :required-bridges required :available-scripts (set required)}]
      (t/is (= facts (law/assert-production-build! facts))))))

(t/deftest production-build-contract-rejects-missing-late-and-duplicate-bridges
  (let [facts {:phases ["build:app-bridge" :shadow]
               :required-bridges ["build:app-bridge"]
               :available-scripts #{"build:app-bridge"}}]
    (doseq [override [{:available-scripts #{}}
                      {:phases [:shadow]}
                      {:phases [:shadow "build:app-bridge"]}
                      {:phases ["build:app-bridge" "build:app-bridge" :shadow]}
                      {:phases ["build:app-bridge" :css :shadow]}]]
      (t/is (thrown-with-msg? js/Error #"Unsupported Vite migration configuration"
                             (law/assert-production-build! (merge facts override)))))))

(def legacy-file
  "Representative legacy file fixture."
  (shape/legacy-file-record
   {:path "frontend/src/pages/LegacyPage.tsx"
    :role :route
    :island :routes
    :blocked-by []
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
                   :role :route
                   :island :routes
                   :blocked-by []
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

(t/deftest renamed-native-routes-cannot-reenter-under-new-legacy-identities
  (let [renamed (assoc native-route
                       :record/id "route:routes/renamed-route"
                       :route "routes/renamed-route"
                       :implementation "app/NativePage"
                       :status :legacy)]
    (doseq [infrastructure? [false true]]
      (t/is (= [{:law :legacy-routes/no-new-identities
                 :routes [(:record/id renamed)]}]
               (law/ratchet-violations
                 {:baseline [legacy-file legacy-route native-route]
                  :current [renamed]
                  :changed-paths ["frontend/src/cljs/knoxx/frontend/app.cljs"]
                  :infrastructure? infrastructure?}))
            "Neither other legacy deletions nor infrastructure admission can mask a renamed regression"))))

(t/deftest ambiguous-legacy-route-renames-fail-closed
  (let [renamed (assoc legacy-route
                       :record/id "route:routes/renamed-legacy-route"
                       :route "routes/renamed-legacy-route")]
    (t/is (= [:legacy-routes/no-new-identities]
             (mapv :law
                   (law/ratchet-violations
                     {:baseline [legacy-file legacy-route]
                      :current [renamed]
                      :changed-paths ["frontend/src/cljs/knoxx/frontend/app.cljs"]
                      :infrastructure? false})))
          "An unchanged bridge implementation cannot prove the renamed expression has the same URL")))

(t/deftest native-route-identities-may-change-or-grow-with-legacy-progress
  (let [renamed (assoc native-route
                       :record/id "route:routes/renamed-native-route"
                       :route "routes/renamed-native-route")]
    (doseq [current [[renamed] [native-route renamed]]]
      (t/is (empty?
              (law/ratchet-violations
                {:baseline [legacy-file native-route]
                 :current current
                 :changed-paths ["frontend/src/cljs/knoxx/frontend/app.cljs"]
                 :infrastructure? false}))))))

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

(t/deftest migration-integration-changes-require-infrastructure-declaration
  (doseq [path ["frontend/.clj-kondo/config.edn"
               "scripts/pre-push-checks.sh"
               "scripts/lint-frontend-cljs-changed.sh"]]
    (t/testing path
      (let [inputs {:baseline [legacy-file legacy-route]
                    :current [legacy-file legacy-route]
                    :changed-paths [path]
                    :infrastructure? false}]
        (t/is (= [:migration-slice/must-progress]
                 (mapv :law (law/ratchet-violations inputs))))
        (t/is (empty? (law/ratchet-violations
                       (assoc inputs :infrastructure? true)))))))
  (t/testing "an unrelated path has no migration progress obligation"
    (t/is (empty? (law/ratchet-violations
                   {:baseline [legacy-file legacy-route]
                    :current [legacy-file legacy-route]
                    :changed-paths ["docs/verification/example.md"]
                    :infrastructure? false})))))

(t/deftest modern-typescript-modules-remain-subject-to-the-ratchet
  (doseq [extension ["mts" "cts"]]
    (let [path (str "frontend/src/lib/legacy." extension)
          modern-file (shape/legacy-file-record
                        {:path path :role :library :island :shared
                         :blocked-by [] :disposition :port :tests []})
          inputs {:baseline [modern-file] :current [modern-file]
                  :changed-paths [path] :infrastructure? false}]
      (t/testing "editing an existing module requires migration progress"
        (t/is (= [:migration-slice/must-progress]
                 (mapv :law (law/ratchet-violations inputs)))))
      (t/testing "an infrastructure declaration cannot admit a new legacy module"
        (t/is (= [:typescript-count/non-growth :legacy-source/no-new-paths]
                 (mapv :law
                       (law/ratchet-violations
                         (assoc inputs :baseline [] :infrastructure? true)))))))))

(t/deftest source-paths-with-linebreaks-remain-governed
  (doseq [extension ["ts" "tsx" "mts" "cts" "cljs" "cljc"]]
    (let [path (str "frontend/src/lib/legacy\nmodule." extension)]
      (when (contains? #{"ts" "tsx" "mts" "cts"} extension)
        (t/is (some? (re-find law/legacy-source-pattern path))
              "A newline in the filename cannot remove TypeScript from inventory"))
      (t/is (= [:migration-slice/must-progress]
               (mapv :law
                     (law/ratchet-violations
                       {:baseline [legacy-file] :current [legacy-file]
                        :changed-paths [path] :infrastructure? false})))
            "A newline in the filename cannot exempt an edit from progress"))))
