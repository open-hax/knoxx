(ns knoxx.frontend.pages.translations.review-controller-test
  "Availability boundaries between resource work and legacy enrichment."
  (:require [cljs.test :refer [deftest is use-fixtures]]
            [knoxx.frontend.pages.translations.api :as api]
            [knoxx.frontend.pages.translations.review-controller :as controller]))

(def ^:private original-list-documents api/list-documents)
(def ^:private original-list-publication-reviews api/list-publication-reviews)

(use-fixtures
  :each
  {:after (fn []
            (set! api/list-documents original-list-documents)
            (set! api/list-publication-reviews
                  original-list-publication-reviews))})

(defn- completion
  []
  (let [resolve! (atom nil)
        promise (js/Promise. (fn [resolve _reject]
                               (reset! resolve! resolve)))]
    {:promise promise :resolve! #(when-let [resolve @resolve!] (resolve %))}))

(defn- controller-state
  [completed]
  (let [state (atom {:documents nil
                     :selected nil
                     :loading nil
                     :error :unset
                     :project "devel"})]
    {:state state
     :setters
     {:set-loading! (fn [loading?]
                      (swap! state assoc :loading loading?)
                      (when-not loading? ((:resolve! completed) true)))
      :set-error! #(swap! state assoc :error %)
      :set-documents! #(swap! state assoc :documents %)
      :set-selected! (fn [next-value]
                       (swap! state update :selected
                              #(if (fn? next-value)
                                 (next-value %)
                                 next-value)))
      :set-project! #(swap! state assoc :project %)}}))

(def ^:private resource-work
  {:publication "publications/resource-es"
   :document "docs/resource"
   :garden "gardens/sonic"
   :project "devel"
   :source_locale "en"
   :locale "es"
   :title "Resource survives"
   :revision "source-1"
   :work_state "missing"
   :reviewable false
   :approved false
   :allowed_actions ["dispatch"]})

(deftest ^:async resource-inventory-survives-legacy-list-failure
  (let [completed (completion)
        {:keys [state setters]} (controller-state completed)]
    (set! api/list-publication-reviews
          (fn []
            (js/Promise.resolve {:project "devel"
                                 :reviews [resource-work]})))
    (set! api/list-documents
          (fn [_]
            (js/Promise.reject (js/Error. "OpenPlanner unavailable"))))
    (controller/load-documents! "devel" "" #js {:current 0} setters)
    (await (:promise completed))
    (is (= "Resource survives"
           (:title (first (:documents @state)))))
    (is (= "publications/resource-es"
           (:publication (first (:documents @state)))))
    (is (nil? (:error @state)))
    (is (false? (:loading @state)))))

(deftest ^:async legacy-only-list-failure-remains-visible
  (let [completed (completion)
        {:keys [state setters]} (controller-state completed)]
    (set! api/list-publication-reviews
          (fn []
            (js/Promise.resolve {:project "devel" :reviews []})))
    (set! api/list-documents
          (fn [_]
            (js/Promise.reject (js/Error. "OpenPlanner unavailable"))))
    (controller/load-documents! "devel" "" #js {:current 0} setters)
    (await (:promise completed))
    (is (= [] (:documents @state)))
    (is (= "OpenPlanner unavailable" (:error @state)))
    (is (false? (:loading @state)))))
