(ns knoxx.backend.law.policy-values
  "Pure policy identifiers, role aliases and bootstrap configuration normalization."
  (:require [clojure.string :as str]))

(defn slugify [value fallback]
  (let [s (-> (str value "")
              str/trim str/lower-case
              (str/replace #"[^a-z0-9]+" "-")
              (str/replace #"^[-]+|[-]+$" ""))]
    (if (str/blank? s) fallback s)))

(defn normalize-email [v]
  (some-> v str str/trim str/lower-case not-empty))

(defn normalize-actor-id [v]
  (some-> v str str/trim not-empty))

(defn unique [vs]
  (vec (distinct (filter some? vs))))

(defn keywordish-id
  [value]
  (cond
    (keyword? value) (some-> value name str/trim not-empty)
    (string? value) (some-> value str str/trim not-empty)
    (nil? value) nil
    :else (some-> value str str/trim not-empty)))

(defn role-slug-aliases
  [slug]
  (let [s (some-> slug str str/trim not-empty)]
    (->> [s
          (some-> s (str/replace #"_" "-"))
          (some-> s (str/replace #"-" "_"))
          (when s (slugify s s))]
         (remove str/blank?)
         distinct
         vec)))

(defn requested-role-slugs
  [role-slugs]
  (->> role-slugs
       (map #(some-> % str str/trim not-empty))
       (remove nil?)
       distinct
       vec))

(defn known-contract-role-slugs
  [records]
  (->> records
       (filter #(= "roles" (:contractClass %)))
       (keep #(some-> (or (:id %) (get-in % [:contract :role/id])) str str/trim not-empty))
       set))

(defn rolePriority [slug]
  (case slug "system_admin" 100 "system-admin" 100 "org_admin" 90 "org-admin" 90
    "developer" 80 "data_analyst" 70 "data-analyst" 70 "knowledge_worker" 60 "knowledge-worker" 60 0))

(defn actor-projection-role-slugs
  [actor]
  (->> (or (:actor/roles actor) [])
       (map (fn [role]
              (cond
                (keyword? role) (-> role name (str/replace #"_" "-"))
                (string? role) (-> role str/trim (str/replace #"_" "-"))
                :else nil)))
       (remove str/blank?)
       distinct
       vec))

(defn contract-records-by-class
  [records contract-class]
  (->> records (filter #(= contract-class (:contractClass %))) vec))

(defn role-record-slug
  [rec]
  (some-> (or (:id rec) (get-in rec [:contract :role/id])) str str/trim not-empty))

(defn role-display-name
  [slug contract]
  (or (some-> (:role/label contract) str str/trim not-empty)
      (some-> (:role/name contract) str str/trim not-empty)
      (->> (str/split slug #"[-_]+")
           (remove str/blank?)
           (map str/capitalize)
           (str/join " "))))

(defn- split-bootstrap-values [value]
  (->> (str/split (str value) #"[\s,]+")
       (map str/trim)
       (remove str/blank?)
       distinct
       vec))

(defn previous-bootstrap-system-admin-emails
  "Canonical credential identifiers that were previously configured as the
   bootstrap administrator. The historical default is known without operator
   input; custom prior identities must be named explicitly during migration."
  [opts current-email]
  (->> (concat ["system-admin@open-hax.local"]
               (split-bootstrap-values
                (or (:bootstrapSystemAdminPreviousEmails opts)
                    (:bootstrap-system-admin-previous-emails opts)
                    "")))
       (map str/lower-case)
       (remove #(= % (some-> current-email str str/lower-case)))
       distinct
       vec))

(defn self-org-slug
  [email]
  (let [normalized (or (normalize-email email) "user")]
    (slugify (str "self-" (str/replace normalized #"@" "-at-")) "self-user")))

(defn bootstrap-allowlist-emails [opts]
  (->> (split-bootstrap-values (or (:bootstrapAllowlistEmails opts) (:bootstrap-allowlist-emails opts) ""))
       (map str/lower-case)
       vec))

(defn bootstrap-allowlist-role-slugs [opts]
  (let [role-slugs (split-bootstrap-values (or (:bootstrapAllowlistRoleSlugs opts)
                                               (:bootstrap-allowlist-role-slugs opts)
                                               ""))]
    (if (seq role-slugs) role-slugs ["knowledge-worker"])))
