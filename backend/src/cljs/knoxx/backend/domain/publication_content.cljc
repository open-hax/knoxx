(ns knoxx.backend.domain.publication-content
  "Pure rendering of receipt-bound text and standalone Markdown media references.

   Prose remains literal. A whole block may reference a staged image, a staged
   audio file, or a canonical Suno song. Code-fenced examples stay literal;
   arbitrary HTML, remote images and players are never interpreted."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication-media :as law]))

(defn- escape-html [value]
  (str/escape value
              {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;" \' "&#39;"}))

(defn- paragraph [block]
  (str "<p>" (-> block escape-html (str/replace "\n" "<br>")) "</p>"))

(defn- reference [block]
  ;; A source file's final line ending does not turn its last media block into
  ;; prose. The original block remains untouched for literal rendering/digests.
  (let [line (str/replace block #"\r?\n$" "")]
    (if-let [[_ label path] (re-matches #"!\[([^\]\n]*)\]\(([^)\n]+)\)" line)]
      (when (law/staged-image-path? path)
        {:kind :image :label label :path path})
      (when-let [[_ label path] (re-matches #"\[([^\]\n]+)\]\(([^)\n]+)\)" line)]
        (cond
          (law/staged-audio-path? path) {:kind :audio :label label :path path}
          (law/suno-song-url? path) {:kind :suno :label label :path path})))))

(defn- render-block [block]
  (if-let [{:keys [kind label path]} (reference block)]
    (let [label (escape-html label)
          path (escape-html path)]
      (case kind
        :image (str "<figure><img src=\"" path "\" alt=\"" label "\" loading=\"lazy\">"
                    "<figcaption>" label "</figcaption></figure>")
        :audio (str "<figure><figcaption>" label "</figcaption>"
                    "<audio controls preload=\"none\" src=\"" path "\"></audio>"
                    "<a href=\"" path "\">" label "</a></figure>")
        :suno (str "<p><a href=\"" path "\" rel=\"noopener noreferrer\">" label "</a></p>")))
    (paragraph block)))

(defn- fence-after [fence line]
  (if-let [[_ marker suffix] (re-matches #" {0,3}(`{3,}|~{3,})(.*)" line)]
    (let [character (subs marker 0 1)]
      (if fence
        (when-not (and (= character (:character fence))
                       (>= (count marker) (:width fence)) (str/blank? suffix))
          fence)
        {:character character :width (count marker)}))
    fence))

(defn render-fragment
  "Render only the approved text blocks; no metadata or external state is read.

   Media bytes stay owned by the separately deployed staged asset tree or Suno.
   Neither an image reference nor a song link transfers asset custody to CMS."
  [blocks]
  (law/require-text-blocks! blocks)
  (let [rendered (reduce (fn [{:keys [fence fragments]} block]
                           {:fence (reduce fence-after fence (str/split-lines block))
                            :fragments (conj fragments (if fence (paragraph block) (render-block block)))})
                         {:fence nil :fragments []}
                         (remove str/blank? blocks))]
    (str "<article class=\"published-document\">"
         (str/join "" (:fragments rendered)) "</article>")))
