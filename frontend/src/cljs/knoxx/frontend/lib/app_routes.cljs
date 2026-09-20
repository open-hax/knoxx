(ns knoxx.frontend.lib.app-routes
  "Route constants and path helpers, ported from src/lib/app-routes.ts.
   Pure — no React, no DOM. Canonical implementation; the TypeScript copy
   retires when its consumers migrate. Route map keys are kebab-case
   (:docs-view, :graph-export-debug)."
  (:require [clojure.string :as str]))

(def ops-base-path
  "Canonical operations console prefix."
  "/ops")
(def legacy-ops-base-path
  "Historical operations prefix accepted by redirects."
  "/next")
(def agents-route
  "Agent workspace route."
  "/agents")
(def events-route
  "Event management route."
  "/events")
(def ingestion-route
  "Document ingestion route."
  "/ingestion")
;; Legacy route kept for redirects.
(def legacy-event-agents-route
  "Historical event-agent route accepted by redirects."
  "/event-agents")
;; Back-compat name: this was previously the event-agent control surface.
(def event-agents-route
  "Compatibility alias for the event management route."
  events-route)
(def basic-user-roles
  "Accepted legacy and canonical names for the basic user role."
  ["basic_user" "basic-user"])
(def basic-user-role
  "Canonical default basic user role."
  (first basic-user-roles))

(defn- trim-slashes
  [value]
  (str/replace (str value) #"^/+|/+$" ""))

(defn join-path
  "Join route segments with one slash while preserving the root path."
  ([base-path] (join-path base-path ""))
  ([base-path subpath]
   (let [base (trim-slashes base-path)
         segment (trim-slashes subpath)]
     (cond
       (and (= "" base) (= "" segment)) "/"
       (= "" base) (str "/" segment)
       (= "" segment) (str "/" base)
       :else (str "/" base "/" segment)))))

(def ops-routes
  "Named canonical routes for operations console navigation."
  {:root ops-base-path
   :documents (join-path ops-base-path "documents")
   :docs-view (join-path ops-base-path "docs/view")
   :agents (join-path ops-base-path "agents")
   :studio (join-path ops-base-path "studio")
   :vectors (join-path ops-base-path "vectors")
   :labels (join-path ops-base-path "labels")
   :graph-export-debug (join-path ops-base-path "graph-export-debug")
   :settings (join-path ops-base-path "settings")
   :admin (join-path ops-base-path "admin")})

(defn basic-user-role?
  "Whether the supplied roles include a recognized basic user role."
  ([] (basic-user-role? []))
  ([role-slugs]
   (boolean (some (set basic-user-roles) (or role-slugs [])))))

(defn can-access-path?
  "Basic users are limited to the chat/auth surfaces; everyone else is allowed."
  ([pathname] (can-access-path? pathname []))
  ([pathname role-slugs]
   (if-not (basic-user-role? role-slugs)
     true
     (contains? #{"/" "" "/login" "/signup"} pathname))))

(defn remap-legacy-ops-path
  "Rewrite legacy /next* paths onto the canonical /ops* prefix, preserving
   search and hash."
  ([pathname] (remap-legacy-ops-path pathname "" ""))
  ([pathname search] (remap-legacy-ops-path pathname search ""))
  ([pathname search fragment]
   (cond
     (= pathname legacy-ops-base-path)
     (str ops-base-path search fragment)

     (str/starts-with? pathname (str legacy-ops-base-path "/"))
     (str ops-base-path (subs pathname (count legacy-ops-base-path)) search fragment)

     :else
     (str pathname search fragment))))
