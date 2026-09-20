(ns knoxx.backend.extern.mailbox-store
  "Mailbox clocks, operation identities and process-local writer sequencing."
  (:require [clio.extern.js.runtime :as runtime]
            [knoxx.backend.law.mailbox-store :as law]))

(defn new-id! "Generate a new claim or command identity at the host boundary." [] (runtime/random-uuid))
(defn- now! [] (.toISOString (js/Date.)))
(defn instant!
  "Normalize a timestamp once; accepted facts use fixed-width UTC instants."
  [value]
  (when-not (or (string? value) (number? value))
    (law/refuse! 400 "mailbox_clock_invalid" "Mailbox timestamps must be strings or numbers"))
  (let [date (js/Date. value)]
    (when-not (js/Number.isFinite (.getTime date))
      (law/refuse! 400 "mailbox_clock_invalid" "Mailbox timestamp is invalid"))
    (law/checked! :mailbox/clock law/Instant (.toISOString date))))
(defn clock! "Read the configured clock with validation before persistence." [options]
  (instant! ((or (:clock! options) now!))))
(defn- after [instant milliseconds]
  (instant! (+ (.getTime (js/Date. instant)) milliseconds)))

(defn operation
  "Stamp a finite operation after it obtains its turn at the provider boundary."
  [options kind scope payload]
  (let [at (clock! options)
        operation {:kind kind :scope scope :payload payload :at at
                   :id (or (:operation-id payload) (get-in payload [:options :operation-id]) (new-id!))}]
    (law/checked!
     :mailbox/operation law/Operation
     (case kind
       :claim (assoc operation :lease-until (after at (or (:lease-ms payload) 30000))
                               :next-at (after at (* 1000 (or (:delay-seconds payload) 0))))
       :register (assoc operation :route-expires-at (after at (* 1000 (or (:ttl-seconds payload) 1800))))
       :create (if-let [expiry (:mailbox/expires-at payload)]
                 (assoc-in operation [:payload :mailbox/expires-at] (instant! expiry)) operation)
       operation))))

(defonce ^:private tails* (atom {}))
(defn- ^:async run-after! [previous operation]
  (when previous
    (try (await previous)
         ;; knoxx-lint/allow-silent-catch — a failed write must not poison the next repair attempt.
         (catch :default _ nil)))
  (await (operation)))
(defn- ^:async forget-completed! [directory task]
  (try
    (await task)
    ;; knoxx-lint/allow-silent-catch — cleanup must not publish a second rejected promise.
    (catch :default _ nil)
    (finally
      (when (identical? task (get @tails* directory)) (swap! tails* dissoc directory)))))
(defn serialized!
  "Serialize this process's writers; Clio separately fences competing processes."
  [directory operation]
  (let [task (run-after! (get @tails* directory) operation)]
    (swap! tails* assoc directory task)
    (forget-completed! directory task)
    task))
