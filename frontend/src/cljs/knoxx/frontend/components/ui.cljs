(ns knoxx.frontend.components.ui
  "Shared hand-rolled Helix UI primitives (button/card/input).
   Stand-ins for `@open-hax/uxx`'s Button/Card/Input in migrated pages —
   to be replaced by native uxx-helix once it is consumable from source
   (see kanban: knoxx-frontend-uxx-helix-native)."
  (:require [helix.core :refer [defnc]]
            [helix.dom :as d]))

(defn- button-classes [variant size]
  (str "rounded-lg font-medium transition disabled:opacity-60 "
       (case size
         :sm "px-3 py-1.5 text-xs "
         "px-4 py-2 text-sm ")
       (case variant
         :primary "bg-cyan-600 text-white hover:bg-cyan-500"
         :ghost "border border-slate-700 text-slate-300 hover:bg-slate-800"
         "border border-slate-600 bg-slate-800 text-slate-100 hover:bg-slate-700")))

(defnc button
  "Button with :variant (:primary :secondary :ghost), :size (:sm :md),
   :loading (disables + aria-busy, label stays visible), :disabled,
   :on-click, :type."
  [{:keys [variant size loading disabled on-click type children]}]
  (d/button {:type (or type "button")
             :disabled (boolean (or loading disabled))
             :aria-busy (boolean loading)
             :on-click on-click
             :class-name (button-classes variant size)}
            children))

(defnc badge
  "Small status pill with :variant (:default :success :warning :error :info)."
  [{:keys [variant children]}]
  (d/span {:class-name (str "inline-flex items-center rounded-full border px-1.5 py-0.5 text-[10px] font-medium "
                            (case variant
                              :success "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                              :warning "border-amber-500/30 bg-amber-500/10 text-amber-200"
                              :error "border-rose-500/30 bg-rose-500/10 text-rose-200"
                              :info "border-sky-500/30 bg-sky-500/10 text-sky-200"
                              "border-slate-700 bg-slate-800 text-slate-200"))}
          children))

(defnc card
  "Container with :variant (:default :elevated) and :padding (:sm :md :lg)."
  [{:keys [variant padding class-name children]}]
  (d/div {:class-name (str "rounded-xl border border-slate-800 bg-slate-950/60 "
                           (case padding
                             :sm "p-2 "
                             :lg "p-6 "
                             "p-4 ")
                           (when (= variant :elevated) "shadow-lg shadow-black/20 ")
                           (or class-name ""))}
         children))

(defnc input
  "Text input passing through :value :on-change :placeholder :disabled :type."
  [{:keys [value on-change placeholder disabled type]}]
  (d/input {:type (or type "text")
            :value value
            :on-change on-change
            :placeholder placeholder
            :disabled (boolean disabled)
            :class-name (str "w-full rounded-lg border border-slate-700 bg-slate-900 "
                             "px-3 py-2 text-sm text-slate-100 outline-none "
                             "focus:border-cyan-500 disabled:opacity-60")}))
