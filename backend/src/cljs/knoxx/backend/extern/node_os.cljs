(ns knoxx.backend.extern.node-os
  "The Node effective POSIX identity boundary."
  (:require ["node:process" :as node-process]))

(defn effective-posix-identity!
  "Return the current process's effective numeric POSIX user and group ids."
  []
  (let [^js geteuid (aget node-process "geteuid")
        ^js getegid (aget node-process "getegid")]
    (when-not (and (fn? geteuid) (fn? getegid))
      (throw (js/Error. "sandbox host requires POSIX uid/gid support")))
    {:uid (.call geteuid node-process)
     :gid (.call getegid node-process)}))
