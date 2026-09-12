(ns knoxx.frontend.pages.contracts.style
  "Theme-derived contract workspace presentation shared by its bounded views.")

(def column "A flexible clipped workbench column."
  {:flex 1 :display "flex" :flexDirection "column" :minWidth 0 :overflow "hidden"})
(def row "A compact aligned row." {:display "flex" :alignItems "center"})

(defn color "Resolve a palette color from decoded theme data." [state family shade]
  (get-in state [:palette family shade]))
(defn token "Resolve a theme token without introducing another theme instance." [state family token-name]
  (get-in state [:tokens family token-name]))
(defn border "Preserve the workspace's subtle one-pixel separators." [state]
  (str "1px solid " (color state :fg :subtle)))
(defn small "Small muted text." [state]
  {:fontSize (token state :fontSize :xs) :color (color state :fg :muted)})
(defn heading "Small uppercase field and section labels." [state]
  (merge (small state) {:fontWeight 600 :textTransform "uppercase" :letterSpacing "0.05em"}))
(defn button "The existing toolbar button treatment." [state]
  {:padding "5px 12px" :borderRadius (token state :radius :sm) :border (border state)
   :background (color state :bg :default) :color (color state :fg :soft)
   :fontSize (token state :fontSize :xs) :cursor "pointer"})
(defn active-button "The cyan selected-toggle treatment." [state active?]
  (cond-> (button state)
    active? (assoc :border (str "1px solid " (color state :accent :cyan))
                   :background "rgba(102, 217, 239, 0.08)" :color (color state :accent :cyan))))
(defn input "The existing contract identity input treatment." [state]
  {:width "100%" :padding "7px 10px" :borderRadius (token state :radius :md)
   :border (border state) :background (color state :bg :darker)
   :color (color state :fg :default) :fontSize (token state :fontSize :sm) :outline "none"})
(defn sidebar "Dock the contract library or overlay it on a narrow viewport." [state]
  (let [narrow? (:is-narrow state)]
    {:width (if narrow? "min(88vw, 360px)" 360) :minWidth (if narrow? "min(88vw, 320px)" 300)
     :maxWidth (when narrow? "calc(100vw - 40px)") :borderRight (border state)
     :display "flex" :flexDirection "column" :background (color state :bg :darker) :overflow "hidden"
     :position (if narrow? "absolute" "relative") :inset (when narrow? "0 auto 0 0")
     :zIndex (when narrow? 15) :boxShadow (when narrow? "0 12px 36px rgba(0, 0, 0, 0.45)")}))
(defn chat-panel "Retain wide and narrow librarian dimensions." [state]
  (merge {:background (color state :bg :darker) :display "flex" :flexDirection "column" :overflow "hidden"}
         (if (:is-narrow state)
           {:height "42dvh" :minHeight 240 :maxHeight "60dvh" :borderTop (border state)}
           {:width 460 :minWidth 380 :borderLeft (border state) :minHeight 0})))
