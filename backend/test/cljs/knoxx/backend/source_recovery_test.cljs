(ns knoxx.backend.source-recovery-test
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.source-review :as domain]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.clio-source-authoring-store :as sources]
            [knoxx.backend.infra.clio-source-review-store :as reviews]
            [knoxx.backend.infra.source-authoring :as authoring]
            [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.infra.source-review-store :as review-store]))

(def scope {:org-id "org-a" :project "wiki"})
(def actor {:id "principal-a" :kind :human})
(def creation {:operation-id "create-one" :title "Source recovery" :content "Original source." :source-locale :en
               :garden :test/garden :target-locales [:es]})
(defn- ^:async refused [f]
  (try (await (f)) nil (catch :default error (ex-data error))))
(defn- ^:async fixture! [f]
  (let [root (disk/temp-directory!) contracts (str root "/contracts")]
    (try
      (fs/ensure-dir! contracts)
      (await (files/write-text! contracts "namespaces/garden.edn"
                                "{:namespace :test :resources [{:garden/id :garden :garden/title \"Garden\" :garden/status :active :garden/locales [:en :es]}]}"))
      (let [options {:source {:directory (str root "/source")} :review {:directory (str root "/review")}}
            dependencies {:source-provider (sources/open! (:source options)) :provider (reviews/open! (:review options))
                          :now! (constantly "2026-09-12T12:00:00.000Z")}]
        (await (f {:contracts-dir contracts} dependencies options)))
      (catch :default error (is false (str "Unexpected recovered source workflow error: " error)))
      (finally (fs/remove-tree! root)))))
(defn- command [id action current]
  {:operation-id id :revision (:revision current) :source-locale (:source-locale current)
   :expected-head (:head current) :action action})
(defn- ^:async accept! [config document dependencies]
  (let [current (await (review/read! config document dependencies))
        submitted (await (review/command! config document actor (command "submit-one" :submit current) dependencies))]
    (await (review/command! config document actor (command "accept-one" :accept (:review submitted)) dependencies))))

(deftest ^:async real-source-and-review-ledgers-recover-exact-revision-acceptance
  (await (fixture!
    (^:async fn [config dependencies options]
      (let [created (await (authoring/create! config scope actor creation dependencies))
            document (assoc scope :document (get-in created [:review :document]))
            accepted (await (accept! config document dependencies))
            restarted (assoc dependencies :source-provider (sources/open! (:source options)) :provider (reviews/open! (:review options)))
            revision (get-in accepted [:review :revision])]
        (is (true? (get-in accepted [:review :accepted])))
        (is (true? (:accepted (await (review/read! config document restarted)))))
        (let [saved (await (authoring/save! config document actor {:operation-id "save-one" :expected-revision revision :content "Revised source."} restarted))]
          (is (false? (get-in saved [:review :accepted])))
          (is (true? (get-in saved [:review :stale])))
          (is (= 2 (count (get-in saved [:review :history]))))
          (is (= "source_review_stale_revision"
                 (:code (await (refused #(review/command! config document actor (command "stale-accept" :accept (:review accepted)) restarted))))))
          (is (= "Revised source." (get-in (await (authoring/create! config scope actor creation restarted)) [:review :content])))))))))

(deftest ^:async pending-projection-blocks-old-acceptance-and-exact-save-retry-repairs
  (await (fixture!
    (^:async fn [config dependencies _options]
      (let [created (await (authoring/create! config scope actor creation dependencies))
            document (assoc scope :document (get-in created [:review :document]))
            accepted (await (accept! config document dependencies))
            save {:operation-id "repair-one" :expected-revision (get-in accepted [:review :revision]) :content "Durable revised content."}]
        (with-redefs [files/write-text! (fn [_ _ _] (throw (ex-info "disk full" {:status 503 :code "test_disk_full"})))]
          (is (= "test_disk_full" (:code (await (refused #(authoring/save! config document actor save dependencies)))))))
        (is (= "source_projection_repair_required" (:code (await (refused #(review/read! config document dependencies))))))
        (let [repaired (await (authoring/save! config document actor save dependencies))]
          (is (true? (:existing? repaired)))
          (is (= "Durable revised content." (get-in repaired [:review :content])))
          (is (false? (get-in repaired [:review :accepted]))))
        (is (= "source_authoring_operation_conflict"
               (:code (await (refused #(authoring/save! config document actor (assoc save :content "Conflicting bytes") dependencies)))))))))))

(deftest ^:async review-provider-rejects-stale-head-and-retains-first-retry-fact
  (await (fixture!
    (^:async fn [_config dependencies options]
      (let [scope (assoc scope :document :docs/one)
            command {:operation-id "one" :revision "sha256-one" :source-locale :en :expected-head nil :action :submit}
            event (domain/event-for-command scope actor command "2026-09-12T12:00:00Z")
            provider (:provider dependencies)]
        (is (false? (:existing? (await (review-store/admit-source-review! provider scope nil event)))))
        (is (true? (:existing? (await (review-store/admit-source-review! (reviews/open! (:review options)) scope nil event)))))
        (is (= "source_review_stale_head"
               (:code (await (refused #(review-store/admit-source-review! provider scope nil
                                              (assoc event :review/id "two" :review/action :accept))))))))))))
