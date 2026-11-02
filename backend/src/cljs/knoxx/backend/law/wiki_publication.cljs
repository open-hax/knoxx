(ns knoxx.backend.law.wiki-publication
  "Closed publication command data shared by browser and agent adapters."
  (:require [knoxx.backend.law.publication :as publication]
            [malli.core :as m]))

(def QualifiedWireId
  "Resource identity retains its namespace over JSON."
  [:and publication/NonBlankString [:fn #(qualified-keyword? (keyword %))]])

(def PublishWire
  "Request one explicit destination at the source digest the caller saw."
  [:map {:closed true}
   [:publication QualifiedWireId]
   [:expected_revision publication/ConcreteRevision]])

(def Scope
  "Server-resolved organization/project context for publication evidence."
  [:map {:closed true}
   [:org-id publication/NonBlankString]
   [:project [:maybe publication/NonBlankString]]])

(def ReceiptWire
  "Receipt identity and trigger correlation occupy distinct wire fields."
  [:map
   [:publication {:optional true} :string]
   [:revision {:optional true} [:maybe :string]]
   [:correlation {:optional true}
    [:map {:closed true}
     [:publication {:optional true} :string]
     [:revision {:optional true} [:maybe :string]]
     [:trigger {:optional true} :string]
     [:origin {:optional true} :string]]]])

(defn assert-receipt-wire!
  "Reject a malformed publication receipt projection."
  [receipt]
  (when-not (m/validate ReceiptWire receipt)
    (throw (ex-info "Invalid Wiki publication receipt" {:code "wiki_receipt_invalid"})))
  receipt)

(defn assert-command!
  "Classify malformed browser/tool requests before any resource write."
  [body]
  (when-not (m/validate PublishWire body)
    (throw (ex-info "Invalid Wiki publication command"
                    {:status 400 :code "wiki_publication_invalid"})))
  body)
