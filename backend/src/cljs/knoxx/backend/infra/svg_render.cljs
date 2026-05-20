(ns knoxx.backend.infra.svg-render
  "SVG rendering via headless Chromium/Puppeteer.

   Keeps one browser process warm so repeated Discord image uploads avoid the
   cold-start cost. Uses puppeteer-core so deployments can provide Chromium via
   PUPPETEER_EXECUTABLE_PATH/KNOXX_CHROMIUM_PATH or a common system path."
  (:require [clojure.string :as str]
            [shadow.cljs.modern :refer [js-await]]
            ["node:fs" :as fs]
            ["puppeteer-core" :as puppeteer-core]))

(defonce ^:private browser-atom (atom nil))
(defonce ^:private browser-promise-atom (atom nil))

(def ^:private chromium-candidate-paths
  ["/usr/bin/chromium"
   "/usr/bin/chromium-browser"
   "/usr/bin/google-chrome-stable"
   "/usr/bin/google-chrome"
   "/snap/bin/chromium"])

(defn- env-value
  [k]
  (let [v (aget (.-env js/process) k)]
    (when (and (some? v) (not (str/blank? (str v))))
      (str v))))

(defn- existing-file?
  [path]
  (try
    (.existsSync fs path)
    (catch :default _
      false)))

(defn- executable-path
  []
  (or (env-value "PUPPETEER_EXECUTABLE_PATH")
      (env-value "KNOXX_CHROMIUM_PATH")
      (some #(when (existing-file? %) %) chromium-candidate-paths)))

(defn- puppeteer-module
  []
  (or (.-default puppeteer-core) puppeteer-core))

(defn- launch-options
  []
  (let [opts #js {:args #js ["--no-sandbox"
                             "--disable-setuid-sandbox"]}]
    (when-let [path (executable-path)]
      (aset opts "executablePath" path))
    opts))

(defn- remember-browser!
  [browser]
  (reset! browser-atom browser)
  (reset! browser-promise-atom nil)
  browser)

(defn- forget-launch!
  [err]
  (reset! browser-promise-atom nil)
  (throw err))

(defn- get-browser
  []
  (cond
    @browser-atom
    (js/Promise.resolve @browser-atom)

    @browser-promise-atom
    @browser-promise-atom

    :else
    (let [launch-promise (-> (.launch (puppeteer-module) (launch-options))
                             (.then remember-browser!)
                             (.catch forget-launch!))]
      (reset! browser-promise-atom launch-promise)
      launch-promise)))

(defn- svg-document
  [svg-string]
  (str "<!doctype html>"
       "<html><head><meta charset='utf-8'></head>"
       "<body style='margin:0;padding:0;background:transparent'>"
       svg-string
       "</body></html>"))

(defn- render-page!
  [page svg-string width height]
  (let [render-promise
        (js-await [_ (.setViewport page #js {:width width :height height})]
          (js-await [_ (.setJavaScriptEnabled page false)]
            (js-await [_ (.setContent page (svg-document svg-string) #js {:waitUntil "networkidle0"})]
              (js-await [element (.$ page "svg")]
                (when-not element
                  (throw (js/Error. "Cannot render SVG: no <svg> element found.")))
                (js-await [png (.screenshot element #js {:type "png"
                                                         :omitBackground true})]
                  (.from js/Buffer png))))))]
    (.finally render-promise (fn [] (.close page)))))

(defn svg->png
  "Renders an SVG string to a PNG Node Buffer via headless Chromium.
   Returns a js/Promise<Buffer>."
  [svg-string {:keys [width height] :or {width 600 height 300}}]
  (js-await [browser (get-browser)]
    (js-await [page (.newPage browser)]
      (render-page! page svg-string width height))))

(defn shutdown!
  "Closes the warm Chromium browser, if present. Returns a js/Promise."
  []
  (if-let [browser @browser-atom]
    (do
      (reset! browser-atom nil)
      (reset! browser-promise-atom nil)
      (.catch (.close browser)
              (fn [err]
                (.warn js/console "[svg-render] failed to close Chromium" err)
                false)))
    (js/Promise.resolve true)))
