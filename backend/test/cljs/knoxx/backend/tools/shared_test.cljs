(ns knoxx.backend.tools.shared-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.tools.shared :as shared]))

(deftest sanitize-custom-tool-name-teaches-runtime-name
  (testing "dotted canonical tools are advertised with their callable sanitized runtime name"
    (let [tool #js {:name "discord.send"
                    :description "Send a message to Discord."
                    :promptSnippet "Send with discord.send when replying."
                    :promptGuidelines #js ["Use discord.send to post replies."]}
          sanitized (shared/sanitize-custom-tool-name tool)]
      (is (= "discord_send" (aget sanitized "name")))
      (is (= "discord.send" (aget sanitized "originalName")))
      (is (re-find #"Call this tool as `discord_send`" (aget sanitized "description")))
      (is (re-find #"Call as `discord_send`" (aget sanitized "promptSnippet")))
      (is (re-find #"discord_send" (aget sanitized "promptSnippet")))
      (is (re-find #"canonical discord.send" (aget (aget sanitized "promptGuidelines") 0)))
      (is (re-find #"discord_send" (aget (aget sanitized "promptGuidelines") 0))))))
