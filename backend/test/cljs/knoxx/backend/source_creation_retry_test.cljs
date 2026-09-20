(ns knoxx.backend.source-creation-retry-test
  "Accepted creation receipts survive mutable garden topology and projection loss."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :as test]
            [knoxx.backend.domain.source-authoring :as domain]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.clio-source-authoring-store :as sources]
            [knoxx.backend.infra.clio-source-review-store :as reviews]
            [knoxx.backend.infra.source-authoring :as authoring]
            [knoxx.backend.infra.source-authoring-store :as store]))

(def ^:private scope {:org-id "org-a" :project "wiki"})
(def ^:private actor {:id "human-a" :kind :human})
(def ^:private command {:operation-id "create-one" :title "Creation recovery" :content "Accepted bytes."
                       :source-locale :en :garden :test/garden :target-locales [:es]})
(def ^:private garden {:garden/id :garden :garden/title "Garden" :garden/status :active :garden/locales [:en :es]})

(defn- ^:async attempt [operation]
  (try {:result (await (operation))} (catch :default error {:error (ex-data error)})))

(defn- ^:async garden! [contracts resource]
  (if resource
    (await (files/write-text! contracts "namespaces/garden.edn"
                             (files/manifest-text {:namespace :test :resources [resource]})))
    (fs/delete-if-exists! (str contracts "/namespaces/garden.edn"))))

(defn- ^:async fixture! [operation]
  (let [root (disk/temp-directory!) contracts (str root "/contracts") directory (str root "/sources")
        config {:contracts-dir contracts}
        dependencies {:source-provider (sources/open! {:directory directory})
                      :provider (reviews/open! {:directory (str root "/reviews")})
                      :now! (constantly "2026-09-20T00:00:00Z")}]
    (try
      (fs/ensure-dir! contracts)
      (await (garden! contracts garden))
      (await (operation {:root root :contracts contracts :directory directory
                         :config config :dependencies dependencies}))
      (finally (fs/remove-tree! root)))))

(test/deftest ^:async accepted-create-repairs-after-garden-archive-locale-removal-or-deletion
  (doseq [changed [(assoc garden :garden/status :archived) (assoc garden :garden/locales [:en]) nil]]
    (await (fixture!
      (^:async fn [{:keys [root contracts directory config dependencies]}]
        (let [identity (domain/creation-identity scope command)
              document-scope (assoc scope :document (:document identity))]
          (with-redefs [files/write-text! (fn [_ _ _] (throw (ex-info "Projection failed" {:code "test_disk_full"})))]
            (test/is (= "test_disk_full"
                        (get-in (await (attempt #(authoring/create! config scope actor command dependencies))) [:error :code]))))
          (let [history (await (store/source-events! (:source-provider dependencies) document-scope))
                reopened (assoc dependencies :source-provider (sources/open! {:directory directory}))]
            (test/is (= 1 (count history)))
            (await (garden! contracts changed))
            (test/is (= "source_authoring_garden_refused"
                        (get-in (await (attempt #(authoring/create! config scope actor
                                                                   (assoc command :operation-id "new-create") reopened)))
                                [:error :code])))
            (let [outcome (await (attempt #(authoring/create! config scope actor command reopened)))]
              (test/is (nil? (:error outcome)))
              (test/is (true? (get-in outcome [:result :existing?])))
              (test/is (= (first history) (get-in outcome [:result :event])))
              (test/is (= "Accepted bytes." (await (files/read-text! (str root "/" (:source-path identity))))))
              (test/is (= (:source/manifest (first history))
                          (some-> (await (files/read-text! (str contracts "/" (:manifest-path identity))))
                                  files/parse-manifest)))
              (test/is (= history (await (store/source-events! (:source-provider reopened) document-scope))))))))))))

(test/deftest ^:async accepted-create-still-refuses-changed-command-and-actor
  (await (fixture!
    (^:async fn [{:keys [contracts directory config dependencies]}]
      (let [created (await (authoring/create! config scope actor command dependencies))
            document-scope (assoc scope :document (get-in created [:event :source/document :document/id]))
            history (await (store/source-events! (:source-provider dependencies) document-scope))
            reopened (assoc dependencies :source-provider (sources/open! {:directory directory}))]
        (await (garden! contracts nil))
        (doseq [[who request] [[(assoc actor :id "another-human") command]
                               [actor (assoc command :content "Different bytes.")]
                               [actor (assoc command :title "Different title")]
                               [actor (assoc command :source-locale :es)]
                               [actor (assoc command :target-locales [:fr])]
                               [actor (assoc command :garden :other/garden)]]]
          (test/is (= "source_authoring_operation_conflict"
                      (get-in (await (attempt #(authoring/create! config scope who request reopened))) [:error :code]))))
        (test/is (= history (await (store/source-events! (:source-provider reopened) document-scope)))))))))
