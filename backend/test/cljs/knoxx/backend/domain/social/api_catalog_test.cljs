(ns knoxx.backend.domain.social.api-catalog-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.domain.social.api-catalog :as sut]))

(def fixture
  {"schemaVersion" 1 "sources" {"discord" {"commit" "pinned"}}
   "sourceDocuments" [{"key" "discord.openapi" "sha256" "source-hash"}]
   "operations" [{"platform" "discord" "id" "create_message" "kind" "procedure"
                  "family" "channels" "document" "discord.openapi"
                  "pointer" ["paths" "/channels/{id}/messages" "post"]
                  "search" "create message content poll embeds attachments" "requestFields" ["content" "poll"]}
                 {"platform" "discord" "id" "answer_message_poll" "kind" "procedure"
                  "family" "channels" "search" "answer message poll vote"}
                 {"platform" "bluesky" "id" "app.bsky.feed.post" "kind" "record"
                  "family" "app.bsky.feed" "document" "app.bsky.feed.post" "pointer" ["defs" "main"]
                  "search" "create message post reply thread"}]
   "documents" {"discord.openapi" {"paths" {"/channels/{id}/messages" {"post" {"requestBody" {"$ref" "#/components/schemas/Message"}}}}
                                     "components" {"schemas" {"Message" {"required" ["content"] "properties" {"poll" {"type" "object"}}}}}}
                "app.bsky.feed.post" {"defs" {"main" {"type" "record"} "replyRef" {"required" ["root" "parent"]}}}}})

(deftest feature-language-finds-capabilities-in-request-fields
  (let [result (sut/discover fixture {:platform "discord" :query "make a poll" :limit 1})]
    (is (= "create_message" (get-in result [:results 0 :operation])))
    (is (= ["content" "poll"] (get-in result [:results 0 :requestFields])))
    (is (= 1 (:matched result)) "answering a poll does not create one")
    (is (nil? (:nextOffset result))))
  (let [first-page (sut/discover fixture {:platform "discord" :query "poll" :limit 1})
        second-page (sut/discover fixture {:platform "discord" :query "poll" :limit 1 :offset 1})]
    (is (= 2 (:matched first-page)))
    (is (= 1 (:nextOffset first-page)))
    (is (= #{"answer_message_poll" "create_message"}
           (set (map :operation (concat (:results first-page) (:results second-page))))))))

(deftest an-empty-query-shows-the-complete-family-map-instead-of-an-arbitrary-shortlist
  (let [result (sut/discover fixture {})]
    (is (= 3 (:totalEntries result)))
    (is (empty? (:results result)))
    (is (= 2 (count (:families result))))))

(deftest schema-lookup-preserves-real-fields-and-local-reference-identity
  (let [op (sut/describe fixture {:platform "discord" :operation "create_message"})
        ref (sut/describe fixture {:platform "discord" :operation "create_message"
                                  :reference "#/components/schemas/Message"})]
    (is (= "source-hash" (get-in op [:source "sha256"])))
    (is (= "#/components/schemas/Message" (get-in op [:definition :schema "requestBody" "$ref"])))
    (is (= ["content"] (get-in ref [:definition :schema "required"])))
    (is (= {"type" "object"} (get-in ref [:definition :schema "properties" "poll"])))
    (is (= ["root" "parent"]
           (get-in (sut/describe fixture {:platform "bluesky" :operation "app.bsky.feed.post" :reference "#replyRef"})
                   [:definition :schema "required"])))))

(deftest schema-discovery-cannot-follow-external-urls-or-pretend-missing-operations-exist
  (is (thrown? js/Error (sut/describe fixture {:platform "discord" :operation "absent"})))
  (is (thrown? js/Error (sut/describe fixture {:platform "discord" :operation "create_message" :reference "https://example.test/secret"})))
  (is (thrown? js/Error (sut/describe fixture {:platform "bluesky" :operation "app.bsky.feed.post" :reference "../../secret"})))
  (is (thrown? js/Error (sut/discover {"schemaVersion" 99} {}))))
