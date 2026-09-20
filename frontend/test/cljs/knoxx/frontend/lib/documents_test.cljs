(ns knoxx.frontend.lib.documents-test
  "Session transport preserves JSON, multipart, and HTTP refusal contracts."
  (:require [cljs.test :as test]
            [knoxx.frontend.lib.documents :as documents]))

(def ^:private originals (atom nil))

(test/use-fixtures :each
  {:before (fn []
             (reset! originals {:fetch js/fetch
                                :session-storage (js/Object.getOwnPropertyDescriptor js/globalThis "sessionStorage")})
             (js/Object.defineProperty
              js/globalThis "sessionStorage"
              #js {:configurable true :value #js {:getItem (fn [_] "document-session")}}))
   :after (fn []
            (set! (.-fetch js/globalThis) (:fetch @originals))
            (if-let [descriptor (:session-storage @originals)]
              (js/Object.defineProperty js/globalThis "sessionStorage" descriptor)
              (js/Reflect.deleteProperty js/globalThis "sessionStorage")))})

(test/deftest ^:async json-requests-keep-session-payload-and-error-text
  (let [captured (atom nil)
        status (atom 200)
        body (atom "{\"saved\":true}")]
    (set! (.-fetch js/globalThis)
          (fn [path init]
            (reset! captured {:path path :init init})
            (js/Promise.resolve (js/Response. @body #js {:status @status}))))
    (test/is (= {:saved true} (await (documents/update-database "lake/a" {:forumMode false}))))
    (let [{:keys [path init]} @captured]
      (test/is (= "/api/settings/databases/lake%2Fa" path))
      (test/is (= "PATCH" (.-method init)))
      (test/is (= "document-session" (.get (.-headers init) "x-knoxx-session-id")))
      (test/is (= "application/json" (.get (.-headers init) "Content-Type")))
      (test/is (= {:forumMode false} (js->clj (js/JSON.parse (.-body init)) :keywordize-keys true))))
    (doseq [[code text expected] [[403 "Lake is private" "Lake is private"]
                                [502 "" "Request failed: 502"]]]
      (reset! status code)
      (reset! body text)
      (test/is (= expected (try (await (documents/fetch-documents)) nil
                               (catch :default cause (.-message cause))))))))

(test/deftest ^:async multipart-upload-keeps-files-and-browser-content-type
  (let [captured (atom nil)
        status (atom 200)
        file (js/File. #js ["document"] "a.md")]
    (set! (.-fetch js/globalThis)
          (fn [path init]
            (reset! captured {:path path :init init})
            (js/Promise.resolve (js/Response. "{\"uploaded\":1}" #js {:status @status}))))
    (test/is (= {:uploaded 1} (await (documents/upload-documents [file] true))))
    (let [{:keys [path init]} @captured]
      (test/is (= "/api/documents/upload" path))
      (test/is (= "POST" (.-method init)))
      (test/is (= "document-session" (.get (.-headers init) "x-knoxx-session-id")))
      (test/is (nil? (.get (.-headers init) "Content-Type")))
      (test/is (= "a.md" (.-name (.get (.-body init) "files"))))
      (test/is (= "true" (.get (.-body init) "autoIngest"))))
    (reset! status 403)
    (test/is (= "Failed to upload documents"
                (try (await (documents/upload-documents [file] false)) nil
                     (catch :default cause (.-message cause)))))))
