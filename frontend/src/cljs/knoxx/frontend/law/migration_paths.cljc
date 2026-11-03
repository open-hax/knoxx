(ns knoxx.frontend.law.migration-paths
  "Pure containment and source-root obligations for migration file discovery."
  (:require [clojure.string :as str]))

(defn contained-path?
  "Whether platform-relative path facts remain inside their source root."
  [{:keys [relative-path absolute? separator]}]
  (and (not absolute?)
       (not= relative-path "..")
       (not (str/starts-with? relative-path (str ".." separator)))))

(defn assert-source-root!
  "Admit only a real directory as the governed source root."
  [{:keys [path symbolic-link? directory?] :as facts}]
  (when (or symbolic-link? (not directory?))
    (throw (ex-info "Unsafe migration source root" {:path path})))
  facts)

(defn assert-source-symlink!
  "Admit only symbolic links to regular files inside the source root."
  [{:keys [path target file?] :as facts}]
  (when-not (and file? (contained-path? target))
    (throw (ex-info "Unsafe symbolic link in migration source tree" {:path path})))
  facts)

(defn assert-import-target!
  "Admit a contained resolved import target and preserve its source evidence."
  [{:keys [target] :as facts}]
  (when-not (contained-path? facts)
    (throw (ex-info "Local import leaves governed frontend source tree"
                    (select-keys facts [:path :source :target]))))
  target)

(defn assert-vite-alias-target!
  "Admit an alias whose lexical and available canonical targets stay contained."
  [{:keys [target lexical canonical] :as facts}]
  (when (or (not (contained-path? lexical))
            (and canonical (not (contained-path? canonical))))
    (throw (ex-info "Vite alias leaves governed frontend source tree"
                    (select-keys facts [:path :target]))))
  target)

