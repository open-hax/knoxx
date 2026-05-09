(ns knoxx.backend.svg-render-test
  (:require [cljs.test :refer [async deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.svg-render :as svg-render]
            ["node:fs" :as fs]))

(def chromium-candidate-paths
  ["/usr/bin/chromium"
   "/usr/bin/chromium-browser"
   "/usr/bin/google-chrome-stable"
   "/usr/bin/google-chrome"
   "/snap/bin/chromium"])

(defn- nonblank-env
  [k]
  (let [v (aget (.-env js/process) k)]
    (when (and (some? v) (not (str/blank? (str v))))
      (str v))))

(defn- executable-available?
  []
  (boolean
   (or (nonblank-env "PUPPETEER_EXECUTABLE_PATH")
       (nonblank-env "KNOXX_CHROMIUM_PATH")
       (some #(try
                (when (.existsSync fs %) %)
                (catch :default _ nil))
             chromium-candidate-paths))))

(def browser-feature-svg
  "<svg width='240' height='120' xmlns='http://www.w3.org/2000/svg'>
     <defs>
       <filter id='glow'><feGaussianBlur stdDeviation='3' result='b'/><feMerge><feMergeNode in='b'/><feMergeNode in='SourceGraphic'/></feMerge></filter>
       <linearGradient id='g'><stop offset='0%' stop-color='#ff00aa'/><stop offset='100%' stop-color='#00ddff'/></linearGradient>
     </defs>
     <rect width='240' height='120' fill='#101020'/>
     <text x='20' y='70' font-family='Georgia, Arial' font-size='42' fill='url(#g)' filter='url(#glow)'>Knoxx</text>
   </svg>")

(deftest svg->png-renders-browser-svg-features
  (testing "Chromium produces a PNG buffer for filter, text font fallback, and text gradient SVGs"
    (async done
      (if-not (executable-available?)
        (do
          (is true "Skipping Chromium SVG render smoke test; no executable configured/found.")
          (done))
        (-> (svg-render/svg->png browser-feature-svg {:width 240 :height 120})
            (.then (fn [buf]
                     (is (= "89504e470d0a1a0a" (.toString (.subarray buf 0 8) "hex")))
                     (is (> (.-length buf) 1000))))
            (.then svg-render/shutdown!)
            (.then (fn [_] (done)))
            (.catch (fn [err]
                      (is false (str err))
                      (-> (svg-render/shutdown!)
                          (.finally done)))))))))
