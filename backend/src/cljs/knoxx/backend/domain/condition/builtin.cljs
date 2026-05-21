(ns knoxx.backend.domain.condition.builtin
  "Built-in condition functions for common trigger predicates.
   Condition fns receive whatever args the trigger expression passes them.
   Typical call: (conditions/discord.mention event)"
  (:require [clojure.string :as str]
            [knoxx.backend.domain.condition.registry :as registry]))

;; ─── Discord conditions ───────────────────────────────────────────────

(defn condition-discord-mention
  "True if the Discord message payload mentions the bot user.
   Call: (conditions/discord.mention event)"
  [event]
  (let [payload (or (:event/payload event) {})
        bot-user-id (some-> (:gatewayBotUserId payload) str str/trim not-empty)
        content (str (or (:content payload) ""))]
    (boolean
     (and bot-user-id
          (or (str/includes? content (str "<@" bot-user-id ">"))
              (str/includes? content (str "<@!" bot-user-id ">")))))))

(defn condition-discord-keyword
  "True if the Discord message content contains any of the supplied keywords.
   Keywords are compared case-insensitively.
   Call: (conditions/discord.keyword event [\"frankie\" \"yap\"])"
  [event keywords]
  (let [payload (or (:event/payload event) {})
        content (str/lower-case (str (or (:content payload) "")))
        kw-list (if (sequential? keywords) keywords [])]
    (or (empty? kw-list)
        (some #(str/includes? content (str/lower-case (str %))) kw-list))))

(defn condition-discord-channel
  "True if the Discord message is from one of the specified channel IDs.
   Call: (conditions/discord.channel event [\"123\" \"456\"])"
  [event channels]
  (let [payload (or (:event/payload event) {})
        channel-id (str (or (:channelId payload) (:channel-id payload) ""))
        ch-list (if (sequential? channels) channels [])]
    (or (empty? ch-list)
        (contains? (set (map str ch-list)) channel-id))))

(defn condition-discord-author
  "True if the Discord message author ID is in the allowed set.
   Call: (conditions/discord.author event [\"123\"])"
  [event author-ids]
  (let [payload (or (:event/payload event) {})
        author-id (str (or (:authorId payload) (:author-id payload) ""))
        id-list (if (sequential? author-ids) author-ids [])]
    (or (empty? id-list)
        (contains? (set (map str id-list)) author-id))))

;; ─── Generic conditions ───────────────────────────────────────────────

(defn condition-always
  "Always returns true."
  [& _]
  true)

(defn condition-never
  "Always returns false."
  [& _]
  false)

;; ─── Registration ─────────────────────────────────────────────────────

(defn register-builtins!
  "Register all built-in conditions. Call once at startup."
  []
  (registry/register-condition! :conditions/discord.mention condition-discord-mention)
  (registry/register-condition! :conditions/discord.keyword condition-discord-keyword)
  (registry/register-condition! :conditions/discord.channel condition-discord-channel)
  (registry/register-condition! :conditions/discord.author condition-discord-author)
  (registry/register-condition! :conditions/always condition-always)
  (registry/register-condition! :conditions/never condition-never))