(ns knoxx.frontend.law.migration-html
  "Admission contract for authored HTML outside the migration source census.")

(defn assert-entrypoint!
  "Allow the existing Shadow bootstrap without admitting uncounted HTML code."
  [{:keys [path scripts handlers script-urls base-hrefs] :as facts}]
  (let [unsupported-script (some #(when-not (and (= "/cljs/app.js" (:source %))
                                                (:html? %)
                                                (not (:inline-code? %))) %)
                                 scripts)]
    (when (or unsupported-script (seq handlers) (seq script-urls) (seq base-hrefs))
      (throw (ex-info "HTML execution leaves governed frontend source tree"
                      {:path path :script unsupported-script
                       :handlers handlers :script-urls script-urls
                       :base-hrefs base-hrefs}))))
  facts)
