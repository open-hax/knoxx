(ns knoxx.backend.infra.http-server
  "Application HTTP lifecycle through the named Fastify server boundary."
  (:require [knoxx.backend.extern.http-server :as server]))

(defn create-app!
  "Construct an opaque server handle with optional request logging."
  ([] (server/create-app!))
  ([options] (server/create-app! options)))

(defn ensure-json-empty-body-parser!
  "Install the explicit empty-JSON-body compatibility parser."
  [app] (server/ensure-json-empty-body-parser! app))

(defn add-hook!
  "Register a server lifecycle callback without inspecting native handles."
  [app hook handler] (server/add-hook! app hook handler))

(defn register-default-plugins!
  "Install the application's declared transport plugins."
  [app] (server/register-default-plugins! app))

(defn listen!
  "Bind the configured HTTP listener."
  [app host port] (server/listen! app host port))

(defn close!
  "Close the owned listener and its connections."
  [app] (server/close! app))
