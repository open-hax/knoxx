(ns knoxx.backend.extern.native-subscription
  "Adapt the native Claude worker to the Knoxx provider event and transcript shapes."
  (:require ["#knoxx-native-claude" :as claude]))

(defn abort-controller []
  (js/AbortController.))

(defn abort! [controller]
  (.abort controller))

(defn ^:async run-turn!
  [{:keys [model home]} controller prompt ready delta]
  (let [result (await (claude/runTurn #js {:model model
                                          :prompt prompt
                                          :signal (.-signal controller)
                                          :home home
                                          :ready ready
                                          :delta delta}))
        usage (js->clj (.-usage result) :keywordize-keys true)
        text (.-text result)]
    (when-not (and (string? text)
                   (seq text)
                   (map? usage)
                   (every? #(and (number? %) (js/Number.isFinite %) (not (neg? %))) (vals usage)))
      (throw (js/Error. "Native subscription worker returned an invalid result")))
    {:text text :usage usage}))

(defn text-delta [delta]
  #js {:type "message_update"
       :assistantMessageEvent #js {:type "text_delta" :delta delta}})

(defn transcript-message [{:keys [role text usage]}]
  #js {:role role
       :content (if (= role "assistant") #js [#js {:type "text" :text text}] text)
       :usage (when usage (clj->js usage))})
