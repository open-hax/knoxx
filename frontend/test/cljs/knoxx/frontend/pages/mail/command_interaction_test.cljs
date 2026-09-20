(ns knoxx.frontend.pages.mail.command-interaction-test
  "Human commands and agent invalidations preserve canonical content and unfinished work."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as t]
            [helix.core :as hx]
            [knoxx.frontend.pages.mail.api :as api]
            [knoxx.frontend.pages.mail.page :as page]))

(def ^:private entry {:id "mail-1" :status "delivered" :preview "Short preview"})
(def ^:private response {:box "inbox" :actor-id "actor-1" :entries [entry] :durable true
                        :capabilities {:send true :modes ["inbox-only" "follow-up"] :acknowledge true}})
(def ^:private originals {:list api/list-mailbox :send api/send-message :read api/read-entry :subscribe api/subscribe!})
(def ^:private changed (atom nil))
(def ^:private closed (atom false))
(def ^:private calls (atom []))

(t/use-fixtures :each
  {:before (fn []
             (reset! calls []) (reset! changed nil) (reset! closed false)
             (set! api/list-mailbox (fn [_ _] (js/Promise.resolve response)))
             (set! api/read-entry (fn [_] (js/Promise.resolve (assoc entry :content "Canonical full body"))))
             (set! api/subscribe! (fn [callback _] (reset! changed callback) #(reset! closed true))))
   :after (fn [] (rtl/cleanup)
            (set! api/list-mailbox (:list originals)) (set! api/send-message (:send originals))
            (set! api/read-entry (:read originals)) (set! api/subscribe! (:subscribe originals)))})

(defn- wait-for [predicate]
  (rtl/waitFor (fn [] (when-not (predicate) (throw (js/Error. "Waiting for actual rendered state"))))))
(defn- render! [] (rtl/render (hx/$ page/mail-page-body {:initial-actor-id "actor-1" :navigate (fn [_])})))
(defn- fill! [^js rendered label value]
  (.change rtl/fireEvent (.getByLabelText rendered label) #js {:target #js {:value value}}))
(defn- click! [^js rendered label] (.click rtl/fireEvent (.getByRole rendered "button" #js {:name label})))

(t/deftest ^:async unchanged-uncertain-send-reuses-operation-and-preserves-human-draft
  (set! api/send-message
        (fn [payload]
          (swap! calls conj payload)
          (if (= 1 (count @calls)) (js/Promise.reject (js/Error. "Receipt unavailable"))
              (js/Promise.resolve {:ok true :entry entry}))))
  (let [^js rendered (render!)]
    (await (wait-for #(.queryByText rendered "Short preview")))
    (fill! rendered "Recipient" "actor:recipient-2") (fill! rendered "Message" "Full handoff from human")
    (click! rendered "Send message")
    (await (wait-for #(.queryByText rendered "Receipt unavailable")))
    (t/is (= "Full handoff from human" (.-value (.getByLabelText rendered "Message"))))
    (click! rendered "Send message")
    (await (wait-for #(.queryByText rendered "Message delivered.")))
    (t/is (= 2 (count @calls)))
    (t/is (= (first @calls) (second @calls)) "Same operation and intent retries an uncertain receipt")
    (t/is (= #{:operation_id :target :content :mode} (set (keys (first @calls)))))
    (t/is (= "" (.-value (.getByLabelText rendered "Message"))))))

(t/deftest ^:async agent-invalidation-refreshes-open-full-content-without-erasing-compose-draft
  (let [body (atom "Canonical full body") ^js rendered (render!)]
    (set! api/read-entry (fn [id] (swap! calls conj id) (js/Promise.resolve (assoc entry :content @body))))
    (await (wait-for #(.queryByText rendered "Short preview")))
    (fill! rendered "Message" "Unsent editorial note")
    (click! rendered "Read full message")
    (await (wait-for #(.queryByText rendered "Canonical full body")))
    (reset! body "Updated by an agent through the canonical provider")
    (await (rtl/act (fn ^:async receive-change [] (await (@changed)))))
    (await (wait-for #(.queryByText rendered @body)))
    (t/is (= ["mail-1" "mail-1"] @calls))
    (t/is (= "Unsent editorial note" (.-value (.getByLabelText rendered "Message"))))
    (.unmount rendered) (t/is @closed "Live subscriptions are closed with the view")))

(t/deftest ^:async current-policy-refusal-disables-corresponding-human-command
  (set! api/list-mailbox (fn [_ _] (js/Promise.resolve (assoc response :capabilities {:send false :modes [] :acknowledge false}))))
  (set! api/send-message (fn [payload] (swap! calls conj payload)))
  (let [^js rendered (render!)]
    (await (wait-for #(.queryByText rendered "Short preview")))
    (fill! rendered "Recipient" "actor:recipient-2") (fill! rendered "Message" "A denied command")
    (t/is (.-disabled (.getByRole rendered "button" #js {:name "Send message"})))
    (t/is (nil? (.queryByRole rendered "button" #js {:name "Acknowledge"})))
    (t/is (empty? @calls))))

(t/deftest ^:async an-old-inbox-response-cannot-replace-a-new-outbox-view
  (let [release (atom nil)]
    (set! api/list-mailbox
          (fn [box _] (if (= box "inbox") (js/Promise. #(reset! release %1))
                          (js/Promise.resolve (assoc response :box "outbox" :entries [(assoc entry :preview "Current outbox")])))))
    (let [^js rendered (render!)]
      (click! rendered "outbox")
      (await (wait-for #(.queryByText rendered "Current outbox")))
      (await (rtl/act (fn ^:async settle-inbox [] (@release response) (await (js/Promise.resolve)))))
      (t/is (some? (.queryByText rendered "Current outbox")))
      (t/is (nil? (.queryByText rendered "Short preview"))))))

(t/deftest ^:async completing-a-send-refreshes-the-current-filter-instead-of-its-old-closure
  (let [release (atom nil)]
    (set! api/send-message (fn [_] (js/Promise. #(reset! release %1))))
    (set! api/list-mailbox (fn [box status] (swap! calls conj [box status])
                            (js/Promise.resolve (assoc response :box box))))
    (let [^js rendered (render!)]
      (await (wait-for #(.queryByText rendered "Short preview")))
      (fill! rendered "Recipient" "actor:recipient") (fill! rendered "Message" "Handoff")
      (click! rendered "Send message") (click! rendered "outbox")
      (await (wait-for #(= ["outbox" "all"] (last @calls))))
      (await (rtl/act (fn ^:async settle-send [] (@release {:ok true :entry entry}) (await (js/Promise.resolve)))))
      (await (wait-for #(.queryByText rendered "Message delivered.")))
      (t/is (= [["inbox" "all"] ["outbox" "all"] ["outbox" "all"]] @calls)))))
