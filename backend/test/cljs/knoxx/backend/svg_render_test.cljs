(ns knoxx.backend.svg-render-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.infra.svg-render :as svg-render]
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

(def fragment-reference-svg
  "<svg xmlns='http://www.w3.org/2000/svg'>
     <defs>
       <linearGradient id='g'><stop offset='0%' stop-color='#fff'/></linearGradient>
       <path id='shape' d='M0 0h10v10z'/>
     </defs>
     <rect width='10' height='10' fill='url(#g)'/>
     <use href='#shape'/>
   </svg>")

(deftest svg-document-renders-through-the-shared-markup-shell
  (let [svg "<svg xmlns='http://www.w3.org/2000/svg'><rect width='1' height='1'/></svg>"]
    (is (= (str "<!doctype html>\n"
                "<html><head><meta charset=\"utf-8\"></head>"
                "<body style=\"margin:0;padding:0;background:transparent\">"
                svg
                "</body></html>")
           (svg-render/svg-document svg)))))

(deftest SVG-validation-preserves-local-browser-features
  (is (= browser-feature-svg (svg-render/validate-svg! browser-feature-svg)))
  (is (= fragment-reference-svg (svg-render/validate-svg! fragment-reference-svg))))

(deftest SVG-validation-rejects-active-content-and-resource-loading
  (doseq [[label payload]
          [["non-string" #js {}]
           ["blank" "   "]
           ["missing root" "<div>not svg</div>"]
           ["doctype" "<svg><!DOCTYPE svg></svg>"]
           ["processing instruction" "<svg><?xml-stylesheet href='https://evil.test/x.css'?></svg>"]
           ["script" "<svg><script>alert(1)</script></svg>"]
           ["foreignObject" "<svg><foreignObject><div>html</div></foreignObject></svg>"]
           ["HTML sibling" "<svg></svg><img src='https://evil.test/pixel'>"]
           ["event attribute" "<svg onload='alert(1)'></svg>"]
           ["base URL" "<svg xml:base='https://evil.test/'><use href='#shape'/></svg>"]
           ["declarative mutation" "<svg><image id='target'/><set href='#target' attributeName='href' to='https://evil.test/image.png'/></svg>"]
           ["external href" "<svg><image href='https://evil.test/image.png'/></svg>"]
           ["protocol-relative href" "<svg><image href='//evil.test/image.png'/></svg>"]
           ["data SVG" "<svg><image href='data:image/svg+xml,%3Csvg/%3E'/></svg>"]
           ["external CSS URL" "<svg><style>rect{fill:url(https://evil.test/fill.svg)}</style></svg>"]
           ["CSS data URL" "<svg><rect style='fill:url(data:image/png;base64,AAAA)'/></svg>"]
           ["CSS import" "<svg><style>@import 'https://evil.test/x.css';</style></svg>"]]]
    (testing label
      (is (thrown? js/Error (svg-render/validate-svg! payload))))))

(deftest ^:async prepare-page-disables-code-and-denies-network
  (let [calls (atom [])
        request-handler (atom nil)
        page #js {}]
    (aset page "setViewport"
          (fn [viewport]
            (swap! calls conj [:viewport (js->clj viewport :keywordize-keys true)])
            (js/Promise.resolve nil)))
    (aset page "setJavaScriptEnabled"
          (fn [enabled?]
            (swap! calls conj [:javascript enabled?])
            (js/Promise.resolve nil)))
    (aset page "setRequestInterception"
          (fn [enabled?]
            (swap! calls conj [:interception enabled?])
            (js/Promise.resolve nil)))
    (aset page "on"
          (fn [event-name handler]
            (swap! calls conj [:listener event-name])
            (reset! request-handler handler)
            page))

    (is (= page (await (svg-render/prepare-page! page 320 180))))
    (is (= [[:viewport {:width 320 :height 180}]
            [:javascript false]
            [:interception true]
            [:listener "request"]]
           @calls))

    (let [abort-calls (atom [])
          request #js {}]
      (aset request "abort"
            (fn [reason]
              (swap! abort-calls conj reason)
              (js/Promise.resolve nil)))
      (@request-handler request)
      (is (= ["blockedbyclient"] @abort-calls)))))

(deftest ^:async svg->png-renders-browser-svg-features
  (testing "Chromium produces a PNG buffer for filter, text font fallback, and text gradient SVGs"
    (if-not (executable-available?)
      (is true "Skipping Chromium SVG render smoke test; no executable configured/found.")
      (try
        (let [buf (await (svg-render/svg->png browser-feature-svg {:width 240 :height 120}))]
          (is (= "89504e470d0a1a0a" (.toString (.subarray buf 0 8) "hex")))
          (is (> (.-length buf) 1000)))
        (await (svg-render/shutdown!))
        (catch :default err
          (is false (str err))
          (await (svg-render/shutdown!)))))))
