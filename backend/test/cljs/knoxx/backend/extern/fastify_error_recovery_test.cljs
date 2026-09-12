(ns knoxx.backend.extern.fastify-error-recovery-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.fastify :as fastify]))

(deftest service-refusals-retain-http-classification
  (let [denied (ex-info "Authentication required" {:status 401 :code "identity_required"})]
    (is (= 401 (fastify/error-status denied)))
    (is (= "identity_required" (fastify/error-code denied))))
  (doseq [invalid [200 399 600 "401" false nil]]
    (is (= 500 (fastify/error-status (ex-info "Invalid" {:status invalid})))))
  (is (= 429 (fastify/error-status (js-obj "statusCode" 429) nil))))
