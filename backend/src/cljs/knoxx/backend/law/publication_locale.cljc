(ns knoxx.backend.law.publication-locale
  "Portable locale contracts for publication resources and artifacts."
  (:require [malli.core :as m]))

(defn locale-keyword?
  "True for an unqualified language tag keyword, optionally with a variant."
  [value]
  (and (keyword? value)
       (nil? (namespace value))
       (boolean (re-matches #"[A-Za-z]{2,3}(-[A-Za-z0-9]{1,8})*" (name value)))))

(def Locale
  "A single unqualified language tag keyword."
  [:and keyword? [:fn locale-keyword?]])

(def LocaleCatalog
  "A target's explicit, non-empty, duplicate-free catalog of accepted locales."
  [:and [:vector Locale]
   [:fn {:error/message "a publication target must declare one or more distinct accepted locales"}
    #(and (seq %) (= (count %) (count (distinct %))))]])

(def ArtifactLocaleConflict
  "The renderer artifact and publication intent disagree about the locale whose
   bytes may be exposed. Values remain `:any` to preserve invalid refusal evidence."
  [:map
   [:conflict/type [:= :publication/artifact-locale-conflict]]
   [:conflict/artifact-locale :any]
   [:conflict/publication-locale :any]])

(defn artifact-locale-conflict
  "nil when `artifact` and `intent` name the same locale; otherwise return the
   typed conflict that preserves both values for a failed receipt."
  [artifact intent]
  (when (not= (:artifact/locale artifact) (:publication/locale intent))
    {:conflict/type :publication/artifact-locale-conflict
     :conflict/artifact-locale (:artifact/locale artifact)
     :conflict/publication-locale (:publication/locale intent)}))

(defn artifact-locale-conflict?
  "True when `value` is the typed locale-identity conflict."
  [value]
  (m/validate ArtifactLocaleConflict value))
