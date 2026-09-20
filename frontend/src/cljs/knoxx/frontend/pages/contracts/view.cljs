(ns knoxx.frontend.pages.contracts.view
  "Native contract editor and librarian view over the existing controller boundary."
  (:require [clojure.string :as str]
            [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.lib.contracts-view :as boundary]
            [knoxx.frontend.pages.contracts.library :as library]
            [knoxx.frontend.pages.contracts.style :as style]))

(hx/defnc selected-heading
  "Preserve the selected contract's class, runtime state and source context."
  [{:keys [state]}]
  (let [entry (:selected-entry state)
        status (:status entry)
        color-path (case status "running" [:accent :green] "idle" [:accent :cyan]
                         "disabled" [:fg :muted] "error" [:accent :red] [:fg :soft])]
    (d/div {:style (merge style/row {:gap 12 :minWidth 0})}
      (when entry
        (d/span {:style {:color (apply style/color state color-path) :fontSize 14}}
          (get {"running" "●" "idle" "◐" "disabled" "○" "error" "✕"} status "·")))
      (d/span {:style {:fontSize (style/token state :fontSize :base) :fontWeight 600 :color (style/color state :fg :default)}}
        (if entry (:label entry) "New contract"))
      (when entry
        (d/span {:style (style/small state)}
          (str (:contractClass entry) " · " (:triggerKind entry) " · " (:sourceKind entry)))))))

(hx/defnc toolbar
  "Expose the same validation, persistence, copy and panel controls."
  [{:keys [state actions]}]
  (let [saving? (:saving state) disabled-save? (or saving? (not (:is-dirty state)))
        buttons [[:toggle-left-panel (if (:show-left-panel state) "✕ Left" "☰ Left") false
                  (style/active-button state (:show-left-panel state))]
                 [:refresh (if (:loading-agents state) "Loading…" "Refresh") (:loading-agents state) (style/button state)]
                 [:validate (if (:validating state) "Validating…" "✓ Validate") (:validating state)
                  (assoc (style/button state) :border (str "1px solid " (style/color state :accent :green))
                         :background "rgba(166, 226, 46, 0.08)" :color (style/color state :accent :green))]
                 [:save (if saving? "Saving…" "Save") disabled-save?
                  (assoc (style/button state) :border "none" :background (style/color state :accent :cyan)
                         :color (style/color state :bg :default) :fontWeight 600 :opacity (if disabled-save? 0.5 1))]
                 [:toggle-copy "Clone" (nil? (:selected-id state)) (style/button state)]
                 [:toggle-normalized (if (:show-normalized state) "Hide JSON" "Show JSON") false (style/button state)]
                 [:toggle-chat (if (:show-chat state) "✕ Chat" "💬 Chat") false (style/active-button state (:show-chat state))]]]
    (d/div {:style {:display "flex" :gap 6 :flexWrap "wrap" :width (when (:is-narrow state) "100%")}}
      (for [[action label disabled? presentation] buttons]
        (d/button {:key (name action) :type "button" :on-click (get actions action) :disabled disabled? :style presentation} label)))))

(hx/defnc editor-header
  "Keep selection context and commands visible above the draft."
  [{:keys [state actions]}]
  (d/div {:style {:padding (if (:is-narrow state) "12px 14px" "12px 20px") :borderBottom (style/border state)
                  :display "flex" :alignItems (if (:is-narrow state) "flex-start" "center")
                  :justifyContent "space-between" :gap 16 :flexDirection (if (:is-narrow state) "column" "row")
                  :background (style/color state :bg :darker)}}
    (hx/$ selected-heading {:state state}) (hx/$ toolbar {:state state :actions actions})))

(hx/defnc copy-row
  "Collect the destination identity for the existing copy command."
  [{:keys [state actions]}]
  (when (:show-copy state)
    (d/div {:style (merge style/row {:padding "8px 20px" :gap 8 :borderBottom (style/border state)
                                   :background (style/color state :bg :darker)})}
      (d/span {:style (style/small state)} "New ID")
      (d/input {:value (:copy-target state) :on-change #((:set-copy-target actions) (boundary/input-value %))
                :placeholder "new-contract-id"
                :style (assoc (style/input state) :flex 1 :padding "4px 10px" :borderRadius (style/token state :radius :sm)
                              :background (style/color state :bg :default))})
      (d/button {:type "button" :on-click (:copy actions) :disabled (str/blank? (:copy-target state))
                 :style (assoc (style/button state) :padding "4px 12px" :border "none"
                               :background (style/color state :accent :cyan) :color (style/color state :bg :default) :fontWeight 600)} "Clone"))))

(hx/defnc identity-form
  "Edit the existing identity, kind, version and enabled fields in raw EDN."
  [{:keys [state actions]}]
  (let [field! (:update-field actions) label-style {:display "flex" :flexDirection "column" :gap 4}]
    (d/div {:style {:flexShrink 0 :padding (if (:is-narrow state) "10px 14px" "12px 20px")
                    :borderBottom (style/border state) :background (style/color state :bg :default)}}
      (d/div {:style {:display "grid" :gridTemplateColumns (if (:is-narrow state) "1fr" "minmax(180px, 1.2fr) minmax(120px, 0.7fr) minmax(100px, 0.45fr) auto")
                      :gap 10 :alignItems "end"}}
        (d/label {:style label-style}
          (d/div {:style (style/heading state)} "Contract identity")
          (d/input {:value (:contract-id state) :disabled (:saving state) :style (style/input state)
                    :on-change #(field! "id" (boundary/input-value %))}))
        (d/label {:style label-style}
          (d/div {:style (style/heading state)} "Kind")
          (d/select {:value (:contract-kind state) :disabled (:saving state) :style (style/input state)
                     :on-change #(field! "kind" (boundary/input-value %))}
            (for [kind ["agent" "policy" "fulfillment" "tool-call" "trigger"]]
              (d/option {:key kind :value kind} kind))))
        (d/label {:style label-style}
          (d/div {:style (style/heading state)} "Version")
          (d/input {:type "number" :min 1 :value (:contract-version state) :disabled (:saving state) :style (style/input state)
                    :on-change #(field! "version" (boundary/input-value %))}))
        (d/label {:style {:display "inline-flex" :alignItems "center" :gap 8 :minHeight 34 :padding "0 4px"
                          :fontSize (style/token state :fontSize :sm) :color (style/color state :fg :default)}}
          (d/input {:type "checkbox" :checked (:enabled state) :disabled (:saving state)
                    :on-change #(field! "enabled" (boundary/input-checked? %))}) "Enabled")))))

(hx/defnc results-panel
  "Render normalized output and the controller's exact success or failure messages."
  [{:keys [state]}]
  (let [notice (:notice state) success? (= "success" (:tone notice))]
    (d/div {:style {:flexShrink 0}}
      (when (:show-normalized state)
        (d/div {:style {:borderTop (style/border state) :padding "12px 16px" :background (style/color state :bg :darker)}}
          (d/div {:style (assoc (style/heading state) :marginBottom 8)} "Normalized view")
          (d/pre {:style {:maxHeight 200 :overflow "auto" :padding 12 :borderRadius (style/token state :radius :md)
                          :background (style/color state :bg :default) :fontSize (style/token state :fontSize :xs)
                          :color (style/color state :fg :soft)}} (:normalized-json state))))
      (when notice
        (d/div {:style {:padding "8px 16px" :fontSize (style/token state :fontSize :sm)
                        :borderTop (str "1px solid " (if success? "rgba(166, 226, 46, 0.3)" "rgba(249, 38, 114, 0.3)"))
                        :background (if success? "rgba(166, 226, 46, 0.06)" "rgba(249, 38, 114, 0.06)")
                        :color (style/color state :accent (if success? :green :red))}} (:text notice)))
      (when (seq (:error state))
        (d/div {:style {:padding "8px 16px" :fontSize (style/token state :fontSize :sm)
                        :borderTop "1px solid rgba(249, 38, 114, 0.3)" :background "rgba(249, 38, 114, 0.06)"
                        :color (style/color state :accent :red)}} (:error state))))))

(hx/defnc editor-panel
  "Compose visible identity fields, the existing CodeMirror editor and results."
  [{:keys [state actions editor]}]
  (d/div {:style style/column}
    (hx/$ editor-header {:state state :actions actions})
    (hx/$ copy-row {:state state :actions actions})
    (hx/$ identity-form {:state state :actions actions})
    (d/div {:style {:flex 1 :display "flex" :minHeight 0 :overflow "hidden"}}
      (d/div {:style style/column}
        (d/div {:style {:flex 1 :minHeight 0}}
          (hx/$ editor {& (boundary/editor-props state actions)}))
        (hx/$ results-panel {:state state})))))

(hx/defnc librarian-panel
  "Share one librarian view between wide and narrow docks without remounting its controller."
  [{:keys [state actions chat chat-pane]}]
  (let [content (hx/<>
                 (d/div {:style (merge style/row {:padding "6px 12px" :borderBottom (style/border state)
                                                 :justifyContent "space-between" :fontSize (style/token state :fontSize :xs)})}
                   (d/span {:style (style/heading state)} "📋 Contract Librarian")
                   (d/label {:style (merge style/row {:gap 6 :cursor "pointer"
                                                     :color (style/color state (if (:auto-focus state) :accent :fg)
                                                                         (if (:auto-focus state) :cyan :muted))})}
                     (d/input {:type "checkbox" :checked (:auto-focus state) :on-change (:toggle-auto-focus actions)
                               :style {:cursor "pointer"}}) "Auto-focus"))
                 (hx/$ chat-pane {& (boundary/chat-props chat (:open-source actions))}))]
    (if (:is-narrow state)
      (d/div {:style (style/chat-panel state)} content)
      (d/aside {:style (style/chat-panel state)} content))))

(hx/defnc contracts-view
  "Render the contract library, visible editor and responsive librarian workspace."
  [{:keys [state actions chat chat-pane editor]}]
  (d/div {:style {:display "flex" :flex "1 1 0%" :minHeight 0 :minWidth 0 :flexDirection "column"
                  :background (style/color state :bg :default) :color (style/color state :fg :default)}}
    (d/div {:style {:display "flex" :flex "1 1 0%" :minHeight 0 :minWidth 0 :overflow "hidden" :position "relative"}}
      (when (and (:is-narrow state) (:show-left-panel state))
        (d/button {:type "button" :aria-label "Close contracts panel" :on-click (:toggle-left-panel actions)
                   :style {:position "absolute" :inset 0 :zIndex 14 :border "none" :background "rgba(0, 0, 0, 0.45)"
                           :padding 0 :cursor "pointer"}}))
      (hx/$ library/library-dock {:state state :actions actions})
      (hx/$ editor-panel {:state state :actions actions :editor editor})
      (when (and (not (:is-narrow state)) (:show-chat state))
        (hx/$ librarian-panel {:state state :actions actions :chat chat :chat-pane chat-pane})))
    (when (and (:is-narrow state) (:show-chat state))
      (hx/$ librarian-panel {:state state :actions actions :chat chat :chat-pane chat-pane}))))
