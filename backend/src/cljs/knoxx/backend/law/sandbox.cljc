(ns knoxx.backend.law.sandbox
  "Pure classifications for sandbox command outcomes.

   Container execution stays in the domain/extern runtime boundary. This law
   only decides whether a Knoxx-owned command fulfilled its contract and how
   much failure evidence is safe to return."
  (:require [clojure.string :as str]))

(def max-internal-command-detail-chars
  "Maximum stderr/error characters retained in a tool-level failure."
  2000)

(defn exact-git-safe-directory
  "Return one exact absolute sandbox workdir that Git may trust.

   Git rejects a bind-mounted repository when the mount owner differs from the
   non-root container user.  The runtime may grant trust to that one workdir,
   but never to a relative path, the filesystem root, or a wildcard pattern."
  [workdir]
  (let [workdir (some-> workdir str str/trim not-empty)
        segments (when workdir (rest (str/split workdir #"/" -1)))]
    (when (or (nil? workdir)
              (not (str/starts-with? workdir "/"))
              (= workdir "/")
              (str/includes? workdir "*")
              (some #{"" "." ".."} segments))
      (throw (ex-info "sandbox git safe.directory must be one exact absolute workdir"
                      {:sandbox/workdir workdir})))
    workdir))

(defn- first-nonblank
  [values]
  (some (fn [value]
          (let [text (str (or value ""))]
            (when-not (str/blank? text) text)))
        values))

(defn- bounded-detail
  [detail]
  (let [detail (str (or detail ""))]
    (subs detail 0 (min max-internal-command-detail-chars (count detail)))))

(defn internal-command-failure
  "Normalize a failed Knoxx-owned command, or return nil for success.

   This law is deliberately not applied to a user-requested sandbox exec: a
   nonzero user command is an observed result. Read, write, and commit are
   Knoxx-owned implementations, so a nonzero result means their promise was
   not fulfilled and must become a tool error."
  [operation result]
  (when-not (= true (:ok result))
    (let [operation (str operation)
          exit-code (or (:exitCode result) 1)
          detail (bounded-detail
                  (first-nonblank [(:stderr result) (:error result)]))]
      {:operation operation
       :exit-code exit-code
       :detail detail
       :message (str operation " failed (exit " exit-code ")"
                     (when-not (str/blank? detail)
                       (str ": " detail)))})))
