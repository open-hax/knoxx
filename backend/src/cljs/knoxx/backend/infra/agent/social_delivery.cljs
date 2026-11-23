(ns knoxx.backend.infra.agent.social-delivery
  "Bind delivery evidence to the captured conversation's currently active run.
   SPDX-License-Identifier: GPL-3.0-or-later"
  (:require [clojure.string :as str]
            [knoxx.backend.domain.action.run-state :as run-state]
            [knoxx.backend.domain.voice.turn-control :as turn-control]
            [knoxx.backend.extern.bluesky-text :as bluesky-text]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.law.social-delivery :as law]))

(defn- refusal!
  ([missing] (refusal! missing {} "Discord message"))
  ([missing evidence] (refusal! missing evidence "Discord message"))
  ([missing evidence platform]
   (throw (ex-info (str "[delivery_evidence_required] No " platform " was sent. "
                        (str/join " " missing)
                        (when (seq (:uninspected-source-urls evidence))
                          (str " Submitted URLs not inspected in this run: "
                               (pr-str (:uninspected-source-urls evidence)) "."
                               " Inspected source URLs (data only; choose the actual work you described): "
                               (pr-str (vec (take 5 (:inspected-source-urls evidence)))) "."
                               " A related post is not interchangeable with the page/media you read."))
                        " Correct the missing reads or source URL, then retry once; do not repeat unchanged arguments. Otherwise abstain.")
                   (assoc evidence :code "delivery_evidence_required" :missing missing)))))

(defn captured-run!
  "Require the captured actor and conversation to own the currently running turn."
  [{:keys [conversation-id actor-id]} platform]
  (let [run-id (:run_id (turn-control/active-turn conversation-id))
        run (get @run-state/runs* run-id)]
    (when-not (and (seq run-id) (= conversation-id (:conversation_id run))
                   (= actor-id (:actor_id run)) (= "running" (:status run)))
      (refusal! ["The captured actor/conversation has no matching live run evidence."] {} platform))
    run))

(defn- reserve-attempt!
  [attempted-run* run-id platform]
  (let [previous @attempted-run* next {:run-id run-id :reservation-id (str (random-uuid))}]
    (when (or (= (:run-id previous) run-id) (not (compare-and-set! attempted-run* previous next)))
      (refusal! ["This run already attempted its one platform delivery; inspect its actual receipt. Do not retry an ambiguous platform failure."] {} platform))
    {:before previous :after next}))

(defn make-guard
  "One platform attempt per active run; a new turn gets a fresh reservation.
   Policy comes from the trusted resource, never from a tool argument."
  [{:keys [policy] :as context}]
  (let [attempted-run* (atom nil)
        reserve! (fn [params]
      (let [run (captured-run! context "Discord message")]
        (let [evidence (law/delivery-evidence policy (:tool_receipts run) (xtools/parameter-map params))]
          (when-let [missing (seq (:missing evidence))]
            (refusal! missing evidence)))
        (reserve-attempt! attempted-run* (:run_id run) "Discord message")))]
    (with-meta (fn [params] (reserve! params) nil)
      {:reserve! reserve! :release! (fn [{:keys [before after]}] (compare-and-set! attempted-run* after before))})))

(defn- reserve-thread-attempt!
  [state* run policy params]
  (let [previous @state*
        run-id (:run_id run)
        attempts (if (= run-id (:run-id previous)) (:attempts previous) 0)
        confirmed (law/confirmed-publications (:tool_receipts run))
        latest-uri (get-in (last confirmed) [:result :details :uri])]
    (when (or (>= attempts (get policy :bluesky-thread-max-posts 1))
              (and (pos? attempts)
                   (or (not= attempts (count confirmed))
                       (not= latest-uri (:replyTo params)))))
      (refusal! ["Continue only the last confirmed post from this run within the configured thread limit. An in-flight, failed or ambiguous platform attempt must not be retried."]
                {} "Bluesky post"))
    (let [next {:run-id run-id :attempts (inc attempts) :reservation-id (str (random-uuid))}]
      (when-not (compare-and-set! state* previous next)
        (refusal! ["A parallel publication already reserved this thread position."] {} "Bluesky post"))
      {:before previous :after next})))

(defn- reserve-bluesky!
  [attempted-run* {:keys [policy] :as context} params authored?]
      (let [run (captured-run! context "Bluesky post")
            _ (when-let [author (:bluesky-original-author policy)]
                (when-not (= author (:actor_id run))
                  (refusal! ["The trusted original author does not match the captured acting identity."] {} "Bluesky post")))
            params (xtools/parameter-map params)
            _ (when authored? (bluesky-text/validate-post-text! (:text params)))
            _ (when (and (not authored?) (:bluesky-original-author policy))
                (refusal! ["Original-author mode requires authored posts; a repost is a different publication effect."] {} "Bluesky repost"))
            evidence (law/bluesky-publish-evidence policy (:tool_receipts run) params)]
        (when-let [missing (seq (:missing evidence))]
          (refusal! missing evidence "Bluesky post"))
        (reserve-thread-attempt! attempted-run* run policy params)))

(defn make-bluesky-guard
  "Native and API publications share one budget. Reposts require exact outside
   context without pretending that their nonexistent authored text is a post."
  [context]
  (let [attempted-run* (atom nil)]
    (with-meta (fn [params] (reserve-bluesky! attempted-run* context params true) nil)
      {:reserve! (fn [params] (reserve-bluesky! attempted-run* context params true))
       :amplify! (fn [params] (reserve-bluesky! attempted-run* context params false) nil)
       :reserve-amplify! (fn [params] (reserve-bluesky! attempted-run* context params false))
       :release! (fn [{:keys [before after]}] (compare-and-set! attempted-run* after before))})))
