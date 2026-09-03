(ns knoxx.backend.infra.publication-draft-tool
  "The single, policy-pinned tool available to the publication post drafter."
  (:require [clojure.string :as str]
            [knoxx.backend.domain.text :refer [tool-text-result]]
            [knoxx.backend.domain.tools :refer [create-tool-obj maybe-tool-update!]]
            [knoxx.backend.infra.agent.event-policy-authority :as event-policy-authority]
            [knoxx.backend.infra.publication-admission-hook :as admission-hook]
            [knoxx.backend.infra.publication-draft-store :as draft-store]))

(def save-draft-params
  [:map
   [:title {:optional true
            :description "Optional title. When absent or blank, Knoxx derives it from the first Markdown ATX heading or the pinned source document."}
    :string]
   [:content {:description "Required complete Markdown body for the crafted draft post."} :string]])

(defn- required-policy!
  [policies key]
  (let [value (get policies key)]
    (if (or (nil? value)
            (and (string? value) (str/blank? value)))
      (throw (ex-info (str (name key) " is missing from the admitted draft policy")
                      {:code :publication-draft-policy-missing :field key}))
      value)))

(defn- draft-input
  [policies params]
  (when-not (true? (:publication-draft? policies))
    (throw (ex-info "save_publication_draft requires a server-pinned draft session"
                    {:code :publication-draft-policy-required})))
  {:source-document-id (required-policy! policies :source-document-id)
   :source-revision (required-policy! policies :source-revision)
   :source-locale (required-policy! policies :source-locale)
   :gardens (required-policy! policies :gardens)
   :org-id (required-policy! policies :org-id)
   :project (:project policies)
   :title (aget params "title")
   :content (aget params "content")})

(defn- admission-scope
  [policies]
  (cond-> {:org-id (required-policy! policies :org-id)
           :membership-id (required-policy! policies :membership-id)}
    (some? (:project policies)) (assoc :project (:project policies))))

(defn- admission-succeeded?
  [result]
  (and (true? (:ok result))
       (number? (:failed result))
       (zero? (:failed result))
       (number? (:admitted result))
       (pos? (:admitted result))))

(defn ^:async save-draft!
  "Persist a deterministic draft and re-admit it for automatic translation."
  ([config policies params]
   (save-draft! config policies params admission-hook/admit!))
  ([config policies params admit!]
   (let [saved (await (draft-store/persist-or-resume!
                       config (draft-input policies params)))
         scope (admission-scope policies)
         admission (await (admit! scope {:document (:draft/id saved)
                                         :generate-drafts? false}))]
     (when-not (admission-succeeded? admission)
       (throw (ex-info "generated draft admission failed"
                       {:code :generated-draft-admission-failed
                        :draft/id (:draft/id saved)
                        :admission admission})))
     (let [completion (await (draft-store/mark-complete! config saved))]
       {:draft/id (:draft/id saved)
        :draft/source-document (:draft/source-document saved)
        :draft/source-revision (:draft/source-revision saved)
        :draft/policy-fingerprint (:draft/policy-fingerprint saved)
        :draft/content-path (:content-path saved)
        :draft/manifest-path (:manifest-path saved)
        :draft/completion-path (:draft/completion-path completion)
        :draft/publication-count (count (:draft/publications saved))
        :draft/created? (:draft/created? saved)
        :draft/completion-created? (:draft/completion-created? completion)
        :draft/admission-complete? true
        :draft/admission admission}))))

(defn make-save-draft-execute
  [auth-context]
  (^:async fn [_runtime config _tool-call-id params a b c]
    (let [on-update (or (when (fn? a) a) (when (fn? b) b) (when (fn? c) c))
          policies (:resourcePolicies auth-context)]
      (when-not (event-policy-authority/authorized? auth-context)
        (throw (ex-info "save_publication_draft requires server-admitted event authority"
                        {:status 403
                         :code :publication-draft-authority-required})))
      (maybe-tool-update! on-update "Saving and admitting review-bound publication draft…")
      (let [result (await (save-draft! config policies params))]
        (tool-text-result
         (str "Saved " (:draft/id result) " as an unpublished draft and admitted "
              (:draft/publication-count result)
              " locale relations for translation and human review.")
         result)))))

(defn save-publication-draft-tool
  [auth-context]
  (partial create-tool-obj
           "save_publication_draft"
           "Save Publication Draft"
           "Save one source-revision-pinned post as immutable draft resources and admit every locale for translation."
           "Call save_publication_draft exactly once with the complete crafted Markdown post; title is optional."
           ["Supply complete Markdown content and, optionally, a nonblank title."
            "When title is absent or blank, Knoxx uses the first Markdown ATX heading or a source-document fallback."
            "Document identity, source revision, gardens, locales, tenant, and publication state are server-pinned."
            "The result remains unpublished; human review and an explicit state transition are still required."]
           save-draft-params
           (make-save-draft-execute auth-context)))

(defn create-publication-draft-tools
  [runtime config auth-context]
  (let [build-tool (save-publication-draft-tool auth-context)]
    (clj->js [(build-tool runtime config)])))
