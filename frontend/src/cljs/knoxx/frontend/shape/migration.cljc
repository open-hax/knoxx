(ns knoxx.frontend.shape.migration
  "Pure structural record construction for the frontend strangler manifest."
  (:require [clojure.string :as str]))

(defn normalize-path
  "Return a repository path with POSIX separators."
  [path]
  (str/replace path "\\" "/"))

(defn legacy-file-record
  "Construct one file record from classified migration attributes."
  [{:keys [path bridge tests disposition island blocked-by role]}]
  (let [path (normalize-path path)
        kind (if (str/ends-with? path ".tsx") :tsx :ts)]
    (cond-> {:record/id (str "file:" path)
             :path path
             :kind kind
             :island island
             :role role
             :disposition disposition
             :status :legacy
             :tests (vec (sort tests))
             :blocked-by blocked-by}
      bridge (assoc :bridge bridge))))

(defn bridge-export-record
  "Construct one record for a named compatibility-bridge export."
  [{:keys [bridge path source] :as attributes}]
  (let [export-name (:symbol attributes)]
    {:record/id (str "bridge:" (name bridge) ":" export-name)
     :path (normalize-path path)
     :kind :bridge-export
     :bridge bridge
     :symbol export-name
     :source source
     :status :legacy}))

(defn route-record
  "Construct one record for a Shadow-owned route and its current implementation."
  [{:keys [path route implementation legacy?]}]
  {:record/id (str "route:" route)
   :path (normalize-path path)
   :kind :route
   :route route
   :implementation implementation
   :status (if legacy? :legacy :native)})

(defn legacy-test-suite-record
  "Construct the explicit retirement record for one Vitest suite."
  [{:keys [path island disposition]}]
  {:record/id (str "legacy-test-suite:" (normalize-path path))
   :path (normalize-path path)
   :kind :legacy-test-suite
   :island island
   :disposition disposition
   :status :legacy})

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
