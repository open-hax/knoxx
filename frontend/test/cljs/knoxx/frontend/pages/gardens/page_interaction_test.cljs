(ns knoxx.frontend.pages.gardens.page-interaction-test
  "Port of src/pages/GardensPage.test.tsx to the node :test build —
  create-garden flow and delete-with-inline-confirmation flow, via jsdom +
  @testing-library/react against the gardens page (no router/auth deps).
  The api namespace is mocked by set!-ing its vars; the POST payload
  itself is covered by build-save-request unit tests."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.pages.gardens.api :as api]
            [knoxx.frontend.pages.gardens.view :refer [gardens-page]]))

;; jsdom globals come from the :test build's :prepend-js (they must exist
;; before react-dom's module load — see shadow-cljs.edn).

(def fork-garden
  {:garden_id "fork-garden"
   :title "Fork Garden"
   :description "Existing garden"
   :status "active"
   :theme "monokai"
   :target_languages ["es"]
   :auto_translate true})

(def list-results (atom []))
(def list-calls (atom 0))
(def save-calls (atom []))
(def delete-calls (atom []))

(def ^:private real-list api/list-gardens)
(def ^:private real-save api/save-garden)
(def ^:private real-delete api/delete-garden)

(use-fixtures :each
  {:before (fn []
             (reset! list-calls 0)
             (reset! save-calls [])
             (reset! delete-calls [])
             (set! api/list-gardens
                   (fn []
                     (swap! list-calls inc)
                     (js/Promise.resolve {:ok true
                                          :count (count @list-results)
                                          :gardens @list-results})))
             (set! api/save-garden
                   (fn [req]
                     (swap! save-calls conj req)
                     (js/Promise.resolve {:ok true})))
             (set! api/delete-garden
                   (fn [id]
                     (swap! delete-calls conj id)
                     (js/Promise.resolve {:ok true}))))
   :after (fn []
            (rtl/cleanup)
            (set! api/list-gardens real-list)
            (set! api/save-garden real-save)
            (set! api/delete-garden real-delete))})

(defn- wait-until [msg pred]
  (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))))

(defn- by-role-name [^js r role nm]
  (.getByRole r role #js {:name nm}))

(deftest creates-garden-and-refreshes
  (reset! list-results [])
  (async done
    (let [r (rtl/render ($ gardens-page))]
      (-> (wait-until "initial load" #(pos? @list-calls))
          (.then (fn []
                   (.click rtl/fireEvent (by-role-name r "button" "+ New Garden"))
                   (.change rtl/fireEvent (.getByPlaceholderText r "my-garden-id")
                            #js {:target #js {:value "new-garden"}})
                   (.change rtl/fireEvent (.getByPlaceholderText r "My Garden")
                            #js {:target #js {:value "New Garden"}})
                   (.change rtl/fireEvent (.getByPlaceholderText r "Brief description of this garden")
                            #js {:target #js {:value "A test garden"}})
                   (.click rtl/fireEvent (by-role-name r "button" "Español"))
                   (.click rtl/fireEvent (by-role-name r "button" "Create Garden"))
                   (wait-until "success notice"
                               #(some? (.queryByText r "Garden \"New Garden\" created successfully")))))
          (.then (fn []
                   (let [{:keys [url method body]} (first @save-calls)]
                     (is (= "/api/openplanner/v1/gardens" url))
                     (is (= "POST" method))
                     (is (= {:garden_id "new-garden"
                             :title "New Garden"
                             :description "A test garden"
                             :theme "monokai"
                             :target_languages ["es"]
                             :auto_translate true}
                            body)))
                   (is (= 2 @list-calls) "list reloaded after create")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest delete-requires-inline-confirmation
  (reset! list-results [fork-garden])
  (async done
    (let [r (rtl/render ($ gardens-page))]
      (-> (wait-until "garden rendered" #(some? (.queryByText r "Fork Garden")))
          (.then (fn []
                   (.click rtl/fireEvent (by-role-name r "button" "Delete"))
                   (is (= [] @delete-calls) "no DELETE before confirmation")
                   (wait-until "confirm visible"
                               #(some? (.queryByText r "Confirm Delete")))))
          (.then (fn []
                   (.click rtl/fireEvent (by-role-name r "button" "Confirm Delete"))
                   (wait-until "deleted notice"
                               #(some? (.queryByText r "Garden \"fork-garden\" deleted")))))
          (.then (fn []
                   (is (= ["fork-garden"] @delete-calls))
                   (is (= 2 @list-calls) "list reloaded after delete")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
