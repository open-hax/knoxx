(ns knoxx.backend.infra.routes.auth.mcp-login
  "Render the same-origin MCP login continuation."
  (:require [open-hax.uxx.render.html :as html]))

(defn return-path?
  "Allow only the same-origin MCP consent entry point."
  [path]
  (boolean
   (and (string? path)
        (re-matches #"/api/mcp/oauth/authorize(?:\?[^#\x00-\x1f\x7f]*)?" path))))

(defn send-page!
  "Render the local login form with its validated consent continuation."
  [^js reply status return-path error]
  (-> (.code reply status)
      (.header "Cache-Control" "no-store")
      (.header "Referrer-Policy" "strict-origin")
      (.header "Content-Security-Policy" "default-src 'none'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'")
      (.type "text/html; charset=utf-8")
      (.send
       (str "<!doctype html>\n"
            (html/render
             [:html {:lang "en"}
              [:head {} [:meta {:charset "utf-8"}] [:title {} "Sign in to Knoxx"]]
              [:body {}
               [:main {}
                [:h1 {} "Sign in to Knoxx"]
                [:p {} "Sign in with your Knoxx account to continue to MCP authorization."]
                (when error [:p {:role "alert"} error])
                [:form {:method "POST" :action "/api/auth/local/login"}
                 [:input {:type "hidden" :name "mcpReturnTo" :value return-path}]
                 [:p {} [:label {} "Email " [:input {:type "email" :name "email"
                                                     :autocomplete "username" :required true}]]]
                 [:p {} [:label {} "Password " [:input {:type "password" :name "password"
                                                        :autocomplete "current-password" :required true}]]]
                 [:button {:type "submit"} "Sign in"]]]]])))))
