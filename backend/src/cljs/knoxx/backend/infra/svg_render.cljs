(ns knoxx.backend.infra.svg-render
  "SVG rendering via headless Chromium/Puppeteer.

   Keeps one browser process warm so repeated Discord image uploads avoid the
   cold-start cost. Uses puppeteer-core so deployments can provide Chromium via
   PUPPETEER_EXECUTABLE_PATH/KNOXX_CHROMIUM_PATH or a common system path."
  (:require [clojure.string :as str]
            [knoxx.backend.law.svg :as svg-law]
            [open-hax.uxx.markup :as markup]
            [open-hax.uxx.render.html :as html]
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

(defn ^:async launch-browser!
  []
  (try
    (remember-browser! (await (.launch (puppeteer-module) (launch-options))))
    (catch :default err
      (forget-launch! err))))

(defn ^:async get-browser
  []
  (cond
    @browser-atom
    @browser-atom

    @browser-promise-atom
    (await @browser-promise-atom)

    :else
    (let [launch-promise (launch-browser!)]
      (reset! browser-promise-atom launch-promise)
      (await launch-promise))))

(defn- trusted-svg-markup
  [svg-string]
  (markup/trusted-html (svg-law/validate-svg! svg-string)))

(defn svg-document-node
  "Build the browser document around law-validated SVG using the shared AST."
  [svg-string]
  [:html {}
   [:head {}
    [:meta {:charset "utf-8"}]]
   [:body {:style "margin:0;padding:0;background:transparent"}
    (markup/raw-html (trusted-svg-markup svg-string))]])

(defn svg-document
  "Render a complete, deterministic browser document around validated SVG."
  [svg-string]
  (str "<!doctype html>\n" (html/render (svg-document-node svg-string))))

(defn ^:async prepare-page!
  "Configure a fresh Puppeteer page as a code-free, network-denied SVG canvas."
  [page width height]
  (await (.setViewport page #js {:width width :height height}))
  (await (.setJavaScriptEnabled page false))
  (await (.setRequestInterception page true))
  (.on page "request"
       (fn [request]
         (.abort request "blockedbyclient")))
  page)

(defn- ^:async render-svg!
  [page svg-string width height]
  (let [_ (await (prepare-page! page width height))
        _ (await (.setContent page (svg-document svg-string)
                              #js {:waitUntil "networkidle0"}))
        element (await (.$ page "svg"))]
    (when-not element
      (throw (js/Error. "Cannot render SVG: no <svg> element found.")))
    (let [png (await (.screenshot element #js {:type "png"
                                               :omitBackground true}))]
      (.from js/Buffer png))))

(defn- render-page!
  [page svg-string width height]
  (let [render-promise (render-svg! page svg-string width height)]
    (.finally render-promise (fn [] (.close page)))))

(defn ^:async svg->png
  "Renders a validated SVG string to a PNG Node Buffer via headless Chromium.
   Returns a js/Promise<Buffer>."
  [svg-string {:keys [width height] :or {width 600 height 300}}]
  (let [browser (await (get-browser))
        page (await (.newPage browser))]
    (render-page! page svg-string width height)))

(defn ^:async shutdown!
  "Closes the warm Chromium browser, if present. Returns a js/Promise."
  []
  (if-let [browser @browser-atom]
    (do
      (reset! browser-atom nil)
      (reset! browser-promise-atom nil)
      (try
        (await (.close browser))
        true
        (catch :default err
          (.warn js/console "[svg-render] failed to close Chromium" err)
          false)))
    true))
