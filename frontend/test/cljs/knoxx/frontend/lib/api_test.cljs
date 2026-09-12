(ns knoxx.frontend.lib.api-test
  "Written FIRST (TDD) — contract for the shared knoxx request helper
  (CLJS port of lib/api/core.ts `request`): x-knoxx auth headers from
  localStorage, credentials include, JSON body handling, error-text
  propagation, keywordized results. Global fetch is mocked."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.api :as api]))

(def ^:private fetch-calls (atom []))
(def ^:private next-response (atom nil))

(defn- response [status body-str]
  #js {:ok (< status 400)
       :status status
       :json (fn [] (js/Promise.resolve (js/JSON.parse body-str)))
       :text (fn [] (js/Promise.resolve body-str))})

(def ^:private previous-fetch (atom nil))
(def ^:private previous-storage (atom nil))

(t/use-fixtures :each
  {:before (fn []
             (reset! previous-fetch (.-fetch js/globalThis))
             (reset! previous-storage (.-localStorage js/globalThis))
             (reset! fetch-calls [])
             (reset! next-response (response 200 "{\"ok\":true}"))
             (set! (.-localStorage js/globalThis)
                   #js {:getItem (fn [k]
                                   (case k
                                     "knoxx_user_email" "pi@open-hax.local"
                                     "knoxx_org_slug" "open-hax"
                                     nil))})
             (set! (.-fetch js/globalThis)
                   (fn [path init]
                     (swap! fetch-calls conj {:path path :init init})
                     (js/Promise.resolve @next-response))))
   :after (fn []
            (set! (.-fetch js/globalThis) @previous-fetch)
            (set! (.-localStorage js/globalThis) @previous-storage))})

(t/deftest ^:async get-request-sends-auth-headers-and-credentials
  (let [body (await (api/request "/api/thing"))
        {:keys [path ^js init]} (first @fetch-calls)]
    (t/is (= "/api/thing" path))
    (t/is (= "include" (.-credentials init)))
    (t/is (= "pi@open-hax.local" (.get (.-headers init) "x-knoxx-user-email")))
    (t/is (= "open-hax" (.get (.-headers init) "x-knoxx-org-slug")))
    (t/is (nil? (.get (.-headers init) "Content-Type")) "no content type without body")
    (t/is (= {:ok true} body) "json result keywordized")))

(t/deftest ^:async post-request-encodes-json-body
  (await (api/request "/api/thing" {:method "POST" :body {:model "glm-5"}}))
  (let [{:keys [^js init]} (first @fetch-calls)]
    (t/is (= "POST" (.-method init)))
    (t/is (= "application/json" (.get (.-headers init) "Content-Type")))
    (t/is (= "{\"model\":\"glm-5\"}" (.-body init)))))

(t/deftest ^:async error-responses-throw-with-body-text
  (reset! next-response (response 500 "kaboom"))
  (try
    (await (api/request "/api/thing"))
    (t/is false "should have thrown")
    (catch :default error
      (t/is (= "kaboom" (.-message error))))))

(t/deftest ^:async request-text-returns-raw-body
  (reset! next-response (response 200 "line1\nline2"))
  (t/is (= "line1\nline2" (await (api/request-text "/api/translations/export/sft")))))

(t/deftest ^:async missing-identity-omits-headers
  (set! (.-localStorage js/globalThis) #js {:getItem (fn [_] nil)})
  (await (api/request "/api/thing"))
  (let [{:keys [^js init]} (first @fetch-calls)]
    (t/is (nil? (.get (.-headers init) "x-knoxx-user-email")))))
