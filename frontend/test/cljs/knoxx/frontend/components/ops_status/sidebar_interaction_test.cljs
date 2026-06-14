(ns knoxx.frontend.components.ops-status.sidebar-interaction-test
  "Interaction contract for the Helix sidebar ops status: ws stats drive
  the metric rows; the ingestion poll drives the three section states.
  ws connect-stream and the documents api are mocked via set!."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            ["@testing-library/react" :as rtl]
            ["react" :as react]
            [helix.core :refer [$]]
            [knoxx.frontend.lib.ws :as ws]
            [knoxx.frontend.pages.documents.api :as documents-api]
            [knoxx.frontend.components.ops-status.sidebar :refer [sidebar-ops-status]]))

(def stream-handlers (atom nil))
(def disconnects (atom 0))
(def progress-response (atom {:active false :canResumeForum false}))

(def ^:private real-connect ws/connect-stream)
(def ^:private real-progress documents-api/ingestion-progress)

(use-fixtures :each
  {:before (fn []
             (reset! stream-handlers nil)
             (reset! disconnects 0)
             (reset! progress-response {:active false :canResumeForum false})
             (set! ws/connect-stream
                   (fn [handlers & _]
                     (reset! stream-handlers handlers)
                     {:disconnect #(swap! disconnects inc)
                      :set-conversation-id (fn [_])}))
             (set! documents-api/ingestion-progress
                   (fn [] (js/Promise.resolve @progress-response))))
   :after (fn []
            (rtl/cleanup)
            (set! ws/connect-stream real-connect)
            (set! documents-api/ingestion-progress real-progress))})

(defn- wait-until [msg pred]
  (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))))

(deftest stats-update-metric-rows
  (async done
    (let [r (rtl/render ($ sidebar-ops-status))]
      (-> (wait-until "stream attached" #(some? @stream-handlers))
          (.then (fn []
                   (.act react
                         (fn []
                           ((:on-stats @stream-handlers)
                            (clj->js {:cpu_percent 42.5 :memory_percent 33.3
                                      :gpu [{:util_gpu 91}]}))))
                   (wait-until "cpu shown" #(some? (.queryByText r "42.5%")))))
          (.then (fn []
                   (is (some? (.queryByText r "33.3%")))
                   (is (some? (.queryByText r "91.0%")))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest ingestion-states-render
  (async done
    (reset! progress-response {:active true
                               :progress {:processedChunks 10 :totalChunks 40
                                          :percentPrecise 25.0 :currentFile "doc.md"}})
    (let [r (rtl/render ($ sidebar-ops-status))]
      (-> (wait-until "active ingestion" #(some? (.queryByText r "10 / 40 (25.00%)")))
          (.then (fn []
                   (is (some? (.queryByText r "doc.md")))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest disconnects-stream-on-unmount
  (async done
    (let [r (rtl/render ($ sidebar-ops-status))]
      (-> (wait-until "stream attached" #(some? @stream-handlers))
          (.then (fn []
                   (.unmount r)
                   (is (= 1 @disconnects))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
