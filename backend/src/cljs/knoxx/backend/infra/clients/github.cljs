(ns knoxx.backend.infra.clients.github
  "GitHub OAuth/user API client protocol.

   Covers the auth/session namespace requests to GitHub's OAuth token endpoint
   and user/email APIs. Implementations should return CLJS maps and keep fetch,
   headers, and JSON conversion inside the client.")

(defprotocol IGitHubClient
  (oauth-access-token! [client code redirect-uri])
  (authenticated-user! [client access-token])
  (authenticated-emails! [client access-token]))
