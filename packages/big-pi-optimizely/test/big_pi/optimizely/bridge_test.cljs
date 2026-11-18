(ns big-pi.optimizely.bridge-test
  (:require [cljs.test :refer [deftest is async]]
            [promesa.core :as p]
            [big-pi.optimizely.bridge.cms :as bridge]))

(def source {:key "article-1" :version "v1" :locale "en" :displayName "Title"
             :properties {:Body "Original" :Count 7}})
(def provenance {:source-system "local" :source-id "store-a" :key "article-1" :version "v1" :field "Body"
                 :locale "en" :etag "\"source-1\"" :payload-format "big-pi-edn-v1" :payload-edn (pr-str source)})
(def reviewed {:doc_id "doc1" :revision "r1" :title "Title" :content "Übersetzt"
               :metadata {:optimizely provenance}})
(def history {:document (assoc reviewed :revision_heads ["r1"] :conflicted false)
              :revisions [reviewed]})
(defn clients [calls vendor-response history-response]
  {:optimizely (fn [req] (swap! calls conj [:vendor req])
                 (p/resolved (if (= :get (:method req)) vendor-response
                                 {:status 201 :headers {} :body {:key "article-1" :version "v2" :locale "de" :status "draft" :properties (get-in req [:body :properties])}})))
   :knoxx (fn [req] (swap! calls conj [:knoxx req])
            (p/resolved (if (= :get (:method req)) history-response
                            {:status 200 :headers {} :body (merge (select-keys (:body req) [:title :content :metadata :visibility]) {:doc_id "doc1" :revision "r1" :revision_heads ["r1"] :conflicted false})})))})
(def vendor {:status 200 :headers {"etag" "\"source-1\"" "x-big-pi-source-id" "store-a"} :body source})
(def import-request {:key "article-1" :version "v1" :field "Body" :source-system "local"})
(def export-request {:document-id "doc1" :revision "r1" :target-locale "de" :field "Body"})

(deftest import-exact-source-and-provenance
  (async done
    (let [calls (atom [])]
      (-> (bridge/import! (clients calls vendor nil) import-request)
          (p/then (fn [result]
                    (is (= "r1" (get-in result [:document :revision])))
                    (is (= "/v1/content/article-1/versions/v1" (get-in @calls [0 1 :path])))
                    (is (= provenance (get-in @calls [1 1 :body :metadata :optimizely])))
                    (is (= "internal" (get-in @calls [1 1 :body :visibility])))
                    (is (= (pr-str source) (get-in @calls [1 1 :body :metadata :optimizely :payload-edn])))
                    (done)))
          (p/catch (fn [e] (is false (str e)) (done)))))))

(deftest draft-export-only
  (async done
    (let [calls (atom [])]
      (-> (bridge/export! (clients calls vendor {:status 200 :body history}) export-request)
          (p/then (fn [result]
                    (is (= "v2" (get-in result [:vendor :version])))
                    (is (= "store-a" (get-in result [:source :source-id])))
                    (let [request (second (last @calls))]
                      (is (= :post (:method request)))
                      (is (= "/v1/content/article-1/versions" (:path request)))
                      (is (= "de" (get-in request [:body :locale])))
                      (is (= "Übersetzt" (get-in request [:body :properties :Body])))
                      (is (= 7 (get-in request [:body :properties :Count])))
                      (is (not (contains? (:body request) :metadata))))
                    (is (not-any? #(re-find #"(?i)publish" (get-in % [1 :path])) @calls))
                    (done)))
          (p/catch (fn [e] (is false (str e)) (done)))))))

(deftest refuse-unsafe-export
  (async done
    (let [cases [{:history (assoc-in history [:document :revision_heads] ["r1" "r2"]) :vendor vendor}
                 {:history (assoc history :revisions []) :vendor vendor}
                 {:history (assoc-in history [:revisions 0 :doc_id] "other") :vendor vendor}
                 {:history (update-in history [:revisions 0] dissoc :metadata) :vendor vendor}
                 {:history history :vendor (assoc-in vendor [:body :properties :Body] "mutated")}
                 {:history history :vendor (assoc-in vendor [:headers "etag"] "\"changed\"")}]]
      (-> (reduce (fn [pending {:keys [history vendor]}]
                    (p/then pending (fn [_]
                      (let [calls (atom [])]
                        (-> (bridge/export! (clients calls vendor {:status 200 :body history}) export-request)
                            (p/then (fn [_] (is false "should reject")))
                            (p/catch (fn [_] (is (not-any? #(= :post (get-in % [1 :method])) @calls)))))))))
                  (p/resolved nil) cases)
          (p/finally done)))))

(deftest import-rejects-structured-property
  (async done
    (let [calls (atom []) bad (assoc-in vendor [:body :properties :Body] {:html "unsafe"})]
      (-> (bridge/import! (clients calls bad nil) import-request)
          (p/then (fn [_] (is false "must refuse structured properties") (done)))
          (p/catch (fn [_] (is (= 1 (count @calls))) (done)))))))

(deftest ambiguous-write-never-retries
  (async done
    (let [calls (atom [])
          base (clients calls vendor {:status 200 :body history})
          clients (assoc base :optimizely
                         (fn [request]
                           (if (= :post (:method request))
                             (do (swap! calls conj [:vendor request])
                                 (p/rejected (ex-info "connection lost" {})))
                             ((:optimizely base) request))))]
      (-> (bridge/export! clients export-request)
          (p/then (fn [_] (is false "uncertain write cannot succeed") (done)))
          (p/catch (fn [error]
                     (is (= :outcome-uncertain (:code (ex-data (or (ex-cause error) error)))))
                     (is (= 1 (count (filter #(= :post (get-in % [1 :method])) @calls))))
                     (done)))))))

(deftest reject-missing-etag-and-source-mismatch
  (async done
    (-> (reduce (fn [pending bad]
                  (p/then pending (fn [_]
                    (let [calls (atom [])]
                      (-> (bridge/import! (clients calls bad nil) import-request)
                          (p/then (fn [_] (is false "must reject source")))
                          (p/catch (fn [_] (is (= 1 (count @calls))))))))))
                (p/resolved nil)
                [(assoc vendor :headers {}) (assoc-in vendor [:body :version] "v2")])
        (p/finally done))))

(deftest rejects-intervening-head-before-export
  (async done
    (let [calls (atom []) reads (atom 0)
          base (clients calls vendor {:status 200 :body history})
          c (assoc base :knoxx
                   (fn [request]
                     (swap! reads inc)
                     (if (= 1 @reads)
                       ((:knoxx base) request)
                       (p/resolved {:status 200 :body (assoc-in history [:document :revision_heads] ["r1" "r2"])}))))]
      (-> (bridge/export! c export-request)
          (p/then (fn [_] (is false "intervening head must reject") (done)))
          (p/catch (fn [_] (is (not-any? #(= :post (get-in % [1 :method])) @calls)) (done)))))))

(deftest updating-requires-observed-revision-and-preserves-metadata
  (async done
    (let [calls (atom [])
          old (assoc-in history [:document :metadata] {:existing "keep"})
          c (clients calls vendor {:status 200 :body old})]
      (-> (bridge/import! c (assoc import-request :document-id "doc1" :expected-revision "r1"))
          (p/then (fn [_]
                    (let [request (second (last @calls))]
                      (is (= :patch (:method request)))
                      (is (= ["r1"] (get-in request [:body :parents])))
                      (is (= "keep" (get-in request [:body :metadata :existing])))
                      (is (not-any? #(re-find #"(?i)publish" (get-in % [1 :path])) @calls)))
                    (done)))
          (p/catch (fn [e] (is false (str e)) (done)))))))

(deftest update-stale-or-unknown-fields-refused
  (async done
    (-> (p/all
         (map (fn [req]
                (let [calls (atom [])]
                  (-> (bridge/import! (clients calls vendor {:status 200 :body history}) req)
                      (p/then (fn [_] (is false "must reject request")))
                      (p/catch (fn [_] (is (not-any? #(#{:post :patch} (get-in % [1 :method])) @calls)))))))
              [(assoc import-request :document-id "doc1")
               (assoc import-request :document-id "doc1" :expected-revision "stale")
               (assoc import-request :status "public")]))
        (p/finally done))))

(deftest ambiguous-cms-write-never-retries
  (async done
    (let [calls (atom [])
          base (clients calls vendor nil)
          c (assoc base :knoxx (fn [req] (swap! calls conj [:knoxx req])
                                 (p/rejected (ex-info "connection lost" {}))))]
      (-> (bridge/import! c import-request)
          (p/then (fn [_] (is false "must not assume write success") (done)))
          (p/catch (fn [e]
                     (is (= :outcome-uncertain (:code (ex-data (or (ex-cause e) e)))))
                     (is (= 1 (count (filter #(= :post (get-in % [1 :method])) @calls))))
                     (done)))))))

(deftest malformed-cms-success-is-uncertain
  (async done
    (let [calls (atom [])
          c (assoc (clients calls vendor nil) :knoxx
                   (fn [request] (swap! calls conj [:knoxx request])
                     (p/resolved {:status 200 :body {:doc_id "doc1" :revision "r1"}})))]
      (-> (bridge/import! c import-request)
          (p/then (fn [_] (is false "missing output evidence") (done)))
          (p/catch (fn [e]
                     (is (= :outcome-uncertain (:code (ex-data (or (ex-cause e) e)))))
                     (is (= 1 (count (filter #(= :post (get-in % [1 :method])) @calls))))
                     (done)))))))

(deftest rejects-oversized-provenance-before-write
  (async done
    (let [calls (atom []) bad (assoc-in vendor [:body :properties :large] (apply str (repeat 66000 "x")))]
      (-> (bridge/import! (clients calls bad nil) import-request)
          (p/then (fn [_] (is false "oversized metadata") (done)))
          (p/catch (fn [_] (is (not-any? #(= :post (get-in % [1 :method])) @calls)) (done)))))))

(deftest opaque-revision-not-used-as-path
  (async done
    (let [calls (atom []) rev "sha256:abc/def"
          h (-> history (assoc-in [:document :revision] rev)
                (assoc-in [:document :revision_heads] [rev])
                (assoc-in [:revisions 0 :revision] rev))]
      (-> (bridge/export! (clients calls vendor {:status 200 :body h}) (assoc export-request :revision rev))
          (p/then (fn [_] (is (every? #(not (clojure.string/includes? (get-in % [1 :path]) rev)) @calls)) (done)))
          (p/catch (fn [e] (is false (str e)) (done)))))))

(deftest store-identity-must-match
  (async done
    (let [calls (atom [])
          h (assoc-in history [:revisions 0 :metadata :optimizely :source-id] "store-a")
          v (assoc-in vendor [:headers "x-big-pi-source-id"] "store-b")]
      (-> (bridge/export! (clients calls v {:status 200 :body h}) export-request)
          (p/then (fn [_] (is false "different stores must not share source authority")))
          (p/catch (fn [e]
                     (let [data (ex-data (or (ex-cause e) e))]
                       (is (= :source-identity-mismatch (:code data)))
                       (is (= 409 (:status data))))))
          (p/finally (fn []
                       (is (not-any? #(= :post (get-in % [1 :method])) @calls))
                       (done)))))))

(deftest source-identity-is-required
  (async done
    (let [calls (atom [])]
      (-> (bridge/import! (clients calls (update vendor :headers dissoc "x-big-pi-source-id") nil) import-request)
          (p/then (fn [_] (is false "missing source identity must not import")))
          (p/catch (fn [_] (is (not-any? #(= :post (get-in % [1 :method])) @calls))))
          (p/finally done)))))

(deftest legacy-provenance-without-identity-cannot-export
  (async done
    (let [calls (atom [])
          h (update-in history [:revisions 0 :metadata :optimizely] dissoc :source-id)]
      (-> (bridge/export! (clients calls vendor {:status 200 :body h}) export-request)
          (p/then (fn [_] (is false "legacy provenance cannot authorize export")))
          (p/catch (fn [_] (is (not-any? #(= :post (get-in % [1 :method])) @calls))))
          (p/finally done)))))

(deftest rejects-invalid-payload-envelopes
  (async done
    (let [records [(dissoc provenance :payload-format)
                   (assoc provenance :payload-format "unknown")
                   (assoc provenance :payload-edn "{broken")
                   (update provenance :payload-edn str " nil")
                   (assoc provenance :payload-edn (pr-str (assoc source :version "other")))
                   (-> provenance (dissoc :payload-format :payload-edn) (assoc :payload source))]]
      (-> (reduce (fn [pending record]
                    (p/then pending
                            (fn [_]
                              (let [calls (atom [])
                                    h (assoc-in history [:revisions 0 :metadata :optimizely] record)]
                                (-> (bridge/export! (clients calls vendor {:status 200 :body h}) export-request)
                                    (p/then (fn [_] (is false "invalid envelope must be refused")))
                                    (p/catch (fn [_] (is (not-any? #(= :post (get-in % [1 :method])) @calls)))))))))
                  (p/resolved nil) records)
          (p/finally done)))))
