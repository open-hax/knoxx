(ns knoxx.backend.mongo-client-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.infra.mongo-client :as mongo-client]))

(deftest redacted-mongo-location-removes-credentials-and-options
  (testing "startup logs retain only the useful local endpoint"
    (is (= "mongodb://127.0.0.1:27017/openplanner"
           (mongo-client/redacted-mongo-location
            "mongodb://app-user:secret@127.0.0.1:27017/openplanner?authSource=openplanner&directConnection=true"))))
  (testing "credential-free URLs remain useful while options stay private"
    (is (= "mongodb://localhost:27017"
           (mongo-client/redacted-mongo-location
            "mongodb://localhost:27017?directConnection=true")))))

(deftest redact-mongo-credentials-in-diagnostics
  (testing "standard and SRV credentials are scrubbed wherever they appear"
    (is (= "failed mongodb://host/db then mongodb+srv://cluster/db"
           (mongo-client/redact-mongo-credentials
            (str "failed mongodb://app-user:secret@host/db then "
                 "mongodb+srv://cloud-user:cloud-secret@cluster/db"))))))
