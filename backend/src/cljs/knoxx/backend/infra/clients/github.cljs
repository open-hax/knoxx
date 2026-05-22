(ns knoxx.backend.infra.clients.github
  "GitHub OAuth/user API client protocol.

   Covers the auth/session namespace requests to GitHub's OAuth token endpoint
   and user/email APIs. Implementations return CLJS data and keep fetch,
   headers, and JSON conversion inside the client."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [promesa.core :as p]))

(defprotocol IGitHubClient
  (oauth-access-token! [client code redirect-uri])
  (authenticated-user! [client access-token])
  (authenticated-emails! [client access-token]))

(defn- configured?
  [value]
  (not (str/blank? (str (or value "")))))

(defn- checked-body!
  [resp label]
  (if (:ok resp)
    (:body resp)
    (throw (js/Error. (str label " failed (" (:status resp) "): " (pr-str (:body resp)))))))

(defrecord FetchGitHubClient [config http-client timeout-ms]
  IGitHubClient
  (oauth-access-token! [_ code redirect-uri]
    (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                               {:url "https://github.com/login/oauth/access_token"
                                :opts {:method "POST"
                                       :headers {"Content-Type" "application/json"
                                                 "Accept" "application/json"}
                                       :json (cond-> {:client_id (:client-id config)
                                                     :client_secret (:client-secret config)
                                                     :code code}
                                               (configured? redirect-uri) (assoc :redirect_uri redirect-uri))}
                                :timeout-ms (or timeout-ms 30000)})
            body (checked-body! resp "GitHub token exchange")]
      (if (:error body)
        (throw (js/Error. (str "GitHub OAuth error: " (or (:error_description body) (:error body)))))
        (:access_token body))))
  (authenticated-user! [_ access-token]
    (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                               {:url "https://api.github.com/user"
                                :opts {:method "GET"
                                       :headers {"Authorization" (str "Bearer " access-token)
                                                 "Accept" "application/json"}}
                                :timeout-ms (or timeout-ms 30000)})]
      (checked-body! resp "GitHub user lookup")))
  (authenticated-emails! [_ access-token]
    (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                               {:url "https://api.github.com/user/emails"
                                :opts {:method "GET"
                                       :headers {"Authorization" (str "Bearer " access-token)
                                                 "Accept" "application/json"}}
                                :timeout-ms (or timeout-ms 30000)})]
      (checked-body! resp "GitHub email lookup"))))

(defn client
  ([config] (client config {}))
  ([config {:keys [http-client timeout-ms]}]
   (->FetchGitHubClient config (or http-client xfetch/default-client) (or timeout-ms 30000))))
