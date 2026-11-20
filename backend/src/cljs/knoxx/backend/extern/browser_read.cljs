(ns knoxx.backend.extern.browser-read
  "Run the fixed browser reader as argv, without a shell or inherited service secrets."
  (:require [knoxx.backend.law.browser-source :as source]
            [malli.core :as m]
            ["node:child_process" :as child]))

(defn- parse-output
  [stdout]
  (try (js->clj (.parse js/JSON stdout) :keywordize-keys true)
       (catch :default _error nil)))

(defn decode-output
  "Decode the fixed reader protocol; native failures retain typed diagnostics."
  [url process stdout]
  (let [result (parse-output stdout)]
    (if (and (not (:failed? process)) (m/validate source/ReaderResponse result)
             (= url (:source result)))
      result
      (let [evidence (source/failure-evidence url process
                                             (if (:failed? process)
                                               (select-keys (:reader_error result) [:code :httpStatus :errorClass])
                                               {:code "invalid_reader_response"}))]
        (throw (ex-info (source/failure-message evidence) evidence))))))

(defn exec-reader!
  "Execute argv without a shell or service secrets; decode process facts once."
  [reader-file browser-url url complete]
  (.execFile child "python3" (clj->js [reader-file url])
             #js {:timeout 80000 :maxBuffer 2097152
                  :env #js {:PATH "/usr/local/bin:/usr/bin:/bin" :SHUVCRAWL_URL browser-url}}
             (fn [error stdout _stderr]
               (complete {:failed? (some? error) :exit-code (some-> error .-code)
                          :signal (some-> error .-signal) :killed (boolean (some-> error .-killed))}
                         stdout))))

(defn read!
  "The trusted reader only receives a URL and public browser-service address."
  [reader-file browser-url url]
  (js/Promise.
   (fn [resolve reject]
     (exec-reader! reader-file browser-url url
                   (fn [process stdout]
                     (try (resolve (decode-output url process stdout))
                          (catch :default error (reject error))))))))
