(ns knoxx.frontend.pages.gardens.view
  "Gardens admin page. Helix port of src/pages/GardensPage.tsx.
   No router/auth dependencies — the whole page is node-testable."
  (:require [clojure.string :as str]
            [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.components.ui :as ui]
            [knoxx.frontend.pages.gardens.api :as api]
            [knoxx.frontend.pages.gardens.logic :as logic]))

(defnc theme-badge [{:keys [theme]}]
  (when-let [{:keys [label colors]} (logic/theme-info theme)]
    (d/span {:class-name "rounded px-2 py-0.5 text-xs font-medium"
             :style #js {:backgroundColor (:bg colors)
                         :color (:accent colors)
                         :border (str "1px solid " (:accent colors) "40")}}
            label)))

(defnc theme-preview [{:keys [theme]}]
  (when-let [{:keys [label colors]} (logic/theme-info theme)]
    (d/div {:class-name "rounded-lg p-4 text-sm"
            :style #js {:backgroundColor (:bg colors)
                        :color (:text colors)
                        :fontFamily "monospace"}}
           (d/div {:style #js {:color (:accent colors)}} "# Preview")
           (d/p {:class-name "mt-2 opacity-80"}
                (str "This is how your garden will look with the " label " theme."))
           (d/pre {:class-name "mt-2 rounded p-2 opacity-90"
                   :style #js {:background "rgba(255,255,255,0.05)"}}
                  "const greeting = \"Hello\";\nconsole.log(greeting);"))))

(defn- form-label [text]
  (d/label {:class-name "mb-1 block text-sm font-medium"} text))

(defn- form-select [value on-change options]
  (d/select {:value value
             :on-change #(on-change (.. % -target -value))
             :class-name "w-full rounded-lg border border-slate-700 bg-slate-900 p-2 text-sm text-slate-100"}
            (for [{:keys [value label]} options]
              (d/option {:key value :value value} label))))

(defnc language-picker [{:keys [selected on-toggle]}]
  (d/div {:class-name "flex flex-wrap gap-2"}
         (for [{:keys [code name]} logic/available-languages
               :let [selected? (boolean (some #{code} selected))]]
           (d/button {:key code
                      :type "button"
                      :on-click #(on-toggle code)
                      :class-name (str "rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors "
                                       (if selected?
                                         "border-cyan-500 bg-cyan-950/30 text-cyan-300"
                                         "border-slate-600 bg-slate-800 text-slate-400 hover:border-slate-400"))}
                     name
                     (when selected? (d/span {:class-name "ml-1.5 text-cyan-500"} "✓"))))))

(defnc garden-form-fields
  [{:keys [form update! editing?]}]
  (d/div {:class-name "grid gap-4 md:grid-cols-2"}
              (d/div
               (form-label "Garden ID")
               ($ ui/input {:value (:garden-id form)
                            :on-change #(update! :garden-id (.. % -target -value))
                            :placeholder "my-garden-id"
                            :disabled editing?})
               (d/p {:class-name "mt-1 text-xs text-slate-500"}
                    "Unique identifier (slug format, cannot be changed after creation)"))
              (d/div
               (form-label "Title")
               ($ ui/input {:value (:title form)
                            :on-change #(update! :title (.. % -target -value))
                            :placeholder "My Garden"}))
              (d/div {:class-name "md:col-span-2"}
                     (form-label "Description")
                     (d/textarea {:value (:description form)
                                  :on-change #(update! :description (.. % -target -value))
                                  :placeholder "Brief description of this garden"
                                  :rows 2
                                  :class-name "w-full rounded-lg border border-slate-700 bg-slate-900 p-2 text-sm text-slate-100"}))
              (d/div
               (form-label "Theme")
               (form-select (:theme form) #(update! :theme %)
                            (map #(select-keys % [:value :label]) logic/themes)))
              (when editing?
                (d/div
                 (form-label "Status")
                 (form-select (:status form) #(update! :status %)
                              [{:value "active" :label "Active"}
                               {:value "draft" :label "Draft"}
                               {:value "archived" :label "Archived"}])))
              (d/div {:class-name "md:col-span-2"}
                     (form-label "Target Languages")
                     (d/p {:class-name "mb-2 text-xs text-slate-500"}
                          "Select languages for automatic translation. Published documents will be translated to these languages.")
                     ($ language-picker {:selected (:target-languages form)
                                         :on-toggle #(update! :target-languages
                                                              (logic/toggle-language (:target-languages form) %))})
                     (when (seq (:target-languages form))
                       (d/div {:class-name "mt-3 flex items-center gap-2"}
                              (d/input {:type "checkbox"
                                        :id "auto-translate"
                                        :checked (:auto-translate form)
                                        :on-change #(update! :auto-translate (.. % -target -checked))
                                        :class-name "h-4 w-4 rounded border-slate-600 text-cyan-600"})
                              (d/label {:html-for "auto-translate"
                                        :class-name "text-sm text-slate-400"}
                                       "Automatically translate new publications"))))
              (d/div {:class-name "md:col-span-2"}
                     (form-label "Theme Preview")
                     ($ theme-preview {:theme (:theme form)}))))

(defnc garden-form
  [{:keys [form set-form editing? saving on-save on-cancel]}]
  (let [update! (fn [k v] (set-form (assoc form k v)))]
    ($ ui/card {:variant :elevated :padding :lg}
       (d/div {:class-name "mb-4 flex items-start justify-between"}
              (d/h2 {:class-name "text-lg font-semibold"}
                    (if editing? "Edit Garden" "Create Garden"))
              ($ ui/button {:variant :ghost :size :sm :on-click on-cancel} "✕"))
       ($ garden-form-fields {:form form :update! update! :editing? editing?})
       (d/div {:class-name "mt-6 flex gap-2"}
              ($ ui/button {:variant :primary :loading saving :on-click on-save}
                 (if editing? "Save Changes" "Create Garden"))
              ($ ui/button {:variant :ghost :on-click on-cancel} "Cancel")))))

(defnc garden-card
  [{:keys [garden saving confirming? on-edit on-delete on-confirm on-cancel-confirm]}]
  (let [{:keys [garden_id title description status theme target_languages]} garden]
    ($ ui/card {:variant :elevated :padding :md}
       (d/div {:class-name "flex items-start justify-between gap-3"}
              (d/div
               (d/h2 {:class-name "text-lg font-semibold"} title)
               (d/p {:class-name "mt-1 text-sm text-slate-400"} description))
              (d/div {:class-name "flex items-center gap-2"}
                     ($ theme-badge {:theme theme})
                     (d/span {:class-name "rounded-full bg-slate-800 px-2 py-1 text-xs font-medium text-slate-200"}
                             garden_id)))
       (d/div {:class-name "mt-4 flex items-center gap-4 text-sm"}
              (d/span {:class-name (str "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium "
                                        (if (= status "active")
                                          "bg-green-900/30 text-green-300"
                                          "bg-slate-700 text-slate-300"))}
                      status)
              (when (seq target_languages)
                (d/span {:class-name "inline-flex items-center gap-1 text-xs text-slate-400"}
                        (d/span "🌐")
                        (str/join ", " (map logic/language-name target_languages)))))
       (d/div {:class-name "mt-5 flex gap-2"}
              ($ ui/button {:variant :primary :size :sm
                            :on-click #(js/window.open (logic/garden-html-url garden_id) "_blank")}
                 "View Garden")
              ($ ui/button {:variant :secondary :size :sm :on-click #(on-edit garden)} "Edit")
              (if confirming?
                (hx/<>
                 ($ ui/button {:variant :primary :size :sm :loading saving
                               :on-click #(on-delete garden_id)}
                    "Confirm Delete")
                 ($ ui/button {:variant :ghost :size :sm :on-click on-cancel-confirm} "Cancel"))
                ($ ui/button {:variant :ghost :size :sm :on-click #(on-confirm garden_id)} "Delete"))))))

(defn- run-load! [{:keys [set-gardens! set-loading! set-error!]}]
  (set-loading! true)
  (set-error! nil)
  (-> (api/list-gardens)
      (.then #(set-gardens! (vec (:gardens %))))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-loading! false))))

(defn- run-save!
  [form editing {:keys [set-saving! set-error! set-notice!]} on-success]
  (if-let [problem (logic/validate-form form)]
    (set-error! problem)
    (do (set-saving! true)
        (set-error! nil)
        (set-notice! nil)
        (-> (api/save-garden (logic/build-save-request form (some? editing)))
            (.then (fn []
                     (set-notice! (str "Garden \"" (:title form) "\" "
                                       (if editing "updated" "created")
                                       " successfully"))
                     (on-success)))
            (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
            (.finally #(set-saving! false))))))

(defn- run-delete!
  [garden-id {:keys [set-saving! set-error! set-notice! set-confirming!]} on-success]
  (set-confirming! nil)
  (set-saving! true)
  (set-error! nil)
  (-> (api/delete-garden garden-id)
      (.then (fn []
               (set-notice! (str "Garden \"" garden-id "\" deleted"))
               (on-success)))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-saving! false))))

(defn- notice-banner [notice]
  (d/div {:class-name "rounded-lg border border-green-900/40 bg-green-950/30 p-4 text-sm text-green-300"}
         notice))

(defn- error-banner [error]
  (d/div {:class-name "rounded-lg border border-red-900/40 bg-red-950/30 p-4 text-sm text-red-300"}
         error))

(defnc gardens-body
  [{:keys [gardens loading error notice saving show-form? editing confirming
           set-confirming form set-form on-new on-save on-cancel on-edit on-delete]}]
  (d/div {:class-name "space-y-4"}
         (d/div {:class-name "flex items-start justify-between"}
                (d/div
                 (d/h1 {:class-name "text-2xl font-bold"} "Gardens")
                 (d/p {:class-name "mt-1 text-sm text-slate-500"}
                      "Publishable operator views with themed markdown rendering."))
                ($ ui/button {:variant :primary :on-click on-new} "+ New Garden"))
         (when notice (notice-banner notice))
         (when loading
           ($ ui/card {:padding :md}
              (d/div {:class-name "text-sm text-slate-500"} "Loading gardens...")))
         (when error (error-banner error))
         (when show-form?
           ($ garden-form {:form form :set-form set-form :editing? (some? editing)
                           :saving saving :on-save on-save :on-cancel on-cancel}))
         (d/div {:class-name "grid gap-4 md:grid-cols-2"}
                (for [garden gardens]
                  ($ garden-card {:key (:garden_id garden)
                                  :garden garden
                                  :saving saving
                                  :confirming? (= confirming (:garden_id garden))
                                  :on-edit on-edit
                                  :on-delete on-delete
                                  :on-confirm set-confirming
                                  :on-cancel-confirm #(set-confirming nil)})))
         (when (and (some? gardens) (empty? gardens) (not loading))
           ($ ui/card {:padding :lg}
              (d/div {:class-name "text-center"}
                     (d/p {:class-name "text-slate-500"} "No gardens yet.")
                     (d/p {:class-name "mt-1 text-sm text-slate-400"}
                          "Create a garden to start publishing markdown documents with themes."))))))

(defnc gardens-page []
  (let [[gardens set-gardens!] (hooks/use-state nil)
        [loading set-loading!] (hooks/use-state true)
        [error set-error!] (hooks/use-state nil)
        [notice set-notice!] (hooks/use-state nil)
        [saving set-saving!] (hooks/use-state false)
        [show-form? set-show-form!] (hooks/use-state false)
        [editing set-editing!] (hooks/use-state nil)
        [confirming set-confirming!] (hooks/use-state nil)
        [form set-form!] (hooks/use-state logic/blank-form)
        setters {:set-gardens! set-gardens! :set-loading! set-loading!
                 :set-error! set-error! :set-notice! set-notice!
                 :set-saving! set-saving! :set-confirming! set-confirming!}
        load! #(run-load! setters)
        reset-form! (fn []
                      (set-form! logic/blank-form)
                      (set-editing! nil)
                      (set-show-form! false))
        start-edit! (fn [garden]
                      (set-editing! garden)
                      (set-form! (logic/form-from-garden garden))
                      (set-show-form! true))
        save! #(run-save! form editing setters (fn [] (reset-form!) (load!)))
        delete! #(run-delete! % setters load!)]
    (hooks/use-effect [] (load!) nil)
    ($ gardens-body {:gardens gardens :loading loading :error error :notice notice
                     :saving saving :show-form? show-form? :editing editing
                     :confirming confirming :set-confirming set-confirming!
                     :form form :set-form set-form!
                     :on-new (fn [] (reset-form!) (set-show-form! true))
                     :on-save save! :on-cancel reset-form!
                     :on-edit start-edit! :on-delete delete!})))
