(ns knoxx.frontend.law.migration-lifecycle
  "Admission of implicit package-script execution around inspected frontend commands."
  (:require [clojure.string :as str]))

(defn assert-script-hooks!
  "Require inventory support for nonempty pre/post hooks on declared package scripts."
  [{:keys [path scripts] :as facts}]
  (doseq [script-name (keys scripts)
          prefix ["pre" "post"]
          :let [hook-name (str prefix script-name)
                command (get scripts hook-name)]
          :when (not (str/blank? command))]
    (throw (ex-info "Package script lifecycle hooks require explicit migration inventory support"
                    {:path path :script script-name :hook hook-name})))
  facts)
