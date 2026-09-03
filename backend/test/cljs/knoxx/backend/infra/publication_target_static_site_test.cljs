(ns knoxx.backend.infra.publication-target-static-site-test
  "The static-site target against a real filesystem: every DoD line of
   knoxx-publication-static-site-target, exercised through the same effect
   boundary production uses."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.infra.publication-target-registry :as registry]
            [knoxx.backend.infra.publication-target-static-site :as static-site]
            [knoxx.backend.law.publication-manifest :as manifest-law]
            ["node:fs" :as node-fs]
            ["node:os" :as os]
            ["node:path" :as path]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def ^:private intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/target :knoxx.publication/static-site
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required
   :document/source-locale :en})

(def ^:private artifact
  {:artifact/content "<!doctype html><p>Sonda — contenido traducido</p>"
   :artifact/media-type "text/html"
   :artifact/encoding "utf-8"
   :artifact/locale :es
   :artifact/revision "probe-revision"})

(def ^:private publish-plan
  {:op :publish
   :intent intent
   :desired {:materialized/revision "probe-revision" :materialized/path "/probe"}
   :previous nil
   :concrete-revision "probe-revision"})

(def ^:private artifact-path
  "The path derivation is pinned in the law test; here it is only how the
   adapter's writes are found on disk."
  "artifacts/knoxx.docs/probe/es/probe-revision.html")

;; ── Filesystem helpers ─────────────────────────────────────────────────────

(defn- temp-root!
  "A fresh, empty content root. The caller removes it."
  []
  (.mkdtempSync node-fs (.join path (.tmpdir os) "knoxx-static-site-")))

(defn- remove-root!
  "Delete a content root and everything under it."
  [root]
  (.rmSync node-fs root #js {:recursive true :force true}))

(defn- ^:async with-root!
  "Run async `f` with a fresh empty content root, removing the root
   afterwards even when `f` throws."
  [f]
  (let [root (temp-root!)]
    (try
      (await (f root))
      (finally
        (remove-root! root)))))

(defn- with-root-sync!
  "The synchronous `with-root!`: run `f` with a fresh content root and
   always remove it."
  [f]
  (let [root (temp-root!)]
    (try
      (f root)
      (finally
        (remove-root! root)))))

(defn- target-in
  "The adapter over `root`, declared exactly as a resource would declare it."
  [root]
  (static-site/static-site-target
   {:publication-target/id :knoxx.publication/static-site
    :publication-target/kind static-site/kind
    :publication-target/config {:content-root root}
    :publication-target/enabled? true}))

(defn- read-manifest
  "Parse the manifest under `root`."
  [root]
  (manifest-law/edn->manifest
   (.readFileSync node-fs (.join path root "manifest.edn") "utf8")))

(defn- read-artifact
  "The probe artifact's bytes as a UTF-8 string."
  [root]
  (.readFileSync node-fs (.join path root artifact-path) "utf8"))

(defn- artifact-exists?
  "True when the probe artifact file exists."
  [root]
  (.existsSync node-fs (.join path root artifact-path)))

(defn- manifest-exists?
  "True when a manifest exists under `root`."
  [root]
  (.existsSync node-fs (.join path root "manifest.edn")))

(defn- age-artifact!
  "Set the artifact's mtime far into the past, so a later rewrite is
   distinguishable from a replay that touched nothing. mtime granularity
   alone cannot tell \"rewritten just now\" from \"written just now\"."
  [root]
  (let [long-ago (js/Date. 946684800000)]
    (.utimesSync node-fs (.join path root artifact-path) long-ago long-ago)))

(defn- artifact-aged?
  "True when the artifact's mtime is still the one `age-artifact!` set."
  [root]
  (< (.getTime (.-mtime (.statSync node-fs (.join path root artifact-path))))
     950000000000))

;; ── DoD: a published document is fetchable at its manifest path ───────────

(deftest ^:async a-published-document-is-fetchable-with-the-expected-bytes
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)
            receipt (await (effects/execute-plan!
                            store target {} publish-plan artifact))]
        (is (= :publication/materialized (:receipt/type receipt)))
        (is (= "probe-revision" (:materialized/revision receipt)))
        (is (= "/probe" (:materialized/path receipt)))
        (testing "the manifest names the route and the artifact PATH, and the
                  bytes at that path are exactly what the renderer produced"
          (let [route (first (:manifest/routes (read-manifest root)))]
            (is (= "/probe" (:route/path route)))
            (is (= :es (:route/locale route)))
            (is (= artifact-path (:route/artifact route)))
            (is (= "probe-revision" (:route/revision route)))
            (is (= :knoxx.docs/probe-es (:publication/id route)))
            (testing "media type and encoding come STRAIGHT off the artifact —
                      no derivation, no defaulting"
              (is (= "text/html" (:route/media-type route)))
              (is (= "utf-8" (:route/encoding route))))
            (is (= (:artifact/content artifact) (read-artifact root))))))))))

(deftest ^:async titled-materialization-receipt-matches-the-committed-route
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)
            titled-intent (assoc intent :document/title "Deployment Probe")
            titled-plan (assoc publish-plan
                               :intent titled-intent
                               :desired {:materialized/revision "probe-revision"
                                         :materialized/path "/probe"
                                         :materialized/title "Deployment Probe"})
            receipt (await (effects/execute-plan!
                            store target {} titled-plan artifact))
            observed (await (effects/observe! target {} titled-intent))
            replay (await (effects/execute-plan!
                           store target {} titled-plan artifact))]
        (is (= :publication/materialized (:receipt/type receipt)))
        (is (= "Deployment Probe" (:materialized/title receipt)))
        (is (= "Deployment Probe" (:materialized/title observed)))
        (is (= "Deployment Probe"
               (:route/title (first (:manifest/routes (read-manifest root))))))
        (is (= receipt replay)
            "the persisted idempotency receipt retains the committed title"))))))

(deftest ^:async byte-artifacts-are-written-verbatim
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)
            bytes (.encode (js/TextEncoder.) "binäre bytes — ünïcode")
            receipt (await (effects/execute-plan!
                            store target {} publish-plan
                            (assoc artifact :artifact/content bytes)))]
        (is (= :publication/materialized (:receipt/type receipt)))
        (let [on-disk (.readFileSync node-fs (.join path root artifact-path))]
          (is (= (vec bytes) (vec (js/Uint8Array. on-disk)))
              "the adapter transports; it does not re-encode")))))))

(deftest ^:async media-type-and-encoding-are-never-derived-or-defaulted
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)
            plain (assoc artifact
                         :artifact/content "plain text, sin adornos"
                         :artifact/media-type "text/plain")
            receipt (await (effects/execute-plan!
                            store target {} publish-plan plain))]
        (is (= :publication/materialized (:receipt/type receipt)))
        (let [route (first (:manifest/routes (read-manifest root)))]
          (is (= "text/plain" (:route/media-type route)))
          (is (= "utf-8" (:route/encoding route)))
          (is (= "artifacts/knoxx.docs/probe/es/probe-revision.txt"
                 (:route/artifact route)))))))))

;; ── DoD: replay is a noop and does not rewrite the artifact ───────────────

(deftest ^:async replay-does-not-rewrite-the-artifact
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)
            first-receipt (await (effects/execute-plan!
                                  store target {} publish-plan artifact))]
        (age-artifact! root)
        (let [second-receipt (await (effects/execute-plan!
                                     store target {} publish-plan artifact))]
          (is (= first-receipt second-receipt)
              "the reservation answers :done with the recorded receipt")
          (is (artifact-aged? root)
              "the artifact file was NOT rewritten — its mtime is still the
               one the first write left behind")
          (testing "and even with the reservation released, the adapter itself
                    short-circuits an identical route rather than rewriting"
            (effects/release! store (effects/publish-idempotency-key
                                     :knoxx.publication/static-site
                                     intent "probe-revision"))
            (let [receipt (await (effects/publish! target {}
                                                   {:intent intent
                                                    :artifact artifact
                                                    :previous nil
                                                    :concrete-revision "probe-revision"
                                                    :idempotency/key "direct-replay"}))]
              (is (= :publication/materialized (:receipt/type receipt)))
              (is (artifact-aged? root)
                  "identical manifest route + present bytes = no write")))))))))

;; ── DoD: a path move leaves exactly one public route ──────────────────────

(deftest ^:async a-path-move-leaves-exactly-one-public-route
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)
            _ (await (effects/execute-plan! store target {} publish-plan artifact))
            moved-intent (assoc intent :publication/path "/moved")
            moved-plan {:op :publish
                        :intent moved-intent
                        :desired {:materialized/revision "probe-revision"
                                  :materialized/path "/moved"}
                        :previous {:materialized/revision "probe-revision"
                                   :materialized/path "/probe"}
                        :concrete-revision "probe-revision"}
            receipt (await (effects/execute-plan!
                            store target {} moved-plan artifact))]
        (is (= :publication/materialized (:receipt/type receipt)))
        (let [routes (:manifest/routes (read-manifest root))]
          (is (= 1 (count routes)) "exactly one public route for the publication")
          (is (= "/moved" (:route/path (first routes)))))
        (testing "observe! is keyed on publication identity, so it sees the
                  route at its NEW path even when asked with the stale intent"
          (let [observed (await (effects/observe! target {} intent))]
            (is (= "/moved" (:materialized/path observed)))
            (is (= "probe-revision" (:materialized/revision observed))))))))))

;; ── DoD: a removal stops serving and is visible to observe! ───────────────

(deftest ^:async a-removal-stops-serving-and-is-observable
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)]
        (await (effects/execute-plan! store target {} publish-plan artifact))
        (let [observed (await (effects/observe! target {} intent))]
          (is (some? observed) "materialized before removal")
          (let [receipt (await (effects/execute-plan!
                                store target {}
                                {:op :remove :intent intent :observed observed}
                                nil))]
            (is (= :publication/removed (:receipt/type receipt)))
            (testing "the publication id is carried on the receipt — without it
                      the projection drops the removal and a republish of the
                      same revision reads as :noop with nothing public"
              (is (= :knoxx.docs/probe-es (:publication/id receipt)))
              (is (= "/probe" (:removed/path receipt))))
            (is (empty? (:manifest/routes (read-manifest root)))
                "the route left the manifest, so it stops serving")
            (is (not (artifact-exists? root))
                "and its bytes were reclaimed after the commit")
            (is (nil? (await (effects/observe! target {} intent)))
                "observe! sees the removal"))))))))

(deftest ^:async removal-is-idempotent-and-an-empty-root-is-valid
  (await
   (with-root!
    (^:async fn [root]
      (let [target (target-in root)]
        (testing "an empty content root is a valid initial state, not an error"
          (is (nil? (await (effects/observe! target {} intent)))))
        (let [observed {:materialized/revision "probe-revision"
                        :materialized/path "/probe"}
              first-receipt (await (effects/remove! target {} intent observed))
              second-receipt (await (effects/remove! target {} intent observed))]
          (is (= :publication/removed (:receipt/type first-receipt)))
          (is (= first-receipt second-receipt)
              "removing what was never published is the same receipt"))
        (testing "and a root that does not exist yet at all is equally valid"
          (let [unborn (.join path root "never-created")
                unborn-target (target-in unborn)]
            (is (nil? (await (effects/observe! unborn-target {} intent)))))))))))

;; ── DoD: crashes leave nothing public and the next run converges ───────────

(deftest ^:async a-crash-between-artifact-and-manifest-leaves-nothing-public
  (await
   (with-root!
    (^:async fn [root]
      (testing "an artifact file no manifest entry names is not public"
        (.mkdirSync node-fs (.dirname path (.join path root artifact-path))
                    #js {:recursive true})
        (.writeFileSync node-fs (.join path root artifact-path)
                        (:artifact/content artifact) "utf8")
        (let [store (static-site/static-site-store root)
              target (target-in root)]
          (is (nil? (await (effects/observe! target {} intent)))
              "orphaned bytes are not a materialization")
          (testing "the next run converges: the same plan now publishes and
                    serves the expected bytes"
            (let [receipt (await (effects/execute-plan!
                                  store target {} publish-plan artifact))]
              (is (= :publication/materialized (:receipt/type receipt)))
              (is (= (:artifact/content artifact) (read-artifact root)))
              (is (some? (await (effects/observe! target {} intent))))))))))))

(deftest ^:async a-crash-after-reservation-reconciles-by-observation
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)
            key (effects/publish-idempotency-key
                 :knoxx.publication/static-site intent "probe-revision")
            reservation (effects/reserve! store key)]
        (is (= :reserved (:reservation/status reservation))
            "claimed, then the process 'died' before publishing")
        (let [receipt (await (effects/execute-plan!
                              store target {} publish-plan artifact))]
          (is (= :publication/materialized (:receipt/type receipt)))
          (is (= (:artifact/content artifact) (read-artifact root))))
        (testing "the retained claim now reports the recorded receipt"
          (is (= :done (:reservation/status (effects/reserve! store key))))))))))

(deftest ^:async an-interrupted-manifest-write-is-old-or-new-never-torn
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)]
        (await (effects/execute-plan! store target {} publish-plan artifact))
        (testing "a dead writer's temp file beside the manifest is never
                  mistaken for the manifest — the live manifest still parses
                  as wholly the old one"
          (.writeFileSync node-fs
                          (.join path root "manifest.edn.tmp-deadbeef")
                          "{:manifest/version 1 :manifest/routes [{")
          (let [manifest (read-manifest root)]
            (is (= 1 (count (:manifest/routes manifest))))
            (is (= "/probe" (:route/path (first (:manifest/routes manifest)))))))
        (testing "and the next write replaces it wholly"
          (let [new-plan (-> publish-plan
                             (assoc :concrete-revision "revision-two")
                             (assoc-in [:desired :materialized/revision] "revision-two"))
                new-artifact (assoc artifact
                                    :artifact/content "<!doctype html><p>Segunda</p>"
                                    :artifact/revision "revision-two")
                receipt (await (effects/execute-plan!
                                store target {} new-plan new-artifact))]
            (is (= :publication/materialized (:receipt/type receipt)))
            (let [manifest (read-manifest root)
                  routes (:manifest/routes manifest)]
              (is (= 1 (count routes)))
              (is (= "revision-two" (:route/revision (first routes))))
              (is (= "artifacts/knoxx.docs/probe/es/revision-two.html"
                     (:route/artifact (first routes)))))
            (testing "the superseded revision's bytes were reclaimed"
              (is (not (artifact-exists? root)))))))))))

;; ── Concurrency: two reconciler runs touching one root ────────────────────

(deftest ^:async concurrent-runs-converge-on-one-consistent-manifest
  (await
   (with-root!
    (^:async fn [root]
      (let [store (static-site/static-site-store root)
            target (target-in root)
            other-intent (-> intent
                             (assoc :publication/id :knoxx.docs/otra-es)
                             (assoc :publication/document :knoxx.docs/otra)
                             (assoc :publication/path "/otra"))
            plan-for (fn [an-intent]
                       {:op :publish
                        :intent an-intent
                        :desired {:materialized/revision "probe-revision"
                                  :materialized/path (:publication/path an-intent)}
                        :concrete-revision "probe-revision"})
            other-artifact (assoc artifact
                                  :artifact/content "<!doctype html><p>Otra</p>")
            receipts (await (js/Promise.all
                             #js [(effects/execute-plan!
                                   store target {} (plan-for intent) artifact)
                                  (effects/execute-plan!
                                   store target {} (plan-for other-intent)
                                   other-artifact)]))]
        (is (= [:publication/materialized :publication/materialized]
               (mapv :receipt/type receipts)))
        (let [manifest (read-manifest root)
              routes (:manifest/routes manifest)]
          (is (= 2 (count routes)) "both runs committed, neither update lost")
          (is (= #{"/probe" "/otra"} (set (map :route/path routes)))))
        (is (= (:artifact/content artifact) (read-artifact root)))
        (is (= (:artifact/content other-artifact)
               (.readFileSync node-fs
                              (.join path root
                                     "artifacts/knoxx.docs/otra/es/probe-revision.html")
                              "utf8"))))))))

;; ── The adapter validates what it is handed ────────────────────────────────

(deftest ^:async the-adapter-refuses-an-unlawful-artifact-before-writing
  (await
   (with-root!
    (^:async fn [root]
      (let [target (target-in root)
            stale (assoc artifact :artifact/revision "an-older-revision")
            outcome (try
                      (await (effects/publish! target {}
                                               {:intent intent
                                                :artifact stale
                                                :previous nil
                                                :concrete-revision "probe-revision"
                                                :idempotency/key "refused"}))
                      :published
                      (catch :default err
                        (if (some? (ex-data err)) :refused :threw-untyped)))]
        (is (= :refused outcome)
            "a revision-conflicting artifact must not publish")
        (is (not (manifest-exists? root)) "no commit for a refused artifact")
        (is (not (artifact-exists? root)) "and no bytes either"))))))

;; ── The adapter contains no policy ─────────────────────────────────────────

(deftest the-adapter-contains-no-admissibility-gating-or-planning-logic
  (let [source (.readFileSync
                node-fs
                (.join path (.cwd js/process)
                       "src/cljs/knoxx/backend/infra/publication_target_static_site.cljs")
                "utf8")]
    (testing "it validates what it is handed — the one check an adapter owns"
      (is (str/includes? source "assert-artifact!")))
    (doseq [policy ["publication-gate" "admissible-publication" "reconcile-plan"
                    "publication-plan" "publishes?"]]
      (testing (str "no reference to " policy)
        (is (not (str/includes? source policy)))))))

;; ── Registry wiring ────────────────────────────────────────────────────────

(deftest the-target-resolves-through-the-registry-as-its-kind
  (with-root-sync!
   (fn [root]
     (let [declaration {:publication-target/id :knoxx.publication/static-site
                        :publication-target/kind static-site/kind
                        :publication-target/config {:content-root root}
                        :publication-target/enabled? true}
           target-registry (registry/make-registry
                            [declaration]
                            {static-site/kind static-site/static-site-target})
           target (registry/resolve-target! target-registry
                                            :knoxx.publication/static-site)]
       (is (= :knoxx.publication/static-site (effects/target-id target)))))))

;; ── Idempotency store ──────────────────────────────────────────────────────

(deftest the-store-claims-atomically-and-remembers-completions
  (with-root-sync!
   (fn [root]
     (let [store (static-site/static-site-store root)
           receipt {:receipt/type :publication/materialized
                    :materialized/revision "probe-revision"
                    :materialized/path "/probe"
                    :idempotency/key "key-one"}]
       (is (= :reserved (:reservation/status (effects/reserve! store "key-one")))
           "one operation claims the key — the O_EXCL create IS the check")
       (is (= :in-flight (:reservation/status (effects/reserve! store "key-one")))
           "a second claim sees the first, with no window between read and claim")
       (effects/complete! store "key-one" receipt)
       (let [done (effects/reserve! store "key-one")]
         (is (= :done (:reservation/status done)))
         (is (= receipt (:receipt done))
             "the receipt round-trips through EDN with namespaced keys intact"))
       (effects/release! store "key-one")
       (is (= :reserved (:reservation/status (effects/reserve! store "key-one")))
           "a released claim may be taken again")
       (testing "an absurdly long or separator-laden key still maps to a safe,
                 deterministic filename"
         (let [ugly (apply str "k|\"|/" (repeat 512 "x"))]
           (is (= :reserved (:reservation/status (effects/reserve! store ugly))))
           (let [fresh-store (static-site/static-site-store root)]
             (is (= :in-flight (:reservation/status (effects/reserve! fresh-store ugly)))
                 "a second store instance lands on the SAME file — the mapping is deterministic")
             (effects/release! fresh-store ugly)
             (is (= :reserved (:reservation/status (effects/reserve! store ugly)))
                  "and releasing through it frees the key for the first"))))))))

(deftest ^:async a-stale-lock-does-not-wedge-the-next-run
  (await
   (with-root!
    (^:async fn [root]
      (testing "a lock abandoned by a crashed writer is taken over rather than
                failing every subsequent publication forever"
        (let [lock-path (.join path root "manifest.lock")]
          (.writeFileSync node-fs lock-path "dead-writer-token")
          (let [long-ago (js/Date. 946684800000)]
            (.utimesSync node-fs lock-path long-ago long-ago))
          (let [store (static-site/static-site-store root)
                target (target-in root)
                receipt (await (effects/execute-plan!
                                store target {} publish-plan artifact))]
            (is (= :publication/materialized (:receipt/type receipt)))
            (is (= (:artifact/content artifact) (read-artifact root)))
            (is (not (.existsSync node-fs lock-path))
                "and the lock was released behind it"))))))))
