(ns knoxx.frontend.auth.boundary-interaction-test
  "Exercise verified login boundaries and retain authority when logout fails."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.auth.boundary :as boundary]
            [knoxx.frontend.auth.context :as auth]))
(def calls "Actual fetch requests made by the UI." (atom []))
(def session? "Fixture server authorization state." (atom false))
(def logout-fails? "Fixture server logout refusal." (atom false))
(def original-fetch "Restored fetch boundary." js/fetch)
(defn- reply [status body]
  #js {:ok (= 200 status) :status status :json #(js/Promise.resolve (clj->js body))})
(defn- respond [path]
  (case path
    "/api/auth/config" (reply 200 {:identityProvider "axxium" :methods [{:id "password" :kind "password" :available true}]})
    "/api/auth/context" (if @session? (reply 200 {:user {:id "owner" :email "owner@wiki.test"} :actor {:id "principal"}
                                                 :roleSlugs ["system-admin"] :permissions [] :isSystemAdmin true})
                             (reply 401 {:error "Authentication required"}))
    "/api/auth/local/login" (do (reset! session? true) (reply 200 {:principal {"principal/id" "principal"}}))
    "/api/auth/logout" (if @logout-fails? (reply 503 {:error "Logout unavailable"})
                          (do (reset! session? false) (reply 200 {:ok true})))
    (reply 404 {:error "Unexpected auth request"})))
(t/use-fixtures :each
  {:before (fn [] (reset! calls []) (reset! session? false) (reset! logout-fails? false)
             (set! (.-fetch js/globalThis) (fn [path init] (swap! calls conj {:path path :init init}) (js/Promise.resolve (respond path)))))
   :after (fn [] (rtl/cleanup) (set! (.-fetch js/globalThis) original-fetch))})
(hx/defnc protected-content "A protected consumer of the shared context." []
  (let [context (auth/use-auth)]
    (d/div (d/p "Protected Knoxx workspace")
           (when (.-error context) (d/p {:role "alert"} (.-error context)))
           (d/button {:on-click (fn ^:async logout! [] (try (await (.logout context)) (catch :default _ nil)))} "Test sign out"))))
(defn- render! [] (rtl/render (hx/$ boundary/auth-boundary {:children (hx/$ protected-content)})))
(t/deftest ^:async anonymous-context-renders-real-method-registry
  (render!)
  (await (.findByText rtl/screen "Identity managed by Axxium"))
  (t/is (nil? (.queryByText rtl/screen "Protected Knoxx workspace")))
  (t/is (some? (.getByLabelText rtl/screen "Username or email")))
  (t/is (= "include" (.-credentials ^js (:init (first @calls))))))
(t/deftest ^:async password-login-refreshes-server-authority
  (render!)
  (await (.findByLabelText rtl/screen "Username or email"))
  (.change rtl/fireEvent (.getByLabelText rtl/screen "Username or email") #js {:target #js {:value "owner@wiki.test"}})
  (.change rtl/fireEvent (.getByLabelText rtl/screen "Password") #js {:target #js {:value "test-password"}})
  (.click rtl/fireEvent (.getByRole rtl/screen "button" #js {:name "Sign in with password"}))
  (await (.findByText rtl/screen "Protected Knoxx workspace"))
  (let [^js init (:init (first (filter #(= "/api/auth/local/login" (:path %)) @calls)))]
    (t/is (= "POST" (.-method init)))
    (t/is (= {:identifier "owner@wiki.test" :password "test-password"} (js->clj (js/JSON.parse (.-body init)) :keywordize-keys true))))
  (t/is (= 2 (count (filter #(= "/api/auth/context" (:path %)) @calls)))))
(t/deftest ^:async failed-logout-does-not-pretend-session-ended
  (reset! session? true) (reset! logout-fails? true) (render!)
  (await (.findByText rtl/screen "Protected Knoxx workspace"))
  (.click rtl/fireEvent (.getByRole rtl/screen "button" #js {:name "Test sign out"}))
  (await (.findByText rtl/screen "Logout unavailable"))
  (t/is (some? (.getByText rtl/screen "Protected Knoxx workspace")))
  (t/is @session?))
