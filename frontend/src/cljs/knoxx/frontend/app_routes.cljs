(ns knoxx.frontend.app-routes
  "Shadow-cljs owned route constants + helpers.

   This replaces src/lib/app-routes.ts as the source-of-truth for routing."
  (:require [clojure.string :as str]))

(def ops-base-path
  "Canonical operations console prefix."
  "/ops")
(def legacy-ops-base-path
  "Historical operations prefix accepted by redirects."
  "/next")

;; Primary app routes (shadow is the router source-of-truth)
(def chat-route
  "Primary chat workspace path."
  "/")
(def login-route
  "Identity sign-in path."
  "/login")
(def signup-route
  "Identity registration path."
  "/signup")

(def mail-route
  "Actor mailbox workspace path."
  "/mail")
(def studio-route
  "Broadcast studio workspace path."
  "/studio")
(def cms-route
  "Content management index path."
  "/cms")
(def cms-editor-route
  "Content editor path."
  "/cms/editor")
(def contracts-route
  "Contract workspace path."
  "/contracts")
(def data-route
  "Data workspace path."
  "/data")
(def gardens-route
  "Publication garden workspace path."
  "/gardens")
(def translations-route
  "Translation review workspace path."
  "/translations")

(def agents-route
  "Agent workbench path."
  "/agents")
(def events-route
  "Event runtime workspace path."
  "/events")
(def legacy-event-agents-route
  "Historical event-agent path accepted by redirects."
  "/event-agents")
(def event-agents-route
  "Compatibility alias for the event runtime workspace."
  events-route)
(def basic-user-roles
  "Accepted canonical and legacy basic user role names."
  #{"basic_user" "basic-user"})

(defn- trim-slashes [value]
  (-> (or value "")
      (str/replace #"^/+" "")
      (str/replace #"/+$" "")))

(defn join-path
  "Join an optional route segment with exactly one separating slash."
  [base-path & [subpath]]
  (let [base (trim-slashes base-path)
        segment (trim-slashes (or subpath ""))]
    (cond
      (and (empty? base) (empty? segment)) "/"
      (empty? base) (str "/" segment)
      (empty? segment) (str "/" base)
      :else (str "/" base "/" segment))))

(def ops-routes
  "Named operations routes built from the canonical console prefix."
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
  "Whether the role collection includes a recognized basic user role."
  [role-slugs]
  (boolean (some basic-user-roles (or role-slugs []))))

(defn can-access-path?
  "Restrict basic users to chat and identity pages under the existing policy."
  [pathname role-slugs]
  (if-not (basic-user-role? role-slugs)
    true
    (contains? #{"/" "" "/login" "/signup"} pathname)))

(defn remap-legacy-ops-path
  "Preserve query and fragment while redirecting the historical operations prefix."
  [pathname search fragment]
  (cond
    (= pathname legacy-ops-base-path) (str ops-base-path search fragment)
    (str/starts-with? pathname (str legacy-ops-base-path "/"))
    (str ops-base-path (subs pathname (count legacy-ops-base-path)) search fragment)
    :else (str pathname search fragment)))
