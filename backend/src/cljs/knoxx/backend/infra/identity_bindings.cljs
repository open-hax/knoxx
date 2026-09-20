(ns knoxx.backend.infra.identity-bindings
  "Durable, explicit identity coordinates separate from mutable policy directory rows."
  (:require [knoxx.backend.infra.clio-application-store :as application]
            [knoxx.backend.law.identity-binding :as law]))

(defn- projection []
  (let [entries (atom {})]
    {:store entries :snapshot #(deref entries)}))

(defn- bind-coordinate! [entries binding]
  (law/assert-binding! binding)
  (let [principal-id (:principal-id binding)]
    (if-let [existing (get @entries principal-id)]
      (when-not (= existing binding)
        (throw (ex-info "Identity is already bound to different policy coordinates"
                        {:status 409 :code "identity_binding_conflict"})))
      (swap! entries assoc principal-id binding))
    binding))

(defn open!
  "Open bindings over the canonical Clio application ledger."
  [{:keys [directory]}]
  (application/open! {:directory directory :stream "knoxx/identity-bindings"
                      :projection projection
                      :reads {:identity/binding (fn [entries principal-id] (get @entries principal-id))}
                      :writes {:identity/bind bind-coordinate!}}))

(defn ^:async read!
  "Look up only an immutable verified principal ID; email is never a lookup key."
  [store principal-id]
  (await (application/read! store :identity/binding [principal-id])))

(defn ^:async bind!
  "Admit one complete binding; conflicting rebinds refuse atomically."
  [store binding]
  (await (application/write! store :identity/bind [binding])))
