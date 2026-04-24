(ns knoxx.backend.agents.tools
  "Tool input preview and assistant tool-call helpers."
  (:require [clojure.string :as str]
            [knoxx.backend.agents.content :refer [nonblank preview-text-nonblank json-preview-nonblank fenced]]
            [knoxx.backend.text :refer [clip-text]]))

(defn- tool-args->markdown-preview
  "Tool-specific input previews that are always human readable (no raw JSON).

   This is intentionally conservative: we only special-case tools where we know
   the user expectation is strict (bash/read). Everything else falls back to
   value->preview-text rendering (which the frontend formats into bullets)."
  [tool-name raw-args]
  (let [tool-name (-> (str (or tool-name ""))
                      (str/split #"[./:]")
                      last
                      str/lower-case)
        args (cond
               (map? raw-args) raw-args
               (and raw-args (not= raw-args js/undefined))
               (try
                 (js->clj raw-args :keywordize-keys true)
                 (catch :default _ nil))
               :else nil)
        arg-value (fn [& keys]
                    (or (some (fn [k]
                                (cond
                                  (and (map? args) (keyword? k)) (get args k)
                                  (and (map? args) (string? k)) (or (get args k) (get args (keyword k)))
                                  :else nil))
                              keys)
                        (some (fn [k]
                                (cond
                                  (and (map? raw-args) (keyword? k)) (get raw-args k)
                                  (and (map? raw-args) (string? k)) (or (get raw-args k) (get raw-args (keyword k)))
                                  :else nil))
                              keys)
                        (some (fn [k]
                                (when (and raw-args
                                           (not= raw-args js/undefined)
                                           (or (object? raw-args) (fn? raw-args)))
                                  (aget raw-args (if (keyword? k) (name k) (str k)))))
                              keys)))]
    (case tool-name
      "bash"
      (when (map? args)
        (let [cmd (arg-value :command :cmd)
              timeout (arg-value :timeout :timeoutSeconds :timeoutMs)]
          (when (and (string? cmd) (not (str/blank? cmd)))
            (let [[clipped-cmd clipped?] (clip-text cmd 20000)]
              (str
               (fenced "bash" (if clipped? (str clipped-cmd "…") clipped-cmd))
               (when clipped? "\n\n_(truncated)_")
               (when (some? timeout)
                 (str "\n\n- timeout: " timeout)))))))

      "read"
      (let [path (arg-value :path "path")
            offset (arg-value :offset "offset")
            limit (arg-value :limit "limit")]
        (when (and (string? path) (not (str/blank? path)))
          (fenced "yaml"
                  (str "path: " path
                       "\noffset: " (if (some? offset) offset "(default)")
                       "\nlimit: " (if (some? limit) limit "(default)")))))

      nil)))

(defn- copy-js-object
  [value]
  (when (and value
             (not= value js/undefined)
             (not (array? value))
             (= "object" (goog/typeOf value)))
    (let [copy (js-obj)
          own-keys (distinct (concat (array-seq (.keys js/Object value))
                                     (array-seq (.getOwnPropertyNames js/Object value))))]
      (doseq [k own-keys]
        (aset copy k (aget value k)))
      copy)))

(defn tool-call-input-preview
  [tool-name raw-args]
  (or (tool-args->markdown-preview tool-name raw-args)
      (preview-text-nonblank raw-args 20000)
      (json-preview-nonblank raw-args 20000)
      (let [copied (copy-js-object raw-args)]
        (or (tool-args->markdown-preview tool-name copied)
            (preview-text-nonblank copied 20000)
            (json-preview-nonblank copied 20000)))))

(defn tool-call-preview-from-part
  [part]
  (let [part-type (some-> (aget part "type") str str/lower-case)]
    (when (contains? #{"toolcall" "tool_call"} part-type)
      (let [tool-call-id (some-> (aget part "id") str nonblank)
            tool-name (some-> (aget part "name") str nonblank)
            arguments (aget part "arguments")
            input-preview (tool-call-input-preview tool-name arguments)]
        (when (and tool-call-id input-preview)
          {:tool_call_id tool-call-id
           :tool_name tool-name
           :input_preview input-preview})))))

(defn ^:export assistant-tool-call-previews
  [assistant-message]
  (let [content (when assistant-message
                  (aget assistant-message "content"))]
    (if (array? content)
      (->> (array-seq content)
           (keep tool-call-preview-from-part)
           vec)
      [])))
