(ns knoxx.frontend.infra.migration-html-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-html :as html]))

(defn- with-files [files inspect]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-migration-html-"))]
    (try
      (doseq [[path source] files]
        (let [absolute (node-path/join root path)]
          (fs/mkdirSync (node-path/dirname absolute) #js {:recursive true})
          (fs/writeFileSync absolute source)))
      (inspect root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest current-bootstrap-and-inert-markup-remain-admissible
  (doseq [source ["<div id='root'></div><script src='/cljs/app.js'></script>"
                  "<SCRIPT SRC='&#47;cljs/app.js'></SCRIPT>"
                  "<script type='application/json'>{\"source\":\"console.log('data')\"}</script>"
                  "<script type='text/plain'>example()</script><p>onclick is documentation</p>"
                  "<img src='/favicon.svg'><a href='/docs'>Documentation</a>"
                  "<p data-example='javascript:example()'>Documentation</p>"]]
    (t/is (nil? (html/assert-html! "frontend/index.html" source)))))

(t/deftest html-cannot-load-relocated-public-javascript
  (doseq [source ["<script src='/legacy.js'></script>"
                  "<ScRiPt SrC='&#47;legacy.js'></ScRiPt>"
                  "<script type='module' src='/legacy.js'></script>"
                  "<script language='javascript' src='/legacy.js'></script>"
                  "<script>window.legacy = true</script>"
                  "<script src='/cljs/app.js'>window.legacy = true</script>"
                  "<svg><script href='/legacy.js'></script></svg>"]]
    (t/is (thrown? js/Error (html/assert-html! "frontend/index.html" source)) source)))

(t/deftest active-attributes-and-base-redirection-cannot-hide-html-code
  (doseq [source ["<body ONLOAD='window.legacy=true'>"
                  "<svg onload='window.legacy=true'></svg>"
                  "<a href='java&#x73;cript:legacy()'>Run</a>"
                  "<a href=' &#9;java&#10;script:legacy()'>Run</a>"
                  "<base href='https://example.invalid/'><script src='/cljs/app.js'></script>"]]
    (t/is (thrown? js/Error (html/assert-html! "frontend/index.html" source)) source)))

(t/deftest srcdoc-html-uses-the-same-execution-contract
  (t/is (nil? (html/assert-html! "frontend/index.html" "<iframe srcdoc='&lt;p&gt;Help&lt;/p&gt;'></iframe>")))
  (t/is (thrown? js/Error
                (html/assert-html! "frontend/index.html"
                                   "<iframe srcdoc='&lt;script&gt;legacy()&lt;/script&gt;'></iframe>"))))

(t/deftest copied-public-html-is-inspected-without-rejecting-generated-javascript
  (with-files {"frontend/index.html" "<script src='/cljs/app.js'></script>"
               "frontend/public/index.html" "<script src='/cljs/app.js'></script>"
               "frontend/public/cljs/app.js" "generatedShadowCode();"
               "frontend/public/cljs/cljs-runtime/legacy.js" "generatedShadowRuntime();"}
    (fn [root] (t/is (nil? (html/assert-entrypoints! root)))))
  (doseq [path ["frontend/index.html" "frontend/public/index.html" "frontend/public/help/nested.HTM"]]
    (with-files {path "<script src='/legacy.js'></script>"
                 "frontend/public/legacy.js" "window.legacy=true;"}
      (fn [root] (t/is (thrown? js/Error (html/assert-entrypoints! root)) path)))))
