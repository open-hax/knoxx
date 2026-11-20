(ns knoxx.backend.domain.social.api-response
  "Preserve platform response data while withholding authentication secrets."
  (:require [clojure.string :as str]))

(def ^:private credential-fields
  #{"token" "accessjwt" "refreshjwt" "access_token" "refresh_token"
    "password" "secret" "client_secret" "authorization" "bot_token"})

(defn redact
  "Return sanitized JSON-shaped data and the number of withheld credential fields."
  ([value] (redact value []))
  ([value secrets]
  (cond
    (map? value)
    (reduce-kv (fn [{:keys [body redacted-fields]} key entry]
                 (if (contains? credential-fields (str/lower-case (if (keyword? key) (name key) (str key))))
                   {:body (assoc body key "[credential withheld]") :redacted-fields (inc redacted-fields)}
                   (let [child (redact entry secrets)]
                     {:body (assoc body key (:body child))
                      :redacted-fields (+ redacted-fields (:redacted-fields child))})))
               {:body {} :redacted-fields 0} value)
    (vector? value)
    (let [parts (mapv #(redact % secrets) value)]
      {:body (mapv :body parts) :redacted-fields (reduce + (map :redacted-fields parts))})
    (string? value)
    (let [safe (reduce #(str/replace %1 %2 "[credential withheld]") value
                       (filter #(and (string? %) (seq %)) secrets))]
      {:body safe :redacted-fields (if (= safe value) 0 1)})
    :else {:body value :redacted-fields 0})))
