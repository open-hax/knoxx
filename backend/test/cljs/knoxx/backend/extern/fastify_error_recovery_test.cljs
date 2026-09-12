(ns knoxx.backend.extern.fastify-error-recovery-test
  "Regressions through the actual owned Fastify routing and session boundary."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.extern.http-server :as http-server]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.auth.session-guards :as guards]))

(deftest service-refusals-retain-http-classification
  (let [denied (ex-info "Authentication required" {:status 401 :code "identity_required"})]
    (is (= 401 (fastify/error-status denied)))
    (is (= "identity_required" (fastify/error-code denied))))
  (doseq [invalid [200 399 600 "401" false nil]]
    (is (= 500 (fastify/error-status (ex-info "Invalid" {:status invalid})))))
  (is (= 429 (fastify/error-status (js-obj "statusCode" 429) nil))))

(defn- ^:async session-response!
  "Inject a request through a real callback prehandler and protected handler."
  [resolve! optional?]
  (let [app (http-server/create-app! {:request-logging? false})
        calls (atom 0)]
    (try
      (with-redefs [authz/resolve-request-context! (fn [_runtime request]
                                                   (resolve! request))]
        (.get app "/protected"
              #js {:preHandler ((if optional?
                                  guards/make-optional-session-guard
                                  guards/make-session-guard) {})}
              (fn [request reply]
                (swap! calls inc)
                (fastify/send-json! reply 200 {:context (.-ctx ^js request)})))
        (let [response (await (.inject app #js {:method "GET" :url "/protected"}))]
          {:status (.-statusCode ^js response)
           :body (js->clj (.json response) :keywordize-keys true)
           :calls @calls}))
      (finally (await (http-server/close! app))))))

(deftest ^:async required-session-refusals-keep-their-http-status
  (doseq [[status code message] [[401 "identity_required" "Authentication required"]
                                [403 "membership_denied" "Membership required"]
                                [503 "policy_unavailable" "private-policy-backend"]]]
    (let [result (await (session-response!
                         (fn [_request] (throw (ex-info message {:status status :code code})))
                         false))]
      (is (= status (:status result)))
      (is (= code (get-in result [:body :code])))
      (is (zero? (:calls result)))
      (when (= 503 status)
        (is (= "internal error" (get-in result [:body :message])))))))

(deftest ^:async session-success-and-optional-failure-complete-once
  (let [context {:user-id "verified-user" :org-id "verified-org"}
        authenticated (await (session-response! (constantly context) false))
        anonymous (await (session-response!
                          (fn [_request] (throw (ex-info "No session" {:status 401})))
                          true))]
    (is (= {:status 200 :body {:context context} :calls 1} authenticated))
    (is (= {:status 200 :body {:context nil} :calls 1} anonymous))))

(deftest ^:async unclassified-and-falsy-throws-cannot-open-a-protected-route
  (doseq [error [(js/Error. "private internal detail") nil false]]
    (let [result (await (session-response! (fn [_request] (throw error)) false))]
      (is (= 500 (:status result)))
      (is (zero? (:calls result)))
      (is (= "internal error" (get-in result [:body :message]))))))

(deftest ^:async qualified-wiki-identifiers-reach-the-owned-router
  (let [app (http-server/create-app! {:request-logging? false})
        document-id (str "sandbox.wiki/research/" (apply str (repeat 117 "x")))]
    (try
      (.get app "/documents/:documentId"
            (fn [request reply]
              (fastify/send-json! reply 200 {:id (fastify/request-param request :documentId)})))
      (let [response (await (.inject app #js {:method "GET"
                                             :url (str "/documents/" (js/encodeURIComponent document-id))}))
            excessive (await (.inject app #js {:method "GET"
                                              :url (str "/documents/" (apply str (repeat 1025 "x")))}))]
        (is (> (count document-id) 100))
        (is (= 200 (.-statusCode ^js response)))
        (is (= document-id (.-id ^js (.json response))))
        (is (= 404 (.-statusCode ^js excessive))))
      (finally (await (http-server/close! app))))))
