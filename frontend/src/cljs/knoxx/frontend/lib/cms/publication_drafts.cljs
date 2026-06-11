(ns knoxx.frontend.lib.cms.publication-drafts
  "Broadcast-playlist CMS publication drafting, ported from
   src/lib/cms/publicationDrafts.ts. Pure — no React, no DOM (uses js/Date for
   the date prefix; pass :now for deterministic output). Canonical impl; the TS
   copy retires when its consumer (BroadcastStudioPage) migrates.

   Block/metadata maps use the serialized CMS key shape (:publication_kind,
   :block_schema_version, :source_audio_paths, :show_labels, …)."
  (:require [clojure.string :as str]))

(defn- clean-title
  [value]
  (let [t (-> (str value) str/trim (str/replace #"\s+" " "))]
    (if (= "" t) "Broadcast Playlist" t)))

(defn slugify-publication-title
  [title]
  (let [base (-> (clean-title title)
                 str/lower-case
                 (.normalize "NFKD")
                 (str/replace #"[\u0300-\u036f]" "")
                 (str/replace #"[^a-z0-9]+" "-")
                 (str/replace #"^-+|-+$" ""))
        sliced (subs base 0 (min 80 (count base)))]
    (if (= "" sliced) "broadcast-playlist" sliced)))

(defn- track-title
  [track]
  (let [candidate (or (:title track)
                      (:name track)
                      (last (str/split (str (:path track)) #"/"))
                      (:path track))
        stripped (-> (str/replace (str candidate) #"(?i)\.[a-z0-9]{2,5}$" "")
                     str/trim)]
    (if (= "" stripped) (:path track) stripped)))

(defn- unique-strings
  [values]
  (->> values
       (filter #(and (string? %) (pos? (count (str/trim %)))))
       (map str/trim)
       distinct
       vec))

(defn- nonblank-trimmed
  [value]
  (let [t (str/trim (or value ""))]
    (when-not (= "" t) t)))

(defn- to-track-ref
  [track]
  (when (pos? (count (str/trim (str (:path track)))))
    (cond-> {:path (:path track)
             :title (track-title track)
             :labels (unique-strings (or (:labels track) []))}
      (and (number? (:duration track)) (js/Number.isFinite (:duration track)))
      (assoc :duration (:duration track))
      (nonblank-trimmed (:description track)) (assoc :description (nonblank-trimmed (:description track)))
      (nonblank-trimmed (:mime track)) (assoc :mime (nonblank-trimmed (:mime track)))
      (nonblank-trimmed (:source_url track)) (assoc :source_url (nonblank-trimmed (:source_url track))))))

(defn- build-fallback-content
  [title description tracks]
  (let [preview (vec (take 40 tracks))
        omitted (max 0 (- (count tracks) (count preview)))
        n (count tracks)
        plural (if (= n 1) "" "s")
        lines (concat
               [(str "# " title)
                ""
                (or (nonblank-trimmed description)
                    (str "A Broadcast Studio playlist with " n " track" plural "."))
                ""
                "## Playlist summary"
                ""
                (str "This block publication contains " n " track" plural
                     ". The complete track snapshot is stored in structured publication metadata.")
                ""]
               (map-indexed (fn [i track]
                              (let [bits (str (inc i) ". " (:title track))]
                                (if (:description track)
                                  (str bits " — " (:description track))
                                  bits)))
                            preview)
               [(if (> omitted 0)
                  (str "…and " omitted " more tracks in the structured playlist block.")
                  "")
                ""])]
    (str/join "\n" lines)))

(defn build-playlist-publication-draft
  [input]
  (let [title (clean-title (:title input))
        slug (slugify-publication-title title)
        created-at (or (:now input) (js/Date.))
        date-prefix (subs (.toISOString created-at) 0 10)
        source-path (str "cms/playlists/" date-prefix "-" slug ".md")
        tracks (vec (keep to-track-ref (:tracks input)))
        description (nonblank-trimmed (:description input))
        content (build-fallback-content title description tracks)
        n (count tracks)
        plural (if (= n 1) "" "s")
        blocks [{:id "hero" :type "hero" :title title
                 :subtitle (or description (str n " track" plural " from Broadcast Studio"))}
                {:id "intro" :type "rich_text"
                 :markdown (or description "A curated audio publication drafted from the Broadcast Studio queue.")}
                {:id "playlist" :type "playlist" :title title
                 :description description :layout "cards" :tracks tracks
                 :show_labels true :show_descriptions true :show_duration true}]]
    {:title title
     :slug slug
     :sourcePath source-path
     :content content
     :metadata {:publication_kind "playlist"
                :block_schema_version 1
                :source_audio_paths (mapv :path tracks)
                :blocks blocks}}))
