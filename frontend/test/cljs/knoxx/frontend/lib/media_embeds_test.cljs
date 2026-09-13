(ns knoxx.frontend.lib.media-embeds-test
  "cljs.test parity for the ported media-embed extraction — mirrors
   src/lib/mediaEmbeds.test.ts plus edge cases, verifying the CLJS impl is
   canonical before the TS copy retires."
  (:require [cljs.test :as t]
            [clojure.string :as str]
            [knoxx.frontend.lib.media-embeds :as sut]))

(t/deftest extracts-workspace-relative-audio-and-rewrites-markdown
  (let [out (sut/extract-embeds-from-markdown "Here is the track: [mix](Music/test.wav)")]
    (t/is (str/includes? (:markdown out) "mix (embedded below)"))
    (t/is (not (str/includes? (:markdown out) "(Music/test.wav)")))
    (t/is (= 1 (count (:content-parts out))))
    (t/is (= {:type "audio"
            :url "/api/workspace-media/raw?path=Music%2Ftest.wav"
            :filename "mix"
            :mimeType "audio/wav"}
           (first (:content-parts out))))))

(t/deftest does-not-auto-embed-remote-http-media
  (let [input "Remote: [clip](https://example.com/video.mp4)"
        out (sut/extract-embeds-from-markdown input)]
    (t/is (= [] (:content-parts out)))
    (t/is (= input (:markdown out)))))

(t/deftest ignores-fenced-code-blocks
  (let [input (str/join "\n" ["```" "[mix](Music/test.wav)" "```" "Outside: [mix](Music/test.wav)"])
        out (sut/extract-embeds-from-markdown input)]
    (t/is (= 1 (count (:content-parts out))))
    (t/is (str/includes? (:markdown out) "```\n[mix](Music/test.wav)\n```"))
    (t/is (str/includes? (:markdown out) "Outside: mix (embedded below)"))))

(t/deftest image-syntax-and-dedup
  (t/testing "![alt](path) image syntax extracts and the same url is not duplicated"
    (let [out (sut/extract-embeds-from-markdown
               "![pic](Graphics/a.png) and again [pic](Graphics/a.png)")]
      (t/is (= 1 (count (:content-parts out))) "same url de-duplicated across image+link")
      (t/is (= "image" (:type (first (:content-parts out)))))
      (t/is (= "/api/workspace-media/raw?path=Graphics%2Fa.png" (:url (first (:content-parts out)))))))
  (t/testing "non-media links are left untouched"
    (let [out (sut/extract-embeds-from-markdown "see [docs](/some/page)")]
      (t/is (= [] (:content-parts out)))
      (t/is (= "see [docs](/some/page)" (:markdown out))))))

(t/deftest blank-input-is-passthrough
  (t/is (= {:markdown "" :content-parts []} (sut/extract-embeds-from-markdown "")))
  (t/is (= {:markdown nil :content-parts []} (sut/extract-embeds-from-markdown nil))))
