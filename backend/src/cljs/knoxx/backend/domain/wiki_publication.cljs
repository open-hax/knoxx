(ns knoxx.backend.domain.wiki-publication
  "Resource ownership and exact document-to-publication selection."
  (:require [knoxx.backend.law.publication :as law]))

(defn owned-intent!
  "Visibility permits reading; only explicit document ownership permits publishing."
  [index scope publication-id]
  (let [document (get-in index [:documents (:document scope)])
        matches (filterv #(= publication-id (:publication/id %)) (:publications index))
        intent (first matches)]
    (when-not (and document (string? (:org-id scope)) (seq (:org-id scope))
                   (= (:org-id scope) (:document/org-id document))
                   (= (:document scope) (:publication/document intent)))
      (throw (ex-info "Owned publication was not found"
                      {:status 404 :code "wiki_publication_not_found"})))
    (when-not (= 1 (count matches))
      (throw (ex-info "Publication identity is ambiguous"
                      {:status 409 :code "wiki_publication_identity_conflict"})))
    (law/assert-valid! :wiki/publication law/PublicationIntentResource intent)))
