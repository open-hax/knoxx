(ns knoxx.frontend.infra.navigation-guard-test
  "Unsaved work can veto sign-out and link navigation until its guard is removed."
  (:require [cljs.test :as t]
            [knoxx.frontend.infra.navigation-guard :as navigation]))

(t/deftest sign-out-is-cancellable-without-clearing-authentication
  (let [original (.-confirm js/window) dirty? (atom true)]
    (set! (.-confirm js/window) (constantly false))
    (let [cleanup (navigation/install! #(deref dirty?))]
      (try
        (t/is (false? (navigation/request-sign-out!)))
        (reset! dirty? false)
        (t/is (true? (navigation/request-sign-out!)))
        (finally (cleanup) (set! (.-confirm js/window) original))))))

(t/deftest a-cancelled-link-does-not-reach-the-router
  (let [original (.-confirm js/window)
        anchor (.createElement js/document "a")]
    (.setAttribute anchor "href" "https://example.test/unsaved-navigation-target")
    (.appendChild (.-body js/document) anchor)
    (set! (.-confirm js/window) (constantly false))
    (let [cleanup (navigation/install! (constantly true))
          event (js/window.MouseEvent. "click" #js {:button 0 :bubbles true :cancelable true})]
      (try
        (t/is (false? (.dispatchEvent anchor event)))
        (t/is (.-defaultPrevented event))
        (finally (cleanup) (.remove anchor) (set! (.-confirm js/window) original))))))

(t/deftest cleanup-restores-native-history-and-removes-the-veto
  (let [push (.-pushState (.-history js/window)) original-replace (.-replaceState (.-history js/window))
        cleanup (navigation/install! (constantly true))]
    (t/is (not= push (.-pushState (.-history js/window))))
    (cleanup)
    (t/is (= push (.-pushState (.-history js/window))))
    (t/is (= original-replace (.-replaceState (.-history js/window))))
    (t/is (navigation/request-sign-out!))))
