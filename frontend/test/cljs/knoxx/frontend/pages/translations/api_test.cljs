(ns knoxx.frontend.pages.translations.api-test
  "Wire-shape tests for project/garden scoped legacy translation calls."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.api :as http]
            [knoxx.frontend.pages.translations.api :as api]))

(def calls (atom []))
(def ^:private original-request http/request)

(t/use-fixtures
 :each
 {:before (fn []
            (reset! calls [])
            (set! http/request
                  (fn
                    ([path]
                     (swap! calls conj [path nil])
                     {:ok true})
                    ([path opts]
                     (swap! calls conj [path opts])
                     {:ok true}))))
  :after (fn [] (set! http/request original-request))})

(t/deftest document-detail-encodes-project-and-garden-query
  (api/get-document "docs/a b" "pt-BR"
                    {:project "knoxx-session" :garden-id "gardens/sonic"})
  (t/is (= [["/api/translations/documents/docs%2Fa%20b/pt-BR?project=knoxx-session&garden_id=gardens%2Fsonic"
             nil]]
           @calls)))

(t/deftest document-and-segment-review-carry-row-scope-in-body
  (api/review-document "docs/a" "es"
                       {:project "knoxx-session" :garden-id "gardens/sonic"}
                       {:overall "reject"})
  (api/submit-label "segment/1"
                    {:project "knoxx-session" :garden-id "gardens/sonic"}
                    {:overall "needs_edit" :adequacy "poor"})
  (t/is (= [["/api/translations/documents/docs%2Fa/es/review"
             {:method "POST"
              :body {:overall "reject"
                     :project "knoxx-session"
                     :garden_id "gardens/sonic"}}]
            ["/api/translations/segments/segment%2F1/labels"
             {:method "POST"
              :body {:overall "needs_edit"
                     :adequacy "poor"
                     :project "knoxx-session"
                     :garden_id "gardens/sonic"}}]]
           @calls)))
