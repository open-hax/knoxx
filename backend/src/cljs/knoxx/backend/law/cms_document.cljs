(ns knoxx.backend.law.cms-document
  (:require [clojure.string :as str]))
(defn require-id! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9_-]{1,100}" value))
    (throw (ex-info "Invalid CMS identity" {:status 400}))) value)

(defn- page-number! [field value default maximum]
  (let [number (if (nil? value) default
                  (when (and (string? value) (re-matches #"[0-9]{1,10}" value))
                    (parse-long value)))]
    (when-not (and (integer? number) (<= (if (= field :limit) 1 0) number maximum))
      (throw (ex-info "Invalid CMS pagination" {:status 400 :field field})))
    number))

(defn require-page!
  "Validate HTTP pagination before opening the store. Limit defaults to 100."
  [query]
  {:limit (page-number! :limit (:limit query) 100 1000)
   :offset (page-number! :offset (:offset query) 0 2147483647)})
(defn require-body! [body]
  (when-not (and (map? body) (string? (:title body)) (<= 1 (count (str/trim (:title body))) 200)
                 (string? (:content body)) (<= (count (:content body)) 262144)
                 (contains? #{nil "internal" "review"} (:visibility body))
                 (or (nil? (:metadata body))
                     (and (map? (:metadata body)) (<= (count (pr-str (:metadata body))) 65536))))
    (throw (ex-info "CMS requires a title, bounded text/metadata and internal or review visibility" {:status 400}))) body)

(defn require-parents! [id body]
  (when (and id (not (contains? body :parents)))
    (throw (ex-info "Load a document revision before saving" {:status 428})))
  (let [parents (or (:parents body) [])]
    (when-not (and (vector? parents) (every? string? parents)
                   (= (count parents) (count (distinct parents)))
                   (if id (seq parents) (empty? parents)))
      (throw (ex-info "CMS updates require the revisions observed by the editor" {:status 400}))))
  body)

(defn require-logical-source! [value]
  (when (some? value)
    (when-not (and (string? value) (<= 1 (count value) 1024)
                   (not (re-find #"[\\\u0000-\u001f\u007f]" value))
                   (not (str/starts-with? value "/"))
                   (not (re-find #"^[A-Za-z]:" value))
                   (every? #(not (contains? #{"" "." ".."} %)) (str/split value #"/" -1)))
      (throw (ex-info "CMS logical source must be a safe relative identifier" {:status 400}))))
  value)
