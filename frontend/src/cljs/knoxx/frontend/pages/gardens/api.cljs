(ns knoxx.frontend.pages.gardens.api
  "Authenticated Knoxx-owned Garden deployment reads."
  (:require [knoxx.frontend.lib.api :as api]
            [knoxx.frontend.pages.gardens.logic :as logic]))

(def list-path "/api/publications/gardens")

(defn load-deployment!
  []
  (-> (api/request list-path)
      (.then logic/normalize-deployment)))
