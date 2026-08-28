(ns knoxx.frontend.pages.gardens.logic
  "Pure projection helpers for the deploy-owned Garden review surface.

   A Garden owns identity and locale membership. Publication intents own paths.
   Content and Puck presentation contracts deliberately do not enter this
   model."
  (:require [clojure.string :as str]))

(def available-languages
  {"en" "English" "es" "Español" "fr" "Français" "de" "Deutsch"
   "ja" "日本語" "zh" "中文" "ko" "한국어" "pt" "Português"
   "ru" "Русский" "it" "Italiano" "ar" "العربية" "hi" "हिन्दी"
   "nl" "Nederlands" "pl" "Polski" "tr" "Türkçe" "vi" "Tiếng Việt"})

(defn language-name [code]
  (get available-languages code code))

(defn- trim-trailing-slash [value]
  (str/replace (or value "") #"/+$" ""))

(defn public-url
  "Join a deployment base URL and a contract-owned publication path."
  [site-url publication-path]
  (str (trim-trailing-slash site-url)
       (if (str/starts-with? publication-path "/") "" "/")
       publication-path))

(defn normalize-deployment
  "Make the backend deployment DTO convenient for the UI without inventing
   mutable Garden state."
  [{:keys [site-url gardens]}]
  {:site-url site-url
   :gardens
   (mapv (fn [{:keys [garden publications]}]
           {:id (:id garden)
            :title (:title garden)
            :status (:status garden)
            :locales (vec (:locales garden))
            :placements
            (->> publications
                 (map (fn [publication]
                        {:id (:id publication)
                         :locale (:locale publication)
                         :path (:path publication)
                         :state (:state publication)
                         :url (public-url site-url (:path publication))}))
                 (sort-by (juxt :locale :path))
                 vec)})
         gardens)})

(defn receipt-summary
  "One line describing what a reconciliation receipt says happened.

   Matched against the FULL wire value, not its name. `send-result!` encodes
   keyword values through `shape.resource-identity/encode-wire-values`, which
   renders them `namespace/name` precisely so identity survives JSON — while
   `clj->js` strips the namespace from map KEYS, which is why the key is
   `:type` and the value is \"publication/materialized\". Matching on the name
   alone would collapse distinct namespaces onto one branch, which is the thing
   that encoding exists to prevent.

   A receipt with no recognized type reports as recorded rather than as
   success: the reconciler emits a receipt for a blocked or failed plan too,
   and calling those success would be the UI lying on the reconciler's behalf."
  [receipt]
  (case (:type receipt)
    "publication/materialized" "Published."
    ;; A noop has three quite different causes and the receipt says which in
    ;; `:reason`. Reporting them all as "already published" told a reviewer
    ;; their content was live when the planner had actually refused to publish
    ;; it — an archived garden and a converged route read identically.
    ;; `converge` emits its noop with NO reason, so absence means genuinely
    ;; converged and is the only case that may say so.
    "publication/noop"
    (case (:reason receipt)
      "publication-not-public"
      "Not published: the contract does not ask for this to be public."
      "garden-not-active"
      "Not published: the garden is not active."
      nil "Already published at this revision; nothing changed."
      (str "Nothing was done: " (:reason receipt) "."))
    "publication/removed" "Withdrawn from publication."
    "publication/blocked" (str "Blocked: "
                               (if-let [bs (seq (:blockers receipt))]
                                 (str/join ", " bs)
                                 "the plan is not admissible"))
    "publication/failed" "Reconciliation failed; see the receipt journal."
    (str "Reconciliation recorded"
         (when-let [t (:type receipt)] (str ": " t))
         ".")))

(defn placement-published?
  "Whether a placement's contract state asks for publication. `:state` is the
   DESIRED state from the contract, never a materialization fact — a placement
   can read `published` and have no bytes behind it, which is exactly the case
   the publish action exists to resolve."
  [placement]
  (= "published" (:state placement)))

(defn receipt-tone
  "How a reconciliation receipt should be presented: `:success`, `:warning` or
   `:error`.

   Separate from `receipt-summary` because the words and the colour must agree,
   and a banner that paints every outcome emerald contradicts a summary written
   precisely so a blocked plan does not read as a published one. A receipt this
   function does not recognize is a warning rather than a success: the
   reconciler emits receipts for outcomes that are not wins, and the unknown
   case is far likelier to be one of those than a success nobody named."
  [receipt]
  (case (:type receipt)
    ("publication/materialized" "publication/removed") :success
    ;; A noop is a success only when it means "already converged", which
    ;; `converge` signals by emitting NO reason. A noop carrying one came from
    ;; `takedown` — the contract does not ask for this to be public, or the
    ;; garden is not active — and those are refusals to publish. Painting them
    ;; emerald tells a reviewer their content is live when the planner has
    ;; declined to make it so.
    "publication/noop" (if (:reason receipt) :warning :success)
    "publication/blocked" :warning
    "publication/failed" :error
    :warning))

(defn publishable-placements
  "The placements a publish-all run would attempt, in listed order."
  [garden]
  (filterv placement-published? (:placements garden)))

(defn run-summary
  "One line for a completed publish-all run, counted by what actually happened.

   Counts by outcome rather than reporting only successes, because most of a
   garden is usually NOT publishable yet — a locale awaiting translation or
   approval answers `blocked`, and a run that mentioned only what it published
   would read as though the rest had quietly worked."
  [receipts]
  (let [published (count (filter #(= "publication/materialized" (:type %)) receipts))
        converged (count (filter #(and (= "publication/noop" (:type %))
                                       (nil? (:reason %)))
                                 receipts))
        blocked (count (filter #(= "publication/blocked" (:type %)) receipts))
        failed (count (filter #(= "publication/failed" (:type %)) receipts))
        other (- (count receipts) published converged blocked failed)]
    (str/join ", "
              (cond-> []
                (pos? published) (conj (str published " published"))
                (pos? converged) (conj (str converged " already current"))
                (pos? blocked) (conj (str blocked " blocked"))
                (pos? failed) (conj (str failed " failed"))
                (pos? other) (conj (str other " not published"))
                (empty? receipts) (conj "nothing to publish")
                true (conj (str "(" (count receipts) " attempted)"))))))

(defn run-tone
  "The worst tone in a run. A single failure must not be hidden behind a
   majority of successes."
  [receipts]
  (let [tones (set (map receipt-tone receipts))]
    (cond (contains? tones :error) :error
          (contains? tones :warning) :warning
          :else :success)))
