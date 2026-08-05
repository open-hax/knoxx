(ns knoxx.backend.infra.svg-render
  "SVG rendering via headless Chromium/Puppeteer.

   Keeps one browser process warm so repeated Discord image uploads avoid the
   cold-start cost. Uses puppeteer-core so deployments can provide Chromium via
   PUPPETEER_EXECUTABLE_PATH/KNOXX_CHROMIUM_PATH or a common system path."
  (:require [clojure.string :as str]
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

(def ^:private svg-root-pattern #"(?is)^\s*<svg\b")
(def ^:private prohibited-declaration-pattern #"(?is)<\s*!(?:doctype|entity)\b")
(def ^:private processing-instruction-pattern #"(?is)<\?")
(def ^:private prohibited-element-pattern
  #"(?is)<\s*(?:script|foreignobject|iframe|object|embed|link|img|audio|video|source|base|meta|html|body|form|input|button|textarea|select|option)\b")
(def ^:private event-attribute-pattern #"(?is)\son[a-z0-9:_-]*\s*=")
(def ^:private base-attribute-pattern #"(?is)\s(?:xml:base|base)\s*=")
(def ^:private resource-attribute-pattern
  #"(?is)\s(?:href|xlink:href|src)\s*=\s*(?:\"([^\"]*)\"|'([^']*)'|([^\s>]+))")
(def ^:private css-url-pattern
  #"(?is)url\(\s*(?:\"([^\"]*)\"|'([^']*)'|([^)]*))\s*\)")
(def ^:private css-import-pattern #"(?is)@import\b")

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

(defn- svg-preview
  [svg-string]
  (let [value (str (or svg-string ""))]
    (.slice value 0 (min 160 (count value)))))

(defn- reject-svg!
  [message type svg-string]
  (throw (ex-info message
                  {:type type
                   :preview (svg-preview svg-string)})))

(defn- captured-reference
  [match]
  (str/trim (str (or (nth match 1 nil)
                         (nth match 2 nil)
                         (nth match 3 nil)
                         ""))))

(defn- local-fragment-reference?
  [value]
  (str/starts-with? value "#"))

(defn validate-svg!
  "Validate SVG before it crosses the explicit raw-markup capability boundary.

   Local fragment references remain valid for gradients, filters, masks, clips,
   symbols, and other in-document resources. Any reference that could resolve
   outside the document is rejected; Chromium request interception is still
   enabled as a defense-in-depth network boundary."
  [svg-string]
  (when-not (string? svg-string)
    (reject-svg! "SVG content must be a string" :svg/invalid-content svg-string))
  (let [candidate (str/trim svg-string)]
    (when (str/blank? candidate)
      (reject-svg! "SVG content cannot be blank" :svg/blank-content svg-string))
    (when-not (re-find svg-root-pattern candidate)
      (reject-svg! "SVG content must begin with an <svg> root"
                   :svg/missing-root svg-string))
    (when (re-find prohibited-declaration-pattern candidate)
      (reject-svg! "SVG declarations and entities are not allowed"
                   :svg/prohibited-declaration svg-string))
    (when (re-find processing-instruction-pattern candidate)
      (reject-svg! "SVG processing instructions are not allowed"
                   :svg/processing-instruction svg-string))
    (when (re-find prohibited-element-pattern candidate)
      (reject-svg! "SVG contains an active or HTML-only element"
                   :svg/prohibited-element svg-string))
    (when (re-find event-attribute-pattern candidate)
      (reject-svg! "SVG event attributes are not allowed"
                   :svg/event-attribute svg-string))
    (when (re-find base-attribute-pattern candidate)
      (reject-svg! "SVG base URL attributes are not allowed"
                   :svg/base-url svg-string))
    (when (re-find css-import-pattern candidate)
      (reject-svg! "SVG CSS imports are not allowed"
                   :svg/css-import svg-string))
    (doseq [match (re-seq resource-attribute-pattern candidate)]
      (let [reference (captured-reference match)]
        (when-not (local-fragment-reference? reference)
          (reject-svg! "SVG resource attributes must use local fragments"
                       :svg/external-resource svg-string))))
    (doseq [match (re-seq css-url-pattern candidate)]
      (let [reference (captured-reference match)]
        (when-not (local-fragment-reference? reference)
          (reject-svg! "SVG CSS URLs must use local fragments"
                       :svg/external-css-resource svg-string))))
    candidate))

(defn- trusted-svg-markup
  [svg-string]
  (markup/trusted-html (validate-svg! svg-string)))

(defn svg-document-node
  "Build the browser document around validated SVG using the shared AST."
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
