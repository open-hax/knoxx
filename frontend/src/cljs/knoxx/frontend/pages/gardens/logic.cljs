(ns knoxx.frontend.pages.gardens.logic
  "Pure logic for the gardens admin page. CLJS port of the helpers and
   request construction in src/pages/GardensPage.tsx."
  (:require [clojure.string :as str]))

(def themes
  [{:value "monokai" :label "Monokai"
    :colors {:bg "#272822" :text "#f8f8f2" :accent "#a6e22e"}}
   {:value "night-owl" :label "Night Owl"
    :colors {:bg "#011627" :text "#d6deeb" :accent "#82aaff"}}
   {:value "proxy-console" :label "Proxy Console"
    :colors {:bg "#0a0a0a" :text "#e0e0e0" :accent "#00d4ff"}}])

(defn theme-info [theme]
  (first (filter #(= theme (:value %)) themes)))

(def available-languages
  [{:code "es" :name "Español"} {:code "fr" :name "Français"}
   {:code "de" :name "Deutsch"} {:code "ja" :name "日本語"}
   {:code "zh" :name "中文"} {:code "ko" :name "한국어"}
   {:code "pt" :name "Português"} {:code "ru" :name "Русский"}
   {:code "it" :name "Italiano"} {:code "ar" :name "العربية"}
   {:code "hi" :name "हिन्दी"} {:code "nl" :name "Nederlands"}
   {:code "pl" :name "Polski"} {:code "tr" :name "Türkçe"}
   {:code "vi" :name "Tiếng Việt"}])

(defn language-name [code]
  (or (:name (first (filter #(= code (:code %)) available-languages)))
      code))

(defn toggle-language [langs code]
  (if (some #{code} langs)
    (filterv #(not= code %) langs)
    (conj (vec langs) code)))

(def blank-form
  {:garden-id "" :title "" :description "" :theme "monokai"
   :status "active" :target-languages [] :auto-translate true})

(defn form-from-garden
  "Edit-form state prefilled from an existing garden."
  [garden]
  {:garden-id (:garden_id garden)
   :title (:title garden)
   :description (or (:description garden) "")
   :theme (or (:theme garden) "monokai")
   :status (:status garden)
   :target-languages (vec (or (:target_languages garden) []))
   :auto-translate (if (some? (:auto_translate garden)) (:auto_translate garden) true)})

(defn validate-form [{:keys [garden-id title]}]
  (when (or (str/blank? garden-id) (str/blank? title))
    "Garden ID and title are required"))

(defn build-save-request
  "Save request {:url :method :body} — POST to create, PATCH to update."
  [{:keys [garden-id title description theme status target-languages auto-translate]} editing?]
  (if editing?
    {:url (str "/api/openplanner/v1/gardens/" (js/encodeURIComponent garden-id))
     :method "PATCH"
     :body {:title title
            :description description
            :theme theme
            :status status
            :target_languages target-languages
            :auto_translate auto-translate}}
    {:url "/api/openplanner/v1/gardens"
     :method "POST"
     :body {:garden_id (str/trim garden-id)
            :title title
            :description description
            :theme theme
            :target_languages target-languages
            :auto_translate auto-translate}}))

(defn garden-html-url [garden-id]
  (str "/api/openplanner/v1/public/gardens/" garden-id "/html"))
