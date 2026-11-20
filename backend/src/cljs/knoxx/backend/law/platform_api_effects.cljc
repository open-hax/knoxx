(ns knoxx.backend.law.platform-api-effects
  "Named, serializable contracts for API effect descriptions and expansion."
  (:require [malli.core :as m]))

(def registry
  {::effect [:map {:closed true}
             [:effect [:enum :read :definition :subscription :unclassified :publish :edit :amplify
                       :react :thread :conversation :presence :profile :curate :delete :moderate
                       :credential :account :binary :repository :repo-import]]
             [:audience [:enum :none :public :conversation :operator :channel-or-resource :account]]
             [:mutation [:enum :read :create :replace :update :delete :execute]]
             [:publication? :boolean] [:credential? :boolean] [:destructive? :boolean]
             [:separate-auth? :boolean] [:body-dependent? :boolean]
             [:inspection-required [:vector :keyword]]
             [:classification [:enum :derived-from-pinned-operation :derived-from-pinned-record-collection]]
             [:path {:optional true} [:vector [:or :string :int]]]
             [:collection {:optional true} :string] [:target {:optional true} :string]]
   ::analysis [:map {:closed true}
               [:platform [:enum "discord" "bluesky"]] [:operation :string]
               [:actions [:vector [:ref ::effect]]]
               [:complete? :boolean] [:callable? :boolean] [:classification :string]]})

(def effect-valid? (m/validator [:schema {:registry registry} ::effect]))
(def analysis-valid? (m/validator [:schema {:registry registry} ::analysis]))

(defn assert-effect!
  "Reject malformed derived metadata without echoing a request payload."
  [effect]
  (when-not (effect-valid? effect)
    (throw (ex-info "Invalid API effect description" {:code "platform_effect_contract_invalid"})))
  effect)

(defn assert-analysis!
  "Validate the effect expansion; this is not permission or a platform receipt."
  [analysis]
  (when-not (analysis-valid? analysis)
    (throw (ex-info "Invalid API effect expansion" {:code "platform_effect_contract_invalid"})))
  analysis)

(defn require-resolved!
  "A future executor must resolve every body-dependent effect first. Payload,
   actor, audience and reservation checks are still required independently."
  [analysis]
  (assert-analysis! analysis)
  (when-not (and (:complete? analysis) (:callable? analysis))
    (throw (ex-info "API effects are unresolved or this entry is not an HTTP operation"
                    {:code "platform_effect_unresolved" :phase :preflight})))
  analysis)
