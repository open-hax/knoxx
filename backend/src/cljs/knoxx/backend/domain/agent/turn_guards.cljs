(ns knoxx.backend.domain.agent.turn-guards
  "Pure guard calculations for agent turn streaming.")

(def default-death-spiral-streak-limit 6)
(def default-death-spiral-total-limit 12)

(def empty-tool-loop-state
  {:last nil
   :streak 0
   :counts {}})

(defn observe-tool-call
  "Update repeated-tool-call state and return an abort decision.

   `guard-state` is a map like {:last signature :streak n :counts {signature n}}.
   `event` accepts :tool-name, :tool-call-id, :input-preview, :aborting?, and
   optional :streak-limit/:total-limit overrides."
  [guard-state {:keys [tool-name tool-call-id input-preview aborting? streak-limit total-limit]}]
  (let [{:keys [last streak counts]} (merge empty-tool-loop-state guard-state)
        streak-limit (or streak-limit default-death-spiral-streak-limit)
        total-limit (or total-limit default-death-spiral-total-limit)
        signature (str tool-name "::" (or input-preview ""))
        next-total (inc (get counts signature 0))
        next-counts (assoc counts signature next-total)
        next-streak (if (= signature last) (inc streak) 1)
        abort? (and (not aborting?)
                    (or (>= next-streak streak-limit)
                        (>= next-total total-limit)))
        reason (when abort?
                 (str "death_spiral_detected: tool '" tool-name "' repeated "
                      next-total "x (streak " next-streak ")"))]
    {:state {:last signature
             :streak next-streak
             :counts next-counts}
     :abort? (boolean abort?)
     :reason reason
     :signature signature
     :tool-name tool-name
     :tool-call-id tool-call-id
     :count next-total
     :streak next-streak}))
