(ns knoxx.frontend.lib.api
  "Shared knoxx HTTP helper. CLJS port of `request` +
   `buildKnoxxAuthHeaders` from src/lib/api/core.ts: x-knoxx identity
   headers from localStorage, credentials include, JSON bodies, error
   text propagation, keywordized JSON results."
  (:require [clojure.string :as str]
            [knoxx.frontend.lib.storage :as storage]))

(defn- identity-value [storage-key]
  (some-> (storage/safe-get-item storage-key) str/trim not-empty))

(defn auth-headers
  "js/Headers with the x-knoxx identity headers when present."
  ^js []
  (let [headers (js/Headers.)]
    (when-let [email (identity-value "knoxx_user_email")]
      (.set headers "x-knoxx-user-email" email))
    (when-let [org (identity-value "knoxx_org_slug")]
      (.set headers "x-knoxx-org-slug" org))
    headers))

(defn- ^:async throw-error-text [^js res path]
  (let [text (await (.text res))]
    (throw (js/Error. (if (seq text)
                       text
                       (str "Request to " path " failed (" (.-status res) ")"))))))

(defn- fetch-init ^js [method body]
  (let [headers (auth-headers)
        init #js {:credentials "include" :headers headers}]
    (when method (set! (.-method init) method))
    (when (some? body)
      (.set headers "Content-Type" "application/json")
      (set! (.-body init) (js/JSON.stringify (clj->js body))))
    init))

(defn- ^:async raw-request [path {:keys [method body]} read-body]
  (let [^js res (await (js/fetch path (fetch-init method body)))]
    (if (.-ok res)
      (await (read-body res))
      (await (throw-error-text res path)))))

(defn ^:async request
  "Fetches `path`, resolving to the keywordized JSON body. `opts` may
   carry :method and :body (CLJS data, JSON-encoded)."
  ([path] (request path nil))
  ([path opts]
   (js->clj (await (raw-request path opts (fn [^js res] (.json res))))
            :keywordize-keys true)))

(defn request-text
  "Like `request` but resolves to the raw response text."
  ([path] (request-text path nil))
  ([path opts]
   (raw-request path opts (fn [^js res] (.text res)))))
