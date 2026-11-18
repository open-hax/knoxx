(ns big-pi.optimizely.extern.server
  (:require ["./runtime.mjs" :as runtime]
            [big-pi.optimizely.bridge.cms :as bridge]
            [big-pi.optimizely.domain.cms :as cms]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [promesa.core :as p]))

(defn- decode-json [value]
  ;; Only identifier-like keys become keywords. Arbitrary JSON names (including
  ;; spaces and empty strings) must stay strings so EDN can round-trip them.
  (walk/postwalk
   (fn [item]
     (if (map? item)
       (into {} (map (fn [[key value]]
                       [(if (and (string? key) (re-matches #"[A-Za-z_][A-Za-z0-9_-]*" key))
                          (keyword key) key) value])) item)
       item))
   (js->clj value)))

(defn- encode-json [value]
  ;; JSON field names are arbitrary strings. The default keyword encoder drops
  ;; namespaces (a/b -> b), so preserve the complete spelling at this boundary.
  (clj->js value :keyword-fn #(subs (str %) 1)))

(defn- decode-state [text]
  (let [state (reader/read-string text)]
    (when-not (and (map? state) (integer? (:generation state)) (map? (:content state))
                   (map? (:contenttypes state)) (map? (:locales state)) (vector? (:history state)))
      (throw (js/Error. "Invalid state snapshot")))
    state))

(defn- dispatch [store raw]
  (when (.-failed store) (throw (js/Error. "State unavailable")))
  (let [request {:method (keyword (str/lower-case (.-method raw)))
                 :path (.-path raw)
                 :query (js->clj (or (.-query raw) #js {}))
                 :headers (js->clj (or (.-headers raw) #js {}))
                 :body (decode-json (.-body raw))
                 :actor {:id "local" :capabilities #{:read :draft}}}
        before (.-value store)
        result (if (= (:path request) "/v1/capabilities")
                 {:state before :response {:status 200 :headers {} :body cms/capabilities}}
                 (cms/handle before request))
        after (:state result)]
    (when (not= before after)
      (try
        (.save store (pr-str after))
        (set! (.-value store) after)
        (catch :default error
          (set! (.-failed store) true)
          (throw error))))
    (encode-json (update (:response result) :headers assoc "X-Big-Pi-Source-Id" (.-identity store)))))

(defn- response-map [raw]
  {:status (.-status raw) :headers (into {} (map (fn [[k v]] [(str/lower-case k) v]) (js->clj (.-headers raw))))
   :body (decode-json (.-body raw))})

(defn- error-data [error]
  ;; NBB may wrap ex-info while evaluating asynchronous CLJS. Keep the nearest
  ;; domain error (especially an uncertain write) instead of its transport cause.
  (loop [current error remaining 8]
    (let [data (ex-data current)]
      (if (or (:code data) (nil? current) (zero? remaining))
        data
        (recur (ex-cause current) (dec remaining))))))

(defn- bridge-dispatch [store raw]
  (let [local (fn [request]
                (p/resolved (response-map (dispatch store (encode-json (update request :method name))))))
        remote (fn [request]
                 (p/then (runtime/knoxxFetch (encode-json (update request :method name))) response-map))
        input (decode-json (.-body raw))
        operation (if (= (.-path raw) "/big-pi/import") bridge/import! bridge/export!)]
    (-> (p/then (p/resolved nil) (fn [_] (operation {:optimizely local :knoxx remote} input)))
        (p/then (fn [result] (encode-json {:status 200 :headers {} :body result})))
        (p/catch (fn [error]
                   (let [data (error-data error)
                         code (:code data)
                         status (or (:status data) 502)]
                     (encode-json {:status status :headers {}
                               :body {:code (name (or code :upstream-failure))
                                      :error (if (= code :outcome-uncertain)
                                               "Draft outcome uncertain; inspect before retrying"
                                               "Bridge operation failed")}})))))))

(defn start [args]
  (if (some #{"--stdio"} args)
    (runtime/startStdio)
    (runtime/startHttp cms/initial-state decode-state dispatch bridge-dispatch)))
