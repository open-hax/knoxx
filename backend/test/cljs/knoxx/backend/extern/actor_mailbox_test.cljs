(ns knoxx.backend.extern.actor-mailbox-test
  (:require [clio.extern.js.fs :as fs] [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.actor-mailbox :as wire] [knoxx.backend.extern.actor-tools :as tools]
            [knoxx.backend.extern.clio-store-fixture :as disk] [knoxx.backend.extern.http-server :as server]
            [knoxx.backend.infra.actor-mailbox-commands :as commands] [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.http :as http] [knoxx.backend.infra.mailbox-delivery :as delivery]
            [knoxx.backend.infra.mailbox-delivery-registry :as deliveries] [knoxx.backend.infra.routes.actors :as routes]
            [knoxx.backend.infra.stores.clio-mailbox-store :as clio] [knoxx.backend.infra.stores.mailbox-store :as registry]))
(def context {:org-id "org-a" :user-id "user-a" :membership-id "member-a" :actor-binding "sender"
              :permissions ["agent.chat.use"] :tool-policies [{:tool-id "actors.send-message" :effect "allow"}]})
(deftest wire-keeps-ids-and-rejects-authority-fields
  (let [decoded (wire/decode-send "tool-id" #js {:operation_id "forged" :target "actor:reader" :content "hello"
                                               :org_id "foreign" :actor_id "impersonated" :mode "inbox_only"} nil)]
    (is (= "tool-id" (:operation-id decoded))) (is (= "inbox-only" (:mode decoded)))
    (is (not (contains? decoded :org-id))) (is (= {} (:lineage decoded))))
  (is (= {:status "pending" :id "one" :content "body"}
         (select-keys (wire/entry-wire {:mailbox/id "one" :mailbox/status "pending" :mailbox/content "body"}) [:id :status :content])))
  (is (= 17 (:limit (wire/request-filters #js {:query #js {:limit "17"}})))))
(defn- dependencies [current]
  {:route! (fn [app method path options] (.route app (js/Object.assign #js {:method method :url path} options)))
   :ensure-permission! authz/ensure-permission! :json-response! http/json-response! :error-response! http/error-response!
   :session-guard (fn [request reply done]
                    (if-let [ctx @current] (do (aset request "ctx" ctx) (done))
                      (.send (.code reply 401) #js {:error_code "unauthenticated"})))})
(defn- ^:async response [app method path body]
  (let [result (await (.inject app (clj->js (cond-> {:method method :url path} body (assoc :payload body)))))]
    {:status (.-statusCode result) :body (js->clj (.json result) :keywordize-keys true)}))
(deftest ^:async real-http-and-sdk-tools-share-durable-send-and-denial
  (let [directory (disk/temp-directory!) previous @registry/provider* prior @deliveries/provider*
        current (atom context) app (server/create-app!)]
    (try
      (registry/install! (clio/open! {:directory directory})) (deliveries/install! (delivery/provider))
      (routes/register-actor-routes! app {} {} (dependencies current))
      (with-redefs [authz/current-context! (fn [_ _] @current)]
        (let [sent (await (response app "POST" "/api/actors/messages"
                                    {:operation_id "http-message" :target "actor:sender" :mode "inbox-only" :content "HTTP message"}))]
          (is (= 200 (:status sent))) (is (= "delivered" (get-in sent [:body :entry :status]))))
        (let [tool (aget (tools/create-tools {} {} context) 0)]
          (await ((aget tool "execute") "sdk-message" #js {:target "actor:sender" :content "SDK message" :mode "inbox-only"} nil nil nil)))
        (is (= #{"http-message" "sdk-message"} (set (map :id (get-in (await (response app "GET" "/api/actors/mailbox" nil)) [:body :entries])))))
        (is (= "SDK message" (get-in (await (response app "GET" "/api/actors/mailbox/sdk-message" nil)) [:body :entry :content])))
        (reset! current (assoc context :tool-policies [{:tool-id "actors.send-message" :effect "deny"}]))
        (is (= 0 (alength (tools/create-tools {} {} @current))))
        (is (= 403 (:status (await (response app "POST" "/api/actors/messages"
                                              {:operation_id "denied" :target "actor:sender" :mode "inbox-only" :content "denied"})))))
        (reset! current nil) (is (= 401 (:status (await (response app "GET" "/api/actors/mailbox" nil))))))
      (catch :default error (is false (str "Unexpected mailbox HTTP/tool failure: " error)))
      (finally (await (.close app)) (registry/install! previous) (deliveries/install! prior) (fs/remove-tree! directory)))))
(deftest ^:async two-session-tools-retain-their-own-lineage
  (try
    (let [first-tool (aget (tools/create-tools {} {} context) 0) second-tool (aget (tools/create-tools {} {} context) 0) captured (atom [])]
      (tools/bind-lineage! first-tool {:conversation-id "first" :session-id "first-session"})
      (tools/bind-lineage! second-tool {:conversation-id "second" :session-id "second-session"})
      (with-redefs [commands/send! (fn [_ _ _ command] (swap! captured conj command) {:ok true :entry {:mailbox/id (:operation-id command)}})]
        (await ((aget second-tool "execute") "two" #js {:target "self" :content "second"} nil nil nil))
        (await ((aget first-tool "execute") "one" #js {:target "self" :content "first"} nil nil nil))
        (is (= ["second" "first"] (mapv #(get-in % [:lineage :conversation-id]) @captured)))))
    (catch :default error (is false (str "Unexpected lineage failure: " error)))))
