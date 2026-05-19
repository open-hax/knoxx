(ns knoxx.backend.domain.label.audio
  "Audio file labeling and symlink organization system.
   Labels are stored in a JSON file and can be used to organize
   audio files into symlink-based directory structures."
  (:require [clojure.string :as str]))

;; ── Label Storage ──────────────────────────────────────────────────

(def labels-file "audio-labels.json")

(defn- read-labels-file
  "Read the labels JSON file from the workspace root. Returns promise."
  [fs workspace-root]
  (let [path (str workspace-root "/" labels-file)]
    (-> (.readFile fs path "utf8")
        (.then (fn [content]
                 (-> (js/JSON.parse content)
                     (js->clj :keywordize-keys true))))
        (.catch (fn [_] {})))))

(defn- write-labels-file!
  "Write labels to the JSON file. Returns promise."
  [fs workspace-root labels]
  (let [path (str workspace-root "/" labels-file)
        data (js/JSON.stringify (clj->js labels) nil 2)]
    (.writeFile fs path data "utf8")))

(defn- ensure-labels-file!
  "Create labels file if it doesn't exist. Returns promise of labels."
  [fs workspace-root]
  (let [path (str workspace-root "/" labels-file)]
    (-> (.stat fs path)
        (.catch (fn [_]
                  ;; File doesn't exist, create it
                  (write-labels-file! fs workspace-root {})))
        (.then (fn [_] (read-labels-file fs workspace-root))))))

;; ── Public API ─────────────────────────────────────────────────────

(defn get-labels
  "Get all labels for a file path. Returns promise."
  [fs workspace-root file-path]
  (-> (ensure-labels-file! fs workspace-root)
      (.then (fn [labels]
               (get labels file-path [])))))

(defn get-all-labels
  "Get all unique labels across all files. Returns promise."
  [fs workspace-root]
  (-> (ensure-labels-file! fs workspace-root)
      (.then (fn [labels]
               (->> (vals labels)
                    (apply concat)
                    distinct
                    sort
                    vec)))))

(defn get-files-by-label
  "Get all file paths that have a specific label. Returns promise."
  [fs workspace-root label]
  (-> (ensure-labels-file! fs workspace-root)
      (.then (fn [labels]
               (->> labels
                    (filter (fn [[_ labels]] (some #(= % label) labels)))
                    (map key)
                    vec)))))

(defn add-label!
  "Add a label to a file. Returns promise of updated labels."
  [fs workspace-root file-path label]
  (-> (ensure-labels-file! fs workspace-root)
      (.then (fn [labels]
               (let [current (get labels file-path [])
                     updated (if (some #(= % label) current)
                               current
                               (conj current label))]
                 (-> (write-labels-file! fs workspace-root (assoc labels file-path updated))
                     (.then (fn [_] (vec updated)))))))))

(defn remove-label!
  "Remove a label from a file. Returns promise of updated labels."
  [fs workspace-root file-path label]
  (-> (ensure-labels-file! fs workspace-root)
      (.then (fn [labels]
               (let [current (get labels file-path [])
                     updated (vec (remove #(= % label) current))]
                 (-> (write-labels-file! fs workspace-root (assoc labels file-path updated))
                     (.then (fn [_] updated))))))))

(defn set-labels!
  "Set all labels for a file (replaces existing). Returns promise."
  [fs workspace-root file-path new-labels]
  (-> (ensure-labels-file! fs workspace-root)
      (.then (fn [labels]
               (-> (write-labels-file! fs workspace-root (assoc labels file-path (vec new-labels)))
                   (.then (fn [_] (vec new-labels))))))))

;; ── Symlink Organization ───────────────────────────────────────────

(defn- sanitize-dirname
  "Sanitize a label for use as a directory name."
  [label]
  (-> label
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-|-$" "")))

(defn- create-symlinks-for-label
  "Create symlinks for a single label. Returns promise."
  [fs node-path audio-dir label files]
  (let [label-dir (str audio-dir "/" (sanitize-dirname label))]
    (-> (.mkdir fs label-dir #js {:recursive true})
        (.catch (fn [_] nil))
        (.then (fn [_]
                 (let [create-link (fn [file-path]
                                     (let [filename (.basename node-path file-path)
                                           link-path (str label-dir "/" filename)]
                                       (-> (.symlink fs file-path link-path)
                                           (.catch (fn [_] nil)))))]
                   (js/Promise.all (clj->js (map create-link files)))))))))

(defn- count-symlinks
  "Count total symlinks in audio directory. Returns promise."
  [fs audio-dir]
  (-> (.readdir fs audio-dir)
      (.then (fn [dirs]
               (let [dir-paths (map (fn [d] (str audio-dir "/" d)) (js->clj dirs))
                     count-dir (fn [dp]
                                 (-> (.stat fs dp)
                                     (.then (fn [stat]
                                              (if (.isDirectory stat)
                                                (-> (.readdir fs dp)
                                                    (.then (fn [f] (count (js->clj f))))
                                                    (.catch (fn [_] 0)))
                                                0)))
                                     (.catch (fn [_] 0))))]
                 (-> (js/Promise.all (clj->js (map count-dir dir-paths)))
                     (.then (fn [counts]
                              (reduce + 0 (js->clj counts))))))))
      (.catch (fn [_] 0))))

(defn sync-symlinks!
  "Create symlink directory structure for labeled files.
   Creates ./audio/<label>/ symlinks pointing to original files.
   Returns promise of symlink count."
  [fs node-path workspace-root]
  (-> (ensure-labels-file! fs workspace-root)
      (.then (fn [labels]
               (let [audio-dir (str workspace-root "/audio")
                     all-labels (->> (vals labels)
                                     (apply concat)
                                     distinct)
                     label-files (fn [label]
                                   (->> labels
                                        (filter (fn [[_ lbls]] (some #(= % label) lbls)))
                                        (map key)))]
                 (-> (.mkdir fs audio-dir #js {:recursive true})
                     (.catch (fn [_] nil))
                     (.then (fn [_]
                              (let [process-label (fn [label]
                                                    (create-symlinks-for-label
                                                     fs node-path audio-dir label (label-files label)))]
                                (js/Promise.all (clj->js (map process-label all-labels))))))
                     (.then (fn [_] (count-symlinks fs audio-dir)))))))))
