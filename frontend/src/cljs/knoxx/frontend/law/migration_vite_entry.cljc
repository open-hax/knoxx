(ns knoxx.frontend.law.migration-vite-entry
  "Admission of the inspected HTML entry used by the required browser build.")

(defn assert-default-entry!
  "Require default Vite inputs to identify only the HTML entry inspected by the census."
  [{:keys [path expected entries] :as facts}]
  (when-not (and (seq entries) (every? #(= expected %) entries))
    (throw (ex-info "Default Vite entry leaves governed HTML inventory"
                    {:path path :expected expected :entries entries})))
  facts)
