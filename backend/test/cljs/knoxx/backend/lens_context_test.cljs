(ns knoxx.backend.lens-context-test
  "Unit tests for `lens_grab_context`, KnoxxLens contract permissions, and `web.read` SSRF/HTML safeguards."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.contracts.resolve :as contract-resolve]
            [knoxx.backend.domain.lens.context :as lens-context]
            [knoxx.backend.extern.tools :as xtools]
            [knoxx.backend.infra.lens.tools :as lens-tools]
            [knoxx.backend.infra.openplanner.tools :as op-tools]
            [knoxx.backend.infra.registry.tools :as tool-registry]
            [knoxx.backend.law.lens-context :as lens-context-law]
            [knoxx.backend.law.mcp-tool-annotations :as annotations]))

(deftest classify-scope-and-normalize-params-test
  (testing "normalizes parameters and classifies same-server cross-channel vs cross-server vs same-channel"
    (let [norm (lens-context/normalize-grab-params
                {:query "  openplanner graph  "
                 :topK 50
                 :windowBefore -2
                 :windowAfter "3"
                 :currentChannelId "ch-open"
                 :currentGuildId "guild-a"})]
      (is (= "openplanner graph" (:query norm)))
      (is (= 12 (:top-k norm)))
      (is (= 0 (:window-before norm)))
      (is (= 3 (:window-after norm)))
      (is (= "same-channel · historical"
             (lens-context/classify-connection-scope norm {:channel-id "ch-open" :guild-id "guild-a"})))
      (is (= "same-server · cross-channel"
             (lens-context/classify-connection-scope norm {:channel-id "ch-other" :guild-id "guild-a"})))
      (is (= "cross-server"
             (lens-context/classify-connection-scope norm {:channel-id "ch-ext" :guild-id "guild-b"}))))))

(deftest select-anchors-and-assemble-grabbed-context-test
  (testing "prioritizes cross-channel/cross-server anchors, attaches user/channel/guild/URL edges, and emits <grabbed_context>"
    (let [norm (lens-context/normalize-grab-params
                {:query "vector graph edges"
                 :topK 4
                 :currentChannelId "ch-1"
                 :currentGuildId "g-1"})
          graph-result
          {:stats {:visited 18 :edges 42}
           :nodes [{:id "discord:g-1:ch-1:m-1" :nodeType "discord.message" :score 0.84
                    :text "Same channel mention" :data {:channel_id "ch-1" :guild_id "g-1" :author "alice" :ts "2026-10-30T09:00:00Z"}}
                   {:id "discord:g-1:ch-2:m-2" :nodeType "discord.message" :score 0.80
                    :text "Cross channel design note" :data {:channel_id "ch-2" :channel_name "architecture" :guild_id "g-1" :author "bob" :ts "2026-10-30T09:05:00Z"}}
                   {:id "discord:g-2:ch-9:m-9" :nodeType "discord.message" :score 0.78
                    :text "External server benchmark" :data {:channel_id "ch-9" :channel_name "benchmarks" :guild_id "g-2" :author "carol" :ts "2026-10-30T09:10:00Z"}}
                   {:id "discord:guild:g-1" :nodeType "discord.guild" :label "OpenHax" :data {:guild_id "g-1" :guild_name "OpenHax"}}
                   {:id "discord:guild:g-2" :nodeType "discord.guild" :label "Promethean" :data {:guild_id "g-2" :guild_name "Promethean"}}
                   {:id "discord:user:u-bob" :nodeType "discord.user" :label "bob"}
                   {:id "web:url:https://example.com/spec" :nodeType "web.url" :label "https://example.com/spec"}]
           :edges [{:source "discord:g-1:ch-2:m-2" :target "discord:guild:g-1" :edgeKind "in_guild"}
                   {:source "discord:g-1:ch-2:m-2" :target "discord:user:u-bob" :edgeKind "authored_by"}
                   {:source "discord:g-1:ch-2:m-2" :target "web:url:https://example.com/spec" :edgeKind "mentions_web_url"}
                   {:source "discord:g-2:ch-9:m-9" :target "discord:guild:g-2" :edgeKind "in_guild"}]}
          anchors (lens-context/select-anchor-nodes graph-result norm)
          assembled (lens-context/assemble-grabbed-context
                     {:query "vector graph edges"
                      :norm-params norm
                      :graph-result graph-result
                      :anchors anchors
                      :rows-by-id {"discord:g-1:ch-2:m-2"
                                   {:id "discord:g-1:ch-2:m-2"
                                    :author "bob"
                                    :ts "2026-10-30T09:05:00Z"
                                    :text "Cross channel design note"
                                    :extra {:channel_id "ch-2" :channel_name "architecture" :guild_id "g-1" :guild_name "OpenHax"}}}
                      :windows-by-anchor-id
                      {"discord:g-1:ch-2:m-2"
                       [{:id "discord:g-1:ch-2:m-1b" :author "alice" :ts "2026-10-30T09:04:00Z"
                         :text "Did we wire guild edges?" :extra {:channel_id "ch-2" :channel_name "architecture" :guild_id "g-1"}}
                        {:id "discord:g-1:ch-2:m-2" :author "bob" :ts "2026-10-30T09:05:00Z"
                         :text "Cross channel design note" :extra {:channel_id "ch-2" :channel_name "architecture" :guild_id "g-1"}}]}})
          envelope-raw (second (re-find #"<grabbed_context>([\s\S]*?)</grabbed_context>" (:text assembled)))
          decoded (xtools/parse-json envelope-raw)]
      (is (= ["discord:g-1:ch-2:m-2" "discord:g-2:ch-9:m-9" "discord:g-1:ch-1:m-1"]
             (mapv :id anchors)))
      (is (= 3 (:count decoded)))
      (is (= "same-server · cross-channel" (:scope (first (:items decoded)))))
      (is (= 2 (:windowCount (first (:items decoded)))))
      (is (str/includes? (:text (first (:items decoded))) "Did we wire guild edges?"))
      (is (= "cross-server" (:scope (second (:items decoded)))))
      (is (= "Promethean" (:guildName (second (:items decoded))))))))

(deftest companion-contract-and-tool-registration-test
  (testing "knoxx_lens_companion exposes web.read, browser.read, graph_query, and lens_grab_context while denying autonomous Discord sends"
    (is (lens-context-law/valid-params? {:query "shared link" :topK 6 :currentChannelId "123"}))
    (is (= {:readOnlyHint true :openWorldHint false}
           (annotations/for-tool "lens_grab_context")))
    (is (= "lens_grab_context"
           (:id (tool-registry/get-tool "lens_grab_context"))))
    (let [tools (lens-tools/create-tools {} {} {:toolPolicies [{:toolId "lens_grab_context" :effect "allow"}]})
          resolved (contract-resolve/resolve-agent-contract
                    {:contracts-dir "../contracts"}
                    "knoxx_lens_companion"
                    "knoxx_lens")
          granted (set (:tool-ids resolved))]
      (is (= 1 (count tools)))
      (is (every? #(contains? granted %)
                  ["web.read" "browser.read" "graph_query" "lens_grab_context" "lens_refine_topics"]))
      (is (not (contains? granted "discord.send")))
      (is (not (contains? granted "discord.publish"))))))

(deftest web-read-ssrf-and-html-sanitization-test
  (testing "rejects private/loopback/internal URLs and extracts clean readable text from HTML"
    (is (true? (op-tools/private-or-local-host? "localhost")))
    (is (true? (op-tools/private-or-local-host? "127.0.0.1")))
    (is (true? (op-tools/private-or-local-host? "169.254.169.254")))
    (is (true? (op-tools/private-or-local-host? "host.docker.internal")))
    (is (false? (op-tools/private-or-local-host? "example.com")))
    (is (= "https://example.com/docs"
           (op-tools/assert-public-http-url! "https://example.com/docs")))
    (is (thrown-with-msg? js/Error #"refuses private or local"
                          (op-tools/assert-public-http-url! "http://127.0.0.1:7777/v1/health")))
    (is (thrown-with-msg? js/Error #"only supports http/https"
                          (op-tools/assert-public-http-url! "file:///etc/passwd")))
    (let [parsed (op-tools/html->readable-text
                  "<html><head><title>OpenPlanner &amp; Graph</title><meta name=\"description\" content=\"Vector &amp; edge memory\"><style>.x{color:red}</style><script>alert(1)</script></head><body><main><h1>Overview</h1><p>Cross-channel links work.</p></main></body></html>")]
      (is (= "OpenPlanner & Graph" (:title parsed)))
      (is (= "Vector & edge memory" (:description parsed)))
      (is (str/includes? (:text parsed) "Cross-channel links work."))
      (is (not (str/includes? (:text parsed) "alert(1)")))
      (is (not (str/includes? (:text parsed) "color:red"))))))
