(ns knoxx.backend.law.translation-source-split
  "Deterministic server-owned paragraph partitioning for translation sources.

  Every returned member retains its original source characters. Blank-line
  separators trail the preceding paragraph; leading separators stay with the
  first nonblank paragraph and trailing whitespace stays with the last. Fenced
  and indented Markdown code, mapping front matter, and list-item subtrees are
  indivisible where an internal split would remove required syntax context."
  (:require [clojure.string :as str]))

(defn- source-character
  "Return the one-character string at `index`."
  [source index]
  (subs source index (inc index)))

(defn- line-break-end
  "Return the exclusive end of the line break at `index`, when present."
  [source index]
  (when (< index (count source))
    (case (source-character source index)
      "\n" (inc index)
      "\r" (if (and (< (inc index) (count source))
                      (= "\n" (source-character source (inc index))))
               (+ index 2)
               (inc index))
      nil)))

(defn- horizontal-whitespace-end
  "Skip blank characters other than line breaks from `index`."
  [source index]
  (loop [cursor index]
    (if (< cursor (count source))
      (let [character (source-character source cursor)]
        (if (and (not= "\r" character)
                 (not= "\n" character)
                 (str/blank? character))
          (recur (inc cursor))
          cursor))
      cursor)))

(defn- line-content-end
  "Return the exclusive end of the current line, before its line break."
  [source line-start]
  (loop [cursor line-start]
    (if (or (= cursor (count source))
            (line-break-end source cursor))
      cursor
      (recur (inc cursor)))))

(defn- next-line-start
  "Return the next line start, or the source end for a final unterminated line."
  [source content-end]
  (or (line-break-end source content-end) content-end))

(defn- source-lines
  "Describe source lines with exact half-open content and line spans."
  [source]
  (loop [line-start 0
         lines []]
    (if (< line-start (count source))
      (let [content-end (line-content-end source line-start)
            line-end (next-line-start source content-end)]
        (recur line-end
               (conj lines {:start line-start
                            :content-end content-end
                            :end line-end})))
      lines)))

(defn- line-text
  "Return one line's content without normalizing or including its line break."
  [source {:keys [start content-end]}]
  (subs source start content-end))

(defn- blank-line?
  "Whether a source line contains only blank characters."
  [source line]
  (str/blank? (line-text source line)))

(defn- next-tab-column
  "Advance a Markdown indentation column to the next four-column tab stop."
  [column]
  (+ column (- 4 (mod column 4))))

(defn- indentation-info
  "Return the cursor and visual column after one line's leading indentation."
  [source {:keys [start content-end]}]
  (loop [cursor start
         column 0]
    (if (< cursor content-end)
      (case (source-character source cursor)
        " " (recur (inc cursor) (inc column))
        "\t" (recur (inc cursor) (next-tab-column column))
        {:cursor cursor :column column})
      {:cursor cursor :column column})))

(defn- leading-bom-free
  "Remove the optional Unicode byte-order marker from the first line only."
  [text]
  (if (str/starts-with? text "\uFEFF")
    (subs text 1)
    text))

(defn- front-matter-opening
  "Return a recognized front-matter marker at the exact source beginning."
  [source lines]
  (when-let [first-line (first lines)]
    (when (zero? (:start first-line))
      (let [opening (-> (line-text source first-line)
                        leading-bom-free
                        str/trimr)]
        (when (contains? #{"---" "+++"} opening)
          opening)))))

(defn- front-matter-entry?
  "Whether a line supplies mapping evidence for the selected front matter."
  [marker text]
  (case marker
    "---" (boolean
           (or (= "{}" (str/trim text))
               (re-find #"^[A-Za-z0-9_.-]+[ \t]*:(?:[ \t]+.*)?$" text)
               (re-find #"^[\"'][^\"']+[\"'][ \t]*:(?:[ \t]+.*)?$"
                        text)))
    "+++" (boolean
           (or (re-find #"^[A-Za-z0-9_.-]+[ \t]*=" text)
               (re-find #"^\[[^\]]+\][ \t]*$" text)))
    false))

(defn- front-matter-closing?
  "Whether one exact column-zero line closes `marker`."
  [marker text]
  (let [closing (str/trimr text)]
    (if (= "---" marker)
      (contains? #{"---" "..."} closing)
      (= "+++" closing))))

(defn- front-matter-span
  "Return a closed mapping front-matter span, refusing ambiguous rule prose."
  [source lines]
  (when-let [marker (front-matter-opening source lines)]
    (loop [remaining (next lines)
           mapping-entry? false]
      (when-let [line (first remaining)]
        (let [text (line-text source line)]
          (if (front-matter-closing? marker text)
            (when mapping-entry?
              [0 (:content-end line)])
            (recur (next remaining)
                   (or mapping-entry?
                       (front-matter-entry? marker text)))))))))

(defn- indent-end
  "Skip the at-most-three literal spaces Markdown permits before a fence."
  [source line-start content-end]
  (loop [cursor line-start
         spaces 0]
    (if (and (< cursor content-end)
             (< spaces 3)
             (= " " (source-character source cursor)))
      (recur (inc cursor) (inc spaces))
      cursor)))

(defn- marker-run-end
  "Return the end of a repeated fence marker run."
  [source start content-end marker]
  (loop [cursor start]
    (if (and (< cursor content-end)
             (= marker (source-character source cursor)))
      (recur (inc cursor))
      cursor)))

(defn- opening-fence
  "Describe a CommonMark-style backtick or tilde opening fence on one line."
  [source line-start content-end]
  (let [marker-start (indent-end source line-start content-end)]
    (when (< marker-start content-end)
      (let [marker (source-character source marker-start)]
        (when (or (= "`" marker) (= "~" marker))
          (let [marker-end (marker-run-end source marker-start content-end marker)
                length (- marker-end marker-start)
                info (subs source marker-end content-end)]
            (when (and (>= length 3)
                       (or (= "~" marker) (not (str/includes? info "`"))))
              {:marker marker :length length})))))))

(defn- closing-fence?
  "Whether one line closes `fence` without interpreting its contents."
  [source line-start content-end {:keys [marker length]}]
  (let [marker-start (indent-end source line-start content-end)
        marker-end (marker-run-end source marker-start content-end marker)]
    (and (>= (- marker-end marker-start) length)
         (str/blank? (subs source marker-end content-end)))))

(defn- fenced-code-spans
  "Return half-open spans whose internal blank lines cannot split paragraphs.

  A closed span stops before the closing line's line break so blank lines after
  the fence can still separate the fenced block from following prose. An
  unclosed fence protects the remainder of the source, matching Markdown's
  interpretation rather than guessing where code ended."
  [source]
  (loop [line-start 0
         active nil
         spans []]
    (if (< line-start (count source))
      (let [content-end (line-content-end source line-start)
            following-line (next-line-start source content-end)]
        (cond
          (and active (closing-fence? source line-start content-end active))
          (recur following-line nil
                 (conj spans [(:start active) content-end]))

          active
          (recur following-line active spans)

          :else
          (if-let [fence (opening-fence source line-start content-end)]
            (recur following-line (assoc fence :start line-start) spans)
            (recur following-line nil spans))))
      (cond-> spans
        active (conj [(:start active) (count source)])))))

(defn- atx-heading-line?
  "Whether one line is a complete ATX heading block."
  [text]
  (boolean (re-find #"^ {0,3}#{1,6}(?:[ \t]+.*)?$" text)))

(defn- setext-underline-line?
  "Whether one line can finish a Setext heading."
  [text]
  (boolean (re-find #"^ {0,3}(?:=+|-+)[ \t]*$" text)))

(defn- thematic-break-line?
  "Whether one line is a complete CommonMark thematic break."
  [text]
  (boolean
   (re-find #"^ {0,3}(?:(?:\*[ \t]*){3,}|(?:_[ \t]*){3,}|(?:-[ \t]*){3,})$"
            text)))

(defn- blockquote-opening-line?
  "Whether one line begins a root CommonMark block quote."
  [text]
  (boolean (re-find #"^ {0,3}>" text)))

(defn- block-boundary-line?
  "Whether a line ends syntax after which indented code may start directly."
  [source {:keys [start content-end] :as line}]
  (let [text (line-text source line)]
    (or (atx-heading-line? text)
        (setext-underline-line? text)
        (thematic-break-line? text)
        (opening-fence source start content-end))))

(defn- paragraph-interrupting-block-line?
  "Whether a line cannot be a lazy continuation of a list-item paragraph."
  [source line]
  (let [text (line-text source line)]
    (or (block-boundary-line? source line)
        (blockquote-opening-line? text))))

(defn- pipe-cells
  "Return simple pipe-delimited cells without interpreting their source bytes."
  [text]
  (let [trimmed (str/trim text)]
    (when (str/includes? trimmed "|")
      (let [without-leading (if (str/starts-with? trimmed "|")
                              (subs trimmed 1)
                              trimmed)
            body (if (str/ends-with? without-leading "|")
                   (subs without-leading 0 (dec (count without-leading)))
                   without-leading)]
        (str/split body #"\|" -1)))))

(defn- delimiter-cell?
  "Whether one GFM table delimiter cell has at least three hyphens."
  [cell]
  (boolean (re-matches #":?-{3,}:?" (str/trim cell))))

(defn- gfm-table-span-at
  "Return one validated, contiguous GFM pipe-table span at `line-index`."
  [source lines line-index]
  (when (< (inc line-index) (count lines))
    (let [header-line (nth lines line-index)
          delimiter-line (nth lines (inc line-index))
          header-cells (pipe-cells (line-text source header-line))
          delimiter-cells (pipe-cells (line-text source delimiter-line))]
      (when (and (seq header-cells)
                 (= (count header-cells) (count delimiter-cells))
                 (every? delimiter-cell? delimiter-cells)
                 (<= (:column (indentation-info source header-line)) 3)
                 (<= (:column (indentation-info source delimiter-line)) 3))
        (loop [row-index (+ line-index 2)
               table-end (:content-end delimiter-line)]
          (if (< row-index (count lines))
            (let [line (nth lines row-index)]
              (if (and (not (blank-line? source line))
                       (<= (:column (indentation-info source line)) 3)
                       (seq (pipe-cells (line-text source line))))
                (recur (inc row-index) (:content-end line))
                [[(:start header-line) table-end] row-index]))
            [[(:start header-line) table-end] row-index]))))))

(defn- gfm-table-spans
  "Return validated table spans; blank lines always terminate a table."
  [source lines]
  (loop [line-index 0
         spans []]
    (if (< line-index (count lines))
      (if-let [[span next-index] (gfm-table-span-at source lines line-index)]
        (recur next-index (conj spans span))
        (recur (inc line-index) spans))
      spans)))

(defn- indented-code-line?
  "Whether a nonblank line begins at Markdown's four-column code indent."
  [source line]
  (and (not (blank-line? source line))
       (>= (:column (indentation-info source line)) 4)))

(defn- indented-code-spans
  "Return spans that retain blank lines inside root indented code blocks.

  Indentation may start a block only at the source beginning or after a blank
  line. This prevents an indented continuation of ordinary prose from
  swallowing a later paragraph boundary. Trailing blank lines stay outside
  the protected span."
  [source lines table-end-offsets]
  (loop [remaining lines
         previous-line nil
         active-start nil
         last-code-end nil
         spans []]
    (if-let [line (first remaining)]
      (let [blank? (blank-line? source line)
            code? (indented-code-line? source line)]
        (cond
          (and active-start blank?)
          (recur (next remaining) line active-start last-code-end spans)

          (and active-start code?)
          (recur (next remaining) line active-start (:content-end line) spans)

          active-start
          (recur (next remaining) line nil nil
                 (conj spans [active-start last-code-end]))

          (and code?
               (or (nil? previous-line)
                   (blank-line? source previous-line)
                   (block-boundary-line? source previous-line)
                   (contains? table-end-offsets
                              (:content-end previous-line))))
          (recur (next remaining) line (:start line) (:content-end line) spans)

          :else
          (recur (next remaining) line nil nil spans)))
      (cond-> spans
        active-start (conj [active-start last-code-end])))))

(def ^:private decimal-digits
  "Characters admitted in a Markdown ordered-list marker."
  #{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9"})

(defn- decimal-digit?
  "Whether one source character is an ASCII decimal digit."
  [character]
  (contains? decimal-digits character))

(defn- ordered-marker-end
  "Return the end of a one-to-nine-digit ordered-list marker."
  [source marker-start content-end]
  (loop [cursor marker-start
         digits 0]
    (if (and (< cursor content-end)
             (< digits 9)
             (decimal-digit? (source-character source cursor)))
      (recur (inc cursor) (inc digits))
      (when (and (pos? digits)
                 (< cursor content-end)
                 (contains? #{"." ")"}
                            (source-character source cursor)))
        (inc cursor)))))

(defn- list-marker-end
  "Return the end of an unordered or ordered marker beginning at `start`."
  [source start content-end]
  (when (< start content-end)
    (let [character (source-character source start)]
      (if (contains? #{"-" "+" "*"} character)
        (inc start)
        (ordered-marker-end source start content-end)))))

(defn- marker-padding-info
  "Return visual indentation after the spaces or tabs following a marker."
  [source start content-end start-column]
  (loop [cursor start
         column start-column]
    (if (< cursor content-end)
      (case (source-character source cursor)
        " " (recur (inc cursor) (inc column))
        "\t" (recur (inc cursor) (next-tab-column column))
        {:cursor cursor :column column})
      {:cursor cursor :column column})))

(defn- list-marker
  "Describe a Markdown list marker and its required continuation column."
  [source line]
  (let [{marker-start :cursor marker-indent :column}
        (indentation-info source line)
        content-end (:content-end line)
        marker-character (when (< marker-start content-end)
                           (source-character source marker-start))]
    (when-let [marker-end (list-marker-end source marker-start content-end)]
      (let [marker-width (- marker-end marker-start)
            marker-column (+ marker-indent marker-width)
            unordered? (contains? #{"-" "+" "*"} marker-character)
            ordered-digits (when-not unordered?
                             (subs source marker-start (dec marker-end)))]
        (cond
          (= marker-end content-end)
          {:indent marker-indent
           :content-indent (inc marker-column)
           :kind (if unordered? :unordered :ordered)
           :starts-at-one? (boolean
                            (and ordered-digits
                                 (re-matches #"0*1" ordered-digits)))
           :content? false}

          (contains? #{" " "\t"}
                     (source-character source marker-end))
          (let [padding (marker-padding-info source marker-end content-end
                                             marker-column)]
            {:indent marker-indent
             :content-indent (:column padding)
             :kind (if unordered? :unordered :ordered)
             :starts-at-one? (boolean
                              (and ordered-digits
                                   (re-matches #"0*1" ordered-digits)))
             :content? (< (:cursor padding) content-end)})

          :else nil)))))

(defn- paragraph-interrupting-marker?
  "Whether a nonempty marker may interrupt an active CommonMark paragraph."
  [{:keys [kind starts-at-one? content?]}]
  (and content?
       (or (= :unordered kind)
           (and (= :ordered kind) starts-at-one?))))

(defn- root-list-marker-permitted?
  "Whether `marker` may begin an item at this exact block position."
  [source lines line-index marker]
  (let [previous-line (when (pos? line-index)
                        (nth lines (dec line-index)))]
    (or (nil? previous-line)
        (blank-line? source previous-line)
        (block-boundary-line? source previous-line)
        (paragraph-interrupting-marker? marker))))

(defn- list-continuation?
  "Whether a nonblank line remains inside one list-item subtree."
  [source root-marker line]
  (let [indent (:column (indentation-info source line))
        nested-marker (list-marker source line)
        required-indent (:content-indent root-marker)]
    (if nested-marker
      (>= (:indent nested-marker) required-indent)
      (>= indent required-indent))))

(defn- list-item-span
  "Return one item subtree span and the index of the first line after it."
  [source lines item-index root-marker]
  (let [item-line (nth lines item-index)]
    (loop [line-index (inc item-index)
           last-content-end (:content-end item-line)
           blank-since-content? false]
      (if (< line-index (count lines))
        (let [line (nth lines line-index)]
          (cond
            (blank-line? source line)
            (recur (inc line-index) last-content-end true)

            (list-continuation? source root-marker line)
            (recur (inc line-index) (:content-end line) false)

            (and (not blank-since-content?)
                 (nil? (list-marker source line))
                 (not (paragraph-interrupting-block-line? source line)))
            (recur (inc line-index) (:content-end line) false)

            :else
            [[(:start item-line) last-content-end] line-index]))
        [[(:start item-line) last-content-end] line-index]))))

(defn- list-item-spans
  "Return protected spans for root list items, excluding sibling boundaries."
  [source lines]
  (loop [line-index 0
         continuing-list? false
         spans []]
    (if (< line-index (count lines))
      (let [line (nth lines line-index)
            marker (list-marker source line)]
        (if (and marker
                 (<= (:indent marker) 3)
                 (or continuing-list?
                     (root-list-marker-permitted?
                      source lines line-index marker)))
          (let [[span next-index]
                (list-item-span source lines line-index marker)
                next-marker (when (< next-index (count lines))
                              (list-marker source (nth lines next-index)))]
            (recur next-index
                   (boolean (and next-marker
                                 (<= (:indent next-marker) 3)))
                   (conj spans span)))
          (recur (inc line-index) false spans)))
      spans)))

(defn- merge-protected-spans
  "Sort and union overlapping syntax spans without bridging source gaps."
  [spans]
  (reduce
   (fn [merged [start end]]
     (if-let [[previous-start previous-end] (peek merged)]
       (if (<= start previous-end)
         (conj (pop merged) [previous-start (max previous-end end)])
         (conj merged [start end]))
       [[start end]]))
   []
   (sort-by first spans)))

(defn- span-start-protected?
  "Whether a candidate begins inside an already authoritative syntax span."
  [protected-spans [candidate-start]]
  (some (fn [[protected-start protected-end]]
          (<= protected-start candidate-start (dec protected-end)))
        protected-spans))

(defn- exclude-nested-spans
  "Drop lower-precedence syntax guesses born inside stronger contexts."
  [protected-spans candidate-spans]
  (remove #(span-start-protected? protected-spans %) candidate-spans))

(defn- protected-source-spans
  "Return every syntax context whose internal blanks are not boundaries."
  [source]
  (let [lines (source-lines source)
        front-matter (keep identity [(front-matter-span source lines)])
        fences (exclude-nested-spans front-matter (fenced-code-spans source))
        fenced-spans (merge-protected-spans (concat front-matter fences))
        tables (exclude-nested-spans fenced-spans
                                     (gfm-table-spans source lines))
        primary-spans (merge-protected-spans (concat fenced-spans tables))
        indented-code (exclude-nested-spans
                       primary-spans
                       (indented-code-spans source lines (set (map second tables))))
        code-spans (merge-protected-spans
                    (concat primary-spans indented-code))
        list-items (exclude-nested-spans
                    code-spans (list-item-spans source lines))]
    (merge-protected-spans (concat code-spans list-items))))

(defn- blank-line-separator-end
  "Return the exclusive end of a blank-line separator starting at `index`.

  A separator contains at least two line breaks, with only horizontal blank
  characters between them. Additional blank lines belong to the same
  separator. Line-ending bytes are never normalized."
  [source index]
  (when-let [first-break-end (line-break-end source index)]
    (let [second-break-start
          (horizontal-whitespace-end source first-break-end)]
      (when-let [second-break-end (line-break-end source second-break-start)]
        (loop [separator-end second-break-end]
          (let [next-break-start
                (horizontal-whitespace-end source separator-end)]
            (if-let [next-break-end (line-break-end source next-break-start)]
              (recur next-break-end)
              separator-end)))))))

(declare partition-source)

(defn exact-source-parts?
  "Whether `parts` are the complete admissible partition of `source`.

  Translation manifests require a nonblank source and a nonempty vector of
  nonblank members. The members must also equal this server-owned canonical
  partition; accepting an arbitrary lossless partition would permit two
  authoritative manifests for the same source revision."
  [source parts]
  (and (string? source)
       (not (str/blank? source))
       (vector? parts)
       (seq parts)
       (every? string? parts)
       (= source (apply str parts))
       (every? (complement str/blank?) parts)
       (= parts (partition-source source))))

(defn- partition-source
  "Partition one nonblank source at complete blank-line separators."
  [source]
  (loop [part-start 0
         scan-index 0
         protected-spans (protected-source-spans source)
         parts []]
    (if (< scan-index (count source))
      (let [[protected-start protected-end] (first protected-spans)]
        (cond
          (= scan-index protected-start)
          (recur part-start protected-end (next protected-spans) parts)

          :else
          (if-let [separator-end
                   (blank-line-separator-end source scan-index)]
            (let [candidate (subs source part-start separator-end)]
              (if (not (str/blank? candidate))
                (recur separator-end separator-end protected-spans
                       (conj parts candidate))
                (recur part-start separator-end protected-spans parts)))
            (recur part-start (inc scan-index) protected-spans parts))))
      (let [remainder (subs source part-start)]
        (cond
          (empty? remainder) parts
          (not (str/blank? remainder)) (conj parts remainder)
          :else (update parts (dec (count parts)) str remainder))))))

(defn source-parts
  "Return deterministic, exact paragraph members for `source`.

  The server can pass this result directly to the translation split-manifest
  constructor. The UI does not need to rediscover paragraph boundaries. Blank
  sources are refused because they cannot produce an admissible manifest."
  [source]
  (when-not (string? source)
    (throw (ex-info "translation source text must be a string"
                    {:source source})))
  (when (str/blank? source)
    (throw (ex-info "translation source text must not be blank"
                    {:source source})))
  (let [parts (partition-source source)]
    (when-not (exact-source-parts? source parts)
      (throw (ex-info "translation source partition lost source text"
                      {:source source
                       :source-parts parts})))
    parts))
