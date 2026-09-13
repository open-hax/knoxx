(ns knoxx.backend.domain.cms-document
  (:require [knoxx.backend.law.cms-document :as law]
            [knoxx.backend.law.publication :as publication]))
(defn record [org id source body previous]
  (law/require-id! org) (law/require-id! id) (law/require-body! body)
  {:doc_id id :title (:title body) :content (:content body) :source_path source
   :visibility (or (:visibility body) (:visibility previous) "internal")
   :garden_id (str "cms." org "/workspace") :metadata {}})
(defn garden [org]
  {:namespace (keyword (str "cms." org))
   :resources [{:garden/id (keyword (str "cms." org) "workspace") :garden/title "Workspace Publications" :garden/status :active :garden/locales [:en :es]}]})
(defn- manifest-data [org id source title]
  {:namespace (keyword (str "cms." org))
   :resources (into [{:document/id (keyword (str "cms." org) (str "doc-" id)) :document/title title :document/source-locale :en
                      :document/visibility :private :document/org-id org :document/source {:path source}}]
                    (for [locale [:en :es]]
                      {:publication/id (keyword (str "cms." org) (str "doc-" id "-" (name locale))) :publication/document (keyword (str "cms." org) (str "doc-" id))
                       :publication/garden (keyword (str "cms." org) "workspace") :publication/target :open-hax.publication/static-site
                       :publication/locale locale :publication/revision :source/current :publication/state :withheld
                       :publication/path (str "/cms/" org "/" id "/" (name locale)) :translation/review :required}))})

(defn manifest "Validate emitted resources with the production publication laws before persistence." [org id source title]
  (let [result (manifest-data org id source title)]
    (publication/assert-valid! :cms/document publication/Document (first (:resources result)))
    (doseq [intent (rest (:resources result))]
      (publication/assert-valid! :cms/publication publication/PublicationIntentResource intent))
    result))
