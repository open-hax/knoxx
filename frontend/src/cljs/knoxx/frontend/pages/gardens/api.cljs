(ns knoxx.frontend.pages.gardens.api
  "Authenticated Knoxx-owned Garden deployment reads, and the one write a
   deployment review can make: demanding reconciliation of a placement."
  (:require [knoxx.frontend.lib.api :as api]
            [knoxx.frontend.pages.gardens.logic :as logic]))

(def list-path "/api/publications/gardens")

(defn load-deployment!
  []
  (-> (api/request list-path)
      (.then logic/normalize-deployment)))

(defn reconcile-publication!
  "Demand reconciliation of one publication and answer with its receipt.

   Reconciliation is per-publication by contract: `law.publication-reconciler/
   ReconcileTrigger` is a closed map requiring `:publication/id`, so there is no
   reconcile-everything call to make here and a caller must name what it wants.

   This is the only route to publication for an intent that needs no approval.
   A translated locale reaches reconciliation through the translation page,
   which chains it onto recording an approval — but a source-locale intent
   carries `:translation/review :none`, has no translation receipt, never
   appears among reviewable translations, and therefore had no path at all
   before this."
  [publication-id]
  (api/request "/api/publications/reconcile"
               {:method "POST"
                :body {:publicationId publication-id}}))
