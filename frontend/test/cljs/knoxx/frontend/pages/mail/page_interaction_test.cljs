(ns knoxx.frontend.pages.mail.page-interaction-test
  "Port of src/pages/MailPage.test.tsx to the node :test build — INTERACTION
  tests (filter changes refetch, acknowledge calls the API and reloads),
  not just static renders. Uses jsdom globals + @testing-library/react
  against the bridge-free mail-page-body; the api namespace is mocked by
  set!-ing its vars."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.pages.mail.api :as api]
            [knoxx.frontend.pages.mail.page :as page]))

;; jsdom globals come from the :test build's :prepend-js (they must exist
;; before react-dom's module load — see shadow-cljs.edn).

;; ── api mocks ────────────────────────────────────────────────────────────────

(def ^:private list-calls (atom []))
(def ^:private ack-calls (atom []))

(defn- mailbox-response [status]
  {:ok true
   :box "inbox"
   :actor-id "actor-7"
   :durable true :capabilities {:send true :modes ["inbox-only"] :acknowledge true}
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

(t/use-fixtures :each
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
  (rtl/render (hx/$ page/mail-page-body {:initial-actor-id "actor-7"
                                 :navigate (fn [_])})))

(defn- wait-until
  "RTL waitFor retries while the callback THROWS — cljs.test `is` doesn't
   throw, so wrap the predicate and throw until it holds."
  [msg pred]
  (rtl/waitFor (fn [] (when-not (pred) (throw (js/Error. (str "still waiting: " msg)))))))

(defn- entry-visible? [^js r]
  (some? (.queryByText r "Fork Tales handoff is ready.")))

;; ── tests ────────────────────────────────────────────────────────────────────

(t/deftest ^:async loads-entries-and-refetches-on-filter-changes
  (let [r (render-page)]
    (await (wait-until "entry rendered" #(entry-visible? r)))
    (t/is (= [["inbox" "all"]] @list-calls) "initial load")
    (.change rtl/fireEvent (.getByLabelText r "Status") #js {:target #js {:value "pending"}})
    (await (wait-until "status refetch" #(= [["inbox" "all"] ["inbox" "pending"]] @list-calls)))
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "outbox"}))
    (await (wait-until "box refetch" #(= ["outbox" "pending"] (last @list-calls))))
    (t/is (= 3 (count @list-calls)) "three fetches total")))

(t/deftest ^:async acknowledges-entry-and-reloads
  (let [r (render-page)]
    (await (wait-until "entry rendered" #(entry-visible? r)))
    (.click rtl/fireEvent (.getByRole r "button" #js {:name "Acknowledge"}))
    (await (wait-until "ack called" #(= ["mail-1"] @ack-calls)))
    (await (wait-until "reloaded" #(= 2 (count @list-calls))))
    (t/is (= ["mail-1"] @ack-calls))
    (t/is (= 2 (count @list-calls)) "initial load + post-ack reload")))
