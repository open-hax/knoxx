(ns knoxx.backend.infra.agent.runtime
  (:require [knoxx.backend.infra.run-event-payload :as run-payload]
            [knoxx.backend.infra.run-events :as run-events]
            [clojure.string :as str]
            [knoxx.backend.domain.realtime :refer [broadcast-ws-session!]]
            [knoxx.backend.domain.action.run-state :refer [append-run-event!]]
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

(defn- control-event
  [{:keys [conversation-id session-id run-id message kind metadata]} status error]
  (let [event-type (str (if (= kind "follow_up") "follow_up" "steer") "_" status)
        preview (if (> (count message) 240) (str (subs message 0 240) "…") message)
        payload (cond-> {:status status :preview preview :metadata (or metadata {})}
                  (= status "failed") (assoc :error (str error)))]
    (run-payload/tool-event-payload run-id conversation-id session-id event-type payload)))

(defn- ^:async publish-control-event!
  [{:keys [run-id session-id] :as control} status error]
  (let [event (control-event control status error)]
    (when run-id
      (append-run-event! run-id event)
      (await (run-events/flush! run-id)))
    (broadcast-ws-session! session-id "events" event)))

(defn- ^:async deliver-control!
  [session {:keys [conversation-id session-id run-id message kind] :as control}]
  (try
    (await (if (= kind "follow_up") (follow-up! session message) (steer! session message)))
    (await (publish-control-event! control "queued" nil))
    {:ok true :conversation_id conversation-id :session_id session-id :run_id run-id :kind kind}
    (catch :default error
      (await (publish-control-event! control "failed" error))
      (throw error))))

(defn ^:async queue-agent-control!
  "Publish and acknowledge live controls only after their audit event is durable."
  [_runtime _config {:keys [conversation-id message] :as control}]
  (cond
    (str/blank? conversation-id)
    (js/Promise.reject (js/Error. "conversation_id is required for live controls"))

    (str/blank? message)
    (js/Promise.reject (js/Error. "message is required for live controls"))

    :else
    (if-let [session (active-agent-session conversation-id)]
      (if-not (streaming? session)
        (js/Promise.reject (js/Error. "No active running turn is available for live controls"))
        (await (deliver-control! session control)))
      (js/Promise.reject (js/Error. "Conversation is not active in the agent runtime")))))
