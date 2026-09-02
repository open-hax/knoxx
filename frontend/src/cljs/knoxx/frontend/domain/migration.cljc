(ns knoxx.frontend.domain.migration
  "Pure assembly of source facts into the canonical frontend migration ledger."
  (:require [clojure.string :as str]
            [knoxx.frontend.law.migration :as law]
            [knoxx.frontend.shape.migration :as shape]))

(defn source-stem
  "Remove TypeScript and test suffixes for sibling test association."
  [path]
  (-> path
      (str/replace #"\.(?:test|spec)\.tsx?$" "")
      (str/replace #"\.tsx?$" "")))

(defn tests-by-source
  "Index legacy test suites by their probable sibling source stem."
  [sources]
  (->> sources
       (filter (comp shape/test-source? :path))
       (group-by (comp source-stem :path))
       (map (fn [[stem tests]] [stem (mapv :path tests)]))
       (into {})))

(defn assemble-records
  "Build, validate, and deterministically order all manifest records."
  [{:keys [sources bridge-exports routes]}]
  (let [test-index (tests-by-source sources)
        file-records (map (fn [{:keys [path] :as source}]
                            (shape/legacy-file-record
                             (assoc source :tests (if (shape/test-source? path)
                                                    []
                                                    (get test-index (source-stem path) [])))))
                          sources)
        suite-records (->> file-records
                           (filter #(= :test (:role %)))
                           (map (fn [record]
                                  (shape/legacy-test-suite-record
                                   {:path (:path record)
                                    :island (:island record)
                                    :disposition (:disposition record)}))))
        records (concat file-records bridge-exports routes suite-records)]
    (->> records
         (sort-by :record/id)
         vec
         law/assert-manifest!)))
