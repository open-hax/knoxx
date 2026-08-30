(ns knoxx.frontend.pages.translations.review-controller
  "Generation-guarded async orchestration for the translation review page."
  (:require [knoxx.frontend.pages.translations.api :as api]
            [knoxx.frontend.pages.translations.logic :as logic]))

(defn- next-load-id!
  [^js load-seq]
  (let [load-id (inc (.-current load-seq))]
    (set! (.-current load-seq) load-id)
    load-id))

(defn- current-load?
  [^js load-seq load-id]
  (= load-id (.-current load-seq)))

(defn cancel!
  "Invalidate one in-flight generation if it is still current."
  [^js load-seq load-id]
  (when (current-load? load-seq load-id)
    (set! (.-current load-seq) (inc load-id))))

(defn- preserve-current-selection!
  [documents set-selected!]
  ;; Resolve against state current when the setter runs. A request can start on
  ;; row A and finish after the reviewer chose B; closing over A moves backward.
  (set-selected!
   (fn [current]
     (when current
       (let [updated (first (filter #(logic/same-work? % current) documents))]
         (if (= updated current) current updated))))))

(defn- install-projection!
  [documents-response reviews {:keys [set-documents! set-selected!]}]
  (let [documents (logic/attach-publication-reviews
                   (:documents documents-response)
                   reviews)]
    (set-documents! documents)
    (preserve-current-selection! documents set-selected!)))

(defn- load-legacy-projection!
  [reviews-response project target-lang load-seq load-id setters]
  (let [all-reviews (:reviews reviews-response)
        reviews (cond->> all-reviews
                  (seq target-lang) (filter #(= target-lang (:locale %))))
        effective-project (logic/effective-project reviews-response project)]
    (if (and effective-project (not= effective-project project))
      ((:set-project! setters) effective-project)
      (-> (api/list-documents {:project effective-project
                               :target-lang target-lang})
          (.then (fn [documents-response]
                   (when (current-load? load-seq load-id)
                     (install-projection! documents-response reviews setters))))))))

(defn load-documents!
  "Load resource work first, then its project-scoped legacy compatibility rows."
  [project target-lang load-seq
   {:keys [set-loading! set-error! set-documents!] :as setters}]
  (let [load-id (next-load-id! load-seq)]
    (set-loading! true)
    (set-error! nil)
    (-> (api/list-publication-reviews)
        (.then (fn [reviews-response]
                 (when (current-load? load-seq load-id)
                   (load-legacy-projection! reviews-response project target-lang
                                            load-seq load-id setters))))
        (.catch (fn [^js err]
                  (when (current-load? load-seq load-id)
                    (set-error! (or (.-message err) (str err)))
                    (set-documents! []))))
        (.finally (fn []
                    (when (current-load? load-seq load-id)
                      (set-loading! false)))))
    load-id))

(defn load-manifest!
  "Load a project manifest without allowing an older project to win."
  [project load-seq set-manifest!]
  (let [load-id (next-load-id! load-seq)]
    (-> (api/get-manifest project)
        (.then (fn [manifest]
                 (when (current-load? load-seq load-id)
                   (set-manifest! manifest))))
        (.catch (fn [_]
                  (when (current-load? load-seq load-id)
                    (set-manifest! nil)))))
    load-id))

(defn- empty-work-detail
  [selected]
  {:document {:title (:title selected) :source_lang (:source_lang selected)}
   :target_lang (:target_lang selected)
   :summary {:total_segments (or (:total_segments selected) 0)
             :approved (or (:approved selected) 0)
             :overall_status (or (:work_state selected) "pending")}
   :segments []})

(defn- detail-request
  [selected project]
  (cond
    (logic/blocked-resource-candidate? selected)
    (js/Promise.resolve (empty-work-detail selected))

    (:contract_content selected)
    (js/Promise.resolve (logic/authored-detail selected))

    (logic/legacy-candidate? selected)
    (api/get-document (:document_id selected) (:target_lang selected)
                      (logic/legacy-review-scope selected project))

    :else
    (js/Promise.resolve (empty-work-detail selected))))

(defn load-detail!
  "Load only the newest selection's detail; never substitute legacy bytes."
  [selected project load-seq
   {:keys [set-detail-loading! set-detail! set-seg-idx! set-form! set-error!]}]
  (let [load-id (next-load-id! load-seq)]
    (set-detail-loading! true)
    (-> (detail-request selected project)
        (.then (fn [detail]
                 (when (current-load? load-seq load-id)
                   (set-detail! detail)
                   (set-seg-idx! nil)
                   (set-form! logic/default-label))))
        (.catch (fn [^js err]
                  (when (current-load? load-seq load-id)
                    (set-error! (or (.-message err) (str err)))
                    (set-detail! nil))))
        (.finally (fn []
                    (when (current-load? load-seq load-id)
                      (set-detail-loading! false)))))
    load-id))
