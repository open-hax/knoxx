(ns knoxx.backend.infra.publication-target-registry
  "Runtime composition for resource-declared publication targets.

   This namespace resolves target identity and configuration into a Knoxx-owned
   adapter. It does not participate in pure reconciliation. In particular,
   locale admissibility is not inferred from an artifact or publication intent:
   callers must supply the locale-catalog guard before a publish can run."
  (:require [knoxx.backend.infra.publication-effects :as effects]
            [knoxx.backend.law.publication-target :as law]))

(defn make-registry
  "Validate declarations and register kind-specific adapter factories.

   Each factory receives one complete validated declaration and returns an
   `IPublicationTarget`; keeping construction here prevents publication call
   sites from choosing or constructing adapters themselves."
  [declarations factories]
  {:declarations (into {} (map (juxt :publication-target/id identity)
                                (law/assert-declarations! declarations)))
   :factories factories})

(defn resolve-target!
  "Resolve `target-id` to its enabled, registered adapter or throw before any
   publication effect can be invoked.

   The constructed adapter must report the declared stable identity. This keeps
   idempotency keys resource-owned even when adapters are replaceable."
  [registry target-id]
  (let [declaration (get-in registry [:declarations target-id])]
    (when-not declaration
      (throw (ex-info "Unknown publication target" {:publication-target/id target-id})))
    (when-not (:publication-target/enabled? declaration)
      (throw (ex-info "Disabled publication target" {:publication-target/id target-id})))
    (let [factory (get-in registry [:factories (:publication-target/kind declaration)])]
      (when-not factory
        (throw (ex-info "Unregistered publication target kind"
                        {:publication-target/id target-id
                         :publication-target/kind (:publication-target/kind declaration)})))
      (let [target (factory declaration)]
        (when-not (satisfies? effects/IPublicationTarget target)
          (throw (ex-info "Publication target factory returned no IPublicationTarget"
                          {:publication-target/id target-id
                           :publication-target/kind (:publication-target/kind declaration)})))
        (when-not (= target-id (effects/target-id target))
          (throw (ex-info "Publication target adapter identity differs from declaration"
                          {:publication-target/id target-id
                           :adapter/id (effects/target-id target)})))
        target))))

(defn- assert-locale-admitted!
  "Require the separately owned locale-catalog guard for a publish operation.

   The guard receives the unchanged declaration, intent, and artifact. No
   default is supplied: accepting every artifact locale here would turn adapter
   selection into the locale policy this registry explicitly does not own."
  [locale-admissible? declaration intent artifact]
  (when-not (fn? locale-admissible?)
    (throw (ex-info "Publication target registry requires a locale admission guard"
                    {:publication-target/id (:publication-target/id declaration)})))
  (when-not (locale-admissible? declaration intent artifact)
    (throw (ex-info "Publication locale is not admitted by target catalog"
                    {:publication-target/id (:publication-target/id declaration)
                     :publication/locale (:publication/locale intent)
                     :artifact/locale (:artifact/locale artifact)}))))

(defn ^:async execute-plan!
  "Resolve the plan's resource-named target, admit publish locales through the
   supplied catalog guard, then delegate with the exact context received.

   Target resolution and locale admission happen before `effects/execute-plan!`,
   so unknown, disabled, malformed, and unadmitted targets cannot call
   `publish!`, `remove!`, or `observe!`."
  [registry store ctx plan artifact locale-admissible?]
  (let [intent (:intent plan)
        target-id (:publication/target intent)
        declaration (get-in registry [:declarations target-id])
        target (resolve-target! registry target-id)]
    (when (= :publish (:op plan))
      (assert-locale-admitted! locale-admissible? declaration intent artifact))
    (await (effects/execute-plan! store target ctx plan artifact))))
