(ns knoxx.frontend.lib.api-test
  "Written FIRST (TDD) — contract for the shared knoxx request helper
  (CLJS port of lib/api/core.ts `request`): x-knoxx auth headers from
  localStorage, credentials include, JSON body handling, error-text
  propagation, keywordized results. Global fetch is mocked."
  (:require [cljs.test :refer [deftest is async use-fixtures]]
            [knoxx.frontend.lib.api :as api]))

(def fetch-calls (atom []))
(def next-response (atom nil))

(defn- response [status body-str]
  #js {:ok (< status 400)
       :status status
       :json (fn [] (js/Promise.resolve (js/JSON.parse body-str)))
       :text (fn [] (js/Promise.resolve body-str))})

(def ^:private real-fetch js/fetch)

(use-fixtures :each
  {:before (fn []
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
   :after (fn [] (set! (.-fetch js/globalThis) real-fetch))})

(deftest get-request-sends-auth-headers-and-credentials
  (async done
    (-> (api/request "/api/thing")
        (.then (fn [body]
                 (let [{:keys [path ^js init]} (first @fetch-calls)]
                   (is (= "/api/thing" path))
                   (is (= "include" (.-credentials init)))
                   (is (= "pi@open-hax.local" (.get (.-headers init) "x-knoxx-user-email")))
                   (is (= "open-hax" (.get (.-headers init) "x-knoxx-org-slug")))
                   (is (nil? (.get (.-headers init) "Content-Type")) "no content type without body")
                   (is (= {:ok true} body) "json result keywordized"))
                 (done)))
        (.catch (fn [err] (is false (str err)) (done))))))

(deftest post-request-encodes-json-body
  (async done
    (-> (api/request "/api/thing" {:method "POST" :body {:model "glm-5"}})
        (.then (fn [_]
                 (let [{:keys [^js init]} (first @fetch-calls)]
                   (is (= "POST" (.-method init)))
                   (is (= "application/json" (.get (.-headers init) "Content-Type")))
                   (is (= "{\"model\":\"glm-5\"}" (.-body init))))
                 (done)))
        (.catch (fn [err] (is false (str err)) (done))))))

(deftest error-responses-throw-with-body-text
  (async done
    (reset! next-response (response 500 "kaboom"))
    (-> (api/request "/api/thing")
        (.then (fn [_] (is false "should have thrown") (done)))
        (.catch (fn [^js err]
                  (is (= "kaboom" (.-message err)))
                  (done))))))

(deftest request-text-returns-raw-body
  (async done
    (reset! next-response (response 200 "line1\nline2"))
    (-> (api/request-text "/api/translations/export/sft")
        (.then (fn [text]
                 (is (= "line1\nline2" text))
                 (done)))
        (.catch (fn [err] (is false (str err)) (done))))))

(deftest missing-identity-omits-headers
  (async done
    (set! (.-localStorage js/globalThis) #js {:getItem (fn [_] nil)})
    (-> (api/request "/api/thing")
        (.then (fn [_]
                 (let [{:keys [^js init]} (first @fetch-calls)]
                   (is (nil? (.get (.-headers init) "x-knoxx-user-email"))))
                 (done)))
        (.catch (fn [err] (is false (str err)) (done))))))
