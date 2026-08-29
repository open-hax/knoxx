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

;; ── publishing ─────────────────────────────────────────────────────────────
;;
;; These render the page and assert on what a reviewer can actually click.
;; The first version of this feature wired the handler, the API call and the
;; receipt summary, and never rendered a button — every unit test passed and
;; the build was clean, because passing unused props to a `defnc` is silent.
;; Only a render assertion catches that, so it is the one this feature keeps.

(deftest publish-button-is-rendered-for-a-published-placement
  (async done
    (let [rendered (rtl/render ($ gardens-page))]
      (-> (wait-until "placement rendered"
                      #(some? (.queryByText rendered "Open Hax Promethean")))
          (.then (fn []
                   (is (some? (.queryByText rendered "Publish"))
                       "a placement whose contract asks to be published offers a publish action")))
          (.finally done)))))

(deftest publish-calls-reconcile-and-reports-the-receipt
  (async done
    (let [calls (atom [])
          real-reconcile api/reconcile-publication!]
      (set! api/reconcile-publication!
            (fn [publication-id]
              (swap! calls conj publication-id)
              (js/Promise.resolve {:type "publication/materialized"})))
      (let [rendered (rtl/render ($ gardens-page))]
        (-> (wait-until "placement rendered"
                        #(some? (.queryByText rendered "Open Hax Promethean")))
            (.then (fn []
                     (rtl/fireEvent.click (.getByText rendered "Publish"))
                     (wait-until "receipt reported"
                                 #(some? (.queryByText
                                          rendered
                                          (fn [content _]
                                            (str/includes? (str content) "Published.")))))))
            (.then (fn []
                     (is (= ["open-hax/home-en"] @calls)
                         "reconciliation is demanded for the placement that was clicked")))
            ;; A rejected wait must FAIL, not skip the assertions and let
            ;; `finally` report a pass. Without this the test is vacuous
            ;; whenever the thing it checks stops happening, which is exactly
            ;; when it needs to speak up.
            (.catch (fn [err]
                      (is false (str "publish flow did not complete: " err))))
            (.finally (fn []
                        (set! api/reconcile-publication! real-reconcile)
                        (done))))))))
