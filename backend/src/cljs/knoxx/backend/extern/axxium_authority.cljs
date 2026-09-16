(ns knoxx.backend.extern.axxium-authority
  "The fixed Axxium authority configuration boundary; callers cannot choose a password destination."
  (:require [clojure.string :as str]))
(defn configured-origin "Read and validate the operator's HTTPS authority origin." []
  (when-let [value (some-> (aget (.-env js/process) "KNOXX_AXXIUM_ORIGIN") str/trim not-empty)]
    (let [url (js/URL. value)]
      (when-not (and (= "https:" (.-protocol url)) (= "/" (.-pathname url))
                     (str/blank? (.-username url)) (str/blank? (.-password url))
                     (str/blank? (.-search url)) (str/blank? (.-hash url)))
        (throw (ex-info "KNOXX_AXXIUM_ORIGIN must be an HTTPS origin" {:status 503})))
      (.-origin url))))
