(ns knoxx.frontend.pages.gardens.page-interaction-test
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            [clojure.string :as str]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.pages.gardens.api :as api]
            [knoxx.frontend.pages.gardens.view :refer [gardens-page]]))

(def deployment
  {:site-url "https://open-hax.promethean.rest"
   :gardens [{:id "open-hax/promethean" :title "Open Hax Promethean"
              :status "active" :locales ["en" "es" "fr" "de" "ja"]
              :placements [{:id "open-hax/home-en" :locale "en" :path "/"
                            :state "published"
                            :url "https://open-hax.promethean.rest/"}]}]})

(def load-calls (atom 0))
(def ^:private real-load api/load-deployment!)

(use-fixtures :each
  {:before (fn []
             (reset! load-calls 0)
             (set! api/load-deployment!
                   (fn []
                     (swap! load-calls inc)
                     (js/Promise.resolve deployment))))
   :after (fn []
            (rtl/cleanup)
            (set! api/load-deployment! real-load))})

(defn- wait-until [message pred]
  (rtl/waitFor
   (fn []
     (when-not (pred)
       (throw (js/Error. (str "still waiting: " message)))))))

(deftest reviews-deployed-contract-and-opens-public-site
  (async done
    (let [rendered (rtl/render ($ gardens-page))]
      (-> (wait-until "Garden contract rendered"
                      #(some? (.queryByText rendered "Open Hax Promethean")))
          (.then
           (fn []
             (is (= 1 @load-calls))
             (is (some? (.queryByText rendered "Contract locale catalog")))
             (is (some? (.queryByText rendered "Publication placements")))
             (let [website-link (.getByRole rendered "link" #js {:name "Open website ↗"})
                   page-link (.getByRole rendered "link" #js {:name "Open published page ↗"})]
               (is (= "https://open-hax.promethean.rest" (.getAttribute website-link "href")))
               (is (= "https://open-hax.promethean.rest/" (.getAttribute page-link "href"))))
             (is (nil? (.queryByRole rendered "button" #js {:name "+ New Garden"})))
             (is (nil? (.queryByRole rendered "button" #js {:name "Delete"})))
             (is (not (str/includes? (.-textContent (.-container rendered)) "Theme")))
             (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
