(ns knoxx.backend.infra.publication-draft-store
  "Content-addressed persistence for agent-crafted publication drafts."
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.domain.publication-draft :as draft]
            [knoxx.backend.extern.publication-draft-store :as xdraft-store]))

(defn- generated-contract-root!
  [config]
  (let [configured (some-> (:generated-contracts-dir config) str str/trim not-empty)]
    (when-not configured
      (throw (ex-info "KNOXX_GENERATED_CONTRACTS_DIR is required for generated drafts"
                      {:code :generated-contracts-dir-unconfigured})))
    configured))

(defn draft-paths
  [config resources]
  (let [contracts-root (generated-contract-root! config)
        local-id (name (:draft/id resources))]
    (xdraft-store/draft-paths contracts-root local-id
                              (:draft/source-path resources))))

(defn ^:async draft-materialized?
  "True when immutable source and manifest bytes have been materialized.

   This is intentionally not evidence that recursive admission completed."
  [config input]
  (let [identity (draft/draft-identity input)
        {:keys [content-path manifest-path]} (draft-paths config identity)]
    (and (await (xdraft-store/file-exists? content-path))
         (await (xdraft-store/file-exists? manifest-path)))))

(defn- completion-marker
  [resources]
  {:draft/id (:draft/id resources)
   :draft/policy-fingerprint (:draft/policy-fingerprint resources)
   :draft/admission-complete? true})

(defn- completion-text
  [resources]
  (str (pr-str (completion-marker resources)) "\n"))

(defn- ^:async completion-marked?
  "True when the exact topology has a valid recursive-admission marker.

   This deliberately says nothing about whether the immutable source and
   manifest files still exist."
  [config input]
  (let [identity (draft/draft-identity input)
        completion-path (:completion-path (draft-paths config identity))
        existing (await (xdraft-store/read-text-or-nil! completion-path))
        expected (completion-text identity)]
    (cond
      (nil? existing) false
      (= expected existing) true
      :else
      (throw
       (ex-info "generated draft completion marker has conflicting bytes"
                {:code :generated-draft-completion-conflict
                 :path completion-path})))))

(defn ^:async draft-complete?
  "True only when the exact topology has a valid recursive-admission marker
   and both immutable draft files still exist."
  [config input]
  (and (await (completion-marked? config input))
       (await (draft-materialized? config input))))

(defn- ^:async same-file-result
  [file-path content]
  (let [existing (await (xdraft-store/read-text! file-path))]
    (if (= existing content)
      {:path file-path :created? false}
      (throw (ex-info "generated draft identity already has different bytes"
                      {:code :generated-draft-conflict
                       :path file-path})))))

(defn- ^:async write-once!
  "Install complete immutable bytes, or validate the incumbent writer's bytes.

   The final path is claimed only after a sibling temp file has been fully
   written and fsynced. A killed or ENOSPC-failed writer can therefore leave
   temp debris, but cannot expose a torn source, manifest, or completion file."
  [file-path content]
  (if (await (xdraft-store/install-text-exclusive! file-path content))
    {:path file-path :created? true}
    (await (same-file-result file-path content))))

(defn ^:async persist!
  "Persist content first and its resource manifest second.

   The resource loader can therefore never observe a manifest whose source is
   absent. Both writes are create-only: byte-equal replay is idempotent and a
   different model answer for the same source revision is a visible conflict."
  [config input]
  (let [resources (draft/draft-resources input)
        {:keys [content-path manifest-path] :as paths} (draft-paths config resources)
        content-write (await (write-once! content-path (:draft/content resources)))
        manifest-text (str (pr-str (:draft/manifest resources)) "\n")
        manifest-write (await (write-once! manifest-path manifest-text))]
    (contract-loader/invalidate-sync-contract-cache!)
    (merge resources paths
           {:draft/created? (or (:created? content-write)
                                (:created? manifest-write))})))

(defn- conflict!
  [message file-path]
  (throw (ex-info message
                  {:code :generated-draft-conflict
                   :path file-path})))

(defn- persisted-title!
  [manifest document-id manifest-path]
  (or (some (fn [resource]
              (when (= document-id (:document/id resource))
                (:document/title resource)))
            (:resources manifest))
      (conflict! "generated draft manifest does not contain its document"
                 manifest-path)))

(defn- parse-manifest!
  [manifest-text manifest-path]
  (try
    (reader/read-string manifest-text)
    (catch :default error
      (throw (ex-info "generated draft manifest is not readable EDN"
                      {:code :generated-draft-conflict
                       :path manifest-path}
                      error)))))

(defn- ^:async resume-materialized!
  "Load and validate the immutable bytes left by an incomplete admission.

   The later model answer is deliberately ignored: draft identity is bound to
   source and publication topology, so retry must admit the already-persisted
   artifact rather than require a nondeterministic model to reproduce it."
  [config input]
  (let [identity (draft/draft-identity input)
        {:keys [content-path manifest-path] :as paths}
        (draft-paths config identity)
        content (await (xdraft-store/read-text! content-path))
        manifest-text (await (xdraft-store/read-text! manifest-path))
        manifest (parse-manifest! manifest-text manifest-path)
        title (persisted-title! manifest (:draft/id identity) manifest-path)
        resources (draft/draft-resources (assoc input
                                                :title title
                                                :content content))
        expected-manifest-text (str (pr-str (:draft/manifest resources)) "\n")]
    (when-not (= expected-manifest-text manifest-text)
      (conflict! "generated draft manifest has conflicting bytes"
                 manifest-path))
    (contract-loader/invalidate-sync-contract-cache!)
    (merge resources paths
           {:draft/created? false
            :draft/resumed? true})))

(defn- ^:async resume-content-prefix!
  "Finish the only recoverable one-file prefix left by `persist!`.

   Content is written before the manifest, so those bytes are the durable
   answer even when a later model retry differs. The original optional title
   was never persisted in this crash window and cannot be recovered. Rebuild
   it from the first persisted Markdown heading (or the pinned source-document
   fallback) by deliberately omitting the retry's title."
  [config input]
  (let [identity (draft/draft-identity input)
        {:keys [content-path manifest-path]}
        (draft-paths config identity)
        content (await (xdraft-store/read-text! content-path))
        recovered-input (assoc input :title nil :content content)
        resources (draft/draft-resources recovered-input)
        manifest-text (str (pr-str (:draft/manifest resources)) "\n")]
    (await (write-once! manifest-path manifest-text))
    (await (resume-materialized! config recovered-input))))

(defn- ^:async persist-unmarked!
  [config input content? manifest? content-path]
  (cond
    (and content? manifest?)
    ;; Close the harmless race in which another retry wrote the marker after
    ;; our first read. Otherwise the persisted artifact owns this retry.
    (if (await (completion-marked? config input))
      (await (persist! config input))
      (await (resume-materialized! config input)))

    content?
    (await (resume-content-prefix! config input))

    ;; `persist!` writes content first, so manifest-only state is not a lawful
    ;; crash prefix: the missing body is not recoverable and replacement model
    ;; bytes must not assume its identity.
    manifest?
    (conflict! "generated draft manifest exists without immutable content"
               content-path)

    :else
    (await (persist! config input))))

(defn ^:async persist-or-resume!
  "Persist a new draft, or resume the exact materialized draft whose recursive
   admission has not completed. Completed drafts retain byte-conflict checks;
   a completion marker with missing files never authorizes replacement bytes."
  [config input]
  (let [identity (draft/draft-identity input)
        {:keys [content-path manifest-path]} (draft-paths config identity)
        marked? (await (completion-marked? config input))
        content? (await (xdraft-store/file-exists? content-path))
        manifest? (await (xdraft-store/file-exists? manifest-path))]
    (cond
      (and marked? content? manifest?) (await (persist! config input))
      marked? (conflict! "completed generated draft is missing immutable files"
                         (if-not content? content-path manifest-path))
      :else (await (persist-unmarked! config input content? manifest?
                                      content-path)))))

(defn ^:async mark-complete!
  "Write the deterministic recursive-admission marker once.

   Callers must establish a coherent successful admission result first. A
   missing marker keeps already-materialized bytes eligible for retry."
  [config resources]
  (let [{:keys [completion-path]} (draft-paths config resources)
        result (await (write-once! completion-path
                                   (completion-text resources)))]
    {:draft/completion-path completion-path
     :draft/completion-created? (:created? result)
     :draft/admission-complete? true}))
