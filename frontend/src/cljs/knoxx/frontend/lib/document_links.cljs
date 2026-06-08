(ns knoxx.frontend.lib.document-links
  "Document-link resolution, ported from src/lib/document-links.ts.

   Pure string logic — no React, no DOM. This is the canonical implementation;
   the TypeScript copy remains only until its sole consumer (SourceDocPage) is
   migrated, at which point the TS module and its vitest test are retired."
  (:require [clojure.string :as str]))

(defn external-href?
  "True for absolute/protocol-relative URLs and mailto:/tel: links."
  [href]
  (let [href (str href)]
    (boolean
     (or (re-find #"(?i)^(?:[a-z][a-z0-9+.-]*:)?//" href)
         (str/starts-with? href "mailto:")
         (str/starts-with? href "tel:")))))

(defn normalize-relative-doc-path
  "Collapse '.', '..', and empty segments in a slash path."
  [input]
  (->> (str/split (str input) #"/")
       (reduce (fn [stack part]
                 (cond
                   (or (= part "") (= part ".")) stack
                   (= part "..") (if (empty? stack) stack (pop stack))
                   :else (conj stack part)))
               [])
       (str/join "/")))

(defn resolve-document-href
  "Resolve an in-doc href against the current document path. Returns the
   normalized target path, or nil for external/anchor/empty links."
  [current-path href]
  (let [trimmed (str/trim (str href))]
    (if (or (str/blank? trimmed)
            (external-href? trimmed)
            (str/starts-with? trimmed "#"))
      nil
      (let [without-hash (or (first (str/split trimmed #"#")) "")
            without-query (or (first (str/split without-hash #"\?")) "")]
        (cond
          (str/blank? without-query) nil
          (str/starts-with? without-query "/")
          (normalize-relative-doc-path (str/replace without-query #"^/+" ""))
          :else
          (let [base (vec (filter seq (str/split (str current-path) #"/")))
                base (if (empty? base) base (pop base))]
            (normalize-relative-doc-path (str/join "/" (conj base without-query)))))))))
