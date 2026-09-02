(ns knoxx.frontend.law.migration
  "Malli contracts and monotonicity laws for the frontend migration ledger."
  (:require [clojure.set :as set]
            [knoxx.frontend.shape.migration :as shape]
            [malli.core :as m]
            [malli.error :as me]))

(def NonBlankString
  "Schema for nonempty manifest string values."
  [:string {:min 1}])
(def Bridge
  "Schema for compatibility-bridge identities."
  [:enum :frontend :app])
(def Island
  "Schema for migration behavior-island identities."
  [:enum :agent-audit :auth :bridge :broadcast-studio :chat-workspace :cms
   :components :contracts :data :event-agents :layout :ops :routes :shared
   :test-infrastructure :translations :workspace])
;; Reserved for generated TypeScript declarations explicitly admitted by the
;; final-state law; no such file exists in the current inventory.
(def Disposition
  "Schema for the terminal action assigned to a legacy record."
  [:enum :port :wrap :delete :retain-generated])
(def Status
  "Schema for current route or source ownership."
  [:enum :legacy :native])

(def LegacyFileRecord
  "Closed schema for one governed TypeScript source file."
  [:map {:closed true}
   [:record/id NonBlankString]
   [:path NonBlankString]
   [:kind [:enum :ts :tsx]]
   [:island Island]
   [:role [:enum :test :bridge :library :route :component :support]]
   [:disposition Disposition]
   [:bridge {:optional true} Bridge]
   [:status [:= :legacy]]
   [:tests [:vector string?]]
   [:blocked-by [:vector keyword?]]])

(def BridgeExportRecord
  "Closed schema for one compatibility-bridge export."
  [:map {:closed true}
   [:record/id NonBlankString]
   [:path NonBlankString]
   [:kind [:= :bridge-export]]
   [:bridge Bridge]
   [:symbol NonBlankString]
   [:source NonBlankString]
   [:status [:= :legacy]]])

(def RouteRecord
  "Closed schema for one Shadow-owned application route."
  [:map {:closed true}
   [:record/id NonBlankString]
   [:path NonBlankString]
   [:kind [:= :route]]
   [:route NonBlankString]
   [:implementation NonBlankString]
   [:status Status]])

(def LegacyTestSuiteRecord
  "Closed schema for one Vitest retirement record."
  [:map {:closed true}
   [:record/id NonBlankString]
   [:path NonBlankString]
   [:kind [:= :legacy-test-suite]]
   [:island Island]
   [:disposition Disposition]
   [:status [:= :legacy]]])

(def ManifestRecord
  "Closed union schema for every migration-ledger record."
  [:multi {:dispatch :kind}
   [:ts LegacyFileRecord]
   [:tsx LegacyFileRecord]
   [:bridge-export BridgeExportRecord]
   [:route RouteRecord]
   [:legacy-test-suite LegacyTestSuiteRecord]])

(defn assert-record!
  "Return a valid manifest record or throw with humanized schema evidence."
  [record]
  (if (m/validate ManifestRecord record)
    record
    (throw (ex-info "Frontend migration record violates its contract"
                    {:record record
                     :errors (me/humanize (m/explain ManifestRecord record))}))))

(defn assert-manifest!
  "Validate every record and require stable unique record identities."
  [records]
  (run! assert-record! records)
  (let [ids (map :record/id records)]
    (when-not (= (count ids) (count (set ids)))
      (throw (ex-info "Frontend migration record identities must be unique"
                      {:duplicate-ids (->> ids frequencies
                                           (keep (fn [[id n]] (when (> n 1) id)))
                                           sort vec)}))))
  records)

(defn records-summary
  "Derive the tracker summary; the ND-EDN records remain the only inventory."
  [records]
  {:files {:ts (count (filter #(= :ts (:kind %)) records))
           :tsx (count (filter #(= :tsx (:kind %)) records))}
   :bridge-exports (->> records (filter #(= :bridge-export (:kind %)))
                        (group-by :bridge)
                        (map (fn [[bridge exports]] [bridge (count exports)]))
                        (into (sorted-map)))
   :routes {:legacy (count (filter #(and (= :route (:kind %))
                                        (= :legacy (:status %))) records))
            :native (count (filter #(and (= :route (:kind %))
                                        (= :native (:status %))) records))}
   :legacy-test-suites (count (filter #(= :legacy-test-suite (:kind %)) records))
   :by-island (->> records
                   (keep (fn [record]
                           (when-let [island (:island record)] island)))
                   frequencies
                   (into (sorted-map)))})

(defn- records-of-kind [records kinds]
  (filter #(contains? kinds (:kind %)) records))

(defn- ids-of-kind [records kinds]
  (->> (records-of-kind records kinds) (map :record/id) set))

(defn- count-kind [records kind]
  (count (filter #(= kind (:kind %)) records)))

(defn legacy-surface-count
  "The migration surface whose monotonic decrease makes a behavior slice real."
  [records]
  (count (filter #(or (contains? #{:ts :tsx :bridge-export :legacy-test-suite}
                                 (:kind %))
                      (and (= :route (:kind %)) (= :legacy (:status %))))
                 records)))

(defn- ratchet-context
  [{:keys [baseline current changed-paths infrastructure?]}]
  (let [file-kinds #{:ts :tsx}
        baseline-files (ids-of-kind baseline file-kinds)
        current-files (ids-of-kind current file-kinds)
        baseline-exports (ids-of-kind baseline #{:bridge-export})
        current-exports (ids-of-kind current #{:bridge-export})
        baseline-native-routes (ids-of-kind
                                (filter #(= :native (:status %)) baseline)
                                #{:route})
        current-native-routes (ids-of-kind
                               (filter #(= :native (:status %)) current)
                               #{:route})]
    {:ts-before (count-kind baseline :ts)
     :ts-after (count-kind current :ts)
     :tsx-before (count-kind baseline :tsx)
     :tsx-after (count-kind current :tsx)
     :added-files (sort (set/difference current-files baseline-files))
     :added-exports (sort (set/difference current-exports baseline-exports))
     :regressed-routes (sort (set/difference baseline-native-routes
                                             current-native-routes))
     :touched? (some shape/migration-surface-path? changed-paths)
     :before (legacy-surface-count baseline)
     :after (legacy-surface-count current)
     :infrastructure? infrastructure?}))

(defn ratchet-violations
  "Return regressions; touched migration surfaces must shrink or declare infrastructure."
  [inputs]
  (let [{:keys [ts-before ts-after tsx-before tsx-after added-files
                added-exports regressed-routes touched? before after
                infrastructure?]} (ratchet-context inputs)]
    (cond-> []
      (> ts-after ts-before)
      (conj {:law :typescript-count/non-growth
             :before ts-before :after ts-after})

      (> tsx-after tsx-before)
      (conj {:law :tsx-count/non-growth
             :before tsx-before :after tsx-after})

      (seq added-files)
      (conj {:law :legacy-source/no-new-paths :added added-files})

      (seq added-exports)
      (conj {:law :bridge-exports/monotonic :added added-exports})

      (seq regressed-routes)
      (conj {:law :native-routes/no-regression :routes regressed-routes})

      (and touched? (>= after before) (not infrastructure?))
      (conj {:law :migration-slice/must-progress
             :before before
             :after after
             :declaration "Add `Migration infrastructure: yes` to the PR body only for infrastructure work."}))))
