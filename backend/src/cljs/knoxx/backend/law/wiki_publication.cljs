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

(defn assert-command!
  "Classify malformed browser/tool requests before any resource write."
  [body]
  (when-not (m/validate PublishWire body)
    (throw (ex-info "Invalid Wiki publication command"
                    {:status 400 :code "wiki_publication_invalid"})))
  body)
