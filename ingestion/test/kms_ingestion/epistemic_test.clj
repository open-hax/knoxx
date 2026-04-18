(ns kms-ingestion.epistemic-test
  "Unit tests for the epistemic primitives (fact/obs/inference/attestation/judgment)
   and the promptdb filesystem driver."
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [kms-ingestion.epistemic :as ep]
            [kms-ingestion.drivers.promptdb :as pdb]
            [babashka.fs :as fs]
            [clojure.java.io :as io]))

;; ---------------------------------------------------------------------------
;; Epistemic schema tests
;; ---------------------------------------------------------------------------

(deftest fact-schema
  (testing "valid fact"
    (is (ep/valid? {:kind  :fact
                    :ctx   :己
                    :claim "agent x exists"
                    :src   "bootstrap"
                    :p     0.99
                    :time  (java.util.Date.)})))
  (testing "missing :claim is invalid"
    (is (not (ep/valid? {:kind :fact
                         :ctx  :己
                         :src  "bootstrap"
                         :p    0.99
                         :time (java.util.Date.)}))))
  (testing ":p out of range is invalid"
    (is (not (ep/valid? {:kind  :fact
                         :ctx   :己
                         :claim "x"
                         :src   "y"
                         :p     1.5
                         :time  (java.util.Date.)})))))

(deftest obs-schema
  (testing "valid obs"
    (is (ep/valid? {:kind   :obs
                    :ctx    :己
                    :about  "discord-message"
                    :signal {:content "hello"}
                    :p      0.8})))
  (testing "missing :about is invalid"
    (is (not (ep/valid? {:kind   :obs
                         :ctx    :己
                         :signal {}
                         :p      0.8})))))

(deftest inference-schema
  (testing "valid inference"
    (is (ep/valid? {:kind  :inference
                    :from  [{:kind :obs :ctx :己 :about "x" :signal {} :p 0.9}]
                    :rule  :contract/discord-patrol
                    :actor :actor/discord-patrol
                    :claim "message is on-topic"
                    :p     0.85}))))

(deftest attestation-schema
  (testing "valid attestation"
    (is (ep/valid? {:kind      :attestation
                    :actor     :actor/discord-patrol
                    :did       "sent-reply"
                    :run-id    (random-uuid)
                    :causedby  (random-uuid)
                    :p         0.95}))))

(deftest judgment-schema
  (testing "valid judgment :held"
    (is (ep/valid? {:kind    :judgment
                    :of      (random-uuid)
                    :verdict :held
                    :auditor :system/fulfillment
                    :p       0.99})))
  (testing "invalid verdict value"
    (is (not (ep/valid? {:kind    :judgment
                         :of      (random-uuid)
                         :verdict :unknown
                         :auditor :system/fulfillment
                         :p       0.99})))))

(deftest actor-binding-schema
  (testing "valid actor binding"
    (is (ep/valid? {:kind        :actor-binding
                    :actor/id    "agent.discord-patrol"
                    :actor/org   "open-hax"
                    :actor/role  :agent/discord-patrol
                    :actor/status :active
                    :contract-id "discord-patrol"}))))

;; ---------------------------------------------------------------------------
;; promptdb driver tests
;; ---------------------------------------------------------------------------

(defn- write-edn! [path data]
  (spit path (pr-str data)))

(deftest promptdb-driver-single-record
  (testing "driver extracts a single valid fact from an EDN file"
    (let [tmp (fs/create-temp-dir)]
      (try
        (write-edn! (str tmp "/fact1.edn")
                    {:kind  :fact
                     :ctx   :己
                     :claim "test claim"
                     :src   "unit-test"
                     :p     0.9
                     :time  (java.util.Date.)})
        (let [driver  (pdb/->PromptdbDriver {:path (str tmp)})
              result  (pdb/extract driver {})]
          (is (= 1 (count (:epistemic-records result))))
          (is (= :fact (-> result :epistemic-records first :kind))))
        (finally
          (fs/delete-tree tmp))))))

(deftest promptdb-driver-vector-of-records
  (testing "driver extracts a vector of records from one EDN file"
    (let [tmp (fs/create-temp-dir)]
      (try
        (write-edn! (str tmp "/bundle.edn")
                    [{:kind  :fact
                      :ctx   :己
                      :claim "claim a"
                      :src   "test"
                      :p     1.0
                      :time  (java.util.Date.)}
                     {:kind   :obs
                      :ctx    :己
                      :about  "something"
                      :signal {:raw "x"}
                      :p      0.7}])
        (let [driver (pdb/->PromptdbDriver {:path (str tmp)})
              result (pdb/extract driver {})]
          (is (= 2 (count (:epistemic-records result)))))
        (finally
          (fs/delete-tree tmp))))))

(deftest promptdb-driver-skips-invalid
  (testing "driver skips EDN files with invalid records and logs them"
    (let [tmp (fs/create-temp-dir)]
      (try
        ;; valid
        (write-edn! (str tmp "/good.edn")
                    {:kind  :fact
                     :ctx   :己
                     :claim "good"
                     :src   "test"
                     :p     0.5
                     :time  (java.util.Date.)})
        ;; invalid – missing :claim
        (write-edn! (str tmp "/bad.edn")
                    {:kind :fact
                     :ctx  :己
                     :src  "test"
                     :p    0.5
                     :time (java.util.Date.)})
        (let [driver (pdb/->PromptdbDriver {:path (str tmp)})
              result (pdb/extract driver {})]
          ;; only the valid record should come through
          (is (= 1 (count (:epistemic-records result)))))
        (finally
          (fs/delete-tree tmp))))))
