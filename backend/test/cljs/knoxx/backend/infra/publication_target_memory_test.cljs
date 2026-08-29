(ns knoxx.backend.infra.publication-target-memory-test
  "The test double held to the adapter contract, and the one place the artifact's
  *production side* is pinned rather than described.

  Two claims live here because they are the same claim from both ends: an
  adapter validates what it is handed, and an adapter never produces what it
  transports."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.infra.publication-target-memory :as memory]
            ["node:fs" :as node-fs]
            ["node:path" :as path]))

;; ── Fixtures ───────────────────────────────────────────────────────────────

(def intent
  {:publication/id :knoxx.docs/probe-es
   :publication/document :knoxx.docs/probe
   :publication/garden :knoxx.docs/promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/probe"
   :translation/review :required
   :document/source-locale :en})

(def artifact
  {:artifact/content "<!doctype html><p>Sonda — contenido traducido</p>"
   :artifact/media-type "text/html"
   :artifact/encoding "utf-8"
   :artifact/locale :es
   :artifact/revision "probe-revision"})

(def publish-plan
  {:op :publish
   :intent intent
   :desired {:materialized/revision "probe-revision" :materialized/path "/probe"}
   :previous nil
   :concrete-revision "probe-revision"})

(defn- op-with
  "The op shape `execute-publish!` builds, so calling `publish!` directly here
   exercises the adapter against the same input production hands it."
  [candidate]
  {:intent intent
   :artifact candidate
   :previous nil
   :concrete-revision "probe-revision"
   :idempotency/key "probe-key"})

;; ── the adapter validates rather than storing anything ────────────────────

(deftest the-memory-target-validates-what-it-is-handed
  (testing "`:artifact` was stored unexamined and handed back, which is fine for
            recording metadata and useless for proving a publication: a dropped
            media type, a missing declared encoding, or no content at all became
            a SERVED route, and every assertion about the materialization — all
            of them reading receipt metadata — stayed green"
    (doseq [[label bad] [["absent entirely" nil]
                         ["no content" (dissoc artifact :artifact/content)]
                         ["no media type" (dissoc artifact :artifact/media-type)]
                         ["no declared encoding" (dissoc artifact :artifact/encoding)]
                         ["no locale" (dissoc artifact :artifact/locale)]
                         ["a revision selector"
                          (assoc artifact :artifact/revision :source/current)]
                         ["a selector hidden on another key"
                          (assoc artifact :render/from :source/current)]
                         ["a revision the op is not publishing"
                          (assoc artifact :artifact/revision "some-other-revision")]]]
      (let [bundle (memory/memory-target)]
        (testing label
          (is (thrown? js/Error (effects/publish! (:target bundle) {} (op-with bad))))
          (is (empty? (memory/public-routes bundle))
              "refused, not stored")
          (is (zero? (memory/materialization-count bundle))
              "and not counted as a materialization either"))))))

(deftest ^:async a-lawful-artifact-is-stored-and-served-unchanged
  (let [bundle (memory/memory-target)
        receipt (await (effects/publish! (:target bundle) {} (op-with artifact)))]
    (is (= :publication/materialized (:receipt/type receipt)))
    (is (= 1 (memory/materialization-count bundle)))
    (testing "stored EXACTLY as handed over — the adapter transports, so any
              difference here would be the adapter having opinions about content"
      (is (identical? artifact (memory/served-artifact bundle "/probe"))))))

;; ── the artifact is produced ABOVE the effect boundary ────────────────────

(defn- read-source
  [relative-path]
  (.readFileSync node-fs (.join path (.cwd js/process) relative-path) "utf8"))

(deftest ^:async the-artifact-is-produced-above-the-effect-boundary
  (testing "the boundary cannot supply one, so it must come from above: a
            :publish plan with no artifact is refused rather than rendered here"
    (let [{:keys [store]} (memory/memory-store)
          bundle (memory/memory-target)
          receipt (await (effects/execute-plan!
                          store (:target bundle) {} publish-plan nil))]
      (is (= :publication/failed (:receipt/type receipt)))
      (is (empty? (memory/public-routes bundle)))))

  (testing "and two independent adapters handed the SAME artifact serve the
            identical value. Produced BELOW the boundary each adapter would
            render from the intent it was given, and two targets publishing the
            same intent at the same revision could serve different bytes with
            nothing in the receipt chain able to say which is right"
    (let [first-bundle (memory/memory-target {:id :first/target})
          second-bundle (memory/memory-target {:id :second/target})]
      (await (effects/publish! (:target first-bundle) {} (op-with artifact)))
      (await (effects/publish! (:target second-bundle) {} (op-with artifact)))
      (is (= (memory/served-artifact first-bundle "/probe")
             (memory/served-artifact second-bundle "/probe")))
      (testing "identical?, not merely equal — an adapter that rendered its own
                copy would satisfy = and still be the divergence this forbids"
        (is (identical? artifact (memory/served-artifact first-bundle "/probe")))
        (is (identical? artifact (memory/served-artifact second-bundle "/probe"))))))

  (testing "no source at or below the boundary constructs artifact content"
    (doseq [relative-path ["src/cljs/knoxx/backend/infra/publication_effects.cljs"
                           "src/cljs/knoxx/backend/infra/publication_target_memory.cljs"]]
      (testing relative-path
        (is (not (str/includes? (read-source relative-path) ":artifact/content"))
            "an adapter that writes this key is rendering, which is the one thing
             the ownership decision forbids it from doing")))))
