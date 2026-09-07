(ns knoxx.frontend.law.migration-html
  "Admission contract for authored HTML outside the migration source census."
  (:require [clojure.string :as str]))

(defn- active-data-document? [{:keys [element mime-type] attribute-name :name}]
  (and (contains? #{["iframe" "src"] ["frame" "src"] ["embed" "src"]
                    ["object" "data"] ["a" "href"] ["a" "xlink:href"]
                    ["area" "href"] ["form" "action"]
                    ["button" "formaction"] ["input" "formaction"]}
                  [element attribute-name])
       (or (contains? #{"text/html" "text/xml" "application/xml"} mime-type)
           (and (re-matches #"[!#$%&'*+.^_`|~0-9a-z-]+/[!#$%&'*+.^_`|~0-9a-z-]+" mime-type)
                (str/ends-with? mime-type "+xml")))))

(defn assert-entrypoint!
  "Allow the existing Shadow bootstrap without admitting uncounted HTML code."
  [{:keys [path scripts handlers script-urls data-urls base-hrefs] :as facts}]
  (let [unsupported-script (some #(when-not (and (= "/cljs/app.js" (:source %))
                                                (:html? %)
                                                (not (:inline-code? %))) %)
                                 scripts)
        active-data-urls (vec (filter active-data-document? data-urls))]
    (when (or unsupported-script (seq handlers) (seq script-urls)
              (seq active-data-urls) (seq base-hrefs))
      (throw (ex-info "HTML execution leaves governed frontend source tree"
                      {:path path :script unsupported-script
                       :handlers handlers :script-urls script-urls
                       :data-urls active-data-urls
                       :base-hrefs base-hrefs}))))
  facts)
