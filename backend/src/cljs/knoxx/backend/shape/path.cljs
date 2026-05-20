(defn normalize-devel-path
  [value]
  (let [trimmed (trim-mention-token value)
        no-prefix (cond
                    (str/starts-with? trimmed "/app/workspace/devel/") (subs trimmed (count "/app/workspace/devel/"))
                    (str/starts-with? trimmed (:workspace-root (cfg))) (subs trimmed (inc (count (:workspace-root (cfg)))))
                    :else trimmed)
        normalized (normalize-relative-path no-prefix)]
    (when (and (not (str/blank? normalized))
               (re-find #"^(orgs|packages|services|docs|spec|specs|tools|ecosystems|src|worktrees|\.ημ)/" normalized))
      normalized)))
(defn- basename
  [path]
  (let [s (-> (str path)
              (str/replace #"\\\\" "/")
              (str/replace #"/+" "/"))
        parts (->> (str/split s #"/")
                   (remove str/blank?))]
    (or (last parts) s)))
