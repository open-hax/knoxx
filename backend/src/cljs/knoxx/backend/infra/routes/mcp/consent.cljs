(ns knoxx.backend.infra.routes.mcp.consent
  "Pure MCP consent-page view construction and server HTML rendering."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :as authz]
            [open-hax.uxx.render.html :as html]))

(def ^:private styles
  (str "body{font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial;margin:24px;}"
       ".box{max-width:920px;} .meta{color:#555;margin-bottom:12px;}"
       ".tools{border:1px solid #ddd;border-radius:8px;padding:12px 16px;}"
       ".actions{margin-top:18px;display:flex;gap:12px;}"
       ".warn{border:1px solid #d9822b;background:#fff8f0;border-radius:8px;padding:10px 14px;margin:12px 0;color:#7a4a10;}"
       "button{padding:8px 14px;border-radius:8px;border:1px solid #333;background:#111;color:#fff;cursor:pointer;}"
       "a{color:#0b67d0;}"))

(defn- tool-checkbox-node
  [tool selected]
  (let [name (str (or (aget tool "name") ""))
        label (str (or (aget tool "label")
                       (aget tool "name")
                       (aget tool "description")
                       name))
        description (str (or (aget tool "description") ""))]
    [:label {:style "display:block; margin: 6px 0;"}
     [:input {:type "checkbox"
              :name "tool"
              :value name
              :checked (contains? selected name)}]
     " "
     [:span {:style "font-weight:600;"} label]
     " "
     [:span {:style "color:#666;"} "(" name ")"]
     [:div {:style "color:#444; margin-left: 22px;"} description]]))

(defn- actor-node
  "Report the actor the resulting token will use, without offering a selector."
  [actor-id]
  (if (str/blank? (str (or actor-id "")))
    [:div {:class "warn"}
     [:strong {} "No actor"]
     " is bound to this session, so tools that use stored credentials (Discord, Bluesky) will fail when called. Assign an actor to this membership in Admin → Actors first if you need them."]
    [:div {} [:strong {} "Acting as:"] " " actor-id]))

(defn- confirm-url
  [base client-id redirect-uri state code-challenge requested-scope]
  (let [url (js/URL. "/api/mcp/oauth/authorize/confirm" base)]
    (.set (.-searchParams url) "client_id" client-id)
    (.set (.-searchParams url) "redirect_uri" redirect-uri)
    (when state (.set (.-searchParams url) "state" state))
    (.set (.-searchParams url) "code_challenge" code-challenge)
    (.set (.-searchParams url) "code_challenge_method" "S256")
    (when-not (str/blank? (str (or requested-scope "")))
      (.set (.-searchParams url) "scope" requested-scope))
    url))

(defn- hidden-input
  [name value]
  [:input {:type "hidden" :name name :value (str (or value ""))}])

(defn page-node
  "Build the consent document from a normalized, policy-free view model."
  [{:keys [base auth-context client-id redirect-uri state code-challenge
           requested-scope tools selected]}]
  (let [action (.-pathname (confirm-url base client-id redirect-uri state
                                        code-challenge requested-scope))
        user-email (str (or (authz/ctx-user-email auth-context) ""))
        org-slug (str (or (authz/ctx-org-slug auth-context) ""))
        actor-id (str (or (authz/ctx-actor-binding auth-context) ""))]
    [:html {}
     [:head {}
      [:meta {:charset "utf-8"}]
      [:title {} "Authorize MCP Client"]
      [:style {} styles]]
     [:body {}
      [:div {:class ["box"]}
       [:h1 {} "Authorize MCP Client"]
       [:div {:class {:meta true}}
        [:div {} [:strong {} "Client:"] " " client-id]
        [:div {} [:strong {} "Redirect URI:"] " " redirect-uri]
        [:div {} [:strong {} "User:"] " " user-email]
        [:div {} [:strong {} "Org:"] " " org-slug]]
       (actor-node actor-id)
       [:form {:method "GET" :action action}
        (hidden-input "client_id" client-id)
        (hidden-input "redirect_uri" redirect-uri)
        (hidden-input "state" state)
        (hidden-input "code_challenge" code-challenge)
        (hidden-input "code_challenge_method" "S256")
        (hidden-input "scope" requested-scope)
        ;; Witness what the user saw. Confirmation refuses if the membership's
        ;; actor changes while this page is open.
        (hidden-input "actor_id" actor-id)
        [:h2 {} "Capabilities"]
        [:p {} "Select exactly which Knoxx tools this client can call. You can always revoke tokens later."]
        [:div {:class [:tools]}
         (map #(tool-checkbox-node % selected) (array-seq tools))]
        [:div {:class ["actions"]}
         [:button {:type "submit"} "Authorize"]
         [:a {:href "/"} "Cancel"]]]]]]))

(defn page
  "Render the consent page as a complete HTML document."
  [view-model]
  (str "<!doctype html>\n" (html/render (page-node view-model))))
