(ns knoxx.frontend.admin.event-agent-utils
  "Pure utility functions extracted from the legacy DiscordSection.tsx.
   No React dependencies — just data transformation.

   All functions operate on CLJS data (keyword keys, plain maps/vectors)."
  (:require [clojure.string :as str]))

(defn split-csv
  "Split a comma-separated string into trimmed, non-empty tokens."
  [value]
  (->> (str/split (str value) #",")
       (map str/trim)
       (filter seq)))

(defn join-csv
  "Join a collection into a comma-separated string."
  [values]
  (str/join ", " (or values [])))

(defn pretty-json
  "Pretty-print a value as JSON."
  [value]
  (js/JSON.stringify (clj->js (or value {})) nil 2))

(defn to-local-date-time
  "Format a timestamp as a locale string, or '—' if invalid."
  [value]
  (if (and value (js/Number.isFinite value))
    (try
      (.toLocaleString (js/Date. value))
      (catch js/Error _
        (str value)))
    "—"))

(defn runtime-for-job
  "Find the runtime job matching a given job id."
  [runtime-jobs job-id]
  (some #(when (= (:id %) job-id) %) runtime-jobs))

(defn seed-json-drafts
  "Build a map of job-id -> JSON draft strings from loaded jobs."
  [jobs]
  (into {}
        (map (fn [job]
               [(:id job)
                {:source-config (pretty-json (or (get-in job [:source :config]) {}))
                 :filters (pretty-json (or (:filters job) {}))
                 :tool-policies (pretty-json (or (get-in job [:agentSpec :toolPolicies]) []))}]))
        jobs))

(defn compact-text
  "Compact text to a max length, appending ellipsis if truncated."
  ([value] (compact-text value 120))
  ([value max]
   (let [normalized (-> (str (or value ""))
                         (str/replace #"\s+" " ")
                         str/trim)]
     (cond
       (empty? normalized) "No description"
       (<= (count normalized) max) normalized
       :else (str (subs normalized 0 (dec max)) "…")))))

(defn normalize-search
  "Lowercase and trim a search string."
  [value]
  (-> (str value)
      str/trim
      str/lower-case))

(defn job-search-text
  "Build a searchable text blob from a job definition."
  [job]
  (->> [(:id job)
         (:name job)
         (:description job)
         (get-in job [:source :kind])
         (get-in job [:source :mode])
         (get-in job [:trigger :kind])
         (str/join " " (get-in job [:trigger :eventKinds]))
         (get-in job [:agentSpec :role])
         (get-in job [:agentSpec :model])
         (:contractSourceId job)]
       (filter identity)
       (str/join " ")
       str/lower-case))

(defn runtime-status-tone
  "Map a runtime status string to a UI tone keyword."
  [status]
  (case status
    "ok" :success
    "error" :danger
    "running" :info
    :default))
