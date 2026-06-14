(ns knoxx.frontend.pages.documents.logic
  "Pure logic for the documents/lakes page. CLJS port of the ingestion
   rate/ETA math, progress-sample windowing, selection toggles and
   restart-decision helpers in src/pages/DocumentsPage.tsx."
  (:require [clojure.string :as str]))

(defn format-eta [seconds]
  (if (or (not (number? seconds)) (zero? seconds) (not (js/Number.isFinite seconds)))
    "Estimating..."
    (let [mins (js/Math.floor (/ seconds 60))
          secs (js/Math.floor (mod seconds 60))]
      (if (pos? mins)
        (str mins "m " secs "s")
        (str secs "s")))))

(defn chunks-per-sec
  "Ingestion rate. With ≥2 samples, rate across the sample window
   (clamped at 0); otherwise processed/elapsed when both available."
  [samples progress elapsed-seconds]
  (if (< (count samples) 2)
    (let [processed (:processedChunks progress)]
      (if (and (number? processed) (number? elapsed-seconds) (pos? elapsed-seconds))
        (/ processed elapsed-seconds)
        0))
    (let [first-sample (first samples)
          last-sample (peek (vec samples))
          dt (max 1 (/ (- (:ts last-sample) (:ts first-sample)) 1000))
          d-chunks (max 0 (- (:processed last-sample) (:processed first-sample)))]
      (/ d-chunks dt))))

(defn remaining-chunks [progress]
  (if progress
    (max 0 (- (or (:totalChunks progress) 0)
              (or (:processedChunks progress) 0)))
    0))

(defn eta-seconds [remaining rate]
  (if (pos? rate) (/ remaining rate) 0))

(defn push-sample
  "Appends a progress sample, keeping only the last 60s (and ≤120 samples)."
  [samples now processed]
  (->> (conj (vec samples) {:ts now :processed processed})
       (filterv #(<= (- now (:ts %)) 60000))
       (take-last 120)
       vec))

(defn toggle-doc [selected path]
  (if (contains? selected path)
    (disj selected path)
    (conj selected path)))

(defn toggle-all [selected documents]
  (if (= (count selected) (count documents))
    #{}
    (into #{} (map :relativePath) documents)))

(defn should-force-fresh? [{:keys [stale canResumeForum]}]
  (boolean (and stale canResumeForum)))

(defn no-active-run? [{:keys [active canResumeForum]}]
  (boolean (and (not active) (not canResumeForum))))

(defn restart-message [force-fresh?]
  (if force-fresh?
    "Ingestion was stalled; started fresh forum ingestion from scratch."
    "Ingestion restart requested. Resuming from saved progress..."))

(defn no-active-restart-error? [message]
  (str/includes? (or message "") "No active ingestion to restart"))
