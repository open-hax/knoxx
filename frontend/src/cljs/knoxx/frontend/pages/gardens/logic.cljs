(ns knoxx.frontend.pages.gardens.logic
  "Pure projection helpers for the deploy-owned Garden review surface.

   A Garden owns identity and locale membership. Publication intents own paths.
   Content and Puck presentation contracts deliberately do not enter this
   model."
  (:require [clojure.string :as str]))

(def available-languages
  {"en" "English" "es" "Español" "fr" "Français" "de" "Deutsch"
   "ja" "日本語" "zh" "中文" "ko" "한국어" "pt" "Português"
   "ru" "Русский" "it" "Italiano" "ar" "العربية" "hi" "हिन्दी"
   "nl" "Nederlands" "pl" "Polski" "tr" "Türkçe" "vi" "Tiếng Việt"})

(defn language-name [code]
  (get available-languages code code))

(defn- trim-trailing-slash [value]
  (str/replace (or value "") #"/+$" ""))

(defn public-url
  "Join a deployment base URL and a contract-owned publication path."
  [site-url publication-path]
  (str (trim-trailing-slash site-url)
       (if (str/starts-with? publication-path "/") "" "/")
       publication-path))

(defn normalize-deployment
  "Make the backend deployment DTO convenient for the UI without inventing
   mutable Garden state."
  [{:keys [site-url gardens]}]
  {:site-url site-url
   :gardens
   (mapv (fn [{:keys [garden publications]}]
           {:id (:id garden)
            :title (:title garden)
            :status (:status garden)
            :locales (vec (:locales garden))
            :placements
            (->> publications
                 (map (fn [publication]
                        {:id (:id publication)
                         :locale (:locale publication)
                         :path (:path publication)
                         :state (:state publication)
                         :url (public-url site-url (:path publication))}))
                 (sort-by (juxt :locale :path))
                 vec)})
         gardens)})
