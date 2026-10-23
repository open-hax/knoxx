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
                                                   :localPasswordEnabled true
                                                   :githubEnabled false}))))
   :after (fn []
            (rtl/cleanup)
            (set! api/fetch-auth-config original-fetch-config))})

(test/deftest ^:async redirect-entry-keeps-native-password-login
  (let [page (rtl/render (hx/$ login/login-page {:on-login-success (fn [] nil)}))]
    (await (rtl/waitFor (fn [] (.getByRole page "link" #js {:name "Continue with Axxium"}))))
    (let [link (.getByRole page "link" #js {:name "Continue with Axxium"})]
      (test/is (= "/api/auth/axxium/start" (.getAttribute link "href"))))
    (test/is (some? (.getByRole page "button" #js {:name "Sign in with password"})))
    (test/is (some? (.getByLabelText page "Password")))))
