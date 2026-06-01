(ns kms-ingestion.drivers.github-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [kms-ingestion.drivers.github :as github]
   [kms-ingestion.drivers.protocol :as protocol]))

;; ─── URL building ─────────────────────────────────────────────────────────

(deftest url-building-test
  (testing "org repos url"
    (is (= "https://api.github.com/orgs/open-hax/repos"
           (github/org-repos-url github/default-api-base "open-hax"))))

  (testing "repo url"
    (is (= "https://api.github.com/repos/open-hax/knoxx"
           (github/repo-url github/default-api-base "open-hax" "knoxx"))))

  (testing "tree url is recursive"
    (is (= "https://api.github.com/repos/open-hax/knoxx/git/trees/main?recursive=1"
           (github/tree-url github/default-api-base "open-hax" "knoxx" "main"))))

  (testing "contents url"
    (is (= "https://api.github.com/repos/open-hax/knoxx/contents/src/core.clj"
           (github/contents-url github/default-api-base "open-hax" "knoxx" "src/core.clj"))))

  (testing "issues + pulls urls"
    (is (= "https://api.github.com/repos/open-hax/knoxx/issues"
           (github/issues-url github/default-api-base "open-hax" "knoxx")))
    (is (= "https://api.github.com/repos/open-hax/knoxx/pulls"
           (github/pulls-url github/default-api-base "open-hax" "knoxx")))))

;; ─── File-id encoding / parsing ───────────────────────────────────────────

(deftest file-id-roundtrip-test
  (testing "blob id roundtrips"
    (let [id (github/blob-id "open-hax" "knoxx" "abc123" "src/core.clj")
          parsed (github/parse-file-id id)]
      (is (= "blob:open-hax/knoxx@abc123:src/core.clj" id))
      (is (= :blob (:kind parsed)))
      (is (= "open-hax" (:owner parsed)))
      (is (= "knoxx" (:repo parsed)))
      (is (= "abc123" (:sha parsed)))
      (is (= "src/core.clj" (:path parsed)))))

  (testing "issue id roundtrips"
    (let [id (github/issue-id "open-hax" "knoxx" 42)
          parsed (github/parse-file-id id)]
      (is (= "issue:open-hax/knoxx#42" id))
      (is (= :issue (:kind parsed)))
      (is (= "42" (:number parsed)))))

  (testing "pr id roundtrips"
    (let [id (github/pr-id "open-hax" "knoxx" 7)
          parsed (github/parse-file-id id)]
      (is (= "pr:open-hax/knoxx#7" id))
      (is (= :pr (:kind parsed)))
      (is (= "7" (:number parsed)))))

  (testing "blob path containing a colon still parses"
    (let [id (github/blob-id "o" "r" "sha" "a/b:c.md")
          parsed (github/parse-file-id id)]
      (is (= "a/b:c.md" (:path parsed)))))

  (testing "unparseable id returns nil"
    (is (nil? (github/parse-file-id "garbage")))))

;; ─── Auth headers ───────────────────────────────────────────────────────────

(deftest auth-headers-test
  (testing "includes token when present"
    (let [h (github/auth-headers "ghp_secret")]
      (is (= "Bearer ghp_secret" (get h "Authorization")))
      (is (= "application/vnd.github+json" (get h "Accept")))
      (is (some? (get h "X-GitHub-Api-Version")))))

  (testing "omits Authorization when token is blank"
    (is (nil? (get (github/auth-headers "") "Authorization")))
    (is (nil? (get (github/auth-headers nil) "Authorization")))))

;; ─── Rate-limit handling ──────────────────────────────────────────────────

(deftest rate-limited?-test
  (testing "429 is rate limited"
    (is (true? (github/rate-limited? {:status 429 :headers {}}))))

  (testing "403 with remaining 0 is rate limited"
    (is (true? (github/rate-limited?
                {:status 403 :headers {"X-RateLimit-Remaining" "0"}}))))

  (testing "403 with Retry-After is rate limited"
    (is (true? (github/rate-limited?
                {:status 403 :headers {"Retry-After" "30"}}))))

  (testing "200 is not rate limited"
    (is (false? (github/rate-limited? {:status 200 :headers {}}))))

  (testing "403 with remaining left is not rate limited"
    (is (false? (github/rate-limited?
                 {:status 403 :headers {"X-RateLimit-Remaining" "100"}})))))

(deftest retry-after-ms-test
  (testing "prefers Retry-After seconds"
    (is (= 30000 (github/retry-after-ms {:headers {"Retry-After" "30"}} 0))))

  (testing "uses RateLimit-Reset when no Retry-After"
    (let [future-secs (+ 10 (quot (System/currentTimeMillis) 1000))
          ms (github/retry-after-ms
              {:headers {"X-RateLimit-Reset" (str future-secs)}} 0)]
      (is (> ms 0))
      (is (<= ms 11000))))

  (testing "falls back to exponential backoff"
    (let [ms0 (github/retry-after-ms {:headers {}} 0)
          ms2 (github/retry-after-ms {:headers {}} 2)]
      (is (= 1000 ms0))
      (is (= 4000 ms2)))))

;; ─── Config validation ────────────────────────────────────────────────────

(deftest validate-config-test
  (testing "valid config"
    (is (:valid (github/validate-config {:org "open-hax" :token "ghp_x"}))))

  (testing "missing org"
    (let [r (github/validate-config {:token "ghp_x"})]
      (is (false? (:valid r)))
      (is (re-find #"org" (:error r)))))

  (testing "missing token"
    (let [r (github/validate-config {:org "open-hax"})]
      (is (false? (:valid r)))
      (is (re-find #"token" (:error r))))))

;; ─── File-type matching ────────────────────────────────────────────────────

(deftest matches-file-type?-test
  (testing "matches configured suffixes"
    (is (true? (github/matches-file-type? "src/core.clj" [".clj" ".md"])))
    (is (true? (github/matches-file-type? "README.MD" [".md"])))
    (is (false? (github/matches-file-type? "bin/data.png" [".clj" ".md"]))))

  (testing "empty list matches everything"
    (is (true? (github/matches-file-type? "anything.xyz" [])))))

;; ─── Classify ──────────────────────────────────────────────────────────────

(deftest classify-test
  (let [files [{:id "a" :content-hash "h1"}
               {:id "b" :content-hash "h2"}
               {:id "c" :content-hash "h3"}]
        existing {"a" "h1"   ;; unchanged
                  "b" "old"} ;; changed
        result (github/classify files existing)
        by-id (into {} (map (juxt :id :status) result))]
    (testing "classifies new/changed/unchanged"
      (is (= :unchanged (get by-id "a")))
      (is (= :changed (get by-id "b")))
      (is (= :new (get by-id "c"))))))

;; ─── Driver protocol ─────────────────────────────────────────────────────────

(deftest driver-creation-test
  (testing "creates a driver satisfying the protocol"
    (let [driver (github/create-driver {:org "open-hax" :token "ghp_x"})]
      (is (some? driver))
      (is (satisfies? protocol/Driver driver)))))

(deftest discover-invalid-config-test
  (testing "discover returns error map when config invalid"
    (let [driver (github/create-driver {:repos ["x"]})
          result (protocol/discover driver {})]
      (is (= 0 (:total-files result)))
      (is (some? (:error result))))))

(deftest get-set-state-test
  (testing "state round-trips"
    (let [driver (github/create-driver {:org "open-hax" :token "ghp_x"})
          new-state {:repos {"open-hax/knoxx" {:last-scan "2026-05-28T00:00:00Z"}}}]
      (protocol/set-state driver new-state)
      (is (= new-state (protocol/get-state driver))))))

(deftest extract-unparseable-test
  (testing "extract of an unparseable id returns error, not exception"
    (let [driver (github/create-driver {:org "open-hax" :token "ghp_x"})
          result (protocol/extract driver "not-a-real-id")]
      (is (nil? (:content result)))
      (is (some? (:error result))))))

(deftest registered-in-registry-test
  (testing "github driver is registered"
    (require 'kms-ingestion.drivers.registry)
    (let [drivers ((resolve 'kms-ingestion.drivers.registry/list-drivers))]
      (is (some #(= "github" %) drivers)))))
