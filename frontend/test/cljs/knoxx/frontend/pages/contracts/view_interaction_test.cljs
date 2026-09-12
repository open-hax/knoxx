(ns knoxx.frontend.pages.contracts.view-interaction-test
  "Visible control and native widget contracts for the migrated contract workspace."
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :as test]
            [helix.core :as hx]
            [helix.dom :as d]
            [knoxx.frontend.lib.contracts-view :as boundary]
            [knoxx.frontend.pages.contracts.view :as view]))

(test/use-fixtures :each {:after #(rtl/cleanup)})

(def ^:private palette
  {:bg {:default "#111" :darker "#000"} :fg {:default "#eee" :muted "#999" :subtle "#333" :soft "#bbb"}
   :accent {:cyan "#66d9ef" :green "#a6e22e" :red "#f92672" :orange "#fd971f"}})
(def ^:private entry
  {:id "trigger" :contractClass "triggers" :label "trigger" :status "idle" :enabled true :version 1
   :triggerKind "manual" :sourceKind "triggers"})
(def ^:private state
  {:palette palette :tokens {:fontSize {:xs "12px" :sm "14px" :base "16px" :lg "18px"}
                             :radius {:xs "3px" :sm "4px" :md "6px"}}
   :agent-entries [entry] :filtered-contracts [["triggers" [entry]]] :selected-id "trigger" :selected-class "triggers"
   :selected-entry entry :edn-draft "{:contract/id \"trigger\"}" :contract-id "trigger" :contract-kind "trigger"
   :contract-version "1" :enabled true :is-dirty true :show-left-panel true :show-chat true :auto-focus true
   :search-query "" :copy-target "" :collapsed-folders [] :validation-errors [] :error ""})

(hx/defnc editor-placeholder
  "Exercise the same native props used by the real CodeMirror component."
  [{:keys [value onChange externalErrors fileName]}]
  (d/div
    (d/textarea {:aria-label "Full contract EDN" :value value :on-change #(onChange (boundary/input-value %))})
    (d/span fileName)
    (for [^js error (array-seq externalErrors)]
      (d/span {:key (.-message error)} (.-message error)))))

(hx/defnc chat-placeholder
  "Exercise the librarian's native source callbacks."
  [{:keys [onOpenHydrationSource onOpenSourceInPreview]}]
  (d/div
    (d/button {:type "button" :on-click #(onOpenHydrationSource #js {:path "/ops/contracts/agents/hydrated"})} "Hydrate contract")
    (d/button {:type "button" :on-click #(onOpenSourceInPreview #js {:url "/ops/contracts/agents/preview"})} "Preview contract")))

(defn- actions [calls]
  (into {} (map (fn [action] [action (fn [& values] (swap! calls conj (into [action] values)))])
               [:toggle-left-panel :toggle-auto-focus :refresh :validate :save :copy :set-draft :set-search
                :toggle-folder :set-copy-target :toggle-copy :toggle-normalized :toggle-chat :select-contract
                :new-contract :update-field :open-source])))

(defn- mount [overrides calls]
  (rtl/render (hx/$ view/contracts-view {:state (merge state overrides) :actions (actions calls)
                                        :editor editor-placeholder :chat-pane chat-placeholder :chat nil})))

(test/deftest native-toolbar-preserves-command-and-disabled-state
  (let [calls (atom []) ^js rendered (mount {:saving true} calls)]
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Saving…"}))
    (test/is (empty? @calls) "Disabled save cannot invoke the action")
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "✓ Validate"}))
    (test/is (= :validate (ffirst @calls)))
    (test/is (nil? (.queryByText rendered "Metadata")) "The permanently hidden legacy metadata subtree is retired")
    (test/is (= 1 (.-length (.getAllByLabelText rendered "Enabled"))))))

(test/deftest native-library-preserves-class-and-search-dispatch
  (let [calls (atom []) ^js rendered (mount {} calls)]
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "trigger on triggers · v1"}))
    (.change rtl/fireEvent (.getByPlaceholderText rendered "Search contracts...") #js {:target #js {:value "review"}})
    (test/is (= [:select-contract "trigger" "triggers"] (first @calls)))
    (test/is (= [:set-search "review"] (second @calls)))))

(test/deftest visible-identity-fields-send-their-native-values
  (let [calls (atom []) ^js rendered (mount {} calls)]
    (.change rtl/fireEvent (.getByLabelText rendered "Contract identity") #js {:target #js {:value "edited"}})
    (.change rtl/fireEvent (.getByLabelText rendered "Kind") #js {:target #js {:value "policy"}})
    (.change rtl/fireEvent (.getByLabelText rendered "Version") #js {:target #js {:value "3"}})
    (.click rtl/fireEvent (.getByLabelText rendered "Enabled"))
    (test/is (= [[:update-field "id" "edited"] [:update-field "kind" "policy"]
                [:update-field "version" "3"] [:update-field "enabled" false]] @calls))))

(test/deftest raw-editor-diagnostics-and-copy-control-retain-their-contract
  (let [calls (atom []) ^js rendered (mount {:show-copy true :copy-target "new-id"
                                            :validation-errors [{:path ["agent" "role"] :message "Unknown role"}]} calls)]
    (test/is (some? (.getByText rendered "agent.role: Unknown role")))
    (.change rtl/fireEvent (.getByLabelText rendered "Full contract EDN") #js {:target #js {:value "{:draft true}"}})
    (.change rtl/fireEvent (.getByPlaceholderText rendered "new-contract-id") #js {:target #js {:value "copied"}})
    (test/is (= [[:set-draft "{:draft true}"] [:set-copy-target "copied"]] @calls))))

(test/deftest narrow-layout-keeps-overlay-and-librarian-source-actions
  (let [calls (atom []) ^js rendered (mount {:is-narrow true} calls)]
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Close contracts panel"}))
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Hydrate contract"}))
    (.click rtl/fireEvent (.getByRole rendered "button" #js {:name "Preview contract"}))
    (test/is (= :toggle-left-panel (ffirst @calls)))
    (test/is (= [[:open-source "/ops/contracts/agents/hydrated"] [:open-source "/ops/contracts/agents/preview"]]
                (subvec @calls 1)))
    (test/is (nil? (.querySelector (.-container rendered) "aside")) "The narrow librarian uses the bottom dock")))
