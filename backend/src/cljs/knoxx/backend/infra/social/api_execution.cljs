(ns knoxx.backend.infra.social.api-execution
  "Run planned requests through captured shared admission and delivery guards."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.social.api-receipts :as receipts]
            [knoxx.backend.extern.bluesky-text :as post-text]
            [knoxx.backend.extern.platform-action :as platform-action]
            [knoxx.backend.law.platform-api-action :as law]))

(defn- publication-guard
  [context action]
  (when (get-in action [:effect :publication?])
    (get-in context [:delivery-guards (keyword (get-in action [:intent :platform]))])))

(defn- preflight!
  [context actions]
  (when-let [refused (first (remove law/admitted-social-write? actions))]
    (throw (ex-info "This platform write is outside the deployed social action policy"
                    {:code "platform_effect_not_allowed" :phase :preflight
                     :operation (:operation refused) :effect (get-in refused [:effect :effect])})))
  (doseq [[_ guarded] (group-by #(get-in % [:intent :platform]) (filter #(publication-guard context %) actions))]
    (when (> (count guarded) 1)
      (throw (ex-info "This delivery policy requires sequential confirmed publications; split the batch and await each receipt"
                      {:code "platform_publication_batch_not_admitted" :phase :preflight}))))
  (doseq [action actions]
    (when (and (= "app.bsky.feed.post" (get-in action [:effect :collection]))
               (not= :delete (get-in action [:effect :mutation])))
      (post-text/validate-record-text! (get-in action [:record "text"])))))

(defn- reserve-guards!
  [context actions tokens*]
  (doseq [action actions]
    (when-let [guard (publication-guard context action)]
      (let [invoke (if (= :amplify (get-in action [:effect :effect])) (:reserve-amplify! (meta guard))
                       (or (:reserve! (meta guard)) guard))]
        (when-not invoke
          (throw (ex-info "Publication effect has no matching evidence guard" {:code "platform_effect_guard_missing" :phase :preflight})))
        (swap! tokens* conj [guard (invoke (:guard-params action))])))))

(defn- release-guards!
  [tokens]
  (doseq [[guard token] (reverse tokens)]
    (when-let [release! (:release! (meta guard))] (release! token))))

(defn- failure-message
  [error outcomes]
  (str/join " " (cons (ex-message error)
                       (map platform-action/outcome-message
                            (distinct (map #(select-keys % [:status :http-status]) outcomes))))))

(defn ^:async execute!
  "Invoke only after all effect preflights. Local aborts release all reservations;
   actual/refused/unknown outcomes settle once and never trigger a retry."
  [context expected-run-id actions invoke]
  (law/assert-descriptors! actions)
  (let [port (:platform-admission context)
        write? (seq actions)
        _ (when (and write?
                     (not (and (fn? (:reserve-many! port)) (fn? (:settle-many! port))
                               (string? expected-run-id) (not (str/blank? expected-run-id)))))
            (throw (ex-info "Platform writes require an admitted actor-bound turn"
                            {:code "platform_action_context_required" :phase :preflight})))
        _ (preflight! context actions)
        reservation (when write? ((:reserve-many! port) expected-run-id (mapv :intent actions)))
        tokens* (atom [])]
    (try
      (try (reserve-guards! context actions tokens*)
           (catch :default error (throw (ex-info (ex-message error) (assoc (ex-data error) :phase :preflight) error))))
      (let [response (await (invoke)) outcomes (receipts/outcomes actions response)
            publications (receipts/publications actions response outcomes)]
        (when reservation ((:settle-many! port) reservation outcomes))
        {:response response :outcomes outcomes :publications publications})
      (catch :default error
        (let [data (or (ex-data error) {}) outcomes (receipts/failure-outcomes actions data)]
          (when reservation ((:settle-many! port) reservation outcomes))
          (when (= :preflight (:phase data)) (release-guards! @tokens*))
          (throw (ex-info (failure-message error outcomes) (assoc data :api-action-outcomes outcomes) error)))))))
