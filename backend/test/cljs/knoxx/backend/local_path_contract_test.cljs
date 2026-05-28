(ns knoxx.backend.local-path-contract-test
  (:require [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(def scanned-roots ["src/cljs" "test/cljs" "scripts"])

(def scanned-files ["package.json" "shadow-cljs.edn" "Dockerfile"])

(def source-extensions #{".cljs" ".clj" ".edn" ".mjs" ".cjs" ".js" ".json"})

(defn- ascii
  [& codes]
  (.apply (.-fromCharCode js/String) js/String (clj->js codes)))

(def legacy-project-name (ascii 100 101 118 101 108))
(def legacy-user-name (ascii 101 114 114))
(def home-prefix (ascii 47 104 111 109 101 47))
(def container-workspace-prefix (ascii 47 97 112 112 47 119 111 114 107 115 112 97 99 101 47))

(def forbidden-patterns
  [{:label "developer-home checkout path"
    :pattern (js/RegExp. (str home-prefix "[^/]+/" legacy-project-name))}
   {:label "single-user home path"
    :pattern (js/RegExp. (str home-prefix legacy-user-name))}
   {:label "legacy container workspace mount"
    :pattern (js/RegExp. (str container-workspace-prefix legacy-project-name))}
   {:label "legacy local docs collection default"
    :pattern (js/RegExp. (str legacy-project-name "_docs"))}
   {:label "legacy local corpus prompt"
    :pattern (js/RegExp. (str legacy-project-name "\\s+corpus"))}])

(defn- source-file?
  [file-path]
  (contains? source-extensions (.extname path file-path)))

(defn- js-array-values
  [entries]
  (mapv #(aget entries %) (range (.-length entries))))

(defn- files-under
  [dir]
  (when (.existsSync fs dir)
    (let [entries (.readdirSync fs dir #js {:withFileTypes true})]
      (reduce (fn [acc entry]
                (let [entry-path (.join path dir (.-name entry))]
                  (cond
                    (.isDirectory entry)
                    (into acc (files-under entry-path))

                    (source-file? entry-path)
                    (conj acc entry-path)

                    :else
                    acc)))
              []
              (js-array-values entries)))))

(defn- scanned-source-files
  []
  (vec (concat (mapcat files-under scanned-roots)
               (filter #(.existsSync fs %) scanned-files))))

(defn- file-violations
  [file-path]
  (let [text (.readFileSync fs file-path "utf8")]
    (->> forbidden-patterns
         (keep (fn [{:keys [label pattern]}]
                 (when (.test pattern text)
                   {:file file-path :label label}))))))

(deftest backend-source-and-tests-do-not-pin-local-workspace-paths
  (let [violations (vec (mapcat file-violations (scanned-source-files)))]
    (is (= [] violations)
        (str "Backend source/test surfaces must stay portable; local path pins found:\n"
             (str/join "\n" (map (fn [{:keys [file label]}]
                                      (str "- " file " :: " label))
                                    violations))))))
