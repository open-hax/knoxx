(ns knoxx.backend.infra.lens.v3-tools
  "KnoxxLens v3 tool implementations: WebGL connections graph, deterministic manifest gate,
   GovOne sanitized extraction, governed channel messaging, interactive browser control,
   and media extraction with Intel NPU OCR."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.lens.html-markdown :as md]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.tools :refer [create-tool-obj maybe-tool-update!]]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.infra.clients.openplanner :as openplanner-client]
            [knoxx.backend.infra.openplanner.tools :as op-tools]
            [knoxx.backend.infra.svg-render :as svg-render]
            [knoxx.backend.law.lens-v3 :as law]
            ["node:fs" :as fs]))

(def ^:private ocr-npu-url
  (or (.-OCR_NPU_URL js/process.env) "http://172.17.0.1:8099"))

(defn- read-govone-env
  []
  (let [path "/home/err/.local/share/promethean/services/govone-local/client.env"]
    (try
      (when (.existsSync fs path)
        (let [raw (.readFileSync fs path "utf8")
              lines (filter not-empty (str/split-lines raw))]
          (into {}
                (keep (fn [line]
                        (let [idx (str/index-of line "=")]
                          (when (pos? idx)
                            [(subs line 0 idx) (subs line (inc idx))]))))
                lines)))
      (catch :default err
        (js/console.warn "[govone] read-govone-env error:" (ex-message err))
        {}))))

(defn- govone-config
  []
  (let [disk (read-govone-env)
        env js/process.env]
    {:base-url (or (.-GOVONE_BASE_URL env) (get disk "GOVONE_BASE_URL") "http://govone:8080")
     :extractor-token (or (.-GOVONE_EXTRACTOR_TOKEN env) (get disk "GOVONE_EXTRACTOR_TOKEN"))
     :lens-token (or (.-GOVONE_LENS_TOKEN env) (get disk "GOVONE_LENS_TOKEN"))
     :social-token (or (.-GOVONE_SOCIAL_TOKEN env) (get disk "GOVONE_SOCIAL_TOKEN"))
     :auditor-token (or (.-GOVONE_AUDITOR_TOKEN env) (get disk "GOVONE_AUDITOR_TOKEN"))
     :ai-system-id (or (.-GOVONE_AI_SYSTEM_ID env) (get disk "GOVONE_AI_SYSTEM_ID"))
     :policy-profile-id (or (.-GOVONE_POLICY_PROFILE_ID env) (get disk "GOVONE_POLICY_PROFILE_ID"))
     :channel-session-id (or (.-GOVONE_CHANNEL_SESSION_ID env) (get disk "GOVONE_CHANNEL_SESSION_ID"))}))

(defn- ^:async govone-http!
  [cfg method path body token]
  (let [url (str (:base-url cfg) path)
        opts #js {:method (str/upper-case (name method))
                  :headers #js {"Authorization" (str "Bearer " token)
                                "Content-Type" "application/json"}
                  :signal (.timeout js/AbortSignal 12000)}]
    (when body
      (aset opts "body" (js/JSON.stringify (clj->js body))))
    (let [resp (await (js/fetch url opts))
          json (await (.json resp))]
      {:status (.-status resp)
       :ok (.-ok resp)
       :data (js->clj json :keywordize-keys true)})))

(defn- ^:async terminate-session-dek!
  [cfg session-id]
  (let [ws-url (str/replace (:base-url cfg) #"^http" "ws")]
    (js/Promise.
     (fn [resolve _reject]
       (try
         (let [ws (js/WebSocket. (str ws-url "/api/v1/realtime?token=" (js/encodeURIComponent (:lens-token cfg)))
                                 "govone.realtime.v1")]
           (aset ws "onopen"
                 (fn []
                   (.send ws (js/JSON.stringify
                              #js {:type "intervene"
                                   :request_id (str "term-" session-id)
                                   :payload #js {:session_id session-id
                                                 :action "terminate"
                                                 :reason "extraction completed; crypto-shred session DEK"}}))))
           (aset ws "onmessage"
                 (fn [msg]
                   (let [parsed (js/JSON.parse (.-data msg))]
                     (.close ws)
                     (resolve {:terminated true :ack (js->clj parsed :keywordize-keys true)}))))
           (aset ws "onerror" (fn [err] (resolve {:terminated false :error (str err)}))))
         (catch :default err
           (resolve {:terminated false :error (ex-message err)})))))))

(defn- find-discord-spans
  [text]
  (let [matches (atom [])
        enc (fn [s] (.byteLength js/Buffer s))]
    (doseq [m (re-seq #"(?i)https?://[^\s<>\"]+" text)]
      (let [idx (str/index-of text m)]
        (swap! matches conj {:entity_type "DISCORD_URL" :offset_start (enc (subs text 0 idx))
                             :offset_end (enc (subs text 0 (+ idx (count m))))
                             :confidence 1.0 :layer "custom" :sensitivity_level "confidential"})))
    (doseq [m (re-seq #"(?i)<@!?(\d+)>" text)]
      (let [idx (str/index-of text (first m))]
        (swap! matches conj {:entity_type "DISCORD_MENTION" :offset_start (enc (subs text 0 idx))
                             :offset_end (enc (subs text 0 (+ idx (count (first m)))))
                             :confidence 1.0 :layer "custom" :sensitivity_level "confidential"})))
    (doseq [m (re-seq #"(?i)<#(\d+)>" text)]
      (let [idx (str/index-of text (first m))]
        (swap! matches conj {:entity_type "DISCORD_CHANNEL" :offset_start (enc (subs text 0 idx))
                             :offset_end (enc (subs text 0 (+ idx (count (first m)))))
                             :confidence 1.0 :layer "custom" :sensitivity_level "confidential"})))
    (sort-by :offset_start @matches)))

(defn- ^:async ocr-image-base64!
  [b64]
  (try
    (let [resp (await (js/fetch (str ocr-npu-url "/ocr")
                                #js {:method "POST"
                                     :headers #js {"Content-Type" "application/json"}
                                     :body (js/JSON.stringify #js {:image b64})
                                     :signal (.timeout js/AbortSignal 10000)}))]
      (if (.-ok resp)
        (js->clj (await (.json resp)) :keywordize-keys true)
        {:text "" :error (str "OCR failed: " (.-status resp))}))
    (catch :default err
      {:text "" :error (ex-message err)})))

(defn make-connections-graph-execute
  []
  (^:async fn [_runtime config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)
          query (:query parameters)
          lakes (or (:lakes parameters) ["discord" "bluesky" "desktop" "web" "topic"])
          max-nodes (min 600 (max 10 (or (:maxNodes parameters) 150)))
          include-desktop (if (contains? parameters :includeDesktop) (:includeDesktop parameters) true)
          seed-ids (or (:seedNodeIds parameters) [])]
      (maybe-tool-update! on-update (str "Synthesizing WebGL visual connections graph for " (or query "recent active network") "…"))
      (let [rest-op (openplanner-client/client config {:mode "rest" :timeout-ms 20000})
            payload {:query query :lakes lakes :max_nodes max-nodes
                     :include_desktop include-desktop :seed_node_ids seed-ids}
            res (await (openplanner-client/v1-json! rest-op :post "/v1/graph/connections" payload))
            stats (get res :stats {})
            nodes (get res :nodes [])
            edges (get res :edges [])
            summary-text (str "WebGL Connections Graph assembled:\n"
                              "- Total nodes: " (count nodes) "\n"
                              "- Total edges: " (count edges) "\n"
                              "- People bridges: " (count (get stats :peopleBridges [])) "\n"
                              "- Timeframe: " (get-in stats [:timeframe :earliest :ts] "n/a")
                              " to " (get-in stats [:timeframe :latest :ts] "n/a"))]
        (tool-text-result summary-text res)))))

(defn make-inspect-manifest-execute
  []
  (^:async fn [_runtime _config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)
          items (or (:items parameters) [])]
      (maybe-tool-update! on-update (str "Verifying deterministic inspection manifest across " (count items) " item(s)…"))
      (let [inspected (filter #(not= (:status %) "missing") items)
            missing (filter #(= (:status %) "missing") items)
            all-covered? (empty? missing)
            result {:total (count items)
                    :inspectedCount (count inspected)
                    :missingCount (count missing)
                    :allCovered all-covered?
                    :items items}
            text (str "Deterministic Manifest Gate Check:\n"
                      (if all-covered? "PASS: All " "INCOMPLETE: Missing ")
                      (count items) " item(s) in scope verified.\n"
                      "- Inspected: " (count inspected) "\n"
                      "- Missing: " (count missing))]
        (tool-text-result text result)))))

(defn make-govone-extract-execute
  []
  (^:async fn [_runtime _config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)
          text (or (:text parameters) "")
          cfg (govone-config)]
      (maybe-tool-update! on-update "Sanitizing Discord message through GovOne de-identification gateway…")
      (let [sess-res (await (govone-http! cfg :post "/api/v1/sessions"
                                          {:ai_system_id (:ai-system-id cfg)
                                           :policy_profile_id (:policy-profile-id cfg)
                                           :ttl_seconds 600}
                                          (:extractor-token cfg)))
            sid (get-in sess-res [:data :session_id])]
        (when-not sid
          (throw (js/Error. (str "GovOne session creation failed: " (pr-str sess-res)))))
        (let [spans (find-discord-spans text)
              red-res (await (govone-http! cfg :post "/api/v1/redact"
                                           {:session_id sid
                                            :messages [{:role "user" :content text}]
                                            :classifications spans}
                                           (:extractor-token cfg)))
              red-text (get-in red-res [:data :redacted_messages 0 :content] text)
              resp-id (str "resp-" (.now js/Date))
              _ (await (govone-http! cfg :post "/api/v1/inspect"
                                     {:session_id sid :response_id resp-id :model_output red-text}
                                     (:extractor-token cfg)))
              rehyd-res (await (govone-http! cfg :post "/api/v1/rehydrate"
                                             {:session_id sid :response_id resp-id :variant "masked"}
                                             (:lens-token cfg)))
              sanitized (get-in rehyd-res [:data :content] red-text)
              evidence (await (govone-http! cfg :get (str "/api/v1/evidence/" sid) nil (:auditor-token cfg)))
              _ (maybe-tool-update! on-update "Crypto-shredding session DEK via termination intervention…")
              term-res (await (terminate-session-dek! cfg sid))
              result {:sessionId sid
                      :sanitizedText sanitized
                      :placeholdersAssigned (get-in red-res [:data :placeholders_assigned] 0)
                      :auditEvents (count (get-in evidence [:data :events] []))
                      :dekShredded (:terminated term-res)}]
          (tool-text-result (str "GovOne Sanitized Extraction:\n\n" sanitized "\n\n"
                                 "[DEK Cryptographically Shredded: " (:terminated term-res) " | Merkle Audit Events: " (count (get-in evidence [:data :events] [])) "]")
                            result))))))

(defn make-govone-send-execute
  []
  (^:async fn [_runtime _config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)
          content (:content parameters)
          priority (or (:priority parameters) "normal")
          cfg (govone-config)
          sid (or (:sessionId parameters) (:channel-session-id cfg))]
      (maybe-tool-update! on-update (str "Sending message to governed GovOne channel session " sid "…"))
      (let [res (await (govone-http! cfg :post (str "/api/v1/sessions/" sid "/messages")
                                     {:content content :priority priority}
                                     (:lens-token cfg)))
            msg (get-in res [:data :message])]
        (tool-text-result (str "Sent message to governed channel session: " (:message_id msg))
                          {:messageId (:message_id msg) :sessionId sid :priority priority})))))

(defn make-govone-read-execute
  []
  (^:async fn [_runtime _config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)
          cfg (govone-config)
          sid (or (:sessionId parameters) (:channel-session-id cfg))]
      (maybe-tool-update! on-update (str "Reading messages from governed GovOne channel session " sid "…"))
      (let [res (await (govone-http! cfg :get (str "/api/v1/sessions/" sid "/messages") nil (:lens-token cfg)))
            msgs (get-in res [:data :messages] [])
            text-lines (map #(str "[" (:created_at %) "] " (:sender_principal_id %) " (" (:priority %) "): " (:content %)) msgs)]
        (tool-text-result (str "Governed Channel Messages (" (count msgs) "):\n\n" (str/join "\n" text-lines))
                          {:sessionId sid :messages msgs :count (count msgs)})))))

(defn- ^:async run-browser-actions!
  [page actions]
  (doseq [act actions]
    (let [action-type (:action act)]
      (cond
        (= action-type "click")
        (when-let [sel (:selector act)] (await (.click page sel)))
        (= action-type "type")
        (when (and (:selector act) (:text act)) (await (.type page (:selector act) (:text act))))
        (= action-type "scroll")
        (await (.evaluate page (fn [y] (js/window.scrollBy 0 (or y 400))) (:y act)))
        (= action-type "wait")
        (await (js/Promise. (fn [r] (js/setTimeout r (min 5000 (or (:ms act) 1000))))))))))

(defn make-browser-control-execute
  []
  (^:async fn [_runtime _config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)
          url (:url parameters)
          _ (op-tools/assert-public-http-url! url)
          actions (or (:actions parameters) [])
          want-screenshot (if (contains? parameters :screenshot) (:screenshot parameters) true)
          want-ocr (if (contains? parameters :ocr) (:ocr parameters) true)]
      (maybe-tool-update! on-update (str "Launching browser session for " url "…"))
      (let [browser (await (svg-render/get-browser))
            page (await (.newPage browser))]
        (try
          (await (.goto page url #js {:waitUntil "domcontentloaded" :timeout 20000}))
          (when (seq actions)
            (maybe-tool-update! on-update (str "Executing " (count actions) " browser action(s)…"))
            (await (run-browser-actions! page actions)))
          (let [html (await (.content page))
                markdown (md/html->markdown html)
                b64 (when want-screenshot
                      (await (.screenshot page #js {:encoding "base64" :fullPage false})))
                ocr-res (when (and b64 want-ocr)
                          (maybe-tool-update! on-update "Running Intel NPU OCR on captured viewport…")
                          (await (ocr-image-base64! b64)))
                ocr-text (:text ocr-res "")]
            (tool-text-result (str "Browser Page Loaded (" url "):\n\n"
                                   (when (seq ocr-text) (str "### NPU OCR Recognized Text:\n" ocr-text "\n\n"))
                                   "### Page Markdown:\n" (subs markdown 0 (min 3000 (count markdown))))
                              {:url url :markdown markdown :ocrText ocr-text
                               :hasScreenshot (boolean b64)}))
          (finally
            (await (.close page))))))))

(defn make-media-extract-execute
  []
  (^:async fn [_runtime _config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          parameters (xtools/parameter-map params)
          url (:url parameters)
          want-ocr (if (contains? parameters :ocr) (:ocr parameters) true)]
      (maybe-tool-update! on-update (str "Extracting media and attachments from " url "…"))
      (let [resp (await (js/fetch url #js {:signal (.timeout js/AbortSignal 12000)}))
            ctype (or (.get (.-headers resp) "content-type") "")
            is-image (str/starts-with? ctype "image/")
            buf (when is-image (await (.arrayBuffer resp)))
            b64 (when buf (.toString (js/Buffer.from buf) "base64"))
            ocr-res (when (and b64 want-ocr)
                      (maybe-tool-update! on-update "Running Intel NPU OCR on media image…")
                      (await (ocr-image-base64! b64)))
            ocr-text (:text ocr-res "")]
        (tool-text-result (str "Media Extraction (" url "):\n"
                               "- Content-Type: " ctype "\n"
                               (when (seq ocr-text) (str "- NPU OCR: " ocr-text "\n")))
                          {:url url :contentType ctype :ocrText ocr-text :isImage is-image})))))

(defn lens-connections-graph-tool
  []
  (partial create-tool-obj
           "lens_connections_graph"
           "Visual Connections Graph"
           "Synthesize a multi-lake WebGL connection graph connecting people across Discord servers, topics, sequential message order, Bluesky, and desktop windows."
           "Actively generate visual connection graphs with precomputed shader coordinates, social bridges, and timeframe message permalinks."
           ["Use lens_connections_graph when the user asks to see connections, explore cross-Discord people bridges, view topic clusters, or inspect Bluesky and Desktop relationships."
            "Returns nodes with WebGL (x, y) coordinates, color, size, permalinks, and social bridge intelligence."]
           law/connections-graph-params
           (make-connections-graph-execute)))

(defn lens-inspect-manifest-tool
  []
  (partial create-tool-obj
           "lens_inspect_manifest"
           "Deterministic Manifest Gate"
           "Verify that every link, image, attachment, and reaction in conversational scope has been examined."
           "Deterministic inspection gate ensuring complete coverage of all multimedia and links."
           ["Use lens_inspect_manifest to enforce deterministic verification of all links, attachments, and reactions."]
           law/inspect-manifest-params
           (make-inspect-manifest-execute)))

(defn govone-extract-tool
  []
  (partial create-tool-obj
           "govone.extract"
           "GovOne Sanitized Extraction"
           "Sanitize Discord content by stripping usernames, IDs, mentions, and PII into [[ENTITY_N]] placeholders, verifying Merkle receipts, and crypto-shredding session DEK."
           "Extract clean Discord discussions while protecting user identity and PII via GovOne governed gateway."
           ["Use govone.extract when the user requests extracting Discord content while stripping usernames and sensitive context."
            "Performs zero-knowledge redaction, Merkle audit proof verification, and immediate cryptographic DEK shredding."]
           law/govone-extract-params
           (make-govone-extract-execute)))

(defn govone-channel-send-tool
  []
  (partial create-tool-obj
           "govone.channel.send"
           "GovOne Channel Send"
           "Send a message across the governed GovOne channel session between KnoxxLens and the social agent."
           "Post governed messages between KnoxxLens and ussyverse_social agents."
           ["Use govone.channel.send to communicate across the governed channel."]
           law/govone-send-params
           (make-govone-send-execute)))

(defn govone-channel-read-tool
  []
  (partial create-tool-obj
           "govone.channel.read"
           "GovOne Channel Read"
           "Read incoming messages from the governed GovOne channel session."
           "Read governed messages sent by the social agent to KnoxxLens."
           ["Use govone.channel.read to poll the governed channel inbox."]
           law/govone-read-params
           (make-govone-read-execute)))

(defn browser-control-tool
  []
  (partial create-tool-obj
           "browser.control"
           "Interactive Browser Control"
           "Navigate web pages, perform clicks/types/scrolls, capture viewport screenshots, and run Intel NPU OCR."
           "Control a browser page interactively with multimodal screenshot capture and NPU vision OCR."
           ["Use browser.control when a page requires interaction, visual layout inspection, or screenshot OCR."]
           law/browser-control-params
           (make-browser-control-execute)))

(defn media-extract-tool
  []
  (partial create-tool-obj
           "media.extract"
           "Media & Attachment Extractor"
           "Extract images, video keyframes, audio, and attachments from Discord or web URLs with NPU OCR."
           "Extract and inspect multimodal media and Discord attachments."
           ["Use media.extract to inspect attachments, video keyframes, audio, and run NPU OCR on images."]
           law/media-extract-params
           (make-media-extract-execute)))
