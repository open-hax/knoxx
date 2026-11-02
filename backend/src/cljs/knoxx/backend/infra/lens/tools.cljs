(ns knoxx.backend.infra.lens.tools
  "KnoxxLens companion tools (`lens_refine_topics` and `lens_grab_context`)."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.lens.context :as lens-context]
            [knoxx.backend.domain.lens.topics :as lens-topics]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.tools :refer [create-tool-obj maybe-tool-update!]]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.infra.auth.authz :refer [ctx-tool-allowed?]]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.lens.v3-tools :as v3]
            [knoxx.backend.law.lens-context :as lens-context-law]
            [knoxx.backend.law.lens-topics :as lens-topics-law]))

(def ^:private event-projection
  {:id 1
   :ts 1
   :author 1
   :session 1
   :message 1
   :text 1
   :meta 1
   :extra.channel_id 1
   :extra.channel_name 1
   :extra.guild_id 1
   :extra.guild_name 1
   :extra.message_id 1
   :extra.urls 1})

(defn make-refine-topics-execute
  "Create execute handler for `lens_refine_topics`."
  []
  (^:async fn [_runtime _config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)]
      (maybe-tool-update! on-update "Refining Discord channel noun & entity topics…")
      (let [normalized (lens-topics/normalize-refinement parameters)]
        (tool-text-result (lens-topics/format-refinement-result-text normalized)
                          normalized)))))

(defn- ^:async query-window-half!
  [rest-op channel-id ts-filter sort-dir limit]
  (if (pos? limit)
    (:rows (await (openplanner-client/mongo-query!
                   rest-op
                   {:collection "events"
                    :filter {:kind "discord.message"
                             :extra.channel_id (str channel-id)
                             :ts ts-filter}
                    :projection event-projection
                    :sort {:ts sort-dir}
                    :limit limit})))
    []))

(defn- dedupe-window-rows
  [fallback-id rows]
  (reduce (fn [acc row]
            (let [rid (str (or (:id row) fallback-id))]
              (if (some #(= rid (str (:id %))) acc)
                acc
                (conj acc row))))
          []
          rows))

(defn- ^:async fetch-channel-window!
  [rest-op {:keys [id channel-id ts]} window-before window-after]
  (if (or (str/blank? (str channel-id))
          (str/blank? (str ts))
          (and (zero? window-before) (zero? window-after)))
    []
    (try
      (let [before-rows (await (query-window-half! rest-op channel-id {:$lte (str ts)} -1 (inc window-before)))
            after-rows (await (query-window-half! rest-op channel-id {:$gt (str ts)} 1 window-after))]
        (dedupe-window-rows id (concat (reverse before-rows) after-rows)))
      (catch :default err
        (js/console.warn "[lens_grab_context] window query failed:" (ex-message err))
        []))))

(defn- ^:async hydrate-anchor-rows!
  [rest-op anchors]
  (let [anchor-ids (mapv :id anchors)]
    (if (seq anchor-ids)
      (let [res (await (openplanner-client/mongo-query!
                        rest-op
                        {:collection "events"
                         :filter {:id {:$in anchor-ids}}
                         :projection event-projection
                         :limit (max 12 (* 2 (count anchor-ids)))}))]
        (into {}
              (keep (fn [r]
                      (when-let [rid (some-> (:id r) str str/trim not-empty)]
                        [rid r])))
              (:rows res)))
      {})))

(defn- ^:async fetch-windows-by-anchor!
  [rest-op anchors rows-by-id window-before window-after]
  (loop [remaining (vec (take 4 anchors))
         acc {}]
    (if (empty? remaining)
      acc
      (let [anchor (first remaining)
            aid (:id anchor)
            hydrated (get rows-by-id aid)
            eff-anchor {:id aid
                        :channel-id (or (get-in hydrated [:extra :channel_id]) (:channel-id anchor))
                        :ts (or (:ts hydrated) (:ts anchor))}
            win (await (fetch-channel-window! rest-op eff-anchor window-before window-after))]
        (recur (subvec remaining 1) (assoc acc aid win))))))

(defn make-grab-context-execute
  "Create execute handler for `lens_grab_context`."
  []
  (^:async fn [_runtime config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)
          {:keys [query top-k window-before window-after] :as norm}
          (lens-context/normalize-grab-params parameters)]
      (when (str/blank? query)
        (throw (js/Error. "query is required for lens_grab_context")))
      (maybe-tool-update! on-update (str "Searching cross-channel & cross-server Discord graph for \"" query "\"…"))
      (let [rest-op (openplanner-client/client config {:mode "rest" :timeout-ms 15000})
            graph-result (await (openplanner-client/graph-memory!
                                 rest-op
                                 {:q query :k (max 8 (* top-k 2)) :lakes ["discord"]
                                  :maxNodes 48 :maxCandidates 900 :includeText true}))
            anchors (lens-context/select-anchor-nodes graph-result norm)
            rows-by-id (await (hydrate-anchor-rows! rest-op anchors))
            _ (when (seq anchors)
                (maybe-tool-update! on-update (str "Grabbing surrounding channel context around " (min 4 (count anchors)) " matched message(s)…")))
            windows-by-anchor-id (await (fetch-windows-by-anchor! rest-op anchors rows-by-id window-before window-after))
            assembled (lens-context/assemble-grabbed-context
                       {:query query :norm-params norm :graph-result graph-result
                        :anchors anchors :rows-by-id rows-by-id :windows-by-anchor-id windows-by-anchor-id})]
        (tool-text-result (:text assembled) (dissoc assembled :text))))))

(defn lens-refine-topics-tool
  "Construct tool definition factory for `lens_refine_topics`."
  []
  (partial create-tool-obj
           "lens_refine_topics"
           "Refine Channel Topics"
           "Curate and publish crisp noun and proper-noun topics for the active KnoxxLens Discord channel map and suggested actions."
           "Refine noisy heuristic channel terms into ranked nouns and proper nouns so KnoxxLens updates Channel Map chips and starter actions."
           ["Use lens_refine_topics when channel heuristic topics look noisy, incomplete, or miss named entities/concepts in the buffer."
            "Include ONLY nouns, noun compounds, and proper nouns (projects, tools, people, organizations, technical concepts) — never verbs, prepositions, or adjectives like 'found', 'using', 'via', 'work', or 'fascinating'."
            "List discarded non-topic words in droppedTerms when cleaning up noisy heuristic candidates."]
           lens-topics-law/refine-topics-params
           (make-refine-topics-execute)))

(defn lens-grab-context-tool
  "Construct tool definition factory for `lens_grab_context`."
  []
  (partial create-tool-obj
           "lens_grab_context"
           "Grab Cross-Channel Graph Context"
           "Run a vector + structural graph query across all Discord servers, channels, users, and shared links, grab surrounding message windows into context, and pin them in KnoxxLens."
           "Intentionally expand context outside the open channel whenever a topic, person, project, reply reference, or shared link connects across channels or servers."
           ["Use lens_grab_context proactively whenever a conversation references another channel, another server, an ongoing project/topic, a recurring person, or thin context where related messages likely exist elsewhere."
            "Searches vector embeddings + user/channel/server/URL graph edges across all ingested Discord servers and channels by default, labeling same-server cross-channel vs cross-server connections."
            "Pass currentChannelId and currentGuildId from <discord_context> when available so cross-channel and cross-server connections are prioritized and labeled accurately."
            "Automatically attaches removable cross-channel context chips into the user's KnoxxLens drawer for follow-up turns."]
           lens-context-law/grab-context-params
           (make-grab-context-execute)))

(defn- append-core-tools
  [tools runtime config auth-context]
  (cond-> tools
    (or (nil? auth-context) (ctx-tool-allowed? auth-context "lens_refine_topics"))
    (conj ((lens-refine-topics-tool) runtime config))

    (or (nil? auth-context) (ctx-tool-allowed? auth-context "lens_grab_context"))
    (conj ((lens-grab-context-tool) runtime config))

    (or (nil? auth-context) (ctx-tool-allowed? auth-context "lens_connections_graph"))
    (conj ((v3/lens-connections-graph-tool) runtime config))

    (or (nil? auth-context) (ctx-tool-allowed? auth-context "lens_inspect_manifest"))
    (conj ((v3/lens-inspect-manifest-tool) runtime config))))

(defn- append-v3-tools
  [tools runtime config auth-context]
  (cond-> tools
    (or (nil? auth-context) (ctx-tool-allowed? auth-context "govone.extract"))
    (conj ((v3/govone-extract-tool) runtime config))

    (or (nil? auth-context) (ctx-tool-allowed? auth-context "govone.channel.send"))
    (conj ((v3/govone-channel-send-tool) runtime config))

    (or (nil? auth-context) (ctx-tool-allowed? auth-context "govone.channel.read"))
    (conj ((v3/govone-channel-read-tool) runtime config))

    (or (nil? auth-context) (ctx-tool-allowed? auth-context "browser.control"))
    (conj ((v3/browser-control-tool) runtime config))

    (or (nil? auth-context) (ctx-tool-allowed? auth-context "media.extract"))
    (conj ((v3/media-extract-tool) runtime config))))

(defn create-tools
  "Create authorized KnoxxLens tools."
  ([runtime config]
   (create-tools runtime config nil))
  ([runtime config auth-context]
   (-> []
       (append-core-tools runtime config auth-context)
       (append-v3-tools runtime config auth-context))))
