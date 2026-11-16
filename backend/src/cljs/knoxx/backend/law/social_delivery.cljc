(ns knoxx.backend.law.social-delivery
  "Evidence obligations for autonomous source-linked social deliveries.
   SPDX-License-Identifier: GPL-3.0-or-later"
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def Obligations
  "Trusted resource policy, separate from evidence supplied by tool receipts."
  [:map {:closed true}
   [:channel-id [:string {:min 1}]]
   [:bluesky-publish? {:optional true} :boolean]
   [:bluesky-thread-max-posts {:optional true} [:int {:min 1 :max 5}]]
   [:bluesky-original-author {:optional true} [:string {:min 1}]]
   [:self-identities [:vector {:min 1} [:string {:min 1}]]]])

(defn- named
  [receipts names]
  (filter #(and (= "completed" (:status %)) (not (:is_error %))
                (contains? names (:tool_name %))) receipts))

(defn urls
  "Extract literal HTTP source references without resolving external text."
  [text]
  (->> (re-seq #"https?://[^\s<>\[\]{}\"']+" (or text ""))
       (map #(str/replace % #"[).,;!?]+$" "")) distinct vec))

(defn- thread-target
  [details]
  (if (contains? details :target) (:target details) (:root details)))

(defn- outside-posts
  [receipts identities]
  (->> (named receipts #{"bluesky_author_feed" "bluesky_thread"})
       (mapcat #(let [d (get-in % [:result :details])]
                  (or (:results d) (when-let [target (thread-target d)] [target]))))
       (filter #(and (not= false (:available %))
                     (seq (:handle %)) (str/starts-with? (or (:uri %) "") "at://did:")
                     (not-any? (fn [id] (or (= id (:handle %))
                                            (str/includes? (:uri %) id))) identities)))))

(defn- media-urls
  [receipt]
  (let [d (get-in receipt [:result :details])
        parts (or (:content_parts d) (:contentParts d))]
    (when (or (seq (:observation d))
              (some #(and (contains? #{"image" "audio" "video"} (:type %))
                           (or (seq (:data %)) (seq (:url %)))) parts))
      (mapcat urls [(get-in receipt [:input :source]) (:source d)
                    (get-in receipt [:input :url])]))))

(defn- inspected-urls
  [receipts outside]
  (let [browser (->> (named receipts #{"browser_read"})
                     (filter #(true? (get-in % [:result :details :metadata :sourceReadable])))
                     (mapcat #(urls (get-in % [:input :url]))))
        media (mapcat media-urls (named receipts #{"multimodal_upload" "multimodal_inspect"}))
        posts (mapcat (fn [{:keys [url uri handle]}]
                        (or (seq (urls url))
                            (when (and (seq handle) (seq uri))
                              [(str "https://bsky.app/profile/" handle "/post/" (last (str/split uri #"/")))]))) outside)]
    {:source-urls (vec (distinct (concat browser media posts)))
     :browser-or-media? (boolean (seq (concat browser media)))}))

(defn- home-history
  [policy receipts]
  (last (filter #(and (= (:channel-id policy) (get-in % [:input :channel_id]))
                      (>= (or (get-in % [:input :limit]) 0) 100))
                (named receipts #{"discord_read" "discord_channel_messages"}))))

(defn- searched-sources
  [receipts]
  (->> (named receipts #{"memory_search"})
       (filter #(= "knoxx.tool_receipt" (get-in % [:input :kind])))
       (map #(get-in % [:input :query])) set))

(defn delivery-evidence
  "Check obligations and name literal inspected sources for a bounded repair.
   Candidates are evidence, not instructions or permission to publish."
  [policy receipts params]
  (when-not (m/validate Obligations policy)
    (throw (ex-info "Invalid social delivery obligations" {:code "delivery_policy_invalid"})))
  (let [outside (outside-posts receipts (:self-identities policy))
        {:keys [source-urls browser-or-media?]} (inspected-urls receipts outside)
        delivered (urls (str (:text params) "\n" (:source-text params)))
        inspected (set source-urls)
        home-read (home-history policy receipts)
        home-urls (set (mapcat urls (filter string? (tree-seq coll? seq (get-in home-read [:result :details])))))
        searched (searched-sources receipts)]
    {:inspected-source-urls source-urls
     :uninspected-source-urls (filterv #(not (contains? inspected %)) delivered)
     :missing (cond-> []
      (not= (:channel-id policy) (:channel_id params))
      (conj (str "Use home channel_id " (:channel-id policy) "."))
      (empty? outside)
      (conj "Inspect a real outside author's bluesky_thread or bluesky_author_feed; your own posts are not outside evidence.")
      (not browser-or-media?)
      (conj "Read the outside link with browser_read, or load/inspect its actual media. A failed or blocked page is insufficient.")
      (nil? home-read)
      (conj (str "Read home Discord channel_id " (:channel-id policy) " with limit 100 before sharing."))
      (or (empty? delivered) (not-every? inspected delivered))
      (conj "Include a real HTTP source URL from the inspected outside work in the Discord text.")
      (or (empty? delivered) (not-every? searched delivered))
      (conj "Call memory_search with kind knoxx.tool_receipt and query equal to each exact delivered source URL before sharing.")
      (some home-urls delivered)
      (conj "This exact source URL is already in recent home Discord history. Choose a different outside work; do not repost it."))}))

(defn missing-evidence
  "Return unmet obligations, using only this run's completed receipts."
  [policy receipts params]
  (:missing (delivery-evidence policy receipts params)))

(defn- reply-missing
  [target thread searched delivered inspected]
  (cond-> []
    (not (re-matches #"at://did:[^/]+/app\.bsky\.feed\.post/[^/?#]+" target))
    (conj "Use a real returned post AT-URI as replyTo.")
    (nil? thread)
    (conj (str "Read bluesky_thread with uri exactly " target
               " and wait for its completed outside-author result before replying. Timeline, search, author feed and recalled transcripts do not substitute for this thread."))
    (not (contains? searched target))
    (conj (str "Call memory_search with kind knoxx.tool_receipt and query exactly " target " before replying."))
    (not-every? inspected delivered)
    (conj "Read each HTTP source URL included in the reply before publishing; use literal inspected URLs.")))

(defn- publication-missing
  [outside browser-or-media? delivered inspected searched]
  (cond-> []
    (empty? outside)
    (conj "Inspect a real outside author's bluesky_thread or bluesky_author_feed before publishing.")
    (not browser-or-media?)
    (conj "Read the source page or inspect its actual media before an original source-linked publication; failed reads are insufficient.")
    (or (empty? delivered) (not-every? inspected delivered))
    (conj "Include the exact HTTP source URL of the outside work inspected in this run.")
    (or (empty? delivered) (not-every? searched delivered))
    (conj "Call memory_search with kind knoxx.tool_receipt and query equal to each exact source URL before publishing.")))

(defn confirmed-publications
  "Original successful publication receipts with a real post URI and CID."
  [receipts]
  (filter #(let [{:keys [uri cid]} (get-in % [:result :details])]
             (and (string? uri)
                  (re-matches #"at://did:[^/]+/app\.bsky\.feed\.post/[^/?#]+" uri)
                  (not (str/blank? cid))))
          (mapcat (fn [receipt]
                    (if (= "bluesky_publish" (:tool_name receipt))
                      [receipt]
                      (map (fn [post]
                             (assoc receipt :tool_name "bluesky_publish"
                                    :input {:text (:text post) :replyTo (:reply_to post)}
                                    :result {:details post}))
                           (get-in receipt [:result :details :publications]))))
                  (named receipts #{"bluesky_publish" "social_request"}))))

(defn- own-continuation?
  [policy receipts target]
  (let [published (last (confirmed-publications receipts))
        uri (get-in published [:result :details :uri])
        did (second (re-matches #"at://(did:[^/]+)/app\.bsky\.feed\.post/[^/?#]+" (or uri "")))]
    (and (> (get policy :bluesky-thread-max-posts 1) 1)
         (= target uri)
         (some #{did} (:self-identities policy)))))

(defn- inspected-own-parent?
  [policy receipts target]
  (let [did (second (re-matches #"at://(did:[^/]+)/app\.bsky\.feed\.post/[^/?#]+" (or target "")))]
    (and (some #{did} (:self-identities policy))
         (some (fn [receipt]
                 (let [post (thread-target (get-in receipt [:result :details]))]
                   (and (= target (get-in receipt [:input :uri]))
                        (= target (:uri post)) (= did (:did post))
                        (true? (:available post)) (not (str/blank? (:cid post))))))
               (named receipts #{"bluesky_thread"})))))

(defn- original-draft-missing
  [policy receipts params inspected searched]
  (let [author (:bluesky-original-author policy)
        text (:text params)
        draft (some #(let [row (get-in % [:result :details])]
                       (and (= text (get-in % [:input :text]))
                            (= text (:text row))
                            (= author (:owner row))
                            (= "private" (:visibility row))
                            (= "provisional" (:tier row))
                            (= "entry" (:kind row))
                            (str/starts-with? (or (:id row) "") "knoxx-knowledge:entry:")))
                    (named receipts #{"knowledge_record"}))
        delivered (urls (str text "\n" (:source-text params)))]
    (cond-> []
      (not draft)
      (conj "Before publishing original work, save this exact post text with knowledge_record as a private provisional entry under the acting author; wait for its completed original write receipt.")
      (some #(= text (get-in % [:input :text])) (confirmed-publications receipts))
      (conj "This exact original text was already published in this run; add a distinct continuation or stop.")
      (not-every? inspected delivered)
      (conj "Inspect every HTTP URL included in original work before publishing it.")
      (not-every? searched delivered)
      (conj "Check exact-source action memory for every HTTP URL included in original work."))))

(defn- original-publish-missing
  [policy receipts params inspected searched]
  (let [target (:replyTo params)]
    (if (and (not (str/blank? target))
             (not (or (own-continuation? policy receipts target)
                      (inspected-own-parent? policy receipts target))))
      ["Original author mode requires its last confirmed post in this run, or a completed bluesky_thread read of the exact available self-authored parent with its DID and CID. Recalled notes or an own root do not substitute for that parent."]
      (original-draft-missing policy receipts params inspected searched))))

(defn bluesky-publish-evidence
  "Require exact reply context, attributed source evidence, or a trusted original draft.
   Thread inspection is context evidence, not verification of factual claims."
  [policy receipts params]
  (when-not (m/validate Obligations policy)
    (throw (ex-info "Invalid social delivery obligations" {:code "delivery_policy_invalid"})))
  (let [outside (outside-posts receipts (:self-identities policy))
        {:keys [source-urls browser-or-media?]} (inspected-urls receipts outside)
        searched (searched-sources receipts)
        target (:replyTo params)
        reply? (not (str/blank? target))
        thread (some #(let [post (thread-target (get-in % [:result :details]))]
                        (when (and (= target (get-in % [:input :uri]))
                                   (= target (:uri post)) (some #{post} outside)) %))
                     (named receipts #{"bluesky_thread"}))
        delivered (urls (str (:text params) "\n" (:source-text params)))
        inspected (set source-urls)]
    {:inspected-source-urls source-urls
     :uninspected-source-urls (filterv #(not (contains? inspected %)) delivered)
     :missing (cond
                (:bluesky-original-author policy)
                (original-publish-missing policy receipts params inspected searched)
                (own-continuation? policy receipts target)
                (cond-> []
                  (not-every? inspected delivered)
                  (conj "Inspect every new HTTP source URL before extending your confirmed thread."))
                reply? (reply-missing target thread searched delivered inspected)
                :else (publication-missing outside browser-or-media? delivered inspected searched))}))
