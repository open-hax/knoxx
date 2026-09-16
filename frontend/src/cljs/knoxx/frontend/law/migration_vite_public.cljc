(ns knoxx.frontend.law.migration-vite-public
  "Admission of copied public assets covered by the frontend HTML census.")

(defn assert-public-directory!
  "Require an inspected public directory or disabled public asset copying."
  [{:keys [path expected actual] :as facts}]
  (when-not (or (nil? actual) (= expected actual))
    (throw (ex-info "Vite public directory leaves governed HTML inventory"
                    {:path path :expected expected :actual actual})))
  facts)
