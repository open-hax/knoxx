(ns knoxx.backend.infra.identity-bindings
  "Durable, explicit identity coordinates separate from mutable policy directory rows."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.clio-application-store :as application]
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

(defn- bind-membership-coordinate! [entries binding]
  (law/assert-binding! binding)
  (let [{:keys [principal-id membership-id]} binding
        canonical (get @entries principal-id)
        owner-keys [:principal-id :entity-id :kind :user-id]
        coordinate [:identity/membership principal-id membership-id]]
    (when-not canonical
      (throw (ex-info "A canonical identity binding is required first"
                      {:status 404 :code "identity_binding_not_found"})))
    (when-not (= (select-keys canonical owner-keys) (select-keys binding owner-keys))
      (throw (ex-info "Membership belongs to a different identity owner"
                      {:status 403 :code "identity_binding_owner_conflict"})))
    (if-let [existing (if (= membership-id (:membership-id canonical))
                        canonical (get @entries coordinate))]
      (when-not (= existing binding)
        (throw (ex-info "Identity membership is already bound to different coordinates"
                        {:status 409 :code "identity_binding_conflict"})))
      ;; Keep the original string-keyed canonical projection unchanged. New facts
      ;; add a separate coordinate; they cannot replace the default binding.
      (swap! entries assoc coordinate binding))
    binding))

(defn- assert-selector! [principal-id selector]
  (when-not (and (string? principal-id) (not (str/blank? principal-id))
                 (map? selector) (seq selector)
                 (every? #{:membership-id :org-id} (keys selector))
                 (every? #(and (string? %) (not (str/blank? %))) (vals selector)))
    (throw (ex-info "An exact membership or organization selector is required"
                    {:status 400 :code "identity_membership_selector_invalid"})))
  selector)

(defn- select-membership [entries principal-id selector]
  (assert-selector! principal-id selector)
  (let [matches (->> (vals @entries)
                     (filter #(and (= principal-id (:principal-id %))
                                   (= selector (select-keys % (keys selector)))))
                     (take 2)
                     vec)]
    (case (count matches)
      0 (throw (ex-info "No owned membership matches the selector"
                        {:status 404 :code "identity_membership_not_found"}))
      1 (first matches)
      (throw (ex-info "Organization selector matches multiple owned memberships"
                      {:status 409 :code "identity_membership_ambiguous"})))))

(defn open!
  "Open bindings over the canonical Clio application ledger."
  [{:keys [directory]}]
  (application/open! {:directory directory :stream "knoxx/identity-bindings"
                      :projection projection
                      :reads {:identity/binding (fn [entries principal-id] (get @entries principal-id))
                              :identity/membership select-membership}
                      :writes {:identity/bind bind-coordinate!
                               :identity/bind-membership bind-membership-coordinate!}}))

(defn ^:async read!
  "Look up only an immutable verified principal ID; email is never a lookup key."
  [store principal-id]
  (await (application/read! store :identity/binding [principal-id])))

(defn ^:async bind!
  "Admit one complete binding; conflicting rebinds refuse atomically."
  [store binding]
  (await (application/write! store :identity/bind [binding])))

(defn ^:async bind-membership!
  "Append an immutable membership for the canonical principal/entity/kind/user.
   The trusted caller must verify current provider ownership before admission;
   binding facts do not establish policy activity or authorize a delegated grant."
  [store binding]
  (await (application/write! store :identity/bind-membership [binding])))

(defn ^:async read-membership!
  "Select exactly one owned binding by membership ID and/or organization ID.
   Invalid, missing and ambiguous selectors return 400/404/409 without fallback.
   Callers must still check current membership activity and delegated scope."
  [store principal-id selector]
  (await (application/read! store :identity/membership [principal-id selector])))
