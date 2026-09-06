(ns knoxx.frontend.law.migration-manifest
  "Portable Malli contracts for the generated frontend migration manifest."
  (:require [malli.core :as m]))

(def MigrationIsland
  [:enum :agents :auth :chat :cms :data :ops :shared :studio])

(def MigrationDisposition
  [:enum :assess :delete :port :wrap])

(def MigrationRecord
  [:multi {:dispatch :migration/kind}
   [:legacy-source
    [:map {:closed true}
     [:migration/kind [:= :legacy-source]]
     [:migration/path string?]
     [:migration/language [:enum :declaration :ts :tsx]]
     [:migration/island MigrationIsland]
     [:migration/disposition MigrationDisposition]
     [:migration/test? boolean?]]]
   [:legacy-test
    [:map {:closed true}
     [:migration/kind [:= :legacy-test]]
     [:migration/path string?]
     [:migration/island MigrationIsland]]]
   [:bridge-export
    [:map {:closed true}
     [:migration/kind [:= :bridge-export]]
     [:migration/path string?]
     [:migration/bridge [:enum :app :frontend]]
     [:migration/export string?]]]
   [:bridge-route
    [:map {:closed true}
     [:migration/kind [:= :bridge-route]]
     [:migration/path string?]
     [:migration/component string?]
     [:migration/index nat-int?]]]])

(defn valid-record?
  "Returns true when a generated manifest record satisfies its kind-specific contract."
  [record]
  (m/validate MigrationRecord record))

(defn assert-record!
  "Returns record when valid; throws ex-info with Malli evidence otherwise."
  [record]
  (if (valid-record? record)
    record
    (throw (ex-info "Frontend migration manifest contract violation"
                    {:record record
                     :explain (m/explain MigrationRecord record)}))))
