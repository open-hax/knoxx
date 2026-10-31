(ns knoxx.frontend.auth.oidc-login-test
  "Redirect SSO is an actual login control alongside the existing native entry."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as test]
            [helix.core :as hx]
            [knoxx.frontend.auth.api :as api]
            [knoxx.frontend.auth.login :as login]))

(def ^:private original-fetch-config api/fetch-auth-config)

(test/use-fixtures :each
  {:before (fn []
             (set! api/fetch-auth-config
                   (fn [] (js/Promise.resolve #js {:axxiumOidcEnabled true
                                                   :axxiumDeviceEnabled true
                                                   :localPasswordEnabled true
                                                   :githubEnabled false}))))
   :after (fn []
            (rtl/cleanup)
            (set! api/fetch-auth-config original-fetch-config))})

(defn- ^:async redirect-entry*
  "Test helper catches rejection through the explicit completion bridge."
  []
  (let [page (rtl/render (hx/$ login/login-page {:on-login-success (fn [] nil)}))]
    (await (rtl/waitFor (fn [] (.getByRole page "link" #js {:name "Continue with Axxium"}))))
    (let [link (.getByRole page "link" #js {:name "Continue with Axxium"})]
      (test/is (= "/api/auth/axxium/start" (.getAttribute link "href"))))
    (test/is (some? (.getByRole page "button" #js {:name "Sign in with password"})))
    (test/is (some? (.getByLabelText page "Password")))
    (test/is (some? (.getByRole page "button" #js {:name "Sign in using another device"})))))

(defn- ^:async run-entry-test!
  "Do not permit a rejected render helper to escape cljs.test completion."
  [done]
  (try (await (redirect-entry*))
       (catch :default error (test/is false (str error)))
       (finally (done))))

(test/deftest redirect-entry-keeps-native-password-and-device-login
  (test/async done (run-entry-test! done)))

(defn- ^:async verify-provider-warning!
  "Render pending, failed, device-only and genuinely unconfigured provider states."
  [done]
  (try
    (doseq [[fetch-config warning?] [[(fn [] (js/Promise. (fn [_resolve _reject] nil))) false]
                                    [(fn [] (js/Promise.reject (js/Error. "isolated config outage"))) false]
                                    [(fn [] (js/Promise.resolve #js {:axxiumDeviceEnabled true})) false]
                                    [(fn [] (js/Promise.resolve #js {})) true]]]
      (set! api/fetch-auth-config fetch-config)
      (let [page (rtl/render (hx/$ login/login-page {:on-login-success (fn [] nil)}))]
        (await (rtl/act (^:async fn [] (await (js/Promise.resolve)))))
        (test/is (= warning? (some? (.queryByText page "GitHub OAuth is not configured. Contact your administrator."))))
        (rtl/cleanup)))
    (catch :default error (test/is false (str error)))
    (finally (done))))

(test/deftest provider-warning-requires-loaded-unconfigured-providers
  (test/async done (verify-provider-warning! done)))
