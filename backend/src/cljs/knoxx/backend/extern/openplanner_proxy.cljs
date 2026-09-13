(ns knoxx.backend.extern.openplanner-proxy
  "Request/response transport for the explicitly authorized legacy proxy."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.law.local-openplanner :as law]))

(defn forward-request
  "Bind authoritative tenant query/body fields without forwarding identity headers."
  [request scope]
  (law/scope! scope)
  (let [query (merge (dissoc (fastify/request-query request) :org_id :org-id :orgId :project) scope)
        params (js/URLSearchParams.)
        method (fastify/request-method request)
        body (fastify/request-body request)]
    (doseq [[key value] query :when (some? value)]
      (.append params (name key) (str value)))
    (cond-> {:method method :path (fastify/request-param request :*)
             :query-string (str "?" (.toString params)) :headers {}}
      (not (contains? #{"GET" "HEAD"} method))
      (assoc :body (js/JSON.stringify (clj->js (merge (if (map? body) body {}) scope)))))))

(defn ^:async send-response!
  "Forward the selected provider's actual HTTP status and response body."
  [reply response]
  (let [content-type (or (some-> response .-headers (.get "content-type")) "application/json")
        body (await (if (str/includes? content-type "application/json") (.json response) (.text response)))]
    (.header reply "content-type" content-type)
    (.send (.code reply (.-status response)) body)))
