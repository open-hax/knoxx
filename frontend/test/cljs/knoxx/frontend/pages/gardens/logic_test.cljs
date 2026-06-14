(ns knoxx.frontend.pages.gardens.logic-test
  "Written FIRST (TDD) — pure-logic contract for the Helix port of
  src/pages/GardensPage.tsx. The build-save-request payload assertions
  mirror GardensPage.test.tsx exactly."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.frontend.pages.gardens.logic :as logic]))

(def blank-form
  {:garden-id "" :title "" :description "" :theme "monokai"
   :status "active" :target-languages [] :auto-translate true})

(deftest theme-info-lookup
  (is (= "Monokai" (:label (logic/theme-info "monokai"))))
  (is (= "#011627" (get-in (logic/theme-info "night-owl") [:colors :bg])))
  (is (nil? (logic/theme-info "nope")))
  (is (= 3 (count logic/themes))))

(deftest language-name-lookup
  (is (= "Español" (logic/language-name "es")))
  (is (= "日本語" (logic/language-name "ja")))
  (is (= "xx" (logic/language-name "xx")) "unknown codes pass through")
  (is (= 15 (count logic/available-languages))))

(deftest toggle-language-adds-and-removes
  (is (= ["es"] (logic/toggle-language [] "es")))
  (is (= ["es" "fr"] (logic/toggle-language ["es"] "fr")))
  (is (= ["fr"] (logic/toggle-language ["es" "fr"] "es"))))

(deftest validate-form-requires-id-and-title
  (is (= "Garden ID and title are required" (logic/validate-form blank-form)))
  (is (= "Garden ID and title are required"
         (logic/validate-form (assoc blank-form :garden-id "g" :title "  "))))
  (is (nil? (logic/validate-form (assoc blank-form :garden-id "g" :title "T")))))

(deftest build-save-request-create
  (let [form (assoc blank-form
                    :garden-id " new-garden "
                    :title "New Garden"
                    :description "A test garden"
                    :target-languages ["es"])
        {:keys [url method body]} (logic/build-save-request form false)]
    (is (= "/api/openplanner/v1/gardens" url))
    (is (= "POST" method))
    (is (= {:garden_id "new-garden"
            :title "New Garden"
            :description "A test garden"
            :theme "monokai"
            :target_languages ["es"]
            :auto_translate true}
           body)
        "payload matches the vitest contract; garden_id trimmed; no status on create")))

(deftest build-save-request-edit
  (let [form (assoc blank-form
                    :garden-id "fork-garden"
                    :title "Fork Garden"
                    :description "d"
                    :status "archived"
                    :theme "night-owl"
                    :auto-translate false)
        {:keys [url method body]} (logic/build-save-request form true)]
    (is (= "/api/openplanner/v1/gardens/fork-garden" url))
    (is (= "PATCH" method))
    (is (= {:title "Fork Garden"
            :description "d"
            :theme "night-owl"
            :status "archived"
            :target_languages []
            :auto_translate false}
           body)
        "edit payload includes status but never garden_id"))
  (testing "garden id is URL-encoded"
    (is (= "/api/openplanner/v1/gardens/a%20b"
           (:url (logic/build-save-request (assoc blank-form :garden-id "a b") true))))))

(deftest garden-html-url
  (is (= "/api/openplanner/v1/public/gardens/fork-garden/html"
         (logic/garden-html-url "fork-garden"))))

(deftest form-from-garden-prefills-edit-state
  (let [form (logic/form-from-garden {:garden_id "g1" :title "T" :description "D"
                                      :status "draft" :target_languages ["es" "fr"]})]
    (is (= "g1" (:garden-id form)))
    (is (= "T" (:title form)))
    (is (= "monokai" (:theme form)) "missing theme defaults")
    (is (= "draft" (:status form)))
    (is (= ["es" "fr"] (:target-languages form)))
    (is (true? (:auto-translate form)) "missing auto_translate defaults true")))
