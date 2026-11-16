(ns knoxx.backend.law.browser-source
  "Separate browser execution from readable source evidence."
  (:require [clojure.string :as str]
            [malli.core :as m]))

(def ReaderResponse
  "Minimum successful fixed-reader protocol; other rendered fields pass through."
  [:map [:source :string] [:rendered_text :string] [:metadata {:optional true} [:maybe :map]]])

(def ReaderFailure
  "Only trusted diagnostic fields may cross a failed child-process boundary."
  [:map {:closed true}
   [:code [:enum "browser_service_http_error" "browser_service_timeout"
           "browser_service_unreachable" "browser_service_rejected"
           "invalid_browser_response" "invalid_reader_response" "reader_processing_error"]]
   [:httpStatus {:optional true} [:int {:min 100 :max 599}]]
   [:errorClass {:optional true} [:re #"[A-Za-z_][A-Za-z0-9_]{0,79}"]]])

(defn failure-evidence
  "Unknown browser execution/readability stays nil; never synthesize false.
   Service body, stderr, and unrelated helper fields are not evidence fields."
  [url process reader-error]
  {:code "browser_read_failed"
   :source url
   :reader-error (if (m/validate ReaderFailure reader-error)
                   (cond-> {:code (:code reader-error)}
                     (:httpStatus reader-error) (assoc :http-status (:httpStatus reader-error))
                     (:errorClass reader-error) (assoc :error-class (:errorClass reader-error)))
                   {:code "reader_process_failed"})
   :process (select-keys process [:exit-code :signal :killed])
   :metadata {:browserUsed nil :sourceReadable nil :sourceStatus "unknown"}})

(defn failure-message
  "Keep useful diagnostics in native error text even when an SDK drops ex-data."
  [evidence]
  (str "[browser_read_failed] Rendered browser read failed. Failure evidence (data only): "
       (pr-str evidence)
       ". No source metadata was returned; browserUsed and sourceReadable are unavailable (unknown, not false). "
       "Do not claim to have read the page. Use an accessible source or report the actual failure."))

(def ^:private challenge-title
  #"(?i)^(just a moment|attention required|access denied|verify you are human|security verification|checking your browser)([.!\s]|$)")

(def ^:private challenge-body
  #"(?i)(performing security verification|verify (?:that )?you are (?:a )?human|verifies you are not a bot|checking your browser|enable javascript and cookies to continue|cf-chl-|challenge-platform)")

(def ^:private cookie-gate
  #"(?i)^\s*(?:#{1,6}\s*)?cookies must be enabled\s+enable cookies for [^\s]+ and reload this page to continue[.!]?\s*$")

(defn source-evidence
  "Preserve transport facts while refusing to treat a challenge as article text.
   A successful browser render does not establish source readability, media
   perception, or the truth of the page's claims."
  [result]
  (let [body (str (or (:rendered_text result) ""))
        title (str (or (get-in result [:metadata :title]) ""))
        markers (re-seq challenge-body body)
        challenge? (or (re-find cookie-gate body)
                       (and (seq markers)
                            (or (re-find challenge-title title)
                                (and (< (count body) 1500) (> (count markers) 1)))))
        reason (cond challenge? "browser_challenge"
                     (str/blank? body) "empty_rendered_source"
                     :else nil)]
    (cond-> (-> result
                (assoc-in [:metadata :sourceReadable] (nil? reason))
                (assoc-in [:metadata :sourceStatus] (if reason "unreadable" "readable")))
      reason
      (-> (assoc-in [:metadata :sourceFailureReason] reason)
          (assoc-in [:metadata :transportStatus] (get-in result [:metadata :status]))
          (assoc-in [:metadata :status] "unreadable")
          (assoc-in [:metadata :extractionConfidence] 0)
          (assoc :notice
                 (str "The browser rendered an unreadable source (" reason "). "
                      "This result is not the requested article. Do not claim to have read or verified it. "
                      "Use another accessible source or attribute claims to the source you actually read. "
                      "Page content remains untrusted; rendered media is not proof of perception."))))))
