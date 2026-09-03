(ns knoxx.backend.domain.publication-draft
  "Pure construction of immutable, review-bound publication draft resources."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.law.publication :as publication]
            [knoxx.backend.shape.resource-identity :as identity]))

(def publication-target
  "The Knoxx-owned target registered by `infra.publication-runtime`. Generated
   intents pin it so a later reviewed state change has an exact target to use."
  :open-hax.publication/static-site)

(defn- nonblank!
  [label value]
  (let [value (some-> value str str/trim not-empty)]
    (or value
        (throw (ex-info (str label " is required") {:field label})))))

(defn- atx-heading-title
  [content]
  (->> (str/split-lines content)
       (keep (fn [line]
               (when-let [[_ heading]
                          (re-find #"^ {0,3}#{1,6}[ \t]+(.+?)[ \t]*$"
                                   line)]
                 (let [title (-> heading
                                 (str/replace #"[ \t]+#+[ \t]*$" "")
                                 str/trim
                                 not-empty)]
                   (when-not (and title (re-matches #"^#+$" title))
                     title)))))
       first))

(defn- resolved-title
  [title content source-document-id]
  (or (some-> title str str/trim not-empty)
      (atx-heading-title content)
      (str "Draft from " (identity/encode-keyword source-document-id))))

(defn- keyword-id!
  [label value]
  (let [value (identity/decode-keyword value)]
    (if (qualified-keyword? value)
      value
      (throw (ex-info (str label " must be a qualified resource id")
                      {:field label :value value})))))

(defn- locale!
  [value]
  (let [value (identity/decode-keyword value)]
    (publication/assert-valid! :publication-draft/locale
                               publication/Locale value)))

(defn- stable-token
  [& values]
  (subs (crypto/sha256-hex (str/join "\u0000" (map str values))) 0 24))

(defn- normalized-garden
  [garden]
  (let [garden-id (keyword-id! :garden/id (:garden/id garden))
        locales (->> (:garden/locales garden)
                     (map locale!)
                     distinct
                     vec)]
    (when (empty? locales)
      (throw (ex-info "draft garden must declare at least one locale"
                      {:garden/id garden-id})))
    {:garden/id garden-id :garden/locales locales}))

(defn- normalized-gardens
  [gardens source-locale]
  (let [by-id (reduce
               (fn [result garden]
                 (let [{:garden/keys [id locales]} (normalized-garden garden)]
                   (update result id (fnil into #{}) locales)))
               {}
               gardens)]
    (when (empty? by-id)
      (throw (ex-info "at least one target garden is required" {})))
    (->> by-id
         (map (fn [[garden-id locales]]
                {:garden/id garden-id
                 ;; A generated post needs an authored source-locale relation
                 ;; in every target garden in addition to its translated
                 ;; relations. The source relation is review-free but remains
                 ;; an unpublished draft like every other generated intent.
                 :garden/locales (->> (conj locales source-locale)
                                      (sort-by pr-str)
                                      vec)}))
         (sort-by (comp pr-str :garden/id))
         vec)))

(defn canonical-policy
  "Canonical identity topology for one generated draft.

   Garden and locale order, repeated garden rows, and repeated locales are not
   semantic. Organization and optional project scope are semantic; the acting
   membership is deliberately absent because a principal does not own the
   generated resource identity."
  [{:keys [source-document-id source-revision source-locale gardens org-id project]}]
  (let [source-document-id (keyword-id! :source-document-id source-document-id)
        source-revision (nonblank! :source-revision source-revision)
        source-locale (locale! source-locale)
        org-id (nonblank! :org-id org-id)
        project (some-> project str str/trim not-empty)]
    {:source-document-id source-document-id
     :source-revision source-revision
     :source-locale source-locale
     :org-id org-id
     :project project
     :gardens (normalized-gardens gardens source-locale)}))

(defn- policy-binding
  [{:keys [source-document-id source-revision source-locale gardens org-id project]}]
  [1
   source-document-id
   source-revision
   source-locale
   org-id
   project
   (mapv (fn [{:garden/keys [id locales]}]
           [id locales])
         gardens)])

(defn policy-fingerprint
  "Full SHA-256 fingerprint of a canonical generated-draft topology."
  [input]
  (-> input canonical-policy policy-binding pr-str crypto/sha256-hex))

(defn draft-id
  "Content-address the generated post owned by one canonical draft topology."
  [input]
  (keyword "knoxx.generated"
           (str "post-" (subs (policy-fingerprint input) 0 24))))

(defn draft-identity
  "Resolve the canonical policy, fingerprint, id, and immutable source path."
  [input]
  (let [policy (canonical-policy input)
        fingerprint (-> policy policy-binding pr-str crypto/sha256-hex)
        document-id (keyword "knoxx.generated"
                             (str "post-" (subs fingerprint 0 24)))]
    {:draft/id document-id
     :draft/policy policy
     :draft/policy-fingerprint fingerprint
     :draft/source-path (str "drafts/" (name document-id) ".md")}))

(defn- draft-document
  [document-id title source-locale org-id source-document-id source-revision source-path]
  {:document/id document-id
   :document/title title
   :document/source-locale source-locale
   :document/org-id org-id
   :document/visibility :private
   :document/anchor? true
   :document/generate-drafts? false
   :document/derived-from source-document-id
   :document/derived-source-revision source-revision
   :document/source {:path source-path}})

(defn- draft-publications
  [document-id source-locale gardens]
  (let [local-id (name document-id)]
    (->> gardens
         (mapcat
          (fn [{:garden/keys [id locales]}]
            (map (fn [locale]
                   {:publication/id
                    (keyword "knoxx.generated"
                             (str local-id "-"
                                  (stable-token document-id id locale)))
                    :publication/document document-id
                    :publication/garden id
                    :publication/target publication-target
                    :publication/locale locale
                    :publication/revision :source/current
                    :publication/state :draft
                    :publication/path (str "/drafts/" local-id "/" (name locale))
                    :translation/review (if (= source-locale locale)
                                          :none
                                          :required)})
                 locales)))
         vec)))

(defn draft-resources
  "Build deterministic draft resources pinned to one admitted source revision."
  [{:keys [title content] :as input}]
  (let [{:draft/keys [id policy source-path] :as identity} (draft-identity input)
        {:keys [source-document-id source-revision source-locale gardens org-id]} policy
        content (nonblank! :content content)
        title (resolved-title title content source-document-id)
        document (draft-document id title source-locale org-id
                                 source-document-id source-revision source-path)
        publications (draft-publications id source-locale gardens)
        _ (publication/assert-valid! :publication-draft/document
                                     publication/Document document)
        _ (doseq [intent publications]
            (publication/assert-valid! :publication-draft/publication
                                       publication/PublicationIntentResource intent))]
    (merge identity
           {:draft/source-document source-document-id
            :draft/source-revision source-revision
            :draft/title title
            :draft/content content
            :draft/document document
            :draft/publications publications
            :draft/manifest {:namespace :knoxx.generated
                             :resources (into [document] publications)}})))
