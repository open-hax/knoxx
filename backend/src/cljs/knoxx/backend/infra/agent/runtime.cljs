(ns knoxx.backend.infra.agent.runtime
  (:require [clojure.string :as str]
            [knoxx.backend.domain.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.domain.action.run-state :refer [tool-event-payload append-run-event!]]
            [knoxx.backend.domain.extension-runtime :as ext-runtime]
            [knoxx.backend.infra.agent.session :refer [active-agent-session]]
            [knoxx.backend.shape.agent :refer [streaming? follow-up! steer!]]
            ["node:path" :as path]))

(ext-runtime/init!)

(defonce eta-mu-runtime* (atom nil))
(defonce eta-mu-module-promise* (atom nil))

(defn- configured-extra-root-records
  [node-path config]
  (let [music-root (some-> (:music-library-root config) str str/trim not-empty)
        extra-roots (->> (or (:extra-workspace-roots config) [])
                         (map (fn [raw]
                                (some-> raw str str/trim not-empty)))
                         (remove nil?))]
    (->> (concat
          (when music-root
            [{:alias "Music"
              :root (.resolve node-path music-root)}])
          (map (fn [raw-root]
                 {:alias nil
                  :root (.resolve node-path raw-root)})
               extra-roots))
         (reduce (fn [acc entry]
                   (if (some #(= (:root %) (:root entry)) acc)
                     acc
                     (conj acc entry)))
                 [])
         vec)))

(defn- allowed-root-records
  [node-path config]
  (vec (cons {:alias nil
              :root (.resolve node-path (:workspace-root config))}
             (configured-extra-root-records node-path config))))

(defn- root-relative-path
  [node-path root candidate]
  (let [rel (.relative node-path root candidate)]
    (when-not (or (str/starts-with? rel "..")
                  (.isAbsolute node-path rel))
      rel)))

(defn resolve-workspace-path
  [_runtime config raw-path]
  (let [node-path path
        requested (some-> raw-path str str/trim not-empty)
        roots (allowed-root-records node-path config)
        music-root (some #(when (= "Music" (:alias %)) %) roots)
        candidate (cond
                    (.isAbsolute node-path (or requested ""))
                    (.resolve node-path requested)

                    (and requested
                         music-root
                         (or (= requested "Music")
                             (str/starts-with? requested "Music/")))
                    (let [suffix (subs requested (min (count requested) (count "Music/")))]
                      (.resolve node-path (:root music-root) suffix))

                    :else
                    (.resolve node-path (:workspace-root config) (or requested "")))
        matched-root (some (fn [root-record]
                             (when (root-relative-path node-path (:root root-record) candidate)
                               root-record))
                           roots)]
    (when-not matched-root
      (throw (js/Error. "Path escapes allowed workspace roots")))
    candidate))

(defn queue-agent-control!
  [_runtime _config {:keys [conversation-id session-id run-id message kind metadata]}]
  (cond
    (str/blank? conversation-id)
    (js/Promise.reject (js/Error. "conversation_id is required for live controls"))

    (str/blank? message)
    (js/Promise.reject (js/Error. "message is required for live controls"))

    :else
    (if-let [session (active-agent-session conversation-id)]
      (if-not (streaming? session)
        (js/Promise.reject (js/Error. "No active running turn is available for live controls"))
        (let [preview (if (> (count message) 240)
                        (str (subs message 0 240) "…")
                        message)
              event-type (if (= kind "follow_up") "follow_up_queued" "steer_queued")
              failure-type (if (= kind "follow_up") "follow_up_failed" "steer_failed")
              metadata (or metadata {})
              invoke (if (= kind "follow_up")
                       #(follow-up! session message)
                       #(steer! session message))]
          (-> (invoke)
              (.then (fn []
                       (let [event (tool-event-payload run-id conversation-id session-id event-type
                                                       {:status "queued"
                                                        :preview preview
                                                        :metadata metadata})]
                         (when run-id
                           (append-run-event! run-id event))
                         (broadcast-ws-session! session-id "events" event)
                         {:ok true
                          :conversation_id conversation-id
                          :session_id session-id
                          :run_id run-id
                          :kind kind})))
              (.catch (fn [err]
                        (let [event (tool-event-payload run-id conversation-id session-id failure-type
                                                        {:status "failed"
                                                         :error (str err)
                                                         :preview preview
                                                         :metadata metadata})]
                          (when run-id
                            (append-run-event! run-id event))
                          (broadcast-ws-session! session-id "events" event))
                        (throw err))))))
      (js/Promise.reject (js/Error. "Conversation is not active in the agent runtime")))))
