(ns knoxx.backend.auth-session-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.auth-session :as auth-session]))

(defn- restore-bootstrap-email!
  [previous]
  (if (some? previous)
    (aset js/process.env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL" previous)
    (js/Reflect.deleteProperty js/process.env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL")))

(deftest ensure-user-membership-repairs-bootstrap-admin-role
  (async done
    (let [previous-email (aget js/process.env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL")
          calls* (atom [])
          initial-ctx #js {"membership" #js {"id" "membership-1"}
                           "org" #js {"id" "org-1"}
                           "roleSlugs" #js ["knowledge_worker"]}
          repaired-ctx #js {"membership" #js {"id" "membership-1"}
                            "org" #js {"id" "org-1"}
                            "roleSlugs" #js ["knowledge_worker" "system_admin"]}
          policy-db #js {"resolveRequestContext"
                         (fn [headers]
                           (swap! calls* conj [:resolve (js->clj headers :keywordize-keys true)])
                           (if (aget headers "x-knoxx-membership-id")
                             (js/Promise.resolve repaired-ctx)
                             (js/Promise.resolve initial-ctx)))
                         "setMembershipRoles"
                         (fn [membership-id payload]
                           (swap! calls* conj [:set membership-id (js->clj payload :keywordize-keys true)])
                           (js/Promise.resolve #js {:ok true}))}]
      (aset js/process.env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL" "foamy125@gmail.com")
      (-> (auth-session/ensure-user-membership! policy-db #js {"id" "gh-1" "login" "foamy"} "foamy125@gmail.com")
          (.then (fn [ctx]
                   (testing "existing bootstrap admin memberships are upgraded before session creation"
                     (is (= ["knowledge_worker" "system_admin"]
                            (js->clj (aget ctx "roleSlugs"))))
                     (is (= [[:resolve {:x-knoxx-user-email "foamy125@gmail.com"}]
                             [:set "membership-1" {:orgId "org-1" :roleSlugs ["system_admin"]}]
                             [:resolve {:x-knoxx-membership-id "membership-1"}]]
                            @calls*)))))
          (.catch (fn [err]
                    (is nil (str "unexpected promise rejection: " err))))
          (.finally (fn []
                      (restore-bootstrap-email! previous-email)
                      (done)))))))

(deftest ensure-user-membership-leaves-non-bootstrap-users-alone
  (async done
    (let [previous-email (aget js/process.env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL")
          calls* (atom [])
          ctx #js {"membership" #js {"id" "membership-2"}
                   "org" #js {"id" "org-2"}
                   "roleSlugs" #js ["knowledge_worker"]}
          policy-db #js {"resolveRequestContext"
                         (fn [headers]
                           (swap! calls* conj [:resolve (js->clj headers :keywordize-keys true)])
                           (js/Promise.resolve ctx))
                         "setMembershipRoles"
                         (fn [membership-id payload]
                           (swap! calls* conj [:set membership-id (js->clj payload :keywordize-keys true)])
                           (js/Promise.resolve #js {:ok true}))}]
      (aset js/process.env "KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL" "foamy125@gmail.com")
      (-> (auth-session/ensure-user-membership! policy-db #js {"id" "gh-2" "login" "other"} "someone@example.com")
          (.then (fn [result]
                   (testing "non-bootstrap emails do not get elevated automatically"
                     (is (= ctx result))
                     (is (= [[:resolve {:x-knoxx-user-email "someone@example.com"}]]
                            @calls*)))))
          (.catch (fn [err]
                    (is nil (str "unexpected promise rejection: " err))))
          (.finally (fn []
                      (restore-bootstrap-email! previous-email)
                      (done)))))))
