(ns knoxx.backend.extern.bluesky-tools-test
  (:require [cljs.test :as test]
            [knoxx.backend.domain.bluesky.bluesky :as legacy]
            [knoxx.backend.extern.bluesky-facets :as facets]
            [knoxx.backend.extern.bluesky-operations :as operations]))

(def expected-tool-ids
  "The nineteen stable Bluesky capability identifiers exposed by the legacy facade."
  ["bluesky.publish" "bluesky.profile" "bluesky.search" "bluesky.author.feed"
   "bluesky.timeline" "bluesky.repost" "bluesky.like" "bluesky.unlike"
   "bluesky.follow" "bluesky.unfollow" "bluesky.delete" "bluesky.thread"
   "bluesky.notifications" "bluesky.followers" "bluesky.follows"
   "bluesky.chat.list" "bluesky.chat.read" "bluesky.chat.send" "bluesky.chat.react"])

(defn- tool-ids [tools]
  (mapv #(aget % "name") (array-seq tools)))

(test/deftest compatibility-catalog-preserves-all-nineteen-tools-and-policy-selection
  (test/is (= expected-tool-ids (tool-ids (legacy/create-bluesky-custom-tools {} {}))))
  (test/is (= [] (tool-ids (legacy/create-bluesky-custom-tools {} {} {}))))
  (test/is (= ["bluesky.profile"]
         (tool-ids (legacy/create-bluesky-custom-tools
                     {} {} {:tool-policies [{:tool-id "bluesky.profile" :effect "allow"}
                                            {:tool-id "bluesky.publish" :effect "deny"}]})))))

(test/deftest rich-text-facets-count-utf8-bytes-before-hashtags-and-links
  (let [text "💡 café #tag https://example.org/x"
        result (facets/build-facets text)]
    (test/is (= [{:byteStart 11 :byteEnd 15} {:byteStart 16 :byteEnd 37}]
           (mapv :index result)))
    (test/is (= [{"$type" "app.bsky.richtext.facet#tag" :tag "tag"}]
           (:features (first result))))
    (test/is (= [{"$type" "app.bsky.richtext.facet#link" :uri "https://example.org/x"}]
           (:features (second result))))
    (test/is (nil? (facets/build-facets "ordinary text")))))

(test/deftest ^:async native-sdk-publish-decodes-arguments-and-retains-progress-callback
  (let [calls (atom []) updates (atom [])]
    (with-redefs [operations/bluesky-publish!
                  (fn [runtime config text images alts reply]
                    (swap! calls conj [runtime config text (vec images) (vec alts) reply])
                    {:uri "at://did/post/one" :url "https://bsky.app/post/one"})]
      (let [tool (legacy/publish-tool {:actor "a"} {:project "p"})
            result (await ((aget tool "execute") "call-1"
                           #js {:text "hello" :images #js ["photo"] :imageAlts #js ["alt"]
                                :replyTo "at://parent"}
                           nil #(swap! updates conj (js->clj % :keywordize-keys true)) nil))]
        (test/is (= [[{:actor "a"} {:project "p"} "hello" ["photo"] ["alt"] "at://parent"]] @calls))
        (test/is (= "Publishing to Bluesky…" (get-in @updates [0 :content 0 :text])))
        (test/is (= "at://did/post/one" (aget (aget result "details") "uri")))))))

(test/deftest ^:async invalid-sdk-publish-stops-before-remote-operation
  (with-redefs [operations/bluesky-publish!
                (fn [_ _ _ _ _ _] (throw (js/Error. "remote operation must not run")))]
    (try
      (await (legacy/publish-execute {} {} "call" #js {:text "  "} nil nil nil))
      (test/is false "Blank text must refuse")
      (catch :default error
        (test/is (= "text is required" (.-message error)))))))
