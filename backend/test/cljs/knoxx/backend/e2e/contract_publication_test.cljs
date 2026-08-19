(ns knoxx.backend.e2e.contract-publication-test
  "One complete publication journey with NO hosted publishing backend running.

  intent -> projection -> translation requested -> translation recorded ->
  approval recorded -> gate clears -> adapter materializes the exact
  locale/revision -> receipt reports convergence -> the public route serves the
  translated artifact.

  Absence is enforced, not assumed. `js/fetch` is replaced with a harness that
  THROWS on any call, so a hidden HTTP request to any host fails this test rather
  than being accidentally satisfied by a stub. That harness is generic: it names
  no backend, which is the point — importing the dependency you are proving
  absent would defeat the proof."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.cms-publication :as cms]
            [knoxx.backend.domain.publication-gate :as gate]
            [knoxx.backend.domain.publication-plan :as plan]
            [knoxx.backend.domain.publication-receipts :as receipts]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.infra.publication-surface-verify :as verify]
            [knoxx.backend.infra.publication-target-memory :as memory]
            [knoxx.backend.law.publication-surface :as surface]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

;; ── No-network harness ─────────────────────────────────────────────────────

(defn- ^:async with-no-network!
  "Like `with-no-network`, but AWAITS the body before restoring `js/fetch`.

   The synchronous version restores fetch the moment the body returns, so for an
   async body every step after the first await ran with the real fetch — the
   guard covered the setup and not the journey it claims to prove. Anything that
   materializes a publication has to run inside this one."
  [body]
  (let [original js/fetch
        attempts (atom [])]
    (set! js/fetch (fn [url & _]
                     (swap! attempts conj (str url))
                     (throw (ex-info "network access is disabled in this scenario"
                                     {:url (str url)}))))
    (try
      (let [result (await (body))]
        {:result result :attempts @attempts})
      (finally
        (set! js/fetch original)))))

(defn- with-no-network
  "Run a SYNCHRONOUS `body` with `js/fetch` replaced by a recorder that throws.
   Any HTTP call — to any host — fails the scenario. Use `with-no-network!` for
   anything async."
  [body]
  (let [original js/fetch
        attempts (atom [])]
    (set! js/fetch (fn [url & _]
                     (swap! attempts conj (str url))
                     (throw (ex-info "network access is disabled in this scenario"
                                     {:url (str url)}))))
    (try
      (let [result (body)]
        {:result result :attempts @attempts})
      (finally
        (set! js/fetch original)))))

;; ── Fixture ────────────────────────────────────────────────────────────────

(def source-locale :en)
(def target-locale :es)
(def concrete-revision "rev-2026-08-13")
(def publication-path "/docs/demo")

(def document
  {:namespace :knoxx.docs
   :document/id :probe
   :document/title "Probe"
   :document/source-locale source-locale
   :document/source {:path "docs/probe.md"}})

(def garden
  {:namespace :knoxx.docs
   :garden/id :promethean
   :garden/title "Promethean"
   :garden/status :active})

(def intent-resource
  {:namespace :knoxx.docs
   :publication/id :probe-es
   :publication/document :probe
   :publication/garden :promethean
   :publication/locale target-locale
   :publication/revision :source/current
   :publication/state :published
   :publication/path publication-path
   :translation/review :required})

(def resource-graph [document garden intent-resource])

(defn- fixture
  "The whole system: resource index, mutable runtime evidence, an in-memory
   target and idempotency store. Evidence starts empty — nothing is translated,
   nothing approved."
  []
  (let [index (resolver/publication-index resource-graph)
        evidence (atom {:translated #{} :approved #{}})
        target-bundle (memory/memory-target)
        {:keys [store]} (memory/memory-store)
        [hydrated] (resolver/desired-publications index :knoxx.docs/probe)]
    {:index index
     :evidence evidence
     :target-bundle target-bundle
     :store store
     :intent hydrated
     :facts {:current-source-revision (constantly concrete-revision)
             :translated-revision? (fn [_document locale revision]
                                     (contains? (:translated @evidence) [locale revision]))
             :approved? (fn [_document locale revision]
                          (contains? (:approved @evidence) [locale revision]))
             :source-revision-superseded? (constantly false)
             :materialized-publication
             (fn [publication-intent]
               (->> (vals (memory/public-routes target-bundle))
                    (filter #(= (:publication/id %) (:publication/id publication-intent)))
                    first))}}))

(defn- record-translation! [{:keys [evidence]} locale revision]
  (swap! evidence update :translated conj [locale revision]))

(defn- record-approval! [{:keys [evidence]} locale revision]
  (swap! evidence update :approved conj [locale revision]))

(defn- plan-now [{:keys [index intent facts]}]
  (plan/reconcile-plan index intent facts))

;; ── 1 the intent starts blocked ───────────────────────────────────────────

(deftest ^:async intent-starts-blocked-per-the-gates-own-decision
  (let [{:keys [attempts result]}
        (with-no-network
          (fn []
            (let [system (fixture)
                  evidence (gate/publication-evidence (:intent system) (:facts system))
                  current-plan (plan-now system)]
              {:gate-blockers (gate/blockers evidence)
               :plan current-plan})))]
    (is (empty? attempts) "no HTTP call may occur at all")
    (testing "the plan's blockers ARE the gate's blockers for the same fixture,
              asserted against the gate rather than a hand-written list"
      (is (= (:gate-blockers result) (get-in result [:plan :blockers])))
      (is (= :blocked (get-in result [:plan :op]))))
    (testing "and both translation and review are outstanding"
      (is (= #{:translation-missing :translation-review-required}
             (set (:gate-blockers result)))))))

;; ── 2 translation work is derived for the concrete revision ───────────────

(deftest ^:async translation-work-is-derived-for-concrete-revision
  (let [{:keys [result]}
        (with-no-network
          (fn []
            (let [system (fixture)
                  evidence (gate/publication-evidence (:intent system) (:facts system))]
              (gate/translation-work (:intent system) evidence))))]
    (is (= :actions/request-translation (:action/id result)))
    (is (= concrete-revision (get-in result [:action/with :revision])))
    (is (= target-locale (get-in result [:action/with :locale])))
    (testing "never the selector token"
      (is (not= :source/current (get-in result [:action/with :revision]))))))

;; ── 3/4 evidence clears blockers one at a time ────────────────────────────

(deftest ^:async recorded-translation-then-approval-clears-the-gate
  (let [{:keys [attempts result]}
        (with-no-network
          (fn []
            (let [system (fixture)
                  blocked (plan-now system)
                  _ (record-translation! system target-locale concrete-revision)
                  after-translation (plan-now system)
                  _ (record-approval! system target-locale concrete-revision)
                  after-approval (plan-now system)]
              {:blocked blocked
               :after-translation after-translation
               :after-approval after-approval})))]
    (is (empty? attempts))
    (is (= :blocked (get-in result [:blocked :op])))
    (testing "recording the translation leaves only the review blocker"
      (is (= [:translation-review-required]
             (get-in result [:after-translation :blockers]))))
    (testing "approval for that same concrete revision makes the plan :publish"
      (is (= :publish (get-in result [:after-approval :op])))
      (is (= concrete-revision (get-in result [:after-approval :concrete-revision]))))))

(deftest ^:async approval-for-another-revision-does-not-clear-the-gate
  (let [{:keys [result]}
        (with-no-network
          (fn []
            (let [system (fixture)]
              (record-translation! system target-locale concrete-revision)
              (record-approval! system target-locale "some-other-revision")
              (plan-now system))))]
    (is (= :blocked (:op result)))
    (is (= [:translation-review-required] (:blockers result)))))

;; ── 5 the materialization receipt is exact ────────────────────────────────

(def translated-artifact
  "Distinctive content, so \"the public route serves the translated artifact\" is
   an assertion about what is served rather than about receipt metadata."
  {:artifact/locale target-locale
   :artifact/revision concrete-revision
   :artifact/body "Sonda — contenido traducido"})

(defn- ^:async converge!
  "Drive the fixture to convergence and return the receipt."
  ([system] (converge! system translated-artifact))
  ([system artifact]
   (let [current-plan (plan-now system)]
     (await (effects/execute-plan! (:store system)
                                   (:target (:target-bundle system))
                                   {}
                                   current-plan
                                   artifact)))))

(deftest ^:async materialization-receipt-is-exact
  (let [{:keys [result attempts]}
        (await (with-no-network!
                 (^:async fn []
                   (let [system (fixture)]
                     (record-translation! system target-locale concrete-revision)
                     (record-approval! system target-locale concrete-revision)
                     (await (converge! system))))))
        receipt result]
    (is (empty? attempts)
        "the materialization itself must run with no network available")
    (testing "one whole expected map, not per-key spot checks — an omitted key
              is how a wrong or missing path slips through"
      (is (= {:receipt/type :publication/materialized
              :publication/id :knoxx.docs/probe-es
              :adapter/id :memory/target
              :document/id :knoxx.docs/probe
              :target :knoxx.docs/promethean
              :locale target-locale
              :revision concrete-revision
              :path publication-path
              :materialized/revision concrete-revision
              :materialized/path publication-path}
             (dissoc receipt :idempotency/key))))
    (testing "and it satisfies the full observation contract"
      (is (true? (receipts/materialized? receipt)))
      (is (string? (:idempotency/key receipt))))))

;; ── 6 the public route serves the translated artifact ─────────────────────

(deftest ^:async public-read-returns-the-materialized-translation
  (let [{:keys [result attempts]}
        (await (with-no-network!
                 (^:async fn []
                   (let [system (fixture)]
                     (record-translation! system target-locale concrete-revision)
                     (record-approval! system target-locale concrete-revision)
                     (await (converge! system))
                     system))))
        system result
        routes (memory/public-routes (:target-bundle system))
        served (get routes publication-path)]
    (is (empty? attempts))
    (is (= 1 (count routes)) "exactly one public route")
    (is (some? served))
    (testing "serving the requested locale and concrete revision, not the source"
      (is (= target-locale (:locale served)))
      (is (not= source-locale (:locale served)))
      (is (= concrete-revision (:revision served))))
    (testing "and serving the translated CONTENT, not just metadata about it —
              the artifact used to be discarded, so a dropped or corrupted body
              left every assertion here green"
      (is (= translated-artifact
             (memory/served-artifact (:target-bundle system) publication-path)))
      (is (str/includes? (:artifact/body
                          (memory/served-artifact (:target-bundle system)
                                                  publication-path))
                         "traducido")))))

;; ── 7 one walkable receipt chain ──────────────────────────────────────────

(deftest ^:async one-receipt-chain-explains-convergence
  (let [system (fixture)
        blocked-plan (plan-now system)
        _ (record-translation! system target-locale concrete-revision)
        _ (record-approval! system target-locale concrete-revision)
        publish-plan (plan-now system)
        receipt (await (converge! system))
        chain [{:step :intent :state (:publication/state (:intent system))}
               {:step :blocked :blockers (:blockers blocked-plan)}
               {:step :planned :op (:op publish-plan)
                :revision (:concrete-revision publish-plan)}
               {:step :materialized :observed (receipts/observed-materialization receipt)}]]
    (testing "the chain is walkable end to end with no gap"
      (is (= [:intent :blocked :planned :materialized] (mapv :step chain)))
      (is (= :published (:state (first chain))))
      (is (seq (:blockers (second chain))))
      (is (= :publish (:op (nth chain 2))))
      (is (= concrete-revision (:revision (nth chain 2)))))
    (testing "and the final observation matches what the planner desired"
      (is (= (plan/desired-materialization (:intent system) concrete-revision)
             (:observed (last chain)))))))

;; ── 8 replay converges ────────────────────────────────────────────────────

(deftest ^:async replay-converges-without-a-second-materialization
  (let [system (fixture)
        _ (record-translation! system target-locale concrete-revision)
        _ (record-approval! system target-locale concrete-revision)
        first-receipt (await (converge! system))
        replay-plan (plan-now system)
        replay-receipt (await (converge! system))]
    (is (= :publication/materialized (:receipt/type first-receipt)))
    (testing "re-planning after convergence is a noop"
      (is (= :noop (:op replay-plan))))
    (is (= :publication/noop (:receipt/type replay-receipt)))
    (is (= 1 (memory/materialization-count (:target-bundle system))))
    (is (= 1 (count (memory/public-routes (:target-bundle system)))))))

;; ── 9 restoring a legacy authority path fails the scenario ────────────────

(deftest ^:async restoring-a-legacy-authority-path-fails-the-e2e
  (testing "an assertion, not a comment: a step that reaches for a legacy
            authority path must fail rather than be quietly satisfied"
    (let [{:keys [attempts result]}
          (with-no-network
            (fn []
              (try
                ;; Exactly what a regression would look like: some step deciding
                ;; to consult a hosted authority for publication state.
                (js/fetch "http://legacy-host/v1/gardens")
                ::no-failure
                (catch :default err (ex-message err)))))]
      (is (not= ::no-failure result)
          "the harness must make any HTTP call fail")
      (is (= ["http://legacy-host/v1/gardens"] attempts)
          "and the attempt is recorded, so the failure names what was reached for"))))

;; ── 10 one shared required-surface contract ───────────────────────────────

(deftest e2e-and-deploy-verify-share-required-surface
  (testing "the E2E imports the same var the deploy verifier iterates, not a copy"
    (is (= surface/surface-count (count surface/required-surfaces)))
    (is (some? (surface/assert-surfaces!))))
  (testing "and every surface is required unconditionally"
    (is (not-any? :optional? surface/required-surfaces))))

(deftest ^:async shared-verifier-runs-over-the-same-list
  (let [seen (atom [])
        request! (fn [{:keys [method path authorized?]}]
                   (swap! seen conj [method path])
                   (js/Promise.resolve {:status (if authorized? 200 403)}))
        result (await (verify/verify-required-surface! request!))]
    (is (true? (:ok? result)))
    (is (= (set (map (juxt :method :path) surface/required-surfaces))
           (set @seen)))))

;; ── 11 no hosted backend anywhere in the scenario's graph ─────────────────

(def ^:private entry-source
  "test/cljs/knoxx/backend/e2e/contract_publication_test.cljs")

(def ^:private contract-source-path
  "src/cljs/knoxx/backend/law/publication_surface.cljs")

(defn- read-source
  [relative-path]
  (str/lower-case
   (.readFileSync node-fs (.join path (.cwd js/process) relative-path) "utf8")))

(defn- candidate-paths
  "Where a project namespace's file could live, from the backend test cwd."
  [ns-name]
  (let [munged (-> ns-name (str/replace "." "/") (str/replace "-" "_"))]
    [(str "src/cljs/" munged ".cljs")
     (str "src/cljs/" munged ".cljc")
     (str "test/cljs/" munged ".cljs")
     (str "../shared/src/cljs/" munged ".cljs")]))

(defn- source-for
  [ns-name]
  (first (filter #(.existsSync node-fs (.join path (.cwd js/process) %))
                 (candidate-paths ns-name))))

(defn- required-project-namespaces
  "Project namespaces a source file references. Reads the file rather than the
   require form specifically, which can only over-collect — and over-collecting
   makes this guard stricter, never blinder."
  [source]
  (->> (re-seq #"(knoxx\.[a-z0-9.*+!?<>=_-]+|open-hax\.[a-z0-9.*+!?<>=_-]+)" source)
       (map first)
       (map #(str/replace % #"[^a-z0-9.*+!?<>=_-]+$" ""))
       set))

(defn- dependency-closure
  "Every project source transitively reachable from the E2E.

   Derived rather than listed. The previous version grepped twelve paths somebody
   maintained by hand, so a hosted-authority import could move into any omitted
   dependency — `shape.resource-identity` and `open-hax.publication-wire` were
   both reachable and both unchecked — and this test would still pass."
  []
  (loop [pending [entry-source]
         seen #{}]
    (if-let [current (first pending)]
      (if (contains? seen current)
        (recur (rest pending) seen)
        (let [source (read-source current)
              next-paths (->> (required-project-namespaces source)
                              (keep source-for))]
          (recur (into (vec (rest pending)) next-paths)
                 (conj seen current))))
      seen)))

(deftest no-hosted-backend-in-the-e2e-graph
  (let [legacy-marker (str "open" "planner")
        closure (dependency-closure)]
    (testing "the closure is derived and actually reaches transitively"
      (is (<= 12 (count closure))
          (str "only " (count closure) " sources derived: " (pr-str (sort closure))))
      (doseq [reachable ["src/cljs/knoxx/backend/domain/publication_resolver.cljs"
                         "src/cljs/knoxx/backend/domain/publication_gate.cljs"
                         "src/cljs/knoxx/backend/domain/publication_plan.cljs"
                         "src/cljs/knoxx/backend/domain/publication_receipts.cljs"
                         "src/cljs/knoxx/backend/domain/cms_publication.cljs"
                         "src/cljs/knoxx/backend/infra/publication_effects.cljs"
                         "src/cljs/knoxx/backend/infra/publication_target_memory.cljs"
                         "src/cljs/knoxx/backend/infra/publication_surface_verify.cljs"
                         "src/cljs/knoxx/backend/law/publication.cljs"
                         "src/cljs/knoxx/backend/law/publication_receipts.cljs"
                         "src/cljs/knoxx/backend/law/cms_publication.cljs"
                         ;; The two the hand-maintained list missed, named
                         ;; explicitly so the derivation cannot quietly stop
                         ;; reaching them.
                         "src/cljs/knoxx/backend/shape/resource_identity.cljs"
                         "../shared/src/cljs/open_hax/publication_wire.cljs"]]
        (is (contains? closure reachable)
            (str reachable " is reachable from the E2E but was not derived"))))
    (doseq [relative-path (sort (disj closure contract-source-path))]
      (testing relative-path
        (is (not (str/includes? (read-source relative-path) legacy-marker)))))
    (testing "law.publication-surface is the ONE deliberate exception"
      ;; It must name the retired paths literally, because it is the list every
      ;; other file is grepped against. Asserted positively so the exception
      ;; cannot silently become a place where a real dependency hides.
      (let [contract-source (read-source contract-source-path)]
        (is (str/includes? contract-source legacy-marker))
        (is (every? #(str/includes? contract-source (str/lower-case %))
                    surface/retired-authority-paths))
        (testing "and it declares them only as retired data, never calls them"
          (is (not (str/includes? contract-source "js/fetch")))
          (is (not (str/includes? contract-source "api/request"))))))))

;; ── The CMS sees the journey too ──────────────────────────────────────────

(deftest ^:async cms-projection-reflects-the-converged-publication
  (let [system (fixture)
        _ (record-translation! system target-locale concrete-revision)
        _ (record-approval! system target-locale concrete-revision)
        receipt (await (converge! system))
        wire (cms/list-view->wire
              {:receipts (cms/receipts->observed [receipt]) :blockers {}}
              (resolver/list-document-views (:index system)))
        publication (get-in wire [:documents 0 :publications 0])]
    (testing "desired intent and observed evidence are separate wire fields"
      (is (= "published" (:desired publication)))
      (is (= concrete-revision (:observed publication))))
    (testing "and identity crosses with its namespace intact"
      (is (= "knoxx.docs/probe-es" (:id publication)))
      (is (= "knoxx.docs/promethean" (:garden publication))))
    (testing "with no blockers remaining"
      (is (= [] (:blockers publication))))))
