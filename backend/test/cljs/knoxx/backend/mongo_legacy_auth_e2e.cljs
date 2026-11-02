(ns knoxx.backend.mongo-legacy-auth-e2e
  "Opt-in legacy login, durable cookie and real Mongo compatibility before identity activation."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.fetch :as fetch]
            [knoxx.backend.extern.identity-fixture :as files]
            [knoxx.backend.extern.legacy-auth-fixture :as legacy]
            [knoxx.backend.extern.mongo-identity-replica-fixture :as replica]
            [knoxx.backend.extern.mongo-remote-identity-fixture :as directory]
            [knoxx.backend.extern.remote-identity-fixture :as remote]
            [knoxx.backend.infra.db.policy :as policy]
            [knoxx.backend.infra.mongo-client :as mongo-client]
            [knoxx.backend.runtime.state :as runtime-state]))

(def ^:private actor
  {:id "actor_remote123" :email "remote@example.test" :display_name "Upstream profile"
   :status "active" :roles ["system-admin"]})

(def ^:private options
  {:bootstrapSystemAdminEmail "bootstrap@example.test"
   :bootstrapSystemAdminPassword "legacy bootstrap fixture password"
   :bootstrapSystemAdminName "Fixture administrator"})

(def ^:private isolated-environment
  {"KNOXX_SESSION_SECRET" nil "KNOXX_API_KEY" nil "KNOXX_AXXIUM_ORIGIN" nil
   "KNOXX_GITHUB_OAUTH_CLIENT_ID" nil "KNOXX_GITHUB_OAUTH_CLIENT_SECRET" nil
   "KNOXX_PUBLIC_BASE_URL" "http://localhost" "NODE_ENV" "test"})

(defn- login! [app email password]
  (remote/inject! app {:method "POST" :url "/api/auth/local/login" :payload {:email email :password password}}))

(defn- context! [app cookie]
  (remote/inject! app {:method "GET" :url "/api/auth/context" :headers (if cookie {:cookie cookie} {})}))

(defn- coordinates [body]
  [(get-in body [:user :id]) (get-in body [:membership :id])
   (get-in body [:actor :id]) (get-in body [:org :id]) (:roleSlugs body)])

(deftest ^:async existing-remote-account-keeps-local-authority-and-refuses-collisions
  (let [directory (files/directory!) mongo (atom nil) app (atom nil) upstream (atom nil)]
    (try
      (reset! mongo (await (replica/open!)))
      (reset! upstream (await (remote/upstream! directory actor)))
      (await
       (legacy/with-environment!
        (assoc isolated-environment "KNOXX_AXXIUM_ORIGIN" (:origin @upstream))
        #(legacy/with-session-state!
          (^:async fn []
            (with-redefs [mongo-client/mongo-client* (atom (:client @mongo))
                          mongo-client/mongo-db* (atom (:db @mongo))
                          runtime-state/policy-context* (atom {:mongo? true :pool nil})
                          fetch/default-client (:client @upstream)]
              (await (directory/seed! (:db @mongo) {:issuer (:origin @upstream)}))
              (reset! app (await (legacy/app! (runtime-state/current-policy-db))))
              (is (= 401 (:status (await (context! @app nil)))))
              (let [before (await (directory/directory-state (:db @mongo)))
                    first-login (await (login! @app (:email actor) "remote fixture password"))
                    cookie (:cookie first-login)
                    current (await (context! @app cookie))
                    second-login (await (login! @app (:email actor) "remote fixture password"))]
                (is (= 200 (:status first-login)))
                (is (string? cookie))
                (is (= 200 (:status second-login)))
                (is (= 200 (:status current)))
                (is (= ["existing-user" "existing-member" "existing-local-actor"]
                       [(get-in current [:body :user :id]) (get-in current [:body :membership :id])
                        (get-in current [:body :actor :id])]))
                (is (= ["local-reviewer"] (get-in current [:body :roleSlugs])))
                (is (= ["org.publications.read"] (get-in current [:body :permissions])))
                (is (= before (await (directory/directory-state (:db @mongo)))))
                (await (remote/close! @app))
                (reset! app nil)
                (reset! mongo (await (replica/restart! @mongo)))
                (reset! mongo-client/mongo-client* (:client @mongo))
                (reset! mongo-client/mongo-db* (:db @mongo))
                (legacy/forget-session-key!)
                (reset! app (await (legacy/app! (runtime-state/current-policy-db))))
                (is (= 200 (:status (await (context! @app cookie)))))
                (is (= before (await (directory/directory-state (:db @mongo))))))
              (doseq [[seed expected] [[{:issuer "https://other-issuer.example.test"} 409]
                                      [{:issuer (:origin @upstream) :auth-provider "local"} 409]
                                      [{:issuer (:origin @upstream) :member-status "inactive"} 403]
                                      [{:issuer (:origin @upstream) :org-status "inactive"} 403]
                                      [{:issuer (:origin @upstream) :user-status "inactive"} 409]]]
                (await (directory/seed! (:db @mongo) seed))
                (let [before (await (directory/directory-state (:db @mongo)))]
                  (is (= expected (:status (await (login! @app (:email actor) "remote fixture password"))))
                      (str "retained legacy classification for " seed))
                  (is (= before (await (directory/directory-state (:db @mongo))))))))))))
      (finally
        (try (when @app (await (remote/close! @app)))
             (finally
               (try (when @upstream (await ((:close! @upstream))))
                    (finally
                      (try (when @mongo (await (replica/close! @mongo)))
                           (finally (files/remove! directory)))))))))))

(deftest ^:async legacy-bootstrap-password-cookie-reload-and-logout-use-real-mongo
  (let [directory (files/directory!) mongo (atom nil) app (atom nil)]
    (try
      (reset! mongo (await (replica/open!)))
      (await
       (legacy/with-environment!
        isolated-environment
        (fn [] (legacy/with-session-state!
          #(directory/with-bootstrap-contracts!
            directory
            (^:async fn []
              (with-redefs [mongo-client/mongo-client* (atom (:client @mongo))
                            mongo-client/mongo-db* (atom (:db @mongo))
                            runtime-state/policy-context* (atom nil)]
                (let [policy-context (await (policy/create-policy-db options))]
                  (reset! runtime-state/policy-context* policy-context)
                  (reset! app (await (legacy/app! policy-context)))
                  (is (true? (:mongo? policy-context)))
                  (is (nil? (:clio-policy-store policy-context)))
                  (is (= 401 (:status (await (context! @app nil)))))
                  (is (= 401 (:status (await (login! @app (:bootstrapSystemAdminEmail options) "wrong password")))))
                  (let [logged-in (await (login! @app (:bootstrapSystemAdminEmail options) (:bootstrapSystemAdminPassword options)))
                        cookie (:cookie logged-in)
                        current (await (context! @app cookie))
                        initial-coordinates (coordinates (:body current))]
                    (is (= 200 (:status logged-in)))
                    (is (string? cookie))
                    (is (= 200 (:status current)))
                    (is (true? (get-in current [:body :isSystemAdmin])))
                    (is (= ["system-admin"] (get-in current [:body :roleSlugs])))
                    (await (remote/close! @app))
                    (reset! app nil)
                    (reset! mongo (await (replica/restart! @mongo)))
                    (reset! mongo-client/mongo-client* (:client @mongo))
                    (reset! mongo-client/mongo-db* (:db @mongo))
                    (legacy/forget-session-key!)
                    (let [reopened (await (policy/create-policy-db options))]
                      (reset! runtime-state/policy-context* reopened)
                      (reset! app (await (legacy/app! reopened)))
                      (let [after-restart (await (context! @app cookie))]
                        (is (= 200 (:status after-restart)))
                        (is (= initial-coordinates (coordinates (:body after-restart)))))
                      (is (= 200 (:status (await (remote/inject! @app {:method "POST" :url "/api/auth/logout" :headers {:cookie cookie}})))))
                      (is (= 401 (:status (await (context! @app cookie)))))
                      (is (= 200 (:status (await (login! @app (:bootstrapSystemAdminEmail options) (:bootstrapSystemAdminPassword options))))))))))))))))
      (finally
        (try (when @app (await (remote/close! @app)))
             (finally
               (try (when @mongo (await (replica/close! @mongo)))
                    (finally (files/remove! directory)))))))))
