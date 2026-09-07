(ns knoxx.frontend.law.migration-shadow
  "Contracts for Shadow source and build effects outside the TypeScript inventory.")

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

(defn assert-build-hooks!
  "Reject executable build hooks whose source and output effects are not inventoried."
  [{:keys [path build-hooks] :as facts}]
  (doseq [hooks build-hooks
          :when (not (or (nil? hooks) (and (sequential? hooks) (empty? hooks))))]
    (throw (ex-info "Shadow build hooks require explicit migration inventory support"
                    {:path path :build-hooks hooks})))
  facts)
