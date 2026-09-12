(ns knoxx.backend.infra.persistence-bootstrap
  "Install explicitly selected application persistence providers before recovery."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.clock :as clock]
            [knoxx.backend.infra.run-events :as events]
            [knoxx.backend.infra.stores.cache-registry :as caches]
            [knoxx.backend.infra.stores.clio-cache-store :as clio-cache]
            [knoxx.backend.infra.stores.clio-mcp-oauth :as clio-mcp]
            [knoxx.backend.infra.stores.clio-run-store :as clio-run]
            [knoxx.backend.infra.stores.clio-thread-store :as clio-thread]
            [knoxx.backend.infra.stores.mongo-cache-store :as mongo-cache]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as mongo-mcp]
            [knoxx.backend.infra.stores.mongo-run-store :as mongo-run]
            [knoxx.backend.infra.stores.mongo-session-store :as sessions]
            [knoxx.backend.infra.stores.mongo-thread-store :as mongo-thread]
            [knoxx.backend.infra.stores.session-store-registry :as runs]
            [knoxx.backend.law.persistence-config :as law]))

(defn mongo-required?
  "Mongo is contacted only when an application service explicitly selects it."
  [config]
  (boolean (some #{:mongodb} (vals (law/selection! config)))))

(defn install-local!
  "Validate every selection before opening a ledger; install only selected EDN ports."
  [config]
  (let [selection (law/selection! config)
        directory (:wiki-directory config)]
    (when-not (and (string? directory) (not (str/blank? directory)))
      (throw (ex-info "Persistence directory is required"
                      {:status 503 :code "persistence_directory_required"})))
    (when (= :edn (:run-provider selection))
      (reset! runs/session-store* (clio-run/open! {:directory (str directory "/runs")
                                                  :clock! clock/instant-iso})))
    (when (= :edn (:thread-provider selection))
      (sessions/install! (clio-thread/open! {:directory (str directory "/threads")})))
    (when (= :edn (:cache-provider selection))
      (caches/install! (clio-cache/open! {:directory (str directory "/cache")})))
    (when (= :edn (:mcp-oauth-provider selection))
      (mongo-mcp/install! (clio-mcp/open! {:directory (str directory "/mcp-oauth")})))
    (events/install! @runs/session-store*)
    selection))

(defn ^:async install-mongo!
  "Await each selected Mongo adapter's indexes before exposing its provider."
  [config db]
  (let [selection (law/selection! config)]
    (when (= :mongodb (:run-provider selection))
      (await (mongo-run/setup-indexes! db))
      (reset! runs/session-store* (mongo-run/create-mongo-run-store db)))
    (when (= :mongodb (:thread-provider selection))
      (await (mongo-thread/setup-indexes! db))
      (sessions/install! (mongo-thread/create-store db)))
    (when (= :mongodb (:cache-provider selection))
      (await (mongo-cache/setup-indexes! db))
      (caches/install! (mongo-cache/create-store db)))
    (when (= :mongodb (:mcp-oauth-provider selection))
      (await (mongo-mcp/setup-indexes! db))
      ;; The compatibility facade's explicit nil selection uses its Mongo path.
      (mongo-mcp/install! nil))
    (events/install! @runs/session-store*)
    selection))
