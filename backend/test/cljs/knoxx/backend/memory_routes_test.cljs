(ns knoxx.backend.memory-routes-test
  (:require [cljs.test :refer [async deftest is]]
            [knoxx.backend.memory-routes :as memory-routes]))

(defn- offset-from-path
  [path]
  (let [url (js/URL. (str "http://localhost" path))
        offset (js/parseInt (or (.get (.-searchParams url) "offset") "0") 10)]
    (if (js/Number.isFinite offset) offset 0)))

(deftest fetch-authorized-session-pages-stops-after-needed-window
  (async done
    (let [page-offsets* (atom [])
          session-row-fetches* (atom [])
          pages {0 {:rows [{:session "s1"}] :has_more true}
                 1 {:rows [{:session "s2"}] :has_more true}
                 2 {:rows [{:session "s3"}] :has_more true}
                 3 {:rows [{:session "s4"}] :has_more false}}
          openplanner-request! (fn [_config _method path]
                                 (let [offset (offset-from-path path)]
                                   (swap! page-offsets* conj offset)
                                   (js/Promise.resolve (get pages offset {:rows [] :has_more false}))))
          authorized-session-ids! (fn [_config _ctx session-ids]
                                    (js/Promise.resolve (set (map str session-ids))))
          fetch-openplanner-session-rows! (fn [_config session-id]
                                            (swap! session-row-fetches* conj session-id)
                                            (js/Promise.resolve [{:extra "{}"}]))
          session-visible-for-page-actor? (fn [_config _rows _actor-id] true)]
      (-> (memory-routes/fetch-authorized-session-pages! {} {} "chat_primary"
                                                         openplanner-request!
                                                         authorized-session-ids!
                                                         fetch-openplanner-session-rows!
                                                         session-visible-for-page-actor?
                                                         1
                                                         0
                                                         []
                                                         3)
          (.then (fn [result]
                   (let [result (js->clj result :keywordize-keys true)]
                     (is (= [0 1 2] @page-offsets*))
                     (is (= ["s1" "s2" "s3"] @session-row-fetches*))
                     (is (= ["s1" "s2" "s3"] (mapv :session (:rows result))))
                     (is (true? (:has_more result))))))
          (.catch (fn [err]
                    (is nil (str "unexpected rejection: " err))))
          (.finally (fn [] (done)))))))