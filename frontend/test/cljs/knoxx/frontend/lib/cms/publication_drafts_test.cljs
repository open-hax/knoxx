(ns knoxx.frontend.lib.cms.publication-drafts-test
  "cljs.test parity for the ported CMS playlist publication drafting — mirrors
   src/lib/cms/publicationDrafts.test.ts, verifying the CLJS impl is canonical
   before the TS copy retires."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.frontend.lib.cms.publication-drafts :as sut]))

(deftest slugifies-publication-titles-for-stable-cms-paths
  (is (= "brain-damage-transmissions"
         (sut/slugify-publication-title " Brain Damage Transmissions! ")))
  (testing "empty/whitespace falls back to the default slug"
    (is (= "broadcast-playlist" (sut/slugify-publication-title "   ")))
    (is (= "broadcast-playlist" (sut/slugify-publication-title "!!!")))))

(deftest builds-a-block-cms-playlist-publication-draft
  (let [draft (sut/build-playlist-publication-draft
               {:title "Late Shift Signals"
                :description "A stack of bumpers for midnight relay."
                :now (js/Date. "2026-05-07T12:00:00.000Z")
                :tracks [{:path "Audio/broadcasts/drop.wav"
                          :name "drop.wav"
                          :duration 42.4
                          :labels ["broadcast" "broadcast" "drop"]
                          :description "A bright station identifier."}]})
        blocks (get-in draft [:metadata :blocks])
        playlist (first (filter #(= "playlist" (:type %)) blocks))]
    (is (= "cms/playlists/2026-05-07-late-shift-signals.md" (:sourcePath draft)))
    (is (= "playlist" (get-in draft [:metadata :publication_kind])))
    (is (= 1 (get-in draft [:metadata :block_schema_version])))
    (is (= ["Audio/broadcasts/drop.wav"] (get-in draft [:metadata :source_audio_paths])))
    (is (= ["hero" "rich_text" "playlist"] (mapv :type blocks)))
    (is (str/includes? (:content draft) "# Late Shift Signals"))
    (is (str/includes? (:content draft) "## Playlist summary"))
    (is (= "drop" (:title (first (:tracks playlist)))))
    (is (= ["broadcast" "drop"] (:labels (first (:tracks playlist)))))
    (is (= "A bright station identifier." (:description (first (:tracks playlist)))))))

(deftest keeps-fallback-markdown-bounded-for-large-playlists
  (let [draft (sut/build-playlist-publication-draft
               {:title "Huge Queue"
                :now (js/Date. "2026-05-07T12:00:00.000Z")
                :tracks (mapv (fn [i] {:path (str "Audio/track-" (inc i) ".mp3")
                                       :name (str "track-" (inc i) ".mp3")})
                              (range 962))})]
    (is (= 962 (count (get-in draft [:metadata :source_audio_paths]))))
    (is (str/includes? (:content draft) "This block publication contains 962 tracks"))
    (is (str/includes? (:content draft) "…and 922 more tracks"))
    (is (< (count (:content draft)) 3000))))
