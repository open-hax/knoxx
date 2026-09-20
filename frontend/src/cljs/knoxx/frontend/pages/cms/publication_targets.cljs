(ns knoxx.frontend.pages.cms.publication-targets
  "Explicit publication intent beside independently observed artifact evidence."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]))

(hx/defnc publication-targets
  "Expose one authorized, source-bound publish command for each real placement."
  [{:keys [publications snapshot can-publish? unsaved? busy? publish!]}]
  (d/section {:class-name "space-y-3 border-t border-white/10 pt-5" :aria-label "Publication targets"}
             (d/h2 {:class-name "font-semibold"} "Publication targets")
             (for [target publications]
               (d/div {:key (:id target) :data-publication-id (:id target)
                       :class-name "space-y-2 rounded-lg border border-white/10 p-3 text-xs"}
                      (d/p {:class-name "font-semibold"} (str (:garden target) " · " (:locale target)))
                      (d/p (str "Requested: " (:desired target)))
                      (d/p (if (:observed target) "Published artifact confirmed." "Publication has not been confirmed."))
                      (when (seq (:blockers target))
                        (d/ul {:class-name "list-inside list-disc text-amber-200"}
                              (for [blocker (:blockers target)]
                                (d/li {:key blocker} (str/replace blocker #"[-_]" " ")))))
                      (d/button {:class-name "btn btn-primary" :type "button" :on-click #(publish! target)
                                 :disabled (boolean (or busy? unsaved? (not can-publish?) (not (:accepted snapshot))))}
                                "Publish")))
             (when (empty? publications)
               (d/p {:class-name "text-xs opacity-60"} "No publication placements are available for this page."))))
