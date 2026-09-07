(ns knoxx.frontend.infra.migration-packages
  "Filesystem and package metadata for local dependency containment."
  (:require ["node:fs" :as fs]
            ["node:path" :as node-path]
            [clojure.string :as str]))

(defn resolver
  "Create a per-inventory package metadata cache."
  [root]
  {:root root :cache (atom {})})

(defn- package-name [specifier]
  (when-not (or (str/starts-with? specifier ".") (node-path/isAbsolute specifier))
    (let [parts (str/split specifier #"/")]
      (str/join "/" (take (if (str/starts-with? specifier "@") 2 1) parts)))))

(defn- dependencies [cache directory]
  (if-let [known (get @cache directory)]
    known
    (let [file (node-path/join directory "package.json")
          data (when (fs/existsSync file)
                 (js->clj (js/JSON.parse (fs/readFileSync file "utf8"))))
          declared (apply merge (map #(get data %) ["peerDependencies" "devDependencies"
                                                    "dependencies" "optionalDependencies"]))]
      (swap! cache assoc directory (or declared {}))
      (or declared {}))))

(defn- declaration [{:keys [root cache]} path specifier]
  (when-let [dependency-name (package-name specifier)]
    (loop [directory (node-path/dirname path)]
      (let [declared (dependencies cache directory)
            parent (node-path/dirname directory)]
        (if (contains? declared dependency-name)
          {:name dependency-name :directory directory :version (get declared dependency-name)}
          (when-not (or (= directory root) (= directory parent))
            (recur parent)))))))

(defn- local-declaration [context path specifier]
  (when-let [{:keys [version] :as declared} (declaration context path specifier)]
    (when (and (string? version)
               (or (re-find #"^(?:file|link|workspace):" version)
                   (str/starts-with? version ".") (node-path/isAbsolute version)))
      declared)))

(defn declared-target
  "Return an explicitly local dependency's source directory when it is named."
  [context path specifier]
  (when context
    (when-let [{:keys [directory version]} (local-declaration context path specifier)]
      (let [target (str/replace version #"^(?:file|link|workspace):" "")]
        (when-not (and (str/starts-with? version "workspace:")
                       (not (str/starts-with? target "."))
                       (not (node-path/isAbsolute target)))
          (let [source (node-path/resolve directory target)]
            (when (and (fs/existsSync source) (not (.isDirectory (fs/statSync source))))
              (throw (ex-info "Unsupported local package source; expected a directory"
                              {:path source :dependency specifier})))
            source))))))

(defn resolved-target
  "Return local source behind a resolution; ordinary installed packages return nil."
  [context path specifier resolved-file external?]
  (let [real-file (fs/realpathSync resolved-file)
        local (when context (local-declaration context path specifier))
        marker (str node-path/sep "node_modules" node-path/sep)
        installed? (str/includes? real-file marker)]
    (when (or (not external?) local (not installed?))
      (if-let [source-root (when (and local installed?) (declared-target context path specifier))]
        (let [package-marker (str marker (:name local) node-path/sep)
              index (.lastIndexOf real-file package-marker)]
          (if (neg? index)
            real-file
            (node-path/resolve source-root (subs real-file (+ index (count package-marker))))))
        real-file))))
