(ns knoxx.backend.domain.discord.tools
  "Compatibility exports for Discord tools; native behavior lives in named extern boundaries."
  (:require [knoxx.backend.extern.discord-tool-operations :as operations]
            [knoxx.backend.extern.discord-upload :as upload]
            [knoxx.backend.extern.discord-message-selection :as selection]
            [knoxx.backend.extern.discord-tool-execution :as execution]
            [knoxx.backend.extern.discord-tool-catalog :as catalog]
            [knoxx.backend.law.discord-tools :as schemas]))

(def discord-token!
  "Compatibility export for discord-token!."
  operations/discord-token!)

(def discord-client!
  "Compatibility export for discord-client!."
  operations/discord-client!)

(def attach-openplanner-labels!
  "Compatibility export for attach-openplanner-labels!."
  selection/attach-openplanner-labels!)

(def discord-fetch-channel-messages!
  "Compatibility export for discord-fetch-channel-messages!."
  operations/discord-fetch-channel-messages!)

(def discord-scroll-channel-messages!
  "Compatibility export for discord-scroll-channel-messages!."
  operations/discord-scroll-channel-messages!)

(def discord-open-dm-channel!
  "Compatibility export for discord-open-dm-channel!."
  operations/discord-open-dm-channel!)

(def discord-fetch-dm-messages!
  "Compatibility export for discord-fetch-dm-messages!."
  operations/discord-fetch-dm-messages!)

(def discord-search-messages!
  "Compatibility export for discord-search-messages!."
  operations/discord-search-messages!)

(def svg-code-block-pattern
  "Compatibility export for svg-code-block-pattern."
  upload/svg-code-block-pattern)

(def maybe-render-svg!
  "Compatibility export for maybe-render-svg!."
  upload/maybe-render-svg!)

(def http-upload-attachment!
  "Compatibility export for http-upload-attachment!."
  upload/http-upload-attachment!)

(def local-upload-attachment!
  "Compatibility export for local-upload-attachment!."
  upload/local-upload-attachment!)

(def fetch-discord-upload-attachment!
  "Compatibility export for fetch-discord-upload-attachment!."
  upload/fetch-discord-upload-attachment!)

(def post-discord-message-chunks!
  "Compatibility export for post-discord-message-chunks!."
  operations/post-discord-message-chunks!)

(def resolve-discord-upload-attachment!
  "Compatibility export for resolve-discord-upload-attachment!."
  upload/resolve-discord-upload-attachment!)

(def discord-send-message!
  "Compatibility export for discord-send-message!."
  operations/discord-send-message!)

(def discord-react!
  "Compatibility export for discord-react!."
  operations/discord-react!)

(def discord-thread-create!
  "Compatibility export for discord-thread-create!."
  operations/discord-thread-create!)

(def discord-list-guilds!
  "Compatibility export for discord-list-guilds!."
  operations/discord-list-guilds!)

(def discord-list-guild-channels!
  "Compatibility export for discord-list-guild-channels!."
  operations/discord-list-guild-channels!)

(def discord-list-channels!
  "Compatibility export for discord-list-channels!."
  operations/discord-list-channels!)

(def discord-send-execute
  "Compatibility export for discord-send-execute."
  execution/discord-send-execute)

(def send-params
  "Compatibility export for send-params."
  schemas/send-params)

(def discord-send-tool
  "Compatibility export for discord-send-tool."
  catalog/discord-send-tool)

(def channel-messages-params
  "Compatibility export for channel-messages-params."
  schemas/channel-messages-params)

(def channel-messages-execute
  "Compatibility export for channel-messages-execute."
  execution/channel-messages-execute)

(def channel-messages-tool
  "Compatibility export for channel-messages-tool."
  catalog/channel-messages-tool)

(def channel-scroll-params
  "Compatibility export for channel-scroll-params."
  schemas/channel-scroll-params)

(def channel-scroll-execute
  "Compatibility export for channel-scroll-execute."
  execution/channel-scroll-execute)

(def channel-scroll-tool
  "Compatibility export for channel-scroll-tool."
  catalog/channel-scroll-tool)

(def dm-messages-params
  "Compatibility export for dm-messages-params."
  schemas/dm-messages-params)

(def dm-messages-execute
  "Compatibility export for dm-messages-execute."
  execution/dm-messages-execute)

(def dm-messages-tool
  "Compatibility export for dm-messages-tool."
  catalog/dm-messages-tool)

(def search-params
  "Compatibility export for search-params."
  schemas/search-params)

(def search-execute
  "Compatibility export for search-execute."
  execution/search-execute)

(def search-tool
  "Compatibility export for search-tool."
  catalog/search-tool)

(def list-servers-params
  "Compatibility export for list-servers-params."
  schemas/list-servers-params)

(def list-servers-execute
  "Compatibility export for list-servers-execute."
  execution/list-servers-execute)

(def list-servers-tool
  "Compatibility export for list-servers-tool."
  catalog/list-servers-tool)

(def guilds-tool
  "Compatibility export for guilds-tool."
  catalog/guilds-tool)

(def list-channels-params
  "Compatibility export for list-channels-params."
  schemas/list-channels-params)

(def list-channels-execute
  "Compatibility export for list-channels-execute."
  execution/list-channels-execute)

(def list-channels-tool
  "Compatibility export for list-channels-tool."
  catalog/list-channels-tool)

(def channels-params
  "Compatibility export for channels-params."
  schemas/channels-params)

(def channels-execute
  "Compatibility export for channels-execute."
  execution/channels-execute)

(def channels-tool
  "Compatibility export for channels-tool."
  catalog/channels-tool)

(def react-params
  "Compatibility export for react-params."
  schemas/react-params)

(def react-execute
  "Compatibility export for react-execute."
  execution/react-execute)

(def react-tool
  "Compatibility export for react-tool."
  catalog/react-tool)

(def thread-create-params
  "Compatibility export for thread-create-params."
  schemas/thread-create-params)

(def thread-create-execute
  "Compatibility export for thread-create-execute."
  execution/thread-create-execute)

(def thread-create-tool
  "Compatibility export for thread-create-tool."
  catalog/thread-create-tool)

(def publish-params
  "Compatibility export for publish-params."
  schemas/publish-params)

(def publish-execute
  "Compatibility export for publish-execute."
  execution/publish-execute)

(def publish-tool
  "Compatibility export for publish-tool."
  catalog/publish-tool)

(def read-params
  "Compatibility export for read-params."
  schemas/read-params)

(def read-execute
  "Compatibility export for read-execute."
  execution/read-execute)

(def read-tool
  "Compatibility export for read-tool."
  catalog/read-tool)

(def create-discord-custom-tools
  "Compatibility export for create-discord-custom-tools."
  catalog/create-discord-custom-tools)

