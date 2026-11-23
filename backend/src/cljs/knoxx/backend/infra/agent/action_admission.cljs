(ns knoxx.backend.infra.agent.action-admission
  "Bind canonical write admission to the captured actor/conversation/run."
  (:require [knoxx.backend.domain.social.action-admission :as domain]
            [knoxx.backend.extern.bluesky-text :as bluesky-text]
            [knoxx.backend.infra.agent.social-delivery :as delivery]
            [knoxx.backend.law.platform-action :as law]))

(defn- reserve!
  [state* context intent]
  (law/assert-valid! law/Intent intent)
  (when (and (= "bluesky" (:platform intent)) (= :publish (:effect intent)))
    (bluesky-text/validate-post-text! (get-in intent [:arguments :text])))
  (let [run (delivery/captured-run! context "platform action")]
    (loop []
      (let [previous @state*
            next (domain/reserve previous (:run_id run) intent)]
        (when-not (:allowed? next)
          (throw (ex-info
                  (str "[platform_action_not_admitted] No new platform write was attempted: "
                       (name (:reason next)) ". Inspect the actual prior outcome; do not switch tool aliases.")
                  {:code "platform_action_not_admitted" :reason (:reason next) :phase :admission})))
        (if (compare-and-set! state* previous (:state next))
          (law/assert-valid! law/Reservation (:reservation next))
          (recur))))))

(defn- reserve-many!
  [state* context expected-run-id intents]
  (law/assert-valid! law/Intents intents)
  (let [run (delivery/captured-run! context "platform action batch")]
    (when-not (= expected-run-id (:run_id run))
      (throw (ex-info "The original turn ended before this request could be admitted"
                      {:code "platform_action_run_changed" :phase :preflight})))
    (loop []
      (let [previous @state* next (domain/reserve-many previous expected-run-id intents)]
        (when-not (:allowed? next)
          (throw (ex-info "Platform action batch was not admitted; inspect prior outcomes before changing aliases"
                          {:code "platform_action_not_admitted" :reason (:reason next) :phase :admission})))
        (if (compare-and-set! state* previous (:state next))
          (law/assert-valid! law/GroupReservation (:reservation next))
          (recur))))))

(defn create
  "One shared state for every native or schema-derived write in this session.
   This is per-run admission, not durable cross-run exactly-once delivery."
  [context]
  (law/assert-valid! law/Context context)
  (let [state* (atom nil)]
    {:reserve! (partial reserve! state* context)
     :reserve-many! (partial reserve-many! state* context)
     :settle-many! (fn [reservation outcomes]
                     (law/assert-valid! law/GroupReservation reservation)
                     (doseq [outcome outcomes] (law/assert-valid! law/Outcome outcome))
                     (swap! state* domain/settle-many reservation outcomes)
                     outcomes)
     :settle! (fn [reservation execution]
                (law/assert-valid! law/Reservation reservation)
                (let [result (domain/outcome (:intent reservation) execution)]
                  (law/assert-valid! law/Outcome result)
                  (swap! state* domain/settle reservation result)
                  result))}))
