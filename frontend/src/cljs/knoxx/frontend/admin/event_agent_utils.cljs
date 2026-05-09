(ns knoxx.frontend.admin.event-agent-utils
  "Pure utility functions extracted from the legacy DiscordSection.tsx.
   No React dependencies — just data transformation.")

(defn split-csv
  "Split a comma-separated string into trimmed, non-empty tokens."
  [value]
  (->> (clojure.string/split (str value) #",")
       (map clojure.string/trim)
       (filter seq)))

(defn join-csv
  "Join a collection into a comma-separated string."
  [values]
  (clojure.string/join ", " (or values [])))

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
  (some #(when (= (.-id %) job-id) %) runtime-jobs))

(defn seed-json-drafts
  "Build a map of job-id -> JSON draft strings from loaded jobs."
  [jobs]
  (into {}
        (map (fn [job]
               [(.-id job)
                {:source-config (pretty-json (or (.. job -source -config) {}))
                 :filters (pretty-json (or (.-filters job) {}))
                 :tool-policies (pretty-json (or (.. job -agentSpec -toolPolicies) []))}]))
        jobs))

(defn compact-text
  "Compact text to a max length, appending ellipsis if truncated."
  ([value] (compact-text value 120))
  ([value max]
   (let [normalized (-> (str (or value ""))
                        (clojure.string/replace #"\s+" " ")
                        clojure.string/trim)]
     (cond
       (empty? normalized) "No description"
       (<= (count normalized) max) normalized
       :else (str (subs normalized 0 (dec max)) "…")))))

(defn normalize-search
  "Lowercase and trim a search string."
  [value]
  (-> (str value)
      clojure.string/trim
      clojure.string/lower-case))

(defn job-search-text
  "Build a searchable text blob from a job definition."
  [job]
  (->> [(.-id job)
         (.-name job)
         (.-description job)
         (.. job -source -kind)
         (.. job -source -mode)
         (.. job -trigger -kind)
         (clojure.string/join " " (.. job -trigger -eventKinds))
         (.. job -agentSpec -role)
         (.. job -agentSpec -model)
         (.-contractSourceId job)]
       (filter identity)
       (clojure.string/join " ")
       clojure.string/lower-case))

(defn runtime-status-tone
  "Map a runtime status string to a UI tone keyword."
  [status]
  (case status
    "ok" :success
    "error" :danger
    "running" :info
    :default))
