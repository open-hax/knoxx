(ns knoxx.backend.infra.routes.document-admission-queue
  "Serialize admission against its caller-owned tail and recover failed work.")

(defn- ^:async run-after-document-admission!
  [previous task-fn]
  (when previous
    (try
      (await previous)
      ;; knoxx-lint/allow-silent-catch — an earlier admission must not poison the queue.
      (catch :default _
        nil)))
  (await (task-fn)))

(defn- ^:async recover-document-admission-tail!
  [task]
  (try
    (await task)
    ;; knoxx-lint/allow-silent-catch — only the stored recovery tail consumes this rejection.
    (catch :default _
      nil)))

(defn enqueue-document-admission!
  "Serialize one complete admission pass behind the process-wide tail."
  [tail task-fn]
  ;; No await occurs between reading and replacing the tail, so the single JS
  ;; event loop gives every caller an exact predecessor. The recovery promise
  ;; prevents one failed admission from poisoning every later deployment.
  (let [task (run-after-document-admission!
              @tail task-fn)]
    (reset! tail
            (recover-document-admission-tail! task))
    task))

(defn ^:async await-document-admission-barrier!
  "Resolve only after every document admission already queued in this process.

   The no-op occupies the same serialized tail as HTTP admission and generated
   draft re-entry. Callers that drain asynchronous owners must await this
   barrier again after those owners release, because an owner may enqueue a
   recursive admission while the first barrier is waiting."
  [tail]
  (await
   (enqueue-document-admission! tail
    (fn [] (js/Promise.resolve {:settled true})))))
