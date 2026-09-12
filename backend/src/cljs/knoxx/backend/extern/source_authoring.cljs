(ns knoxx.backend.extern.source-authoring
  "Canonical filesystem projection and per-document sequencing for source facts."
  (:require ["node:crypto" :as crypto]
            ["node:fs/promises" :as fs]
            ["node:path" :as path]
            [cljs.reader :as reader]
            [knoxx.backend.law.source-review :as law]))

(defonce ^:private tails (atom {}))
(defn- ^:async after! [previous operation]
  (when previous (try (await previous) (catch :default _ nil)))
  (await (operation)))
(defn with-document-lock!
  "Hold sequencing across every await; failed operations cannot poison later repair."
  [key operation]
  (when-not (fn? operation) (throw (ex-info "Invalid source operation" {:status 500})))
  (let [task (after! (get @tails key) operation)]
    (swap! tails assoc key task)
    (.then task (fn [_] (when (identical? task (get @tails key)) (swap! tails dissoc key)))
           (fn [_] (when (identical? task (get @tails key)) (swap! tails dissoc key))))
    task))
(defn- missing? [error] (= "ENOENT" (.-code error)))
(defn ^:async read-text! "Read source bytes as UTF-8, distinguishing absence from failure." [file]
  (try (await (.readFile fs file "utf8")) (catch :default error (if (missing? error) nil (throw error)))))
(defn parse-manifest "Decode one resource manifest at the owning EDN boundary." [text]
  (try (reader/read-string text)
       (catch :default cause (throw (ex-info "Invalid source resource manifest" {:status 409 :code "source_manifest_invalid"} cause)))))
(defn manifest-text "Encode deterministic readable source resource data." [manifest]
  (str (pr-str manifest) "\n"))
(defn- inside? [root target]
  (let [relative (.relative path root target)]
    (and (not (.isAbsolute path relative)) (not= ".." relative) (not (.startsWith relative (str ".." (.-sep path)))))))
(defn- contained! [root target]
  (when-not (inside? root target)
    (throw (ex-info "Source projection escapes resource provenance" {:status 409 :code "document_source_outside_provenance_root"}))) target)
(defn- ^:async real-parent! [root parent]
  (try (contained! root (await (.realpath fs parent)))
       (catch :default error
         (if (missing? error)
           (let [ancestor (await (real-parent! root (.dirname path parent)))]
             (.join path ancestor (.basename path parent)))
           (throw error)))))
(defn ^:async contained-path!
  "Resolve existing symlinks and prove the nearest existing parent is inside the root."
  [root relative]
  (law/assert-valid! :source-authoring/root law/NonBlank root)
  (law/assert-valid! :source-authoring/path law/NonBlank relative)
  (let [canonical-root (await (.realpath fs root))
        candidate (contained! canonical-root (.resolve path canonical-root relative))
        parent (await (real-parent! canonical-root (.dirname path candidate)))]
    (try (contained! canonical-root (await (.realpath fs candidate)))
         (catch :default error
           (if (missing? error) (contained! canonical-root (.join path parent (.basename path candidate))) (throw error))))))
(defn ^:async write-text!
  "Atomically replace one projection file after canonical parent containment checks.

  This is file atomicity only. Durable admission precedes projection, so interrupted
  projection is detected by the source digest and repaired by retrying its command."
  [root relative content]
  (law/assert-valid! :source-authoring/content law/NonBlank content)
  (let [destination (await (contained-path! root relative)) parent (.dirname path destination)]
    (await (.mkdir fs parent #js {:recursive true}))
    (let [checked (await (contained-path! root relative))
          temporary (.join path (.dirname path checked) (str ".source-" (crypto/randomUUID) ".tmp"))]
      (try
        (let [handle (await (.open fs temporary "wx" 384))]
          (try (await (.writeFile handle content "utf8")) (await (.sync handle))
               (finally (await (.close handle)))))
        (await (.rename fs temporary checked))
        checked
        (finally (try (await (.unlink fs temporary)) (catch :default error (when-not (missing? error) (throw error)))))))))
