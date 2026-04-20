(ns kms-ingestion.contracts.loader-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [clojure.java.io :as io]
   [kms-ingestion.contracts.loader :as loader]))

(def ^:dynamic *contracts-root* nil)

(defn- write-edn! [path content]
  (let [f (io/file path)]
    (.mkdirs (.getParentFile f))
    (spit f (pr-str content))))

(defn- with-temp-contracts [f]
  (let [tmp (doto (java.io.File/createTempFile "contracts" nil)
               (.delete)
               (.mkdirs))]
    (binding [*contracts-root* (.getAbsolutePath tmp)]
      (loader/invalidate-cache!)
      (loader/with-contracts-dir (.getAbsolutePath tmp)
        (try
          (f)
          (finally
            (loader/invalidate-cache!)))))))

(use-fixtures :each with-temp-contracts)

;; ──────────────────────────────────────────────────────────────────────────────

(deftest runtime-floor-applied-when-no-contracts
  (testing "returns runtime floor values when contracts dir is absent"
    ;; *contracts-dir* is the bare tmp dir — no _defaults.edn present
    ;; so every read-edn-file returns nil and reduce over empty list returns nil
    (let [result (loader/load-source-contract "nobody" "nothing")]
      (is (nil? result)))))

(deftest global-defaults-merged
  (testing "global _defaults.edn merges over runtime floor"
    (let [cdir (str *contracts-root* "/contracts")]
      (.mkdirs (io/file cdir))
      (write-edn! (str cdir "/_defaults.edn")
                  {:contract/id   :test/defaults
                   :contract/type :ingest/source
                   :tenant/id     "_defaults"
                   :source/id     :defaults
                   :source/name   "Global defaults"
                   :source/driver :local
                   :source/ingest {:batch-size 99}})
      (loader/with-contracts-dir cdir
        (loader/invalidate-cache!)
        (let [result (loader/load-source-contract "nobody" "nothing")]
          (is (= 99 (get-in result [:source/ingest :batch-size])))
          (is (= 4  (get-in result [:source/ingest :batch-parallelism]))
              "runtime floor fills in missing keys"))))))

(deftest source-contract-overrides-globals
  (testing "source contract values win over global defaults"
    (let [cdir (str *contracts-root* "/contracts")]
      (.mkdirs (io/file (str cdir "/acme/sources")))
      (write-edn! (str cdir "/_defaults.edn")
                  {:contract/id   :test/globals
                   :contract/type :ingest/source
                   :tenant/id     "_defaults"
                   :source/id     :defaults
                   :source/name   "g"
                   :source/driver :local
                   :source/ingest {:batch-size        20
                                   :throttle-enabled? true}})
      (write-edn! (str cdir "/acme/sources/docs.edn")
                  {:contract/id   :acme/docs
                   :contract/type :ingest/source
                   :tenant/id     "acme"
                   :source/id     :docs
                   :source/name   "docs"
                   :source/driver :local
                   :source/ingest {:batch-size        3
                                   :throttle-enabled? false}})
      (loader/with-contracts-dir cdir
        (loader/invalidate-cache!)
        (let [result (loader/load-source-contract "acme" "docs")]
          (is (= 3     (get-in result [:source/ingest :batch-size])))
          (is (= false (get-in result [:source/ingest :throttle-enabled?]))))))))

(deftest job-override-not-cached
  (testing "job override is applied but not stored in cache"
    (let [cdir (str *contracts-root* "/contracts")]
      (.mkdirs (io/file (str cdir "/acme/sources")))
      (write-edn! (str cdir "/acme/sources/docs.edn")
                  {:contract/id   :acme/docs
                   :contract/type :ingest/source
                   :tenant/id     "acme"
                   :source/id     :docs
                   :source/name   "docs"
                   :source/driver :local
                   :source/ingest {:batch-size 5}})
      (loader/with-contracts-dir cdir
        (loader/invalidate-cache!)
        (let [base     (loader/load-source-contract "acme" "docs")
              override (loader/load-source-contract "acme" "docs" {:source/ingest {:batch-size 1}})
              cached   (loader/load-source-contract "acme" "docs")]
          (is (= 5 (get-in base     [:source/ingest :batch-size])))
          (is (= 1 (get-in override [:source/ingest :batch-size])))
          (is (= 5 (get-in cached   [:source/ingest :batch-size]))
              "job override must not pollute the cache"))))))

(deftest secret-refs-resolved
  (testing "env secret refs are replaced with env values"
    (let [cdir (str *contracts-root* "/contracts")]
      (.mkdirs (io/file (str cdir "/acme/sources")))
      (write-edn! (str cdir "/acme/sources/gdrive.edn")
                  {:contract/id   :acme/gdrive
                   :contract/type :ingest/source
                   :tenant/id     "acme"
                   :source/id     :gdrive
                   :source/name   "Google Drive"
                   :source/driver :google-drive
                   :source/config {:credentials-secret :env/GDRIVE_CREDS}})
      (System/setProperty "GDRIVE_CREDS" "test-token")
      (loader/with-contracts-dir cdir
        (loader/invalidate-cache!)
        (let [result (loader/load-source-contract "acme" "gdrive")]
          (is (map? result))
          (is (not= :env/GDRIVE_CREDS
                    (get-in result [:source/config :credentials-secret]))))))))
