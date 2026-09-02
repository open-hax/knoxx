(ns knoxx.frontend.migration.main
  "CLI composition root for writing and checking the frontend migration ledger."
  (:require [clojure.string :as str]
            [knoxx.frontend.infra.migration-manifest :as infra]
            [knoxx.frontend.law.migration :as law]))

(defn- fail! [message evidence]
  (.error js/console message)
  (when evidence (.error js/console (pr-str evidence)))
  (set! (.-exitCode js/process) 1))

(defn- check-current! [records rendered]
  (let [committed (infra/read-manifest)]
    (if (= committed rendered)
      true
      (do
        (fail! "Frontend migration manifest is stale; run `pnpm migration:write`."
               {:committed-records (count (infra/parse-records committed))
                :generated-records (count records)})
        false))))

(defn- check-ratchet! [records]
  (let [base-sha (or (.. js/process -env -KNOXX_MIGRATION_BASE_SHA) "")
        infrastructure? (= "true" (str/lower-case
                                    (or (.. js/process -env -KNOXX_MIGRATION_INFRASTRUCTURE)
                                        "false")))]
    (when (seq base-sha)
      (if-let [baseline (infra/base-manifest base-sha)]
        (let [baseline (law/assert-manifest! baseline)
              violations (law/ratchet-violations
                          {:baseline baseline
                           :current records
                           :changed-paths (infra/changed-paths base-sha)
                           :infrastructure? infrastructure?})]
          (when (seq violations)
            (fail! "Frontend migration ratchet failed." violations)))
        (when-not infrastructure?
          (fail! "The base revision has no migration manifest; this bootstrap PR must declare migration infrastructure."
                 {:base-sha base-sha}))))))

(defn main
  "Write or validate the migration ledger for the requested CLI command."
  []
  (try
    (let [command (or (aget (.-argv js/process) 2) "--check")
          records (infra/current-records)
          rendered (infra/render-records records)]
      (case command
        "--write" (do (infra/write-manifest! rendered)
                       (println (pr-str (law/records-summary records))))
        "--check" (when (check-current! records rendered)
                    (check-ratchet! records)
                    (when-not (= 1 (.-exitCode js/process))
                      (println (pr-str (law/records-summary records)))))
        (fail! "Usage: frontend-migration-manifest [--write|--check]" {:command command})))
    (catch :default error
      (fail! "Frontend migration manifest command failed."
             {:message (.-message error)
              :data (ex-data error)}))))
