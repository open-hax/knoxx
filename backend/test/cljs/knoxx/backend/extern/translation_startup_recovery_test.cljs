(ns knoxx.backend.extern.translation-startup-recovery-test
  "Actual translation reconciliation refuses old startup ownership and completes a fresh attempt."
  (:require [cljs.test :as test]
            [knoxx.backend.extern.event-queue-fixture :as queue]
            [knoxx.backend.extern.provider-recovery-fixture :as disk]
            [knoxx.backend.infra.agent.run-admission :as admission]
            [knoxx.backend.infra.agent.runner :as runner]
            [knoxx.backend.infra.agent.startup-settlement :as startup]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.clio-run-store :as clio]
            [knoxx.backend.infra.stores.session-store-registry :as registry]
            [knoxx.backend.infra.translation-agent-dispatch :as dispatch]
            [knoxx.backend.infra.translation-agent-sink :as sink]
            [knoxx.backend.infra.translation-evidence-store :as evidence]
            [knoxx.backend.infra.translation-split-store :as splits]
            [knoxx.backend.law.translation-agent :as agent-law]
            [knoxx.backend.law.translation-split :as split-law]
            [knoxx.backend.shape.session-persistence :as runs]))

(def ^:private work {:document :proof/doc :locale :de :revision "sha256-source" :replace-stale? false})
(def ^:private context {:dispatch/garden "proof/garden" :dispatch/document-wire-id "proof/doc"
                       :dispatch/source-locale :en :dispatch/org-id "proof-org" :dispatch/project "proof"
                       :dispatch/membership-id "proof-member" :dispatch/source-digest "sha256-source"})
(defn- digest [value] (str "hash-" (hash value)))

(defn- ^:async complete-translation! [deps turn]
  (let [record (await (evidence/dispatch-for-batch! (:evidence-store deps) (:translation-turn/run-id turn)))
        policies (agent-law/session-policies record turn)]
    (doseq [[index member] (map-indexed vector (get-in turn [:translation-turn/manifest :split-manifest/splits]))]
      (await (sink/submit-pair!
              deps policies
              {:source_text (:split/source-text member) :translated_text (str "Übersetzt: " (:split/source-text member))
               :segment_index index :split_id (:split/id member)
               :attempt_id (get-in turn [:translation-turn/candidate-claim :candidate-claim/members index
                                        :candidate-claim-member/attempt-id])})))))

(defn- ^:async emit! [deps complete? models* event]
  (let [turn (await (splits/turn-by-id! (:split-store deps) (get-in event [:event/payload :turn-id])))
        id (:translation-turn/run-id turn)
        config {startup/reservation-key (str (random-uuid))}
        body {:run-id id :session-id id :conversation-id id :model "owned-model" :message "Translate."
              :auth-context {:org-id "proof-org"}
              :agent-spec {:event-id (:event/id event) :trigger-id "publication-translation"}}]
    (await (runner/enqueue-event-turn!
            config body
            (^:async fn []
              (await (admission/create-initial-run! id id id "2026-09-20T00:00:00.000Z" "owned-model"
                                                    "direct" "off" nil {:org_id "proof-org"} [] config))
              (when complete? (swap! models* inc) (await (complete-translation! deps turn))))))
    {:matchedTriggers [:publication/translation-needed]}))

(defn- proof-deps [directory]
  {:content-root directory :evidence-store (evidence/memory-store) :split-store (splits/memory-store digest)
   :digest-hex digest :clock (constantly "2026-09-20T00:00:00.000Z")
   :observe-source-revision (fn [_] "sha256-source") :emit-candidate-events! (fn [_] true)
   :translation-execution (split-law/execution-snapshot
                           digest {:agent-id "publication_translator" :model "owned-model" :thinking :off
                                   :system-prompt "Translate all admitted splits." :tool-ids ["save_translation"]})})

(test/deftest ^:async admitted-restart-refusal-becomes-retriable-and-next-attempt-completes
  (await (queue/with-queue!
          (^:async fn []
            (let [directory (disk/temporary-directory) deps (proof-deps directory) models* (atom 0)
                  source "Source text for a deterministic translation.\n"]
              (try
                (let [first-pass (await (dispatch/dispatch-work!
                                         (assoc deps :emit! #(emit! deps false models* %)) work context source))
                      old-id (:translation/run-id first-pass) provider @registry/session-store*]
                  (await (queue/wait-idle!))
                  (test/is (= :dispatch/accepted (:dispatch/outcome first-pass)))
                  (let [old-run (await (runs/get-run provider old-id)) history (await (runs/events-since provider old-id nil))]
                    (test/is (= "running" (:status old-run)))
                    (test/is (= ["event_turn_queued" "event_turn_started" "run_started"] (mapv :type history)))
                    (runner/reset-event-turn-queue!) (runner/reset-event-turn-settlers!)
                    (let [reopened (clio/open! {:directory (:directory provider)})
                          resumed (assoc deps :emit! #(emit! deps true models* %))]
                      (reset! registry/session-store* reopened) (events/install! reopened)
                      (let [refused (await (dispatch/dispatch-work! resumed work context source))]
                        (test/is (= :dispatch/failed (:dispatch/outcome refused)))
                        (test/is (= :dispatch/failed (:dispatch/outcome
                                                     (await (evidence/dispatch-for-batch! (:evidence-store deps) old-id)))))
                        (test/is (zero? @models*)))
                      (let [fresh (await (dispatch/dispatch-work! resumed work context source))]
                        (await (queue/wait-idle!))
                        (test/is (= :dispatch/accepted (:dispatch/outcome fresh)))
                        (test/is (not= old-id (:translation/run-id fresh)))
                        (test/is (= 1 @models*))
                        (test/is (= :dispatch/completed (:dispatch/outcome
                                                        (await (evidence/dispatch-for-batch!
                                                                (:evidence-store deps) (:translation/run-id fresh))))))
                        (test/is (= 1 (count (await (evidence/completed-translations!
                                                   (:evidence-store deps) {:org-id "proof-org" :project "proof"}))))))
                      (test/is (= old-run (await (runs/get-run reopened old-id))))
                      (test/is (= history (await (runs/events-since reopened old-id nil)))))))
                (finally (disk/remove! directory))))))))
