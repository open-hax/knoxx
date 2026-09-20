(ns knoxx.frontend.pages.cms.page-interaction-test
  "Human command controls retain drafts under actual projection refresh behavior."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.lib.api :as http]
            [knoxx.frontend.pages.cms.logic-test :as fixture]
            [knoxx.frontend.pages.cms.view :as view]))

(defn- change! [element text]
  (.change rtl/fireEvent element #js {:target #js {:value text}}))

(defn- inventory [commands]
  {:documents [{:document {:document/id "wiki/example" :document/title "Shared knowledge"}}]
   :gardens [{:garden/id "garden/local" :garden/title "Local garden"}]
   :capabilities ["publication/read" "publication/write" "publication/review" "publication/assist"]
   :commands commands})

(defn- install-boundary! [snapshot calls commands]
  (set! http/request
        (fn request
          ([path] (request path nil))
          ([path options]
           (swap! calls conj [path options])
           (js/Promise.resolve
            (cond
              (= path "/api/publications/documents") (inventory commands)
              (.endsWith path "/review") (merge @snapshot (select-keys (inventory commands) [:capabilities :commands]))
              (.endsWith path "/publications") {:document "wiki/example" :publications []}
              :else (throw (js/Error. (str "Unexpected test request " path)))))))))

(defn- ^:async open-page [^js rendered]
  (.click rtl/fireEvent (await (.findByRole rendered "button" #js {:name "Shared knowledge" :exact true})))
  (await (.findByRole rendered "tab" #js {:name "Edit" :exact true})))

(t/deftest ^:async denied-save-keeps-create-available
  (let [original http/request calls (atom []) snapshot (atom fixture/snapshot)
        commands ["wiki_list" "wiki_read" "wiki_create" "wiki_review"]]
    (install-boundary! snapshot calls commands)
    (try
      (let [^js rendered (rtl/render (hx/$ view/cms-page))]
        (await (open-page rendered))
        (.click rtl/fireEvent (.getByRole rendered "tab" #js {:name "Edit" :exact true}))
        (t/is (.-readOnly (.getByLabelText rendered "Source content" #js {:exact true})))
        (t/is (.-disabled (.getByRole rendered "button" #js {:name "Save revision" :exact true})))
        (t/is (not (.-disabled (.getByRole rendered "button" #js {:name "New page" :exact true})))))
      (finally (rtl/cleanup) (set! http/request original)))))

(t/deftest ^:async refresh-preserves-dirty-source-and-blocks-its-stale-save
  (let [original http/request calls (atom []) snapshot (atom fixture/snapshot)
        commands ["wiki_list" "wiki_read" "wiki_create" "wiki_save" "wiki_review"]]
    (install-boundary! snapshot calls commands)
    (try
      (let [^js rendered (rtl/render (hx/$ view/cms-page))]
        (await (open-page rendered))
        (.click rtl/fireEvent (.getByRole rendered "tab" #js {:name "Edit" :exact true}))
        (change! (.getByLabelText rendered "Source content" #js {:exact true}) "Unfinished human draft")
        (swap! snapshot assoc :content "Concurrent agent source" :revision "sha256-source-2")
        (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Refresh pages" :exact true}))
        (await (.findByRole rendered "button" #js {:name "Discard draft and load latest" :exact true}))
        (t/is (= "Unfinished human draft" (.-value (.getByLabelText rendered "Source content" #js {:exact true}))))
        (t/is (.-disabled (.getByRole rendered "button" #js {:name "Save revision" :exact true})))
        (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Discard draft and load latest" :exact true}))
        (t/is (= "Concurrent agent source" (.-value (.getByLabelText rendered "Source content" #js {:exact true})))))
      (finally (rtl/cleanup) (set! http/request original)))))
