(ns knoxx.frontend.pages.gardens.logic-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.pages.gardens.logic :as logic]))

(deftest public-site-links-preserve-contract-paths
  (is (= "https://open-hax.promethean.rest/es/"
         (logic/public-url "https://open-hax.promethean.rest/" "/es/")))
  (is (= "http://localhost:4173/docs/probe"
         (logic/public-url "http://localhost:4173" "docs/probe"))))

(deftest deployment-wire-normalizes-without-content-or-style
  (let [view (logic/normalize-deployment
              {:site-url "https://open-hax.promethean.rest"
               :gardens
               [{:garden {:id "open-hax/garden" :title "Open Hax"
                           :status "active" :locales ["en" "es"]}
                 :publications
                 [{:id "open-hax/home-es" :locale "es" :path "/es/"
                   :state "published"}
                  {:id "open-hax/home-en" :locale "en" :path "/"
                   :state "published"}]}]})
        garden (first (:gardens view))]
    (is (= "open-hax/garden" (:id garden)))
    (is (= ["en" "es"] (:locales garden)))
    (is (= ["en" "es"] (mapv :locale (:placements garden)))
        "placements are stable by locale then path")
    (is (= ["https://open-hax.promethean.rest/"
            "https://open-hax.promethean.rest/es/"]
           (mapv :url (:placements garden))))
    (testing "Garden review does not regrow OpenPlanner-era content/style fields"
      (is (not-any? #(contains? garden %)
                    [:description :theme :auto-translate :content :layout])))))

(deftest language-labels-are-human-readable
  (is (= "Español" (logic/language-name "es")))
  (is (= "xx" (logic/language-name "xx"))))
