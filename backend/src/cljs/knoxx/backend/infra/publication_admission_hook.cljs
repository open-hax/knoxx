(ns knoxx.backend.infra.publication-admission-hook
  "Cycle-free callback from generated resource tools into document admission.")

(defonce ^:private admit-handler* (atom nil))

(defn register!
  "Install the application-composed exact-document admission handler."
  [handler]
  (when-not (fn? handler)
    (throw (ex-info "publication admission handler must be callable" {})))
  (reset! admit-handler* handler)
  true)

(defn clear!
  []
  (reset! admit-handler* nil))

(defn configured?
  []
  (fn? @admit-handler*))

(defn admit!
  [scope selection]
  (if-let [handler @admit-handler*]
    (handler scope selection)
    (throw (ex-info "publication document admission is not configured"
                    {:code :publication-admission-unconfigured}))))
