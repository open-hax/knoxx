(ns knoxx.backend.infra.clients.openplanner
  "OpenPlanner service client boundary.

   Callers should depend on IOpenPlannerClient instead of constructing
   OpenPlanner URLs, headers, JSON bodies, or native fetch calls inline."
  (:require [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.json :as xjson]
            [promesa.core :as p]))

(defprotocol IOpenPlannerClient
  (enabled? [client]
    "True when the client has enough configuration to call OpenPlanner.")
  (url-for [client suffix]
    "Return the OpenPlanner URL for suffix.")
  (request! [client method suffix body]
    "Execute an OpenPlanner JSON request. Resolves to CLJS response body or rejects."))

(defn trim-trailing-slashes
  [s]
  (str/replace (str (or s "")) #"/+$" ""))

(defn headers-for
  [config]
  {"Content-Type" "application/json"
   "Authorization" (str "Bearer " (:openplanner-api-key config))
   "X-Tenant-ID" (or (:session-project-name config) "knoxx-session")})

(defn configured?
  [config]
  (and (not (str/blank? (:openplanner-base-url config)))
       (not (str/blank? (:openplanner-api-key config)))))

(defrecord FetchOpenPlannerClient [config http-client timeout-ms]
  IOpenPlannerClient
  (enabled? [_]
    (configured? config))
  (url-for [_ suffix]
    (str (trim-trailing-slashes (:openplanner-base-url config)) suffix))
  (request! [client method suffix body]
    (if-not (enabled? client)
      (js/Promise.reject (js/Error. "OpenPlanner is not configured"))
      (let [opts (cond-> {:method method
                          :headers (headers-for config)}
                   body (assoc :body (xjson/stringify body)))]
        (p/let [resp (xfetch/json! (or http-client xfetch/default-client)
                                   {:url (url-for client suffix)
                                    :opts opts
                                    :timeout-ms (or timeout-ms 60000)})]
          (if (:ok resp)
            (:body resp)
            (throw (js/Error. (str "OpenPlanner request failed ("
                                   (:status resp)
                                   "): "
                                   (pr-str (:body resp)))))))))))

(defn client
  ([config] (client config {}))
  ([config {:keys [http-client timeout-ms]}]
   (->FetchOpenPlannerClient config (or http-client xfetch/default-client) (or timeout-ms 60000))))
