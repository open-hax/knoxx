(ns knoxx.frontend.app-route-ownership-test
  "Check the production route declaration against capabilities retained by the editor."
  (:require [cljs.test :as t]
            [knoxx.frontend.infra.migration-manifest :as manifest]))

(t/deftest visual-draft-deep-links-retain-the-editor-that-consumes-their-path
  (let [matches (filter #(and (= :route (:kind %))
                             (= "(str routes/cms-editor-route \"/*\")" (:route %)))
                       (manifest/current-records))]
    (t/is (= 1 (count matches)) "The draft-path route remains uniquely declared")
    (t/is (= "app/VisualCmsEditorPage" (:implementation (first matches)))
          "The generic CMS page cannot load or save a visual draft selected by the URL splat")))
