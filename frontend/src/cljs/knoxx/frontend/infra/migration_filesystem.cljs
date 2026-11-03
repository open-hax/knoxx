(ns knoxx.frontend.infra.migration-filesystem
  "Node file discovery constrained by the migration source-root contracts."
  (:require ["node:fs" :as fs]
            ["node:path" :as node-path]
            [knoxx.frontend.law.migration :as law]))

(defn- relative-path-facts
  "Read platform-relative path facts for the source boundary contract."
  [root target]
  (let [relative (node-path/relative root target)]
    {:relative-path relative
     :absolute? (node-path/isAbsolute relative)
     :separator node-path/sep}))

(defn- symlink-facts
  "Read a symbolic link's target facts without admitting it into the inventory."
  [root path]
  (try
    (let [target (fs/realpathSync path)]
      {:path path :target (relative-path-facts root target)
       :file? (.isFile (fs/statSync target))})
    (catch :default _ {:path path :file? false})))

(defn- walk-files-under
  "Return file paths under directory without leaving the canonical root."
  [root directory]
  (->> (fs/readdirSync directory)
       (mapcat (fn [entry-name]
                 (let [path (node-path/join directory entry-name)
                       stat (fs/lstatSync path)]
                   (cond
                     (.isSymbolicLink stat)
                     (do (law/assert-source-symlink! (symlink-facts root path))
                         [path])

                     (.isDirectory stat) (walk-files-under root path)
                     :else [path]))))
       sort))

(defn walk-files
  "Return sorted absolute file paths without following unsafe symbolic links."
  [root]
  (let [stat (fs/lstatSync root)]
    (law/assert-source-root! {:path root
                              :symbolic-link? (.isSymbolicLink stat)
                              :directory? (.isDirectory stat)}))
  (let [canonical-root (fs/realpathSync root)]
    (walk-files-under canonical-root canonical-root)))

