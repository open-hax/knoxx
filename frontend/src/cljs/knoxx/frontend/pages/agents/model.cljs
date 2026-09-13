(ns knoxx.frontend.pages.agents.model
  "Pure contract editing and identity projections for the agent workbench."
  (:require [cljs.pprint :as pprint]
            [cljs.reader :as reader]
            [clojure.string :as str]))

(def trigger-kind-options
  "Trigger kinds accepted by the structured contract editor." ["manual" "event" "cron"])
(def source-kind-options
  "Source kinds offered by the structured contract editor." ["manual" "discord" "github" "webhook" "cron"])
(def thinking-options
  "Model reasoning settings offered by the editor." ["off" "minimal" "low" "medium" "high" "xhigh"])

(def default-agent-contract
  "Initial unsaved agent contract shown by the New agent action."
  {:contract/id "new-agent"
   :contract/kind :agent
   :contract/version 1
   :enabled true
   :trigger-kind :manual
   :source-kind :manual
   :source-mode :respond
   :agent {:roles []
           :model "gemma4:31b"
           :thinking :off}
   :prompts {:system ""
             :task ""}
   :events {:always []
            :maybe []}
   :data {}
   :hooks {}})

(defn keywordish->text
  "Render keywords with their namespace and symbols without losing identity." [value]
  (cond
    (keyword? value) (if-let [prefix (namespace value)]
                       (str prefix "/" (name value))
                       (name value))
    (symbol? value) (str value)
    (nil? value) ""
    :else (str value)))

(defn keywordish->plain
  "Render the local name of a keyword-like configuration value." [value]
  (let [text (keywordish->text value)]
    (-> text
        (str/replace #"^:" "")
        (str/replace #"^.*/" ""))))

(defn role-ref->id
  "Normalize a role reference to its selectable contract identifier." [value]
  (-> (keywordish->text value)
      (str/replace #"^:" "")
      (str/replace #"^role/" "")
      str/trim))

(defn role-id->keyword
  "Encode a selected contract identifier as a role reference." [role-id]
  (keyword "role" role-id))

(defn prompt-display
  "Render structured prompt values as EDN while preserving text prompts." [value]
  (cond
    (nil? value) ""
    (string? value) value
    :else (pr-str value)))

(defn draft->edn
  "Pretty-print a structured contract for its raw EDN editor." [draft]
  (with-out-str
    (pprint/pprint draft)))

(defn parse-contract-edn
  "Parse a raw contract and require a map for structured editing." [edn-text]
  (let [parsed (reader/read-string edn-text)]
    (when-not (map? parsed)
      (throw (js/Error. "Contract EDN must parse to a map.")))
    parsed))

(defn parse-int-or
  "Decode a finite integer or retain the supplied default." [value default-value]
  (let [n (js/parseInt (str value) 10)]
    (if (js/Number.isFinite n) n default-value)))

(defn normalize-contract-id
  "Trim a contract identifier and reject blank values." [value]
  (some-> value str str/trim not-empty))

(defn selected-role-ids
  "Collect both singular and plural role fields without duplicate choices." [draft]
  (let [agent (:agent draft)
        roles (:roles agent)
        role (:role agent)]
    (->> (concat (or roles [])
                 (cond
                   (sequential? role) role
                   role [role]
                   :else []))
         (map role-ref->id)
         (remove str/blank?)
         distinct
         vec)))

(defn with-selected-role-ids
  "Replace selected roles with canonical references and retire the singular field." [draft role-ids]
  (let [roles (->> role-ids
                   (remove str/blank?)
                   distinct
                   (mapv role-id->keyword))]
    (update draft :agent
            (fn [agent]
              (-> (or agent {})
                  (assoc :roles roles)
                  (dissoc :role))))))

(defn selected-agent-contract-id
  "Resolve a selected contract while excluding the unsaved placeholder."
  [selected-id draft]
  (or (some-> selected-id str str/trim not-empty)
      (let [draft-id (some-> (:contract/id draft) str str/trim not-empty)]
        (when-not (= draft-id "new-agent")
          draft-id))))
