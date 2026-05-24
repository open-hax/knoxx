(ns knoxx.backend.openplanner-semantic-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.infra.openplanner.memory :as memory]
            [knoxx.backend.infra.openplanner.semantic :as semantic]))

(def vector-search-body
  {:result {:ids [["doc-1"]]
            :documents [["A long semantic match from the corpus about Knoxx ingestion."]]
            :metadatas [[{:sourcePath "docs/knoxx.md" :chunkCount 3}]]
            :distances [[0.25]]}})

(defn- fake-json-response
  [body]
  (doto (js-obj)
    (aset "ok" true)
    (aset "status" 200)
    (aset "headers" nil)
    (aset "text" (fn [] (js/Promise.resolve (.stringify js/JSON (clj->js body)))))))

(defn- install-fake-fetch!
  [requests* response-body]
  (let [original-fetch (aget js/globalThis "fetch")]
    (aset js/globalThis
          "fetch"
          (fn [url init]
            (swap! requests* conj {:url (str url)
                                   :method (aget init "method")
                                   :body (when-let [body (aget init "body")]
                                           (js->clj (.parse js/JSON body) :keywordize-keys true))})
            (js/Promise.resolve (fake-json-response response-body))))
    original-fetch))

(deftest ^:async openplanner-semantic-search-resolves-vector-hits
  (let [requests* (atom [])
        original-fetch (install-fake-fetch! requests* vector-search-body)]
    (try
      (let [result (await (memory/openplanner-semantic-search! {:openplanner-base-url "http://openplanner.test"
                                                                :openplanner-api-key "test-key"
                                                                :project-name "devel"}
                                                               {:query "knoxx" :k 1}))]
        (is (= [{:url "http://openplanner.test/v1/search/vector"
                 :method "POST"
                 :body {:q "knoxx" :k 1 :project "devel"}}]
               @requests*))
        (is (= :vector (:mode result)))
        (is (= [{:id "doc-1"
                 :document "A long semantic match from the corpus about Knoxx ingestion."
                 :metadata {:sourcePath "docs/knoxx.md" :chunkCount 3}
                 :distance 0.25}]
               (:hits result))))
      (finally
        (aset js/globalThis "fetch" original-fetch)))))

(deftest ^:async semantic-search-documents-returns-passive-hydration-shape
  (let [requests* (atom [])
        original-fetch (install-fake-fetch! requests* vector-search-body)]
    (try
      (let [result (await (semantic/semantic-search-documents! nil
                                                               {:openplanner-base-url "http://openplanner.test"
                                                                :openplanner-api-key "test-key"
                                                                :project-name "devel"}
                                                               {:query "knoxx"
                                                                :top-k 1
                                                                :max-snippet-chars 240}
                                                               nil))
            first-result (first (:results result))]
        (is (= [{:url "http://openplanner.test/v1/search/vector"
                 :method "POST"
                 :body {:q "knoxx" :k 1 :project "devel"}}]
               @requests*))
        (is (= "knoxx" (:query result)))
        (is (= "docs/knoxx.md" (:path first-result)))
        (is (= 0.75 (:score first-result)))
        (is (= 3 (:chunkCount first-result)))
        (is (re-find #"semantic match" (:snippet first-result))))
      (finally
        (aset js/globalThis "fetch" original-fetch)))))
