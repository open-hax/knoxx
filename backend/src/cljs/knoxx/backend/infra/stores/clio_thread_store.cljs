(ns knoxx.backend.infra.stores.clio-thread-store
  "Canonical conversation persistence, separate from individual runs and cache state."
  (:require [knoxx.backend.extern.thread-store :as host]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.stores.thread-store-reference :as reference]
            [knoxx.backend.infra.system-instance :as instance]
            [knoxx.backend.law.thread-store :as law]
            [knoxx.backend.shape.thread-store :as protocol]))

(defn- ^:async mutate!
  [{:keys [engine directory now-ms instance-id]} operation]
  (let [thread-id (:thread-id operation)]
    (law/assert-valid! :thread/id law/NonBlank thread-id)
    (await (host/with-lock! (str directory "/thread-store.lock")
            (^:async fn []
              (let [stamped (assoc operation :stamp (host/stamp thread-id (now-ms) instance-id))]
                (await (clio/write! engine :thread/admit [stamped]))
                (if (= :delete (:kind operation)) true
                  (await (clio/read! engine :thread/read [thread-id (now-ms)])))))))))

(defrecord ClioThreadStore [engine directory now-ms instance-id]
  protocol/IThreadStore
  (read-thread [_ thread-id] (clio/read! engine :thread/read [thread-id (now-ms)]))
  (conversation-thread [_ conversation-id] (clio/read! engine :thread/conversation [conversation-id (now-ms)]))
  (put-thread! [store thread] (mutate! store {:kind :put :thread-id (:session_id thread) :thread thread}))
  (patch-thread! [store thread-id patch] (mutate! store {:kind :patch :thread-id thread-id :patch patch}))
  (rewind-thread! [store thread-id turns] (mutate! store {:kind :rewind :thread-id thread-id :turns turns}))
  (delete-thread! [store thread-id] (mutate! store {:kind :delete :thread-id thread-id}))
  (active-threads [_] (clio/read! engine :thread/active [(now-ms)])))

(defn open!
  "Open an explicit provider; invalid options must fail before files are created."
  [{:keys [directory now-ms instance-id] :or {now-ms host/now-ms} :as options}]
  (when-not (and (map? options) (fn? now-ms))
    (throw (ex-info "Invalid thread provider options" {:status 400 :code "thread_store_invalid_options"})))
  (let [instance-id (or instance-id (instance/current-id))]
    (law/assert-valid! :thread/instance law/NonBlank instance-id)
    (law/assert-valid! :thread/clock law/Milliseconds (now-ms))
    (let [engine (clio/open! {:directory directory :stream "knoxx/threads" :projection reference/projection
                             :reads reference/reads :writes {:thread/admit reference/admit!}})]
      (->ClioThreadStore engine (:directory engine) now-ms instance-id))))
