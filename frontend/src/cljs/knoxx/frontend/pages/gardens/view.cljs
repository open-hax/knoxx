(ns knoxx.frontend.pages.gardens.view
  "Review of deploy-owned Garden and publication contracts, and the one action
   a reviewer can take on them: demanding that a placement be reconciled.

   Publishing lives here rather than on the translation page because the
   translation page is built from translation receipts. A source-locale intent
   has none — it needs no translation — so it never appears there, and until
   this action existed it had no route to publication through any UI at all.
   Its bytes were deployed, valid, and unreachable."
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

(defnc placement-row [{:keys [placement publishing on-publish]}]
  (let [{:keys [id locale path state url]} placement
        in-flight? (= publishing id)]
    (d/li {:class-name "flex flex-col gap-2 rounded border border-slate-800 bg-slate-950/40 p-3 sm:flex-row sm:items-center sm:justify-between"}
          (d/div
           (d/div {:class-name "flex flex-wrap items-center gap-2"}
                  (d/span {:class-name "font-mono text-xs text-cyan-300"} path)
                  (status-pill state)
                  (d/span {:class-name "text-xs text-slate-500"}
                          (logic/language-name locale)))
           (d/p {:class-name "mt-1 font-mono text-[11px] text-slate-600"} id))
          (d/div {:class-name "flex items-center gap-3"}
                 ;; Offered for any placement whose contract asks to be published.
                 ;; Deliberately NOT hidden once bytes exist: `:state` is desired
                 ;; state, not a materialization fact, so this UI cannot tell
                 ;; whether a placement is actually live. Reconciling an
                 ;; already-published revision answers `publication/noop` and says
                 ;; so, which beats a button that guesses and hides itself.
                 (when (logic/placement-published? placement)
                   ($ ui/button {:size :sm
                                 :variant :secondary
                                 :disabled (some? publishing)
                                 :on-click #(on-publish id)}
                      (if in-flight? "Publishing..." "Publish")))
                 (d/a {:href url :target "_blank" :rel "noreferrer"
                       :class-name "text-sm font-medium text-cyan-300 hover:text-cyan-200"}
                      "Open published page ↗")))))

(defnc garden-card [{:keys [garden publishing on-publish on-publish-all]}]
  (let [{:keys [id title status locales placements]} garden]
    ($ ui/card {:variant :elevated :padding :md}
       (d/div {:class-name "flex flex-wrap items-start justify-between gap-3"}
              (d/div
               (d/h2 {:class-name "text-lg font-semibold"} title)
               (d/p {:class-name "mt-1 font-mono text-xs text-slate-500"} id))
              (d/div {:class-name "flex items-center gap-3"}
                     ;; Sequential, one reconciliation at a time — the same
                     ;; discipline `infra.translation-dispatch/dispatch-intents!`
                     ;; keeps, and for the same reason: each publish writes an
                     ;; artifact and renames a shared manifest, and fanning a
                     ;; garden out in one pass is how a reconciliation run
                     ;; becomes an incident.
                     (when (seq (logic/publishable-placements garden))
                       ($ ui/button {:size :sm
                                     :variant :primary
                                     :disabled (some? publishing)
                                     :on-click #(on-publish-all garden)}
                          (str "Publish all ("
                               (count (logic/publishable-placements garden))
                               ")")))
                     (status-pill status)))
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
                        ($ placement-row {:key (:id placement)
                                          :placement placement
                                          :publishing publishing
                                          :on-publish on-publish})))
                (d/p {:class-name "text-sm text-slate-500"}
                     "No publication intents target this Garden."))))))

(defn- error-banner [error]
  (d/div {:class-name "rounded-lg border border-red-900/40 bg-red-950/30 p-4 text-sm text-red-300"}
         error))

(def ^:private tone-classes
  {:success "border-emerald-900/40 bg-emerald-950/30 text-emerald-300"
   :warning "border-amber-900/40 bg-amber-950/30 text-amber-200"
   :error   "border-red-900/40 bg-red-950/30 text-red-300"})

(defn- notice-banner
  "A reconciliation outcome, coloured by what it actually says. Success styling
   is reserved for receipts that record one; everything else, including an
   unrecognized type, is at least a warning."
  [notice tone]
  (d/div {:class-name (str "rounded-lg border p-4 text-sm "
                           (get tone-classes tone (:warning tone-classes)))}
         notice))

(defnc gardens-body [{:keys [deployment loading error notice notice-tone
                             publishing on-publish on-publish-all]}]
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
           (when notice (notice-banner notice notice-tone))
           (when (and (not loading) (empty? gardens) (nil? error))
             ($ ui/card {:padding :lg}
                (d/p {:class-name "text-center text-slate-500"}
                     "No deployed Garden contracts were found.")))
           (d/div {:class-name "grid gap-4"}
                  (for [garden gardens]
                    ($ garden-card {:key (:id garden)
                                    :garden garden
                                    :publishing publishing
                                    :on-publish on-publish
                                    :on-publish-all on-publish-all}))))))

(defnc gardens-page []
  (let [[deployment set-deployment!] (hooks/use-state nil)
        [loading set-loading!] (hooks/use-state true)
        [error set-error!] (hooks/use-state nil)
        [notice set-notice!] (hooks/use-state nil)
        [notice-tone set-notice-tone!] (hooks/use-state :success)
        ;; The publication id in flight, or nil. One at a time: reconciliation
        ;; writes an artifact and renames a shared manifest, and two concurrent
        ;; demands from one reviewer is a race this UI has no reason to create.
        [publishing set-publishing!] (hooks/use-state nil)
        load! (fn [] (-> (api/load-deployment!)
                         (.then set-deployment!)
                         (.catch (fn [^js err]
                                   (set-error! (or (.-message err) (str err)))))
                         (.finally #(set-loading! false))))
        on-publish
        (fn [publication-id]
          (set-publishing! publication-id)
          (set-error! nil)
          (set-notice! nil)
          (-> (api/reconcile-publication! publication-id)
              (.then (fn [receipt]
                       (set-notice-tone! (logic/receipt-tone receipt))
                       (set-notice! (str publication-id " \u2014 "
                                         (logic/receipt-summary receipt)))
                       ;; Re-read: a publish can change what the projection
                       ;; reports, and a stale card is how a reviewer comes to
                       ;; believe a second click is needed.
                       (load!)))
              (.catch (fn [^js err]
                        (set-error! (str publication-id " \u2014 "
                                         (or (.-message err) (str err))))))
              (.finally #(set-publishing! nil))))

        on-publish-all
        (fn [garden]
          (let [placements (logic/publishable-placements garden)]
            (set-error! nil)
            (set-notice! nil)
            ;; Strictly sequential: each step waits for the previous receipt.
            ;; `reduce` over a promise chain rather than `Promise.all`, because
            ;; every publish renames the same manifest and concurrent renames
            ;; are how one of them is lost.
            (-> (reduce (fn [chain placement]
                          (.then chain
                                 (fn [acc]
                                   (set-publishing! (:id placement))
                                   (-> (api/reconcile-publication! (:id placement))
                                       (.then #(conj acc %))
                                       ;; One refusal must not abandon the rest
                                       ;; of the garden; it is recorded as a
                                       ;; failed receipt and the run continues.
                                       (.catch (fn [^js err]
                                                 (conj acc {:type "publication/failed"
                                                            :reason (or (.-message err)
                                                                        (str err))})))))))
                        (js/Promise.resolve [])
                        placements)
                (.then (fn [receipts]
                         (set-notice-tone! (logic/run-tone receipts))
                         (set-notice! (str (:title garden) " — "
                                           (logic/run-summary receipts)))
                         (load!)))
                (.catch (fn [^js err]
                          (set-error! (or (.-message err) (str err)))))
                (.finally #(set-publishing! nil)))))]
    (hooks/use-effect [] (load!) nil)
    ($ gardens-body {:deployment deployment :loading loading :error error
                     :notice notice :notice-tone notice-tone
                     :publishing publishing :on-publish on-publish
                     :on-publish-all on-publish-all})))
