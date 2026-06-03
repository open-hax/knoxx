(ns knoxx.backend.domain.label.audio
  "Audio file labeling and symlink organization system.
   Labels are stored in a JSON file and can be used to organize
   audio files into symlink-based directory structures."
  (:require [clojure.string :as str]))

;; ── Label Storage ──────────────────────────────────────────────────

(def labels-file "audio-labels.json")

(defn- ^:async read-labels-file
  "Read the labels JSON file from the workspace root. Returns promise."
  [fs workspace-root]
  (let [path (str workspace-root "/" labels-file)]
    (try
      (let [content (await (.readFile fs path "utf8"))]
        (-> (js/JSON.parse content)
            (js->clj :keywordize-keys true)))
      (catch :default _ {}))))

(defn- write-labels-file!
  "Write labels to the JSON file. Returns promise."
  [fs workspace-root labels]
  (let [path (str workspace-root "/" labels-file)
        data (js/JSON.stringify (clj->js labels) nil 2)]
    (.writeFile fs path data "utf8")))

(defn- ^:async ensure-labels-file!
  "Create labels file if it doesn't exist. Returns promise of labels."
  [fs workspace-root]
  (let [path (str workspace-root "/" labels-file)]
    (try
      (await (.stat fs path))
      (catch :default _
        (await (write-labels-file! fs workspace-root {}))))
    (await (read-labels-file fs workspace-root))))

;; ── Public API ─────────────────────────────────────────────────────

(defn ^:async get-labels
  "Get all labels for a file path. Returns promise."
  [fs workspace-root file-path]
  (let [labels (await (ensure-labels-file! fs workspace-root))]
    (get labels file-path [])))

(defn ^:async get-all-labels
  "Get all unique labels across all files. Returns promise."
  [fs workspace-root]
  (let [labels (await (ensure-labels-file! fs workspace-root))]
    (->> (vals labels)
         (apply concat)
         distinct
         sort
         vec)))

(defn ^:async get-files-by-label
  "Get all file paths that have a specific label. Returns promise."
  [fs workspace-root label]
  (let [labels (await (ensure-labels-file! fs workspace-root))]
    (->> labels
         (filter (fn [[_ labels]] (some #(= % label) labels)))
         (map key)
         vec)))

(defn ^:async add-label!
  "Add a label to a file. Returns promise of updated labels."
  [fs workspace-root file-path label]
  (let [labels (await (ensure-labels-file! fs workspace-root))
        current (get labels file-path [])
        updated (if (some #(= % label) current)
                  current
                  (conj current label))]
    (await (write-labels-file! fs workspace-root (assoc labels file-path updated)))
    (vec updated)))

(defn ^:async remove-label!
  "Remove a label from a file. Returns promise of updated labels."
  [fs workspace-root file-path label]
  (let [labels (await (ensure-labels-file! fs workspace-root))
        current (get labels file-path [])
        updated (vec (remove #(= % label) current))]
    (await (write-labels-file! fs workspace-root (assoc labels file-path updated)))
    updated))

(defn ^:async set-labels!
  "Set all labels for a file (replaces existing). Returns promise."
  [fs workspace-root file-path new-labels]
  (let [labels (await (ensure-labels-file! fs workspace-root))]
    (await (write-labels-file! fs workspace-root (assoc labels file-path (vec new-labels))))
    (vec new-labels)))

;; ── Symlink Organization ───────────────────────────────────────────

(defn- sanitize-dirname
  "Sanitize a label for use as a directory name."
  [label]
  (-> label
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-|-$" "")))

(defn- ^:async create-symlink!
  [fs node-path label-dir file-path]
  (let [filename (.basename node-path file-path)
        link-path (str label-dir "/" filename)]
    (try
      (await (.symlink fs file-path link-path))
      (catch :default _ nil))))

(defn- ^:async create-symlinks-for-label
  "Create symlinks for a single label. Returns promise."
  [fs node-path audio-dir label files]
  (let [label-dir (str audio-dir "/" (sanitize-dirname label))]
    (try
      (await (.mkdir fs label-dir #js {:recursive true}))
      (catch :default _ nil))
    (await (js/Promise.all (clj->js (map (partial create-symlink! fs node-path label-dir) files))))))

(defn- ^:async count-dir-files
  [fs dp]
  (try
    (let [stat (await (.stat fs dp))]
      (if (.isDirectory stat)
        (try
          (let [f (await (.readdir fs dp))]
            (count (js->clj f)))
          (catch :default _ 0))
        0))
    (catch :default _ 0)))

(defn- ^:async count-symlinks
  "Count total symlinks in audio directory. Returns promise."
  [fs audio-dir]
  (try
    (let [dirs (await (.readdir fs audio-dir))
          dir-paths (map (fn [d] (str audio-dir "/" d)) (js->clj dirs))
          counts (await (js/Promise.all (clj->js (map (partial count-dir-files fs) dir-paths))))]
      (reduce + 0 (js->clj counts)))
    (catch :default _ 0)))

(defn ^:async sync-symlinks!
  "Create symlink directory structure for labeled files.
   Creates ./audio/<label>/ symlinks pointing to original files.
   Returns promise of symlink count."
  [fs node-path workspace-root]
  (let [labels (await (ensure-labels-file! fs workspace-root))
        audio-dir (str workspace-root "/audio")
        all-labels (->> (vals labels)
                        (apply concat)
                        distinct)
        label-files (fn [label]
                      (->> labels
                           (filter (fn [[_ lbls]] (some #(= % label) lbls)))
                           (map key)))]
    (try
      (await (.mkdir fs audio-dir #js {:recursive true}))
      (catch :default _ nil))
    (let [process-label (fn [label]
                          (create-symlinks-for-label
                           fs node-path audio-dir label (label-files label)))]
      (await (js/Promise.all (clj->js (map process-label all-labels))))
      (await (count-symlinks fs audio-dir)))))

