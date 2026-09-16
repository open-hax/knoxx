(ns knoxx.frontend.law.migration-config
  "Admission of inspected config imports and calls during migration census.")

(defn- admitted-import? [profile {:keys [module clause? default? namespace? named]}]
  (and clause?
       (cond
         (or (= module "vite") (and (= profile :vitest) (= module "vitest/config")))
         (and (not default?) (not namespace?) (seq named) (every? #{"defineConfig"} named))
         (and (= profile :vite) (contains? #{"path" "node:path"} module))
         (and (empty? named) (not= default? namespace?))
         (and (= profile :vite) (= module "@vitejs/plugin-react"))
         (and default? (not namespace?) (empty? named))
         :else false)))

(defn admitted-call?
  "Whether a parsed call invokes a known config helper with its inspected arguments."
  [{:keys [module exported member argument-count dirname-literals?]}]
  (or (and (contains? #{"vite" "vitest/config"} module)
           (= exported "defineConfig") (nil? member) (= argument-count 1))
      (and (= module "@vitejs/plugin-react") (= exported "default")
           (nil? member) (zero? argument-count))
      (and (contains? #{"path" "node:path"} module)
           (contains? #{"default" "*"} exported) (= member "resolve") dirname-literals?)))

(defn assert-source!
  "Reject uninspected config execution while preserving its static expression grammar."
  [{:keys [path profile imports static-statements?]}]
  (when-not (and (every? (partial admitted-import? profile) imports) static-statements?)
    (throw (ex-info (str "Unsupported " (if (= profile :vite) "Vite" "Vitest")
                         " migration configuration")
                    {:path path :detail "Uninspected config imports or executable expressions are not supported"}))))
