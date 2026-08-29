(ns knoxx.backend.law.mcp-token
  "The CLJS token-record contract at the MCP native-object boundary."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(def NonBlankString
  [:and string? [:fn #(not (str/blank? %))]])

(def TokenRecord
  "The grant-derived record consumed by the MCP route's JavaScript integration."
  [:map {:closed false}
   [:accessToken NonBlankString]
   [:clientId NonBlankString]
   [:userEmail NonBlankString]
   [:tools [:vector NonBlankString]]
   [:membershipId {:optional true} NonBlankString]
   [:orgSlug {:optional true} NonBlankString]
   [:actorId {:optional true} NonBlankString]])

(defn valid-record?
  "True when `record` is admissible at the MCP token-record boundary."
  [record]
  (m/validate TokenRecord record))

(defn assert-record!
  "Return `record`, or fail before malformed authorization data becomes native."
  [record]
  (if (valid-record? record)
    record
    (throw (ex-info "Invalid MCP token record"
                    {:contract :mcp/token-record
                     :errors (me/humanize (m/explain TokenRecord record))}))))
