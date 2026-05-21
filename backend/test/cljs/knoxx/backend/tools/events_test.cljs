(ns knoxx.backend.tools.events-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.clients.knoxx-control :as knoxx-client]))

(defn- assert-valid-control-headers
  [headers]
  (is (= "application/json" (get headers "Content-Type")))
  (is (= "system-admin@open-hax.local" (get headers "x-knoxx-user-email")))
  (is (= "test-key" (get headers "X-API-Key")))
  (when (exists? js/Headers)
    (let [h (js/Headers. (clj->js headers))]
      (is (= "application/json" (.get h "Content-Type")))
      (is (= "test-key" (.get h "X-API-Key"))))))

(deftest knoxx-control-client-headers-remain-fetch-compatible-with-api-key
  (testing "actor/event tools use the Knoxx control client header contract"
    (assert-valid-control-headers (knoxx-client/headers-for {:knoxx-api-key "test-key"}))))
