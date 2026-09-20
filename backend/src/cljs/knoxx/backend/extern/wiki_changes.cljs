(ns knoxx.backend.extern.wiki-changes
  "Authenticated SSE invalidations with fresh authorization before every write."
  (:require [knoxx.backend.extern.fastify :as fastify]
            [knoxx.backend.infra.auth.authz :as authz]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.identity :as identity]
            [knoxx.backend.infra.wiki-commands :as commands]
            [knoxx.backend.shape.resource-identity :as resource]))

(defn- same-scope? [expected change]
  (= (select-keys expected [:org-id :project]) (select-keys change [:org-id :project])))

(defn- frame [document]
  (str "event: publication-changed\ndata: "
       (js/JSON.stringify (clj->js {:document (some-> document resource/encode-keyword)})) "\n\n"))

(defn- close-stream! [{:keys [closed? subscriptions* interval* pending* raw]}]
  (when (compare-and-set! closed? false true)
    (doseq [unsubscribe @subscriptions*] (unsubscribe))
    (when-let [interval @interval*] (js/clearInterval interval))
    (reset! pending* [])
    (.end raw)))

(defn- ^:async refresh-context! [{:keys [server-runtime config ctx scope]}]
  (let [current (await (identity/current-context! (authz/policy-db server-runtime) ctx))]
    (commands/ensure-command! current "wiki_read" "publication/read")
    (when-not (= scope (commands/scope config current))
      (throw (ex-info "Stream identity scope changed" {:status 403})))))

(defn- ^:async drain! [{:keys [busy? closed? pending* raw] :as state}]
  (when (compare-and-set! busy? false true)
    (try
      (loop []
        (when (and (not @closed?) (seq @pending*))
          (let [batch @pending*]
            (reset! pending* [])
            (await (refresh-context! state))
            (doseq [document batch :while (not @closed?)]
              (when-not (.write raw (frame document)) (close-stream! state)))
            (recur))))
      (catch :default error
        (when-not (contains? #{401 403} (fastify/error-status error))
          (fastify/log-unclassified-failure! "wiki-changes" error))
        (close-stream! state))
      (finally (reset! busy? false)))))

(defn- enqueue! [{:keys [closed? scope pending*] :as state} change]
  (when (and (not @closed?) (same-scope? scope change))
    (swap! pending* #(if (< (count %) 64) (conj % (:document change)) [nil]))
    (drain! state)))

(defn- start-stream! [server-runtime config ctx reply]
  (let [raw (.-raw reply)
        scope (commands/scope config ctx)
        state {:raw raw :scope scope :server-runtime server-runtime :config config :ctx ctx
               :closed? (atom false) :busy? (atom false) :pending* (atom [])
               :subscriptions* (atom []) :interval* (atom nil)}
        close! #(close-stream! state)
        enqueue! #(enqueue! state %)]
    (.hijack reply)
    (.writeHead raw 200 #js {"Content-Type" "text/event-stream"
                            "Cache-Control" "no-cache, no-transform"
                            "Connection" "keep-alive" "X-Accel-Buffering" "no"})
    (.flushHeaders raw)
    (.once raw "close" close!)
    (.once raw "error" close!)
    (swap! (:subscriptions* state) conj (commands/subscribe! enqueue!)
           (clio/subscribe! #(enqueue! (assoc scope :document nil))))
    (reset! (:interval* state) (js/setInterval #(enqueue! (assoc scope :document nil)) 15000))
    (enqueue! (assoc scope :document nil))))

(defn register!
  "Open a credential-bound stream; changing credentials or permissions closes it."
  [app server-runtime config]
  (fastify/route!
   app {:method "GET" :url "/api/publications/changes"
        :handler (^:async fn [request reply]
                   (await (authz/with-request-context!
                           server-runtime request reply
                           (fn [ctx]
                             (commands/ensure-command! ctx "wiki_read" "publication/read")
                             (start-stream! server-runtime config ctx reply)))))}))
