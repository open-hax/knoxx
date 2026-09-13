(ns knoxx.frontend.components.ops-status.sidebar-interaction-test
  "Interaction contract for the Helix sidebar ops status: ws stats drive
  the metric rows; the ingestion poll drives the three section states.
  ws connect-stream and the documents api are mocked via set!."
  (:require ["@testing-library/react" :as rtl]
            ["react" :as react]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.components.ops-status.sidebar :as sidebar]
            [knoxx.frontend.lib.ws :as ws]
            [knoxx.frontend.pages.documents.api :as documents-api]))

(def ^:private stream-handlers (atom nil))
(def ^:private disconnects (atom 0))
(def ^:private progress-response (atom {:active false :canResumeForum false}))

(def ^:private real-connect ws/connect-stream)
(def ^:private real-progress documents-api/ingestion-progress)

(t/use-fixtures :each
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

(t/deftest ^:async stats-update-metric-rows
  (let [r (rtl/render (hx/$ sidebar/sidebar-ops-status))]
    (await (wait-until "stream attached" #(some? @stream-handlers)))
    (.act react (fn []
                  ((:on-stats @stream-handlers)
                   (clj->js {:cpu_percent 42.5 :memory_percent 33.3 :gpu [{:util_gpu 91}]}))))
    (await (wait-until "cpu shown" #(some? (.queryByText r "42.5%"))))
    (t/is (some? (.queryByText r "33.3%")))
    (t/is (some? (.queryByText r "91.0%")))))

(t/deftest ^:async ingestion-states-render
  (reset! progress-response {:active true
                             :progress {:processedChunks 10 :totalChunks 40
                                        :percentPrecise 25.0 :currentFile "doc.md"}})
  (let [r (rtl/render (hx/$ sidebar/sidebar-ops-status))]
    (await (wait-until "active ingestion" #(some? (.queryByText r "10 / 40 (25.00%)"))))
    (t/is (some? (.queryByText r "doc.md")))))

(t/deftest ^:async disconnects-stream-on-unmount
  (let [r (rtl/render (hx/$ sidebar/sidebar-ops-status))]
    (await (wait-until "stream attached" #(some? @stream-handlers)))
    (.unmount r)
    (t/is (= 1 @disconnects))))
