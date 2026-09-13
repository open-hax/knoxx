(ns knoxx.backend.mcp-consent-test
  "Actual TCP consent and MCP requests against Axxium and canonical local OAuth storage."
  (:require [axxium.infra.identity :as axxium]
            [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            [knoxx.backend.domain.mcp.mcp-expose :as expose]
            [knoxx.backend.extern.axxium :as identity-http]
            [knoxx.backend.extern.fastify :as http]
            [knoxx.backend.extern.identity-fixture :as fixture]
            [knoxx.backend.infra.identity :as identity]
            [knoxx.backend.infra.identity-bootstrap :as bootstrap]
            [knoxx.backend.infra.routes.mcp :as mcp]
            [knoxx.backend.infra.stores.clio-mcp-oauth :as oauth]
            [knoxx.backend.infra.stores.mcp-oauth-dispatch :as dispatch]
            [knoxx.backend.infra.stores.mongo-mcp-oauth :as tokens]
            [knoxx.backend.runtime.state :as runtime]
            ["@fastify/cookie" :as cookie]
            ["@fastify/formbody" :as formbody]
            ["fastify" :as fastify]
            ["node:crypto" :as crypto]))

(def ^:private origin "http://localhost")
(def ^:private callback "https://client.example/callback")
(def ^:private verifier (apply str (repeat 48 "v")))
(def ^:private challenge (.digest (.update (crypto/createHash "sha256") verifier) "base64url"))

(defn- ^:async request! [base method path token body extra-headers]
  (let [response (await (js/fetch (str base path)
                                (clj->js (cond-> {:method method :redirect "manual"
                                                 :headers (merge {"content-type" "application/json"}
                                                                 (when token {"cookie" (str "axxium_session=" token)})
                                                                 extra-headers)}
                                           body (assoc :body (js/JSON.stringify (clj->js body)))))))
        text (await (.text response))]
    {:status (.-status response) :text text :location (.get (.-headers response) "location")
     :json (try (js->clj (js/JSON.parse text) :keywordize-keys true) (catch :default _ nil))}))

(defn- tools []
  (clj->js (mapv (fn [id] {:name id :description id :parameters {:type "object" :properties {}}
                           :execute (fn [& _] #js {:content #js []})})
                 ["wiki_get" "wiki_save"])))

(defn- ^:async with-server! [run!]
  (let [directory (fixture/directory!)
        previous-context @runtime/policy-context*
        previous-provider (dispatch/current)
        app (fastify/fastify)]
    (try
      (with-redefs [identity-http/options (fn [_] {:provider :edn :directory (str directory "/identity")
                                                   :public-base-url origin})
                    expose/create-knoxx-custom-tools-js (fn [_ _ _] (tools))]
        (let [context (await (bootstrap/create-context! {:policy-provider :edn :wiki-directory directory} {}))
              created (await (axxium/signup! (:axxium-service context)
                                            {:username "mcp-user" :email "mcp@example.test"
                                             :password "correct horse battery staple"}))]
          (reset! runtime/policy-context* context)
          (dispatch/install! (oauth/open! {:directory (str directory "/oauth")}))
          (await (.register app cookie/default))
          (await (.register app formbody/default))
          (.setErrorHandler app (fn [error _request reply]
                                 (.send (.code reply (http/error-status error))
                                        (clj->js {:error (http/error-code error)}))))
          (mcp/register-mcp-http-routes! app nil {:knoxx-base-url origin :contracts-dir "../contracts"})
          (let [base (await (.listen app #js {:host "127.0.0.1" :port 0}))]
            (await (run! {:base base :context context :created created :directory directory})))))
      (finally
        (await (.close app))
        (reset! runtime/policy-context* previous-context)
        (dispatch/install! previous-provider)
        (fixture/remove! directory)))))

(defn- approval [client actor]
  {:client_id client :redirect_uri callback :code_challenge challenge
   :code_challenge_method "S256" :tool ["wiki_get"] :actor_id actor :state "returned-state"})

(defn- ^:async register-client! [base]
  (let [response (await (request! base "POST" "/api/mcp/oauth/register" nil
                                  {:redirect_uris [callback]} {}))]
    (is (= 201 (:status response)) (:text response))
    (get-in response [:json :client_id])))

(defn- ^:async consent-guards! [{:keys [base created]}]
  (let [token (:token created)
        client (await (register-client! base))
        body (approval client (get-in created [:principal :principal/id]))
        confirm "/api/mcp/oauth/authorize/confirm"]
    (is (= 302 (:status (await (request! base "GET" "/api/mcp/oauth/authorize" nil nil {})))))
    (is (str/includes? (:location (await (request! base "GET" "/api/mcp/oauth/authorize" nil nil {}))) "/login?"))
    (is (= 404 (:status (await (request! base "GET" confirm token nil {})))))
    (doseq [headers [{} {"origin" "https://foreign.example"}]]
      (is (= 403 (:status (await (request! base "POST" confirm token body headers))))))
    (is (= 401 (:status (await (request! base "POST" confirm nil body {"origin" origin})))))
    (is (= 401 (:status (await (request! base "POST" confirm token body
                                        {"origin" origin "authorization" "Basic broken"})))))))

(defn- ^:async issue-grant! [{:keys [base created]}]
  (let [client (await (register-client! base))
        response (await (request! base "POST" "/api/mcp/oauth/authorize/confirm" (:token created)
                                 (approval client (get-in created [:principal :principal/id])) {"origin" origin}))]
    (is (= 302 (:status response)) (:text response))
    (let [code (.get (.-searchParams (js/URL. (:location response))) "code")
          exchange {:grant_type "authorization_code" :code code :code_verifier verifier
                    :client_id client :redirect_uri callback}
          results (await (js/Promise.all
                          #js [(request! base "POST" "/api/mcp/oauth/token" nil exchange {})
                               (request! base "POST" "/api/mcp/oauth/token" nil exchange {})]))
          results (vec (array-seq results))]
      (is (= [200 400] (vec (sort (map :status results)))) (pr-str results))
      (get-in (first (filter #(= 200 (:status %)) results)) [:json :access_token]))))

(defn- ^:async mcp-request! [base token method]
  (request! base "POST" "/mcp" nil {:jsonrpc "2.0" :id 1 :method method :params {}}
            {"authorization" (str "Bearer " token) "accept" "application/json, text/event-stream"}))

(deftest ^:async real-browser-consent-refuses-get-and-cross-origin-mutations
  (await (with-server! consent-guards!)))

(deftest ^:async real-code-exchange-is-one-use-and-grants-are-current-principal-bound
  (await
   (with-server!
     (fn ^:async verify-grant! [{:keys [base context created] :as fixture}]
       (let [token (await (issue-grant! fixture))
             ctx (await (identity/resolve-bearer-context! context token))
             stored (await (tokens/get-token! token))]
         (is (= (:principal created) (:axxium-principal ctx)))
         (is (not (str/includes? stored token)))
         (is (= 200 (:status (await (mcp-request! base (:token created) "tools/list")))))
         (let [listed (await (mcp-request! base token "tools/list"))]
           (is (= 200 (:status listed)) (:text listed))
           (is (str/includes? (:text listed) "wiki_get"))
           (is (not (str/includes? (:text listed) "wiki_save"))))
         (is (= 401 (:status (await (mcp-request! base "not-a-token" "tools/list")))))
         (await (tokens/delete-token-for-membership! token (get-in ctx [:membership :id])))
         (is (= 401 (:status (await (mcp-request! base token "tools/list"))))))))))
