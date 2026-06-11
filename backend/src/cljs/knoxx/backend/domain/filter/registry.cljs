(ns knoxx.backend.domain.filter.registry
  "Registry for scope filter functions.

   Filters are pure functions actions can request through :action/scope:

     :action/scope {:filters [:vector/exclude-shared]}

   Resolved filters are injected into the action's ctx :scope under their
   registered keyword, alongside bound actions and stores.")

(defonce ^:private registry* (atom {}))

(defn register-filter!
  "Register a pure filter function under a namespaced keyword id."
  [id f]
  (swap! registry* assoc id f))

(defn filter-fn
  "Look up a filter function by id. Returns nil if not found."
  [id]
  (get @registry* id))

(defn filter-ids
  "Return all registered filter ids."
  []
  (vec (sort (keys @registry*))))

;; ── Built-in filters ───────────────────────────────────────────────────

;; Remove items from candidates that already appear in seen, comparing by an
;; identity key (defaults to :id).
(register-filter!
 :vector/exclude-shared
 (fn exclude-shared
   ([candidates seen] (exclude-shared candidates seen :id))
   ([candidates seen identity-key]
    (let [seen-ids (into #{} (map identity-key) seen)]
      (filterv #(not (contains? seen-ids (identity-key %))) candidates)))))
