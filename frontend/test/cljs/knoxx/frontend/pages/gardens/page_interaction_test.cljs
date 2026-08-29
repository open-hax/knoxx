(ns knoxx.frontend.pages.gardens.page-interaction-test
  (:require [cljs.test :refer [deftest is use-fixtures]]
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
                            :url "https://open-hax.promethean.rest/"}
                           {:id "open-hax/home-es" :locale "es" :path "/es/"
                            :state "published"
                            :url "https://open-hax.promethean.rest/es/"}
                           {:id "open-hax/home-archived" :locale "fr" :path "/fr/"
                            :state "archived"
                            :url "https://open-hax.promethean.rest/fr/"}]}]})

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

(deftest ^:async reviews-deployed-contract-and-opens-public-site
  (let [rendered (rtl/render ($ gardens-page))]
    (await (wait-until "Garden contract rendered"
                       #(some? (.queryByText rendered "Open Hax Promethean"))))
    (is (= 1 @load-calls))
    (is (some? (.queryByText rendered "Contract locale catalog")))
    (is (some? (.queryByText rendered "Publication placements")))
    (let [website-link (.getByRole rendered "link" #js {:name "Open website ↗"})
          page-link (first (.getAllByRole
                            rendered "link" #js {:name "Open published page ↗"}))]
      (is (= "https://open-hax.promethean.rest"
             (.getAttribute website-link "href")))
      (is (= "https://open-hax.promethean.rest/"
             (.getAttribute page-link "href"))))
    (is (nil? (.queryByRole rendered "button" #js {:name "+ New Garden"})))
    (is (nil? (.queryByRole rendered "button" #js {:name "Delete"})))
    (is (not (str/includes? (.-textContent (.-container rendered)) "Theme")))))

;; These tests render what a reviewer can click. A handler-only test is
;; vacuous when the control itself disappears.

(deftest ^:async publish-button-is-rendered-for-a-published-placement
  (let [rendered (rtl/render ($ gardens-page))]
    (await (wait-until "placement rendered"
                       #(some? (.queryByText rendered "Open Hax Promethean"))))
    (is (= 2 (count (.getAllByText rendered "Publish")))
        "published placements expose actions; the archived placement does not")))

(deftest ^:async publish-calls-reconcile-and-reports-the-receipt
  (let [calls (atom [])
        real-reconcile api/reconcile-publication!]
    (set! api/reconcile-publication!
          (fn [publication-id]
            (swap! calls conj publication-id)
            (js/Promise.resolve {:type "publication/materialized"})))
    (try
      (let [rendered (rtl/render ($ gardens-page))]
        (await (wait-until "placement rendered"
                           #(some? (.queryByText rendered "Open Hax Promethean"))))
        (rtl/fireEvent.click (first (.getAllByText rendered "Publish")))
        (await (wait-until
                "receipt reported"
                #(some? (.queryByText
                         rendered
                         (fn [content _]
                           (str/includes? (str content) "Published."))))))
        (is (= ["open-hax/home-en"] @calls)
            "reconciliation targets the placement the reviewer clicked"))
      (finally
        (set! api/reconcile-publication! real-reconcile)))))

(deftest ^:async publish-all-runs-every-publishable-placement-in-order
  (let [calls (atom [])
        in-flight (atom 0)
        max-in-flight (atom 0)
        real-reconcile api/reconcile-publication!]
    (set! api/reconcile-publication!
          (^:async fn [publication-id]
            (let [active (swap! in-flight inc)]
              (swap! max-in-flight max active)
              (try
                (await (js/Promise.resolve nil))
                (swap! calls conj publication-id)
                (if (= publication-id "open-hax/home-es")
                  {:type "publication/blocked"
                   :blockers ["translation-missing"]}
                  {:type "publication/materialized"})
                (finally
                  (swap! in-flight dec))))))
    (try
      (let [rendered (rtl/render ($ gardens-page))]
        (await (wait-until "garden rendered"
                           #(some? (.queryByText rendered "Open Hax Promethean"))))
        (is (some? (.queryByText rendered "Publish all (2)"))
            "archived placements are not offered for publication")
        (rtl/fireEvent.click (.getByText rendered "Publish all (2)"))
        (await (wait-until
                "complete run summary reported"
                #(some? (.queryByText
                         rendered
                         (fn [content _]
                           (and (str/includes? (str content) "1 published")
                                (str/includes? (str content) "1 blocked")
                                (str/includes? (str content) "(2 attempted)")))))))
        (is (= ["open-hax/home-en" "open-hax/home-es"] @calls)
            "every publish executes in the listed order")
        (is (= 1 @max-in-flight)
            "one shared-manifest reconciliation is in flight at a time"))
      (finally
        (set! api/reconcile-publication! real-reconcile)))))
