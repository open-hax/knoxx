(ns knoxx.backend.entrypoint
  "All-CLJS backend entrypoint.

   Goal: remove handwritten Node.js .mjs shims (server.mjs) so the backend can
   be run directly from shadow-cljs, enabling nREPL-driven runtime experiments.

   This namespace is intended to be used as a shadow-cljs :esm + :runtime :node
   module with an :init-fn."
  (:require [knoxx.backend.bootstrap :as bootstrap]
            ["fastify" :default Fastify]
            ["@fastify/cors" :default fastifyCors]
            ["@fastify/websocket" :default fastifyWebsocket]
            ["@fastify/multipart" :default fastifyMultipart]
            ["@fastify/cookie" :default fastifyCookie]
            ["@fastify/formbody" :default fastifyFormbody]
            ["@sinclair/typebox" :refer [Type]]
            ["@mariozechner/pi-coding-agent" :as sdk]
            ["node:crypto" :as crypto]
            ["node:fs" :as fs]
            ["node:fs/promises" :as fs-promises]
            ["node:path" :as path]
            ["node:os" :as os]
            ["node:child_process" :refer [execFile]]
            ["node:util" :refer [promisify]]
            ["nodemailer" :default nodemailer]
            ["@modelcontextprotocol/sdk/server/mcp.js" :refer [McpServer]]
            ["@modelcontextprotocol/sdk/server/streamableHttp.js" :refer [StreamableHTTPServerTransport]]
            ["@modelcontextprotocol/sdk/types.js" :refer [isInitializeRequest]]
            ["zod" :refer [z]]))

(defn- fs-bundle
  "Knoxx expects runtime.fs to be promise-based (fs/promises) but some routes
   also need fs.createReadStream.

   Provide a merged object that includes both."
  []
  (js/Object.assign
   #js {}
   fs-promises
   #js {:createReadStream (aget fs "createReadStream")
        :promises (aget fs "promises")}))

(defn init
  "shadow-cljs module init-fn. Starts the Fastify server."
  []
  (bootstrap/bootstrap!
   #js {:Fastify Fastify
        :fastifyCors fastifyCors
        :fastifyWebsocket fastifyWebsocket
        :fastifyMultipart fastifyMultipart
        :fastifyCookie fastifyCookie
        :fastifyFormbody fastifyFormbody
        :Type Type
        :sdk sdk
        :crypto crypto
        :fs (fs-bundle)
        :path path
        :os os
        :execFileAsync (promisify execFile)
        :nodemailer nodemailer
        ;; MCP deps
        :McpServer McpServer
        :StreamableHTTPServerTransport StreamableHTTPServerTransport
        :isInitializeRequest isInitializeRequest
        ;; Schema/runtime helpers
        :z z}))
