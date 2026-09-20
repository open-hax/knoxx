(ns knoxx.backend.domain.wiki-capabilities
  "One capability catalog for human controls and agent commands.")

(def catalog
  "Each interface capability names its actual application permission."
  [{:id "publication/read" :permission "org.publications.read"}
   {:id "publication/write" :permission "org.publications.manage"}
   {:id "publication/review" :permission "org.publications.review"}
   {:id "publication/assist" :permission "org.publications.assist"}
   {:id "publication/publish" :permission "org.publications.publish"}])

(def command-catalog
  "One operation identity for HTTP controls, agent tools and explicit denies.

   Review has action-specific permissions: writers submit/comment, reviewers
   accept/request changes. Advertising the tool never grants both operations."
  [{:id "wiki_list" :capabilities ["publication/read"]}
   {:id "wiki_read" :capabilities ["publication/read"]}
   {:id "wiki_create" :capabilities ["publication/write"]}
   {:id "wiki_save" :capabilities ["publication/write"]}
   {:id "wiki_review" :capabilities ["publication/write" "publication/review"]}
   {:id "wiki_assist" :capabilities ["publication/assist"]}
   {:id "wiki_publications" :capabilities ["publication/read"]}
   {:id "wiki_publish" :capabilities ["publication/publish"]}])

(defn permission
  "Resolve a known capability. Unknown names never become permissions."
  [id] (:permission (some #(when (= id (:id %)) %) catalog)))

(defn command-capabilities
  "Return a registered command's allowed capability pairings; unknown IDs return nil."
  [id] (:capabilities (some #(when (= id (:id %)) %) command-catalog)))

(defn explicitly-denied?
  "An explicit operation deny wins over every permission or administrator grant."
  [policies command]
  (boolean (some #(and (= command (or (:tool-id %) (:toolId %)))
                       (contains? #{"deny" :deny} (:effect %))) policies)))

(defn- permitted?
  [permissions administrator? capability]
  (when-let [required (permission capability)]
    (or administrator? (contains? permissions required))))

(defn available-commands
  "Grant entries cannot invent commands or substitute for required permissions."
  [permissions administrator? policies]
  (into []
        (comp (filter #(and (not (explicitly-denied? policies (:id %)))
                            (some (partial permitted? permissions administrator?) (:capabilities %))))
              (map :id))
        command-catalog))

(defn available
  "Advertise a capability only when at least one corresponding command remains usable."
  ([permissions administrator?] (available permissions administrator? []))
  ([permissions administrator? policies]
   (let [commands (set (available-commands permissions administrator? policies))]
     (into []
           (comp (filter #(and (permitted? permissions administrator? (:id %))
                               (some (fn [command]
                                       (and (contains? commands (:id command))
                                            (some #{(:id %)} (:capabilities command))))
                                     command-catalog)))
                 (map :id))
           catalog))))
