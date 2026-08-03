(ns knoxx.backend.law.mcp-oauth
  "Malli contracts for Knoxx's MCP OAuth persistence boundaries.

   Contract policy only — no I/O. The store namespace calls these; it does not
   define them, so the obligations of a boundary can be read without reading
   the code that performs it."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def NonBlankString
  [:and string? [:fn {:error/message "must not be blank"} #(not (str/blank? %))]])

(def RevocationRequest
  "Identity required to revoke an access token.

   Both fields are non-blank on purpose. A blank membership would widen the
   delete from 'this caller's token' to 'this token, whoever owns it' — the
   difference between revoking your own credential and revoking someone
   else's. A blank token matches nothing against today's schema, but the
   obligation belongs here rather than in whichever route happens to call in."
  [:map
   [:access-token  NonBlankString]
   [:membership-id NonBlankString]])

(def RevocationResult
  "Decoded outcome of a revocation.

   A count rather than a boolean, and required: a result that lost its count
   must not be readable as 'nothing was deleted'."
  [:map
   [:deleted-count [:int {:min 0}]]])

(defn valid-revocation-request?
  [request]
  (m/validate RevocationRequest request))

(defn valid-revocation-result?
  [result]
  (m/validate RevocationResult result))
