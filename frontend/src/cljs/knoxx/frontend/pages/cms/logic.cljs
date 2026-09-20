(ns knoxx.frontend.pages.cms.logic
  "Validated source projections and revision-bound authoring decisions."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def SourceReview
  "The server projection for one exact source revision and review history."
  [:map
   [:document :string] [:title :string] [:content :string] [:revision :string]
   [:source_locale :string] [:head [:maybe :string]]
   [:status [:enum "draft" "in_review" "needs_revision" "accepted"]]
   [:accepted :boolean] [:history [:vector :map]] [:lessons [:vector :map]]])

(def Publication
  "A requested placement with independently observed publication evidence."
  [:map [:id :string] [:document :string] [:garden :string] [:locale :string]
   [:revision :string] [:path :string] [:desired :string]
   [:observed [:maybe :string]] [:blockers [:vector :string]]])

(defn assert-snapshot!
  "Reject malformed server projections before installing editor state."
  [value]
  (when-not (m/validate SourceReview value)
    (throw (ex-info "Invalid source review response" {:value value})))
  value)

(defn assert-publications!
  "Validate target identities and observed evidence independently of HTTP status."
  [value]
  (when-not (and (map? value) (string? (:document value))
                 (vector? (:publications value))
                 (every? #(m/validate Publication %) (:publications value)))
    (throw (ex-info "Invalid publication targets response" {:value value})))
  value)

(defn wire-id
  "Preserve a resource identity's complete namespace on the wire."
  [value]
  (if (keyword? value) (subs (str value) 1) value))

(defn document-id
  "Read the canonical identity from resource views and direct review snapshots."
  [row]
  (wire-id (if (map? (:document row))
             (or (get-in row [:document :document/id]) (get-in row [:document :id]))
             (or (:document row) (:document/id row) (:id row)))))

(defn document-title
  "Use the document metadata title, retaining identity as the visible fallback."
  [row]
  (or (:title row) (get-in row [:document :document/title])
      (get-in row [:document :title]) (document-id row)))

(defn normalize-inventory
  "Accept the resource topology envelope or the historical bare view vector."
  [value]
  (let [inventory (if (sequential? value) {:documents (vec value)} value)]
    (when-not (and (map? inventory) (sequential? (:documents inventory)))
      (throw (ex-info "Invalid document inventory response" {:value value})))
    (update inventory :documents vec)))

(defn fresh-editor
  "Initialize a draft against the source bytes actually read from the server."
  [snapshot]
  {:snapshot snapshot :content (:content snapshot) :title (:title snapshot)
   :base-revision (:revision snapshot) :base-content (:content snapshot)
   :conflict? false})

(defn dirty?
  "Whether local source text differs from its acknowledged base."
  [editor]
  (and editor (not= (:content editor) (:base-content editor))))

(defn receive-snapshot
  "Follow clean live updates; preserve dirty text and reject stale source writes."
  [editor snapshot]
  (if (and (= (get-in editor [:snapshot :document]) (:document snapshot)) (dirty? editor))
    (assoc editor :snapshot snapshot
           :conflict? (not= (:base-revision editor) (:revision snapshot)))
    (fresh-editor snapshot)))

(defn writable-draft?
  "Only nonblank, changed text with an acknowledged source base may be saved."
  [editor]
  (and (dirty? editor) (not (:conflict? editor))
       (not (str/blank? (:title editor))) (not (str/blank? (:content editor)))))

(defn review-basis
  "Bind review notes to both the source revision and its history head."
  [snapshot]
  (select-keys snapshot [:revision :source_locale :head]))

(defn empty-review
  "Start an explicit review form against the current projection."
  [snapshot]
  {:basis (review-basis snapshot) :notes "" :before "" :after "" :reason "" :lessons ""})

(defn review-dirty?
  "Typed feedback is unfinished work even when source text is unchanged."
  [form]
  (boolean (some #(not (str/blank? (get form % "")))
                 [:notes :before :after :reason :lessons])))

(defn review-form-current?
  "A reviewer must explicitly acknowledge a changed source or history head."
  [form snapshot]
  (= (:basis form) (review-basis snapshot)))

(defn creation-dirty?
  "Compare the initial page form with the values the writer first opened."
  [creation]
  (and creation (not= (:initial creation) (dissoc creation :initial))))

(defn unsaved?
  "Protect source drafts, initial creation text, and unfinished feedback together."
  [{:keys [editor review-form creation]}]
  (boolean (or (dirty? editor) (review-dirty? review-form) (creation-dirty? creation))))

(defn command-available?
  "Honor explicit command denials while accepting older projections without a list."
  [commands command]
  (or (nil? commands) (boolean (some #{command} commands))))

(defn capability?
  "Read grants supplied by the authenticated server projection."
  [capabilities capability]
  (boolean (some #{capability} capabilities)))

(defn review-action-available?
  "Expose only transitions admitted by the current source workflow state."
  [snapshot action]
  (let [status (:status snapshot)]
    (case action
      "comment" true
      "submit" (contains? #{"draft" "needs_revision"} status)
      "request_changes" (contains? #{"in_review" "accepted"} status)
      "accept" (= "in_review" status)
      false)))

(defn lines
  "Keep only nonblank trimmed items from a human-entered list."
  [value]
  (->> (str/split (or value "") #"[\n,]") (map str/trim) (remove str/blank?) vec))

(defn review-payload
  "Construct review feedback without caller-supplied scope or principal claims."
  [snapshot action form]
  (cond-> {:revision (:revision snapshot) :source_locale (:source_locale snapshot)
           :expected_head (:head snapshot) :action action
           :notes (str/trim (:notes form "")) :lessons (lines (:lessons form))}
    (or (seq (:before form)) (seq (:after form)))
    (assoc :corrections [{:before (:before form) :after (:after form) :reason (:reason form)}])))

(defn status-label
  "Readable source workflow labels shared by navigation and review panels."
  [status]
  (get {"draft" "Draft" "in_review" "In review" "needs_revision" "Needs revision"
        "accepted" "Accepted"} status "Draft"))
