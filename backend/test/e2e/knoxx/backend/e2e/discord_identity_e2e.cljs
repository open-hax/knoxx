(ns knoxx.backend.e2e.discord-identity-e2e
  "Discord tools, asserted on identity rather than on not-crashing.

   \"Gateway not started\" is a true answer and a useless one. It says the tool
   refused before doing anything, which tells us nothing about the question
   that matters: does a Discord call go out *as the actor the token was issued
   for*, carrying that actor's credential and no other?

   Knoxx reaches Discord two different ways, and they resolve identity
   differently. Both are covered here, because the difference is not obvious
   from either call site:

     REST    — discord.list.servers and friends call discord-token!, which
               resolves the *current actor's* discord_bot credential through
               the actor scope the MCP token established.
     Gateway — the voice tools call (dg/gateway-manager) with no arguments,
               which is the process-wide default manager, not an actor-owned
               one. That is asserted below as it stands."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.domain.discord.gateway :as gateway]
            [knoxx.backend.e2e.discord-double :as discord-double]
            [knoxx.backend.e2e.harness :as harness]
            [knoxx.backend.e2e.mcp-client :as mcp]))

(defn- header-value
  "One header from a captured request, case-insensitively."
  [request wanted]
  (some (fn [[k v]] (when (= (str/lower-case (str k)) (str/lower-case wanted)) (str v)))
        (:headers request)))

(defn- discord-api-calls
  []
  (harness/captured-matching "discord.com"))

;; ── REST: the actor's own credential, on the wire ───────────────────────────

(deftest ^:async rest-tools-call-discord-as-the-granted-actor-test
  (let [started (await (harness/start!))]
    (try
      (let [client (harness/client started)
            _      (await (mcp/initialize! client))
            result (await (mcp/call-tool! client "discord_list_servers" {}))
            outcome (mcp/call-outcome result)
            calls   (discord-api-calls)]

        (testing "the tool ran rather than refusing for want of a credential"
          (is (not= :rpc-error (:status outcome))
              (str "discord_list_servers failed at the protocol level: "
                   (:detail outcome)))
          (is (not (str/includes? (str (:detail outcome)) "must include botToken"))
              (str "the seeded credential did not reach discord-token!: "
                   (:detail outcome)))
          (is (not (str/includes? (str (:detail outcome)) "No current actor_id"))
              (str "the MCP token's actor did not establish an actor scope: "
                   (:detail outcome))))

        (testing "it reached the Discord API"
          (is (seq calls)
              "no request to discord.com was attempted")
          (is (some #(str/includes? (:url %) "/users/@me/guilds") calls)
              (str "expected the current-user guilds endpoint, got "
                   (pr-str (mapv :url calls)))))

        (testing "it authenticated as the seeded actor's bot, and only as it"
          (let [auth (some-> (first calls) (header-value "authorization"))]
            (is (some? auth) "the outbound request carried no Authorization header")
            (is (= (str "Bot " harness/discord-bot-token) auth)
                (str "expected the seeded actor's bot token, got " (pr-str auth)))))

        (testing "no Discord call used any other credential"
          (let [auths (into #{} (keep #(header-value % "authorization")) calls)]
            (is (= #{(str "Bot " harness/discord-bot-token)} auths)
                (str "more than one identity reached Discord: " (pr-str auths))))))
      (finally (await (harness/stop! started))))))

(deftest ^:async rest-tools-refuse-when-the-actor-has-no-credential-test
  ;; The same tool, the same token, the same actor — but nothing seeded for it.
  ;; Without this, a credential lookup that silently fell back to a process-wide
  ;; bot token would pass the test above and be invisible here.
  (let [started (await (harness/start!
                        (harness/policy-context harness/system-admin-context {})))]
    (try
      (let [client  (harness/client started)
            _       (await (mcp/initialize! client))
            outcome (mcp/call-outcome
                     (await (mcp/call-tool! client "discord_list_servers" {})))]
        (is (= :tool-error (:status outcome))
            (str "an actor with no Discord credential must be refused, got "
                 (pr-str outcome)))
        (is (str/includes? (str (:detail outcome)) "e2e_actor")
            (str "the refusal should name the actor it looked up: " (:detail outcome)))
        (is (empty? (discord-api-calls))
            (str "a call went to Discord for an actor with no credential: "
                 (pr-str (mapv :url (discord-api-calls))))))
      (finally (await (harness/stop! started))))))

;; ── Gateway: the tools drive it, and as whom ────────────────────────────────

(deftest ^:async voice-tools-drive-the-gateway-test
  (let [started (await (harness/start!))]
    (try
      (await
       (discord-double/with-gateway!
         {:identity-token harness/discord-bot-token
          :bot-user-id "e2e-bot-user"
          :guild-id "e2e-guild"
          :connected? true}
         (^:async fn [double]
           (let [client (harness/client started)
                 _      (await (mcp/initialize! client))]

             (testing "status reaches the gateway instead of refusing"
               (let [outcome (mcp/call-outcome
                              (await (mcp/call-tool! client "discord_voice_status" {})))]
                 (is (= :ok (:status outcome))
                     (str "discord_voice_status did not reach the manager: "
                          (:detail outcome)))
                 (is (some #(= :get-voice-connection (:method %)) ((:calls double)))
                     "status did not ask the manager for its voice connection")
                 (is (str/includes? (str (:detail outcome)) "e2e-guild")
                     (str "status did not report the guild the manager is connected "
                          "to: " (:detail outcome)))))

             (testing "join passes the channel through to the gateway"
               (let [outcome (mcp/call-outcome
                              (await (mcp/call-tool! client "discord_voice_join"
                                                     {:channel_id "e2e-channel"})))]
                 (is (= :ok (:status outcome))
                     (str "discord_voice_join failed: " (:detail outcome)))
                 (is (str/includes? (str (:detail outcome)) "e2e-guild")
                     (str "the tool did not report the guild the gateway returned: "
                          (:detail outcome)))))

             (testing "the gateway saw exactly the calls the tools claim to make"
               (let [calls ((:calls double))
                     joins (filter #(= :join-voice (:method %)) calls)]
                 (is (seq joins) (str "no joinVoice reached the manager: " (pr-str calls)))
                 (is (= ["e2e-channel"] (:args (first joins)))
                     (str "joinVoice got the wrong channel: " (pr-str (first joins))))))))))
      (finally (await (harness/stop! started))))))

(deftest ^:async voice-tools-use-the-default-manager-not-an-actor-owned-one-test
  ;; Recorded because it is load-bearing and surprising, and it is the sharpest
  ;; identity statement this file makes.
  ;;
  ;; The REST tools above are actor-scoped: discord-token! resolves *this
  ;; actor's* credential, and an actor owning none is refused. The voice tools
  ;; are not. They call (dg/gateway-manager) with no arguments, which is the
  ;; process-wide default — so a voice call speaks as whichever bot the default
  ;; manager was started with, regardless of the actor the MCP token carries.
  ;;
  ;; Asserted three ways so the claim cannot rot quietly: the actor-owned
  ;; registry stays empty, the actor owns no Discord credential in this run,
  ;; and the join still lands on the default manager anyway.
  (let [started (await (harness/start!
                        ;; No credentials at all. A REST tool refuses under this
                        ;; context; a voice tool does not.
                        (harness/policy-context harness/system-admin-context {})))]
    (try
      (await
       (discord-double/with-gateway!
         {:identity-token "default-manager-token"}
         (^:async fn [double]
           (let [client (harness/client started)
                 _      (await (mcp/initialize! client))
                 outcome (mcp/call-outcome
                          (await (mcp/call-tool! client "discord_voice_join"
                                                 {:channel_id "shared-channel"})))]
             (is (= :ok (:status outcome))
                 (str "voice join failed: " (:detail outcome)))
             (is (some #(= :join-voice (:method %)) ((:calls double)))
                 "the default manager did not receive the join")
             (is (empty? (gateway/gateway-managers))
                 (str "an actor-owned gateway manager exists, so voice tools may "
                      "now be actor-scoped — which would change which bot speaks "
                      "in a channel: " (pr-str (keys (gateway/gateway-managers)))))))))
      (finally (await (harness/stop! started))))))
