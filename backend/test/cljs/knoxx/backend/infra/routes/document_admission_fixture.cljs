(ns knoxx.backend.infra.routes.document-admission-fixture
  "Explicit resource snapshots, durable event fixtures and dispatch dependencies.")

(defn document
  [id path anchor?]
  {:document/id id
   :document/title (name id)
   :document/source-locale :en
   :document/visibility :public
   :document/source {:path path}
   :document/anchor? anchor?})

(def garden
  {:garden/id :knoxx.gardens/main
   :garden/title "Main"
   :garden/status :active
   :garden/locales [:en :es]})

(defn publication
  [id document-id]
  {:publication/id id
   :publication/document document-id
   :publication/garden :knoxx.gardens/main
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :draft
   :publication/path (str "/" (name id))
   :translation/review :required})

(defn record
  [kind definition suffix]
  {:ok? true
   :resource/kind kind
   :resource/file-path (str "/workspace/contracts/" suffix ".edn")
   :resource/definition definition})

(defn records
  [documents]
  (into [(record :garden garden "garden")]
        (concat
         (map-indexed (fn [idx doc]
                        (record :document doc (str "document-" idx)))
                      documents)
         (map-indexed (fn [idx doc]
                        (record :publication
                                (publication
                                 (keyword "knoxx.publications"
                                          (str (name (:document/id doc)) "-es"))
                                 (:document/id doc))
                                (str "publication-" idx)))
                      documents))))

(def scope
  {:org-id "org-1"
   :membership-id "member-1"
   :project "knoxx-local"})

(defn flush-promises!
  []
  (js/Promise. (fn [resolve _reject]
                 (js/setTimeout resolve 0))))

(defn duplicate-error
  []
  (doto (js/Error. "E11000 duplicate key")
    (aset "code" 11000)))

(defn deferred
  []
  (let [resolve* (atom nil)
        promise (js/Promise.
                 (fn [resolve _reject]
                   (reset! resolve* resolve)))]
    {:promise promise
     :resolve! (fn [value] (@resolve* value))}))

(defn source-roots
  [resource-records]
  (into {}
        (map (fn [entry]
               [(get-in entry [:resource/definition :document/id])
                "/workspace"])
             (filter #(= :document (:resource/kind %)) resource-records))))

(defn persist-once!
  [persisted event]
  (if (contains? @persisted (:id event))
    (throw (duplicate-error))
    (do (swap! persisted assoc (:id event) event)
        (js/Promise.resolve {:ok true :ids [(:id event)]}))))

(defn deps
  [resource-records contents persisted emitted dispatches]
  {:resource-records! (fn [_] (js/Promise.resolve resource-records))
   :document-source-roots (fn [_ _] (source-roots resource-records))
   :canonical-document-path! (fn [root doc]
                               (js/Promise.resolve
                                (str root "/" (get-in doc [:document/source :path]))))
   :source-content! (fn [_root doc]
                      (js/Promise.resolve (get contents (:document/id doc))))
   :draft-complete? (fn [_policy]
                      (js/Promise.resolve false))
   :persist-event! (fn [event] (persist-once! persisted event))
   :emit-indexed! (fn [event]
                    (swap! emitted conj event)
                    (js/Promise.resolve {:matchedTriggers []}))
   :register-turn-settler! (fn [_event-id _settle!] true)
   :unregister-turn-settler! (fn [_event-id] true)
   :release-indexed-event! (fn [_event-id] true)
   :dispatch-document! (fn [document-id snapshot-deps]
                         (swap! dispatches conj
                                {:document-id document-id
                                 :snapshot-deps snapshot-deps})
                         (js/Promise.resolve
                          {:considered 1
                           :admissible 1
                           :runner :agent
                           :dispatched [{:dispatch/outcome
                                         :dispatch/claimed}]}))
   :clock (constantly "2026-09-02T12:00:00.000Z")
   :digest-hex (fn [value] (str "digest-" (hash value)))})
