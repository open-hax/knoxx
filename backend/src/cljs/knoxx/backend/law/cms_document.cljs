(ns knoxx.backend.law.cms-document
  (:require [clojure.string :as str]))
(defn require-id! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9_-]{1,100}" value))
    (throw (ex-info "Invalid CMS identity" {:status 400}))) value)
(defn require-body! [body]
  (when-not (and (map? body) (string? (:title body)) (<= 1 (count (str/trim (:title body))) 200)
                 (string? (:content body)) (<= (count (:content body)) 262144)
                 (contains? #{nil "internal" "review"} (:visibility body)))
    (throw (ex-info "CMS requires a title, bounded text and internal or review visibility" {:status 400}))) body)
