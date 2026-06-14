(ns knoxx.frontend.pages.translations.model-section
  "Translation pipeline model config. Helix port of
   src/components/admin-page/TranslationModelSection.tsx."
  (:require [helix.core :as hx :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.pages.translations.api :as api]))

(defn- run-load! [{:keys [set-loading! set-error! set-notice! set-models!
                          set-current! set-draft! set-updated-at!]}]
  (set-loading! true)
  (set-error! nil)
  (set-notice! nil)
  (-> (js/Promise.all #js [(api/pipeline-config)
                           (-> (api/list-proxx-models) (.catch (fn [_] [])))])
      (.then (fn [[config models]]
               (set-models! models)
               (set-current! (:model config))
               (set-draft! (:model config))
               (set-updated-at! (:updated_at config))))
      (.catch (fn [^js err] (set-error! (or (.-message err) (str err)))))
      (.finally #(set-loading! false))))

(defn- run-save! [draft {:keys [set-saving! set-error! set-notice!
                                set-current! set-draft! set-updated-at!]}]
  (let [normalized (.trim draft)]
    (if (empty? normalized)
      (set-error! "Model is required")
      (do (set-saving! true)
          (set-error! nil)
          (set-notice! nil)
          (-> (api/update-pipeline-config normalized)
              (.then (fn [updated]
                       (set-current! (:model updated))
                       (set-draft! (:model updated))
                       (set-updated-at! (:updated_at updated))
                       (set-notice! {:tone :success
                                     :text (str "Translation model updated to " (:model updated) ".")})))
              (.catch (fn [^js err]
                        (set-notice! {:tone :error :text (or (.-message err) (str err))})))
              (.finally #(set-saving! false)))))))

(defnc model-config-fields
  [{:keys [models current draft set-draft updated-at saving can-manage on-refresh on-save]}]
  (d/div {:class-name "grid gap-3 lg:grid-cols-[1fr_auto] lg:items-end"}
         (d/label {:class-name "space-y-1 block"}
                  (d/div {:class-name "text-xs font-semibold uppercase tracking-wide text-slate-400"}
                         "Translation model")
                  (d/input {:list "translation-model-options"
                            :value draft
                            :on-change #(set-draft (.. % -target -value))
                            :disabled (or (not can-manage) saving)
                            :placeholder (or (not-empty current) "glm-5")
                            :class-name "w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"})
                  (d/datalist {:id "translation-model-options"}
                              (for [{:keys [id]} models]
                                (d/option {:key (str "translation-model-" id) :value id})))
                  (d/div {:class-name "text-xs text-slate-500"}
                         "Current: "
                         (d/span {:class-name "text-slate-200"} (or (not-empty current) "(unknown)"))
                         (when updated-at
                           (d/span {:class-name "ml-2"} (str "(updated " updated-at ")")))))
         (d/div {:class-name "flex gap-2"}
                (d/button {:type "button"
                           :on-click on-refresh
                           :disabled saving
                           :class-name "inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"}
                          "Refresh")
                (d/button {:type "button"
                           :on-click on-save
                           :disabled (or (not can-manage) saving)
                           :class-name "inline-flex items-center justify-center rounded-lg bg-sky-600 px-4 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"}
                          (if saving "Saving…" "Save")))))

(defnc model-section
  "Pipeline model config card (port of TranslationModelSection)."
  [{:keys [can-manage]}]
  (let [[loading set-loading!] (hooks/use-state true)
        [saving set-saving!] (hooks/use-state false)
        [notice set-notice!] (hooks/use-state nil)
        [error set-error!] (hooks/use-state nil)
        [models set-models!] (hooks/use-state [])
        [current set-current!] (hooks/use-state "")
        [draft set-draft!] (hooks/use-state "")
        [updated-at set-updated-at!] (hooks/use-state nil)
        setters {:set-loading! set-loading! :set-saving! set-saving!
                 :set-error! set-error! :set-notice! set-notice!
                 :set-models! set-models! :set-current! set-current!
                 :set-draft! set-draft! :set-updated-at! set-updated-at!}
        load! #(run-load! setters)]
    (hooks/use-effect [] (load!) nil)
    (d/div {:class-name "rounded-xl border border-slate-800 bg-slate-950/60 p-4"}
           (d/div {:class-name "mb-1 text-sm font-semibold text-slate-100"} "Translation pipeline")
           (d/p {:class-name "mb-4 text-xs text-slate-500"}
                "Controls the model used by the translation worker when it starts Knoxx translator agent sessions.")
           (if loading
             (d/div {:class-name "text-sm text-slate-300"} "Loading translation config…")
             (d/div {:class-name "space-y-4"}
                    ($ model-config-fields {:models models :current current :draft draft
                                            :set-draft set-draft! :updated-at updated-at
                                            :saving saving :can-manage can-manage
                                            :on-refresh load!
                                            :on-save #(run-save! draft setters)})
                    (when-not can-manage
                      (d/div {:class-name "rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200"}
                             "You do not have "
                             (d/code {:class-name "font-mono"} "org.translations.manage")
                             " permission."))
                    (when notice
                      (d/div {:class-name (if (= :success (:tone notice))
                                            "rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-200"
                                            "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200")}
                             (:text notice)))
                    (when error
                      (d/div {:class-name "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"}
                             error)))))))
