(ns big-pi.optimizely.domain-test
  (:require [big-pi.optimizely.domain.cms :as cms]
            [cljs.test :refer [deftest is]]))

(defn request [state method path & [body headers query actor]]
  (cms/handle state {:method method :path path :body body :headers (or headers {})
                     :query (or query {}) :actor (or actor {})}))

(deftest content-lifecycle
  (let [initial (cms/initial-state)
        created (request initial :post "/v1/content"
                         {:key "article-1" :contentType "Article"
                          :initialVersion {:displayName "Hello" :locale "en"
                                           :properties {:title "Hello" :nested {:a 1 :b 2}}}}
                         {"prefer" "return=representation"})
        state (:state created)
        etag (get-in created [:response :headers "ETag"])
        patched (request state :patch "/v1/content/article-1/versions/1"
                         {:properties {:nested {:a nil :c 3} :tags ["x"]}}
                         {"if-match" etag "content-type" "application/merge-patch+json"
                          "prefer" "return=representation"})]
    (is (= 201 (get-in created [:response :status])))
    (is (= "draft" (get-in created [:response :body :status])))
    (is (= 200 (get-in patched [:response :status])))
    (is (= {:b 2 :c 3} (get-in patched [:response :body :properties :nested])))
    (is (= 412 (get-in (request (:state patched) :patch "/v1/content/article-1/versions/1"
                                {:displayName "stale"} {"if-match" etag}) [:response :status])))
    (is (= 304 (get-in (request state :get "/v1/content/article-1/versions/1" nil
                                {"if-none-match" etag}) [:response :status])))
    (is (= 403 (get-in (request state :patch "/v1/content/article-1/versions/1"
                                {:status "published"}) [:response :status])))
    (is (seq (:history (:state patched))))))

(deftest locale-versions-and-validation
  (let [created (request (cms/initial-state) :post "/v1/content"
                         {:key "a" :contentType "Article"
                          :initialVersion {:displayName "English" :locale "en" :properties {}}})
        state (:state created)
        translated (request state :post "/v1/content/a/versions"
                            {:displayName "Deutsch" :locale "de" :properties {:title "Hallo"}}
                            {"prefer" "return=representation"})]
    (is (= 201 (get-in translated [:response :status])))
    (is (= "de" (get-in translated [:response :body :locale])))
    (is (= 2 (count (get-in (request (:state translated) :get "/v1/content/a/versions") [:response :body :items]))))
    (is (= 400 (get-in (request state :post "/v1/content" {:unknown true}) [:response :status])))
    (is (= 400 (get-in (request state :get "/v1/content" nil nil {"pageSize" "0"}) [:response :status])))
    (is (= 501 (get-in (request state :get "/v1/media") [:response :status])))
    (is (= 400 (get-in (request state :get "/v1/content/../x") [:response :status])))))

(deftest resource-contracts
  (let [initial (cms/initial-state)
        created (request initial :post "/v1/contenttypes" {:key "News" :displayName "News" :properties {}})
        state (:state created)
        fetched (request state :get "/v1/contenttypes/News")
        tag (get-in fetched [:response :headers "ETag"])]
    (is (= 201 (get-in created [:response :status])))
    (is (nil? (get-in created [:response :body])))
    (is (= "/v1/contenttypes/News" (get-in created [:response :headers "Location"])))
    (is (= 409 (get-in (request state :post "/v1/contenttypes" {:key "News" :displayName "News" :properties {}}) [:response :status])))
    (is (= 200 (get-in (request state :patch "/v1/contenttypes/News" {:displayName "Updated"}
                                {"if-match" tag "prefer" "return=representation"}) [:response :status])))
    (is (= 404 (get-in (request state :patch "/v1/locales/missing" {:displayName "Missing"}) [:response :status])))
    (is (= 415 (get-in (request state :patch "/v1/locales/en" {:displayName "English"}
                                {"content-type" "application/json"}) [:response :status])))
    (is (= #{"en" "de" "es"} (set (map :key (get-in (request state :get "/v1/locales") [:response :body :items])))))))

(deftest draft-boundaries
  (let [created (request (cms/initial-state) :post "/v1/content"
                         {:key "a" :contentType "Article"
                          :initialVersion {:displayName "English" :locale "en" :properties {:list [1 2]}}})
        state (:state created)
        patched (request state :patch "/v1/content/a/versions/1"
                         {:properties {:list [3]}} {"prefer" "return=representation"})]
    (is (= [3] (get-in patched [:response :body :properties :list])))
    (is (= [1 2] (get-in state [:content "a" :versions "1" :properties :list])))
    (is (= 400 (get-in (request state :patch "/v1/content/a/versions/1" {:locale nil}) [:response :status])))
    (is (= 400 (get-in (request state :patch "/v1/content/a/versions/1" {:bogus true}) [:response :status])))
    (is (= 403 (get-in (request state :patch "/v1/content/a/versions/1" {:status "published"} nil nil {:can-publish? true}) [:response :status])))
    (is (= ["en"] (get-in (request state :get "/v1/content/a") [:response :body :locales])))))

(deftest contract-boundaries
  (let [base (cms/initial-state)
        create (fn [s k] (:state (request s :post "/v1/content"
                                  {:key k :contentType "Article"
                                   :initialVersion {:displayName "Name" :locale "en" :properties {} :status "draft"}})))
        state (reduce create base ["z" "a"])
        versions (reduce (fn [s _] (:state (request s :post "/v1/content/a/versions"
                                                    {:displayName "Version" :locale "de" :properties {}})))
                         state (range 11))
        listing (get-in (request versions :get "/v1/content/a/versions") [:response :body])]
    (is (= ["a" "z"] (mapv :key (get-in (request state :get "/v1/content") [:response :body :items]))))
    (is (= 12 (:totalCount listing)))
    (is (= (mapv str (range 1 13)) (mapv :version (:items listing))))
    (is (= 200 (get-in (request state :get "/v1/content" nil nil {"pageIndex" "10000"}) [:response :status])))
    (is (= 400 (get-in (request state :get "/v1/content" nil nil {"pageIndex" "10001"}) [:response :status])))
    (is (= 400 (get-in (request state :patch "/v1/content/a/versions/1" {:locale "de"}) [:response :status])))
    (is (= 403 (get-in (request state :post "/v1/content/a/versions" {:displayName "V" :locale "en" :properties {} :status "published"}) [:response :status])))
    (is (= 400 (get-in (request state :post "/v1/locales" {:key "invalid_tag" :displayName "Bad"}) [:response :status])))
    (is (= 400 (get-in (request state :patch "/v1/locales/en" {:displayName nil}) [:response :status])))
    (is (= 400 (get-in (request state :patch "/v1/contenttypes/Article" {:properties nil}) [:response :status])))
    (is (= 400 (get-in (request state :post "/v1/content/a/versions" {:displayName "  " :locale "en" :properties {}}) [:response :status])))
    (is (= 400 (get-in (request state :post "/v1/content/a/versions" {:displayName (apply str (repeat 256 "x")) :locale "en" :properties {}}) [:response :status])))
    (is (= 501 (get-in (request state :post "/v1/content" {:contentType "Article" :container "a" :initialVersion {:displayName "X" :locale "en" :properties {}}}) [:response :status])))
    (is (= 400 (get-in (request state :get "/v1/content/%2e%2e/versions") [:response :status])))
    (let [before (get-in (request versions :get "/v1/content/a/versions/1") [:response :headers "ETag"])
          changed (:state (request versions :patch "/v1/content/a/versions/2" {:displayName "Updated"}))]
      (is (= before (get-in (request changed :get "/v1/content/a/versions/1") [:response :headers "ETag"]))))))

(deftest exact-name-and-nested-patch-boundaries
  (let [long-name (apply str (repeat 255 "x"))
        created (request (cms/initial-state) :post "/v1/content"
                         {:key "named" :contentType "Article"
                          :initialVersion {:displayName long-name :locale "en" :status "draft"
                                           :properties {:status "editorial" :nested {:remove "x" :list [1 2]}}}})
        state (:state created)
        updated (request state :patch "/v1/content/named/versions/1"
                         {:properties {:nested {:remove nil :list [3]}}}
                         {"prefer" "return=representation"})]
    (is (= 201 (get-in created [:response :status])))
    (is (= "editorial" (get-in updated [:response :body :properties :status])))
    (is (= {:list [3]} (get-in updated [:response :body :properties :nested])))
    (is (= 400 (get-in (request state :get "/v1/content" nil nil {"pageSize" "10001"}) [:response :status])))
    (is (= 400 (get-in (request state :post "/v1/contenttypes" {:key "Broken" :displayName "Broken" :properties []}) [:response :status])))))

(deftest generated-content-keys-skip-explicit-keys
  (let [body {:contentType "Article" :initialVersion {:displayName "Name" :locale "en" :properties {}}}
        explicit-2 (request (cms/initial-state) :post "/v1/content" (assoc body :key "content-2"))
        single-auto (request (:state explicit-2) :post "/v1/content" body {"prefer" "return=representation"})
        explicit-3 (request (cms/initial-state) :post "/v1/content" (assoc body :key "content-3"))
        explicit-4 (request (:state explicit-3) :post "/v1/content" (assoc body :key "content-4"))
        first-auto (request (:state explicit-4) :post "/v1/content" body {"prefer" "return=representation"})
        second-auto (request (:state first-auto) :post "/v1/content" body {"prefer" "return=representation"})
        duplicate (request (:state second-auto) :post "/v1/content" (assoc body :key "content-3"))]
    (is (= 201 (get-in single-auto [:response :status])))
    (is (= "content-3" (get-in single-auto [:response :body :key])))
    (is (= 201 (get-in first-auto [:response :status])))
    (is (= "content-5" (get-in first-auto [:response :body :key])))
    (is (= 201 (get-in second-auto [:response :status])))
    (is (= "content-6" (get-in second-auto [:response :body :key])))
    (is (= 409 (get-in duplicate [:response :status])))
    (is (= #{"content-3" "content-4" "content-5" "content-6"}
           (set (keys (:content (:state second-auto))))))))
