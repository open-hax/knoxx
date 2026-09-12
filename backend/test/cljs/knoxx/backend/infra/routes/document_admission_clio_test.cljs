(ns knoxx.backend.infra.routes.document-admission-clio-test
  "The production admission writer must work through the actual Clio provider."
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.infra.clients.openplanner :as client]
            [knoxx.backend.infra.clients.openplanner-clio :as local]
            [knoxx.backend.infra.routes.document-admission :as admission]))

(def ^:private source-event
  {:schema "openplanner.event.v1" :id "publication-snapshot-clio-regression"
   :ts "2026-09-12T12:00:00.000Z" :source "knoxx-publication"
   :kind "document-snapshot" :source_ref {:project "wiki" :message "docs/page"}
   :text "People decide source acceptance."
   :extra {:org_id "org-a" :document_id "docs/page" :source_revision "sha256-source"}})

(defn- options [directory]
  {:directory directory
   :embedding-config {:embed-provider-base-url "http://127.0.0.1:9999/v1"
                      :embed-provider-model "fixture-model" :embed-provider-dimensions 3}
   :embed! (fn [texts] {:model "fixture-model" :dimensions 3
                        :vectors (mapv (constantly [1 0 0]) texts)})})

(defn- ^:async attempt! [operation]
  (try {:result (await (operation))}
       (catch :default error {:error (ex-data error)})))

(deftest ^:async source-admission-persists-and-replays-through-the-selected-clio-provider
  (let [directory (disk/temp-directory!)]
    (try
      (let [provider (local/open! (options directory))
            first-result (await (attempt! #(admission/persist-openplanner-event! {} provider source-event)))
            reopened (local/open! (options directory))
            retry (await (attempt! #(admission/persist-openplanner-event! {} reopened source-event)))]
        (is (nil? (:error first-result)))
        (is (= 1 (get-in first-result [:result :count])))
        (is (true? (get-in retry [:result :existing])))
        (is (= 1 (get-in retry [:result :index-result :vector-count])))
        (is (= source-event (await (client/event-by-id! reopened (:id source-event))))))
      (finally (fs/remove-tree! directory)))))

(deftest ^:async failed-indexing-keeps-the-source-fact-and-retry-repairs-its-projection
  (let [directory (disk/temp-directory!)]
    (try
      (let [provider (local/open! (assoc (options directory) :embed!
                                        (fn [_] (throw (ex-info "model unavailable" {:code "fixture_model_unavailable"})))))
            failed (await (attempt! #(admission/persist-openplanner-event! {} provider source-event)))]
        (is (= "fixture_model_unavailable" (get-in failed [:error :code])))
        (is (= source-event (await (client/event-by-id! provider (:id source-event)))))
        (let [reopened (local/open! (options directory))
              retry (await (attempt! #(admission/persist-openplanner-event! {} reopened source-event)))]
          (is (nil? (:error retry)))
          (is (true? (get-in retry [:result :existing])))
          (is (= [(:id source-event)] (get-in retry [:result :index-result :repaired-event-ids])))
          (is (= [[(:id source-event)]]
                 (get-in (await (client/vector-search! reopened
                                                      {:org_id "org-a" :project "wiki" :q "source acceptance"}))
                         [:result :ids])))))
      (finally (fs/remove-tree! directory)))))
