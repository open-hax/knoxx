(ns knoxx.frontend.pages.mail.view
  "Route wrapper for the actor mailbox page: resolves auth (app bridge)
   and navigation (react-router) and hands them to the node-testable
   mail.page body."
  (:require ["react-router-dom" :as router]
            [helix.core :as hx]
            [knoxx.frontend.auth.context :as auth-ctx]
            [knoxx.frontend.pages.mail.page :as page]))

(defn- auth-actor-id [^js auth]
  (or (some-> (.-actor auth) .-id)
      (some-> (.-membership auth) .-actorId)))

(hx/defnc mail-page "Remount mailbox drafts when the verified tenant or actor changes." []
  (let [auth (auth-ctx/use-auth)
        navigate (router/useNavigate)]
    (hx/$ page/mail-page-body {:key (str (some-> ^js auth .-org .-id) ":" (auth-actor-id auth))
                       :initial-actor-id (auth-actor-id auth)
                       :navigate navigate})))
