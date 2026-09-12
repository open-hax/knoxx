(ns knoxx.frontend.pages.source-doc.forum-state
  "Pagination and opt-in image state for a single forum document."
  (:require [clojure.string :as str]
            [helix.hooks :as hooks]
            [knoxx.frontend.pages.source-doc.forum-thread :as forum]))

(defn- visible-posts [prepared only-with-images]
  (if only-with-images (filterv #(seq (:image-urls %)) prepared) prepared))

(defn use-pages
  "Prepare and paginate posts, resetting controls when the document changes."
  [thread]
  (let [[page set-page] (hooks/use-state 1)
        [size set-size] (hooks/use-state 40)
        [images? set-images] (hooks/use-state false)
        prepared (hooks/use-memo [thread] (forum/build-prepared-posts thread))
        visible (hooks/use-memo [prepared images?] (visible-posts prepared images?))
        total (max 1 (js/Math.ceil (/ (count visible) (max 1 size))))
        paged (hooks/use-memo [page size visible] (vec (take size (drop (* (dec page) size) visible))))
        urls (hooks/use-memo [prepared] (vec (distinct (mapcat :image-urls prepared))))]
    (hooks/use-effect [(:threadId thread) (:threadUrl thread) (:posts thread)]
      (set-page 1) (set-size 40) (set-images false))
    (hooks/use-effect [total] (set-page #(min (max 1 %) total)))
    {:current-page page :set-current-page set-page :posts-per-page size
     :set-posts-per-page set-size :only-with-images images? :set-only-with-images set-images
     :total-pages total :visible-count (count visible) :paged-posts paged :thread-image-urls urls}))

(def ^:private initial-images
  {:expanded-images {} :failed-images {} :image-retry-nonce {}
   :zoom-gallery [] :zoom-index -1 :zoom-failed false})

(defn- image-src [nonces url image-key]
  (let [nonce (get nonces image-key 0)]
    (if (zero? nonce) url (str url (if (str/includes? url "?") "&" "?") "retry=" nonce))))

(defn- open-gallery [set-state urls url]
  (let [gallery (if (seq urls) urls [url])
        index (first (keep-indexed #(when (= %2 url) %1) gallery))]
    (set-state #(assoc % :zoom-gallery gallery :zoom-index (or index 0) :zoom-failed false))))

(defn- step-gallery [set-state delta]
  (set-state (fn [{:keys [zoom-gallery zoom-index] :as state}]
               (if (seq zoom-gallery)
                 (assoc state :zoom-index (if (neg? zoom-index) 0 (mod (+ zoom-index delta) (count zoom-gallery)))
                        :zoom-failed false)
                 state))))

(defn- image-actions [state set-state urls]
  {:toggle #(set-state (fn [current] (update-in current [:expanded-images %] not)))
   :build-src #(image-src (:image-retry-nonce state) %1 %2)
   :retry #(set-state (fn [current] (-> current (assoc-in [:failed-images %] false)
                                       (update-in [:image-retry-nonce %] (fnil inc 0)))))
   :set-failed #(set-state (fn [current] (update current :failed-images %)))
   :open-zoom #(open-gallery set-state urls %)
   :close-zoom #(set-state (fn [current] (assoc current :zoom-gallery [] :zoom-index -1 :zoom-failed false)))
   :step-zoom #(step-gallery set-state %)
   :set-zoom-failed #(set-state (fn [current] (assoc current :zoom-failed %)))})

(defn use-images
  "Own image retries and gallery keyboard listeners without eagerly loading previews."
  [thread urls]
  (let [[state set-state] (hooks/use-state initial-images)
        {:keys [zoom-gallery zoom-index]} state
        image-url (when (<= 0 zoom-index (dec (count zoom-gallery))) (nth zoom-gallery zoom-index))
        actions (image-actions state set-state urls)]
    (hooks/use-effect [(:threadId thread) (:threadUrl thread) (:posts thread)] (set-state initial-images))
    (hooks/use-effect [image-url (count zoom-gallery)]
      (when image-url
        (let [on-key (fn [event]
                       (case (.-key event)
                         "Escape" ((:close-zoom actions))
                         "ArrowRight" ((:step-zoom actions) 1)
                         "ArrowLeft" ((:step-zoom actions) -1)
                         nil))]
          (.addEventListener js/window "keydown" on-key)
          (fn [] (.removeEventListener js/window "keydown" on-key)))))
    (assoc (merge state actions) :zoom-image-url image-url)))
