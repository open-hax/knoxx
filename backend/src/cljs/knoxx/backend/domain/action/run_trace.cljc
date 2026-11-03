(ns knoxx.backend.domain.action.run-trace
  "Pure text replacement, tool-event projection and input enrichment for run traces."
  (:require [clojure.string :as str]))

(defn append-limited
  "Append one value and retain the most recent limit entries."
  [items item limit]
  (let [values (conj (vec items) item)]
    (if (> (count values) limit)
      (subvec values (- (count values) limit))
      values)))

(defn next-text-id
  "Allocate a text-block ID without reusing surviving IDs after tail deletion."
  [blocks kind]
  (let [ids (set (map :id blocks))]
    (loop [index (count blocks)]
      (let [candidate (str (name kind) ":" index)]
        (if (contains? ids candidate) (recur (inc index)) candidate)))))

(defn- replace-text-block [block remaining replacement at]
  (let [prefix (subs (:content block) 0 remaining)
        content (str prefix replacement)]
    (when (seq content)
      (cond-> (assoc block :content content)
        (seq replacement) (assoc :status "streaming" :at at)))))

(defn- project-text-replacement [kind replacement at state block]
  (let [{:keys [items remaining replaced?]} state]
    (if (not= kind (:kind block))
      (update state :items conj block)
      (cond
        replaced? state
        (>= remaining (count (:content block)))
        (-> state (update :items conj block) (update :remaining - (count (:content block))))
        :else
        (assoc state :replaced? true :remaining 0
               :items (if-let [changed (replace-text-block block remaining replacement at)]
                        (conj items changed) items))))))

(defn replace-text-tail
  "Converge one channel to a full snapshot, preserving its exact observed prefix.
   Missing or stale prefixes are restored; tools and other channels retain order."
  [blocks kind offset snapshot at]
  (let [observed (apply str (map :content (filter #(= kind (:kind %)) blocks)))
        offset (if (str/starts-with? observed (subs snapshot 0 offset)) offset 0)
        replacement (subs snapshot offset)
        {:keys [items replaced?]}
        (reduce (partial project-text-replacement kind replacement at)
                {:items [] :remaining offset :replaced? false} (vec blocks))]
    (if (and (not replaced?) (seq replacement))
      (conj items {:id (next-text-id items kind) :kind kind
                   :status "streaming" :content replacement :at at})
      items)))

(defn- tool-block-id [{:keys [tool_call_id tool_name at]}]
  (cond
    (and (string? tool_call_id) (seq tool_call_id)) (str "tool:" tool_call_id)
    (and (string? tool_name) (seq tool_name)) (str "tool:" tool_name ":" (or at ""))
    :else (str "tool:" (or at ""))))

(defn- index-by-id [items id]
  (first (keep-indexed (fn [index item] (when (= (:id item) id) index)) items)))

(defn- started-block [existing block-id {:keys [tool_name tool_call_id preview at]}]
  (merge existing {:id block-id :kind :tool_call :toolName tool_name
                   :toolCallId tool_call_id :inputPreview preview
                   :status "streaming" :at at :updates []}))

(defn- updated-block [existing block-id {:keys [tool_name tool_call_id preview at]}]
  (if existing
    (cond-> (assoc existing :status "streaming" :at (or at (:at existing)))
      (seq preview) (update :updates #(append-limited % preview 8)))
    {:id block-id :kind :tool_call :toolName tool_name :toolCallId tool_call_id
     :status "streaming" :at at :updates (cond-> [] (seq preview) (conj preview))}))

(defn- ended-block
  [existing block-id {:keys [tool_name tool_call_id preview is_error at]}]
  (let [block {:id block-id :kind :tool_call :toolName tool_name
               :toolCallId tool_call_id :status (if is_error "error" "done")
               :outputPreview preview :isError (boolean is_error) :at at}]
    (if existing
      (merge existing block {:updates (:updates existing)
                             :inputPreview (:inputPreview existing)})
      (assoc block :updates []))))

(defn apply-tool-event
  "Project one tool start, update or end; unrelated events preserve the trace."
  [blocks {event-type :type :as event}]
  (let [items (vec blocks)
        block-id (tool-block-id event)
        index (index-by-id items block-id)
        existing (when (number? index) (nth items index))
        project (case event-type
                  "tool_start" started-block
                  "tool_update" updated-block
                  "tool_end" ended-block
                  nil)]
    (if project
      (let [block (project existing block-id event)]
        (if (number? index) (assoc items index block) (conj items block)))
      items)))

(defn- preview-present? [value]
  (when (string? value)
    (let [trimmed (str/trim value)
          lowered (str/lower-case trimmed)]
      (and (not (str/blank? trimmed)) (not= lowered "null") (not= lowered "undefined")))))

(defn valid-input-preview?
  "Require a concrete receipt ID and a non-sentinel textual preview."
  [receipt-id input-preview]
  (and (string? receipt-id) (seq receipt-id) (preview-present? input-preview)))

(defn- receipt-with-input [existing receipt-id tool-name input-preview]
  (if existing
    (cond-> existing
      (not (preview-present? (:input_preview existing))) (assoc :input_preview input-preview)
      (nil? (:input existing)) (assoc :input input-preview)
      (and (not (seq (:tool_name existing))) (seq tool-name)) (assoc :tool_name tool-name))
    {:id receipt-id :tool_name tool-name :status "running"
     :input input-preview :input_preview input-preview}))

(defn- trace-input-index [items receipt-id]
  (first (keep-indexed
          (fn [index block]
            (when (and (= (:kind block) :tool_call)
                       (or (= (:toolCallId block) receipt-id)
                           (= (:id block) (str "tool:" receipt-id))))
              index))
          items)))

(defn- block-with-input [existing receipt-id tool-name input-preview]
  (if existing
    (cond-> existing
      (not (preview-present? (:inputPreview existing))) (assoc :inputPreview input-preview)
      (and (not (seq (:toolName existing))) (seq tool-name)) (assoc :toolName tool-name))
    {:id (str "tool:" receipt-id) :kind :tool_call :toolName tool-name
     :toolCallId receipt-id :status "streaming" :inputPreview input-preview :updates []}))

(defn backfill-input-preview
  "Fill missing receipt and trace inputs while preserving prior observations."
  [run receipt-id tool-name input-preview]
  (let [receipts (vec (:tool_receipts run))
        receipt-index (index-by-id receipts receipt-id)
        receipt (receipt-with-input (when (number? receipt-index) (nth receipts receipt-index))
                                    receipt-id tool-name input-preview)
        blocks (vec (:trace_blocks run))
        block-index (trace-input-index blocks receipt-id)
        block (block-with-input (when (number? block-index) (nth blocks block-index))
                                receipt-id tool-name input-preview)]
    (assoc run
           :tool_receipts (if (number? receipt-index)
                            (assoc receipts receipt-index receipt)
                            (append-limited receipts receipt 40))
           :trace_blocks (if (number? block-index)
                           (assoc blocks block-index block)
                           (conj blocks block)))))
