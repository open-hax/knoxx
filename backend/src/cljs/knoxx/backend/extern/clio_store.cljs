(ns knoxx.backend.extern.clio-store
  "Node path boundary for application-owned canonical Clio directories."
  (:require ["node:path" :as path]
            [knoxx.backend.law.clio-application-store :as law]))

(defn resolve-directory
  "Resolve an explicitly validated directory without changing the process cwd."
  [directory]
  (path/resolve (law/assert-directory! directory)))

(defn report-subscriber-failure!
  "Report a failed observer independently of an already accepted durable write."
  [error]
  (.error js/console "[clio-store] Change subscriber failed" error))

(defn ^:async notify-subscriber!
  "Invoke a zero-payload observer and contain both thrown and rejected failures."
  [listener]
  (try (await (listener))
       (catch :default error (report-subscriber-failure! error))))
