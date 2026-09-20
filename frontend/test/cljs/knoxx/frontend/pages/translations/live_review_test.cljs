(ns knoxx.frontend.pages.translations.live-review-test
  "Candidate, review and source identity all constrain unfinished translation feedback."
  (:require [cljs.test :as t]
            [knoxx.frontend.pages.translations.live-review :as live]))

(t/deftest review-basis-detects-source-candidate-and-history-updates
  (let [row {:publication "publication/es" :revision "source-1"}
        segment {:id "split/1" :resource_split true :candidate_set_id "candidate/1"
                 :review_id "review/1" :source_text "Hello" :translated_text "Hola"}
        basis (live/review-basis row segment)]
    (t/is (not= basis (live/review-basis (assoc row :revision "source-2") segment)))
    (t/is (not= basis (live/review-basis row (assoc segment :candidate_set_id "candidate/2"))))
    (t/is (not= basis (live/review-basis row (assoc segment :review_id "review/2"))))
    (t/is (not= basis (live/review-basis row (assoc segment :translated_text "Buenos días"))))))

(t/deftest unchanged-hydration-is-clean-and-human-corrections-are-dirty
  (let [form {:corrected_text "Hola" :editor_notes ""} draft {:baseline form}]
    (t/is (not (live/dirty? draft form)))
    (t/is (live/dirty? draft (assoc form :corrected_text "Buenas")))
    (t/is (live/dirty? draft (assoc form :editor_notes "Keep the formal register")))))
