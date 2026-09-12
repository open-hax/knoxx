(ns knoxx.backend.domain.source-authoring
  "Deterministic source resources and immutable authoring event construction."
  (:require [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.law.publication :as publication]
            [knoxx.backend.law.source-authoring :as law]
            [knoxx.backend.law.source-review :as review]))

(defn creation-identity
  "Tenant/project and operation identity determine one retry-stable document."
  [scope command]
  (let [owner (crypto/sha256-hex (pr-str [(:org-id scope) (:project scope)]))
        token (crypto/sha256-hex (pr-str [owner (:operation-id command)]))]
    {:document (keyword (str "wiki." owner) (str "page-" token))
     :source-path (str "cms/wiki/" owner "/" token ".md")
     :manifest-path (str "namespaces/wiki-" owner "-" token ".edn")}))

(defn- publication-intent
  [document garden source-locale locale]
  {:publication/id (keyword (namespace document) (str (name document) "-" (name locale)))
   :publication/document document :publication/garden garden
   :publication/target :open-hax.publication/static-site
   :publication/locale locale :publication/revision :source/current
   :publication/state :draft
   :publication/path (str "/" (name locale) "/wiki/" (name document) "/")
   :translation/review (if (= locale source-locale) :none :required)})

(defn creation-resources
  "Construct a private resource document and explicit draft publication intents."
  [scope command garden]
  (review/assert-valid! :source-authoring/create law/CreateCommand command)
  (let [{:keys [document source-path] :as identity} (creation-identity scope command)
        source-locale (:source-locale command)
        locales (vec (sort (distinct (cons source-locale (:target-locales command)))))]
    (when-not (and (= (:garden command) (:garden/id garden))
                   (= :active (:garden/status garden))
                   (every? (set (:garden/locales garden)) locales))
      (throw (ex-info "choose an active garden supporting every requested locale"
                      {:status 409 :code "source_authoring_garden_refused"})))
    (let [resource {:document/id document :document/title (:title command)
                    :document/source-locale source-locale
                    :document/org-id (:org-id scope) :document/visibility :private
                    :document/anchor? true :document/generate-drafts? false
                    :document/source {:path source-path}}
          publications (mapv #(publication-intent document (:garden command) source-locale %) locales)]
      (publication/assert-valid! :source-authoring/document publication/Document resource)
      (doseq [intent publications]
        (publication/assert-valid! :source-authoring/publication publication/PublicationIntentResource intent))
      (assoc identity :resource resource
             :manifest {:namespace :wiki :resources (into [resource] publications)}))))

(defn source-event
  "Create a fully replayable source fact from server-observed resource data."
  [scope actor id action previous document content content-revision timestamp]
  (review/assert-valid!
   :source-authoring/event law/Event
   {:source/id id :source/scope scope :source/actor actor :source/action action
    :source/previous-revision previous :source/revision content-revision
    :source/content content :source/document document :source/recorded-at timestamp}))

(defn creation-manifest-compatible?
  "A create retry may observe later lawful publication states; identity and siblings stay exact."
  [expected actual]
  (let [strip-state (fn [entry]
                      (if (:publication/id entry)
                        (do (publication/assert-valid! :source-authoring/publication-state
                                                       publication/PublicationState (:publication/state entry))
                            (dissoc entry :publication/state))
                        entry))]
    (and (map? actual)
         (= (dissoc expected :resources) (dissoc actual :resources))
         (sequential? (:resources actual))
         (= (mapv strip-state (:resources expected))
            (mapv strip-state (:resources actual))))))
