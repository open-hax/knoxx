(ns knoxx.backend.e2e.mcp-tools-e2e
  "Every MCP tool, over a real socket, through the shipped serving path.

   This is the suite that answers \"which tools actually work\" without a
   browser, an OAuth round trip, or a deployed connector — the question that
   used to require asking a hosted assistant to try a tool and reading what it
   said back.

   Three kinds of assertion, in increasing depth:

   1. The catalog is well formed. A tool that arrives with no schema is present
      and unusable, and nothing logs an error when that happens.
   2. Every tool with a fixture accepts its arguments and runs. A JSON-RPC
      error means the server refused or threw; a tool-level error means the
      tool ran and reported a problem. That only passes when its fixture names
      the exact dependency error this harness intentionally cannot satisfy.
   3. A credential-backed tool resolves the seeded actor's credential and puts
      it on the wire. That path had no test at all before the policy context
      grew a credential seam."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.e2e.mcp-client :as mcp]
            [knoxx.backend.e2e.tool-fixtures :as fixtures]))

(def ^:private legal-tool-name #"^[A-Za-z0-9_-]{1,128}$")

(defn- ^:async exposed-tools!
  "The catalog a started harness serves."
  [started]
  (let [client (harness/client started)
        _      (await (mcp/initialize! client))
        listed (await (mcp/list-tools! client))]
    (is (:ok listed) (str "tools/list refused: " (pr-str (:error listed))))
    (vec (get-in listed [:result :tools]))))

;; ── 1. the catalog ──────────────────────────────────────────────────────────

(defn- schema-faults
  "Structural faults in one tool descriptor. Empty when it is usable.

   Annotations are not checked here — they are a separate, ratcheted concern
   below, because most of the catalog is missing them and that is old debt
   rather than a regression this suite introduced."
  [{tool-name :name :keys [description inputSchema]}]
  (cond-> []
    (not (re-matches legal-tool-name (str tool-name)))
    (conj "name is not an MCP-legal tool name")

    (nil? inputSchema)
    (conj "no inputSchema — a client cannot construct a call")

    (and inputSchema (not= "object" (:type inputSchema)))
    (conj (str "inputSchema.type is " (pr-str (:type inputSchema)) ", not \"object\""))

    (str/blank? (str description))
    (conj "no description — a model has nothing to select on")))

(deftest ^:async catalog-is-served-test
  (let [started (await (harness/start!))]
    (try
      (let [tools (await (exposed-tools! started))]
        (testing "the loopback grant reaches a non-trivial catalog"
          (is (< 20 (count tools))
              (str "expected the full tool surface, got " (count tools)
                   ": " (pr-str (mapv :name tools)))))

        (testing "no tool name is registered twice"
          (let [names (mapv :name tools)]
            (is (= (count names) (count (distinct names)))
                (str "duplicate registrations shadow earlier tools: "
                     (pr-str (->> names frequencies (filter #(< 1 (val %))))))))))
      (finally (await (harness/stop! started))))))

(deftest ^:async every-tool-is-structurally-usable-test
  (let [started (await (harness/start!))]
    (try
      (let [tools  (await (exposed-tools! started))
            faulty (into {}
                         (keep (fn [tool]
                                 (let [faults (schema-faults tool)]
                                   (when (seq faults) [(:name tool) faults]))))
                         tools)]
        (is (empty? faulty)
            (str "tools arrived degraded — present but not callable:\n"
                 (str/join "\n" (map (fn [[n fs]] (str "  " n ": " (str/join "; " fs)))
                                     faulty)))))
      (finally (await (harness/stop! started))))))

(def unannotated-baseline
  "How many served tools currently declare no MCP annotations.

   A ratchet, not a target. MCP's ToolAnnotations defaults are pessimistic when
   a tool says nothing — destructiveHint and openWorldHint default to true,
   readOnlyHint to false — so every tool in this count is presented to a user
   as a destructive, open-world write regardless of what it does. That is the
   conservative and correct default when nobody has checked, which is exactly
   why law.mcp-tool-annotations refuses to guess.

   Lower it as entries are added there. It must never rise: a new tool arriving
   unannotated is a new tool the client has to warn about."
  73)

(deftest ^:async annotation-coverage-does-not-regress-test
  (let [started (await (harness/start!))]
    (try
      (let [tools       (await (exposed-tools! started))
            unannotated (->> tools (remove :annotations) (mapv :name) sort vec)]
        (.log js/console
              (str "\n[mcp-e2e] " (count unannotated) " of " (count tools)
                   " tools declare no annotations (baseline "
                   unannotated-baseline "):\n  " (str/join "\n  " unannotated)))
        (is (<= (count unannotated) unannotated-baseline)
            (str "tools without MCP annotations grew from " unannotated-baseline
                 " to " (count unannotated) ". Add entries to "
                 "law.mcp-tool-annotations for:\n  "
                 (str/join "\n  " unannotated))))
      (finally (await (harness/stop! started))))))

;; ── 2. calling them ─────────────────────────────────────────────────────────

(defn- ^:async sweep!
  "Call every fixture-covered tool in turn, returning {tool-name outcome}.

   Serial rather than concurrent: several tools mutate process-global runtime
   state, and a parallel sweep would report failures that only exist because
   the sweep itself raced."
  [client tools]
  (let [exposed (into #{} (map :name) tools)
        planned (->> (fixtures/callable false)
                     (filter (fn [[tool-name _]] (contains? exposed tool-name)))
                     (sort-by key)
                     vec)]
    (loop [remaining planned
           results   {}]
      (if-let [[tool-name fixture] (first remaining)]
        (let [outcome (mcp/call-outcome
                       (await (mcp/call-tool! client tool-name (:args fixture))))]
          (recur (rest remaining) (assoc results tool-name outcome)))
        results))))

(defn- report-line
  [[tool-name outcome]]
  (str "  " (name (:status outcome)) "  " tool-name "  "
       (some-> (:detail outcome) (str/replace #"\s+" " ") (subs 0 (min 90 (count (str (:detail outcome))))))))

(deftest ^:async covered-tools-accept-their-arguments-test
  (let [started (await (harness/start!))]
    (try
      (let [client  (harness/client started)
            tools   (await (exposed-tools! started))
            results (await (sweep! client tools))
            refused (into {} (filter (fn [[_ o]] (= :rpc-error (:status o)))) results)
            unexpected-errors
            (into {}
                  (filter (fn [[tool-name outcome]]
                            (and (= :tool-error (:status outcome))
                                 (not (fixtures/allowed-tool-error?
                                       tool-name (:detail outcome))))))
                  results)
            errored (count (filter (fn [[_ o]] (= :tool-error (:status o))) results))
            ok      (count (filter (fn [[_ o]] (= :ok (:status o))) results))]

        (.log js/console
              (str "\n[mcp-e2e] " ok " ok · " errored " reported an error · "
                   (count refused) " refused, of " (count results) " called\n"
                   (str/join "\n" (map report-line (sort-by key results)))))

        (testing "no tool refuses its own fixture"
          (is (empty? refused)
              (str "these tools rejected arguments their fixture says are valid — "
                   "a schema conversion or a handler is broken:\n"
                   (str/join "\n" (map (fn [[n o]] (str "  " n ": " (:detail o)))
                                       refused)))))

        (testing "tool errors are only reviewed dependency failures"
          (is (empty? unexpected-errors)
              (str "these tools reported unreviewed errors. Fix the defect, or add "
                   "the narrow dependency message as :allowed-error on its fixture:\n"
                   (str/join "\n" (map (fn [[tool-name outcome]]
                                          (str "  " tool-name ": " (:detail outcome)))
                                        unexpected-errors)))))

        (testing "the sweep actually called something"
          (is (< 5 (count results))
              "fixtures stopped matching the exposed catalog")))
      (finally (await (harness/stop! started))))))

;; ── 3. credentials reach the wire ───────────────────────────────────────────

(deftest ^:async credentialed-tool-uses-the-seeded-actor-test
  (let [started (await (harness/start!))]
    (try
      (let [client (harness/client started)
            tools  (await (exposed-tools! started))]
        (if-not (contains? (into #{} (map :name) tools) "bluesky_profile")
          (is false "bluesky_profile is no longer exposed; pick another credentialed tool")
          (let [outcome (mcp/call-outcome
                         (await (mcp/call-tool! client "bluesky_profile" {})))
                calls   (harness/captured-matching "bsky")]
            (testing "the call is not refused for want of an actor"
              (is (not= :rpc-error (:status outcome))
                  (str "bluesky_profile failed at the protocol level: " (:detail outcome)))
              (is (not (str/includes? (str (:detail outcome)) "No current actor_id"))
                  (str "the token's actor did not reach the credential lookup: "
                       (:detail outcome))))

            (testing "it reached the network carrying the seeded credential"
              (is (seq calls)
                  (str "no outbound Bluesky request was attempted; the tool failed "
                       "before the credential was used: " (:detail outcome)))
              (is (some #(str/includes? (str (:body %) (pr-str (:headers %))) "e2e.test")
                        calls)
                  (str "the seeded actor credential did not appear on any outbound "
                       "request: " (pr-str (mapv :url calls))))))))
      (finally (await (harness/stop! started))))))

;; ── coverage, reported rather than assumed ──────────────────────────────────

(deftest ^:async fixture-coverage-test
  (let [started (await (harness/start!))]
    (try
      (let [tools     (await (exposed-tools! started))
            names     (mapv :name tools)
            uncovered (fixtures/uncovered names)
            stale     (fixtures/stale names)]
        (.log js/console
              (str "\n[mcp-e2e] " (count names) " tool(s) served"
                   (when (seq uncovered)
                     (str "; " (count uncovered) " with no fixture:\n  "
                          (str/join "\n  " uncovered)))
                   "\n[mcp-e2e] covered by a dedicated suite rather than the sweep:\n  "
                   (str/join "\n  " (map (fn [[t ns-name]] (str t " — " ns-name))
                                         (fixtures/covered-elsewhere)))
                   "\n[mcp-e2e] known off the MCP surface:\n"
                   (fixtures/absence-report)))

        (testing "each fixture has exactly one execution disposition"
          (is (empty? (fixtures/disposition-faults))
              (str "fixtures must declare exactly one of :args, :needs, :absent, "
                   "or :covered-by: " (pr-str (fixtures/disposition-faults)))))

        (testing "no fixture names a tool that no longer exists"
          (is (empty? stale)
              (str "these fixtures are stale; the tools were renamed or removed:\n  "
                   (str/join "\n  " stale))))

        (testing "every recorded absence is still true"
          (let [wrong (fixtures/wrongly-absent names)]
            (is (empty? wrong)
                (str "a fixture claims a tool is off the MCP surface, but it is "
                     "served:\n  " (str/join "\n  " wrong)))))

        (testing "fixture coverage does not collapse"
          (let [covered (- (count names) (count uncovered))]
            (is (<= 0.5 (/ covered (max 1 (count names))))
                (str "only " covered " of " (count names)
                     " tools have a fixture; the sweep is no longer representative "
                     "of the surface")))))
      (finally (await (harness/stop! started))))))
