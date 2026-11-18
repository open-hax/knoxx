(ns big-pi.optimizely.knoxx-contract-test
  (:require [cljs.test :refer [deftest is async]]
            [promesa.core :as p]
            [big-pi.optimizely.bridge.cms :as bridge]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.law.cms-document :as law]))

(def source {:key "item-1" :version "1" :locale "en" :displayName "Technical"
             :properties {:Body "Original" "a/b" 1 "c/b" 2 "free form" "value" "" nil
                          :nested {"a/b" nil "free form" [nil "ok"]}}})

(defn- wire [body]
  (let [reply #js {}]
    (aset reply "code" (fn [_] reply))
    (aset reply "type" (fn [_] reply))
    (aset reply "send" (fn [encoded] encoded))
    (js->clj (fastify/send-json! reply 200 body) :keywordize-keys true)))

(deftest actual-knoxx-codec-preserves-source-properties
  (async done
    (let [saved (atom nil) calls (atom [])
          vendor (fn [req]
                   (swap! calls conj req)
                   (p/resolved (if (= :get (:method req))
                                 {:status 200 :headers {"etag" "\"1\"" "x-big-pi-source-id" "store-a"} :body source}
                                 {:status 201 :headers {} :body (merge (:body req) {:key "item-1" :version "2" :status "draft"})})))
          knoxx (fn [req]
                  (if (= :post (:method req))
                    (let [incoming (fastify/request-body #js {:body (clj->js (:body req))})
                          checked (law/require-body! incoming)
                          doc (merge checked {:doc_id "doc1" :revision "r1" :revision_heads ["r1"] :conflicted false})]
                      (reset! saved doc)
                      (p/resolved {:status 200 :headers {} :body (wire doc)}))
                    (let [doc (assoc @saved :content "Übersetzt")]
                      (p/resolved {:status 200 :headers {} :body (wire {:document doc :revisions [doc]})}))))
          clients {:optimizely vendor :knoxx knoxx}]
      (-> (bridge/import! clients {:key "item-1" :version "1" :field "Body" :source-system "local"})
          (p/then (fn [_] (bridge/export! clients {:document-id "doc1" :revision "r1" :target-locale "de" :field "Body"})))
          (p/then (fn [result]
                    (is (= "2" (get-in result [:vendor :version])))
                    (is (= (assoc (:properties source) :Body "Übersetzt") (get-in (last @calls) [:body :properties])))
                    (done)))
          (p/catch (fn [error] (is false (str error)) (done)))))))
