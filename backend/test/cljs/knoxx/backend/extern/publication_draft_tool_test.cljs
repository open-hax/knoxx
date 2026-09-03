(ns knoxx.backend.extern.publication-draft-tool-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.publication-draft-tool :as draft-tool]))

(deftest raw-save-draft-arguments-decode-to-validated-cljs-data
  (is (= {:title "A grounded post"
          :content "# A grounded post\n\nDraft body."}
         (draft-tool/decode-save-draft-params!
          #js {:title "A grounded post"
               :content "# A grounded post\n\nDraft body."})))
  (is (= {:content "# Title derived here"}
         (draft-tool/decode-save-draft-params!
          #js {:content "# Title derived here"}))))

(deftest malformed-save-draft-arguments-fail-at-the-wire-boundary
  (doseq [[label params] [["missing content" #js {:title "Incomplete"}]
                          ["wrong content type" #js {:content 42}]
                          ["wrong title type" #js {:title 42 :content "Body"}]
                          ["undeclared field" #js {:content "Body" :publish true}]
                          ["missing object" nil]]]
    (testing label
      (let [error (try
                    (draft-tool/decode-save-draft-params! params)
                    nil
                    (catch :default err err))]
        (is (= 400 (:status (ex-data error))))
        (is (= :publication-draft-tool-params-invalid
               (:code (ex-data error))))))))
