(ns knoxx.frontend.pages.mail.page-interaction-test
  "Port of src/pages/MailPage.test.tsx to the node :test build — INTERACTION
  tests (filter changes refetch, acknowledge calls the API and reloads),
  not just static renders. Uses jsdom globals + @testing-library/react
  against the bridge-free mail-page-body; the api namespace is mocked by
  set!-ing its vars."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            ["@testing-library/react" :as rtl]
            [helix.core :refer [$]]
            [knoxx.frontend.pages.mail.api :as api]
            [knoxx.frontend.pages.mail.page :refer [mail-page-body]]))

;; jsdom globals come from the :test build's :prepend-js (they must exist
;; before react-dom's module load — see shadow-cljs.edn).

;; ── api mocks ────────────────────────────────────────────────────────────────

(def list-calls (atom []))
(def ack-calls (atom []))

(defn- mailbox-response [status]
  {:ok true
   :box "inbox"
   :actor-id "actor-7"
   :entries [{:id "mail-1"
              :kind "actor-message"
              :status status
              :source {:actor-id "agent-a"}
              :target {:actor-id "actor-7"}
              :delivery {:mode "handoff"}
              :contentRef {}
              :metadata {}
              :preview "Fork Tales handoff is ready."}]})

(def ^:private real-list-mailbox api/list-mailbox)
(def ^:private real-acknowledge api/acknowledge-entry)

(use-fixtures :each
  {:before (fn []
             (reset! list-calls [])
             (reset! ack-calls [])
             (set! api/list-mailbox
                   (fn [box status]
                     (swap! list-calls conj [box status])
                     (js/Promise.resolve (mailbox-response "pending"))))
             (set! api/acknowledge-entry
                   (fn [id]
                     (swap! ack-calls conj id)
                     (js/Promise.resolve {:ok true}))))
   :after (fn []
            (rtl/cleanup)
            (set! api/list-mailbox real-list-mailbox)
            (set! api/acknowledge-entry real-acknowledge))})

(defn- render-page []
  (rtl/render ($ mail-page-body {:initial-actor-id "actor-7"
                                 :navigate (fn [_])})))

(defn- wait-until
  "RTL waitFor retries while the callback THROWS — cljs.test `is` doesn't
   throw, so wrap the predicate and throw until it holds."
  [msg pred]
  (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))))

(defn- entry-visible? [^js r]
  (some? (.queryByText r "Fork Tales handoff is ready.")))

;; ── tests ────────────────────────────────────────────────────────────────────

(deftest loads-entries-and-refetches-on-filter-changes
  (async done
    (let [r (render-page)]
      (-> (wait-until "entry rendered" #(entry-visible? r))
          (.then (fn []
                   (is (= [["inbox" "all"]] @list-calls) "initial load")
                   (.change rtl/fireEvent (.getByLabelText r "Status")
                            #js {:target #js {:value "pending"}})
                   (wait-until "status refetch"
                               #(= [["inbox" "all"] ["inbox" "pending"]] @list-calls))))
          (.then (fn []
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "outbox"}))
                   (wait-until "box refetch" #(= ["outbox" "pending"] (last @list-calls)))))
          (.then (fn []
                   (is (= 3 (count @list-calls)) "three fetches total")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))

(deftest acknowledges-entry-and-reloads
  (async done
    (let [r (render-page)]
      (-> (wait-until "entry rendered" #(entry-visible? r))
          (.then (fn []
                   (.click rtl/fireEvent (.getByRole r "button" #js {:name "Acknowledge"}))
                   (wait-until "ack called" #(= ["mail-1"] @ack-calls))))
          (.then (fn []
                   (wait-until "reloaded" #(= 2 (count @list-calls)))))
          (.then (fn []
                   (is (= ["mail-1"] @ack-calls))
                   (is (= 2 (count @list-calls)) "initial load + post-ack reload")
                   (done)))
          (.catch (fn [err] (is false (str "unexpected: " err)) (done)))))))
