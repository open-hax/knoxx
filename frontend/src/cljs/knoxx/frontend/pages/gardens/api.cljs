(ns knoxx.frontend.pages.gardens.api
  "Gardens REST calls. CLJS port of the bare fetches in
   src/pages/GardensPage.tsx (the OpenPlanner gardens endpoints take no
   knoxx auth headers).")

(defn- json-or-throw [^js res fallback-msg]
  (if (.-ok res)
    (-> (.json res) (.then #(js->clj % :keywordize-keys true)))
    (-> (.text res)
        (.then (fn [text]
                 (throw (js/Error. (if (seq text)
                                     text
                                     (str fallback-msg (.-status res))))))))))

(defn list-gardens []
  (-> (js/fetch "/api/openplanner/v1/gardens")
      (.then #(json-or-throw % "Failed to load gardens: "))))

(defn save-garden
  "Executes a {:url :method :body} request built by logic/build-save-request."
  [{:keys [url method body]}]
  (-> (js/fetch url #js {:method method
                         :headers #js {"Content-Type" "application/json"}
                         :body (js/JSON.stringify (clj->js body))})
      (.then #(json-or-throw % "Failed to save garden: "))))

(defn delete-garden [garden-id]
  (-> (js/fetch (str "/api/openplanner/v1/gardens/" (js/encodeURIComponent garden-id))
                #js {:method "DELETE"})
      (.then #(json-or-throw % "Failed to delete garden: "))))
