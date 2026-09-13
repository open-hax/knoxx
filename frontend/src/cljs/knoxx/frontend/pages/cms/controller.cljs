(ns knoxx.frontend.pages.cms.controller
  "Source command orchestration with optimistic concurrency and human draft protection."
  (:require [clojure.string :as str]
            [helix.hooks :as hooks]
            [knoxx.frontend.infra.navigation-guard :as navigation]
            [knoxx.frontend.pages.cms.api :as api]
            [knoxx.frontend.pages.cms.logic :as logic]))

(defn- message [error]
  (or (.-message ^js error) (str error)))

(defn operation-id!
  "Keep an operation identity across identical repair retries; rotate when intent changes."
  [^js pending kind payload]
  (let [current (.-current pending)]
    (if (and (= kind (:kind current)) (= payload (:payload current)))
      (:id current)
      (let [id (str (random-uuid))]
        (set! (.-current pending) {:kind kind :payload payload :id id})
        id))))

(defn- install-review [state snapshot]
  (-> state
      (update :editor logic/receive-snapshot snapshot)
      (update :review-form #(if (logic/review-dirty? %) % (logic/empty-review snapshot)))))

(defn- grants [state]
  (let [snapshot (get-in state [:editor :snapshot])
        row (first (filter #(= (:selected state) (logic/document-id %))
                           (get-in state [:inventory :documents])))
        capabilities (or (:capabilities snapshot) (:capabilities row)
                         (get-in state [:inventory :capabilities]))
        commands (or (:commands snapshot) (:commands row) (get-in state [:inventory :commands]))
        allowed? #(and (logic/capability? capabilities %1) (logic/command-available? commands %2))]
    {:can-create? (allowed? "publication/write" "wiki_create")
     :can-write? (allowed? "publication/write" "wiki_save")
     :can-comment? (allowed? "publication/write" "wiki_review")
     :can-review? (allowed? "publication/review" "wiki_review")
     :can-assist? (allowed? "publication/assist" "wiki_assist")
     :can-publish? (allowed? "publication/publish" "wiki_publish")}))

(defn- ^:async read-inventory! [state! generation]
  (let [id (swap! generation inc)]
    (try
      (let [inventory (await (api/list-documents))]
        (when (= id @generation) (state! #(assoc % :inventory inventory :loading? false))))
      (catch :default error
        (when (= id @generation) (state! #(assoc % :error (message error) :loading? false)))))))

(defn- ^:async read-document! [document state! generation]
  (let [id (swap! generation inc)]
    (try
      (let [snapshot (await (api/get-review document))]
        (when (= id @generation)
          (state! #(if (= document (:selected %)) (install-review % snapshot) %))
          (if (logic/command-available? (:commands snapshot) "wiki_publications")
            (let [targets (await (api/get-publications document))]
              (when (= id @generation)
                (state! #(if (= document (:selected %))
                           (assoc % :publications (:publications targets)) %))))
            (state! #(assoc % :publications [])))))
      (catch :default error
        (when (= id @generation) (state! #(assoc % :error (message error))))))))

(defn- ^:async execute! [state! pending command on-success]
  (state! #(assoc % :busy? true :error nil :notice nil))
  (try
    (let [result (await (command))]
      (set! (.-current ^js pending) nil)
      (on-success result))
    (catch :default error (state! #(assoc % :error (message error))))
    (finally (state! #(assoc % :busy? false)))))

(defn- creation [inventory]
  (let [first-garden (first (:gardens inventory))
        values {:title "" :content "" :source_locale "en" :target_locales "es"
                :garden (or (logic/wire-id (or (:garden/id first-garden) (:id first-garden))) "")}]
    (assoc values :initial values)))

(defn- form-actions
  [{:keys [state! snapshot]}]
  {:set-tab! #(state! (fn [value] (assoc value :tab %)))
   :set-content! #(state! (fn [value] (assoc-in value [:editor :content] %)))
   :set-review-form! #(state! (fn [value] (assoc value :review-form %)))
   :set-creation! #(state! (fn [value] (assoc value :creation %)))
   :set-instruction! #(state! (fn [value] (assoc value :instruction %)))
   :dismiss-error! #(state! (fn [value] (assoc value :error nil)))
   :discard-source! #(state! (fn [value] (assoc value :editor (logic/fresh-editor snapshot))))
   :discard-review! #(state! (fn [value] (assoc value :review-form (logic/empty-review snapshot))))
   :acknowledge-review! #(state! (fn [value] (assoc-in value [:review-form :basis] (logic/review-basis snapshot))))})

(defn- navigation-actions
  [{:keys [state! selected latest]}]
  {:select! (fn
     [document]
     (when
       (and (not= document selected) (not (:busy? (.-current latest))) (not (logic/unsaved? (.-current latest))))
       (state!
         #(assoc % :selected document :editor nil :review-form nil :publications [] :creation nil :suggestion nil :tab :read))))
   :new! #(when-not (logic/unsaved? (.-current latest)) (state! (fn [value] (assoc value :creation (creation (:inventory value))))))
   :cancel-creation! #(state! (fn [value] (assoc value :creation nil)))})

(defn- creation-actions
  [{:keys [state state! permissions operation command! refresh!]}]
  {:create! (fn
     []
     (when
       (:can-create? permissions)
       (let
         [form (:creation state)
          payload (->
            (select-keys form [:title :content :source_locale :garden])
            (assoc :target_locales (logic/lines (:target_locales form))))]
         (command!
           #(api/create-document (operation :create payload))
           (fn
             [result]
             (state!
               #(-> % (assoc :selected (get-in result [:review :document]) :editor (logic/fresh-editor (:review result)) :review-form (logic/empty-review (:review result)) :creation nil :tab :read)))
             (refresh!))))))})

(defn- source-actions
  [{:keys [state state! snapshot selected permissions operation command! installed!]}]
  {:save! (fn
     []
     (when
       (and (:can-write? permissions) (logic/writable-draft? (:editor state)))
       (let
         [payload {:expected_revision (get-in state [:editor :base-revision])
           :content (get-in state [:editor :content])}]
         (command!
           #(api/save-source selected (operation [:save selected] payload))
           (fn [result] (state! #(assoc % :editor (logic/fresh-editor (:review result)))) (installed! result))))))
   :review! (fn
     [action]
     (when
       (and
         (not (logic/dirty? (:editor state)))
         (logic/review-form-current? (:review-form state) snapshot)
         (logic/review-action-available? snapshot action)
         (if
           (contains? #{"accept" "request_changes"} action)
           (:can-review? permissions)
           (:can-comment? permissions)))
       (let
         [payload (logic/review-payload snapshot action (:review-form state))]
         (command! #(api/submit-review selected (operation [:review selected] payload))
                   (fn [result]
                     (state! #(assoc % :review-form (logic/empty-review (:review result))
                                      :notice "Review recorded for this source revision."))
                     (installed! result))))))})

(defn- assistant-actions
  [{:keys [state state! snapshot selected permissions command!]}]
  {:ask! (fn
     []
     (when
       (and (:can-assist? permissions) (not (str/blank? (:instruction state))))
       (let
         [basis (:revision snapshot)]
         (command!
           #(api/assist selected {:expected_revision basis :instruction (:instruction state)})
           (fn [result] (state! #(assoc % :suggestion (assoc result :basis basis)
                                         :notice "Writing suggestion received. Review it before updating your draft.")))))))
   :adopt! (fn
     []
     (when
       (and
         (:can-write? permissions)
         (not (logic/dirty? (:editor state)))
         (= (:revision snapshot) (get-in state [:suggestion :basis])))
       (state!
         #(-> % (assoc-in [:editor :content] (or (get-in % [:suggestion :content]) (get-in % [:suggestion :text]) "")) (assoc :tab :edit :suggestion nil)))))})

(defn- publication-actions
  [{:keys [state state! snapshot selected permissions command! installed!]}]
  {:publish! (fn
     [publication]
     (when
       (and (:can-publish? permissions) (:accepted snapshot) (not (logic/unsaved? state)))
       (command!
         #(api/publish selected {:publication (:id publication) :expected_revision (:revision snapshot)})
         (fn
           [result]
           (installed! result)
           (state! #(assoc % :notice "Publication request recorded. Review the observed result and blockers below."))))))})

(def ^:private initial-state
  {:inventory {:documents [] :gardens []} :loading? true :selected nil :editor nil
   :review-form nil :creation nil :publications [] :tab :read :instruction ""
   :suggestion nil :busy? false :error nil :notice nil :refresh 0})

(defn- use-source-effects! [state state! inventory-generation document-generation refresh!]
  (let [selected (:selected state)]
    (hooks/use-effect [(:refresh state)]
      (read-inventory! state! inventory-generation)
      (fn [] (swap! inventory-generation inc)))
    (hooks/use-effect [selected (:refresh state)]
      (when selected (read-document! selected state! document-generation))
      (fn [] (swap! document-generation inc)))
    (hooks/use-effect :once
      (let [close! (api/subscribe! (fn [_] (refresh!)))
            changed! (fn [_] (refresh!))]
        (.addEventListener js/window "knoxx:publication-changed" changed!)
        (fn [] (close!) (.removeEventListener js/window "knoxx:publication-changed" changed!))))))

(defn use-source-controller
  "Expose human controls for the same authorized commands used by agents."
  []
  (let [[state state!] (hooks/use-state initial-state)
        ^js latest (hooks/use-ref state) ^js pending (hooks/use-ref nil)
        inventory-generation (hooks/use-memo :once (atom 0))
        document-generation (hooks/use-memo :once (atom 0))
        refresh! #(state! (fn [value] (update value :refresh inc)))
        snapshot (get-in state [:editor :snapshot]) permissions (grants state)
        installed! (fn [result]
                     (state! #(install-review % (:review result)))
                     (refresh!))
        context {:state state :state! state! :latest latest :snapshot snapshot
                 :selected (:selected state) :permissions permissions :refresh! refresh!
                 :installed! installed! :command! #(execute! state! pending %1 %2)
                 :operation #(assoc %2 :operation_id (operation-id! pending %1 %2))}]
    (set! (.-current latest) state)
    (navigation/use-navigation-guard! (logic/unsaved? state))
    (use-source-effects! state state! inventory-generation document-generation refresh!)
    (merge state permissions {:snapshot snapshot :unsaved? (logic/unsaved? state) :refresh! refresh!}
           (form-actions context) (navigation-actions context) (creation-actions context)
           (source-actions context) (assistant-actions context) (publication-actions context))))
