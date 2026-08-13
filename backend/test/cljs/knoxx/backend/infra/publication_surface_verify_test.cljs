(ns knoxx.backend.infra.publication-surface-verify-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.set :as set]
            [clojure.string :as str]
            [malli.core :as m]
            [knoxx.backend.infra.publication-surface-verify :as verify]
            [knoxx.backend.law.publication-surface :as surface]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

(defn- source-text
  [relative-path]
  (.readFileSync node-fs (.join path (.cwd js/process) relative-path) "utf8"))

(defn- repo-text
  "Read a path relative to the repository root, so ingestion and frontend files
   are reachable from the backend test build."
  [relative-path]
  (.readFileSync node-fs
                 (.join path (.cwd js/process) ".." relative-path)
                 "utf8"))

;; ── 1/2 the shared contract ───────────────────────────────────────────────

(deftest required-surface-list-is-complete
  (is (= surface/surface-count (count surface/required-surfaces))
      "a silently shortened list must fail rather than verify less")
  (is (some? (surface/assert-surfaces!)))
  (is (true? (m/validate [:vector surface/RequiredSurface] surface/required-surfaces)))
  (testing "every entry names method, path, permission and access"
    (doseq [required surface/required-surfaces]
      (is (contains? #{"GET" "PATCH"} (:method required)))
      (is (str/starts-with? (:path required) "/api/"))
      (is (seq (:permission required)))
      (is (contains? #{:read :write} (:access required)))
      (is (seq (:why required)))))
  (testing "read and write carry distinct permissions"
    (let [by-access (group-by :access surface/required-surfaces)]
      (is (seq (:read by-access)))
      (is (seq (:write by-access)))
      (is (empty? (set/intersection
                   (set (map :permission (:read by-access)))
                   (set (map :permission (:write by-access))))))))
  (testing "both write surfaces exist — publication state and translation model"
    (is (= #{"/api/cms/publications/intents/:publicationId"
             "/api/translations/config"}
           (set (map :path (filter #(= :write (:access %)) surface/required-surfaces)))))))

(deftest verifier-and-contract-share-one-var
  (testing "the verifier iterates the shared var itself, not a copy"
    (is (str/includes? (source-text "src/cljs/knoxx/backend/infra/publication_surface_verify.cljs")
                       "surface/required-surfaces"))
    (testing "and performs no flag, env, or skip branch"
      (let [code (-> (source-text "src/cljs/knoxx/backend/infra/publication_surface_verify.cljs")
                     (str/replace #"(?s)\"(\\.|[^\"\\])*\"" "\"\"")
                     (str/replace #";[^\n]*" ""))]
        (is (not (str/includes? code "getenv")))
        (is (not (str/includes? code "js/process.env")))
        (is (not (str/includes? code "EXPECT")))))))

;; ── 3 the verifier awaits both requests before reading status ─────────────

(deftest ^:async verifier-awaits-authorized-and-unauthorized
  (let [events (atom [])
        request! (fn [{:keys [authorized?]}]
                   (swap! events conj [:request authorized?])
                   (js/Promise.resolve {:status (if authorized? 200 403)}))
        result (await (verify/verify-surface!
                       request!
                       (first surface/required-surfaces)))]
    (testing "both an authorized and an unauthorized request were made"
      (is (= [[:request true] [:request false]] @events)))
    (is (= 200 (:authorized-status result)))
    (is (= 403 (:unauthorized-status result)))
    (is (true? (:ok? result)))))

(deftest ^:async a-wide-open-surface-fails-verification
  (testing "answering 200 to an anonymous caller is a FAILURE even though the
            surface responds — verifying only the happy path would pass it"
    (let [request! (fn [_] (js/Promise.resolve {:status 200}))
          result (await (verify/verify-surface! request! (first surface/required-surfaces)))]
      (is (false? (:ok? result)))
      (is (= 200 (:unauthorized-status result))))))

(deftest ^:async verify-covers-every-surface-unconditionally
  (let [seen (atom [])
        request! (fn [{:keys [method path authorized?]}]
                   (swap! seen conj [method path authorized?])
                   (js/Promise.resolve {:status (if authorized? 200 403)}))
        result (await (verify/verify-required-surface! request!))]
    (is (true? (:ok? result)))
    (is (empty? (:failures result)))
    (is (= surface/surface-count (count (:results result))))
    (testing "each surface was checked twice: authorized and unauthorized"
      (is (= (* 2 surface/surface-count) (count @seen)))
      (is (= (set (map (juxt :method :path) surface/required-surfaces))
             (set (map (fn [[method path _]] [method path]) @seen)))))))

(deftest ^:async one-broken-surface-fails-the-whole-verification
  (let [request! (fn [{:keys [path authorized?]}]
                   (js/Promise.resolve
                    {:status (cond
                               (= path "/api/translations/config") 503
                               authorized? 200
                               :else 403)}))
        result (await (verify/verify-required-surface! request!))]
    (is (false? (:ok? result)))
    (is (seq (:failures result)))
    (is (contains? (set (map :path (:failures result))) "/api/translations/config"))))

;; ── 4 no await outside an ^:async function ────────────────────────────────

(deftest verifier-has-no-await-in-plain-defn
  (let [source (source-text "src/cljs/knoxx/backend/infra/publication_surface_verify.cljs")
        forms (str/split source #"\(defn")]
    (doseq [form (rest forms)
            :when (str/includes? form "(await ")]
      (is (str/starts-with? form " ^:async")
          (str "an await sits outside an ^:async defn: "
               (first (str/split-lines form)))))))

;; ── 5/6/7 retirement greps ────────────────────────────────────────────────

(def shipped-sources
  "Shipped modules that must not reference a retired authority path."
  ["backend/src/cljs/knoxx/backend/infra/routes/cms_publication.cljs"
   "backend/src/cljs/knoxx/backend/infra/routes/publications.cljs"
   "backend/src/cljs/knoxx/backend/infra/routes/translation_config.cljs"
   "backend/src/cljs/knoxx/backend/domain/translation_config.cljs"
   "frontend/src/pages/CmsPage.tsx"
   "frontend/src/lib/api/publications.ts"
   "frontend/src/lib/api/openplanner.ts"
   "frontend/src/cljs/knoxx/frontend/pages/translations/api.cljs"
   "frontend/src/cljs/knoxx/frontend/lib/publication_wire.cljs"
   "ingestion/src/kms_ingestion/translation/worker.clj"
   "ingestion/src/kms_ingestion/contracts/resolve.clj"
   "ingestion/src/kms_ingestion/config.clj"])

(deftest no-retired-authority-path-has-a-shipped-caller
  (doseq [relative-path shipped-sources]
    (testing relative-path
      (let [source (repo-text relative-path)]
        (doseq [retired surface/retired-authority-paths]
          (is (not (str/includes? source retired))
              (str retired " still has a caller in " relative-path)))))))

(deftest cms-reads-no-legacy-garden-surface
  (let [source (repo-text "frontend/src/pages/CmsPage.tsx")]
    (is (not (str/includes? source "/api/openplanner/v1/gardens")))
    (is (not (str/includes? source "garden_publications")))
    (testing "and it does read the replacement surface"
      (is (str/includes? source "listPublicationTopology")))))

;; ── 8 the conditional-skip flag is gone ───────────────────────────────────

(deftest expect-openplanner-rest-flag-is-gone
  (doseq [flag surface/retired-deploy-flags]
    (testing flag
      (doseq [relative-path shipped-sources]
        (is (not (str/includes? (repo-text relative-path) flag))))))
  (testing "and the required surfaces are unconditional — no access is optional"
    (is (every? #(contains? #{:read :write} (:access %)) surface/required-surfaces))
    (is (not-any? :optional? surface/required-surfaces))))
