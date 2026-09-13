(ns knoxx.backend.infra.clio-policy-store
  "Canonical finite local directory commands, current roles and membership views."
  (:require [knoxx.backend.domain.local-policy-commands :as commands]
            [knoxx.backend.domain.local-policy-directory :as directory]
            [knoxx.backend.domain.local-policy-reads :as reads]
            [knoxx.backend.extern.local-policy :as host]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.law.identity-binding :as binding-law]
            [knoxx.backend.law.local-policy :as law]))

(def empty-state
  "Empty disposable policy projection; canonical operations remain authoritative."
  {:orgs {} :users {} :memberships {} :roles {} :credentials {} :data-lakes {} :tools {} :commands {} :audit []})

(defn- apply-transition! [state transition & args]
  (let [[next-state result] (apply transition @state args)] (reset! state next-state) result))

(defn- projection []
  (let [state (atom empty-state)] {:store state :snapshot #(deref state)}))

(defn- read-method [operation]
  (fn [state & args] (reads/read-operation @state operation args)))

(defn open!
  "Open an explicit isolated directory. Catalog initialization is a separate admission."
  [{:keys [directory]}]
  (let [engine (clio/open!
                {:directory directory :stream "knoxx/policy" :projection projection
                 :reads (into {} (map #(vector % (read-method %))) reads/operations)
                 :writes {:policy/initialize (fn [state input] (apply-transition! state directory/initialize input))
                          :policy/observe (fn [state binding principal at] (apply-transition! state directory/observe binding principal at))
                          :policy/command (fn [state command] (apply-transition! state commands/admit command))}})]
    {:engine engine :directory (:directory engine)}))

(defn read!
  "Trusted composition reads the finite projection; public callers use authorized-read!."
  [store operation args]
  (clio/read! (:engine store) operation args))

(defn ^:async initialize!
  "Install an explicit server-owned role catalog and default organization once."
  [store {:keys [roles] :as catalog}]
  (binding-law/assert-role-catalog! roles)
  (await (host/with-lock! (str (:directory store) "/policy-store.lock")
          (fn [] (clio/write! (:engine store) :policy/initialize [catalog])))))

(defn ^:async observe!
  "Observe a freshly verified Axxium binding without deriving authority from email."
  [store binding principal]
  (binding-law/assert-principal-binding! principal binding)
  (await (host/with-lock! (str (:directory store) "/policy-store.lock")
          (fn [] (clio/write! (:engine store) :policy/observe [binding principal (host/now-iso)])))))

(defn ^:async authorized-read!
  "Revalidate directory read authority against the same current projection returned."
  [store actor operation args]
  (let [state (await (read! store :policy/state []))]
    (reads/authorize-read! state actor operation args)
    (reads/read-operation state operation args)))

(defn ^:async command!
  "Serialize authorization and durable command admission, returning the first retry result."
  [store command]
  (law/assert-schema! law/Command command)
  (await (host/with-lock! (str (:directory store) "/policy-store.lock")
          (fn [] (clio/write! (:engine store) :policy/command [command])))))
