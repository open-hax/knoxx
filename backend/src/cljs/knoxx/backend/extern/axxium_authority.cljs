(ns knoxx.backend.extern.axxium-authority
  "The fixed Axxium authority configuration boundary; callers cannot choose a password destination."
  (:require [clojure.string :as str]))
(defn configured-origin "Read a fixed HTTPS authority, or loopback HTTP for local development." []
  (when-let [value (some-> (aget (.-env js/process) "KNOXX_AXXIUM_ORIGIN") str/trim not-empty)]
    (let [url (js/URL. value)]
      (when-not (and (or (= "https:" (.-protocol url))
                         (and (= "http:" (.-protocol url))
                              (contains? #{"localhost" "127.0.0.1" "[::1]"}
                                         (.-hostname url))))
                     (= "/" (.-pathname url))
                     (str/blank? (.-username url)) (str/blank? (.-password url))
                     (str/blank? (.-search url)) (str/blank? (.-hash url)))
        (throw (ex-info "KNOXX_AXXIUM_ORIGIN must be HTTPS or loopback HTTP" {:status 503})))
      (.-origin url))))
