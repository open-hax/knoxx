(ns knoxx.frontend.pages.source-doc.forum-view
  "Native forum reading UI, independent of the Markdown bridge."
  (:require [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.pages.source-doc.forum-state :as state]
            [knoxx.frontend.pages.source-doc.forum-thread :as forum]))

(def ^:private btn-class
  "rounded border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 hover:bg-slate-800")

(hx/defnc forum-pagination
  "Navigate the visible post pages." [{:keys [current-page total-pages set-current-page]}]
  (d/div {:class "mt-3 flex flex-wrap items-center gap-2 text-xs"}
    (d/button {:type "button" :class btn-class :on-click #(set-current-page 1)} "First")
    (d/button {:type "button" :class btn-class :on-click #(set-current-page (fn [p] (max 1 (- p 10))))} "-10")
    (d/button {:type "button" :class btn-class :on-click #(set-current-page (fn [p] (max 1 (dec p))))} "Prev")
    (d/span {:class "px-2 text-slate-300"} "Page " current-page " / " total-pages)
    (d/button {:type "button" :class btn-class :on-click #(set-current-page (fn [p] (min total-pages (inc p))))} "Next")
    (d/button {:type "button" :class btn-class :on-click #(set-current-page (fn [p] (min total-pages (+ p 10))))} "+10")
    (d/button {:type "button" :class btn-class :on-click #(set-current-page total-pages)} "Last")
    (d/label {:class "ml-2 flex items-center gap-2 text-slate-300"}
      (d/span "Jump")
      (d/input {:type "number" :min 1 :max total-pages :value current-page
                :class "w-20 rounded border border-slate-700 bg-slate-900 px-2 py-1 text-xs"
                :on-change (fn [e]
                             (let [n (js/Number (or (.. e -target -value) 1))]
                               (when (js/Number.isFinite n)
                                 (set-current-page (min total-pages (max 1 n))))))}))))

(hx/defnc failed-image
  "Offer a retry and the original URL after a preview failure."
  [{:keys [url image-key retry]}]
  (d/div {:class "flex h-56 flex-col items-center justify-center gap-2 px-3 text-center text-xs text-slate-300"}
    (d/p {:class "text-rose-300"} "Image load timed out or failed.")
    (d/div {:class "flex items-center gap-2"}
      (d/button {:type "button" :on-click #(do (.preventDefault %) (retry image-key))
                 :class "rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-1 text-[11px] text-cyan-200 hover:bg-cyan-500/20"} "Retry")
      (d/a {:href url :target "_blank" :rel "noreferrer"
            :class "text-[11px] text-cyan-300 underline hover:text-cyan-200"} "Open original"))))

(hx/defnc post-image
  "Load one explicitly requested image with its retry identity."
  [{:keys [url image-index post-key post-label failed-images build-src retry open-zoom set-failed]}]
  (let [image-key (str post-key ":" image-index)]
    (d/div {:class "block overflow-hidden rounded border border-slate-700 bg-slate-950"}
      (if (get failed-images image-key)
        (hx/$ failed-image {:url url :image-key image-key :retry retry})
        (d/button {:type "button" :class "block w-full" :on-click #(open-zoom url)}
          (d/img {:src (build-src url image-key) :alt (str "Post " post-label " image " (inc image-index))
                  :loading "lazy" :referrerPolicy "no-referrer"
                  :on-error #(set-failed (fn [failures] (assoc failures image-key true)))
                  :class "h-56 w-full object-contain bg-black"}))))))

(hx/defnc post-images
  "Display bounded links and explicitly requested image previews."
  [{:keys [post-key post-label image-urls show-images failed-images toggle build-src retry open-zoom set-failed]}]
  (d/div {:class "mt-2 space-y-1"}
    (d/div {:class "flex items-center justify-between"}
      (d/p {:class "text-xs font-medium uppercase tracking-wide text-slate-400"} "Images")
      (d/button {:type "button" :on-click #(toggle post-key)
                 :class "rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[11px] text-cyan-200 hover:bg-cyan-500/20"}
        (if show-images "Hide in post" "Load in post")))
    (for [[index url] (map-indexed vector (take 12 image-urls))]
      (d/a {:key (str post-label "-img-" index) :href url :target "_blank" :rel "noreferrer"
            :class "block truncate text-xs text-cyan-300 underline hover:text-cyan-200"} url))
    (when (> (count image-urls) 12)
      (d/p {:class "text-[11px] text-slate-500"} "+" (- (count image-urls) 12) " more images"))
    (when show-images
      (d/div {:class "grid gap-2 pt-1 sm:grid-cols-2"}
        (for [[index url] (map-indexed vector (take 8 image-urls))]
          (hx/$ post-image {:key (str post-label "-preview-" index) :url url :image-index index :post-key post-key :post-label post-label
                            :failed-images failed-images :build-src build-src :retry retry :open-zoom open-zoom :set-failed set-failed}))))))

(hx/defnc forum-header
  "Show the source identity and original thread link."
  [{:keys [thread]}]
  (d/header {:class "rounded-lg border border-slate-700 bg-slate-900/70 p-4"}
    (d/h2 {:class "text-xl font-semibold text-cyan-200"}
      (or (:threadTitle thread) (str "Thread " (or (:threadId thread) ""))))
    (d/div {:class "mt-2 grid grid-cols-2 gap-2 text-xs text-slate-300 md:grid-cols-4"}
      (d/div "Thread ID: " (or (:threadId thread) "N/A"))
      (d/div "Category: " (or (:forumCategory thread) "N/A"))
      (d/div "Total posts: " (or (get-in thread [:stats :totalPosts]) (count (:posts thread)) 0))
      (d/div "Participants: " (or (get-in thread [:stats :uniqueUsers]) (count (:participants thread)) 0)))
    (when (:threadUrl thread)
      (d/a {:href (:threadUrl thread) :target "_blank" :rel "noreferrer"
            :class "mt-2 inline-block text-xs text-cyan-300 underline hover:text-cyan-200"}
        "Open original thread"))))

(hx/defnc forum-filters
  "Select image-bearing posts and the page size."
  [{:keys [pages]}]
  (let [{:keys [only-with-images set-only-with-images set-current-page posts-per-page set-posts-per-page visible-count]} pages]
    (d/div {:class "rounded-lg border border-slate-800 bg-slate-900/60 p-3"}
      (d/div {:class "flex flex-wrap items-center gap-3"}
        (d/label {:class "flex items-center gap-2 text-xs text-slate-300"}
          (d/input {:type "checkbox" :checked only-with-images
                    :on-change #(do (set-only-with-images (.. % -target -checked)) (set-current-page 1))})
          "Only posts with images")
        (d/label {:class "flex items-center gap-2 text-xs text-slate-300"}
          (d/span "Posts per page")
          (d/select {:aria-label "Posts per page" :value posts-per-page
                     :class "rounded border border-slate-700 bg-slate-900 px-2 py-1 text-xs"
                     :on-change #(do (set-posts-per-page (js/Number (.. % -target -value))) (set-current-page 1))}
            (for [size [20 40 80 120]] (d/option {:key size :value size} (str size)))))
        (d/span {:class "text-xs text-slate-400"} "Showing " visible-count " posts"))
      (hx/$ forum-pagination {& pages}))))

(hx/defnc post-header
  "Identify a post and offer opt-in preview loading."
  [{:keys [post post-label image-urls show? toggle post-key]}]
  (d/div {:class "mb-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs"}
    (d/span {:class "font-semibold text-cyan-200"} (or (:username post) "Unknown user"))
    (d/span {:class "text-slate-400"} (forum/format-post-date post))
    (d/span {:class "rounded bg-slate-800 px-1.5 py-0.5 text-[11px] text-slate-300"} "Post " post-label)
    (when (seq image-urls)
      (d/button {:type "button" :on-click #(toggle post-key)
                 :class "rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[11px] text-cyan-200 hover:bg-cyan-500/20"}
        (if show? "Hide in-post images" (str "Load images in post (" (count image-urls) ")"))))))

(hx/defnc forum-post
  "Render the full post text and its image controls."
  [{:keys [prepared images]}]
  (let [{:keys [post-key body image-urls]} prepared
        show? (boolean (get (:expanded-images images) post-key))]
    (d/article {:class "rounded-lg border border-slate-800 bg-slate-900/60 p-3"}
      (hx/$ post-header {& (assoc prepared :show? show? :toggle (:toggle images))})
      (d/pre {:class "whitespace-pre-wrap break-words text-sm leading-6 text-slate-100"} body)
      (when (seq image-urls)
        (hx/$ post-images {& (merge prepared images {:show-images show?})})))))

(hx/defnc zoom-content
  "Display a selected image or an actionable loading failure."
  [{:keys [zoom-image-url zoom-failed set-zoom-failed]}]
  (if zoom-failed
    (d/div {:class "rounded border border-slate-700 bg-slate-900 p-4 text-center text-slate-100"
            :on-click #(.stopPropagation %)}
      (d/p {:class "text-rose-300"} "Failed to load zoomed image.")
      (d/a {:href zoom-image-url :target "_blank" :rel "noreferrer"
            :class "mt-2 inline-block text-cyan-300 underline"} "Open original image"))
    (d/img {:src zoom-image-url :alt "Zoomed forum image" :referrerPolicy "no-referrer"
            :class "max-h-[92vh] max-w-[92vw] object-contain"
            :on-error #(set-zoom-failed true) :on-click #(.stopPropagation %)})))

(hx/defnc forum-gallery
  "Navigate an explicitly opened image gallery."
  [{:keys [images]}]
  (let [{:keys [zoom-image-url zoom-gallery zoom-index step-zoom close-zoom]} images]
    (when zoom-image-url
      (d/div {:class "fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4" :on-click #(close-zoom)}
        (hx/$ zoom-content {& images})
        (when (> (count zoom-gallery) 1)
          (hx/<>
            (d/button {:type "button" :on-click #(do (.stopPropagation %) (step-zoom -1))
                       :class "absolute left-4 top-1/2 -translate-y-1/2 rounded bg-slate-900/80 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"} "Prev")
            (d/button {:type "button" :on-click #(do (.stopPropagation %) (step-zoom 1))
                       :class "absolute right-4 top-1/2 -translate-y-1/2 rounded bg-slate-900/80 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"} "Next")))
        (d/button {:type "button" :on-click #(close-zoom)
                   :class "absolute right-4 top-4 rounded bg-slate-900/80 px-3 py-1 text-sm text-slate-100 hover:bg-slate-800"} "Close")
        (when (> (count zoom-gallery) 1)
          (d/div {:class "absolute bottom-4 left-1/2 -translate-x-1/2 rounded bg-slate-900/80 px-3 py-1 text-xs text-slate-200"}
            (inc zoom-index) " / " (count zoom-gallery)))))))

(hx/defnc forum-thread-view
  "Read a paginated forum thread with optional image previews."
  [{:keys [thread]}]
  (let [pages (state/use-pages thread) images (state/use-images thread (:thread-image-urls pages))]
    (d/div {:class "space-y-4 text-slate-100"}
      (hx/$ forum-header {:thread thread})
      (d/div {:class "space-y-3"}
        (hx/$ forum-filters {:pages pages})
        (for [{:keys [post index] :as prepared} (:paged-posts pages)]
          (hx/$ forum-post {:key (str (or (:postId post) index)) :prepared prepared :images images}))
        (hx/$ forum-pagination {& pages}))
      (hx/$ forum-gallery {:images images}))))
