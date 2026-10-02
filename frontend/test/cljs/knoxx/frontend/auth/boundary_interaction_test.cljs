(ns knoxx.frontend.auth.boundary-interaction-test
  "Port of src/pages/AuthContext.test.tsx to the node :test build —
  401 renders the login surface instead of protected content, and invite
  redemption refreshes the auth context into the protected app. Global
  fetch is mocked (the auth api uses raw fetch, not the knoxx helper)."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.auth.boundary :as boundary]
            [knoxx.frontend.auth.context :as auth]))

;; jsdom globals come from the :test build's :prepend-js.

(def fetch-calls "Requests made by the mocked auth API." (atom []))
(def context-status "Status returned by the mocked context endpoint." (atom 401))
(def logout-mode "Controls successful and failed sign out." (atom :success))

(defn- json-response [status body]
  #js {:ok (< status 400)
       :status status
       :statusText "status"
       :json (fn [] (js/Promise.resolve (clj->js body)))})

(defn- route-response [path]
  (cond
    (re-find #"/api/auth/context" path)
    (case @context-status
      200 (json-response 200 {:user {:id "u1" :email "pi@open-hax.local" :displayName "Pi" :status "active"}
                              :actor {:id "actor-1"}
                              :org nil :membership nil
                              :roleSlugs ["system-admin"] :permissions []
                              :isSystemAdmin true :authProvider "local"})
      503 (json-response 503 {:error "identity backend unavailable"})
      (json-response 401 {:error "unauthorized"}))

    (re-find #"/api/auth/config" path)
    (json-response 200 {:githubEnabled false :localPasswordEnabled false})

    (re-find #"/api/auth/invite/redeem" path)
    (do (reset! context-status 200)
        (json-response 200 {:ok true}))

    (re-find #"/api/auth/logout" path)
    (json-response 200 {:ok true})

    :else (json-response 404 {:error (str "unexpected " path)})))

(def ^:private real-fetch js/fetch)

(t/use-fixtures :each
  {:before (fn []
             (reset! fetch-calls [])
             (reset! context-status 401)
             (reset! logout-mode :success)
             (set! (.-fetch js/globalThis)
                   (fn [path init]
                     (swap! fetch-calls conj {:path (str path) :init init})
                     (if (and (re-find #"/api/auth/logout" (str path))
                              (= :network @logout-mode))
                       (js/Promise.reject (js/Error. "network unavailable"))
                       (js/Promise.resolve (route-response (str path)))))))
   :after (fn []
            (rtl/cleanup)
            (set! (.-fetch js/globalThis) real-fetch))})

(defn- wait-until
  ([msg pred] (wait-until msg pred nil))
  ([msg pred opts]
   (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))
                (clj->js (or opts {})))))

(hx/defnc protected-content
  "A protected page rendered only after authentication."
  []
  (d/div "Protected Knoxx workspace"))

(defn- render-boundary []
  (rtl/render (hx/$ boundary/auth-boundary {:children (hx/$ protected-content)})))

(t/deftest ^:async renders-login-surface-on-401
  (let [r (render-boundary)]
    (await (wait-until "login page" #(some? (.queryByText r "Knowledge operations platform"))))
    (t/is (some? (.queryByText r "GitHub OAuth is not configured. Contact your administrator.")))
    (t/is (nil? (.queryByText r "unauthorized"))
          "an ordinary signed-out visit has no error banner")
    (t/is (nil? (.queryByText r "Protected Knoxx workspace")))
    (let [context-call (first (filter #(re-find #"/api/auth/context" (:path %)) @fetch-calls))]
      (t/is (some? context-call))
      (t/is (= "include" (.-credentials ^js (:init context-call)))))))

(t/deftest ^:async invite-redemption-refreshes-into-protected-app
  (let [r (render-boundary)]
    (await (wait-until "login page" #(some? (.queryByText r "Knowledge operations platform"))))
    (.change rtl/fireEvent (.getByLabelText r "Email")
             #js {:target #js {:value "pi@open-hax.local"}})
    (.change rtl/fireEvent (.getByLabelText r "Invite code")
             #js {:target #js {:value "INVITE-1"}})
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Redeem invite"}))
    (await (wait-until "redeemed" #(some? (.queryByText r "Invite accepted! Redirecting…"))))
    (await (wait-until "protected app" #(some? (.queryByText r "Protected Knoxx workspace"))
                       {:timeout 1500}))
    (let [redeem (first (filter #(re-find #"/api/auth/invite/redeem" (:path %)) @fetch-calls))
          ^js init (:init redeem)]
      (t/is (= "POST" (.-method init)))
      (t/is (= "include" (.-credentials init)))
      (t/is (= {"code" "INVITE-1" "email" "pi@open-hax.local"}
               (js->clj (js/JSON.parse (.-body init))))))
    (t/is (= 2 (count (filter #(re-find #"/api/auth/context" (:path %)) @fetch-calls)))
          "auth context refetched after redemption")))

(hx/defnc auth-consumer
  "Display the identity supplied to protected content."
  []
  (let [^js a (auth/use-auth)]
    (d/div
     (d/p (str "signed in as " (.. a -user -email)
               " admin=" (.-isSystemAdmin a)))
     (d/button {:on-click #(.refresh a)} "Refresh identity")
     (d/button {:on-click #(.logout a)} "Sign out"))))

(t/deftest ^:async authenticated-children-can-use-auth
  (reset! context-status 200)
  (let [r (rtl/render (hx/$ boundary/auth-boundary {:children (hx/$ auth-consumer)}))]
    (await (wait-until "consumer sees auth"
                       #(some? (.queryByText r "signed in as pi@open-hax.local admin=true"))))))

(t/deftest ^:async unavailable-context-is-not-a-signed-out-state
  (reset! context-status 503)
  (let [r (render-boundary)]
    (await (wait-until "service failure" #(some? (.queryByText r #"Could not check your session"))))
    (t/is (nil? (.queryByText r "Knowledge operations platform")))
    (reset! context-status 200)
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Try again"}))
    (await (wait-until "retry restored auth"
                       #(some? (.queryByText r "Protected Knoxx workspace"))))))

(t/deftest ^:async failed-refresh-preserves-verified-actor
  (reset! context-status 200)
  (let [r (rtl/render (hx/$ boundary/auth-boundary {:children (hx/$ auth-consumer)}))]
    (await (wait-until "actor" #(some? (.queryByText r "signed in as pi@open-hax.local admin=true"))))
    (reset! context-status 503)
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Refresh identity"}))
    (await (wait-until "visible error" #(some? (.queryByRole r "alert"))))
    (t/is (some? (.queryByText r "signed in as pi@open-hax.local admin=true")))
    (t/is (nil? (.queryByText r "Knowledge operations platform")))))

(t/deftest ^:async failed-logout-preserves-verified-actor
  (reset! context-status 200)
  (let [r (rtl/render (hx/$ boundary/auth-boundary {:children (hx/$ auth-consumer)}))]
    (await (wait-until "actor" #(some? (.queryByText r "signed in as pi@open-hax.local admin=true"))))
    (reset! logout-mode :network)
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Sign out"}))
    (await (wait-until "logout error" #(some? (.queryByText r #"Sign out failed"))))
    (t/is (some? (.queryByText r "signed in as pi@open-hax.local admin=true")))))

(t/deftest ^:async successful-logout-returns-to-login
  (reset! context-status 200)
  (let [r (rtl/render (hx/$ boundary/auth-boundary {:children (hx/$ auth-consumer)}))]
    (await (wait-until "actor" #(some? (.queryByText r "signed in as pi@open-hax.local admin=true"))))
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Sign out"}))
    (await (wait-until "login page" #(some? (.queryByText r "Knowledge operations platform"))))
    (t/is (nil? (.queryByText r #"Could not check your session")))
    (t/is (nil? (.queryByText r "signed in as pi@open-hax.local admin=true")))))
