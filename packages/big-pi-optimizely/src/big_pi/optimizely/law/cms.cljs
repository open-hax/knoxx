;; Local subset informed by https://docs.optimizely.com/cms-saas/api ; not vendor parity.
(ns big-pi.optimizely.law.cms
  (:require [clojure.string :as str]))

(defn safe-key? "Accept a traversal-free local resource key." [x]
  (and (string? x) (boolean (re-matches #"[A-Za-z0-9_-]+" x))))
(defn locale? "Accept supported language-tag syntax; installed locales are checked separately." [x]
  (and (string? x) (boolean (re-matches #"[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*" x))))
(defn display-name? "Enforce local display-name length and nonblank constraints." [x]
  (and (string? x) (<= 1 (count (str/trim x))) (<= (count x) 255)))
(defn valid-fields? "Reject keys outside the supported local shape." [m allowed]
  (and (map? m) (every? allowed (keys m))))

(def version-fields "Version fields accepted from clients." #{:displayName :locale :properties :status})
(def content-fields "Content fields accepted from clients." #{:key :contentType :container :initialVersion})
(def type-fields "Content type fields accepted from clients." #{:key :displayName :properties})
(def locale-fields "Locale fields accepted from clients." #{:key :displayName})

(defn version? "Validate a complete version or a merge patch's fields." [v partial?]
  (and (valid-fields? v version-fields)
       (or partial? (and (display-name? (:displayName v)) (locale? (:locale v)) (map? (:properties v))))
       (or (not (contains? v :displayName)) (nil? (:displayName v)) (display-name? (:displayName v)))
       (or (not (contains? v :locale)) (nil? (:locale v)) (locale? (:locale v)))
       (or (not (contains? v :properties)) (nil? (:properties v)) (map? (:properties v)))
       (or (not (contains? v :status)) (= "draft" (:status v)))))

(defn content? "Validate a local create-content request." [v]
  (and (valid-fields? v content-fields)
       (or (nil? (:key v)) (safe-key? (:key v)))
       (safe-key? (:contentType v))
       (version? (:initialVersion v) false)))

(defn entity? "Validate a type or locale, with partial input allowed for patches." [v fields partial?]
  (and (valid-fields? v fields)
       (or partial? (and (safe-key? (:key v)) (display-name? (:displayName v))
                         (or (not (contains? fields :properties)) (map? (:properties v)))
                         (or (not= fields locale-fields) (locale? (:key v)))))
       (or (not (contains? v :key)) (safe-key? (:key v)))
       (or (not (contains? v :displayName)) (nil? (:displayName v)) (display-name? (:displayName v)))
       (or (not (contains? v :properties)) (nil? (:properties v)) (map? (:properties v)))))

(defn paging "Validate pagination and optionally version filter parameters." [query filters?]
  (let [parse (fn [s default]
                (if (nil? s) default
                    (when (and (string? s) (re-matches #"[0-9]+" s))
                      (let [n (parse-long s)] (when (and n (<= n 10000)) n)))))
        index (parse (get query "pageIndex") 0)
        size (parse (get query "pageSize") 100)]
    (when (and index size (<= 1 size 10000)
               (every? (if filters? #{"pageIndex" "pageSize" "locales" "statuses"}
                                    #{"pageIndex" "pageSize"}) (keys query))
               (every? #(or (nil? %) (and (vector? %) (every? string? %)))
                       [(get query "locales") (get query "statuses")]))
      {:pageIndex index :pageSize size})))

(defn request? "Validate the decoded HTTP request boundary." [r]
  (and (map? r) (keyword? (:method r)) (string? (:path r))
       (map? (:query r)) (map? (:headers r)) (map? (:actor r))
       (every? string? (keys (:query r))) (every? string? (keys (:headers r)))))
