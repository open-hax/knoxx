(ns knoxx.backend.extern.openplanner-admission-probe
  "Native process boundary for the real OpenPlanner operation-lock proof."
  (:require [knoxx.backend.infra.clients.openplanner :as client]
            [knoxx.backend.infra.clients.openplanner-clio :as local]
            [knoxx.backend.infra.clio-application-store :as engine]))

(defn- open! [directory]
  (local/open! {:directory directory
                :embedding-config {:embed-provider-base-url "http://127.0.0.1:9999/v1"
                                   :embed-provider-model "finite" :embed-provider-dimensions 2}
                :embed! (fn [texts] {:model "finite" :dimensions 2
                                     :vectors (mapv (constantly [1 0]) texts)})}))

(defn prepare!
  "Initialize schemas once before independently opening two worker processes."
  [directory]
  (open! directory)
  true)

(defn ^:async write!
  "Signal ready over native IPC, then concurrently admit this worker's real facts."
  [directory label]
  (let [provider (open! directory)
        start (js/Promise. (fn [complete _reject] (.once js/process "message" complete)))]
    (.send js/process #js {:ready true})
    (await start)
    (doseq [index (range 4)]
      (await (client/events!
              provider [{:id (str label "-" index) :text (str "Fact " label " " index)
                         :source "knoxx" :kind "diagnostic" :ts "2026-09-12T12:00:00Z"
                         :source_ref {:project "wiki" :session "cross-process"}
                         :meta {:role "system"} :extra {:org_id "organization"}}])))
    true))

(defn ^:async verify!
  "Reopen canonical facts and verify both processes' event and vector admissions."
  [directory]
  (let [provider (open! directory)
        expected (set (for [label ["left" "right"] index (range 4)] (str label "-" index)))
        rows (:rows (await (client/session! provider "cross-process" {:org_id "organization" :project "wiki"})))
        operations (engine/history (:ledger provider))]
    (when-not (= expected (set (map :id rows)))
      (throw (ex-info "A process lost event admission" {:actual (mapv :id rows)})))
    (doseq [id expected]
      (when-not (= [1 0] (:embedding (await (engine/read! (:ledger provider) :openplanner/read [{:kind :vector :id id}]))))
        (throw (ex-info "A process lost vector admission" {:id id}))))
    (when-not (= 16 (count operations))
      (throw (ex-info "Cross-process history contains missing or duplicate operations" {:count (count operations)})))
    #js {:ok true :processes 2 :events 8 :vectors 8 :operations 16}))
