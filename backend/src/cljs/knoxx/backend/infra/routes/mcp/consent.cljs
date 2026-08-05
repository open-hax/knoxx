(ns knoxx.backend.infra.routes.mcp.consent
  "The MCP authorization consent page.

   String building only — no I/O, no policy. It renders what the route already
   decided: which tools are on offer, which are pre-ticked, and which actor the
   resulting token will act as. Extracted from infra.routes.mcp so that page
   markup stops competing for room with the OAuth logic."
  (:require [clojure.string :as str]
            [knoxx.backend.infra.auth.authz :as authz]))

(defn escape
  "HTML-escape a value for interpolation into an attribute or text node."
  [s]
  (-> (str (or s ""))
      (.replaceAll "&" "&amp;")
      (.replaceAll "<" "&lt;")
      (.replaceAll ">" "&gt;")
      (.replaceAll "\"" "&quot;")))

(def ^:private styles
  (str "body{font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial;margin:24px;}"
       ".box{max-width:920px;} .meta{color:#555;margin-bottom:12px;}"
       ".tools{border:1px solid #ddd;border-radius:8px;padding:12px 16px;}"
       ".actions{margin-top:18px;display:flex;gap:12px;}"
       ".warn{border:1px solid #d9822b;background:#fff8f0;border-radius:8px;padding:10px 14px;margin:12px 0;color:#7a4a10;}"
       "button{padding:8px 14px;border-radius:8px;border:1px solid #333;background:#111;color:#fff;cursor:pointer;}"
       "a{color:#0b67d0;}"))

(defn- tool-checkbox-html
  [tools selected]
  (->> (array-seq tools)
       (map (fn [tool]
              (let [n       (str (or (aget tool "name") ""))
                    label   (str (or (aget tool "label") (aget tool "name") (aget tool "description") n))
                    desc    (str (or (aget tool "description") ""))
                    checked (if (contains? selected n) "checked" "")]
                (str "\n        <label style=\"display:block; margin: 6px 0;\">\n"
                     "          <input type=\"checkbox\" name=\"tool\" value=\"" (escape n) "\" " checked " />\n"
                     "          <span style=\"font-weight:600;\">" (escape label) "</span>\n"
                     "          <span style=\"color:#666;\">(" (escape n) ")</span>\n"
                     "          <div style=\"color:#444; margin-left: 22px;\">" (escape desc) "</div>\n"
                     "        </label>\n"))))
       (str/join "\n")))

(defn- actor-html
  "How the page reports the actor the token will act as.

   Shown rather than chosen: a membership carries exactly one actor, so there is
   nothing to pick. It is still displayed, because it decides which account a
   Discord or Bluesky call will post from, and that is not something to leave
   implicit on a consent screen.

   When there is no actor, say so and say what it costs. A token can still be
   issued — the read/search tools need no credential — but every
   credential-backed tool will fail at call time, and a user should learn that
   here rather than from an error later."
  [actor-id]
  (if (str/blank? (str (or actor-id "")))
    (str "<div class=\"warn\"><strong>No actor</strong> is bound to this session, so"
         " tools that use stored credentials (Discord, Bluesky) will fail when called."
         " Assign an actor to this membership in Admin → Actors first if you need them.</div>\n")
    (str "<div><strong>Acting as:</strong> " (escape actor-id) "</div>\n")))

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

(defn page
  "The consent page HTML.

   The auth context is a CLJS map, so it must be read with the shared accessors
   rather than aget. Reaching in with (aget ctx \"user\" \"email\") both missed
   the value and threw outright — aget compiles to ctx[\"user\"][\"email\"], and
   the intermediate is undefined, so the `or` fallback never got the chance to
   run and the page 500'd."
  [{:keys [base auth-context client-id redirect-uri state code-challenge
           requested-scope tools selected]}]
  (let [action     (.-pathname (confirm-url base client-id redirect-uri state
                                            code-challenge requested-scope))
        user-email (str (or (authz/ctx-user-email auth-context) ""))
        org-slug   (str (or (authz/ctx-org-slug auth-context) ""))
        actor-id   (str (or (authz/ctx-actor-id auth-context) ""))]
    (str "<!doctype html>\n<html><head><meta charset=\"utf-8\" />\n"
         "<title>Authorize MCP Client</title>\n"
         "<style>" styles "</style></head><body><div class=\"box\">\n"
         "<h1>Authorize MCP Client</h1>\n"
         "<div class=\"meta\">\n"
         "<div><strong>Client:</strong> "       (escape client-id)    "</div>\n"
         "<div><strong>Redirect URI:</strong> " (escape redirect-uri) "</div>\n"
         "<div><strong>User:</strong> "         (escape user-email)   "</div>\n"
         "<div><strong>Org:</strong> "          (escape org-slug)     "</div>\n"
         "</div>\n"
         (actor-html actor-id)
         "<form method=\"GET\" action=\"" (escape action) "\">\n"
         "<input type=\"hidden\" name=\"client_id\" value=\""     (escape client-id)     "\" />\n"
         "<input type=\"hidden\" name=\"redirect_uri\" value=\""   (escape redirect-uri)  "\" />\n"
         "<input type=\"hidden\" name=\"state\" value=\""         (escape (or state "")) "\" />\n"
         "<input type=\"hidden\" name=\"code_challenge\" value=\"" (escape code-challenge) "\" />\n"
         "<input type=\"hidden\" name=\"code_challenge_method\" value=\"S256\" />\n"
         "<input type=\"hidden\" name=\"scope\" value=\""         (escape requested-scope) "\" />\n"
         ;; A witness of what this page showed, not identity. The confirmation
         ;; refuses when the membership's actor moved while the page was open —
         ;; otherwise a token is minted for an actor the user never saw, and it
         ;; is honoured, because it matches the membership. See
         ;; law/consent-actor-unchanged?. Blank when there is no actor, and the
         ;; confirmation treats blank-to-named as a change too.
         "<input type=\"hidden\" name=\"actor_id\" value=\""      (escape actor-id) "\" />\n"
         "<h2>Capabilities</h2>\n"
         "<p>Select exactly which Knoxx tools this client can call. You can always revoke tokens later.</p>\n"
         "<div class=\"tools\">\n"
         (tool-checkbox-html tools selected)
         "</div>\n"
         "<div class=\"actions\">\n"
         "<button type=\"submit\">Authorize</button>\n"
         "<a href=\"/\">Cancel</a>\n"
         "</div></form></div></body></html>")))
