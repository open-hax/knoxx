(ns knoxx.backend.infra.clio-subscription-test
  "Real durable writes keep observer selection private and failure-independent."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.source-authoring :as source-domain]
            [knoxx.backend.domain.source-review :as review-domain]
            [knoxx.backend.extern.clio-store :as host]
            [knoxx.backend.extern.clio-store-fixture :as fixture]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.clio-source-authoring-store :as sources]
            [knoxx.backend.infra.clio-source-review-store :as reviews]
            [knoxx.backend.infra.publication-source-revision :as revision]
            [knoxx.backend.infra.source-authoring-store :as source-store]
            [knoxx.backend.infra.source-review-store :as review-store]))

(def ^:private scope {:org-id "org-a" :project "wiki"})
(def ^:private stream "knoxx/source-authoring")

(defn- open!
  [directory options]
  (clio/open!
   (merge {:directory directory :stream stream
           :projection #(let [state (atom {})] {:store state :snapshot (fn [] @state)})
           :reads {:test/get (fn [state key] (get @state key))}
           :writes {:test/put (fn [state key value] (swap! state assoc key value) value)}
           :change-scope #(first (:operation/args %))}
          options)))

(deftest ^:async selected-observers-only-receive-matching-stream-names
  (let [directory (fixture/temp-directory!) changes (atom []) legacy (atom [])
        unsubscribe (clio/subscribe! {:streams #{stream} :scope scope}
                                    (fn [& args] (swap! changes conj (vec args))))
        legacy-off (clio/subscribe! (fn [& args] (swap! legacy conj (vec args))))]
    (try
      (let [store (open! (str directory "/source") {})]
        (await (clio/write! (open! (str directory "/other") {:stream "knoxx/oauth"})
                           :test/put [scope "private credential"]))
        (await (clio/write! store :test/put [(assoc scope :org-id "foreign") "private source"]))
        (await (clio/write! store :test/put [(assoc scope :project "other") "private source"]))
        (await (clio/write! (open! (str directory "/unscoped") {:change-scope nil})
                           :test/put [scope "private source"]))
        (await (fixture/drain!))
        (is (empty? @changes))
        (is (= [[] [] [] []] @legacy) "Legacy subscribers still receive no arguments")
        (await (clio/write! store "accepted" :test/put [scope "private source"]))
        (await (clio/write! store "accepted" :test/put [scope "private source"]))
        (await (clio/write! store "no-op" :test/put [scope "private source"]))
        (await (fixture/drain!))
        (is (= [[stream]] @changes) "No scope, arguments, result or operation id is broadcast")
        (unsubscribe)
        (unsubscribe)
        (await (clio/write! store :test/put [scope "later private source"]))
        (await (fixture/drain!))
        (is (= [[stream]] @changes)))
      (finally (unsubscribe) (legacy-off) (fs/remove-tree! directory)))))

(deftest ^:async scope-extraction-and-observer-failures-cannot-refuse-a-durable-write
  (let [directory (fixture/temp-directory!) failures (atom []) observed (atom [])
        subscriptions [(clio/subscribe! {:streams #{stream} :scope scope} #(swap! observed conj %))
                       (clio/subscribe! {:streams #{stream}}
                                        (fn [_] (throw (ex-info "sync observer" {}))))
                       (clio/subscribe! {:streams #{stream}}
                                        (^:async fn [_] (throw (ex-info "async observer" {}))))]]
    (try
      (with-redefs [host/report-subscriber-failure! #(swap! failures conj (ex-message %))]
        (doseq [[name extract] [["thrown" (fn [_] (throw (ex-info "scope refused" {})))]
                               ["invalid" (constantly nil)]]]
          (let [store (open! (str directory "/" name) {:change-scope extract})]
            (is (= "accepted" (await (clio/write! store :test/put [scope "accepted"]))))
            (await (fixture/drain!))
            (is (= "accepted" (await (clio/read! (open! (str directory "/" name) {})
                                               :test/get [scope]))))
            (is (= 1 (count (clio/history store))))))
        (is (empty? @observed))
        (is (= 2 (get (frequencies @failures) "sync observer")))
        (is (= 2 (get (frequencies @failures) "async observer")))
        (is (some #{"scope refused"} @failures))
        (is (some #{"Clio change scope must be a map"} @failures)))
      (finally (doseq [unsubscribe subscriptions] (unsubscribe)) (fs/remove-tree! directory)))))

(deftest invalid-selection-and-extractors-refuse-before-installation
  (doseq [selection [{:streams "source"} {:streams #{nil}} {:scope "org-a"} {:unknown true}]]
    (is (thrown? js/Error (clio/subscribe! selection identity))))
  (is (thrown? js/Error (clio/subscribe! nil)))
  (let [directory (fixture/temp-directory!) child (str directory "/invalid")]
    (try
      (is (thrown? js/Error (open! child {:change-scope "invalid"})))
      (is (not (fs/exists? child)))
      (finally (fs/remove-tree! directory)))))

(defn- ^:async admit-source-and-review!
  [source-provider review-provider document-scope]
  (let [actor {:id "principal" :kind :human}
        document {:document/id (:document document-scope) :document/title "Private title"
                  :document/source-locale :en :document/org-id (:org-id document-scope)
                  :document/visibility :private :document/source {:path "cms/private.md"}}
        content "Private content." digest (revision/content-revision content)]
    (await (source-store/admit-source!
               source-provider document-scope nil
               (source-domain/source-event document-scope actor "same-source-id" :observe nil
                                           document content digest "2026-09-20T00:00:00Z")))
       (await (review-store/admit-source-review!
               review-provider document-scope nil
               (review-domain/event-for-command
                document-scope actor {:operation-id "same-review-id" :action :submit
                                      :revision digest :source-locale :en :expected-head nil}
                "2026-09-20T00:00:00Z")))))

(deftest ^:async real-source-providers-select-scope-without-binding-domain-ids-globally
  (let [directory (fixture/temp-directory!) changes (atom [])
        unsubscribe (clio/subscribe! {:streams #{stream "knoxx/source-review"} :scope scope}
                                    #(swap! changes conj %))]
    (try
      (let [source-provider (sources/open! {:directory (str directory "/source")})
            review-provider (reviews/open! {:directory (str directory "/review")})]
        (doseq [selected [(assoc scope :org-id "foreign") (assoc scope :project "other") scope]]
          (await (admit-source-and-review! source-provider review-provider (assoc selected :document :docs/page))))
        (await (fixture/drain!))
        (is (= [stream "knoxx/source-review"] @changes))
        (is (= 3 (count (clio/history (:ledger source-provider)))))
        (is (= 3 (count (clio/history (:ledger review-provider))))))
      (finally (unsubscribe) (fs/remove-tree! directory)))))
