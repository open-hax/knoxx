(ns knoxx.frontend.pages.gardens.view
  "Read-only review of deploy-owned Garden and publication contracts."
  (:require [helix.core :refer [$ defnc]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [knoxx.frontend.components.ui :as ui]
            [knoxx.frontend.pages.gardens.api :as api]
            [knoxx.frontend.pages.gardens.logic :as logic]))

(defn- status-pill [status]
  (d/span {:class-name (str "inline-flex rounded-full px-2 py-0.5 text-xs font-medium "
                            (if (= status "active")
                              "bg-green-900/30 text-green-300"
                              "bg-slate-700 text-slate-300"))}
          status))

(defn- locale-pills [locales]
  (d/div {:class-name "flex flex-wrap gap-2"}
         (for [locale locales]
           (d/span {:key locale
                    :class-name "rounded border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-300"}
                   (str (logic/language-name locale) " · " locale)))))

(defnc placement-row [{:keys [placement]}]
  (let [{:keys [id locale path state url]} placement]
    (d/li {:class-name "flex flex-col gap-2 rounded border border-slate-800 bg-slate-950/40 p-3 sm:flex-row sm:items-center sm:justify-between"}
          (d/div
           (d/div {:class-name "flex flex-wrap items-center gap-2"}
                  (d/span {:class-name "font-mono text-xs text-cyan-300"} path)
                  (status-pill state)
                  (d/span {:class-name "text-xs text-slate-500"}
                          (logic/language-name locale)))
           (d/p {:class-name "mt-1 font-mono text-[11px] text-slate-600"} id))
          (d/a {:href url :target "_blank" :rel "noreferrer"
                :class-name "text-sm font-medium text-cyan-300 hover:text-cyan-200"}
               "Open published page ↗"))))

(defnc garden-card [{:keys [garden]}]
  (let [{:keys [id title status locales placements]} garden]
    ($ ui/card {:variant :elevated :padding :md}
       (d/div {:class-name "flex flex-wrap items-start justify-between gap-3"}
              (d/div
               (d/h2 {:class-name "text-lg font-semibold"} title)
               (d/p {:class-name "mt-1 font-mono text-xs text-slate-500"} id))
              (status-pill status))
       (d/div {:class-name "mt-4"}
              (d/p {:class-name "mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500"}
                   "Contract locale catalog")
              (locale-pills locales))
       (d/div {:class-name "mt-5"}
              (d/p {:class-name "mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500"}
                   "Publication placements")
              (if (seq placements)
                (d/ul {:class-name "space-y-2"}
                      (for [placement placements]
                        ($ placement-row {:key (:id placement) :placement placement})))
                (d/p {:class-name "text-sm text-slate-500"}
                     "No publication intents target this Garden."))))))

(defn- error-banner [error]
  (d/div {:class-name "rounded-lg border border-red-900/40 bg-red-950/30 p-4 text-sm text-red-300"}
         error))

(defnc gardens-body [{:keys [deployment loading error]}]
  (let [{:keys [site-url gardens]} deployment]
    (d/div {:class-name "space-y-4"}
           (d/div {:class-name "flex flex-wrap items-start justify-between gap-3"}
                  (d/div
                   (d/h1 {:class-name "text-2xl font-bold"} "Deployed Gardens")
                   (d/p {:class-name "mt-1 max-w-3xl text-sm text-slate-500"}
                        "Garden contracts own identity and languages. Publication contracts own placement. Content and Puck view contracts are reviewed independently."))
                  (d/div {:class-name "flex gap-2"}
                         (d/a {:href "/cms"
                               :class-name "rounded-lg border border-slate-700 px-3 py-2 text-sm font-medium text-slate-200 hover:border-slate-500"}
                              "Review content & layout")
                         (when (seq site-url)
                           (d/a {:href site-url :target "_blank" :rel "noreferrer"
                                 :class-name "rounded-lg bg-cyan-700 px-3 py-2 text-sm font-medium text-white hover:bg-cyan-600"}
                                "Open website ↗"))))
           (when loading
             ($ ui/card {:padding :md}
                (d/div {:class-name "text-sm text-slate-500"} "Loading deployed Garden contracts...")))
           (when error (error-banner error))
           (when (and (not loading) (empty? gardens) (nil? error))
             ($ ui/card {:padding :lg}
                (d/p {:class-name "text-center text-slate-500"}
                     "No deployed Garden contracts were found.")))
           (d/div {:class-name "grid gap-4"}
                  (for [garden gardens]
                    ($ garden-card {:key (:id garden) :garden garden}))))))

(defnc gardens-page []
  (let [[deployment set-deployment!] (hooks/use-state nil)
        [loading set-loading!] (hooks/use-state true)
        [error set-error!] (hooks/use-state nil)]
    (hooks/use-effect
     []
     (-> (api/load-deployment!)
         (.then set-deployment!)
         (.catch (fn [^js err]
                   (set-error! (or (.-message err) (str err)))))
         (.finally #(set-loading! false)))
     nil)
    ($ gardens-body {:deployment deployment :loading loading :error error})))
