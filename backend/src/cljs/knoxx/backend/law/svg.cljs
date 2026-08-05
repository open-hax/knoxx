(ns knoxx.backend.law.svg
  "Pure validation law for SVG accepted by the headless browser renderer."
  (:require [clojure.string :as str]))

(def ^:private prohibited-declaration-pattern #"(?is)<\s*!(?:doctype|entity)\b")
(def ^:private processing-instruction-pattern #"(?is)<\?")
(def ^:private prohibited-element-pattern
  #"(?is)<\s*(?:script|foreignobject|iframe|object|embed|link|img|audio|video|source|base|meta|html|body|form|input|button|textarea|select|option|animate(?:motion|transform)?|set|discard)\b")
(def ^:private event-attribute-pattern #"(?is)\son[a-z0-9:_-]*\s*=")
(def ^:private base-attribute-pattern #"(?is)\s(?:xml:base|base)\s*=")
(def ^:private resource-attribute-pattern
  #"(?is)\s(?:href|xlink:href|src)\s*=\s*(?:\"([^\"]*)\"|'([^']*)'|([^\s>]+))")
(def ^:private css-url-pattern
  #"(?is)url\(\s*(?:\"([^\"]*)\"|'([^']*)'|([^)]*))\s*\)")
(def ^:private css-import-pattern #"(?is)@import\b")
(def ^:private tag-name-pattern #"^[A-Za-z_][A-Za-z0-9_.:-]*")
(def ^:private closing-tag-pattern #"^/\s*([A-Za-z_][A-Za-z0-9_.:-]*)\s*$")

(defn- svg-preview
  [svg-string]
  (let [value (str (or svg-string ""))]
    (.slice value 0 (min 160 (count value)))))

(defn- reject-svg!
  [message type svg-string]
  (throw (ex-info message
                  {:type type
                   :preview (svg-preview svg-string)})))

(defn- captured-reference
  [match]
  (str/trim (str (or (nth match 1 nil)
                         (nth match 2 nil)
                         (nth match 3 nil)
                         ""))))

(defn- local-fragment-reference?
  [value]
  (str/starts-with? value "#"))

(defn- starts-at?
  [value index token]
  (= token (.slice value index (+ index (count token)))))

(defn- sequence-end
  [candidate start token type]
  (let [index (.indexOf candidate token start)]
    (when (= -1 index)
      (reject-svg! "SVG contains unterminated markup" type candidate))
    (+ index (count token))))

(defn- tag-end
  [candidate start]
  (loop [index (inc start)
         quote nil]
    (when (>= index (count candidate))
      (reject-svg! "SVG contains an unterminated tag"
                   :svg/unterminated-tag candidate))
    (let [character (.charAt candidate index)]
      (cond
        quote
        (recur (inc index) (when-not (= character quote) quote))

        (or (= character "\"") (= character "'"))
        (recur (inc index) character)

        (= character ">")
        index

        :else
        (recur (inc index) nil)))))

(defn- parse-tag
  [candidate start end]
  (let [token (str/trim (.slice candidate (inc start) end))]
    (when (or (str/blank? token) (str/includes? token "<"))
      (reject-svg! "SVG contains a malformed tag" :svg/malformed-tag candidate))
    (if (str/starts-with? token "/")
      (if-let [[_ name] (re-matches closing-tag-pattern token)]
        {:kind :close :name (str/lower-case name)}
        (reject-svg! "SVG contains a malformed closing tag"
                     :svg/malformed-closing-tag candidate))
      (let [self-closing? (boolean (re-find #"/\s*$" token))
            opening-token (if self-closing?
                            (str/trim (str/replace token #"/\s*$" ""))
                            token)
            name (re-find tag-name-pattern opening-token)]
        (when-not name
          (reject-svg! "SVG contains a malformed opening tag"
                       :svg/malformed-opening-tag candidate))
        {:kind :open
         :name (str/lower-case name)
         :self-closing? self-closing?}))))

(defn- require-single-svg-document!
  [candidate]
  (loop [index 0
         stack []
         seen-root? false
         root-closed? false]
    (if (>= index (count candidate))
      (do
        (when (seq stack)
          (reject-svg! "SVG document has unclosed elements"
                       :svg/unclosed-elements candidate))
        (when-not (and seen-root? root-closed?)
          (reject-svg! "SVG content must contain exactly one SVG root"
                       :svg/missing-root candidate))
        candidate)
      (if-not (= "<" (.charAt candidate index))
        (let [next-tag (.indexOf candidate "<" index)
              end (if (= -1 next-tag) (count candidate) next-tag)
              text (.slice candidate index end)]
          (when (and (empty? stack) (not (str/blank? text)))
            (reject-svg! "SVG document contains text outside the root element"
                         :svg/trailing-content candidate))
          (recur end stack seen-root? root-closed?))
        (cond
          (starts-at? candidate index "<!--")
          (do
            (when (empty? stack)
              (reject-svg! "SVG comments must be inside the root element"
                           :svg/outside-root-markup candidate))
            (recur (sequence-end candidate (+ index 4) "-->"
                                 :svg/unterminated-comment)
                   stack seen-root? root-closed?))

          (starts-at? candidate index "<![CDATA[")
          (do
            (when (empty? stack)
              (reject-svg! "SVG CDATA must be inside the root element"
                           :svg/outside-root-markup candidate))
            (recur (sequence-end candidate (+ index 9) "]]>")
                                 :svg/unterminated-cdata)
                   stack seen-root? root-closed?))

          (starts-at? candidate index "<!")
          (reject-svg! "SVG declarations are not allowed"
                       :svg/prohibited-declaration candidate)

          (starts-at? candidate index "<?")
          (reject-svg! "SVG processing instructions are not allowed"
                       :svg/processing-instruction candidate)

          :else
          (let [end (tag-end candidate index)
                {:keys [kind name self-closing?]} (parse-tag candidate index end)
                next-index (inc end)]
            (case kind
              :open
              (if (empty? stack)
                (do
                  (when (or seen-root? root-closed?)
                    (reject-svg! "SVG document contains trailing markup or another root"
                                 :svg/multiple-roots candidate))
                  (when-not (= "svg" name)
                    (reject-svg! "SVG document root must be <svg>"
                                 :svg/missing-root candidate))
                  (recur next-index
                         (if self-closing? [] [name])
                         true
                         self-closing?))
                (recur next-index
                       (if self-closing? stack (conj stack name))
                       seen-root?
                       root-closed?))

              :close
              (do
                (when (empty? stack)
                  (reject-svg! "SVG document contains an unexpected closing tag"
                               :svg/unexpected-closing-tag candidate))
                (when-not (= name (peek stack))
                  (reject-svg! "SVG document contains mismatched tags"
                               :svg/mismatched-tags candidate))
                (let [next-stack (pop stack)]
                  (recur next-index next-stack seen-root? (empty? next-stack))))))))))))

(defn validate-svg!
  "Return one static, resource-local SVG document or throw structured ex-info.

   The accepted document has exactly one balanced `<svg>` root and no markup or
   text outside it. Local fragment references remain available for gradients,
   filters, masks, clips, symbols, and other in-document resources."
  [svg-string]
  (when-not (string? svg-string)
    (reject-svg! "SVG content must be a string" :svg/invalid-content svg-string))
  (let [candidate (str/trim svg-string)]
    (when (str/blank? candidate)
      (reject-svg! "SVG content cannot be blank" :svg/blank-content svg-string))
    (when (re-find prohibited-declaration-pattern candidate)
      (reject-svg! "SVG declarations and entities are not allowed"
                   :svg/prohibited-declaration svg-string))
    (when (re-find processing-instruction-pattern candidate)
      (reject-svg! "SVG processing instructions are not allowed"
                   :svg/processing-instruction svg-string))
    (when (re-find prohibited-element-pattern candidate)
      (reject-svg! "SVG contains an active, mutating, or HTML-only element"
                   :svg/prohibited-element svg-string))
    (when (re-find event-attribute-pattern candidate)
      (reject-svg! "SVG event attributes are not allowed"
                   :svg/event-attribute svg-string))
    (when (re-find base-attribute-pattern candidate)
      (reject-svg! "SVG base URL attributes are not allowed"
                   :svg/base-url svg-string))
    (when (re-find css-import-pattern candidate)
      (reject-svg! "SVG CSS imports are not allowed"
                   :svg/css-import svg-string))
    (doseq [match (re-seq resource-attribute-pattern candidate)]
      (when-not (local-fragment-reference? (captured-reference match))
        (reject-svg! "SVG resource attributes must use local fragments"
                     :svg/external-resource svg-string)))
    (doseq [match (re-seq css-url-pattern candidate)]
      (when-not (local-fragment-reference? (captured-reference match))
        (reject-svg! "SVG CSS URLs must use local fragments"
                     :svg/external-css-resource svg-string)))
    (require-single-svg-document! candidate)))
