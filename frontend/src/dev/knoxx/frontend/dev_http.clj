(ns knoxx.frontend.dev-http
  (:require [clojure.string :as str]))

(defn proxy-predicate
  "Decide which requests should be proxied from the shadow-cljs dev HTTP server
  to the backend.

  We keep shadow's own devtools endpoints local (e.g. /api/remote-relay), while
  sending application API + WS traffic to the backend.

  Signature required by shadow-cljs: (fn [undertow-exchange config] boolean)."
  [ex _config]
  (let [path ^String (.getRequestPath ex)]
    (cond
      ;; shadow-cljs devtools endpoints (must NOT be proxied)
      (str/starts-with? path "/api/remote-") false
      (= path "/api/open-file") false

      ;; application backend endpoints (should be proxied)
      (str/starts-with? path "/api/") true
      (str/starts-with? path "/ws/") true
      (= path "/health") true

      :else false)))
