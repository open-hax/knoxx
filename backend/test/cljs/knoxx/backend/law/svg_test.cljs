(ns knoxx.backend.law.svg-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.law.svg :as svg]))

(def browser-feature-svg
  "<svg width='240' height='120' xmlns='http://www.w3.org/2000/svg'>
     <defs>
       <filter id='glow'><feGaussianBlur stdDeviation='3'/></filter>
       <linearGradient id='g'><stop offset='0%' stop-color='#ff00aa'/></linearGradient>
       <path id='shape' d='M0 0h10v10z'/>
     </defs>
     <rect width='10' height='10' fill='url(#g)' filter='url(#glow)'/>
     <use href='#shape'/>
   </svg>")

(defn- rejection-data
  [value]
  (try
    (svg/validate-svg! value)
    nil
    (catch :default error
      (ex-data error))))

(deftest accepts-one-static-resource-local-SVG-document
  (is (= browser-feature-svg (svg/validate-svg! browser-feature-svg)))
  (is (= "<svg/>" (svg/validate-svg! "  <svg/>  ")))
  (is (= "<svg><svg><rect/></svg></svg>"
         (svg/validate-svg! "<svg><svg><rect/></svg></svg>")))
  (is (= "<svg><path data-label='a > b'/><!-- ok --><![CDATA[2 < 3]]></svg>"
         (svg/validate-svg!
          "<svg><path data-label='a > b'/><!-- ok --><![CDATA[2 < 3]]></svg>"))))

(deftest rejects-active-content-and-resource-loading
  (doseq [[label payload]
          [["non-string" #js {}]
           ["blank" "   "]
           ["missing root" "<div>not svg</div>"]
           ["doctype" "<svg><!DOCTYPE svg></svg>"]
           ["processing instruction" "<svg><?xml-stylesheet href='https://evil.test/x.css'?></svg>"]
           ["script" "<svg><script>alert(1)</script></svg>"]
           ["foreignObject" "<svg><foreignObject><div>html</div></foreignObject></svg>"]
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
      (is (some? (rejection-data payload))))))

(deftest requires-exactly-one-balanced-root-with-no-trailing-content
  (doseq [[label payload expected-type]
          [["trailing HTML" "<svg></svg><div>unexpected</div>" :svg/multiple-roots]
           ["trailing SVG" "<svg></svg><svg></svg>" :svg/multiple-roots]
           ["trailing text" "<svg></svg>unexpected" :svg/trailing-content]
           ["leading comment" "<!-- outside --><svg/>" :svg/outside-root-markup]
           ["trailing comment" "<svg/><!-- outside -->" :svg/outside-root-markup]
           ["mismatched tags" "<svg><g></svg>" :svg/mismatched-tags]
           ["unclosed root" "<svg><g/></svg" :svg/unterminated-tag]
           ["unclosed child" "<svg><g></svg>" :svg/mismatched-tags]
           ["unterminated comment" "<svg><!-- nope</svg>" :svg/unterminated-comment]
           ["unterminated CDATA" "<svg><![CDATA[nope</svg>" :svg/unterminated-cdata]]]
    (testing label
      (let [data (rejection-data payload)]
        (is (= expected-type (:type data)))
        (is (string? (:preview data)))))))
