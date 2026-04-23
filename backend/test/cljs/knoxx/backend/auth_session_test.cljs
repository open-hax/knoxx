(ns knoxx.backend.auth-session-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.auth-session :as auth-session]))

(defn- with-env!
  [bindings f]
  (let [env (.-env js/process)
        previous (into {}
                       (map (fn [[k _]] [k (aget env k)]))
                       bindings)]
    (doseq [[k v] bindings]
      (aset env k v))
    (try
      (f)
      (finally
        (doseq [[k old-value] previous]
          (aset env k (or old-value "")))))))

(deftest ensure-user-membership-syncs-user-contract-before-resolve
  (async done
    (let [calls* (atom [])
          ctx #js {"user" #js {"id" "user-1"
                               "email" "foamy125@gmail.com"
                               "username" "foamy125@gmail.com"}
                   "actor" #js {"id" "foamy125_gmail_com"}
                   "membership" #js {"id" "membership-1"
                                     "actorId" "foamy125_gmail_com"}
                   "roleSlugs" #js ["system_admin"]}
          policy-db #js {"syncUserFromActorContract"
                         (fn [payload]
                           (swap! calls* conj [:sync (js->clj payload :keywordize-keys true)])
                           (js/Promise.resolve #js {:ok true}))
                         "resolveRequestContext"
                         (fn [headers]
                           (swap! calls* conj [:resolve (js->clj headers :keywordize-keys true)])
                           (js/Promise.resolve ctx))}]
      (-> (auth-session/ensure-user-membership! policy-db #js {"id" "gh-1" "login" "foamy"} "foamy125@gmail.com")
          (.then (fn [result]
                   (testing "GitHub email is used as the canonical Knoxx username and syncs user actor contracts first"
                     (is (= ctx result))
                     (is (= [[:sync {:email "foamy125@gmail.com"
                                    :displayName "foamy"
                                    :authProvider "github"
                                    :externalSubject "github:gh-1"}]
                             [:resolve {:x-knoxx-user-email "foamy125@gmail.com"}]]
                            @calls*)))))
          (.catch (fn [err]
                    (is nil (str "unexpected promise rejection: " err))))
          (.finally (fn [] (done)))))))

(deftest ensure-user-membership-propagates-lookup-failure
  (async done
    (let [policy-db #js {"resolveRequestContext"
                         (fn [_headers]
                           (js/Promise.reject (js/Error. "not whitelisted")))}]
      (-> (auth-session/ensure-user-membership! policy-db #js {"id" "gh-2"} "nobody@example.com")
          (.then (fn [_]
                   (is nil "expected rejection when no canonical user exists")))
          (.catch (fn [err]
                   (is (= "not whitelisted" (.-message err)))))
          (.finally (fn [] (done)))))))

(deftest resolve-auth-context-allows-configured-api-key-identity
  (async done
    (with-env! {"KNOXX_API_KEY" "dev-secret"
                "KNOXX_API_KEY_USER_EMAIL" "pi@open-hax.local"
                "NODE_ENV" "development"}
      (fn []
        (let [calls* (atom [])
              ctx #js {"user" #js {"id" "user-pi"
                                    "email" "pi@open-hax.local"}
                       "actor" #js {"id" "pi"}
                       "membership" #js {"id" "membership-pi"
                                          "actorId" "pi"}
                       "roleSlugs" #js ["system_admin"]}
              req #js {"headers" #js {"x-api-key" "dev-secret"}
                       "cookies" #js {}}
              policy-db #js {"syncUserFromActorContract"
                             (fn [payload]
                               (swap! calls* conj [:sync (js->clj payload :keywordize-keys true)])
                               (js/Promise.resolve #js {:ok true}))
                             "resolveRequestContext"
                             (fn [headers]
                               (swap! calls* conj [:resolve (js->clj headers :keywordize-keys true)])
                               (js/Promise.resolve ctx))}]
          (-> (auth-session/resolve-auth-context req policy-db)
              (.then (fn [result]
                       (testing "API-key auth resolves the configured pi actor as a real user membership"
                         (is (= ctx result))
                         (is (= [[:sync {:email "pi@open-hax.local"
                                         :displayName "Pi"
                                         :authProvider "api-key"
                                         :externalSubject "api-key:pi@open-hax.local"}]
                                 [:resolve {:x-knoxx-user-email "pi@open-hax.local"}]]
                                @calls*)))))
              (.catch (fn [err]
                        (is nil (str "unexpected promise rejection: " err))))
              (.finally (fn [] (done)))))))))
