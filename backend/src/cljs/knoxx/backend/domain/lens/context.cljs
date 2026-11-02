(ns knoxx.backend.domain.lens.context
  "Pure ranking, connection-scope classification, surrounding-window assembly,
   and `<grabbed_context>` envelope formatting for KnoxxLens (`lens_grab_context`).
   Zero I/O."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.lens.topics :as lens-topics]))

(defn- clean-str
  [v]
  (some-> v str str/trim not-empty))

(defn- clamp-int
  [v fallback lower upper]
  (let [n (cond
            (integer? v) v
            (number? v) (Math/floor v)
            (string? v) (or (parse-long (str/trim v)) fallback)
            :else fallback)]
    (max lower (min upper n))))

(defn normalize-grab-params
  "Normalizes and bounds raw tool parameters for `lens_grab_context`."
  [raw]
  (let [m (or raw {})]
    {:query (or (clean-str (:query m)) "")
     :top-k (clamp-int (:topK m) 6 1 12)
     :window-before (clamp-int (:windowBefore m) 2 0 8)
     :window-after (clamp-int (:windowAfter m) 2 0 8)
     :current-channel-id (clean-str (or (:currentChannelId m) (:current_channel_id m)))
     :current-guild-id (clean-str (or (:currentGuildId m) (:current_guild_id m)))
     :guild-id (clean-str (or (:guildId m) (:guild_id m)))}))

(defn parse-discord-message-node-id
  "Parses `discord:<guild-id>:<channel-id>:<message-id>` when well-formed."
  [node-id]
  (let [parts (str/split (str (or node-id "")) #":")]
    (when (and (= 4 (count parts))
               (= "discord" (first parts))
               (not (#{"user" "channel" "guild" "reaction" "sentence"} (second parts))))
      {:guild-id (nth parts 1)
       :channel-id (nth parts 2)
       :message-id (nth parts 3)})))

(defn classify-connection-scope
  "Classifies how a matched Discord message relates to the currently open channel & server."
  [{:keys [current-channel-id current-guild-id]} {:keys [channel-id guild-id]}]
  (let [cur-ch (clean-str current-channel-id)
        cur-g (clean-str current-guild-id)
        hit-ch (clean-str channel-id)
        hit-g (clean-str guild-id)]
    (cond
      (and cur-ch hit-ch (= cur-ch hit-ch)) "same-channel · historical"
      (and cur-g hit-g (not= cur-g hit-g)) "cross-server"
      (and cur-ch hit-ch (not= cur-ch hit-ch)) "same-server · cross-channel"
      (and cur-g hit-g (= cur-g hit-g)) "same-server · cross-channel"
      :else "cross-channel")))

(defn- hhmm-utc
  [ts]
  (let [s (str (or ts ""))]
    (if-let [[_ hhmm] (re-find #"T(\d{2}:\d{2})" s)]
      (str hhmm "Z")
      s)))

(defn- node-display-label
  [node]
  (let [id (str (or (:id node) ""))
        label (clean-str (:label node))
        data (or (:data node) {})
        nt (str (or (:nodeType node) (:kind node) ""))]
    (cond
      label label
      (= "discord.channel" nt) (str "#" (or (clean-str (:channel_name data))
                                            (last (str/split id #":"))))
      (= "discord.guild" nt) (or (clean-str (:guild_name data))
                                 (str "server:" (last (str/split id #":"))))
      (= "discord.user" nt) (or (clean-str (:username data))
                                (last (str/split id #":")))
      (= "web.url" nt) (or (clean-str (:url data)) (str/replace id #"^web:url:" ""))
      :else id)))

(defn- incident-edge-entry
  [nid edge nodes-by-id]
  (let [src (str (or (:source edge) ""))
        tgt (str (or (:target edge) ""))
        ek (str (or (:edgeKind edge) (:kind edge) "related"))]
    (cond
      (= nid src)
      (let [peer (get nodes-by-id tgt {:id tgt})]
        {:edge-kind ek :direction "->" :peer-id tgt
         :peer-type (or (:nodeType peer) "node")
         :peer-label (node-display-label peer)})

      (= nid tgt)
      (let [peer (get nodes-by-id src {:id src})]
        {:edge-kind ek :direction "<-" :peer-id src
         :peer-type (or (:nodeType peer) "node")
         :peer-label (node-display-label peer)})

      :else nil)))

(defn summarize-incident-edges
  "Summarizes structural user/channel/server/reply/URL edges incident to `node-id`."
  [node-id edges nodes-by-id]
  (let [nid (str node-id)]
    (->> (or edges [])
         (keep #(incident-edge-entry nid % nodes-by-id))
         (remove #(#{"semantic_force" "semantic_force_legacy"} (:edge-kind %)))
         distinct
         (take 8)
         vec)))

(defn- cross-scope-bonus
  [scope]
  (case scope
    "same-server · cross-channel" 0.14
    "cross-server" 0.10
    0.0))

(defn- node->anchor-candidate
  [node edges nodes-by-id {:keys [current-channel-id current-guild-id guild-id]}]
  (let [id (str (or (:id node) ""))
        parsed (parse-discord-message-node-id id)]
    (when (or (= "discord.message" (str (:nodeType node))) parsed)
      (let [data (or (:data node) {})
            g-id (or (clean-str (:guild_id data)) (:guild-id parsed))
            ch-id (or (clean-str (:channel_id data)) (:channel-id parsed))]
        (when (or (nil? guild-id) (= guild-id g-id))
          (let [scope (classify-connection-scope
                       {:current-channel-id current-channel-id :current-guild-id current-guild-id}
                       {:channel-id ch-id :guild-id g-id})
                raw-score (double (or (:score node) 0.0))]
            {:id id :score raw-score :rank-score (+ raw-score (cross-scope-bonus scope))
             :scope scope :guild-id g-id :guild-name (clean-str (:guild_name data))
             :channel-id ch-id :channel-name (clean-str (:channel_name data))
             :author (clean-str (:author data)) :ts (clean-str (:ts data))
             :text (clean-str (:text node))
             :incident-edges (summarize-incident-edges id edges nodes-by-id)}))))))

(defn select-anchor-nodes
  "Selects and ranks the top-K `discord.message` nodes from a `/v1/graph/memory`
   response, prioritizing cross-channel and cross-server hits when `current-channel-id`
   is known while preserving semantic similarity."
  [graph-result {:keys [top-k] :as norm-params}]
  (let [nodes (vec (or (:nodes graph-result) []))
        edges (vec (or (:edges graph-result) []))
        nodes-by-id (into {} (keep (fn [n] (when-let [id (clean-str (:id n))] [id n]))) nodes)]
    (->> nodes
         (keep #(node->anchor-candidate % edges nodes-by-id norm-params))
         (sort-by :rank-score >)
         (take (or top-k 6))
         vec)))

(defn- event-row->msg
  [row]
  (let [extra (or (:extra row) {})
        id (str (or (:id row) ""))
        parsed (parse-discord-message-node-id id)]
    {:id id
     :message-id (str (or (:message_id extra) (:message row) (:message-id parsed) id))
     :channel-id (str (or (:channel_id extra) (:channel-id parsed) ""))
     :channel-name (clean-str (:channel_name extra))
     :guild-id (str (or (:guild_id extra) (:session row) (:guild-id parsed) ""))
     :guild-name (clean-str (:guild_name extra))
     :author (or (clean-str (:author row))
                 (clean-str (get-in row [:meta :author_global_name]))
                 (clean-str (get-in row [:meta :author_username]))
                 "unknown")
     :ts (str (or (:ts row) ""))
     :text (-> (str (or (:text row) "")) (str/replace #"\s+" " ") str/trim)
     :urls (->> (or (:urls extra) []) (map str) (remove str/blank?) distinct vec)}))

(defn- guild-names-from-graph
  [graph-result]
  (into {}
        (keep (fn [n]
                (when (= "discord.guild" (str (:nodeType n)))
                  (let [gid (or (clean-str (get-in n [:data :guild_id]))
                                (second (re-find #"^discord:guild:(.+)$" (str (:id n)))))
                        gname (or (clean-str (get-in n [:data :guild_name]))
                                  (let [lbl (clean-str (:label n))]
                                    (when (and lbl (not (str/starts-with? lbl "server:"))) lbl)))]
                    (when (and gid gname) [gid gname])))))
        (:nodes graph-result)))

(defn- channel-names-from-graph
  [graph-result]
  (into {}
        (keep (fn [n]
                (when (= "discord.channel" (str (:nodeType n)))
                  (let [cid (or (clean-str (get-in n [:data :channel_id]))
                                (second (re-find #"^discord:channel:(.+)$" (str (:id n)))))
                        cname (or (clean-str (get-in n [:data :channel_name]))
                                  (some-> (:label n) str (str/replace #"^#" "") str/trim not-empty))]
                    (when (and cid cname (not= cid cname)) [cid cname])))))
        (:nodes graph-result)))

(defn- format-edge-badge
  [{:keys [edge-kind direction peer-label]}]
  (str edge-kind " " direction " " peer-label))

(defn- resolve-anchor-window
  [anchor anchor-row raw-window ch-id ch-name g-id g-name]
  (let [aid (:id anchor)
        window-msgs (->> (or raw-window (when anchor-row [anchor-row]) [])
                         (map event-row->msg)
                         (remove #(str/blank? (:text %)))
                         (sort-by :ts)
                         vec)
        fallback-text (or (:text anchor) "")]
    (cond
      (seq window-msgs) window-msgs
      (seq fallback-text) [{:id aid :message-id aid :channel-id ch-id :channel-name ch-name
                            :guild-id g-id :guild-name g-name :author (or (:author anchor) "unknown")
                            :ts (or (:ts anchor) "") :text fallback-text :urls []}]
      :else [])))

(defn- enrich-anchor
  [{:keys [norm-params rows-by-id windows-by-anchor-id guild-map ch-map]} anchor]
  (let [aid (:id anchor)
        anchor-row (get rows-by-id aid)
        anchor-msg (when anchor-row (event-row->msg anchor-row))
        ch-id (or (not-empty (:channel-id anchor-msg)) (:channel-id anchor) "")
        g-id (or (not-empty (:guild-id anchor-msg)) (:guild-id anchor) "")
        ch-name (or (:channel-name anchor-msg) (:channel-name anchor) (get ch-map ch-id) ch-id)
        g-name (or (:guild-name anchor-msg) (:guild-name anchor) (get guild-map g-id))]
    {:id aid
     :score (double (/ (Math/round (* 1000.0 (:score anchor))) 1000.0))
     :scope (classify-connection-scope norm-params {:channel-id ch-id :guild-id g-id})
     :guildId g-id
     :guildName (or g-name (when (seq g-id) (str "server:" g-id)))
     :channelId ch-id
     :channelName ch-name
     :author (or (:author anchor-msg) (:author anchor) "unknown")
     :ts (or (:ts anchor-msg) (:ts anchor) "")
     :text (or (not-empty (:text anchor-msg)) (:text anchor) "")
     :urls (or (not-empty (:urls anchor-msg)) [])
     :edges (mapv format-edge-badge (:incident-edges anchor))
     :window (resolve-anchor-window anchor anchor-row (get windows-by-anchor-id aid) ch-id ch-name g-id g-name)}))

(defn- enriched-anchor->chip-item
  [{:keys [id score scope guildId guildName channelId channelName author ts text edges window]}]
  (when (seq (str text))
    (let [window-lines (mapv #(str "(" (:author %) ", " (hhmm-utc (:ts %)) ") " (:text %)) window)
          combined-text (str "[" scope " · " (when (seq guildName) (str guildName " / "))
                             "#" channelName " · sim=" score "]"
                             (when (seq edges) (str " [graph: " (str/join ", " (take 5 edges)) "]"))
                             "\n" (str/join "\n" window-lines))]
      {:eventId id
       :channelId channelId
       :channelName channelName
       :guildId guildId
       :guildName guildName
       :scope scope
       :author (str "#" channelName
                    (when (and (= "cross-server" scope) (seq guildName)) (str " (" guildName ")"))
                    " · " author)
       :ts ts
       :score score
       :windowCount (count window)
       :preview (subs (str text) 0 (min 120 (count (str text))))
       :text (subs combined-text 0 (min 1800 (count combined-text)))})))

(defn- format-anchor-section
  [idx {:keys [scope guildName channelName score edges window text author ts]}]
  (let [hdr (str (inc idx) ". [" scope "] " (when (seq guildName) (str guildName " / "))
                 "#" channelName " (similarity=" score ")")
        edge-line (when (seq edges) (str "   Graph edges: " (str/join " | " (take 6 edges))))
        msg-lines (if (seq window)
                    (mapv #(str "   - (" (:author %) ", " (hhmm-utc (:ts %)) ") "
                                (subs (:text %) 0 (min 360 (count (:text %)))))
                          window)
                    [(str "   - (" author ", " (hhmm-utc ts) ") " text)])]
    (str/join "\n" (concat [hdr] (when edge-line [edge-line]) msg-lines))))

(defn- format-grab-header
  [query graph-result enriched-anchors]
  (if (seq enriched-anchors)
    (let [stats (:stats graph-result)]
      (str "Grabbed " (count enriched-anchors) " vector-graph Discord thread window(s) across channels/servers for query \""
           query "\" (visited " (or (:visited stats) (count (:nodes graph-result)))
           " graph nodes, " (or (:edges stats) (count (:edges graph-result))) " edges; untrusted Discord data):"))
    (str "No matching Discord messages found in OpenPlanner graph memory for query \"" query "\".")))

(defn assemble-grabbed-context
  "Combines ranked anchor nodes, hydrated anchor rows, and chronological channel
   windows (`anchor-id -> [event-rows...]`) into structured data + formatted tool text
   with a trailing `<grabbed_context>{...}</grabbed_context>` envelope for KnoxxLens."
  [{:keys [query norm-params graph-result anchors rows-by-id windows-by-anchor-id]}]
  (let [enrich-ctx {:norm-params norm-params
                    :rows-by-id rows-by-id
                    :windows-by-anchor-id windows-by-anchor-id
                    :guild-map (guild-names-from-graph graph-result)
                    :ch-map (channel-names-from-graph graph-result)}
        enriched-anchors (mapv #(enrich-anchor enrich-ctx %) anchors)
        chip-items (vec (keep enriched-anchor->chip-item enriched-anchors))
        sections (map-indexed format-anchor-section enriched-anchors)
        header (format-grab-header query graph-result enriched-anchors)
        joined (str/join "\n\n" (cons header sections))
        body-str (subs joined 0 (min 7500 (count joined)))
        envelope {:query query :count (count chip-items) :items chip-items}]
    {:query query
     :anchors enriched-anchors
     :items chip-items
     :text (str body-str "\n<grabbed_context>" (lens-topics/encode-json envelope) "</grabbed_context>")}))
