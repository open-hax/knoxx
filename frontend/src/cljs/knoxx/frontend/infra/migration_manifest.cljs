(ns knoxx.frontend.infra.migration-manifest
  "Node adapter that validates a generated ND-EDN migration manifest with Malli."
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [knoxx.frontend.law.migration-manifest :as law]
            ["node:fs" :as fs]))

(defn- read-record
  [line-number line]
  (try
    (reader/read-string line)
    (catch :default error
      (throw (ex-info "Frontend migration manifest EDN parse failure"
                      {:line line-number
                       :text line}
                      error)))))

(defn validate-file!
  "Reads one EDN form per nonblank line and validates every record with Malli."
  [path]
  (let [lines (-> (.readFileSync fs path "utf8")
                  (str/split-lines))
        records (->> lines
                     (map-indexed vector)
                     (remove (fn [[_ line]] (str/blank? line)))
                     (mapv (fn [[index line]]
                             (let [record (read-record (inc index) line)]
                               (try
                                 (law/assert-record! record)
                                 (catch :default error
                                   (throw (ex-info "Frontend migration manifest record invalid"
                                                   {:line (inc index)
                                                    :record record
                                                    :explain (:explain (ex-data error))}
                                                   error))))))))]
    (println (str "Frontend migration manifest: " (count records) " Malli-valid records"))
    (count records)))

(defn main
  []
  (if-let [path (-> js/process .-argv (.at 2))]
    (validate-file! path)
    (throw (ex-info "Usage: validate-manifest <path.ndedn>" {}))))
