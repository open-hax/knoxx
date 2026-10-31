(ns knoxx.frontend.auth.boundary-interaction-test
  "Port of src/pages/AuthContext.test.tsx to the node :test build —
  401 renders the login surface instead of protected content, and invite
  redemption refreshes the auth context into the protected app. Global
  fetch is mocked (the auth api uses raw fetch, not the knoxx helper)."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as test]
            [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.auth.boundary :as boundary]
            [knoxx.frontend.auth.context :as auth]))

;; jsdom globals come from the :test build's :prepend-js.

(def fetch-calls "Observed isolated auth HTTP calls." (atom []))
(def context-status "Configured isolated auth response status." (atom 401))

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

(test/use-fixtures :each
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

(hx/defnc protected-content "Protected application fixture." []
  (d/div "Protected Knoxx workspace"))

(defn- render-boundary []
  (rtl/render (hx/$ boundary/auth-boundary {:children (hx/$ protected-content)})))

(defn- ^:async verify-login-surface! [done]
  (try
    (let [page (render-boundary)]
      (await (wait-until "login page" #(some? (.queryByText page "Knowledge operations platform"))))
      ;; The mocked config disables every provider; its warning appears only
      ;; after that response arrives, independently of the initial login render.
      (await (wait-until "provider configuration"
                         #(some? (.queryByText page "GitHub OAuth is not configured. Contact your administrator."))))
      (test/is (nil? (.queryByText page "Protected Knoxx workspace")))
      (let [context-call (first (filter #(re-find #"/api/auth/context" (:path %)) @fetch-calls))]
        (test/is (some? context-call))
        (test/is (= "include" (.-credentials ^js (:init context-call))))))
    (catch :default error (test/is false (str "unexpected: " error)))
    (finally (done))))

(test/deftest renders-login-surface-on-401
  (test/async done (verify-login-surface! done)))

(defn- ^:async verify-invite-redemption! [done]
  (try
    (let [page (render-boundary)]
      (await (wait-until "login page" #(some? (.queryByText page "Knowledge operations platform"))))
      (.change rtl/fireEvent (.getByLabelText page "Email") #js {:target #js {:value "pi@open-hax.local"}})
      (.change rtl/fireEvent (.getByLabelText page "Invite code") #js {:target #js {:value "INVITE-1"}})
      (.click rtl/fireEvent (.getByRole page "button" #js {:name "Redeem invite"}))
      (await (wait-until "redeemed" #(some? (.queryByText page "Invite accepted! Redirecting…"))))
      (await (wait-until "protected app" #(some? (.queryByText page "Protected Knoxx workspace")) {:timeout 1500}))
      (let [redeem (first (filter #(re-find #"/api/auth/invite/redeem" (:path %)) @fetch-calls))
            ^js init (:init redeem)]
        (test/is (= "POST" (.-method init)))
        (test/is (= "include" (.-credentials init)))
        (test/is (= {"code" "INVITE-1" "email" "pi@open-hax.local"}
                    (js->clj (js/JSON.parse (.-body init))))))
      (test/is (= 2 (count (filter #(re-find #"/api/auth/context" (:path %)) @fetch-calls)))
               "auth context refetched after redemption"))
    (catch :default error (test/is false (str "unexpected: " error)))
    (finally (done))))

(test/deftest invite-redemption-refreshes-into-protected-app
  (test/async done (verify-invite-redemption! done)))

(hx/defnc auth-consumer "Observe the auth context provided to signed-in children." []
  (let [^js a (auth/use-auth)]
    (d/div (str "signed in as " (.. a -user -email)
                " admin=" (.-isSystemAdmin a)))))

(defn- ^:async verify-authenticated-child! [done]
  (try
    (let [page (rtl/render (hx/$ boundary/auth-boundary {:children (hx/$ auth-consumer)}))]
      (await (wait-until "consumer sees auth"
                         #(some? (.queryByText page "signed in as pi@open-hax.local admin=true")))))
    (catch :default error (test/is false (str "unexpected: " error)))
    (finally (done))))

(test/deftest authenticated-children-can-use-auth
  (reset! context-status 200)
  (test/async done (verify-authenticated-child! done)))
