(ns knoxx.frontend.infra.navigation-guard
  "Browser and router navigation protection for unfinished human work."
  (:require [helix.hooks :as hooks]))

(defn request-sign-out!
  "Allow mounted editors to cancel sign-out before authentication changes."
  []
  (.dispatchEvent js/window (js/window.Event. "knoxx:before-sign-out" #js {:cancelable true})))

(defn- indexed-state [state]
  (let [index (some-> ^js state .-idx)]
    (when (js/Number.isInteger index) index)))

(defn- navigation-link [^js event]
  (when (and (= 0 (.-button event)) (not (.-defaultPrevented event))
             (not (or (.-metaKey event) (.-ctrlKey event) (.-shiftKey event) (.-altKey event))))
    (when-let [anchor (some-> ^js (.-target event) (.closest "a[href]"))]
      (when (and (not (.hasAttribute anchor "download"))
                 (contains? #{"" "_self" nil} (.getAttribute anchor "target")))
        (let [destination (js/URL. (.-href anchor) (.-href (.-location js/window)))]
          (when (not= (str (.-origin destination) (.-pathname destination) (.-search destination))
                      (str (.-origin (.-location js/window)) (.-pathname (.-location js/window)) (.-search (.-location js/window))))
            anchor))))))

(defn- history-listener [index restoring? confirm!]
  (fn [^js event]
               (let [next-index (indexed-state (.-state event)) previous @index]
                 (cond
                   @restoring? (do (reset! restoring? false) (reset! index next-index)
                                   (.stopImmediatePropagation event))
                   (and (some? previous) (some? next-index) (not= previous next-index)
                        (not (confirm!)))
                   (do (.stopImmediatePropagation event) (reset! restoring? true)
                       (.go (.-history js/window) (- previous next-index)))
                   :else (reset! index next-index)))))

(defn- click-listener [dirty? confirm! allow-unload? approval-timer]
  (fn [^js event]
                 (when (and (navigation-link event) (dirty?))
                   (if (confirm!)
                     (do (reset! allow-unload? true)
                         (reset! approval-timer
                                 (js/setTimeout #(reset! allow-unload? false) 0)))
                     (do (.preventDefault event) (.stopImmediatePropagation event))))))

(defn- track-history! [index]
  (let [original-push (.-pushState (.-history js/window)) original-replace (.-replaceState (.-history js/window))
        wrap (fn [original]
               (fn [& args]
                 (.apply original (.-history js/window) (to-array args))
                 (reset! index (indexed-state (.-state (.-history js/window))))))
        push! (wrap original-push) replace! (wrap original-replace)]
    (set! (.-pushState (.-history js/window)) push!)
    (set! (.-replaceState (.-history js/window)) replace!)
    (fn []
      (when (= push! (.-pushState (.-history js/window))) (set! (.-pushState (.-history js/window)) original-push))
      (when (= replace! (.-replaceState (.-history js/window))) (set! (.-replaceState (.-history js/window)) original-replace)))))

(defn install!
  "Guard links, sign-out, page exit and indexed Back/Forward until cleanup."
  [dirty?]
  (let [allow-unload? (atom false)
        approval-timer (atom nil)
        index (atom (indexed-state (.-state (.-history js/window))))
        restoring? (atom false)
        confirm! #(or (not (dirty?)) (.confirm js/window "Discard your unsaved changes?"))
        click! (click-listener dirty? confirm! allow-unload? approval-timer)
        unload! (fn [^js event]
                  (when (and (dirty?) (not @allow-unload?))
                    (.preventDefault event) (set! (.-returnValue event) "")))
        sign-out! (fn [^js event] (when-not (confirm!) (.preventDefault event)))
        history-pop! (history-listener index restoring? confirm!)
        restore-history! (track-history! index)]
    (.addEventListener js/document "click" click! true)
    (.addEventListener js/window "beforeunload" unload!)
    (.addEventListener js/window "knoxx:before-sign-out" sign-out!)
    (.addEventListener js/window "popstate" history-pop! true)
    (fn []
      (.removeEventListener js/document "click" click! true)
      (.removeEventListener js/window "beforeunload" unload!)
      (.removeEventListener js/window "knoxx:before-sign-out" sign-out!)
      (.removeEventListener js/window "popstate" history-pop! true)
      (when @approval-timer (js/clearTimeout @approval-timer))
      (restore-history!))))

(defn use-navigation-guard!
  "Use current draft state without reinstalling browser listeners on each keystroke."
  [dirty?]
  (let [^js latest (hooks/use-ref dirty?)]
    (set! (.-current latest) dirty?)
    (hooks/use-effect :once (install! #(.-current latest)))))
