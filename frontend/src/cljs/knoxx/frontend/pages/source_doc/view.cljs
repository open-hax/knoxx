(ns knoxx.frontend.pages.source-doc.view
  "Document routing and Markdown composition around the native forum reader."
  (:require ["@open-hax/knoxx-frontend-bridge" :as bridge]
            ["react-router-dom" :as router]
            [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [knoxx.frontend.lib.app-routes :as routes]
            [knoxx.frontend.lib.document-links :as links]
            [knoxx.frontend.pages.source-doc.forum-thread :as forum]
            [knoxx.frontend.pages.source-doc.forum-view :as forum-view]))

(def forum-thread-view "Compatibility export for the native forum reader." forum-view/forum-thread-view)
(def forum-pagination "Compatibility export for forum page controls." forum-view/forum-pagination)

(defn- ^:async fetch-document-content [relative-path]
  (let [encoded (->> (str/split (str relative-path) #"/") (filter seq) (map js/encodeURIComponent) (str/join "/"))
        response (await (js/fetch (str "/api/documents/content/" encoded)))]
    (if (.-ok response)
      (js->clj (await (.json response)) :keywordize-keys true)
      (throw (js/Error. (str "Failed to load document (" (.-status response) ")"))))))

(defn- ^:async load-document! [relative active? set-state]
  (try
    (let [data (await (fetch-document-content relative))]
      (when @active? (set-state {:loading false :error "" :content (or (:content data) "")})))
    (catch :default error
      (when @active? (set-state {:loading false :error (or (.-message error) "Failed to load document") :content ""})))))

(defn- use-document [raw-path]
  (let [[state set-state] (hooks/use-state {:loading true :error "" :content ""})]
    (hooks/use-effect [raw-path]
      (let [relative (str/replace raw-path #"^/+" "") active? (atom true)]
        (if (empty? relative)
          (set-state {:loading false :error "Missing document path" :content ""})
          (do (set-state {:loading true :error "" :content ""}) (load-document! relative active? set-state)))
        #(reset! active? false)))
    state))

(defn- open-markdown-link [raw-path navigate href]
  (when (seq href)
    (cond
      (str/starts-with? href "#")
      (some-> (.getElementById js/document (js/decodeURIComponent (subs href 1)))
              (.scrollIntoView #js {:behavior "smooth" :block "start"}))
      (links/external-href? href) (.open js/window href "_blank" "noopener,noreferrer")
      :else (if-let [path (links/resolve-document-href raw-path href)]
              (navigate (str (:docs-view routes/ops-routes) "?path=" (js/encodeURIComponent path)))
              (.open js/window href "_blank" "noopener,noreferrer")))))

(hx/defnc document-content
  "Show forum, Markdown or plain document content after successful loading."
  [{:keys [raw-path content navigate]}]
  (let [thread (hooks/use-memo [raw-path content] (forum/parse-forum-thread raw-path content))]
    (d/div {:class "max-h-[78vh] overflow-auto rounded-lg border border-slate-800 bg-slate-950 p-4"}
      (cond
        thread (hx/$ forum-thread-view {:thread thread})
        (re-find #"(?i)\.(md|mdx)$" raw-path)
        (d/article {:class "text-slate-100"}
          (hx/$ bridge/Markdown {& #js {:content content :theme "dark" :variant "full"
                                        :linkTarget "_self" :onLinkClick #(open-markdown-link raw-path navigate %)}}))
        :else (d/pre {:class "whitespace-pre-wrap text-sm leading-6 text-slate-100"} content)))))

(hx/defnc source-doc-page
  "Load the selected document without allowing obsolete responses to replace it."
  []
  (let [location (router/useLocation) navigate (router/useNavigate) search (.-search location)
        query (hooks/use-memo [search] (js/URLSearchParams. search))
        raw-path (or (.get query "path") "")
        {:keys [loading error content]} (use-document raw-path)]
    (d/div {:class "mx-auto w-full max-w-6xl p-6"}
      (d/div {:class "mb-4 flex items-center justify-between"}
        (d/div (d/h1 {:class "text-2xl font-semibold text-slate-100"} "Document Viewer")
          (d/p {:class "mt-1 font-mono text-xs text-slate-400"} (if (empty? raw-path) "N/A" raw-path)))
        (hx/$ router/Link {:to "/" :class "rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200 hover:bg-slate-800"} "Back to Chat"))
      (d/section {:class "rounded-xl border border-slate-700 bg-slate-900/80 p-4 shadow-xl"}
        (when loading (d/p {:class "text-slate-300"} "Loading document..."))
        (when (seq error) (d/p {:class "text-rose-300"} error))
        (when (and (not loading) (empty? error))
          (hx/$ document-content {:raw-path raw-path :content content :navigate navigate}))))))
