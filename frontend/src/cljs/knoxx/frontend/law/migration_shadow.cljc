(ns knoxx.frontend.law.migration-shadow
  "Contracts for local Shadow JavaScript sources outside the TypeScript inventory.")

(defn assert-file-resolutions!
  "Require explicit inventory support for file modules beyond the governed bridges."
  [{:keys [path resolutions] :as facts}]
  (doseq [[module resolution] resolutions
          :when (and (= :file (:target resolution))
                     (not (contains? #{"@open-hax/knoxx-app-bridge"
                                       "@open-hax/knoxx-frontend-bridge"} module)))]
    (throw (ex-info "Shadow file resolution requires explicit migration inventory support"
                    {:path path :module module :resolution resolution})))
  facts)
