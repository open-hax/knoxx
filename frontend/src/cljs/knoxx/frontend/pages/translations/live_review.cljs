(ns knoxx.frontend.pages.translations.live-review
  "Live candidate hydration that preserves unfinished review evidence and its original basis."
  (:require [helix.hooks :as hooks]
            [knoxx.frontend.infra.navigation-guard :as navigation]
            [knoxx.frontend.pages.cms.api :as publication-api]
            [knoxx.frontend.pages.translations.logic :as logic]))

(defn review-basis
  "Bind input to work, source revision, candidate identity and latest immutable review."
  [row segment]
  {:work (logic/work-row-id row)
   :source (or (get-in row [:publication_review :revision]) (:revision row))
   :candidate (or (:candidate_set_id segment)
                  (get-in row [:publication_review :translation_revision]))
   :segment (:id segment) :review (logic/segment-review-identity segment)
   :source-text (:source_text segment) :candidate-text (:translated_text segment)})

(defn dirty?
  "A correction or score differs from the persisted form it originally described."
  [draft-binding form]
  (and draft-binding (not= (:baseline draft-binding) form)))

(defn use-changes!
  "Refresh real inventory on server events, bounded queue markers and SSE reconnect."
  [refresh!]
  (let [^js latest (hooks/use-ref refresh!)]
    (set! (.-current latest) refresh!)
    (hooks/use-effect
     :once
     (let [changed! (fn [_] ((.-current latest)))
           close! (publication-api/subscribe! changed!)]
       (.addEventListener js/window "knoxx:publication-changed" changed!)
       (fn [] (close!) (.removeEventListener js/window "knoxx:publication-changed" changed!))))))

(defn use-live-review!
  "Return current-or-preserved reviewer input and guarded selection/command helpers."
  [{:keys [selected selected-segment form set-form!]}]
  (let [[draft-binding set-draft-binding!] (hooks/use-state nil)
        basis (review-basis selected selected-segment)
        basis-key (pr-str basis)
        unfinished? (boolean (dirty? draft-binding form))
        stale? (and unfinished? (not= basis (:basis draft-binding)))
        fresh #(hash-map :basis basis :row selected :segment selected-segment
                         :baseline (logic/segment-review-form selected-segment))
        discard! #(let [next-draft-binding (fresh)]
                    (set-draft-binding! next-draft-binding) (set-form! (:baseline next-draft-binding)))
        guarded (fn [change!]
                  (fn [value]
                    (when (or (not unfinished?) (.confirm js/window "Discard your unsaved review changes?"))
                      (set-draft-binding! nil)
                      (set-form! logic/default-label)
                      (change! value))))]
    (navigation/use-navigation-guard! unfinished?)
    (hooks/use-effect
     [basis-key]
     (when-not unfinished?
       (let [next-draft-binding (fresh)]
         (set-draft-binding! next-draft-binding) (set-form! (:baseline next-draft-binding))))
     nil)
    {:draft-stale? (boolean stale?) :dirty? unfinished? :discard-draft discard!
     :guard-selection guarded
     :review-row (if stale? (:row draft-binding) selected)
     :review-segment (if stale? (:segment draft-binding) selected-segment)
     :clear-draft! #(do (set-draft-binding! nil) (set-form! logic/default-label))}))
