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

(defn- throw-error-text [^js res path]
  (-> (.text res)
      (.then (fn [text]
               (throw (js/Error. (if (seq text)
                                   text
                                   (str "Request to " path " failed (" (.-status res) ")"))))))))

(defn- fetch-init ^js [method body]
  (let [headers (auth-headers)
        init #js {:credentials "include" :headers headers}]
    (when method (set! (.-method init) method))
    (when (some? body)
      (.set headers "Content-Type" "application/json")
      (set! (.-body init) (js/JSON.stringify (clj->js body))))
    init))

(defn- raw-request [path {:keys [method body]} read-body]
  (-> (js/fetch path (fetch-init method body))
      (.then (fn [^js res]
               (if (.-ok res)
                 (read-body res)
                 (throw-error-text res path))))))

(defn request
  "Fetches `path`, resolving to the keywordized JSON body. `opts` may
   carry :method and :body (CLJS data, JSON-encoded)."
  ([path] (request path nil))
  ([path opts]
   (-> (raw-request path opts (fn [^js res] (.json res)))
       (.then #(js->clj % :keywordize-keys true)))))

(defn request-text
  "Like `request` but resolves to the raw response text."
  ([path] (request-text path nil))
  ([path opts]
   (raw-request path opts (fn [^js res] (.text res)))))
