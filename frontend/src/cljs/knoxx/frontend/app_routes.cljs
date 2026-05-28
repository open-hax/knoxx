(ns knoxx.frontend.app-routes
  "Shadow-cljs owned route constants + helpers.

   This replaces src/lib/app-routes.ts as the source-of-truth for routing."
  (:require [clojure.string :as str]))

(def ops-base-path "/ops")
(def legacy-ops-base-path "/next")

;; Primary app routes (shadow is the router source-of-truth)
(def chat-route "/")
(def login-route "/login")
(def signup-route "/signup")

(def mail-route "/mail")
(def studio-route "/studio")
(def cms-route "/cms")
(def cms-editor-route "/cms/editor")
(def contracts-route "/contracts")
(def data-route "/data")
(def gardens-route "/gardens")
(def translations-route "/translations")

(def agents-route "/agents")
(def events-route "/events")
(def legacy-event-agents-route "/event-agents")
(def event-agents-route events-route)
(def basic-user-roles #{"basic_user" "basic-user"})

(defn- trim-slashes [value]
  (-> (or value "")
      (str/replace #"^/+" "")
      (str/replace #"/+$" "")))

(defn join-path [base-path & [subpath]]
  (let [base (trim-slashes base-path)
        next (trim-slashes (or subpath ""))]
    (cond
      (and (empty? base) (empty? next)) "/"
      (empty? base) (str "/" next)
      (empty? next) (str "/" base)
      :else (str "/" base "/" next))))

(def ops-routes
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

(defn basic-user-role? [role-slugs]
  (boolean (some basic-user-roles (or role-slugs []))))

(defn can-access-path? [pathname role-slugs]
  (if-not (basic-user-role? role-slugs)
    true
    (contains? #{"/" "" "/login" "/signup"} pathname)))

(defn remap-legacy-ops-path [pathname search hash]
  (cond
    (= pathname legacy-ops-base-path) (str ops-base-path search hash)
    (str/starts-with? pathname (str legacy-ops-base-path "/"))
    (str ops-base-path (subs pathname (count legacy-ops-base-path)) search hash)
    :else (str pathname search hash)))
