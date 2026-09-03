(ns knoxx.backend.infra.translation-agent-structured-output-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.json :as xjson]
            [knoxx.backend.infra.translation-agent-sink :as sink]
            [knoxx.backend.infra.translation-agent-structured-output :as sut]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-split :as split-law]))

(def ^:private config
  {:contracts-dir "test/fixtures/model-contracts"
   :ollama-base-url "http://127.0.0.1:11434/v1/"
   :translation-agent-structured-output-timeout-ms 420000})

(def ^:private work
  {:document :open-hax.documents/structured-output
   :locale :de
   :revision "sha256-structured-source"
   :replace-stale? false})

(def ^:private context
  {:dispatch/garden "open-hax.gardens/promethean"
   :dispatch/document-wire-id "open-hax.documents/structured-output"
   :dispatch/source-locale :en
   :dispatch/org-id "open-hax"
   :dispatch/project "promethean"
   :dispatch/membership-id "member-1"
   :dispatch/source-digest "sha256-structured-source"})

(def ^:private at "2026-09-02T12:00:00.000Z")
(def ^:private run-id "translation-run-structured-output")
(def ^:private source-parts
  ["First source paragraph.\n\n" "Second source paragraph.\n"])
(def ^:private translations
  ["Erster Quellabsatz.\n\n" "Zweiter Quellabsatz.\n"])

(defn- digest-hex
  [value]
  (str "h" (hash value)))

(def ^:private memory-example
  {:translation-memory/id "translation-memory-reviewed-1"
   :translation-memory/org-id "open-hax"
   :translation-memory/project "promethean"
   :translation-memory/garden :open-hax.gardens/promethean
   :translation-memory/document :open-hax.documents/older
   :translation-memory/source-locale :en
   :translation-memory/target-locale :de
   :translation-memory/manifest-id "translation-manifest-older"
   :translation-memory/source-revision "sha256-older-source"
   :translation-memory/source-digest "digest-older-source"
   :translation-memory/candidate-set-id "translation-candidate-set-older"
   :translation-memory/candidate-set-digest "digest-older-set"
   :translation-memory/candidate-revision "translation-output-older"
   :translation-memory/split-id "translation-split-older"
   :translation-memory/split-source-digest "digest-older-split"
   :translation-memory/candidate-attempt-id "translation-attempt-older"
   :translation-memory/candidate-digest "digest-older-candidate"
   :translation-memory/source-text "Source term"
   :translation-memory/target-text "Quellbegriff"
   :translation-memory/review-receipt-id "translation-review-older"})

(defn- manifest
  [parts]
  (split-law/split-manifest
   digest-hex
   {:org-id "open-hax"
    :project "promethean"
    :garden :open-hax.gardens/promethean
    :document :open-hax.documents/structured-output
    :source-locale :en
    :target-locale :de
    :source-revision (:revision work)
    :source-text (apply str parts)
    :source-parts parts}))

(defn- admitted-turn
  [record model parts memory]
  (let [manifest (manifest parts)
        claim (split-law/candidate-claim
               digest-hex manifest (dispatch-law/output-revision record))]
    (split-law/translation-turn-admission
     digest-hex
     {:dispatch-key (:dispatch/key record)
      :run-id run-id
      :admitted-at at
      :manifest manifest
      :candidate-claim claim
      :execution (split-law/execution-snapshot
                  digest-hex
                  {:agent-id "publication_translator"
                   :model model
                   :thinking :off
                   :system-prompt "Use the admitted translation policy exactly."
                   :tool-ids ["save_translation"]
                   :tools-choice :required-first})
      :memory memory})))

(defn- ^:async admitted!
  ([] (admitted! {}))
  ([{:keys [model parts memory]
     :or {model "gemma4:e2b"
          parts source-parts
          memory (split-law/memory-snapshot
                  {:status :found :examples [memory-example]})}}]
   (let [evidence (evidence-store/memory-store)
         splits (split-store/memory-store digest-hex)
         record (dispatch-law/dispatch-record
                 work context :dispatch/accepted at
                 :attempt-id "dispatch-attempt-structured-output")]
     (await (evidence-store/reserve-dispatch! evidence record))
     (let [bound (await (evidence-store/bind-dispatch-batch!
                         evidence record run-id))
           turn (admitted-turn bound model parts memory)]
       (await (split-store/admit-turn! splits turn))
       {:evidence evidence :splits splits :record bound :turn turn}))))

(defn- base-deps
  ([root state]
   (base-deps root state (fn [_] (js/Promise.resolve {:ok true}))))
  ([root {:keys [evidence splits]} emit-candidate-events!]
   {:content-root root
    :evidence-store evidence
    :split-store splits
    :digest-hex digest-hex
    :now-ms (constantly 0)
    :clock (constantly "2026-09-02T12:05:00.000Z")
    :emit-candidate-events! emit-candidate-events!
    :observe-source-revision
    (fn [_] (js/Promise.resolve "sha256-structured-source"))}))

(defn- ollama-response
  [translated-text]
  {:ok true
   :status 200
   :body {:model "gemma4:e2b"
          :done true
          :done_reason "stop"
          :message {:role "assistant"
                    :content (xjson/stringify
                              {:translated_text translated-text})}}})

(defn- pair
  [turn index translated-text]
  (let [source-split (get-in turn [:translation-turn/manifest
                                   :split-manifest/splits index])
        member (get-in turn [:translation-turn/candidate-claim
                             :candidate-claim/members index])]
    {:source_text (:split/source-text source-split)
     :translated_text translated-text
     :source_lang "en"
     :target_lang "de"
     :document_id "open-hax.documents/structured-output"
     :garden_id "open-hax.gardens/promethean"
     :org_id "open-hax"
     :segment_index index
     :split_id (:split/id source-split)
     :attempt_id (:candidate-claim-member/attempt-id member)}))

(deftest ^:async one-native-schema-call-is-made-for-each-missing-split
  (let [{:keys [turn splits] :as state} (await (admitted!))
        requests (atom [])
        request! (fn [request]
                   (let [index (count @requests)]
                     (swap! requests conj request)
                     (js/Promise.resolve
                      (ollama-response (nth translations index)))))
        deps (assoc (base-deps
                     "/tmp/knoxx-translation-structured-output/native" state)
                    :request! request!)
        result (await (sut/complete-turn! config deps (:record state) turn))
        candidates (await (split-store/candidate-splits-for-turn!
                           splits (:translation-turn/id turn)))]
    (testing "only translated_text crosses back from deterministic native chat"
      (is (some? (:translation/receipt result)))
      (is (= translations (mapv :candidate/text candidates)))
      (is (= 2 (count @requests))))
    (doseq [[index request] (map-indexed vector @requests)]
      (let [payload (get-in request [:opts :json])
            messages (:messages payload)
            input (xjson/parse-object (:content (second messages)))
            expected-split (get-in turn [:translation-turn/manifest
                                         :split-manifest/splits index])]
        (testing "the native request is deterministic and schema-closed"
          (is (= "http://127.0.0.1:11434/api/chat" (:url request)))
          (is (= 420000 (:timeout-ms request)))
          (is (= "gemma4:e2b" (:model payload)))
          (is (false? (:stream payload)))
          (is (false? (:think payload)))
          (is (= {:temperature 0 :seed 0} (:options payload)))
          (is (= {:type "object"
                  :additionalProperties false
                  :properties
                  {:translated_text {:type "string" :minLength 1}}
                  :required ["translated_text"]}
                 (:format payload))))
        (testing "the admitted prompt, exact split, locales, and memory are pinned"
          (is (= [{:role "system"
                   :content "Use the admitted translation policy exactly."}
                  (:role (second messages))]
                 [(first messages) "user"]))
          (is (= "en" (:source_locale input)))
          (is (= "de" (:target_locale input)))
          (is (= {:split_id (:split/id expected-split)
                  :segment_index index
                  :source_text (:split/source-text expected-split)}
                 (:split input)))
          (is (= [{:memory_id "translation-memory-reviewed-1"
                   :review_receipt_id "translation-review-older"
                   :candidate_digest "digest-older-candidate"
                   :source_locale "en"
                   :target_locale "de"
                   :source_text "Source term"
                   :translated_text "Quellbegriff"}]
                 (:reviewed_memory_examples input))))))))

(deftest ^:async production-event-timeout-caps-structured-completion
  (let [{:keys [turn] :as state}
        (await (admitted! {:parts [(first source-parts)]}))
        requests (atom [])
        production-config (-> config
                              (dissoc :translation-agent-structured-output-timeout-ms)
                              (assoc :event-agent-turn-timeout-ms 300000
                                     :agent-turn-timeout-ms 0))
        deps (assoc
              (base-deps
               "/tmp/knoxx-translation-structured-output/event-timeout"
               state)
              :request!
              (fn [request]
                (swap! requests conj request)
                (js/Promise.resolve
                 (ollama-response (first translations)))))
        result (await (sut/complete-turn!
                       production-config deps (:record state) turn))]
    (is (some? (:translation/receipt result)))
    (is (= [300000] (mapv :timeout-ms @requests)))))

(deftest ^:async every-missing-split-shares-one-completion-deadline
  (let [{:keys [turn splits] :as state} (await (admitted!))
        requests (atom [])
        times (atom [1000 1000 2000 301001])
        production-config (-> config
                              (dissoc :translation-agent-structured-output-timeout-ms)
                              (assoc :event-agent-turn-timeout-ms 300000
                                     :agent-turn-timeout-ms 0))
        deps (assoc
              (base-deps
               "/tmp/knoxx-translation-structured-output/shared-deadline"
               state)
              :now-ms (fn []
                        (let [value (first @times)]
                          (swap! times subvec 1)
                          value))
              :request!
              (fn [request]
                (swap! requests conj request)
                (js/Promise.resolve
                 (ollama-response (nth translations (dec (count @requests)))))))
        error (try
                (await (sut/complete-turn!
                        production-config deps (:record state) turn))
                nil
                (catch :default err err))]
    (is (= :completion-timeout
           (:translation-agent-structured-output/error (ex-data error))))
    (is (= [300000] (mapv :timeout-ms @requests))
        "the expired shared budget prevents a fresh timeout for split two")
    (is (= 1 (count (await (split-store/candidate-splits-for-turn!
                            splits (:translation-turn/id turn))))))))

(deftest ^:async provider-that-ignores-request-timeout-is-still-bounded
  (let [{:keys [turn splits] :as state}
        (await (admitted! {:parts [(first source-parts)]}))
        bounded-config (-> config
                           (dissoc :translation-agent-structured-output-timeout-ms)
                           (assoc :event-agent-turn-timeout-ms 10
                                  :agent-turn-timeout-ms 0))
        deps (assoc
              (base-deps
               "/tmp/knoxx-translation-structured-output/provider-hang"
               state)
              :request! (fn [_]
                          (js/Promise. (fn [_resolve _reject]))))
        error (try
                (await (sut/complete-turn!
                        bounded-config deps (:record state) turn))
                nil
                (catch :default err err))]
    (is (= :completion-timeout
           (:translation-agent-structured-output/error (ex-data error))))
    (is (= 10 (:timeout-ms (ex-data error))))
    (is (empty? (await (split-store/candidate-splits-for-turn!
                        splits (:translation-turn/id turn)))))))

(deftest ^:async explicit-structured-timeout-can-tighten-the-event-budget
  (let [{:keys [turn] :as state}
        (await (admitted! {:parts [(first source-parts)]}))
        requests (atom [])
        tighter-config (assoc config
                              :event-agent-turn-timeout-ms 300000
                              :translation-agent-structured-output-timeout-ms
                              120000)
        deps (assoc
              (base-deps
               "/tmp/knoxx-translation-structured-output/tighter-timeout"
               state)
              :request!
              (fn [request]
                (swap! requests conj request)
                (js/Promise.resolve
                 (ollama-response (first translations)))))
        result (await (sut/complete-turn! tighter-config deps (:record state) turn))]
    (is (some? (:translation/receipt result)))
    (is (= [120000] (mapv :timeout-ms @requests)))))

(deftest ^:async durable-prefix-skips-provider-work-and-keeps-first-bytes
  (let [{:keys [record turn splits] :as state} (await (admitted!))
        root "/tmp/knoxx-translation-structured-output/prefix"
        runtime-deps (base-deps root state)
        policies (agent-law/session-policies record turn)]
    (await (sink/submit-pair! runtime-deps policies
                              (pair turn 0 (first translations))))
    (let [requests (atom [])
          deps (assoc runtime-deps :request!
                      (fn [request]
                        (swap! requests conj request)
                        (js/Promise.resolve
                         (ollama-response (second translations)))))
          result (await (sut/complete-turn! config deps record turn))
          candidates (await (split-store/candidate-splits-for-turn!
                             splits (:translation-turn/id turn)))
          requested-input
          (xjson/parse-object
           (get-in @requests [0 :opts :json :messages 1 :content]))]
      (is (some? (:translation/receipt result)))
      (is (= 1 (count @requests)))
      (is (= (second source-parts)
             (get-in requested-input [:split :source_text])))
      (is (= translations (mapv :candidate/text candidates))))))

(deftest ^:async malformed-or-incomplete-native-output-never-reaches-the-sink
  (let [cases
        [[:http-response-failed
          {:ok false :status 503 :body {:error "unavailable"}}]
         [:response-model-mismatch
          (assoc-in (ollama-response "Übersetzt") [:body :model]
                    "gemma4:e4b")]
         [:completion-incomplete
          (assoc-in (ollama-response "Übersetzt") [:body :done_reason]
                    "length")]
         [:structured-output-invalid
          (assoc-in (ollama-response "Übersetzt") [:body :message :content]
                    "save_translation({\"translated_text\":\"Übersetzt\"})")]
         [:structured-output-keys-invalid
          (assoc-in (ollama-response "Übersetzt") [:body :message :content]
                    "{\"translated_text\":\"Übersetzt\",\"split_id\":\"forged\"}")]
         [:translated-text-blank
          (assoc-in (ollama-response "Übersetzt") [:body :message :content]
                    "{\"translated_text\":\"   \"}")]]]
    (loop [remaining cases
           index 0]
      (when-let [[expected response] (first remaining)]
        (let [{:keys [turn splits] :as state} (await (admitted!))
              deps (assoc
                    (base-deps
                     (str "/tmp/knoxx-translation-structured-output/invalid-"
                          index)
                     state)
                    :request! (fn [_] (js/Promise.resolve response)))
              error (try
                      (await (sut/complete-turn! config deps (:record state)
                                                 turn))
                      nil
                      (catch :default err err))]
          (is (= expected
                 (:translation-agent-structured-output/error (ex-data error))))
          (is (empty? (await (split-store/candidate-splits-for-turn!
                              splits (:translation-turn/id turn)))))
          (recur (next remaining) (inc index)))))))

(deftest ^:async exact-non-ollama-model-is-rejected-before-http
  (let [{:keys [turn splits] :as state}
        (await (admitted! {:model "gemma4:31b"}))
        requests (atom 0)
        deps (assoc
              (base-deps
               "/tmp/knoxx-translation-structured-output/non-ollama" state)
              :request! (fn [_]
                          (swap! requests inc)
                          (js/Promise.resolve
                           (ollama-response "should not run"))))
        error (try
                (await (sut/complete-turn! config deps (:record state) turn))
                nil
                (catch :default err err))]
    (is (= :model-provider-mismatch
           (:translation-agent-structured-output/error (ex-data error))))
    (is (zero? @requests))
    (is (empty? (await (split-store/candidate-splits-for-turn!
                        splits (:translation-turn/id turn)))))))

(deftest ^:async sink-refusal-fails-the-turn-instead-of-becoming-completion
  (let [only-source ["This source paragraph must actually be translated.\n"]
        {:keys [turn splits] :as state}
        (await (admitted!
                {:parts only-source
                 :memory (split-law/memory-snapshot
                          {:status :empty :examples []})}))
        deps (assoc
              (base-deps
               "/tmp/knoxx-translation-structured-output/refusal" state)
              :request! (fn [_]
                          (js/Promise.resolve
                           (ollama-response (first only-source)))))
        error (try
                (await (sut/complete-turn! config deps (:record state) turn))
                nil
                (catch :default err err))]
    (is (= :pair-translation-untranslated
           (:refusal/type (ex-data error))))
    (is (empty? (await (split-store/candidate-splits-for-turn!
                        splits (:translation-turn/id turn)))))))

(deftest ^:async identical-final-pair-replay-repairs-completed-event-projection
  (let [{:keys [record turn evidence] :as state}
        (await (admitted!
                {:parts [(first source-parts)]
                 :memory (split-law/memory-snapshot
                          {:status :empty :examples []})}))
        requests (atom 0)
        projections (atom [])
        emit! (fn [projection]
                (swap! projections conj projection)
                (if (< (count @projections) 3)
                  (js/Promise.reject (js/Error. "projection unavailable"))
                  (js/Promise.resolve {:ok true})))
        deps (assoc
              (base-deps
               "/tmp/knoxx-translation-structured-output/event-repair"
               state emit!)
              :request! (fn [_]
                          (swap! requests inc)
                          (js/Promise.resolve
                           (ollama-response (first translations)))))
        first-error
        (try
          (await (sut/complete-turn! config deps record turn))
          nil
          (catch :default err err))]
    (testing "the final sink exception gets one same-pair replay"
      (is (= "projection unavailable" (ex-message first-error)))
      (is (= 2 (count @projections)))
      (is (= (first @projections) (second @projections)))
      (is (= 1 @requests))
      (is (= 1 (count (await (evidence-store/completed-translations!
                              evidence
                              {:org-id "open-hax" :project "promethean"}))))))
    (testing "a restart trusts durable candidates, not completed state alone"
      (let [result (await (sut/complete-turn! config deps record turn))]
        (is (some? (:translation/receipt result)))
        (is (= 3 (count @projections)))
        (is (= (first @projections) (last @projections)))
        (is (= 1 @requests)
            "repair does not ask Ollama to translate durable bytes again")))))
