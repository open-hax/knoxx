(ns knoxx.backend.domain.lens.html-markdown
  (:require [clojure.string :as str]))

(defn- strip-dangerous-blocks
  [html]
  (-> (or html "")
      (str/replace #"(?is)<script[^>]*>.*?</script>" "")
      (str/replace #"(?is)<style[^>]*>.*?</style>" "")
      (str/replace #"(?is)<noscript[^>]*>.*?</noscript>" "")))

(defn- convert-headings
  [text]
  (-> text
      (str/replace #"(?is)<h1[^>]*>(.*?)</h1>" "\n# $1\n")
      (str/replace #"(?is)<h2[^>]*>(.*?)</h2>" "\n## $1\n")
      (str/replace #"(?is)<h3[^>]*>(.*?)</h3>" "\n### $1\n")
      (str/replace #"(?is)<h4[^>]*>(.*?)</h4>" "\n#### $1\n")
      (str/replace #"(?is)<h5[^>]*>(.*?)</h5>" "\n##### $1\n")
      (str/replace #"(?is)<h6[^>]*>(.*?)</h6>" "\n###### $1\n")))

(defn- convert-formatting
  [text]
  (-> text
      (str/replace #"(?is)<(strong|b)[^>]*>(.*?)</\1>" "**$2**")
      (str/replace #"(?is)<(em|i)[^>]*>(.*?)</\1>" "*$2*")
      (str/replace #"(?is)<code[^>]*>(.*?)</code>" "`$1`")
      (str/replace #"(?is)<pre[^>]*><code[^>]*>(.*?)</code></pre>" "\n```\n$1\n```\n")))

(defn- convert-links-and-images
  [text]
  (-> text
      (str/replace #"(?is)<a[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>" "[$2]($1)")
      (str/replace #"(?is)<img[^>]*src=\"([^\"]*)\"[^>]*alt=\"([^\"]*)\"[^>]*>" "![$2]($1)")
      (str/replace #"(?is)<img[^>]*src=\"([^\"]*)\"[^>]*>" "![]($1)")))

(defn- convert-structural-tags
  [text]
  (-> text
      (str/replace #"(?is)<li[^>]*>(.*?)</li>" "\n- $1")
      (str/replace #"(?is)<(p|div|tr|blockquote)[^>]*>(.*?)</\1>" "\n\n$2\n\n")
      (str/replace #"(?is)<br\s*/?>" "\n")
      (str/replace #"(?is)<hr\s*/?>" "\n---\n")
      (str/replace #"(?is)<[^>]+>" "")))

(defn- decode-html-entities
  [text]
  (-> text
      (str/replace "&nbsp;" " ")
      (str/replace "&amp;" "&")
      (str/replace "&lt;" "<")
      (str/replace "&gt;" ">")
      (str/replace "&quot;" "\"")
      (str/replace "&#39;" "'")))

(defn html->markdown
  "Converts arbitrary HTML into clean GitHub-Flavored Markdown."
  [html]
  (if (str/blank? html)
    ""
    (-> html
        strip-dangerous-blocks
        convert-headings
        convert-formatting
        convert-links-and-images
        convert-structural-tags
        decode-html-entities
        (str/replace #"\r" "")
        (str/replace #"[ \t]+" " ")
        (str/replace #"\n{3,}" "\n\n")
        str/trim)))
