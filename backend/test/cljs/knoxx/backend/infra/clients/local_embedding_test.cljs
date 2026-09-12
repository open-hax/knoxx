(ns knoxx.backend.infra.clients.local-embedding-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.infra.clients.local-embedding :as sut]
            [knoxx.backend.law.local-embedding :as law]))

(def ^:private config {:embed-provider-base-url "http://127.0.0.1:4333/v1"
                      :embed-provider-model "local-embedding" :embed-provider-dimensions 2})
(def ^:private response {:ok true :status 200
                        :body {:model "local-embedding"
                               :data [{:index 1 :embedding [0 1]} {:index 0 :embedding [1 0]}]}})

(deftest response-indexes-are-authoritative-and-aligned
  (is (= {:model "local-embedding" :dimensions 2 :vectors [[1 0] [0 1]]}
         (law/checked-result! "local-embedding" 2 2 response)))
  (doseq [bad [(assoc-in response [:body :model] "other")
               (assoc-in response [:body :data 0 :index] 0)
               (assoc-in response [:body :data 0 :index] 2)
               (assoc-in response [:body :data 0 :embedding] [1])
               (assoc-in response [:body :data 0 :embedding] [##NaN 0])
               (assoc-in response [:body :data 0 :embedding] [##Inf 0])
               (assoc response :status 500)]]
    (is (thrown? cljs.core/ExceptionInfo (law/checked-result! "local-embedding" 2 2 bad)))))

(deftest ^:async exact-request-and-no-implicit-fallback
  (let [seen (atom nil)]
    (with-redefs [xfetch/default-client (reify xfetch/IHttpClient
                                       (json! [_ request] (reset! seen request) response)
                                       (response! [_ _] (throw (ex-info "Unseeded HTTP fixture method" {:method :response!})))
                                       (text! [_ _] (throw (ex-info "Unseeded HTTP fixture method" {:method :text!})))
                                       (array-buffer! [_ _] (throw (ex-info "Unseeded HTTP fixture method" {:method :array-buffer!}))))]
      (is (= [[1 0] [0 1]] (:vectors (await (sut/embed! config ["First" "Second"]))))))
    (is (= {:model "local-embedding" :input ["First" "Second"] :dimensions 2 :encoding_format "float"}
           (get-in @seen [:opts :json]))))
  (doseq [[cfg texts status] [[{} ["Hello"] 503] [config [] 400]
                              [(assoc config :embed-provider-base-url "https://external.example/v1") ["Hello"] 503]]]
    (try (await (sut/embed! cfg texts)) (is false "Invalid configuration unexpectedly embedded")
         (catch :default error (is (= status (:status (ex-data error))))))))
