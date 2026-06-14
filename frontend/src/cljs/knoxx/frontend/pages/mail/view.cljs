(ns knoxx.frontend.pages.mail.view
  "Route wrapper for the actor mailbox page: resolves auth (app bridge)
   and navigation (react-router) and hands them to the node-testable
   mail.page body."
  (:require [helix.core :refer [$ defnc]]
            ["react-router-dom" :refer [useNavigate]]
            [knoxx.frontend.auth.context :as auth-ctx]
            [knoxx.frontend.pages.mail.page :refer [mail-page-body]]))

(defn- auth-actor-id [^js auth]
  (or (some-> (.-actor auth) .-id)
      (some-> (.-membership auth) .-actorId)))

(defnc mail-page []
  (let [auth (auth-ctx/use-auth)
        navigate (useNavigate)]
    ($ mail-page-body {:initial-actor-id (auth-actor-id auth)
                       :navigate navigate})))
