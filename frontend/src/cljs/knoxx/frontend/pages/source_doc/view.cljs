(ns knoxx.frontend.pages.source-doc.view
  "Helix migration of SourceDocPage.tsx + ForumThreadView.tsx.

   Consumes the already-ported pure logic (forum-thread, document-links,
   app-routes). Renders uxx Markdown via the native uxx-helix component and
   integrates with react-router. Exported (via core.cljs require) as
   window.knoxx.frontend.pages.source_doc.view.source-doc-page for the thin TS
   loader shim. Not node-testable (uxx ESM) — verified via the app build/e2e;
   the parsing/preparation logic it relies on is unit-tested separately."
  (:require [clojure.string :as str]
            [helix.core :refer [$ defnc <>]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.lib.app-routes :refer [ops-routes]]
            [knoxx.frontend.lib.document-links :as links]
            [knoxx.frontend.pages.source-doc.forum-thread :as forum]
            ;; Markdown comes through the Vite frontend-bridge, not @open-hax/uxx
            ;; directly: the bridge bundles react-markdown's transitive deps that
            ;; shadow-cljs cannot resolve under pnpm's nested node_modules, and
            ;; React stays external so there is a single React instance. (The
            ;; native @open-hax/uxx-helix package ships compiled CLJS and cannot
            ;; be ESM-imported here without a double goog/cljs runtime collision —
            ;; native Helix uxx would need its source on the classpath.)
            ["@open-hax/knoxx-frontend-bridge" :refer [Markdown]]
            ["react-router-dom" :refer [useLocation useNavigate Link]]))

(def ^:private btn-class
  "rounded border border-slate-700 bg-slate-900 px-2 py-1 text-slate-200 hover:bg-slate-800")

(defn- fetch-document-content
  "CLJS port of lib/api/common.ts fetchDocumentContent: GET the per-segment
   encoded document path, resolving to {:content :path}."
  [relative-path]
  (let [encoded (->> (str/split (str relative-path) #"/")
                     (filter seq)
                     (map js/encodeURIComponent)
                     (str/join "/"))]
    (-> (js/fetch (str "/api/documents/content/" encoded))
        (.then (fn [res]
                 (if (.-ok res)
                   (.json res)
                   (throw (js/Error. (str "Failed to load document (" (.-status res) ")"))))))
        (.then (fn [data] (js->clj data :keywordize-keys true))))))

(defnc forum-pagination [{:keys [current-page total-pages set-current-page]}]
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

(defnc post-images [{:keys [post-key post-label image-urls show-images
                            failed-images image-retry-nonce
                            toggle build-src retry open-zoom set-failed]}]
  (d/div {:class "mt-2 space-y-1"}
    (d/div {:class "flex items-center justify-between"}
      (d/p {:class "text-xs font-medium uppercase tracking-wide text-slate-400"} "Images")
      (d/button {:type "button" :on-click #(toggle post-key)
                 :class "rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[11px] text-cyan-200 hover:bg-cyan-500/20"}
        (if show-images "Hide in post" "Load in post")))
    (for [[idx url] (map-indexed vector (take 12 image-urls))]
      (d/a {:key (str post-label "-img-" idx) :href url :target "_blank" :rel "noreferrer"
            :class "block truncate text-xs text-cyan-300 underline hover:text-cyan-200"}
        url))
    (when (> (count image-urls) 12)
      (d/p {:class "text-[11px] text-slate-500"} "+" (- (count image-urls) 12) " more images"))
    (when show-images
      (d/div {:class "grid gap-2 pt-1 sm:grid-cols-2"}
        (for [[idx url] (map-indexed vector (take 8 image-urls))]
          (let [image-key (str post-key ":" idx)
                failed? (boolean (get failed-images image-key))]
            (d/div {:key (str post-label "-preview-" idx)
                    :class "block overflow-hidden rounded border border-slate-700 bg-slate-950"}
              (if failed?
                (d/div {:class "flex h-56 flex-col items-center justify-center gap-2 px-3 text-center text-xs text-slate-300"}
                  (d/p {:class "text-rose-300"} "Image load timed out or failed.")
                  (d/div {:class "flex items-center gap-2"}
                    (d/button {:type "button"
                               :on-click (fn [e] (.preventDefault e) (retry image-key))
                               :class "rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-1 text-[11px] text-cyan-200 hover:bg-cyan-500/20"}
                      "Retry")
                    (d/a {:href url :target "_blank" :rel "noreferrer"
                          :class "text-[11px] text-cyan-300 underline hover:text-cyan-200"} "Open original")))
                (d/button {:type "button" :class "block w-full"
                           :on-click #(open-zoom (build-src url image-key))}
                  (d/img {:src (build-src url image-key)
                          :alt (str "Post " post-label " image " (inc idx))
                          :loading "lazy" :referrerPolicy "no-referrer"
                          :on-error #(set-failed (fn [m] (assoc m image-key true)))
                          :class "h-56 w-full object-contain bg-black"}))))))))))

(defnc forum-thread-view [{:keys [thread]}]
  (let [[current-page set-current-page] (hooks/use-state 1)
        [posts-per-page set-posts-per-page] (hooks/use-state 40)
        [only-with-images set-only-with-images] (hooks/use-state false)
        [expanded-images set-expanded-images] (hooks/use-state {})
        [failed-images set-failed-images] (hooks/use-state {})
        [image-retry-nonce set-image-retry-nonce] (hooks/use-state {})
        [zoom-gallery set-zoom-gallery] (hooks/use-state [])
        [zoom-index set-zoom-index] (hooks/use-state -1)
        [zoom-failed set-zoom-failed] (hooks/use-state false)
        prepared-posts (hooks/use-memo [thread] (forum/build-prepared-posts thread))
        visible-posts (hooks/use-memo [only-with-images prepared-posts]
                        (if only-with-images
                          (vec (filter #(pos? (count (:image-urls %))) prepared-posts))
                          prepared-posts))
        total-pages (max 1 (js/Math.ceil (/ (count visible-posts) (max 1 posts-per-page))))
        paged-posts (hooks/use-memo [current-page posts-per-page visible-posts]
                      (let [start (* (dec current-page) posts-per-page)]
                        (vec (take posts-per-page (drop start visible-posts)))))
        thread-image-urls (hooks/use-memo [prepared-posts]
                            (vec (distinct (mapcat :image-urls prepared-posts))))
        zoom-image-url (when (and (>= zoom-index 0) (< zoom-index (count zoom-gallery)))
                         (nth zoom-gallery zoom-index))
        toggle-post-images (fn [k] (set-expanded-images (fn [m] (assoc m k (not (get m k))))))
        build-image-src (fn [url k]
                          (let [nonce (get image-retry-nonce k 0)]
                            (if (zero? nonce)
                              url
                              (str url (if (str/includes? url "?") "&" "?") "retry=" nonce))))
        retry-image (fn [k]
                      (set-failed-images (fn [m] (assoc m k false)))
                      (set-image-retry-nonce (fn [m] (assoc m k (inc (get m k 0))))))
        close-zoom (fn [] (set-zoom-gallery []) (set-zoom-index -1) (set-zoom-failed false))
        step-zoom (fn [delta]
                    (when (pos? (count zoom-gallery))
                      (set-zoom-index (fn [prev] (if (neg? prev) 0 (mod (+ prev delta (count zoom-gallery)) (count zoom-gallery)))))
                      (set-zoom-failed false)))
        open-zoom-for-url (fn [url]
                            (let [gallery (if (pos? (count thread-image-urls)) thread-image-urls [url])
                                  idx (.indexOf (to-array gallery) url)]
                              (set-zoom-gallery gallery)
                              (set-zoom-index (if (>= idx 0) idx 0))
                              (set-zoom-failed false)))]
    (hooks/use-effect [(:threadId thread) (:threadUrl thread) (:posts thread)]
      (set-current-page 1) (set-posts-per-page 40) (set-only-with-images false)
      (set-expanded-images {}) (set-failed-images {}) (set-image-retry-nonce {})
      (set-zoom-gallery []) (set-zoom-index -1) (set-zoom-failed false))
    (hooks/use-effect [total-pages]
      (set-current-page (fn [prev] (min (max 1 prev) total-pages))))
    (hooks/use-effect [zoom-image-url (count zoom-gallery)]
      (when zoom-image-url
        (let [on-key (fn [e]
                       (case (.-key e)
                         "Escape" (close-zoom)
                         "ArrowRight" (step-zoom 1)
                         "ArrowLeft" (step-zoom -1)
                         nil))]
          (.addEventListener js/window "keydown" on-key)
          (fn [] (.removeEventListener js/window "keydown" on-key)))))
    (d/div {:class "space-y-4 text-slate-100"}
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
            "Open original thread")))
      (d/div {:class "space-y-3"}
        (d/div {:class "rounded-lg border border-slate-800 bg-slate-900/60 p-3"}
          (d/div {:class "flex flex-wrap items-center gap-3"}
            (d/label {:class "flex items-center gap-2 text-xs text-slate-300"}
              (d/input {:type "checkbox" :checked only-with-images
                        :on-change (fn [e]
                                     (set-only-with-images (.. e -target -checked))
                                     (set-current-page 1))})
              "Only posts with images")
            (d/label {:class "flex items-center gap-2 text-xs text-slate-300"}
              (d/span "Posts per page")
              (d/select {:value posts-per-page
                         :class "rounded border border-slate-700 bg-slate-900 px-2 py-1 text-xs"
                         :on-change (fn [e]
                                      (set-posts-per-page (js/Number (.. e -target -value)))
                                      (set-current-page 1))}
                (d/option {:value 20} "20")
                (d/option {:value 40} "40")
                (d/option {:value 80} "80")
                (d/option {:value 120} "120")))
            (d/span {:class "text-xs text-slate-400"} "Showing " (count visible-posts) " posts"))
          ($ forum-pagination {:current-page current-page :total-pages total-pages :set-current-page set-current-page}))
        (for [{:keys [post index body post-label post-key image-urls]} paged-posts]
          (let [show? (boolean (get expanded-images post-key))]
            (d/article {:key (str (or (:postId post) index))
                        :class "rounded-lg border border-slate-800 bg-slate-900/60 p-3"}
              (d/div {:class "mb-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs"}
                (d/span {:class "font-semibold text-cyan-200"} (or (:username post) "Unknown user"))
                (d/span {:class "text-slate-400"} (forum/format-post-date post))
                (d/span {:class "rounded bg-slate-800 px-1.5 py-0.5 text-[11px] text-slate-300"} "Post " post-label)
                (when (pos? (count image-urls))
                  (d/button {:type "button" :on-click #(toggle-post-images post-key)
                             :class "rounded border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[11px] text-cyan-200 hover:bg-cyan-500/20"}
                    (if show? "Hide in-post images" (str "Load images in post (" (count image-urls) ")")))))
              (d/pre {:class "whitespace-pre-wrap break-words text-sm leading-6 text-slate-100"} body)
              (when (pos? (count image-urls))
                ($ post-images {:post-key post-key :post-label post-label :image-urls image-urls
                                :show-images show? :failed-images failed-images :image-retry-nonce image-retry-nonce
                                :toggle toggle-post-images :build-src build-image-src :retry retry-image
                                :open-zoom open-zoom-for-url :set-failed set-failed-images})))))
        ($ forum-pagination {:current-page current-page :total-pages total-pages :set-current-page set-current-page}))
      (when zoom-image-url
        (d/div {:class "fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4" :on-click #(close-zoom)}
          (if zoom-failed
            (d/div {:class "rounded border border-slate-700 bg-slate-900 p-4 text-center text-slate-100"
                    :on-click #(.stopPropagation %)}
              (d/p {:class "text-rose-300"} "Failed to load zoomed image.")
              (d/a {:href zoom-image-url :target "_blank" :rel "noreferrer"
                    :class "mt-2 inline-block text-cyan-300 underline"} "Open original image"))
            (d/img {:src zoom-image-url :alt "Zoomed forum image" :referrerPolicy "no-referrer"
                    :class "max-h-[92vh] max-w-[92vw] object-contain"
                    :on-error #(set-zoom-failed true)
                    :on-click #(.stopPropagation %)}))
          (when (> (count zoom-gallery) 1)
            ;; ($ :<> ...) is NOT a fragment in helix 0.2.2 — it createElements a
            ;; literal "<>" tag, which throws InvalidCharacterError on client
            ;; render (server/static markup masks it). <> is the fragment macro.
            (<>
              (d/button {:type "button" :on-click (fn [e] (.stopPropagation e) (step-zoom -1))
                         :class "absolute left-4 top-1/2 -translate-y-1/2 rounded bg-slate-900/80 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"} "Prev")
              (d/button {:type "button" :on-click (fn [e] (.stopPropagation e) (step-zoom 1))
                         :class "absolute right-4 top-1/2 -translate-y-1/2 rounded bg-slate-900/80 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"} "Next")))
          (d/button {:type "button" :on-click #(close-zoom)
                     :class "absolute right-4 top-4 rounded bg-slate-900/80 px-3 py-1 text-sm text-slate-100 hover:bg-slate-800"} "Close")
          (when (> (count zoom-gallery) 1)
            (d/div {:class "absolute bottom-4 left-1/2 -translate-x-1/2 rounded bg-slate-900/80 px-3 py-1 text-xs text-slate-200"}
              (inc zoom-index) " / " (count zoom-gallery))))))))

(defnc source-doc-page []
  (let [location (useLocation)
        navigate (useNavigate)
        search (.-search location)
        query (hooks/use-memo [search] (js/URLSearchParams. search))
        raw-path (or (.get query "path") "")
        [loading set-loading] (hooks/use-state true)
        [error set-error] (hooks/use-state "")
        [content set-content] (hooks/use-state "")
        markdown? (boolean (re-find #"(?i)\.(md|mdx)$" raw-path))
        forum-thread (hooks/use-memo [raw-path content] (forum/parse-forum-thread raw-path content))
        handle-markdown-link
        (fn [href]
          (when (and href (not= "" href))
            (cond
              (str/starts-with? href "#")
              (some-> (.getElementById js/document (js/decodeURIComponent (subs href 1)))
                      (.scrollIntoView #js {:behavior "smooth" :block "start"}))
              (links/external-href? href)
              (.open js/window href "_blank" "noopener,noreferrer")
              :else
              (if-let [next-path (links/resolve-document-href raw-path href)]
                (navigate (str (:docs-view ops-routes) "?path=" (js/encodeURIComponent next-path)))
                (.open js/window href "_blank" "noopener,noreferrer")))))]
    (hooks/use-effect [raw-path]
      (let [relative (str/replace raw-path #"^/+" "")]
        (if (= "" relative)
          (do (set-error "Missing document path") (set-loading false))
          (do
            (set-loading true)
            (set-error "")
            (-> (fetch-document-content relative)
                (.then (fn [data] (set-content (or (:content data) ""))))
                (.catch (fn [err] (set-error (or (.-message err) "Failed to load document"))))
                (.finally (fn [] (set-loading false))))))))
    (d/div {:class "mx-auto w-full max-w-6xl p-6"}
      (d/div {:class "mb-4 flex items-center justify-between"}
        (d/div
          (d/h1 {:class "text-2xl font-semibold text-slate-100"} "Document Viewer")
          (d/p {:class "mt-1 font-mono text-xs text-slate-400"} (if (= "" raw-path) "N/A" raw-path)))
        ($ Link {:to "/" :class "rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200 hover:bg-slate-800"}
          "Back to Chat"))
      (d/section {:class "rounded-xl border border-slate-700 bg-slate-900/80 p-4 shadow-xl"}
        (when loading (d/p {:class "text-slate-300"} "Loading document..."))
        (when (not= "" error) (d/p {:class "text-rose-300"} error))
        (when (and (not loading) (= "" error))
          (d/div {:class "max-h-[78vh] overflow-auto rounded-lg border border-slate-800 bg-slate-950 p-4"}
            (cond
              forum-thread ($ forum-thread-view {:thread forum-thread})
              markdown? (d/article {:class "text-slate-100"}
                          ($ Markdown
                             {& #js {:content content :theme "dark" :variant "full"
                                     :linkTarget "_self" :onLinkClick handle-markdown-link}}))
              :else (d/pre {:class "whitespace-pre-wrap text-sm leading-6 text-slate-100"} content))))))))
