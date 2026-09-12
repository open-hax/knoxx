(ns knoxx.frontend.lib.contracts-view
  "Named React boundary for decoded contract view state, events and opaque editor/chat handles."
  (:require [clojure.string :as str]))

(defn- decode [value] (js->clj value :keywordize-keys true))

(defn decode-controller
  "Decode plain contract state and actions while retaining the chat handle as opaque."
  [^js raw]
  {:state (decode (.-state raw)) :actions (decode (.-actions raw)) :chat (.-chat raw)})

(defn input-value
  "Read a native form event's current string value."
  [^js event]
  (.. event -target -value))

(defn input-checked?
  "Read a native checkbox event's current boolean value."
  [^js event]
  (boolean (.. event -target -checked)))

(defn hover-entry!
  "Retain the unselected contract row's existing hover treatment."
  [^js event selected? entering?]
  (when-not selected?
    (set! (.. event -currentTarget -style -background)
          (if entering? "rgba(255,255,255,0.03)" "transparent"))))

(defn editor-props
  "Encode CodeMirror diagnostics at its native component boundary."
  [state actions]
  {:value (:edn-draft state) :onChange (:set-draft actions) :height "100%"
   :placeholder "Enter EDN contract…" :fileName (or (:selected-id state) "new-contract.edn")
   :onValidate (:validate actions)
   :externalErrors (clj->js (mapv (fn [{:keys [path message]}]
                                  {:message (if (seq path) (str (str/join "." path) ": " message) message)})
                                (:validation-errors state)))})

(defn chat-props
  "Keep the librarian's opaque chat handle and decode its source callbacks."
  [chat open-source!]
  {:controller chat :showFiles false :showCanvasToggle false :onShowFiles #()
   :onOpenHydrationSource (fn [^js source] (open-source! (.-path source)))
   :onOpenSourceInPreview (fn [^js source] (open-source! (.-url source)))})
