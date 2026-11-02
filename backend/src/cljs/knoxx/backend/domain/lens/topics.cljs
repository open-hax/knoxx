(ns knoxx.backend.domain.lens.topics
  "Pure normalization, filtering, and envelope formatting for KnoxxLens topic
   refinement (`lens_refine_topics`). Ensures channel topics remain crisp nouns
   and proper nouns suitable for Channel Map chips and suggested actions."
  (:require [clojure.string :as str]))

(def ^:private banned-topic-terms
  #{"about" "actually" "after" "again" "also" "always" "another" "anything"
    "around" "available" "back" "awesome" "basically" "because" "been" "before"
    "being" "best" "better" "big" "both" "bring" "brought" "build" "building"
    "built" "call" "called" "calling" "came" "cannot" "cant" "change" "changed"
    "changing" "check" "checked" "checking" "clear" "close" "come" "comes"
    "coming" "cool" "could" "create" "created" "creating" "current" "currently"
    "definitely" "different" "does" "doesnt" "doing" "done" "dont" "down"
    "each" "early" "easier" "easy" "either" "else" "enough" "even" "ever"
    "every" "everyone" "everything" "exact" "exactly" "extra" "facinating"
    "fascinating" "fast" "feel" "feels" "felt" "find" "finding" "fine" "first"
    "fixed" "fixing" "folks" "found" "free" "from" "full" "further" "gave"
    "general" "getting" "give" "given" "gives" "giving" "global" "going" "gone"
    "gonna" "good" "gotta" "great" "guys" "hard" "have" "having" "hear" "heard"
    "help" "helped" "helping" "here" "high" "honestly" "hope" "huge" "idea"
    "into" "interesting" "issue" "issues" "item" "items" "its" "itself" "just"
    "keep" "keeping" "kept" "kind" "kinda" "knew" "know" "knowing" "known"
    "knows" "large" "last" "late" "later" "least" "left" "less" "lets" "like"
    "liked" "likely" "line" "little" "live" "local" "long" "look" "looked"
    "looking" "looks" "lost" "lots" "made" "main" "major" "make" "makes"
    "making" "many" "maybe" "mean" "means" "meant" "might" "mine" "more" "most"
    "mostly" "move" "moved" "moving" "much" "must" "near" "need" "needed"
    "needing" "needs" "never" "next" "nice" "none" "normal" "nothing" "now"
    "okay" "once" "ones" "only" "open" "opened" "opening" "other" "others"
    "ours" "over" "part" "parts" "past" "people" "person" "pick" "picked"
    "place" "plan" "planned" "planning" "play" "played" "playing" "please"
    "plus" "point" "points" "possible" "post" "posted" "posting" "pretty"
    "probably" "problem" "pull" "pulled" "push" "pushed" "putting" "question"
    "quick" "quite" "rather" "read" "reading" "ready" "real" "really" "reason"
    "recent" "recently" "right" "rip" "run" "running" "runs" "said" "same"
    "saying" "says" "seem" "seemed" "seems" "seen" "send" "sending" "sent"
    "show" "showed" "showing" "shown" "shows" "side" "since" "small" "some"
    "someone" "something" "sometimes" "soon" "sort" "sound" "sounds" "start"
    "started" "starting" "stay" "still" "stop" "stopped" "stuff" "super" "sure"
    "take" "taken" "takes" "taking" "talk" "talked" "talking" "tell" "telling"
    "than" "thank" "thanks" "that" "thats" "their" "theirs" "them" "then"
    "there" "theres" "these" "they" "thing" "things" "think" "thinking" "this"
    "those" "though" "thought" "through" "time" "times" "today" "together"
    "told" "took" "total" "totally" "tried" "true" "truly" "try" "trying"
    "turn" "turned" "turns" "type" "under" "until" "update" "updated" "updates"
    "updating" "upon" "use" "used" "useful" "uses" "using" "usually" "very"
    "via" "wait" "waiting" "want" "wanted" "wanting" "wants" "watch" "watching"
    "way" "ways" "week" "well" "went" "were" "what" "whatever" "whats" "when"
    "where" "whether" "which" "while" "whole" "whom" "whose" "will" "with"
    "within" "without" "wonder" "wondering" "work" "worked" "working" "works"
    "world" "would" "yeah" "year" "years" "your" "youre" "yours"})

(def ^:private allowed-gerund-nouns
  #{"caching" "chunking" "clustering" "coding" "embedding" "embeddings"
    "engineering" "federation" "grounding" "hosting" "indexing" "logging"
    "modeling" "monitoring" "networking" "onboarding" "packaging" "parsing"
    "planning" "polling" "profiling" "prompting" "provisioning" "queuing"
    "ranking" "reasoning" "rendering" "routing" "sampling" "scaling"
    "scheduling" "scripting" "sharding" "streaming" "tagging" "testing"
    "threading" "tokenization" "tooling" "tracing" "training" "typing"
    "versioning"})

(def ^:private valid-kinds
  #{"noun" "proper_noun"})

(defn- clean-term
  [s]
  (some-> s
          str
          (str/replace #"^[#@\"'`(\[{<]+|[.,!?;:\"'`)\]}>]+$" "")
          (str/replace #"\s+" " ")
          str/trim
          not-empty))

(defn- non-noun-single-word?
  [lower-word]
  (or (< (count lower-word) 2)
      (contains? banned-topic-terms lower-word)
      (boolean (re-matches #"^\d+$" lower-word))
      (and (str/ends-with? lower-word "ly") (> (count lower-word) 4))
      (and (str/ends-with? lower-word "ing")
           (not (contains? allowed-gerund-nouns lower-word)))
      (and (str/ends-with? lower-word "ed") (> (count lower-word) 4))))

(defn- infer-kind
  [term explicit-kind]
  (let [normalized-kind (some-> explicit-kind name str/lower-case (str/replace "-" "_"))]
    (cond
      (contains? valid-kinds normalized-kind) normalized-kind
      (re-find #"[A-Z]" term) "proper_noun"
      :else "noun")))

(defn- valid-topic-term?
  [term kind]
  (let [words (str/split term #"\s+")
        lowers (mapv str/lower-case words)]
    (and (<= 1 (count words) 4)
         (<= 2 (count term) 48)
         (boolean (re-find #"[A-Za-z]" term))
         (not (str/starts-with? (str/lower-case term) "http"))
         (if (= 1 (count words))
           (or (and (= "proper_noun" kind)
                    (not (contains? banned-topic-terms (first lowers)))
                    (boolean (re-find #"^[A-Z0-9][A-Za-z0-9._/-]+$" term)))
               (not (non-noun-single-word? (first lowers))))
           (and (not-every? #(contains? banned-topic-terms %) lowers)
                (not (contains? banned-topic-terms (last lowers))))))))

(defn- coerce-topic-entry
  [idx raw]
  (let [entry (if (string? raw) {:term raw} (or raw {}))
        term (clean-term (or (:term entry) (:label entry) (:name entry)))
        kind (when term (infer-kind term (:kind entry)))
        category (some-> (:category entry) str str/trim str/lower-case not-empty)
        salience-raw (:salience entry)
        salience (if (number? salience-raw)
                   (max 0.0 (min 1.0 (double salience-raw)))
                   (max 0.25 (- 1.0 (* 0.08 idx))))]
    (when term
      {:term term
       :canonical (str/lower-case term)
       :kind kind
       :category (or category (if (= kind "proper_noun") "entity" "concept"))
       :salience (/ (Math/round (* salience 100.0)) 100.0)
       :valid? (valid-topic-term? term kind)})))

(defn- json-escape
  [s]
  (-> (str (or s ""))
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\n" "\\n")
      (str/replace "\r" "\\r")
      (str/replace "\t" "\\t")))

(defn encode-json
  "Minimal pure-ClojureScript JSON encoder for structured tool envelopes."
  [value]
  (cond
    (nil? value) "null"
    (boolean? value) (if value "true" "false")
    (number? value) (str value)
    (keyword? value) (str "\"" (json-escape (name value)) "\"")
    (string? value) (str "\"" (json-escape value) "\"")
    (map? value)
    (str "{"
         (str/join ","
                   (keep (fn [[k v]]
                           (when (some? v)
                             (str (encode-json (if (keyword? k) (name k) (str k)))
                                  ":"
                                  (encode-json v))))
                         value))
         "}")
    (sequential? value)
    (str "[" (str/join "," (map encode-json value)) "]")
    :else
    (str "\"" (json-escape (str value)) "\"")))

(defn- dedupe-and-rank-topics
  [coerced]
  (->> coerced
       (filter :valid?)
       (reduce (fn [{:keys [seen items]} item]
                 (if (contains? seen (:canonical item))
                   {:seen seen :items items}
                   {:seen (conj seen (:canonical item))
                    :items (conj items (dissoc item :canonical :valid?))}))
               {:seen #{} :items []})
       :items
       (sort-by (fn [{:keys [salience kind]}]
                  [(- salience) (if (= kind "proper_noun") 0 1)]))
       (take 8)
       vec))

(defn normalize-refinement
  "Normalize raw tool input into `{:topics [...] :droppedTerms [...] :channelId ... :channelName ...}`."
  [{:keys [topics droppedTerms dropped_terms channelId channel_id channelName channel_name]}]
  (let [coerced (vec (keep-indexed coerce-topic-entry (or topics [])))
        explicit-dropped (->> (or droppedTerms dropped_terms []) (keep clean-term) vec)
        auto-dropped (->> coerced (remove :valid?) (mapv :term))
        accepted (dedupe-and-rank-topics coerced)
        dropped (->> (concat explicit-dropped auto-dropped)
                     (map str/trim)
                     (remove str/blank?)
                     distinct
                     vec)
        ch-id (some-> (or channelId channel_id) str str/trim not-empty)
        ch-name (some-> (or channelName channel_name) str str/trim not-empty)]
    (cond-> {:topics accepted
             :topicTerms (mapv :term accepted)
             :droppedTerms dropped}
      ch-id (assoc :channelId ch-id)
      ch-name (assoc :channelName ch-name))))

(defn format-refinement-result-text
  "Render human-readable summary plus `<refined_topics>{...}</refined_topics>`
   for KnoxxLens client extraction."
  [normalized]
  (let [terms (:topicTerms normalized)
        dropped (:droppedTerms normalized)
        summary (if (seq terms)
                  (str "Refined channel topics (" (count terms) " nouns/proper nouns): "
                       (str/join ", " terms)
                       (when (seq dropped)
                         (str " | Dropped noise: " (str/join ", " dropped))))
                  "No valid noun or proper-noun topics remained after filtering.")]
    (str summary "\n<refined_topics>" (encode-json normalized) "</refined_topics>")))
