(ns knoxx.backend.auth-session-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.auth-session :as auth-session]))

(deftest ensure-user-membership-resolves-context-by-canonical-email
  (async done
    (let [calls* (atom [])
          ctx #js {"user" #js {"id" "user-1"
                               "email" "foamy125@gmail.com"
                               "username" "foamy125@gmail.com"}
                   "actor" #js {"id" "system_admin"}
                   "membership" #js {"id" "membership-1"
                                     "actorId" "system_admin"}
                   "roleSlugs" #js ["system_admin"]}
          policy-db #js {"resolveRequestContext"
                         (fn [headers]
                           (swap! calls* conj (js->clj headers :keywordize-keys true))
                           (js/Promise.resolve ctx))}]
      (-> (auth-session/ensure-user-membership! policy-db #js {"id" "gh-1" "login" "foamy"} "Foamy125@gmail.com")
          (.then (fn [result]
                   (testing "GitHub email is used as the canonical Knoxx username"
                     (is (= ctx result))
                     (is (= [{:x-knoxx-user-email "Foamy125@gmail.com"}]
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
