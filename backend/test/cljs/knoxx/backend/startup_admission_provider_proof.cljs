(ns knoxx.backend.startup-admission-provider-proof
  "Portable startup ownership assertions shared by native and ledger providers."
  (:require [cljs.test :refer [is]]
            [knoxx.backend.domain.thread-store :as thread-domain]
            [knoxx.backend.shape.session-persistence :as runs]
            [knoxx.backend.shape.startup-admission :as startup]
            [knoxx.backend.shape.thread-store :as threads]))

(def at "2026-09-20T12:00:00.000Z")

(defn record
  "Produce complete test coordinates; distinct attempts always receive distinct tokens."
  [prefix]
  {:run_id (str prefix "-run") :session_id (str prefix "-session")
   :conversation_id (str prefix "-conversation") :org_id "startup-org" :user_id "startup-user"
   :startup_token (str prefix "-token") :status "running" :has_active_stream false
   :created_at at :updated_at at :messages [{:role "user" :content "Original message"}]})

(defn ^:async outcome!
  "Observe expected rejection without swallowing its actual classified status."
  [operation]
  (try {:value (await operation)} (catch :default error {:error (ex-data error)})))

(defn event
  "Bind one immutable fact to the attempt's real run/session/conversation."
  [attempt]
  (assoc (select-keys attempt [:run_id :session_id :conversation_id])
         :event_id (str (:run_id attempt) "-event") :at at :type "run_started" :data {:text "Accepted"}))

(defn- ^:async check-historical-run! [provider prefix]
  (let [attempt (record (str prefix "-legacy")) id (:run_id attempt)]
    (await (runs/put-run! provider (dissoc attempt :startup_token)))
    (let [view (await (startup/startup-view provider id)) before (await (runs/get-run provider id))]
      (is (= 409 (get-in (await (outcome! (startup/claim-startup! provider attempt view))) [:error :status])))
      (await (startup/settle-startup! provider attempt view))
      (is (= before (await (runs/get-run provider id))) "A historical run without an attempt token is never compensation-owned"))))

(defn ^:async check-run-ownership!
  "Refuse foreign existing IDs, preserve private FIFO reservations and accepted facts."
  [provider prefix]
  (let [attempt (record prefix) id (:run_id attempt)
        empty-view (await (startup/startup-view provider id))]
    (await (startup/claim-startup! provider attempt empty-view))
    (let [accepted (await (runs/append-event! provider (event attempt)))
          current (await (runs/get-run provider id))
          foreign (assoc attempt :startup_token "different-attempt")
          foreign-view (await (startup/startup-view provider id))]
      (is (= 409 (get-in (await (outcome! (startup/claim-startup! provider foreign foreign-view))) [:error :status])))
      (await (startup/settle-startup! provider foreign foreign-view))
      (is (= current (await (runs/get-run provider id))) "Rejected invocation cannot fail the existing run")
      (await (startup/settle-startup! provider attempt empty-view))
      (is (= "failed" (:status (await (runs/get-run provider id)))))
      (is (= [accepted] (await (runs/events-since provider id nil))))
      (is (= accepted (await (runs/append-event! provider (event attempt)))))))
  (let [reserved (assoc (record (str prefix "-fifo")) :status "queued")
        id (:run_id reserved)
        empty-view (await (startup/startup-view provider id))]
    (await (startup/claim-startup! provider reserved empty-view))
    (let [view (await (startup/startup-view provider id))]
      (await (startup/claim-startup! provider (assoc reserved :status "running") view))
      (is (= "running" (:status (await (runs/get-run provider id)))))
      (await (startup/settle-startup! provider reserved view))
      (is (= "failed" (:status (await (runs/get-run provider id)))))))
  (await (check-historical-run! provider prefix)))

(defn ^:async check-thread-retry!
  "A failed attempt frees the session; a subsequent failure cannot inherit stale success."
  [provider prefix]
  (let [first-attempt (record prefix) id (:session_id first-attempt)
        first-view (await (startup/startup-view provider id))]
    (await (startup/claim-startup! provider first-attempt first-view))
    (await (startup/settle-startup! provider first-attempt first-view))
    (is (:can-send (thread-domain/session-can-send? (await (threads/read-thread provider id)))))
    (let [second-attempt (assoc first-attempt :run_id (str prefix "-second-run") :startup_token "second-token")
          second-view (await (startup/startup-view provider id))]
      (await (startup/claim-startup! provider second-attempt second-view))
      (let [current (await (threads/read-thread provider id))]
        (is (= "running" (:status current)))
        (is (nil? (:startup_failure current)) "A prior failed attempt is not this attempt's settlement receipt"))
      (await (startup/settle-startup! provider second-attempt second-view))
      (is (= "failed" (:status (await (threads/read-thread provider id)))))
      (is (:can-send (thread-domain/session-can-send? (await (threads/read-thread provider id))))))))

(defn ^:async check-thread-replacement!
  "A compensator never touches a newer token, even with identical public coordinates."
  [provider prefix]
  (let [attempt (record prefix) id (:session_id attempt)
        view (await (startup/startup-view provider id))]
    (await (startup/claim-startup! provider attempt view))
    (await (threads/put-thread! provider (assoc attempt :startup_token (str (:startup_token attempt) "-successor"))))
    (let [replacement (await (threads/read-thread provider id))]
      (await (startup/settle-startup! provider attempt view))
      (is (= replacement (await (threads/read-thread provider id)))))))
