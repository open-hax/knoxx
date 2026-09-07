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
                  "<math><mi onclick='1+1'>x</mi></math>"
                  "<a href='java&#x73;cript:legacy()'>Run</a>"
                  "<a href=' &#9;java&#10;script:legacy()'>Run</a>"
                  "<base href='https://example.invalid/'><script src='/cljs/app.js'></script>"]]
    (t/is (thrown? js/Error (html/assert-html! "frontend/index.html" source)) source)))

(t/deftest srcdoc-html-uses-the-same-execution-contract
  (t/is (nil? (html/assert-html! "frontend/index.html" "<iframe srcdoc='&lt;p&gt;Help&lt;/p&gt;'></iframe>")))
  (t/is (thrown? js/Error
                (html/assert-html! "frontend/index.html"
                                   "<iframe srcdoc='&lt;script&gt;legacy()&lt;/script&gt;'></iframe>"))))

(t/deftest document-data-urls-cannot-hide-executable-html-or-xml
  (doseq [source ["<iframe src='data:text/html,&lt;script&gt;1+1&lt;/script&gt;'></iframe>"
                  "<iframe src=' &#9;DA&#10;TA:TEXT/HTML;charset=utf-8;base64,PHNjcmlwdD4xKzE8L3NjcmlwdD4='></iframe>"
                  "<object data='data:image/svg+xml,&lt;svg/&gt;'></object>"
                  "<embed src='data:application/xhtml+xml,&lt;html/&gt;'>"
                  "<iframe src='data:text/xml,&lt;document/&gt;'></iframe>"
                  "<iframe src='data:application/xml,&lt;document/&gt;'></iframe>"
                  "<a target='preview' href='data:text/html,&lt;script&gt;1+1&lt;/script&gt;'>Preview</a>"
                  "<iframe srcdoc='&lt;iframe src=&quot;data:text/html,%3Cscript%3E1%2B1%3C/script%3E&quot;&gt;&lt;/iframe&gt;'></iframe>"]]
    (t/is (thrown? js/Error (html/assert-html! "frontend/index.html" source)) source)))

(t/deftest data-images-and-inert-data-remain-admissible
  (doseq [source ["<img src='data:image/svg+xml,&lt;svg/&gt;'>"
                  "<img src='data:image/png;base64,iVBORw0KGgo='>"
                  "<iframe src='data:text/plain,%3Cscript%3E1%2B1%3C/script%3E'></iframe>"
                  "<iframe src='data:,%3Cscript%3E1%2B1%3C/script%3E'></iframe>"
                  "<iframe src='data:not+xml,hello'></iframe>"
                  "<iframe src='data:invalid type/xml+xml,hello'></iframe>"
                  "<div data-example='data:text/html,&lt;script&gt;1+1&lt;/script&gt;'></div>"]]
    (t/is (nil? (html/assert-html! "frontend/index.html" source)) source)))

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

(t/deftest copied-xml-browser-documents-use-the-same-execution-contract
  (doseq [[extension source]
          [["svg" "<svg xmlns='http://www.w3.org/2000/svg'><script>1+1</script></svg>"]
           ["SVG" "<svg xmlns='http://www.w3.org/2000/svg' onload='1+1'/>"]
           ["svg" "<svg xmlns='http://www.w3.org/2000/svg'><script href='/legacy.js'/></svg>"]
           ["svg" "<svg xmlns='http://www.w3.org/2000/svg' xmlns:l='http://www.w3.org/1999/xlink'><a l:href='javascript:1+1'/></svg>"]
           ["xml" "<h:html xmlns:h='http://www.w3.org/1999/xhtml'><h:script>1+1</h:script></h:html>"]
           ["xhtml" "<html xmlns='http://www.w3.org/1999/xhtml'><body onload='1+1'/></html>"]
           ["xhtml" "<html xmlns='http://www.w3.org/1999/xhtml' xml:base='https://example.invalid/'><script src='/cljs/app.js'/></html>"]
           ["xml" "<svg xmlns='http://www.w3.org/2000/svg'><a href='data:text/html,&lt;script&gt;1+1&lt;/script&gt;'/></svg>"]]]
    (let [path (str "frontend/public/nested/legacy." extension)]
      (with-files {"frontend/index.html" "<iframe src='/nested/legacy.svg'></iframe>"
                   path source}
        (fn [root] (t/is (thrown? js/Error (html/assert-entrypoints! root)) source))))))

(t/deftest static-svg-and-xml-metadata-remain-admissible
  (with-files {"frontend/index.html" "<img src='/logo.svg'>"
               "frontend/public/logo.svg" "<svg xmlns='http://www.w3.org/2000/svg'><path d='M0 0h10v10z'/><metadata><m:script xmlns:m='urn:metadata' onclick='documentation'>Description</m:script></metadata><SCRIPT>Case-sensitive metadata</SCRIPT></svg>"
               "frontend/public/metadata.xml" "<metadata><script onclick='documentation'>Description</script></metadata>"
               "frontend/public/help.xhtml" "<html xmlns='http://www.w3.org/1999/xhtml'><body><p>Help</p><script type='application/json'>{}</script></body></html>"
               "frontend/public/cljs/app.js" "generatedShadowCode();"}
    (fn [root] (t/is (nil? (html/assert-entrypoints! root))))))
