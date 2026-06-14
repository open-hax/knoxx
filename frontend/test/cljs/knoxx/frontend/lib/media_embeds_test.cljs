(ns knoxx.frontend.lib.media-embeds-test
  "cljs.test parity for the ported media-embed extraction — mirrors
   src/lib/mediaEmbeds.test.ts plus edge cases, verifying the CLJS impl is
   canonical before the TS copy retires."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.frontend.lib.media-embeds :as sut]))

(deftest extracts-workspace-relative-audio-and-rewrites-markdown
  (let [out (sut/extract-embeds-from-markdown "Here is the track: [mix](Music/test.wav)")]
    (is (str/includes? (:markdown out) "mix (embedded below)"))
    (is (not (str/includes? (:markdown out) "(Music/test.wav)")))
    (is (= 1 (count (:content-parts out))))
    (is (= {:type "audio"
            :url "/api/workspace-media/raw?path=Music%2Ftest.wav"
            :filename "mix"
            :mimeType "audio/wav"}
           (first (:content-parts out))))))

(deftest does-not-auto-embed-remote-http-media
  (let [input "Remote: [clip](https://example.com/video.mp4)"
        out (sut/extract-embeds-from-markdown input)]
    (is (= [] (:content-parts out)))
    (is (= input (:markdown out)))))

(deftest ignores-fenced-code-blocks
  (let [input (str/join "\n" ["```" "[mix](Music/test.wav)" "```" "Outside: [mix](Music/test.wav)"])
        out (sut/extract-embeds-from-markdown input)]
    (is (= 1 (count (:content-parts out))))
    (is (str/includes? (:markdown out) "```\n[mix](Music/test.wav)\n```"))
    (is (str/includes? (:markdown out) "Outside: mix (embedded below)"))))

(deftest image-syntax-and-dedup
  (testing "![alt](path) image syntax extracts and the same url is not duplicated"
    (let [out (sut/extract-embeds-from-markdown
               "![pic](Graphics/a.png) and again [pic](Graphics/a.png)")]
      (is (= 1 (count (:content-parts out))) "same url de-duplicated across image+link")
      (is (= "image" (:type (first (:content-parts out)))))
      (is (= "/api/workspace-media/raw?path=Graphics%2Fa.png" (:url (first (:content-parts out)))))))
  (testing "non-media links are left untouched"
    (let [out (sut/extract-embeds-from-markdown "see [docs](/some/page)")]
      (is (= [] (:content-parts out)))
      (is (= "see [docs](/some/page)" (:markdown out))))))

(deftest blank-input-is-passthrough
  (is (= {:markdown "" :content-parts []} (sut/extract-embeds-from-markdown "")))
  (is (= {:markdown nil :content-parts []} (sut/extract-embeds-from-markdown nil))))
