(ns knoxx.frontend.pages.source-doc.forum-view-interaction-test
  "Exercise extracted post pagination, opt-in image retry and keyboard gallery controls."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.pages.source-doc.forum-view :as view]))

(t/use-fixtures :each {:after #(rtl/cleanup)})
(def ^:private images ["https://images.test/first.png" "https://images.test/second.png"])
(defn- thread [count-posts]
  {:threadId "thread-1" :threadTitle "Research thread"
   :posts (mapv (fn [index] (cond-> {:postId (str index) :username "Researcher" :content (str "Post content " index)}
                              (= index 1) (assoc :images images))) (range 1 (inc count-posts)))})

(t/deftest pagination-filter-and-page-size-preserve-visible-posts
  (let [^js rendered (rtl/render (hx/$ view/forum-thread-view {:thread (thread 45)}))]
    (t/is (= 40 (.-length (.querySelectorAll (.-container rendered) "article"))))
    (.click rtl/fireEvent (aget (.getAllByRole rendered "button" #js {:name "Next" :exact true}) 0))
    (t/is (some? (.getByText rendered "Post content 41")))
    (.change rtl/fireEvent (.getByLabelText rendered "Posts per page") #js {:target #js {:value "20"}})
    (t/is (= 20 (.-length (.querySelectorAll (.-container rendered) "article"))))
    (.click rtl/fireEvent (.getByLabelText rendered "Only posts with images"))
    (t/is (= 1 (.-length (.querySelectorAll (.-container rendered) "article"))))
    (t/is (some? (.getByText rendered "Post content 1")))))

(t/deftest image-retry-and-gallery-navigation-survive-view-extraction
  (let [^js rendered (rtl/render (hx/$ view/forum-thread-view {:thread (thread 1)}))]
    (t/is (zero? (.-length (.queryAllByRole rendered "img"))))
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Load images in post (2)"}))
    (let [image (.getByAltText rendered "Post 1 image 1")]
      (.error rtl/fireEvent image)
      (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Retry"}))
      (t/is (= "https://images.test/first.png?retry=1" (.getAttribute (.getByAltText rendered "Post 1 image 1") "src"))))
    (.click rtl/fireEvent (.getByAltText rendered "Post 1 image 2"))
    (t/is (= (second images) (.getAttribute (.getByAltText rendered "Zoomed forum image") "src")))
    (.keyDown rtl/fireEvent js/window #js {:key "ArrowRight"})
    (t/is (= (first images) (.getAttribute (.getByAltText rendered "Zoomed forum image") "src")))
    (.keyDown rtl/fireEvent js/window #js {:key "Escape"})
    (t/is (nil? (.queryByAltText rendered "Zoomed forum image")))))
