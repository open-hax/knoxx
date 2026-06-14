(ns knoxx.frontend.auth.boundary-interaction-test
  "Port of src/pages/AuthContext.test.tsx to the node :test build —
  401 renders the login surface instead of protected content, and invite
  redemption refreshes the auth context into the protected app. Global
  fetch is mocked (the auth api uses raw fetch, not the knoxx helper)."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$ defnc]]
            [helix.dom :as d]
            [knoxx.frontend.auth.context :as auth]
            [knoxx.frontend.auth.boundary :refer [auth-boundary]]))

;; jsdom globals come from the :test build's :prepend-js.

(def fetch-calls (atom []))
(def context-status (atom 401))

(defn- json-response [status body]
  #js {:ok (< status 400)
       :status status
       :statusText "status"
       :json (fn [] (js/Promise.resolve (clj->js body)))})

(defn- route-response [path]
  (cond
    (re-find #"/api/auth/context" path)
    (if (= 200 @context-status)
      (json-response 200 {:user {:id "u1" :email "pi@open-hax.local" :displayName "Pi" :status "active"}
                          :actor {:id "actor-1"}
                          :org nil :membership nil
                          :roleSlugs ["system-admin"] :permissions []
                          :isSystemAdmin true :authProvider "local"})
      (json-response 401 {:error "unauthorized"}))

    (re-find #"/api/auth/config" path)
    (json-response 200 {:githubEnabled false :localPasswordEnabled false})

    (re-find #"/api/auth/invite/redeem" path)
    (do (reset! context-status 200)
        (json-response 200 {:ok true}))

    :else (json-response 404 {:error (str "unexpected " path)})))

(def ^:private real-fetch js/fetch)

(use-fixtures :each
  {:before (fn []
             (reset! fetch-calls [])
             (reset! context-status 401)
             (set! (.-fetch js/globalThis)
                   (fn [path init]
                     (swap! fetch-calls conj {:path (str path) :init init})
                     (js/Promise.resolve (route-response (str path))))))
   :after (fn []
            (rtl/cleanup)
            (set! (.-fetch js/globalThis) real-fetch))})

(defn- wait-until
  ([msg pred] (wait-until msg pred nil))
  ([msg pred opts]
   (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))
                (clj->js (or opts {})))))

(defnc protected-content []
  (d/div "Protected Knoxx workspace"))

(defn- render-boundary []
  (rtl/render ($ auth-boundary {:children ($ protected-content)})))

(deftest renders-login-surface-on-401
  (async done
    (let [r (render-boundary)]
      (-> (wait-until "login page" #(some? (.queryByText r "Knowledge operations platform")))
          (.then (fn []
                   (is (some? (.queryByText r "GitHub OAuth is not configured. Contact your administrator.")))
                   (is (nil? (.queryByText r "Protected Knoxx workspace")))
                   (let [context-call (first (filter #(re-find #"/api/auth/context" (:path %)) @fetch-calls))]
                     (is (some? context-call))
                     (is (= "include" (.-credentials ^js (:init context-call)))))
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest invite-redemption-refreshes-into-protected-app
  (async done
    (let [r (render-boundary)]
      (-> (wait-until "login page" #(some? (.queryByText r "Knowledge operations platform")))
          (.then (fn []
                   (.change rtl/fireEvent (.getByLabelText r "Email")
                            #js {:target #js {:value "pi@open-hax.local"}})
                   (.change rtl/fireEvent (.getByLabelText r "Invite code")
                            #js {:target #js {:value "INVITE-1"}})
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Redeem invite"}))
                   (wait-until "redeemed" #(some? (.queryByText r "Invite accepted! Redirecting…")))))
          (.then (fn []
                   (wait-until "protected app" #(some? (.queryByText r "Protected Knoxx workspace"))
                               {:timeout 1500})))
          (.then (fn []
                   (let [redeem (first (filter #(re-find #"/api/auth/invite/redeem" (:path %)) @fetch-calls))
                         ^js init (:init redeem)]
                     (is (= "POST" (.-method init)))
                     (is (= "include" (.-credentials init)))
                     (is (= {"code" "INVITE-1" "email" "pi@open-hax.local"}
                            (js->clj (js/JSON.parse (.-body init))))))
                   (is (= 2 (count (filter #(re-find #"/api/auth/context" (:path %)) @fetch-calls)))
                       "auth context refetched after redemption")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(defnc auth-consumer []
  (let [^js a (auth/use-auth)]
    (d/div (str "signed in as " (.. a -user -email)
                " admin=" (.-isSystemAdmin a)))))

(deftest authenticated-children-can-use-auth
  (reset! context-status 200)
  (async done
    (let [r (rtl/render ($ auth-boundary {:children ($ auth-consumer)}))]
      (-> (wait-until "consumer sees auth"
                      #(some? (.queryByText r "signed in as pi@open-hax.local admin=true")))
          (.then (fn [] (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
