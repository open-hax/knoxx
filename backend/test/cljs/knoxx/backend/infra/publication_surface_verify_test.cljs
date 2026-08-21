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

(def ^:private repo-root
  (.join path (.cwd js/process) ".."))

(def ^:private source-extension
  #"\.(cljs|cljc|clj|tsx|ts|jsx|js)$")

(def ^:private declaration-files
  "Files that name legacy paths as DATA rather than calling them: this contract
   and the guard that reads it. Excluded from the scan, and asserted to still
   contain them below, so the exclusion cannot hide a deletion."
  #{"backend/src/cljs/knoxx/backend/law/publication_surface.cljs"})

(defn- test-file?
  [relative-path]
  (or (str/includes? relative-path "/test/")
      (re-find #"(\.test\.(tsx?|jsx?)|_test\.clj[sc]?|-test\.clj[sc]?)$" relative-path)))

(defn- production-sources
  "Every shipped production source file under the declared roots, repository
   relative.

   A walk rather than an allow-list. The previous guard checked twelve files
   somebody had remembered to add, so a caller in any other file — and there
   were several — passed unnoticed while the test reported the path retired."
  []
  (->> surface/scanned-source-roots
       (mapcat (fn [root]
                 (let [absolute (.join path repo-root root)]
                   (when (.existsSync node-fs absolute)
                     (->> (.readdirSync node-fs absolute #js {:recursive true})
                          array-seq
                          (map #(str root "/" %)))))))
       (filter #(re-find source-extension %))
       (remove test-file?)
       (remove declaration-files)
       sort
       vec))

(defn- callers-of
  [needle]
  (->> (production-sources)
       (filter #(str/includes? (repo-text %) needle))
       vec))

(deftest the-scan-actually-reaches-the-tree
  (testing "a walk that found nothing would make every retirement assertion
            below vacuously true"
    (let [sources (production-sources)]
      (is (< 100 (count sources)) (str "only " (count sources) " sources scanned"))
      (doseq [root surface/scanned-source-roots]
        (is (some #(str/starts-with? % root) sources)
            (str "nothing scanned under " root))))))

(deftest no-retired-authority-path-has-a-caller-anywhere
  (doseq [retired surface/retired-authority-paths]
    (testing retired
      (is (empty? (callers-of retired))
          (str retired " still has callers: " (pr-str (callers-of retired))))))
  (testing "and the contract still names them, so excluding it cannot hide a deletion"
    (let [declaration (repo-text (first declaration-files))]
      (doseq [retired surface/retired-authority-paths]
        (is (str/includes? declaration retired))))))

(deftest legacy-paths-have-exactly-their-known-callers
  (testing "these are outside this epic's scope and still called; naming them
            keeps the exception visible instead of hiding it in an allow-list"
    (doseq [[legacy-path expected] surface/legacy-paths-with-known-callers]
      (testing legacy-path
        (is (= (set expected) (set (callers-of legacy-path)))
            (str "callers changed: " (pr-str (callers-of legacy-path))))))))

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
      (is (empty? (callers-of flag))
          (str flag " still appears in " (pr-str (callers-of flag))))))
  (testing "and the required surfaces are unconditional — no access is optional"
    (is (every? #(contains? #{:read :write} (:access %)) surface/required-surfaces))
    (is (not-any? :optional? surface/required-surfaces))))

;; ── the probe carries the capability it claims to test (Codex on #240) ─────

(deftest the-default-request-carries-the-declared-permission
  (testing "a probe that knows only authorized-or-not cannot tell a correctly
            guarded route from one guarded by something broader"
    (doseq [required surface/required-surfaces]
      (let [request (verify/default-request required true)]
        (is (= (:permission required) (:permission request)))
        (is (true? (:authorized? request)))
        (is (= (:method required) (:method request)))
        (is (= (:path required) (:path request))))))
  (testing "and the unauthorized probe is the same request without authority"
    (is (false? (:authorized? (verify/default-request (first surface/required-surfaces) false))))))

(deftest ^:async a-caller-can-materialize-template-paths-and-bodies
  (testing "several paths are templates and both PATCH surfaces need a body;
            sending the literal template answers 404 or fails validation, which
            would fail this gate while every route was healthy"
    (let [seen (atom [])
          request! (fn [request]
                     (swap! seen conj request)
                     (js/Promise.resolve {:status (if (:authorized? request) 200 403)}))
          materialize (fn [required authorized?]
                        (-> (verify/default-request required authorized?)
                            (update :path str/replace #":documentId" "knoxx.docs%2Fprobe")
                            (update :path str/replace #":publicationId" "knoxx.docs%2Fprobe-es")
                            (cond-> (= "PATCH" (:method required))
                              (assoc :body {:state "withheld"}))))
          result (await (verify/verify-required-surface! request! materialize))]
      (is (true? (:ok? result)))
      (testing "no literal template reached the transport"
        (is (not-any? #(str/includes? (:path %) ":") @seen)))
      (testing "and every PATCH carried a body"
        (is (every? :body (filter #(= "PATCH" (:method %)) @seen))))
      (testing "while the default arity still works unchanged"
        (let [plain (atom [])
              plain-request! (fn [request]
                               (swap! plain conj request)
                               (js/Promise.resolve {:status (if (:authorized? request) 200 401)}))]
          (is (true? (:ok? (await (verify/verify-required-surface! plain-request!)))))
          (is (= (* 2 surface/surface-count) (count @plain))))))))

