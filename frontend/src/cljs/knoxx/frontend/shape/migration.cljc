(ns knoxx.frontend.shape.migration
  "Pure structural record construction for the frontend strangler manifest."
  (:require [clojure.string :as str]))

(def legacy-source-pattern
  "The production tree governed by the TypeScript non-growth ratchet."
  #"^frontend/src/.*\.tsx?$")

(def test-source-pattern
  "Pattern identifying governed Vitest source paths."
  #"\.(?:test|spec)\.tsx?$")

(defn normalize-path
  "Return a repository path with POSIX separators."
  [path]
  (str/replace path "\\" "/"))

(defn test-source?
  "Whether a governed TypeScript path is a Vitest suite."
  [path]
  (boolean (re-find test-source-pattern path)))

(defn file-role
  "Classify a governed file by its migration responsibility."
  [path]
  (cond
    (test-source? path) :test
    (str/includes? path "/bridge/") :bridge
    (str/includes? path "/src/lib/") :library
    (str/includes? path "/src/pages/") :route
    (str/includes? path "/src/components/") :component
    :else :support))

(defn legacy-file-record
  "Construct one file record from classified migration attributes."
  [{:keys [path bridge tests disposition island blocked-by]}]
  (let [path (normalize-path path)
        kind (if (str/ends-with? path ".tsx") :tsx :ts)]
    (cond-> {:record/id (str "file:" path)
             :path path
             :kind kind
             :island island
             :role (file-role path)
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
